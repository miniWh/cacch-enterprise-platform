#!/bin/bash
# ============================================================
# 服务器端：安装已上传到 staging 的 War 包
# 由本地 07-build-local-backend.sh 上传后调用，也可在服务器手工执行
# 用法：/opt/cacch/install-war.sh
#
# 关键约束（踩过坑）：
#   1. 本服务由 systemd 托管，必须且只能通过 systemctl 启停。
#      绝不要 fallback 到 startup.sh 直接启动 —— 那会拉起游离 JVM，
#      systemd 之后的自动重启会因 8080 端口冲突全部失败。
#   2. 需要 felix 的免密 sudo（见 deploy/sudoers-cacch）
# ============================================================
set -uo pipefail

CACCH_HOME=/opt/cacch
TOMCAT=${CACCH_HOME}/tomcat
STAGING=${CACCH_HOME}/staging
WAR_SRC=${STAGING}/cacch-enterprise-web.war
BACKUP_DIR=${CACCH_HOME}/backups
UNIT=/etc/systemd/system/cacch-tomcat.service

if [ ! -f "${WAR_SRC}" ]; then
    echo "!! 未找到待安装包：${WAR_SRC}"
    exit 1
fi

# 是否由 systemd 托管：只看 unit 文件是否存在（不依赖 systemctl 查询，
# 非交互环境下 systemctl list-unit-files 可能失败导致误判）
USE_SYSTEMD=false
[ -f "${UNIT}" ] && USE_SYSTEMD=true

JAR_IN_WAR=$(unzip -l "${WAR_SRC}" 2>/dev/null | grep -c 'WEB-INF/lib/.*\.jar' || true)
echo ">> 待安装包：$(du -h "${WAR_SRC}" | cut -f1)"
echo ">> War 内依赖 jar 数：${JAR_IN_WAR:-0}（Skinny War 应为 0）"
echo ">> Tomcat lib 依赖数：$(ls -1 ${TOMCAT}/lib/*.jar 2>/dev/null | wc -l)"
echo ">> 启停方式：$([ "$USE_SYSTEMD" = true ] && echo systemd || echo 脚本直启)"

port_busy() { ss -lnt 2>/dev/null | grep -q ':8080'; }

# ---------- 备份 ----------
if [ -f "${TOMCAT}/webapps/ROOT.war" ]; then
    mkdir -p "${BACKUP_DIR}"
    cp "${TOMCAT}/webapps/ROOT.war" "${BACKUP_DIR}/ROOT.war.$(date +%Y%m%d_%H%M%S)"
    ls -1t "${BACKUP_DIR}"/ROOT.war.* 2>/dev/null | tail -n +6 | xargs -r rm -f
    echo ">> 已备份旧包到 ${BACKUP_DIR}（保留最近 5 份）"
fi

# ---------- 停止 ----------
echo ">> 停止应用"
if [ "${USE_SYSTEMD}" = true ]; then
    sudo -n systemctl stop cacch-tomcat || echo "   （systemctl stop 返回非 0，继续检查端口）"
else
    "${TOMCAT}/bin/shutdown.sh" >/dev/null 2>&1 || true
fi
for i in $(seq 1 20); do
    port_busy || break
    sleep 1
done

# 兜底：清掉任何残留/游离的 Tomcat JVM，否则后续启动必定端口冲突
if port_busy; then
    echo "!! 8080 仍被占用，清理残留进程"
    pkill -9 -f "catalina.base=${CACCH_HOME}/tomcat" || true
    sleep 3
fi
port_busy && { echo "!! 8080 无法释放，已中止部署（未改动现有包）"; exit 1; }

# ---------- 部署 ----------
echo ">> 部署（重命名为 ROOT.war，context path = /）"
rm -rf "${TOMCAT}/webapps/ROOT" "${TOMCAT}/webapps/ROOT.war"
cp "${WAR_SRC}" "${TOMCAT}/webapps/ROOT.war"
rm -f "${WAR_SRC}"

# ---------- 启动 ----------
echo ">> 启动应用"
if [ "${USE_SYSTEMD}" = true ]; then
    sudo -n systemctl start cacch-tomcat
else
    "${TOMCAT}/bin/startup.sh" >/dev/null
fi

echo ">> 等待启动完成..."
for i in $(seq 1 90); do
    if curl -sf -o /dev/null --max-time 3 http://127.0.0.1:8080/api/v1/auth/captcha; then
        echo ">> 接口已就绪（约 $((i*2))s）"
        break
    fi
    sleep 2
done

# ---------- 校验 ----------
if [ "${USE_SYSTEMD}" = true ]; then
    STATE=$(systemctl is-active cacch-tomcat 2>/dev/null)
    echo ">> systemd 状态：${STATE}"
    if [ "${STATE}" != "active" ]; then
        echo "!! 服务未进入 active 状态（很可能又出现了游离进程或启动失败）"
        echo "!! 排查：systemctl status cacch-tomcat --no-pager -l"
        echo "!!       ss -lntp | grep 8080"
        exit 1
    fi
fi

if curl -sf -o /dev/null --max-time 5 http://127.0.0.1:8080/api/v1/auth/captcha; then
    echo ">> 部署完成：${TOMCAT}/webapps/ROOT.war  $(du -h ${TOMCAT}/webapps/ROOT.war | cut -f1)"
    exit 0
fi

echo "!! 启动超时，请检查日志：tail -f ${TOMCAT}/logs/catalina.out"
echo "!! 回滚：cp \$(ls -1t ${BACKUP_DIR}/ROOT.war.* | head -1) ${TOMCAT}/webapps/ROOT.war 后重启"
exit 1

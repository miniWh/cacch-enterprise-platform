#!/bin/bash
# ============================================================
# 后端发版脚本（在服务器 /opt/cacch/src 下执行）
# 采用 Skinny War：依赖外置到 $CATALINA_HOME/lib，War 仅含业务代码（1~3MB）
# 拷贝时重命名为 ROOT.war 使 context path = /
# 用法：
#   ./deploy.sh          # 日常发版：跳过依赖导出，秒级出包
#   ./deploy.sh --full   # 依赖变更时（改了 pom）：重新同步依赖到 Tomcat lib
# ============================================================
set -euo pipefail

CACCH_HOME=/opt/cacch
SRC=${CACCH_HOME}/src
MVN=${CACCH_HOME}/maven/bin/mvn
TOMCAT=${CACCH_HOME}/tomcat
LIB_DIR=${TOMCAT}/lib
FULL=false
[ "${1:-}" = "--full" ] && FULL=true

cd "${SRC}"

if [ "$FULL" = true ]; then
    echo ">> 全量构建：同步依赖到 ${LIB_DIR}"
    $MVN clean package -DskipTests -Dtomcat.lib.dir="${LIB_DIR}"
    echo ">> Tomcat lib 依赖数量：$(ls -1 ${LIB_DIR}/*.jar 2>/dev/null | wc -l)"
else
    echo ">> 增量构建：跳过依赖导出（War 仅含业务代码）"
    $MVN clean package -DskipTests -Dmdep.skip=true
fi

WAR="${SRC}/cacch-enterprise-web/target/cacch-enterprise-web.war"
[ -f "$WAR" ] || { echo "!! 未找到 War 产物：$WAR"; exit 1; }
echo ">> War 大小：$(du -h "$WAR" | cut -f1)"

# 发版前自动备份当前 War，保留最近 5 份，便于快速回滚
BACKUP_DIR=${CACCH_HOME}/backups
if [ -f "${TOMCAT}/webapps/ROOT.war" ]; then
    mkdir -p "${BACKUP_DIR}"
    cp "${TOMCAT}/webapps/ROOT.war" "${BACKUP_DIR}/ROOT.war.$(date +%Y%m%d_%H%M%S)"
    ls -1t "${BACKUP_DIR}"/ROOT.war.* 2>/dev/null | tail -n +6 | xargs -r rm -f
    echo ">> 已备份旧包到 ${BACKUP_DIR}（保留最近 5 份）"
fi

echo ">> 停止应用"
# 只走 systemd，绝不 fallback 到 shutdown.sh/startup.sh：
# 直启会产生游离 JVM，导致 systemd 后续重启全部因 8080 冲突失败
sudo -n systemctl stop cacch-tomcat || true
for i in $(seq 1 20); do
    ss -lnt 2>/dev/null | grep -q ':8080' || break
    sleep 1
done
if ss -lnt 2>/dev/null | grep -q ':8080'; then
    echo "!! 8080 仍被占用，清理残留进程"
    pkill -9 -f "catalina.base=${CACCH_HOME}/tomcat" || true
    sleep 3
fi
if ss -lnt 2>/dev/null | grep -q ':8080'; then
    echo "!! 8080 无法释放，已中止部署（现有包未改动）"
    exit 1
fi

echo ">> 部署新包（重命名为 ROOT.war）"
rm -rf "${TOMCAT}/webapps/ROOT" "${TOMCAT}/webapps/ROOT.war"
cp "$WAR" "${TOMCAT}/webapps/ROOT.war"

echo ">> 启动应用"
sudo -n systemctl start cacch-tomcat

echo ">> 等待启动完成..."
OK=false
for i in $(seq 1 90); do
    if curl -sf -o /dev/null --max-time 3 http://127.0.0.1:8080/api/v1/auth/captcha; then
        OK=true
        echo ">> 启动成功（耗时约 $((i*2))s）"
        break
    fi
    sleep 2
done

STATE=$(systemctl is-active cacch-tomcat 2>/dev/null)
echo ">> systemd 状态：${STATE}"
[ "${STATE}" = "active" ] || { echo "!! 服务未进入 active 状态，请查：systemctl status cacch-tomcat"; exit 1; }
[ "${OK}" = true ] && exit 0

echo "!! 启动超时，请检查日志：tail -f ${TOMCAT}/logs/catalina.out"
echo "!! 若为 NoClassDefFoundError，多半是依赖未同步，请改用 ./deploy.sh --full"
exit 1

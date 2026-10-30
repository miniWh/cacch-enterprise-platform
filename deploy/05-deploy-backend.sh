#!/bin/bash
# ============================================================
# 阶段 5：首次部署后端（Skinny War → ROOT.war）
# 前置：01~04 已完成，且已在 /opt/cacch/src 构建出 War
# 执行账号：felix（无需 sudo）
# 后续日常发版请用 deploy/deploy.sh
# ============================================================
set -euo pipefail

CACCH_HOME=/opt/cacch
TOMCAT=${CACCH_HOME}/tomcat
SRC=${CACCH_HOME}/src
WAR=${SRC}/cacch-enterprise-web/target/cacch-enterprise-web.war

log() { echo -e "\n\033[32m[$(date +%H:%M:%S)] $*\033[0m"; }

log "1/4 setenv.sh 就位（JVM 参数 + profile + 外置配置路径）"
mkdir -p "${TOMCAT}/bin"
if [ ! -f "${TOMCAT}/bin/setenv.sh" ]; then
    cp "${SRC}/deploy/setenv.sh" "${TOMCAT}/bin/setenv.sh"
    chmod +x "${TOMCAT}/bin/setenv.sh"
fi
echo "已就位：$(ls -l ${TOMCAT}/bin/setenv.sh | awk '{print $1, $3, $4}')"

log "2/4 Connector 仅监听内网（8080 不对外暴露）"
if ! grep -q 'address="127.0.0.1"' "${TOMCAT}/conf/server.xml"; then
    sed -i 's|<Connector port="8080" protocol="HTTP/1.1"|<Connector port="8080" protocol="HTTP/1.1" address="127.0.0.1"|' \
        "${TOMCAT}/conf/server.xml"
    echo "已加固"
else
    echo "已加固，跳过"
fi
grep -A1 '<Connector port="8080"' "${TOMCAT}/conf/server.xml" | head -3

log "3/4 部署 War（重命名为 ROOT.war → context path = /）"
[ -f "$WAR" ] || { echo "!! 未找到 War：$WAR"; exit 1; }
echo "War 大小：$(du -h "$WAR" | cut -f1)"
"${TOMCAT}/bin/shutdown.sh" >/dev/null 2>&1 || true
sleep 2
rm -rf "${TOMCAT}/webapps/ROOT" "${TOMCAT}/webapps/ROOT.war"
cp "$WAR" "${TOMCAT}/webapps/ROOT.war"
ls -l "${TOMCAT}/webapps/"

log "4/4 启动并等待就绪"
"${TOMCAT}/bin/startup.sh"

for i in $(seq 1 90); do
    if curl -sf -o /dev/null --max-time 3 http://127.0.0.1:8080/api/v1/auth/captcha; then
        echo ""
        echo ">> 启动成功，耗时约 $((i*2))s"
        echo ">> captcha 接口响应：$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 http://127.0.0.1:8080/api/v1/auth/captcha)"
        exit 0
    fi
    sleep 2
done

echo ""
echo "!! 启动超时，最近日志："
tail -60 "${TOMCAT}/logs/catalina.out"
exit 1

#!/bin/bash
# ============================================================
# 阶段 3：Tomcat 11 + Nginx（含防火墙 / SELinux）
# 前置：已执行 01-install-jdk.sh、02-install-maven.sh
# 目标主机：10.80.62.200 (Rocky Linux 9.6)  执行账号：felix（需 sudo）
# 幂等：可重复执行
# ============================================================
set -euo pipefail

CACCH_HOME=/opt/cacch
TOMCAT_VER=11.0.27
MIRROR=https://mirrors.aliyun.com/apache
log() { echo -e "\n\033[32m[$(date +%H:%M:%S)] $*\033[0m"; }

log "1/5 安装 Nginx"
sudo dnf install -y nginx

log "2/5 安装 Tomcat ${TOMCAT_VER}"
if [ ! -d "${CACCH_HOME}/apache-tomcat-${TOMCAT_VER}" ]; then
    cd /tmp
    curl -sSL -O "${MIRROR}/tomcat/tomcat-11/v${TOMCAT_VER}/bin/apache-tomcat-${TOMCAT_VER}.tar.gz"
    tar -xzf "apache-tomcat-${TOMCAT_VER}.tar.gz" -C "${CACCH_HOME}"
    rm -f "apache-tomcat-${TOMCAT_VER}.tar.gz"
fi
# 清空自带示例应用，避免暴露 manager / examples
rm -rf "${CACCH_HOME}"/apache-tomcat-${TOMCAT_VER}/webapps/*

# 修正属主与执行权限：本脚本整体以 sudo 运行，解压产物会归 root，
# 否则 felix 无法向 webapps 部署 War、无法执行启动脚本
sudo chown -R felix:felix "${CACCH_HOME}/apache-tomcat-${TOMCAT_VER}"
sudo chmod +x "${CACCH_HOME}"/apache-tomcat-${TOMCAT_VER}/bin/*.sh

# 建立符号链接。必须用 -T（或先删目录）：
# 01 脚本曾 mkdir 过 ${CACCH_HOME}/tomcat，若目标是已存在的目录，
# ln -sfn 会把链接建到该目录【内部】，导致 /opt/cacch/tomcat 变成真目录
if [ -d "${CACCH_HOME}/tomcat" ] && [ ! -L "${CACCH_HOME}/tomcat" ]; then
    sudo rm -rf "${CACCH_HOME}/tomcat"
fi
sudo ln -sfnT "${CACCH_HOME}/apache-tomcat-${TOMCAT_VER}" "${CACCH_HOME}/tomcat"
sudo chown -h felix:felix "${CACCH_HOME}/tomcat"

log "3/5 关闭 Tomcat 启动 jar 扫描（Skinny War 依赖外置后必需）"
CONF="${CACCH_HOME}/apache-tomcat-${TOMCAT_VER}/conf/catalina.properties"
if ! grep -q 'jarsToSkip=\*\.jar' "$CONF"; then
    cat >> "$CONF" <<'EOF'

# cacch: Skinny War 依赖外置后关闭全量 jar 扫描，显著加快启动
tomcat.util.scan.StandardJarScanFilter.jarsToSkip=*.jar
tomcat.util.scan.StandardJarScanFilter.jarsToScan=spring-web-*.jar,cacch-*.jar
EOF
fi

log "4/5 防火墙与 SELinux"
sudo firewall-cmd --permanent --add-service=http >/dev/null
sudo firewall-cmd --reload >/dev/null
sudo setsebool -P httpd_can_network_connect 1
sudo dnf install -y policycoreutils-python-utils >/dev/null 2>&1 || true
sudo semanage fcontext -a -t httpd_sys_content_t "${CACCH_HOME}/ui/dist(/.*)?" 2>/dev/null || true
sudo restorecon -Rv "${CACCH_HOME}/ui/dist" >/dev/null 2>&1 || true

log "5/5 开机自启（Nginx）"
sudo systemctl enable --now nginx

log "===== 验证 ====="
nginx -v 2>&1
echo "Tomcat 链接：$(readlink ${CACCH_HOME}/tomcat)"
echo "Tomcat 真实目录：$(readlink -f ${CACCH_HOME}/tomcat)"
echo "Tomcat 属主：$(stat -c '%U:%G' $(readlink -f ${CACCH_HOME}/tomcat))"
su - felix -c "bash ${CACCH_HOME}/tomcat/bin/version.sh" 2>&1 | grep -E 'Server version|JVM Version'
echo "Nginx 状态：$(systemctl is-active nginx)"
echo "放行服务：$(sudo firewall-cmd --list-services)"
echo ""
echo "提示：Tomcat 的 systemd 托管在部署 cacch-tomcat.service 后执行"
echo "     sudo systemctl enable --now cacch-tomcat"

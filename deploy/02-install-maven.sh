#!/bin/bash
# ============================================================
# 阶段 2：Maven 3.9
# 前置：已执行 01-install-jdk.sh（Maven 依赖 JAVA_HOME）
# 目标主机：10.80.62.200 (Rocky Linux 9.6)  执行账号：felix（需 sudo）
# 说明：dnf 源仅提供 Maven 3.6，不满足 README 的 3.9+ 要求，故手动安装
#       阿里云 apache 镜像仅保留最新版，版本号需按实际可用版本调整（当前 3.9.16）
# 幂等：可重复执行
# ============================================================
set -euo pipefail

CACCH_HOME=/opt/cacch
MAVEN_VER=3.9.16
MIRROR=https://mirrors.aliyun.com/apache/maven
TARBALL=/tmp/apache-maven-${MAVEN_VER}-bin.tar.gz

log() { echo -e "\n\033[32m[$(date +%H:%M:%S)] $*\033[0m"; }

log "1/4 下载并解压 Maven ${MAVEN_VER}"
if [ -x "${CACCH_HOME}/maven/bin/mvn" ]; then
    echo "已安装，跳过"
else
    sudo mkdir -p "${CACCH_HOME}/maven"
    [ -f "${TARBALL}" ] || curl -sSL -o "${TARBALL}" \
        "${MIRROR}/maven-3/${MAVEN_VER}/binaries/apache-maven-${MAVEN_VER}-bin.tar.gz"
    sudo tar -xzf "${TARBALL}" -C "${CACCH_HOME}/maven" --strip-components=1
    sudo chown -R felix:felix "${CACCH_HOME}/maven"
fi

log "2/4 配置 MAVEN_HOME / PATH"
echo "export MAVEN_HOME=${CACCH_HOME}/maven" | sudo tee /etc/profile.d/maven.sh >/dev/null
echo 'export PATH=$MAVEN_HOME/bin:$PATH'      | sudo tee -a /etc/profile.d/maven.sh >/dev/null
sudo chmod +x /etc/profile.d/maven.sh

log "3/4 配置阿里云镜像（加速依赖下载）"
sudo cp "${CACCH_HOME}/maven/conf/settings.xml" "${CACCH_HOME}/maven/conf/settings.xml.bak" 2>/dev/null || true
sudo tee "${CACCH_HOME}/maven/conf/settings.xml" >/dev/null <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <localRepository>/home/felix/.m2/repository</localRepository>
  <mirrors>
    <mirror>
      <id>aliyun-public</id>
      <name>aliyun public</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
sudo chown -R felix:felix "${CACCH_HOME}/maven"

log "4/4 验证"
export MAVEN_HOME=${CACCH_HOME}/maven
export PATH=$MAVEN_HOME/bin:$PATH
mvn -v
echo "JAVA_HOME 在 Maven 中的识别：$(mvn -v 2>/dev/null | grep -i 'Java version' || echo '未识别，请检查 JAVA_HOME')"

log "===== 完成 ====="
echo "Maven 目录：${CACCH_HOME}/maven"
echo "本机仓库：/home/felix/.m2/repository"

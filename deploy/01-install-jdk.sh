#!/bin/bash
# ============================================================
# 阶段 1：基础环境 + JDK 21
# 目标主机：10.80.62.200 (Rocky Linux 9.6)  执行账号：felix（需 sudo）
# 幂等：可重复执行
# ============================================================
set -euo pipefail

CACCH_HOME=/opt/cacch
log() { echo -e "\n\033[32m[$(date +%H:%M:%S)] $*\033[0m"; }

log "1/5 补装缺失的基础命令（minimal 镜像缺 tar/unzip/wget/git）"
sudo dnf install -y tar gzip unzip wget curl vim

log "2/5 安装 JDK 21（devel，含 javac，服务器侧构建 War 需要）"
sudo dnf install -y java-21-openjdk-devel

log "3/5 配置 JAVA_HOME"
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk' | sudo tee /etc/profile.d/java.sh >/dev/null
echo 'export PATH=$JAVA_HOME/bin:$PATH' | sudo tee -a /etc/profile.d/java.sh >/dev/null
sudo chmod +x /etc/profile.d/java.sh

log "4/5 创建目录结构（权交 felix）"
# 注意：不创建 tomcat 目录——后续它是软链，若此处先建成真实目录，
# 03 脚本的 ln -sfn 会把链接建到目录内部
sudo mkdir -p "${CACCH_HOME}"/{config,ui/dist,src,logs}
sudo chown -R felix:felix "${CACCH_HOME}"

log "5/5 系统参数调优（内存仅 3.5G，抑制 swap）"
echo 'vm.swappiness=10' | sudo tee /etc/sysctl.d/99-cacch.conf >/dev/null
sudo sysctl -p /etc/sysctl.d/99-cacch.conf >/dev/null 2>&1 || true
echo -e 'felix soft nofile 65535\nfelix hard nofile 65535' | sudo tee /etc/security/limits.d/cacch.conf >/dev/null

log "===== 验证 ====="
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
java -version 2>&1
javac -version 2>&1
echo "JAVA_HOME=${JAVA_HOME}"
echo "目录：$(ls -ld ${CACCH_HOME})"
echo "swappiness=$(cat /proc/sys/vm/swappiness)"

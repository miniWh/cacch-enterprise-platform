#!/bin/bash
# ============================================================
# 阶段 6：构建前端并上传（在【本地】执行，非服务器）
#
# 前置：本地已装 Node.js 20+ 且 cacch-enterprise-ui/node_modules 已就绪
# 说明：服务器仅 2 核 3.5G，Vite build 在本地跑更快、不与 Tomcat 抢内存
#       执行过程中会提示输入 felix 的 SSH 密码（约 2~3 次）
# ============================================================
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
UI_DIR=$(cd "${SCRIPT_DIR}/../cacch-enterprise-ui" && pwd)
HOST=felix@10.80.62.200
REMOTE_DIR=/opt/cacch/ui/dist

log() { echo -e "\n\033[32m[$(date +%H:%M:%S)] $*\033[0m"; }

log "1/4 本地构建（npm run build → vite build）"
cd "${UI_DIR}"
npm run build

log "2/4 清理服务器旧产物"
ssh "${HOST}" "rm -rf ${REMOTE_DIR} && mkdir -p ${REMOTE_DIR}"

log "3/4 上传 dist"
scp -r dist/* "${HOST}:${REMOTE_DIR}/"

log "4/4 修正 SELinux 上下文（否则 Nginx 读取被拒）"
ssh "${HOST}" "sudo -S -p '' sh -c 'restorecon -Rv ${REMOTE_DIR}'" 2>&1 | tail -3

log "===== 完成 ====="
ssh "${HOST}" "echo '文件数：' \$(find ${REMOTE_DIR} -type f | wc -l); \
               echo '大小：' \$(du -sh ${REMOTE_DIR} | cut -f1)"
echo ""
echo "提示：此时浏览器仍访问不到 —— 还需执行阶段 7（Nginx 站点配置）"

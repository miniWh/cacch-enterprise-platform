#!/bin/bash
# ============================================================
# 阶段 7（可选路径）：【本地】构建后端 War 并上传部署到服务器
#
# 适用场景：日常改业务代码后发版，不想占用服务器 2 核 3.5G 的 CPU/内存
# 前提：本地有 JDK 21 + Maven 3.9+（会自动探测，无需配置 PATH）
# 说明：Skinny War 模式下 War 仅约 120KB（不含依赖），上传秒级完成
#       执行过程中会提示输入 felix 的 SSH 密码（约 3 次）
#
# 用法：
#   ./deploy/07-build-local-backend.sh            # 构建 + 上传 + 部署
#   ./deploy/07-build-local-backend.sh --build-only  # 只构建不上传
#
# 注意：若改动了 pom.xml 的依赖，请改在服务器执行 deploy.sh --full
#       （本脚本用 -Dmdep.skip=true 跳过依赖导出，不会同步 Tomcat lib）
# ============================================================
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
PROJECT_DIR=$(cd "${SCRIPT_DIR}/.." && pwd)
HOST=felix@10.80.62.200
REMOTE_STAGING=/opt/cacch/staging
BUILD_ONLY=false
[ "${1:-}" = "--build-only" ] && BUILD_ONLY=true

log() { echo -e "\n\033[32m[$(date +%H:%M:%S)] $*\033[0m"; }
die() { echo -e "\033[31m!! $*\033[0m"; exit 1; }

# ---------- 1. 探测本地工具链 ----------
log "1/5 探测本地 JDK / Maven"

if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/javac.exe" ]; then
    echo "JDK(来自 JAVA_HOME): ${JAVA_HOME}"
else
    JDK_CANDIDATE=$(ls -d /d/Software/Java/jdk21* /d/Software/Java/jdk-21* \
                       /c/Program\ Files/Java/jdk-21* /c/Program\ Files/Java/jdk21* 2>/dev/null | head -1)
    [ -n "${JDK_CANDIDATE}" ] || die "未找到 JDK 21，请先安装或设置 JAVA_HOME"
    export JAVA_HOME="D:${JDK_CANDIDATE#/d}" 2>/dev/null || export JAVA_HOME="${JDK_CANDIDATE}"
    # 统一转成 Windows 盘符格式，避免 Git Bash 路径无法被 java.exe 识别
    case "${JDK_CANDIDATE}" in
        /d/*) export JAVA_HOME="D:${JDK_CANDIDATE#/d}" ;;
        /c/*) export JAVA_HOME="C:${JDK_CANDIDATE#/c}" ;;
        *)    export JAVA_HOME="${JDK_CANDIDATE}" ;;
    esac
    echo "JDK(自动探测): ${JAVA_HOME}"
fi

MVN=""
if command -v mvn >/dev/null 2>&1; then
    MVN=$(command -v mvn)
else
    MVN=$(ls -d /d/Software/IntelliJ*/plugins/maven/lib/maven3/bin/mvn \
             /c/Program\ Files/JetBrains/IntelliJ*/plugins/maven/lib/maven3/bin/mvn 2>/dev/null | head -1)
fi
[ -n "${MVN}" ] || die "未找到 Maven，请先安装或加入 PATH"
echo "Maven: ${MVN}"

"${JAVA_HOME}/bin/java.exe" -version 2>&1 | head -1
"${MVN}" -v 2>&1 | head -1

# ---------- 2. 本地构建 ----------
log "2/5 本地构建（mvn clean package -DskipTests -Dmdep.skip=true）"
cd "${PROJECT_DIR}"
"${MVN}" clean package -DskipTests -Dmdep.skip=true -B 2>&1 | tail -25

WAR="${PROJECT_DIR}/cacch-enterprise-web/target/cacch-enterprise-web.war"
[ -f "${WAR}" ] || die "未找到 War 产物：${WAR}"

WAR_SIZE=$(du -h "${WAR}" | cut -f1)
JAR_IN_WAR=$(unzip -l "${WAR}" 2>/dev/null | grep -c 'WEB-INF/lib/.*\.jar' || echo 0)
echo ">> War：${WAR_SIZE}（内含依赖 jar：${JAR_IN_WAR} 个，Skinny War 生效）"

[ "${BUILD_ONLY}" = true ] && { log "已按 --build-only 跳过上传"; exit 0; }

# ---------- 3. 上传 ----------
log "3/5 上传到服务器 ${REMOTE_STAGING}"
ssh "${HOST}" "mkdir -p ${REMOTE_STAGING}"
scp "${SCRIPT_DIR}/install-war.sh" "${HOST}:/opt/cacch/install-war.sh"
scp "${WAR}" "${HOST}:${REMOTE_STAGING}/cacch-enterprise-web.war"
ssh "${HOST}" "chmod +x /opt/cacch/install-war.sh"

# ---------- 4. 远程安装 ----------
log "4/5 服务器端安装（备份 → 停 → 部署 → 启）"
ssh "${HOST}" "/opt/cacch/install-war.sh"

# ---------- 5. 验收 ----------
log "5/5 验收"
ssh "${HOST}" "echo '--- 服务状态 ---'; systemctl is-active cacch-tomcat; \
               echo '--- 首页 ---'; curl -s -o /dev/null -w '%{http_code}\n' --max-time 5 http://127.0.0.1/; \
               echo '--- 接口 ---'; curl -s -o /dev/null -w '%{http_code}\n' --max-time 5 http://127.0.0.1/api/v1/auth/captcha; \
               echo '--- 最近错误 ---'; grep -c ERROR /opt/cacch/tomcat/logs/cacch-enterprise.log 2>/dev/null || echo 0"

log "===== 完成 ====="
echo "回滚：ssh ${HOST} '/opt/cacch/rollback.sh list'"

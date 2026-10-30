#!/bin/bash
# ============================================================
# 回滚脚本（在服务器执行）
# 依赖 deploy.sh 发版时自动备份的 /opt/cacch/backups/ROOT.war.*
# 用法：
#   bash rollback.sh          # 回滚到最近一份备份
#   bash rollback.sh list     # 列出可用备份
#   bash rollback.sh <文件名>  # 回滚到指定备份
# ============================================================
set -euo pipefail

CACCH_HOME=/opt/cacch
TOMCAT=${CACCH_HOME}/tomcat
BACKUP_DIR=${CACCH_HOME}/backups

if [ "${1:-}" = "list" ]; then
    echo "可用备份（新→旧）："
    ls -1t "${BACKUP_DIR}"/ROOT.war.* 2>/dev/null || echo "  （无）"
    exit 0
fi

if [ -n "${1:-}" ]; then
    TARGET="${BACKUP_DIR}/$1"
    [ -f "$TARGET" ] || { echo "!! 备份不存在：$TARGET"; exit 1; }
else
    TARGET=$(ls -1t "${BACKUP_DIR}"/ROOT.war.* 2>/dev/null | head -1)
    [ -n "$TARGET" ] || { echo "!! 没有可用备份（$BACKUP_DIR）"; exit 1; }
fi

echo ">> 将回滚到：$TARGET  ($(du -h "$TARGET" | cut -f1))"
read -r -p "确认回滚？[y/N] " ans
[ "$ans" = "y" ] || { echo "已取消"; exit 0; }

echo ">> 停止应用"
if systemctl is-active --quiet cacch-tomcat; then
    sudo systemctl stop cacch-tomcat
else
    "${TOMCAT}/bin/shutdown.sh" >/dev/null 2>&1 || true
    sleep 3
fi

echo ">> 恢复旧包"
rm -rf "${TOMCAT}/webapps/ROOT" "${TOMCAT}/webapps/ROOT.war"
cp "$TARGET" "${TOMCAT}/webapps/ROOT.war"

echo ">> 启动应用"
sudo systemctl start cacch-tomcat

echo ">> 等待启动完成..."
for i in $(seq 1 90); do
    if curl -sf -o /dev/null --max-time 3 http://127.0.0.1:8080/api/v1/auth/captcha; then
        echo ">> 回滚完成并启动成功（耗时约 $((i*2))s）"
        exit 0
    fi
    sleep 2
done

echo "!! 启动超时，请检查日志：tail -f ${TOMCAT}/logs/catalina.out"
exit 1

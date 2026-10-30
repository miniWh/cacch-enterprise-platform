#!/bin/bash
# ============================================================
# 阶段 4：外置生产配置
# 前置：已执行 01~03
# 执行账号：felix（无需 sudo，/opt/cacch/config 已属 felix）
#
# 用法：
#   bash 04-config-prod.sh           # 首次生成；已存在则跳过
#   bash 04-config-prod.sh --force   # 强制重新生成（会更换 jwt-secret，
#                                    # 导致已登录用户 token 全部失效）
# ============================================================
set -euo pipefail

CACCH_HOME=/opt/cacch
CONF_DIR=${CACCH_HOME}/config
CONF=${CONF_DIR}/application-prod.yml

mkdir -p "${CONF_DIR}"

if [ -f "$CONF" ] && [ "${1:-}" != "--force" ]; then
    echo "配置已存在，跳过。如需重新生成密钥：bash $0 --force"
    echo "（注意：重新生成会导致已登录用户 token 全部失效）"
    exit 0
fi

# HS256 需 >=256bit；生成 48 字节(384bit)，兼容 HS256/HS384
SECRET=$(openssl rand -hex 48)

cat > "$CONF" <<EOF
# cacch-enterprise-platform 生产环境外置配置
# 生成时间：$(date '+%Y-%m-%d %H:%M:%S')
# 由 setenv.sh 的 -Dspring.config.location=/opt/cacch/config/ 覆盖 War 内同名文件
#
# 注意：Flyway 保持 false —— V1~V4 已在 10.80.86.93 执行完毕，prod 不操作表结构
spring:
  datasource:
    url: jdbc:postgresql://10.80.86.93:5432/cdb?currentSchema=enterprise
    username: esb
    password: esb
    driver-class-name: org.postgresql.Driver

  data:
    redis:
      host: 10.80.86.95
      port: 6379
      password: cacredis0826
      lettuce:
        pool:
          max-active: 16
          max-idle: 16
          min-idle: 4

  # 排除 Spring Boot 默认安全用户自动配置：
  # 本项目使用自研 JWT 过滤器 + 自定义 SecurityFilterChain，不依赖默认 UserDetailsService。
  # 该自动配置每次启动都会生成随机默认密码并打印 WARN 告警，排除后日志更干净
  # （已实测该默认账号无法通过 httpBasic 登录，仅属噪音）
  autoconfigure:
    exclude: org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

  flyway:
    enabled: false
    locations: classpath:db/migration

cacch:
  security:
    jwt-secret: ${SECRET}
    access-token-expire: 43200
    refresh-token-expire: 43200
    login-channel-policy: KICK_OUT
  cors:
    allowed-origins: http://10.80.62.200
EOF

chmod 600 "$CONF"

echo "已写入：$CONF"
echo "权限：$(stat -c '%a %U:%G' "$CONF")"
echo "jwt-secret 长度：$(echo -n "$SECRET" | wc -c) 字符（hex，即 48 字节 / 384 bit）"
echo ""
echo "jwt-secret = $SECRET"
echo ""
echo "⚠️ 请妥善保管此密钥：泄露即可伪造任意用户登录凭证；"
echo "   重新生成会使所有已登录用户的 token 失效。"

<a id="top"></a>

# cacch-enterprise-platform 运维手册

> 适用环境：生产主机 10.80.62.200（Rocky Linux 9.6）
> 编制日期：2026-10-30　版本：v1.0（基于部署全过程实测整理）
> 面向：值班 / 运维 / 后端开发。命令均可直接复制执行。

---

## 目录

- [一、系统概览](#sec-1)
  - [1.1 一句话架构](#sec-1-1)
  - [1.2 服务清单](#sec-1-2)
  - [1.3 主机资源（余量不多，需留意）](#sec-1-3)
  - [1.4 端口占用](#sec-1-4)
- [二、环境清单](#sec-2)
  - [2.1 登录与权限](#sec-2-1)
  - [2.2 软件安装位置](#sec-2-2)
  - [2.3 关键目录](#sec-2-3)
  - [2.4 敏感配置](#sec-2-4)
- [三、日常运维操作](#sec-3)
  - [3.1 服务启停](#sec-3-1)
  - [3.2 改了 Nginx 配置](#sec-3-2)
  - [3.3 查看日志](#sec-3-3)
  - [3.4 后端发版](#sec-3-4)
  - [3.5 后端发版（本地构建上传，推荐日常使用）](#sec-3-5)
  - [3.6 前端发版](#sec-3-6)
  - [3.7 回滚](#sec-3-7)
- [四、健康检查](#sec-4)
  - [4.1 一键体检](#sec-4-1)
  - [4.2 外部依赖连通性](#sec-4-2)
  - [4.3 资源监控](#sec-4-3)
  - [4.4 数据库状态](#sec-4-4)
- [五、故障排查手册](#sec-5)
  - [5.1 排查速查命令](#sec-5-1)
- [六、备份](#sec-6)
- [七、安全须知](#sec-7)
- [八、附录](#sec-8)
  - [8.1 本次部署踩过的坑（务必不要再犯）](#sec-8-1)
  - [8.2 变更记录](#sec-8-2)
  - [8.3 相关文档](#sec-8-3)

---

## 一、系统概览
<a id="sec-1"></a>

### 1.1 一句话架构
<a id="sec-1-1"></a>

```
浏览器 → Nginx:80 →┬─ 静态资源 /opt/cacch/ui/dist
                   ├─ /api/v1/keruyun/** → 集成平台 10.80.68.10:8081
                   └─ /api/**            → Tomcat 127.0.0.1:8080 → PostgreSQL + Redis
```

[↑ 返回顶部](#top)

### 1.2 服务清单
<a id="sec-1-2"></a>

| 角色 | 地址 / 标识 | 版本 | 归属 |
| --- | --- | --- | --- |
| 站点入口 | `http://10.80.62.200` | Nginx 1.20.1 | 本机 |
| 后端应用 | `127.0.0.1:8080`（**仅内网**） | Tomcat 11.0.27 + Spring Boot 4.0.9 | 本机 |
| 数据库 | `10.80.86.93:5432`，库 `cdb`，schema `enterprise` | **PostgreSQL 12.22** | 外部资产 |
| 缓存 | `10.80.86.95:6379` | Redis 6.2.14 | 外部资产 |
| 集成平台（客如云订单） | `10.80.68.10:8081` | — | 外部资产 |

[↑ 返回顶部](#top)

### 1.3 主机资源（**余量不多，需留意**）
<a id="sec-1-3"></a>

| 项 | 值 | 说明 |
| --- | --- | --- |
| CPU / 内存 | 2 核 / **3.5 Gi** | 属最低配，禁止再部署其他应用 |
| 磁盘 | `/` 62 G 可用、`/home` 31 G 可用 | 充足 |
| 时区 | Asia/Shanghai | 日志时间为 CST |
| 正常运行内存占用 | used ≈ 960 MiB / available ≈ 2.6 Gi | Tomcat RSS 约 375 MB |

[↑ 返回顶部](#top)

### 1.4 端口占用
<a id="sec-1-4"></a>

| 端口 | 绑定 | 用途 | 对外 |
| --- | --- | --- | --- |
| 22 | 0.0.0.0 | SSH | 是 |
| 80 | 0.0.0.0 | Nginx 站点 | 是 |
| 8080 | **127.0.0.1** | Tomcat HTTP | **否** |
| 8005 | **127.0.0.1** | Tomcat shutdown | **否** |

> 8080 不对公网暴露是刻意设计，所有外部请求必须经 Nginx。

[↑ 返回顶部](#top)

---

## 二、环境清单
<a id="sec-2"></a>

### 2.1 登录与权限
<a id="sec-2-1"></a>

```
主机：10.80.62.200
账号：felix（属 wheel，可 sudo，需输入 felix 的密码）
```

[↑ 返回顶部](#top)

### 2.2 软件安装位置
<a id="sec-2-2"></a>

| 组件 | 路径 | 备注 |
| --- | --- | --- |
| JDK | `/usr/lib/jvm/java-21-openjdk` | 符号链接，指向 `java-21-openjdk-21.0.12.1.1-*.x86_64` |
| Maven | `/opt/cacch/maven` | 3.9.16（dnf 源仅 3.6，故手动装） |
| Tomcat | `/opt/cacch/tomcat` → `/opt/cacch/apache-tomcat-11.0.27` | 软链 |
| Nginx 站点配置 | `/etc/nginx/conf.d/cacch.conf` | 主配置 `/etc/nginx/nginx.conf` 未改动 |
| systemd 单元 | `/etc/systemd/system/cacch-tomcat.service` | |

[↑ 返回顶部](#top)

### 2.3 关键目录
<a id="sec-2-3"></a>

```
/opt/cacch/
├── tomcat/                        Tomcat 根目录（软链）
│   ├── bin/setenv.sh              JVM 参数、profile、外置配置、LOG_HOME
│   ├── conf/server.xml            Connector 已限定 address=127.0.0.1
│   ├── lib/                       ★ Skinny War 外置依赖（146 个 jar）
│   ├── webapps/ROOT.war           ★ 应用包（仅约 120 KB）
│   └── logs/catalina.out          Tomcat 容器日志
├── config/application-prod.yml    ★ 外置生产配置（权限 600，含 jwt-secret）
├── ui/dist/                       前端静态资源
├── src/                           后端源码（服务器侧构建用）
├── logs/cacch-enterprise.log      ★ 应用业务日志
└── maven/                         Maven 3.9.16
```

[↑ 返回顶部](#top)

### 2.4 敏感配置
<a id="sec-2-4"></a>

`/opt/cacch/config/application-prod.yml`（权限 600）：

| 配置项 | 值 | 说明 |
| --- | --- | --- |
| `spring.datasource.url` | `jdbc:postgresql://10.80.86.93:5432/cdb?currentSchema=enterprise` | |
| `spring.datasource.username` | `esb` | |
| `spring.redis.host` | `10.80.86.95` | |
| `cacch.security.jwt-secret` | 96 字符 hex（384 bit） | **泄露即可伪造任意用户登录凭证** |
| `cacch.cors.allowed-origins` | `http://10.80.62.200` | |
| `spring.flyway.enabled` | `false` | prod 禁止自动改表结构 |
| `spring.autoconfigure.exclude` | `UserDetailsServiceAutoConfiguration` | 消除默认安全用户噪音 |

[↑ 返回顶部](#top)

---

## 三、日常运维操作
<a id="sec-3"></a>

### 3.1 服务启停
<a id="sec-3-1"></a>

```bash
# 查看状态
systemctl status cacch-tomcat
systemctl status nginx

# 启停 / 重启
sudo systemctl start   cacch-tomcat
sudo systemctl stop    cacch-tomcat
sudo systemctl restart cacch-tomcat      # 发版后常用

sudo systemctl restart nginx             # 改了 Nginx 配置后
```

两者均已 `enable`，开机自动启动。

> ⚠️ 不要用 `bin/startup.sh` / `bin/shutdown.sh` 手动启停了 —— 会脱离 systemd 管理，导致开机不自启、进程挂了不拉起。

[↑ 返回顶部](#top)

### 3.2 改了 Nginx 配置
<a id="sec-3-2"></a>

```bash
sudo nginx -t              # 必须先校验，通过再 reload
sudo systemctl reload nginx
```

[↑ 返回顶部](#top)

### 3.3 查看日志
<a id="sec-3-3"></a>

```bash
# 应用业务日志（最常用）
tail -f /opt/cacch/logs/cacch-enterprise.log

# Tomcat 容器日志（启动异常、部署失败看这里）
tail -f /opt/cacch/tomcat/logs/catalina.out

# Nginx 访问 / 错误日志
tail -f /var/log/nginx/cacch-access.log
tail -f /var/log/nginx/cacch-error.log

# systemd 视角
journalctl -u cacch-tomcat -f
```

常用过滤：

```bash
# 只看错误
grep -E 'ERROR|SEVERE|Exception' /opt/cacch/logs/cacch-enterprise.log | tail -50

# 看某次启动之后的日志（跳过历史）
awk '/Starting CacchEnterpriseApplication/{buf=""} {buf=buf $0 ORS} END{print buf}' \
    /opt/cacch/logs/cacch-enterprise.log

# 查登录相关
grep '【Auth' /opt/cacch/logs/cacch-enterprise.log | tail -20
```

[↑ 返回顶部](#top)

### 3.4 后端发版
<a id="sec-3-4"></a>

> **发版前先判断：这次改没改 `pom.xml` 的依赖？** 这是 Skinny War 模式下最容易踩的坑。

```bash
# A. 改了依赖（pom.xml 变更）—— 必须全量构建，同步依赖到 Tomcat lib
cd /opt/cacch/src
/opt/cacch/maven/bin/mvn clean package -DskipTests -Dtomcat.lib.dir=/opt/cacch/tomcat/lib

# B. 只改了业务代码 —— 跳过依赖导出，秒级出包
cd /opt/cacch/src
/opt/cacch/maven/bin/mvn clean package -DskipTests -Dmdep.skip=true

# 部署（脚本已封装，会自动改名 ROOT.war 并重启）
bash /opt/cacch/src/deploy/deploy.sh
```

产物约 **120 KB**（依赖已外置到 `tomcat/lib`），上传与打包都很快。

**手工部署等价步骤：**

```bash
sudo systemctl stop cacch-tomcat
cp /opt/cacch/src/cacch-enterprise-web/target/cacch-enterprise-web.war \
   /opt/cacch/tomcat/webapps/ROOT.war          # 必须改名 ROOT.war
sudo systemctl start cacch-tomcat
```

[↑ 返回顶部](#top)

### 3.5 后端发版（本地构建上传，推荐日常使用）
<a id="sec-3-5"></a>

后端同样可以**本地构建、只上传产物**，不占用服务器 2 核 3.5G 的 CPU/内存：

```bash
bash deploy/07-build-local-backend.sh                # 构建 → 上传 → 远程安装 → 验收
bash deploy/07-build-local-backend.sh --build-only   # 只构建，不上传
```

**前置条件**（脚本会自动探测，无需配置 PATH）：

- JDK 21：自动探测 `D:/Software/Java/jdk21*` 等常见位置，或读取 `JAVA_HOME`
- Maven 3.9+：优先用 PATH 里的 `mvn`，否则探测 IntelliJ IDEA 自带的
  `plugins/maven/lib/maven3/bin/mvn`
- 首次构建需下载依赖（约 40 秒，之后增量约 15 秒）
- 若本地无 `~/.m2/settings.xml`，建议配一份阿里云镜像，否则走 Maven Central 很慢：

```xml
<mirror>
  <id>aliyun-public</id>
  <url>https://maven.aliyun.com/repository/public</url>
  <mirrorOf>central</mirrorOf>
</mirror>
```

**流程**：本地 `mvn clean package -DskipTests -Dmdep.skip=true` → scp War（约 120 KB）到
`/opt/cacch/staging` → 远程执行 `/opt/cacch/install-war.sh`（备份 → 停 → 改名 ROOT.war → 启 → 校验）。

> ⚠️ 本地构建**跳过了依赖导出**（`-Dmdep.skip=true`），Skinny War 的依赖仍在服务器
> `/opt/cacch/tomcat/lib`。因此**改了 `pom.xml` 依赖时不要走本地构建**，请在服务器执行
> `deploy.sh --full` 重新同步依赖，否则会 `NoClassDefFoundError`。

[↑ 返回顶部](#top)

### 3.6 前端发版
<a id="sec-3-6"></a>

前端在**本地**构建（服务器只有 2 核，本地更快也不抢内存）：

```bash
bash deploy/06-deploy-frontend.sh     # 本地执行：build → scp → restorecon
```

手工等价：

```bash
cd cacch-enterprise-ui && npm run build
scp -r dist/* felix@10.80.62.200:/opt/cacch/ui/dist/
ssh felix@10.80.62.200 'sudo restorecon -R /opt/cacch/ui/dist'   # SELinux，必做
```

[↑ 返回顶部](#top)

### 3.7 回滚
<a id="sec-3-7"></a>

`deploy.sh` 发版时**会自动备份旧包**到 `/opt/cacch/backups/ROOT.war.<时间戳>`，保留最近 5 份。回滚走专用脚本：

```bash
bash deploy/rollback.sh list        # 列出可用备份（新→旧）
bash deploy/rollback.sh             # 回滚到最近一份（会二次确认）
bash deploy/rollback.sh ROOT.war.20261030_142000   # 回滚到指定备份
```

手工等价步骤：

```bash
sudo systemctl stop cacch-tomcat
rm -rf /opt/cacch/tomcat/webapps/ROOT /opt/cacch/tomcat/webapps/ROOT.war
cp /opt/cacch/backups/ROOT.war.<时间戳> /opt/cacch/tomcat/webapps/ROOT.war
sudo systemctl start cacch-tomcat
```

> ⚠️ 备份目录 `/opt/cacch/backups` 位于本机同盘。若需防范主机故障，重要版本请另行异地保存。

[↑ 返回顶部](#top)

---

## 四、健康检查
<a id="sec-4"></a>

### 4.1 一键体检
<a id="sec-4-1"></a>

```bash
# 服务状态
systemctl is-active cacch-tomcat nginx

# 站点与接口
curl -s -o /dev/null -w '首页    %{http_code}\n' http://10.80.62.200/
curl -s -o /dev/null -w '验证码  %{http_code}\n' http://10.80.62.200/api/v1/auth/captcha
curl -s -o /dev/null -w '鉴权    %{http_code}\n' http://10.80.62.200/api/v1/system/users
curl -s -o /dev/null -w '客如云  %{http_code}\n' http://10.80.62.200/api/v1/keruyun/orders
```

**预期：** 首页/验证码/客如云 = `200`；鉴权接口返回 `200` 且响应体为
`{"code":40100,"message":"未登录或登录已过期"}`（未登录被正确拦截）。

[↑ 返回顶部](#top)

### 4.2 外部依赖连通性
<a id="sec-4-2"></a>

```bash
# PostgreSQL
timeout 6 bash -c 'exec 3<>/dev/tcp/10.80.86.93/5432 && echo PG_OK' || echo PG_FAIL
PGPASSWORD='esb' psql -h 10.80.86.93 -U esb -d cdb -tAc 'select 1'

# Redis
redis-cli -h 10.80.86.95 -p 6379 -a 'cacredis0826' --no-auth-warning ping    # 期望 PONG

# 集成平台
timeout 6 bash -c 'exec 3<>/dev/tcp/10.80.68.10/8081 && echo KERUYUN_OK' || echo KERUYUN_FAIL
```

[↑ 返回顶部](#top)

### 4.3 资源监控
<a id="sec-4-3"></a>

```bash
free -h                          # 内存（available 低于 500M 需警惕）
df -h / /home                    # 磁盘
ss -lnt                          # 端口
systemctl show cacch-tomcat -p MainPID --value
ps -o rss,etime -p $(systemctl show cacch-tomcat -p MainPID --value)   # Tomcat 内存与运行时长
```

[↑ 返回顶部](#top)

### 4.4 数据库状态
<a id="sec-4-4"></a>

```bash
# Flyway 迁移历史（确认没有失败的迁移）
PGPASSWORD='esb' psql -h 10.80.86.93 -U esb -d cdb -tAc \
  "select installed_rank,version,description,success from enterprise.flyway_schema_history order by installed_rank"
```

> 生产环境 `spring.flyway.enabled=false`，**表结构变更不会自动执行**。Schema 变更须先在 test 环境验证，再人工在生产库执行。

[↑ 返回顶部](#top)

---

## 五、故障排查手册
<a id="sec-5"></a>

| # | 现象 | 可能原因 | 排查与处理 |
| --- | --- | --- | --- |
| 1 | 站点打不开 / 超时 | Nginx 挂了；防火墙 | `systemctl status nginx`；`sudo firewall-cmd --list-services`（应含 http）；`curl -I localhost` |
| 2 | **502 Bad Gateway** | Tomcat 未启动或启动失败 | `systemctl status cacch-tomcat`；看 `catalina.out` 尾部；常见于端口占用或 War 损坏 |
| 3 | 首页正常但接口 502 | Tomcat 崩了 / 正在重启 | `journalctl -u cacch-tomcat -n 50`；检查是否 OOM（`grep -i oom /var/log/messages`） |
| 4 | 页面白屏 | 前端资源 404 / SELinux 拦截 | F12 看 Network；`curl -I http://10.80.62.200/assets/index-*.js`；`sudo restorecon -R /opt/cacch/ui/dist` |
| 5 | 登录失败 | 密码错；Redis 不通（验证码存 Redis）；jwt-secret 变了 | 看 `cacch-enterprise.log` 中 `【Auth` 相关；`redis-cli ping` |
| 6 | 启动报 `NoClassDefFoundError` | **Skinny War 依赖未同步** | 改用全量构建：`mvn clean package -DskipTests -Dtomcat.lib.dir=/opt/cacch/tomcat/lib` |
| 7 | 启动报 Servlet / `NoSuchMethodError` | Tomcat 版本不对 | 必须是 **Tomcat 11**（Servlet 6.1）。10.1 只有 Servlet 6.0，不兼容 Boot 4 |
| 8 | 改了配置没生效 | 改的是 War 包内的配置 | 生产以外置配置为准：`/opt/cacch/config/application-prod.yml` |
| 9 | 日志里没有业务 INFO | 日志级别 / 路径 | 正常应为 `/opt/cacch/logs/cacch-enterprise.log`；确认进程带 `-DLOG_HOME=/opt/cacch/logs` |
| 10 | 磁盘写满 | 日志滚动失效 | `du -sh /opt/cacch/logs /opt/cacch/tomcat/logs`；logback 已配 30 天 / 10GB 上限 |
| 11 | 应用变慢 | 内存吃紧触发 swap | `free -h`、`vm.swappiness`（已设为 10）；考虑调小 `-Xmx` 或扩容 |
| 12 | 客如云订单页无数据 | 集成平台不可达 | 测 `10.80.68.10:8081` 连通性；看 Nginx error 日志 |
| 13 | **接口能访问，但 `systemctl status` 显示 `activating (auto-restart)` / `Result: exit-code`** | **出现了游离 JVM**：有人用 `startup.sh` 直接启动，绕开了 systemd。该进程占着 8080，systemd 每次自动重启都因端口冲突失败 | `ss -lntp \| grep 8080` 找到 PID；与 `systemctl show cacch-tomcat -p MainPID --value` 比对，不一致即为游离进程。`sudo systemctl stop cacch-tomcat` 后 `kill -9 <游离PID>`，确认 8080 释放再用 `systemctl start`。**永远只用 systemctl 启停** |
| 14 | `systemctl stop` 后进程仍在 | 服务单元缺少 `PIDFile`，systemd 杀错进程 | 确认 `cacch-tomcat.service` 含 `PIDFile=/opt/cacch/tomcat/tomcat.pid`（已修复），改后 `sudo systemctl daemon-reload` |

### 5.1 排查速查命令
<a id="sec-5-1"></a>

```bash
# 最近一次启动是否成功
awk '/Starting CacchEnterpriseApplication/{b=""} {b=b $0 ORS} END{print b}' \
    /opt/cacch/logs/cacch-enterprise.log | grep -E 'Started|ERROR|Exception'

# 端口与进程
ss -lntp | grep -E ':80|:8080|:8005'

# Nginx 最近错误
sudo tail -30 /var/log/nginx/cacch-error.log

# 确认 JVM 参数是否生效
PID=$(systemctl show cacch-tomcat -p MainPID --value)
tr '\0' '\n' < /proc/$PID/cmdline | grep -E 'Xmx|LOG_HOME|spring.profiles|spring.config'
```

[↑ 返回顶部](#top)

---

## 六、备份
<a id="sec-6"></a>

| 对象 | 位置 | 建议频率 | 方法 |
| --- | --- | --- | --- |
| 外置配置（含 jwt-secret） | `/opt/cacch/config/application-prod.yml` | 每次变更 | `cp` 到安全位置，**不要进 git** |
| 应用 War | `/opt/cacch/tomcat/webapps/ROOT.war` | 每次发版（**已自动**，保留 5 份） | `/opt/cacch/backups/`，见 3.6 |
| Nginx 配置 | `/etc/nginx/conf.d/cacch.conf` | 每次变更 | `cp` |
| systemd 单元 | `/etc/systemd/system/cacch-tomcat.service` | 每次变更 | `cp` |
| 前端 dist | `/opt/cacch/ui/dist` | 每次发版 | 源码在 git，可重新构建，可不备份 |
| 数据库 | `10.80.86.93` 的 `cdb` | 按 DBA 策略 | **不在本机**，需协调数据库侧 |

数据库备份示例（需在能连库的机器上执行）：

```bash
pg_dump -h 10.80.86.93 -U esb -n enterprise -Fc -f enterprise_$(date +%Y%m%d).dump cdb
```

[↑ 返回顶部](#top)

---

## 七、安全须知
<a id="sec-7"></a>

1. **jwt-secret 等同万能钥匙**：泄露即可伪造任意用户登录凭证。配置权限已是 600，切勿提交到代码仓库或发给无关人员。重新生成会使所有已登录用户 token 失效。
2. **8080 不得对外**：Connector 已限定 `address="127.0.0.1"`，修改 `server.xml` 时切勿删除该属性。
3. **数据库口令明文在配置中**：`application-prod.yml` 含 DB/Redis 明文口令，靠 600 权限保护。长期建议改环境变量注入。
4. **服务器账号**：felix 口令登录建议改为 SSH 密钥，并避免口令在文档/会话中留存。
5. **PostgreSQL 12.22 已 EOL**（2024-11 起停止官方支持）：当前为外部既有资产，长期需规划升级。
6. **SELinux 处于 Enforcing**：新增对外代理需 `setsebool -P httpd_can_network_connect 1`；新增静态目录需 `semanage fcontext` + `restorecon`。

[↑ 返回顶部](#top)

---

## 八、附录
<a id="sec-8"></a>

### 8.1 本次部署踩过的坑（务必不要再犯）
<a id="sec-8-1"></a>

1. **`ln -sfn` 目标是已存在目录时，链接会被建到目录内部**。建 Tomcat 软链必须用 `-T` 或先删目录。
2. **脚本整体以 `sudo` 执行时，产出文件归 root**，felix 无法部署。必须显式 `chown` 回 felix。
3. **阿里云 apache 镜像只保留最新版本**：Maven 3.9.11 / 3.9.10 / 3.9.9 / 3.9.6 均 404，只有 **3.9.16**。换版本前先探测目录。
4. **`spring.autoconfigure.exclude` 填错类名会直接导致启动失败**。Boot 4 的类路径是 `org.springframework.boot.security.autoconfigure.*`（与 Boot 3 不同），动手前先核实。
5. **systemd 默认工作目录是 `/`**，而 logback 日志路径是相对路径 —— 不设 `WorkingDirectory` 会导致日志写入失败。
6. **Nginx `proxy_pass` 带 URI 会剥离 `/api` 前缀**，而后端 Controller 路径自带 `/api`，会导致 404。本站点用 `ROOT.war` + 无 URI 的 `proxy_pass`。
7. **部署脚本绝不能 fallback 到 `startup.sh` 直启**。会拉起游离 JVM（PPID=1）占住 8080，systemd 之后的每次自动重启都因端口冲突失败，表现为"接口正常但服务在崩溃循环"。判断 systemd 是否托管要看 unit 文件是否存在，**不要**用 `systemctl list-unit-files | grep`（非交互环境下可能失败导致误判）。
8. **`Type=forking` 必须配 `PIDFile`**，否则 systemd 认错主进程，`stop` 只杀掉 startup.sh 的 shell，真正的 JVM 变孤儿存活。
9. **sudo 需要密码会让部署脚本卡住或静默走错分支**。已通过 `deploy/sudoers-cacch` 放开 felix 对 Tomcat 服务与 `restorecon` 的免密（限定命令，非 ALL）。

[↑ 返回顶部](#top)

### 8.2 变更记录
<a id="sec-8-2"></a>

| 日期 | 内容 |
| --- | --- |
| 2026-10-30 | 完成 8 阶段部署上线（JDK21 / Maven3.9.16 / Tomcat11.0.27 / Nginx1.20.1） |
| 2026-10-30 | 修正 README 版本（Spring Boot 4.0.9、Tomcat 11.x） |
| 2026-10-30 | 启用 Skinny War（War 120 KB，依赖外置 146 个 jar） |
| 2026-10-30 | 修复 prod 日志级别与路径；消除默认安全密码 WARN；前端 manualChunks 分包 |
| 2026-10-30 | 新增本地构建上传发版：`deploy/07-build-local-backend.sh` + `deploy/install-war.sh` |
| 2026-10-30 | 修复游离 JVM 导致的重启风暴：service 补 `PIDFile`、加 `StartLimitBurst`；发版脚本强制走 systemd 并做端口预检 |
| 2026-10-30 | 配置限定命令的免密 sudo：`deploy/sudoers-cacch` |

[↑ 返回顶部](#top)

### 8.3 相关文档
<a id="sec-8-3"></a>

- 部署方案：`docs/部署方案-10.80.62.200.md`
- 部署脚本：`deploy/01` ~ `06`
- 本地构建上传发版：`deploy/07-build-local-backend.sh`（本地）+ `deploy/install-war.sh`（服务器）
- 免密 sudo 配置：`deploy/sudoers-cacch`
- 日常发版：`deploy/deploy.sh`（含自动备份）
- 回滚：`deploy/rollback.sh`
- 项目说明：`README.md`

[↑ 返回顶部](#top)

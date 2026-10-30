# cacch-enterprise-platform 企业管理平台

## 一、项目概述
cacch-enterprise-platform 是 Cacch 公司统一的企业级管理后台基座，基于 Spring Boot 4 技术栈构建，采用**模块化单体架构**设计：当前以后端单工程 + 前端单仓方式启动部署，降低开发与运维成本；模块边界严格划分、依赖单向可控，后续业务扩张时可快速垂直拆分为微服务架构，无需大规模代码重构。

平台作为企业管理的**统一入口**，对内承载账号认证、组织架构、权限角色、用户中心、消息通知、日志审计与工作台等基础管理能力；对外为各业务系统（含 cacch-integration-platform 集成中台及后续业务线）提供统一身份认证与用户组织数据来源，避免各系统重复建设账号与权限体系。前端采用 Vue 3 单页应用，所有管理功能在同一工作台内完成。

## 二、技术栈总览
| 组件 | 版本 | 说明 |
| --- | --- | --- |
| Java | 21 LTS | 推荐 Amazon Corretto 21 / Eclipse Temurin 21 |
| Spring Boot | 4.0.9 | 基于 Jakarta EE 11，核心业务框架 |
| Spring Security | 6.x | 认证与权限控制框架，配合 JWT 实现无状态鉴权 |
| MyBatis-Plus | 3.5.16 | 持久层增强框架，简化 CRUD 开发 |
| PostgreSQL | 12.22 | 主数据库，x86_64 Linux 环境，GCC 4.8.5 编译 |
| Flyway | 12.4.0 | 数据库 Schema 版本化迁移工具 |
| Redis | Lettuce 客户端 | 分布式缓存，支撑登录态、验证码、字典、菜单、限流计数 |
| Caffeine | 3.1.8 | JVM 堆内内存缓存，承载字典、参数、菜单树等高频只读数据 |
| WebSocket | Spring 内置 | 站内信、公告、待办变更的实时推送 |
| MapStruct | 1.6.3 | 编译期对象转换工具，实现 DO ↔ DTO 高性能转换 |
| BCrypt | at.favre.lib 0.10.2 | 用户密码哈希存储 |
| Hutool | 5.8.x | 通用工具集，简化日期、字符串、加密等重复代码 |
| PostgreSQL JDBC | 42.7.7 | PostgreSQL JDBC 驱动 |
| Apache Tomcat | 11.0.x | 生产运行时 Servlet 容器（Jakarta EE 11 / Servlet 6.1），外部独立部署；本地开发仍用内嵌容器，无需额外安装 |
| Lombok | 1.18.36 | 编译期代码简化工具 |
| Vue | 3.5.x | 前端框架，组合式 API |
| Vite | 6.x | 前端构建与开发服务器 |
| Element Plus | 2.x | 前端 UI 组件库 |
| Pinia | 2.x | 前端状态管理 |
| TailwindCSS | 3.x | 原子化 CSS，配合 Element Plus 做布局与主题定制 |

## 三、环境要求
### 3.1 开发环境
- JDK：21 LTS 及以上
- 构建工具：Maven 3.9+
- Node.js：20 LTS 及以上，包管理器 pnpm 9+
- IDE：IntelliJ IDEA 2023+（需安装 Lombok、MapStruct 插件）；前端推荐 VS Code（需安装 Volar、ESLint 插件）
- 本地辅助环境：
    - PostgreSQL 12.22
    - Redis 6.x / 7.x

### 3.2 运行环境
- 操作系统：Linux x86_64（推荐 CentOS 7+ / Rocky Linux 9）
- JRE：21 LTS
- Servlet 容器：Apache Tomcat 11.0.x（独立部署，War 包方式），同一 Tomcat 可挂载多个应用，避免每应用启动一个 JVM 进程
- 数据库：PostgreSQL 12.22
- 缓存：Redis 6.x 及以上，支持集群模式
- 前端静态资源：Nginx 1.20+，用于托管打包后的 SPA 并反向代理后端接口

## 四、项目结构
### 4.1 模块划分原则
采用**分层 + 领域**结合的模块化设计，模块职责单一、依赖单向清晰：
- 后端横向按技术分层：公共层、持久层、业务层、编排层、接入层、启动层
- 前端为独立工程，通过 HTTP + JWT 与后端交互，不共享后端代码
- 纵向按业务域隔离，后续拆分微服务时，可按业务域直接抽离对应代码形成独立服务
- 所有后端模块统一继承父工程，依赖版本全局管控，避免版本冲突

### 4.2 模块职责说明
| 模块名称 | 模块类型 | 核心职责 |
|---------|---------|---------|
| cacch-enterprise-platform | 父工程（POM） | 统一管理所有后端子模块依赖版本、打包规则，不包含业务代码 |
| cacch-enterprise-common | 公共基础模块 | 全局常量、通用工具类、统一异常、统一返回体、通用注解、基础 DTO、分页对象，无业务逻辑，所有模块的底层依赖 |
| cacch-enterprise-dao | 数据持久模块 | 数据库实体 DO、Mapper 接口、MyBatis-Plus 配置、数据权限拦截器，仅负责数据读写 |
| cacch-enterprise-service | 单聚合业务模块 | 单一业务域内的核心业务逻辑，仅操作对应聚合根数据，不做跨域流程编排 |
| cacch-enterprise-manager | 业务编排模块 | 跨聚合流程组装、全局事务控制、多服务聚合查询（如登录时同时校验用户、角色、权限、部门），是复杂业务的入口 |
| cacch-enterprise-integration | 第三方适配模块 | 企业微信/钉钉/飞书等身份源客户端封装、扫码登录、组织同步适配，屏蔽外部系统协议差异 |
| cacch-enterprise-async | 异步处理模块 | 登录日志落库、操作日志异步写入、站内信推送、报表类定时任务，解耦主流程与异步逻辑 |
| cacch-enterprise-web | Web 启动模块 | Controller 控制层、Spring Security 配置、WebSocket 配置、配置文件、项目启动类；打包为 War 部署到外部 Tomcat，是单体模式下的唯一部署单元 |
| cacch-enterprise-ui | 前端工程 | Vue 3 + Vite + Element Plus SPA，包含登录页、工作台、系统管理、组织管理、消息中心等页面 |

### 4.3 完整工程目录
```
cacch-enterprise-platform                    # 父工程根目录
├── pom.xml                                 # 父POM：统一依赖管理
├── cacch-enterprise-common                  # 公共基础模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/enterprise/common
│               ├── constant                # 按业务域拆分（如 constant/system、constant/redis）
│               ├── config                   # 按业务域拆分的配置 POJO（如 config/security）
│               ├── utils                    # 通用工具类（密码、JWT、IP、日期）
│               ├── exception               # 统一异常定义
│               ├── result                  # 统一返回封装、分页结果
│               └── annotation               # 通用注解（操作日志、数据权限、限流）
├── cacch-enterprise-dao                    # 数据持久模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/enterprise
│               ├── entity/{biz}            # 按业务域划分的数据库实体 DO
│               │                           # biz 取值：system / auth / org / notice / log
│               ├── mapper/{biz}             # 按业务域划分的 Mapper 接口
│               └── config                  # 数据源、MyBatis-Plus、数据权限拦截器配置
├── cacch-enterprise-service                 # 单聚合业务模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/enterprise/service
│               └── {biz}/api               # 按业务域划分（如 system/api、org/api）
│                   └── impl                # 接口实现类
├── cacch-enterprise-manager                 # 业务编排模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/enterprise/manager
│               └── {biz}/api               # 按业务域划分（如 auth/api）
│                   └── impl                # 跨域编排、登录流程、事务控制实现
├── cacch-enterprise-integration             # 第三方身份源适配模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/enterprise/integration
│               ├── wecom/client            # 企业微信客户端
│               ├── dingtalk/client         # 钉钉客户端
│               └── config                  # RestTemplate、WebClient 配置
├── cacch-enterprise-async                   # 异步处理模块
│   ├── pom.xml
│   └── src
│       └── main
│           └── java/com/cacch/enterprise/async
│               └── {biz}                   # 按业务域划分
│                   ├── task                # 定时任务（如 token 过期清理、在线用户统计）
│                   └── listener            # Spring 事件监听（操作日志、登录日志、站内信）
├── cacch-enterprise-web                     # Web 启动模块（后端单体启动入口）
│   ├── pom.xml
│   └── src
│       ├── main
│       │   ├── java/com/cacch/enterprise
│       │   │   ├── CacchEnterpriseApplication.java   # 启动类，需继承 SpringBootServletInitializer 以支持外部 Tomcat War 部署
│       │   │   ├── controller/{biz}       # 按业务域划分（如 controller/system、controller/auth）
│       │   │   ├── convert/{biz}           # MapStruct 对象转换
│       │   │   ├── dto/{biz}/vo            # 请求/响应 VO
│       │   │   ├── config/{biz}           # 业务域配置注册
│       │   │   ├── exception               # 全局异常处理
│       │   │   ├── security                # Spring Security：JWT 过滤器、用户详情、权限判定
│       │   │   └── websocket               # WebSocket 端点与会话管理
│       │   └── resources
│       │       ├── application.yml          # 主配置文件
│       │       ├── application-dev.yml     # 开发环境配置
│       │       ├── application-test.yml    # 测试环境配置
│       │       ├── application-prod.yml    # 生产环境配置
│       │       └── db/migration            # Flyway 数据库迁移脚本
│       │           └── V1__init_schema.sql
│       └── test                            # 单元测试、集成测试
└── cacch-enterprise-ui                      # 前端工程（独立 Vite 工程，不属于 Maven 构建）
    ├── package.json
    ├── pnpm-lock.yaml
    ├── vite.config.ts
    ├── tailwind.config.js
    └── src
        ├── main.ts                         # 应用入口
        ├── api                             # 按业务域拆分的接口请求封装（axios）
        ├── assets                          # 静态资源、主题变量
        ├── components                      # 全局通用组件
        ├── layout                          # 后台布局（侧边栏、顶栏、标签页）
        ├── router                          # 路由表，按权限动态注册
        ├── stores                          # Pinia：用户、权限、字典、应用配置
        ├── views
        │   ├── login                       # 登录、验证码、扫码登录
        │   ├── workbench                   # 工作台 / Dashboard
        │   ├── system                      # 用户、角色、菜单、部门、字典、参数、日志
        │   ├── org                         # 组织架构、岗位、员工
        │   └── notice                      # 通知公告、站内信
        └── utils                          # 请求封装、权限指令、格式化工具
```

### 4.4 模块依赖规则
- 依赖流向严格单向，禁止循环依赖：`web → manager → service → dao → common`
- `integration` 模块依赖 `common`，被 `manager` / `service` 按需依赖
- `async` 模块依赖 `service` / `manager`，由 `web` 模块统一加载启动
- 下层模块不得反向依赖上层模块，例如 `service` 不能依赖 `manager`，`dao` 不能依赖 `service`
- 前端 `cacch-enterprise-ui` 不参与 Maven 构建，独立打包后由 Nginx 托管，通过 `/api/**` 反向代理访问后端
- 后续拆分微服务时，按业务域拆分对应 `dao/service/manager/controller`，复用 `common` 与基础配置，即可快速形成独立服务

### 4.5 命名规范
- **父工程 groupId**：`com.cacch`
- **所有子模块 artifactId**：统一前缀 `cacch-enterprise-` + 模块功能定位
- **顶层包名**：`com.cacch.enterprise`，各模块内按业务域 `{biz}` 划分子包，业务域取值固定为：`system`（系统管理）、`auth`（认证授权）、`org`（组织架构）、`notice`（消息通知）、`log`（日志审计）
- 数据库表名：统一前缀 `sys_`，全小写，下划线分隔，例如 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_dict`、`sys_notice`
- Flyway 脚本：`V{版本号}__{描述}.sql`，版本号递增，禁止修改已执行脚本
- 前端目录：`src/views/{biz}/` 与后端业务域保持同名，接口文件 `src/api/{biz}.ts`

## 五、快速开始
### 5.1 环境准备
1. 本地安装 JDK 21 与 Maven 3.9+，配置环境变量
2. 本地安装 Node.js 20 LTS 与 pnpm 9+
3. 启动 PostgreSQL 12.22，创建数据库 `cacch_enterprise`
4. 启动 Redis 服务

### 5.2 后端配置修改
修改 `cacch-enterprise-web` 模块下 `application-dev.yml` 核心配置：
```yaml
# 数据源配置
spring:
  datasource:
    url: jdbc:postgresql://127.0.0.1:5432/cacch_enterprise
    username: your_username
    password: your_password

  # Redis配置
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: your_redis_password

  # Flyway：仅 test profile 开启，prod 禁止自动迁移（见 application-test.yml / application-prod.yml）
  flyway:
    enabled: false
    locations: classpath:db/migration

# 应用自身配置
cacch:
  security:
    jwt-secret: your_jwt_secret_at_least_32_bytes
    access-token-expire: 7200        # 访问令牌有效期（秒）
    refresh-token-expire: 604800      # 刷新令牌有效期（秒）
  cors:
    allowed-origins: http://127.0.0.1:5173
```

### 5.3 启动后端（本地开发）
1. 在父工程根目录执行全量编译：
```bash
mvn clean compile
```
2. 运行 `cacch-enterprise-web` 模块下的启动类 `CacchEnterpriseApplication`（本地 main 方法启动，使用内嵌容器，无需本地安装 Tomcat）
3. 启动成功后访问健康检查地址：`http://127.0.0.1:8080/actuator/health`

> 生产环境走外部 Tomcat War 部署，详见第八章。

### 5.4 启动前端
```bash
cd cacch-enterprise-ui
pnpm install
pnpm dev
```
默认访问地址：`http://127.0.0.1:5173`，Vite 已配置 `/api` 代理到后端 `http://127.0.0.1:8080`。

## 六、核心能力模块
1. **统一认证与登录体系**
   支持账号密码登录、图形验证码、登录失败次数锁定、JWT 双令牌（access/refresh）机制，后续可平滑扩展企业微信/钉钉扫码登录。登录态存 Redis，支持强制下线与多端登录策略配置。

2. **RBAC 权限模型**
   用户—角色—菜单—按钮四层授权，菜单与按钮权限以权限标识字符串（如 `system:user:add`）下发给前端，前端路由动态注册、按钮级指令鉴权；同时支持数据权限（全部 / 本部门 / 本部门及下级 / 仅本人），由 MyBatis-Plus 拦截器自动拼接 SQL 条件。

3. **组织架构中心**
   公司—部门—岗位—员工四级树形结构，支持部门排序、负责人、启用/停用；员工与系统用户为一对一关系，组织变更通过服务层事务保证一致性。

4. **用户与账号中心**
   用户 CRUD、重置密码（BCrypt 哈希）、启用/禁用、分配角色、分配部门；初始密码与首次登录强制改密策略可配置。

5. **消息通知中心**
   站内信 + 公告两种类型，基于 WebSocket 向前端实时推送未读消息；历史消息落库，支持已读/未读、按类型筛选。

6. **操作日志与登录审计**
   通过 `@OperationLog` 注解 + Spring 事件机制异步记录操作人、IP、请求参数、耗时、结果；登录日志独立存储，支撑安全审计与异常登录排查。

7. **字典与参数配置**
   数据字典、系统参数两级配置，启动时加载至 Caffeine，变更时主动失效缓存，避免高频数据库查询。

8. **工作台 Dashboard**
   首页聚合展示待办、未读消息、快捷入口与关键统计卡片，统计数据走 Caffeine + Redis 两级缓存。

9. **数据库版本管控**
   Flyway 管理 Schema 变更；**test/prod 共用同一数据库，仅 test 环境启动时执行迁移**，prod 启动不操作数据库结构。

## 七、开发规范

> 详细 AI 编码约束与分层禁止项见项目根目录 [`.cursorrules`](.cursorrules)。

### 7.1 代码分层规范
采用「Controller → Manager → Service → Mapper」四层架构，各层职责严格隔离，禁止越界：
- **Controller 层**：仅做参数校验、请求转发、结果统一封装，不包含业务逻辑与流程编排；权限校验通过 `@PreAuthorize` 或自定义权限注解完成
- **Manager 层**：业务编排入口，负责跨聚合流程组装、全局事务控制、多服务结果聚合查询（如登录、导出、批量授权）；不直接操作数据库，不编写单表 CRUD 逻辑
- **Service 层**：仅承载**单聚合内**的核心业务逻辑，处理单一业务域的数据操作，不做跨域流程编排，不控制跨服务事务
- **Mapper 层**：仅做数据库交互，禁止拼接业务逻辑；数据权限条件必须走拦截器，不得在 XML 中手写部门过滤 SQL
- 所有 DO 与 DTO 转换必须通过 MapStruct 实现，禁止手动 setter/getter 堆砌
- 新代码统一使用 Lombok `@Data` / `@Builder`，避免手工堆砌 getter/setter；构造方法与内部类按需使用，不做 JDK 版本降级约束（本项目基线为 Java 21）

### 7.2 模块开发规范
1. 新增业务功能优先按领域归类到对应模块，不得跨模块随意放置代码
2. 模块间调用必须通过接口层交互，禁止直接依赖实现类，便于后续拆分与替换
3. 公共能力必须下沉到 `common` 模块，禁止在各业务模块重复实现通用逻辑
4. 第三方身份源对接逻辑必须收敛在 `integration` 模块，业务层不得直接调用企业微信/钉钉 SDK
5. 前端新增页面必须在 `src/views/{biz}/` 下按业务域归类，接口文件与页面同名，路由 meta 必须配置权限标识

### 7.3 数据库开发规范
1. 所有表必须包含主键 `id`、创建时间 `create_time`、更新时间 `update_time`、创建人、更新人、逻辑删除标记 `deleted`
2. 禁止直接修改数据库结构，所有变更必须通过 Flyway 脚本提交
3. 表别名采用除前缀外的英文首字母拼接规则（如 `sys_user` 别名 `u`、`sys_role` 别名 `r`），禁止无意义别名
4. PostgreSQL 12 兼容约束：
    - 禁止使用 `MERGE` 语法，幂等写入统一使用 `INSERT ... ON CONFLICT`
    - 分区表避免复杂嵌套设计，减少运维复杂度
    - 大表需提前配置 autovacuum 参数，定期检查表膨胀情况

### 7.4 安全规范
- 用户密码必须 BCrypt 哈希存储，禁止明文或可逆加密
- JWT 密钥、短信密钥等敏感信息禁止硬编码，通过配置文件或配置中心注入
- 所有业务接口必须经过 Spring Security 鉴权， permitAll 仅放行登录、验证码、健康检查等白名单
- 登录、导出、批量操作等接口必须做限流与参数合法性校验
- 操作日志中对密码、身份证、手机号等敏感字段必须做脱敏后再落库
- 前端路由按权限动态注册，后端接口权限必须二次校验，不得仅依赖前端隐藏按钮

## 八、部署与运维
### 8.1 后端打包（War 包）
`cacch-enterprise-web` 模块 `pom.xml` 中打包方式声明为 `war`，并将内嵌 Tomcat 依赖设为 `provided`（不打进 War，由外部 Tomcat 提供）：
```xml
<packaging>war</packaging>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

启动类需继承 `SpringBootServletInitializer`：
```java
public class CacchEnterpriseApplication extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(CacchEnterpriseApplication.class);
    }
    public static void main(String[] args) {
        SpringApplication.run(CacchEnterpriseApplication.class, args);
    }
}
```

在父工程根目录执行打包：
```bash
mvn clean package -DskipTests
```
产物路径：`cacch-enterprise-web/target/cacch-enterprise-web.war`

相比每次打 Fat Jar，War 包瘦身后仅含业务类与第三方依赖，不内嵌整个 Tomcat 运行时；多应用共用同一 Tomcat 进程，JVM 启动与类加载只做一次，迭代发版只需替换 War 包，重启单个应用上下文即可，无需重启整台 JVM。

### 8.2 前端打包
```bash
cd cacch-enterprise-ui
pnpm build
```
产物路径：`cacch-enterprise-ui/dist/`，整体拷贝至 Nginx 静态资源目录。

### 8.3 部署与配置外置
1. 将 War 包拷贝至 Tomcat 的 `webapps/` 目录，默认 context path 为 `cacch-enterprise-web`（如需根路径访问，可将 War 包重命名为 `ROOT.war`）
2. 通过 Tomcat 的 `bin/setenv.sh` 外置配置与 JVM 参数，避免修改 War 包内配置：
```bash
#!/bin/sh
# $CATALINA_HOME/bin/setenv.sh
export JAVA_OPTS="$JAVA_OPTS \
  -Dspring.profiles.active=prod \
  -Dspring.config.location=/opt/cacch/config/ \
  -Xms1g -Xmx2g \
  -Dfile.encoding=UTF-8"
```
3. 启动 Tomcat：
```bash
$CATALINA_HOME/bin/startup.sh        # 启动
$CATALINA_HOME/bin/shutdown.sh       # 停止
```
4. 发版时只需替换 `webapps/cacch-enterprise-web.war` 并删除解压目录，再执行一次 `shutdown.sh && startup.sh`；日常小改动可直接删除 `webapps/cacch-enterprise-web/` 目录让 Tomcat 重新解包，无需重新打包 Jar。

外置配置目录 `/opt/cacch/config/` 下放置 `application-prod.yml`，与 War 包内同名配置互补覆盖。

Nginx 关键配置示例（后端 context path 为 `/cacch-enterprise-web`）：
```nginx
server {
    listen 80;
    root /opt/cacch/ui/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端接口：Nginx 剥离 /api/ 前缀，转发到 Tomcat 的应用上下文
    location /api/ {
        proxy_pass http://127.0.0.1:8080/cacch-enterprise-web/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 60s;
    }

    # WebSocket：注意 context path 与 Upgrade 头
    location /ws/ {
        proxy_pass http://127.0.0.1:8080/cacch-enterprise-web/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

### 8.4 运维注意事项
1. **数据库运维**：PostgreSQL 12 无官方安全补丁，需严格限制数据库访问白名单，定期执行 vacuum 分析，监控表膨胀率；日志类大表（操作日志、登录日志）按月分表或定期归档。
2. **缓存运维**：Redis 需配置持久化策略，登录态、验证码、限流 key 设置合理过期时间，避免缓存穿透与雪崩；字典、菜单缓存变更后必须主动 evict。
3. **版本升级**：Schema 变更脚本仅在 **test profile 启动**时由 Flyway 执行；prod 部署前须确认 test 已完成迁移，prod 启动不会再次执行 SQL。
4. **Tomcat 运维**：关闭 `webapps/` 下应用的自动 JSP 编译与热部署（`Host` 标签 `reloadable="false"`），避免请求高峰时重复解包；多应用共用 Tomcat 时，通过 `setenv.sh` 统一堆大小，单个应用内存溢出不应拖垮整台 JVM。
5. **WebSocket 运维**：多实例部署时需接入 Redis Pub/Sub 或 Sticky Session，保证站内信广播在所有节点可达。

## 九、可选扩展增强
根据项目迭代需要，可按需引入以下主流组件提升可维护性与稳定性：
- **服务容错**：Resilience4j + Spring Retry，实现第三方登录源调用的熔断、降级、重试
- **可观测性**：Spring Boot Actuator + Micrometer + SkyWalking，实现指标监控、链路追踪
- **接口文档**：SpringDoc OpenAPI 3 + Knife4j，自动生成后台接口文档，降低前后端联调成本
- **工作流引擎**：集成 Flowable / Camunda，承载审批、发文、用印等企业内部流程
- **数据报表**：集成 EasyExcel 实现用户、日志、组织数据的批量导入导出
- **微服务演进**：业务扩张时引入 Spring Cloud 相关组件，将认证中心、组织中心拆分为独立微服务，前端统一通过网关访问

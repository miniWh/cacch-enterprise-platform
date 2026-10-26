---
description: cacch-enterprise-platform 项目总规范（架构、编码、日志、事务、Git 提交等）
alwaysApply: true
---

# cacch-enterprise-platform 项目规则
# 适用：Spring Boot 4 + Java 21 模块化单体企业管理平台（后端 + Vue 3 前端）

---

## 一、项目上下文

你正在开发 **cacch-enterprise-platform**，这是 Cacch 公司统一的企业级管理后台基座，基于 Spring Boot 4 构建，采用模块化单体架构：后端单工程 + 前端单仓（`cacch-enterprise-ui`），当前以单项目方式部署，后续可按业务域垂直拆分为微服务。

核心目标：对内承载**账号认证、组织架构、权限角色、用户中心、消息通知、日志审计与工作台**等基础管理能力；对外为各业务系统（含 cacch-integration-platform 集成中台及后续业务线）提供统一身份认证与用户组织数据来源，避免各系统重复建设账号与权限体系。

---

## 二、技术栈约束

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 21 LTS | 推荐 Amazon Corretto 21 / Eclipse Temurin 21 |
| Spring Boot | 4.0.6 | 基于 Jakarta EE 11，核心业务框架 |
| Spring Security | 6.x | 认证与权限控制框架，配合 JWT 实现无状态鉴权 |
| MyBatis-Plus | 3.5.16 | 持久层增强框架 |
| PostgreSQL | 12.22 | 主数据库 |
| Flyway | 12.4.0 | Schema 版本化迁移 |
| Redis | Lettuce 客户端 | 分布式缓存，支撑登录态、验证码、字典、菜单、限流计数 |
| Caffeine | 3.1.8 | JVM 堆内缓存，承载字典、参数、菜单树等高频只读数据 |
| WebSocket | Spring 内置 | 站内信、公告、待办变更的实时推送 |
| MapStruct | 1.6.3 | 编译期对象转换 |
| BCrypt | at.favre.lib 0.10.2 | 用户密码安全哈希 |
| Hutool | 5.8.x | 通用工具集，简化日期、字符串、加密等重复代码 |
| PostgreSQL JDBC | 42.7.7 | PostgreSQL JDBC 驱动 |
| Apache Tomcat | 10.1.x | 生产运行时 Servlet 容器（外部独立部署 War 包）；本地开发用内嵌容器 |
| Lombok | 1.18.36 | 编译期代码简化 |
| 前端栈 | Vue 3.5.x / Vite 6.x / Element Plus 2.x / Pinia 2.x / TailwindCSS 3.x | 前端独立工程 `cacch-enterprise-ui` |

- 所有依赖版本由父 POM 统一管控，**不得在子模块 pom.xml 中自行指定版本号**（父 POM 已声明的依赖）。
- 禁止引入父 POM 未声明的依赖，如需新增依赖，先在父 POM 的 `<dependencyManagement>` 中声明版本，再在子模块中引用。

---

## 三、模块架构规则

### 3.1 模块职责边界（严格遵守，不得越界）

| 模块 | 职责 | 禁止事项 |
|------|------|----------|
| `cacch-enterprise-common` | 全局常量、工具类、统一异常、统一返回体、通用注解、基础 DTO、分页对象、**可被多模块引用的配置 POJO** | 不得包含任何业务逻辑；不得依赖其他子模块 |
| `cacch-enterprise-dao` | 数据库实体 DO、Mapper 接口、MyBatis-Plus 配置、数据权限拦截器 | 仅做数据读写；禁止拼接业务逻辑；不得依赖 service/manager |
| `cacch-enterprise-service` | 单聚合内核心业务逻辑 | 不做跨聚合流程编排；不控制跨服务事务；不得依赖 manager |
| `cacch-enterprise-manager` | 跨聚合流程编排、全局事务控制、多服务聚合查询（如登录时同时校验用户、角色、权限、部门） | 不直接操作数据库；不编写单表 CRUD 逻辑；**不得依赖 web 模块** |
| `cacch-enterprise-integration` | 企业微信/钉钉/飞书等身份源客户端封装、扫码登录、组织同步适配，屏蔽外部系统协议差异 | 业务层不得直接调用第三方 SDK，必须通过此模块 |
| `cacch-enterprise-async` | 登录日志/操作日志异步写入（Spring 事件监听）、站内信推送、报表类定时任务 | 解耦主流程，不得同步阻塞主线程；**不得依赖 web 模块** |
| `cacch-enterprise-web` | Controller、Spring Security 配置、WebSocket 配置、配置文件、启动类、全局异常处理、MapStruct Converter；打包为 War 部署外部 Tomcat | 仅做参数校验、请求转发、结果封装；不含业务逻辑 |
| `cacch-enterprise-ui` | 前端工程（Vue 3 + Vite + Element Plus SPA），登录页、工作台、系统管理、组织管理、消息中心等页面 | 独立 Vite 工程，**不参与 Maven 构建**，不共享后端代码 |

### 3.2 依赖流向（单向严禁反向）

```
web → async / manager → service → dao → common
web → manager → service → integration → common
service → integration → common
manager → integration → common
async → manager / service → … → common
```

完整依赖关系：

| 模块 | 允许依赖 |
|------|----------|
| `web` | manager、async、common（间接） |
| `async` | manager、service（及其传递依赖） |
| `manager` | service、integration |
| `service` | dao、integration |
| `integration` | common |
| `dao` | common |
| `common` | 无内部子模块 |

- **下层模块不得依赖上层模块**（如 `service` 不能依赖 `manager`，`dao` 不能依赖 `service`，**manager/async 不能依赖 web**）。
- 模块间调用必须通过接口层（`{biz}.api` 包下的接口），**禁止直接依赖实现类**。
- 禁止循环依赖。
- 前端 `cacch-enterprise-ui` 不参与 Maven 构建，通过 HTTP + JWT 与后端交互，独立打包后由 Nginx 托管，经 `/api/**` 反向代理访问后端。

### 3.3 包名规范

- 顶层包名：`com.cacch.enterprise`
- **核心原则：各模块内按业务域（小写）划分子包，再按技术职责分层**，禁止将所有业务类平铺在 `api`、`controller` 等顶层包下。
- 业务域包名**固定为 5 个**：`system`（系统管理）、`auth`（认证授权）、`org`（组织架构）、`notice`（消息通知）、`log`（日志审计）；`integration` 模块内按三方系统划分：`wecom`（企业微信）、`dingtalk`（钉钉）、`feishu`（飞书）。

#### 3.3.1 各模块包路径模式

| 模块 | 包路径模式 | 本项目示例 |
|------|-----------|---------|
| **common** | `common.constant.{biz}` | `common.constant.auth.AuthCommonConstants` / `AuthPathConstants` / `AuthScanLoginConstants` |
| **common** | `common.constant.redis`（基础设施） | `common.constant.redis.RedisConstants` |
| **common** | `common.config.{biz}` | `common.config.auth.SecurityProperties` |
| **common** | `common.{exception\|result\|utils\|annotation}`（跨域通用） | `common.exception.BizException` |
| **dao** | `entity.{biz}` / `mapper.{biz}` | `entity.system.SysUserDO` |
| **service** | `service.{biz}.api` / `service.{biz}.api.impl` | `service.system.api.ISysUserService` |
| **manager** | `manager.{biz}.api` / `manager.{biz}.api.impl` | `manager.auth.api.IAuthLoginManager` |
| **integration** | `integration.{三方系统}.client` / `client.dto` | `integration.wecom.client.WeComAuthClient` |
| **integration** | `integration.{三方系统}.adapter` | `integration.dingtalk.adapter.DingTalkOrgSyncAdapter` |
| **integration** | `integration.config`（基础设施） | `integration.config.RestTemplateConfig` |
| **async** | `async.{biz}.task` / `async.{biz}.listener` | `async.log.task.LoginLogArchiveTask` / `async.log.listener.OperationLogListener` |
| **web** | `controller.{biz}` | `controller.system.SysUserController` |
| **web** | `convert.{biz}` | `convert.system.SysUserConverter` |
| **web** | `dto.{biz}.vo` / `dto.{biz}.request` | `dto.system.vo.SysUserVO` |
| **web** | `config.{biz}` | `config.auth.AuthConfiguration` |
| **web** | `security`（基础设施） | `security.JwtAuthenticationFilter` |
| **web** | `websocket`（基础设施） | `websocket.NoticeWebSocketHandler` |
| **web** | `exception`（跨域通用） | `exception.GlobalExceptionHandler` |

#### 3.3.2 业务域包划分规则

- 新增业务功能时，**先确定业务域 `{biz}`**（仅限 3.3 节固定 5 个），再按上表选择对应模块与包路径。
- 同一业务域的 Controller、Manager、Service、Client、Converter、DTO 必须使用**相同的 `{biz}` 包名**。
- 跨业务域的通用能力（异常、返回体、Redis 前缀等）放在无 `{biz}` 的公共包中。
- 禁止在 `service.api`（无业务域）等顶层包下直接新建业务类。

```java
// 正确 —— 用户管理 Controller 放在 controller.system 包
package com.cacch.enterprise.controller.system;

// 正确 —— 登录编排 Manager 接口放在 manager.auth.api 包
package com.cacch.enterprise.manager.auth.api;

// 错误（禁止 —— 业务类平铺在顶层 controller 包）
package com.cacch.enterprise.controller;
public class SysUserController { ... }
```

#### 3.3.3 完整包结构示例（auth 认证业务域 + wecom 身份源）

```
common/constant/auth/AuthCommonConstants.java
common/constant/redis/RedisConstants.java
common/config/auth/SecurityProperties.java
service/auth/api/IAuthLoginService.java
service/auth/api/impl/AuthLoginServiceImpl.java
manager/auth/api/IAuthLoginManager.java
manager/auth/api/impl/AuthLoginManagerImpl.java
integration/wecom/client/WeComAuthClient.java        # 按三方系统划分（wecom/dingtalk/feishu）
integration/wecom/client/dto/WeComUserInfoResponse.java
async/log/listener/LoginLogListener.java             # Spring 事件监听
async/log/task/OnlineUserStatTask.java
controller/auth/AuthLoginController.java
convert/auth/AuthLoginConverter.java
dto/auth/vo/LoginTokenVO.java
config/auth/AuthConfiguration.java
```

### 3.4 配置类归属规范

| 类型 | 所在模块 | 说明 |
|------|----------|------|
| 配置 POJO（如 `SecurityProperties` 的嵌套配置） | `common.config.{biz}` | 纯数据类，无 Spring 注解，可被 manager/async 引用 |
| `@ConfigurationProperties` 绑定类 | `common.config.{biz}` | 使用构造器绑定（不可变），**禁止** `@Component` |
| `@EnableConfigurationProperties` 注册 | `web.config.{biz}` | 仅在启动层注册 Bean |
| HTTP 客户端、RestTemplate 等 | `integration.config` | 第三方基础设施配置（跨业务域） |

```java
// common 模块 — 配置 POJO + 绑定类（auth 业务域）
package com.cacch.enterprise.common.config.auth;

@ConfigurationProperties(prefix = "cacch.security")
public class SecurityProperties {
    private final String jwtSecret;
    private final Long accessTokenExpire;
    private final Long refreshTokenExpire;
    public SecurityProperties(String jwtSecret, Long accessTokenExpire, Long refreshTokenExpire) { ... }
}

// web 模块 — 注册 Bean
package com.cacch.enterprise.config.auth;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class AuthConfiguration {}
```

---

## 四、代码分层规范

采用「Controller → Manager → Service → Mapper」四层架构，各层职责严格隔离：

### Controller 层
- 仅做：参数校验（`@Valid`）、调用 Manager/Service、使用统一返回体封装结果；权限校验通过 `@PreAuthorize` 或自定义权限注解完成
- 禁止：编写任何业务逻辑、直接操作数据库、直接调用 Mapper、直接调用第三方 Client

**Controller 路由规则：**

| 场景 | 调用层 | 示例 |
|------|--------|------|
| 单聚合 CRUD、无跨域编排 | Controller → **Service** | 查询单个用户详情 |
| 跨聚合编排、多 Service 协作、需全局事务 | Controller → **Manager** | 创建用户并分配角色、部门 |
| 多表校验/聚合查询（登录、导出、批量授权） | Controller → **Manager** | 登录认证（同时校验用户、角色、权限、部门） |

```java
// 正确示例 — 跨聚合走 Manager
@PostMapping("/api/v1/users")
public Result<SysUserVO> createUser(@Valid @RequestBody CreateSysUserRequest request) {
    return Result.success(sysUserManager.createSysUser(request));
}

// 正确示例 — 单聚合读走 Service
@GetMapping("/api/v1/users/{id}")
public Result<SysUserVO> getUser(@PathVariable Long id) {
    return Result.success(sysUserConverter.toVO(sysUserService.getById(id)));
}
```

### Manager 层
- 仅做：跨聚合流程编排、全局事务控制（数据库写操作的 `@Transactional`）、多 Service 结果聚合
- 禁止：直接注入 Mapper、编写单表 CRUD 逻辑

### Service 层
- 仅做：单一聚合内的业务逻辑、操作对应聚合根数据
- 禁止：跨聚合调用其他 Service 的写操作、控制跨服务事务、依赖 Manager
- 第三方调用须通过 `integration` 模块的 Client/Adapter，禁止直接使用 `RestTemplate` 或第三方 SDK

### Mapper 层
- 仅做：数据库 SQL 交互
- 禁止：拼接任何业务逻辑、调用其他 Mapper 外的 Bean；数据权限条件必须走拦截器，不得在 XML 中手写部门过滤 SQL

### Integration 层调用链

```
Service / Manager → Adapter（可选，协议/参数转换）→ Client（HTTP / SDK 封装）
```

- 本模块承载企业微信/钉钉/飞书等**身份源**对接（扫码登录、组织同步适配），屏蔽外部系统协议差异
- **Client**：封装 HTTP/SDK 原始调用，处理 URL、序列化、超时
- **Adapter**：在 Client 之上做业务语义转换（请求组装、响应映射、错误码转换）
- 简单场景可省略 Adapter，直接使用 Client；复杂对接建议引入 Adapter

---

## 五、对象转换规范

- **所有 DO ↔ DTO/VO 转换必须通过 MapStruct 实现**，禁止手动 setter/getter 堆砌。
- Converter 接口统一放在 `cacch-enterprise-web` 模块的 `convert.{biz}` 包下（如 `convert.system.SysUserConverter`）。
- MapStruct 接口声明为 `@Mapper(componentModel = "spring")`。
- integration 模块内部的第三方响应 DTO（如 `WeComUserInfoResponse`）可在 integration 包内转换，不必放到 web。
- **数据类编写规范**：DO/DTO/VO/Request 等数据类统一使用 Lombok `@Data` / `@Builder`，禁止手写构造方法与内部类；配置属性类除外（须按第十三节使用构造器绑定，保证不可变）。

```java
// 正确示例
@Mapper(componentModel = "spring")
public interface SysUserConverter {
    SysUserVO toVO(SysUserDO sysUserDO);
    SysUserDO toDO(CreateSysUserRequest request);
}

// 错误示例（禁止）
SysUserVO vo = new SysUserVO();
vo.setId(sysUserDO.getId());
vo.setName(sysUserDO.getName());
```

---

## 六、数据库开发规范

### 6.1 表结构规范
- 所有表必须包含以下标准字段：

```sql
id          BIGINT PRIMARY KEY,     -- 主键（见 6.5 主键策略）
create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- 更新时间
create_by   BIGINT,                 -- 创建人（用户 ID）
update_by   BIGINT,                 -- 更新人（用户 ID）
deleted     SMALLINT NOT NULL DEFAULT 0   -- 逻辑删除标记（0正常 1删除）
```

- 表名统一前缀 `sys_`，全小写，下划线分隔，例如 `sys_user`、`sys_role`、`sys_menu`、`sys_dept`、`sys_dict`、`sys_notice`。
- 表别名规则：采用除前缀外的英文首字母拼接（如 `sys_user` 别名 `u`、`sys_role` 别名 `r`），禁止无意义别名。

### 6.2 Schema 变更规范
- **禁止直接修改数据库结构**，所有变更必须通过 Flyway 脚本提交。
- Flyway 脚本命名：`V{版本号}__{描述}.sql`，版本号递增，**禁止修改已执行脚本**。
- 脚本路径：`cacch-enterprise-web/src/main/resources/db/migration/`
- **执行策略（test/prod 共用库）**：
  - `spring.profiles.active=test`：**唯一**允许 Flyway 自动迁移的环境（`spring.flyway.enabled=true`）
  - `spring.profiles.active=prod`：**禁止** Flyway 自动迁移（`spring.flyway.enabled=false`），生产启动不得改表
  - 新增脚本先在 test 环境启动验证，prod 仅使用已迁移完成的表结构

### 6.3 PostgreSQL 12 兼容约束
- **禁止使用 `MERGE` 语法**，幂等写入统一使用 `INSERT ... ON CONFLICT DO UPDATE`。
- 分区表避免复杂嵌套设计。
- 大表需提前配置 `autovacuum` 参数，定期检查表膨胀；日志类大表（操作日志、登录日志）按月分表或定期归档。
- JSON 字段使用 `JSONB` 类型（PostgreSQL 12 支持）。

### 6.4 MyBatis-Plus 使用规范
- 实体类使用 `@TableName`、`@TableId`、`@TableField` 注解。
- 逻辑删除字段使用 `@TableLogic`，配合全局配置。
- 复杂查询使用 `LambdaQueryWrapper`，禁止字符串拼接 SQL。
- 主键生成使用 `@TableId(type = IdType.ASSIGN_ID)`（雪花算法），与 Flyway 中 `BIGINT` 主键一致。
- 数据权限（全部 / 本部门 / 本部门及下级 / 仅本人）由 MyBatis-Plus 拦截器自动拼接 SQL 条件。

### 6.5 主键策略
- 所有业务表主键为 `BIGINT`，由 MyBatis-Plus 雪花算法生成，**禁止**数据库自增。
- Flyway 脚本中主键列不加 `SERIAL` / `AUTO_INCREMENT`。

---

## 七、安全规范

- **用户密码必须 BCrypt 哈希存储**，禁止明文或可逆加密。
- **JWT 密钥、三方系统密钥等敏感信息禁止硬编码**，必须通过 `application-{profile}.yml`（如 `cacch.security.jwt-secret`）或配置中心注入，不得出现在代码中。
- 所有业务接口必须经过 **Spring Security 鉴权**，`permitAll` 仅放行登录、验证码、健康检查等白名单；后端接口权限必须二次校验，不得仅依赖前端隐藏按钮。
- Controller 层权限校验使用 `@PreAuthorize` 或自定义权限注解（如 `@RequiresPermission`）。
- **JWT 双令牌机制**：access_token 有效期短（如 7200s），refresh_token 用于续期（如 7 天）；登录态存 Redis，支持强制下线与多端登录策略配置。
- 登录、导出、批量操作等接口必须做**限流**与参数合法性校验。
- 操作日志中对密码、身份证、手机号等敏感字段必须**脱敏后再落库**；敏感数据存储必须哈希或加密，禁止明文。
- 前端路由按权限动态注册，后端兜底校验。

---

## 八、命名规范

### 类命名
| 类型 | 命名规则 | 示例 |
|------|----------|------|
| 数据库实体 | `{业务名}DO` | `SysUserDO` |
| 请求 DTO | `{操作}{业务名}Request` | `CreateSysUserRequest` |
| 响应 VO | `{业务名}VO` | `SysUserVO` |
| Service 接口 | `I{业务名}Service` | `ISysUserService` |
| Service 实现 | `{业务名}ServiceImpl` | `SysUserServiceImpl` |
| Manager 接口 | `I{业务名}Manager` | `ISysUserManager` |
| Manager 实现 | `{业务名}ManagerImpl` | `SysUserManagerImpl` |
| Mapper 接口 | `{业务名}Mapper` | `SysUserMapper` |
| Controller | `{业务名}Controller` | `SysUserController` |
| 第三方客户端 | `{系统名}Client` | `WeComAuthClient` |
| 适配器 | `{系统名}Adapter` | `DingTalkOrgSyncAdapter` |
| MapStruct 转换器 | `{业务名}Converter` | `SysUserConverter` |
| 全局异常处理 | `GlobalExceptionHandler` | — |

### 接口 URL 命名
- RESTful 风格，名词复数，小写连字符：`/api/v1/users`
- 查询：`GET /api/v1/users`、`GET /api/v1/users/{id}`
- 新增：`POST /api/v1/users`
- 更新：`PUT /api/v1/users/{id}`
- 删除：`DELETE /api/v1/users/{id}`
- 分页查询：`GET /api/v1/users?page=1&size=20`

### 常量类命名

- **业务域内按维度拆分**，放在 `common.constant.{biz}` 包下（基础设施类如 Redis 放 `common.constant.redis`），每个业务域按以下三个维度各建一个常量类：
  - `{业务域}CommonConstants`：公共请求参数、签名规则、响应码、日志标识、Token 缓存 Key 等**跨接口共享**的常量
  - `{业务域}PathConstants`：该业务域下所有接口 PATH
  - `{业务域}{模块/接口}Constants`：单一接口或模块的**专有参数**，如 `AuthScanLoginConstants`
- **命名规则**：例如 `constant.auth.AuthCommonConstants`、`AuthPathConstants`、`AuthScanLoginConstants`；不属于任何业务域的通用常量保留 `{域}Constants`（如 `constant.redis.RedisConstants`）。
- **禁止**将所有常量堆叠在 `EnterpriseConstants` 等单一「大杂烩」类中；也禁止在 `CommonConstants` 中混入单一接口专有参数。
- **适用范围**：新增业务域一律按上述三维度拆分；**已存在的历史业务域常量类不做强制迁移**，待其发生功能性改动时再按新规拆分。
- 每个常量类声明为 `final`，提供私有构造器，禁止实例化。
- 跨常量类可互相引用（如 `{业务域}CommonConstants.loginTokenRedisKey()` 引用 `RedisConstants.KEY_PREFIX`），但不得引入业务逻辑。
- 常量命名：全大写下划线（`QRCODE_EXPIRE_SECONDS`），工具方法使用 camelCase（`loginTokenRedisKey()`）。

```java
// 正确 —— 业务域内按三维度拆分（以 auth 为例）
package com.cacch.enterprise.common.constant.auth;

public final class AuthCommonConstants {
    private AuthCommonConstants() {}
    public static final String PARAM_CAPTCHA = "captcha";
    public static final int LOGIN_FAIL_LOCK_TIMES = 5;
    public static String loginTokenRedisKey(Long userId) {
        return RedisConstants.KEY_PREFIX + "auth:token:" + userId;
    }
}

public final class AuthPathConstants {
    private AuthPathConstants() {}
    public static final String PATH_LOGIN = "/api/v1/auth/login";
}

public final class AuthScanLoginConstants {
    private AuthScanLoginConstants() {}
    public static final int QRCODE_EXPIRE_SECONDS = 300;
}
```

---

## 九、统一返回体与异常规范

所有接口统一使用 `common` 模块的 `Result<T>` 包装返回值：

```java
// 成功
Result.success(data)
Result.success()

// 失败
Result.fail(ResultCode.XXX)
Result.fail(ResultCode.XXX, "自定义消息")
Result.fail(BizException)
```

- **禁止在 Controller 中直接返回裸实体或裸 DTO**。
- 异常统一由 `web.exception.GlobalExceptionHandler`（`@RestControllerAdvice`）捕获，转换为 `Result` 格式返回。
- Controller **禁止 catch 业务异常**，让异常自然抛给全局处理器。
- Service/Manager 层通过 `throw new BizException(ResultCode.XXX)` 抛出业务异常。

### 错误码号段

| 号段 | 用途 | 示例 |
|------|------|------|
| `0` | 成功 | `SUCCESS` |
| `400xx` | 客户端 / 参数错误 | `PARAM_ERROR`、`PARAM_MISSING`、`PARAM_INVALID` |
| `500xx` | 第三方集成错误（身份源调用失败等） | `INTEGRATION_ERROR`、`INTEGRATION_TIMEOUT` |
| `999xx` | 系统内部错误 | `SYSTEM_ERROR` |

- 新增错误码须同步更新 `common.result.ResultCode` 枚举，按号段归类。

---

## 十、异步与消息规范

- 异步逻辑统一放在 `cacch-enterprise-async` 模块，按业务域划分：Spring 事件监听器放 `async.{biz}.listener`，定时任务（`@Scheduled`）放 `async.{biz}.task`。
- 主流程通过 `ApplicationEventPublisher` 发布事件（如操作日志、登录日志、站内信推送），监听器**异步执行**（事件广播器配置线程池或 `@Async`），不得同步阻塞主流程。
- `@EnableScheduling` 仅在 web 启动类声明。
- 事件监听与异步写入必须保证幂等性：处理前检查状态，避免重复处理。
- 监听失败需记录错误日志，**不得静默吞掉异常**；必要时通过重试或补偿机制保证最终一致。
- 定时任务中单个条目失败不得阻断其余条目处理（逐条 try-catch 并统计成功/失败数）。

---

## 十一、日志规范

- **唯一配置入口**：`cacch-enterprise-web/src/main/resources/logback-spring.xml`，禁止在 `application*.yml` 中配置 `logging.level`、`logging.file` 等（避免与 logback 冲突）。
- 使用 SLF4J + `@Slf4j`（Lombok），禁止直接使用 `System.out.println` 或 MyBatis `StdOutImpl`。
- **TraceId**：HTTP 请求由 `TraceIdFilter` 写入 MDC（键名 `traceId`），logback 格式 `%X{traceId}` 自动输出；跨服务可通过请求头 `X-Trace-Id` 传递。
- 关键业务节点必须打印日志，但**业务层（Controller/Service/Manager）禁止打印完整请求体/敏感字段**。
- **三方 Client 入参/出参（强制）**：`integration` 模块调用外部系统时，每个接口必须在 Client 内用 INFO 打印**完整入参与出参**（经 `ThirdPartyHttpLogSupport` 脱敏）。细则见 `.trae/rules/java-third-party-http-io-logging.md`。
- **日志前缀必须使用中文方括号 `【业务标识】`**，禁止使用英文方括号 `[业务标识]`。
- 格式：`log.info("【SysUser】创建成功，userId={}", userId);`（`【】` 后直接接中文描述，无需额外空格）。
- 日志级别（在 logback-spring.xml 按 profile 配置）：
  - `DEBUG`：调试信息 / 测试环境 SQL，生产环境不开启
  - `INFO`：正常业务流程节点；**逻辑提前终止（跳过/中断/条件不满足 return）与异常处理路径也必须打 INFO**，说明原因与关键业务键，禁止静默终止或吞异常；三方 Client 的入参/出参亦为 INFO
  - `WARN`：可恢复的异常情况（含 BizException）
  - `ERROR`：需要介入处理的错误，必须附带异常堆栈
- **逻辑终止 / 异常 INFO 强制要求**（细则见 `.trae/rules/java-info-logging-on-exit.md`）：
  - 前置条件不满足提前返回、跳过当前记录/批次、业务中断不再继续后续步骤，均须 `log.info` 提示
  - `catch` 或异常分支终止本条/本批处理时，须先/同时打 INFO 说明业务影响；技术细节仍用 `WARN`/`ERROR` 并附堆栈
- **禁止在日志中打印敏感信息**（密码明文、Token、secret、身份证、手机号等），须脱敏后输出。

```java
// 正确示例 —— traceId 由 logback 自动输出，业务日志只需写消息
log.info("【SysUser】创建用户成功，userId={}, username={}", userId, username);
log.error("【WeCom】获取扫码用户身份失败, code={}", code, e);

// 错误示例（禁止）
System.out.println("创建成功");
log.info("[SysUser] 创建成功");           // 禁止使用英文方括号
log.info("password原文: {}", rawPassword);
log.info("request={}", request);  // 业务层禁止；三方 Client 须走 ThirdPartyHttpLogSupport
```

日志输出示例：
```
2026-10-26 10:00:00.123 [http-nio-8080-exec-1] [traceId:a1b2c3d4e5f6...] INFO  c.c.e.controller.system.SysUserController - 【SysUser】查询用户详情...
2026-10-26 14:13:26.123 [http-nio-8080-exec-2] [traceId:...] INFO  c.c.e.i.wecom.client.WeComAuthClient - 【WeCom】获取扫码用户身份入参, url=https://..., 入参={...}
2026-10-26 14:13:26.456 [http-nio-8080-exec-2] [traceId:...] INFO  c.c.e.i.wecom.client.WeComAuthClient - 【WeCom】获取扫码用户身份出参, 出参={...}
```

---

## 十二、事务控制规范（强制）

**核心原则：`@Transactional` 仅用于 MyBatis 数据库操作，Redis 缓存、HTTP 调用、文件 I/O 不得加数据库事务注解。**

### 12.1 适用范围

| 操作类型 | 是否使用 `@Transactional` |
|----------|---------------------------|
| MyBatis 写操作（INSERT/UPDATE/DELETE） | **必须** |
| MyBatis 只读查询（纯 DB） | 可选（复杂查询建议只读事务） |
| Redis 缓存读写 | **禁止** |
| HTTP / 第三方 API 调用 | **禁止** |
| 混合 DB + Redis / HTTP 的方法 | 仅 DB 部分拆到独立方法，由 Manager 编排 |

### 12.2 事务声明规则

- 涉及数据库写操作的方法，**必须显式使用 `@Transactional`**，禁止空注解。
- **禁止使用空注解 `@Transactional`**，必须显式设置 `rollbackFor`、`propagation`、`readOnly`、`timeout`：

```java
// 错误示例（禁止 —— 完全依赖默认值）
@Transactional
public void updateOrder(OrderVO vo) { ... }

// 正确示例 —— 显式声明所有关键属性
@Transactional(
    rollbackFor = Exception.class,
    propagation = Propagation.REQUIRED,
    readOnly = false,
    timeout = 30
)
public void updateOrder(OrderVO vo) { ... }
```

### 12.3 各属性必须显式设置

| 属性 | 要求 | 说明 |
|------|------|------|
| `rollbackFor` | **必须设为 `Exception.class`** | 所有业务异常也必须触发回滚 |
| `propagation` | **必须显式指定** | 禁止依赖默认 `REQUIRED` |
| `readOnly` | **必须显式指定** | DB 读操作 `true`，写操作 `false` |
| `timeout` | **必须显式设置** | 写操作默认 30s，读操作默认 10s |
| `isolation` | 涉及高并发或一致性要求时必须显式设置 | 默认 `DEFAULT` 只在简单场景可用 |

### 12.4 传播行为使用场景

| 场景 | 传播行为 | 说明 |
|------|----------|------|
| Manager 层跨聚合编排主方法 | `Propagation.REQUIRED` | 作为事务入口 |
| Service 层单聚合写操作（被 Manager 调用） | `Propagation.REQUIRED` 或 `MANDATORY` | `MANDATORY` 强制调用方已开事务 |
| 独立事务（审计日志、事件监听） | `Propagation.REQUIRES_NEW` | 不受外部事务回滚影响 |
| 只读 DB 查询 | `Propagation.SUPPORTS` | 有事务则加入，无事务也可执行 |

### 12.5 分层事务职责

| 层级 | 事务职责 | 规则 |
|------|----------|------|
| **Controller** | **禁止使用 `@Transactional`** | 不做任何事务控制 |
| **Manager** | 事务入口，全局事务控制 | DB 写操作必须显式声明事务 |
| **Service** | 参与事务（由 Manager 驱动） | 可声明 `MANDATORY` 强制调用方开事务 |
| **Mapper/DAO** | **禁止使用 `@Transactional`** | 数据库交互层不控制事务 |
| **Async 事件监听 / 异步任务** | 独立事务 | 监听器与异步任务使用 `REQUIRES_NEW` |

### 12.6 禁止事项

- **禁止将 `@Transactional` 加在类级别**，必须精确到方法级别。
- **禁止在 `private` 方法上使用 `@Transactional`**（Spring AOP 代理无效）。
- **禁止在同一个类中自调用事务方法**（自调用不走代理）。须拆分到独立 Bean。
- **禁止在事务内执行远程 HTTP 调用、Redis 操作、文件 I/O**，避免长事务与无效注解。
- **禁止 try-catch 吞掉异常后不抛出**，否则事务不会回滚。

---

## 十三、依赖注入规范（强制）

**核心原则：禁止使用字段注入（`@Autowired`），所有依赖必须通过构造器注入。**

### 13.1 注入方式规则

```java
// 错误示例（禁止 —— 字段注入）
@Service
public class SysUserServiceImpl implements ISysUserService {
    @Autowired
    private SysUserMapper sysUserMapper;
}

// 正确示例 —— 构造器注入，配合 Lombok @RequiredArgsConstructor
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {
    private final SysUserMapper sysUserMapper;
}
```

### 13.2 强制规则

- **所有依赖字段必须声明为 `private final`**，配合 Lombok `@RequiredArgsConstructor`。
- **禁止在字段上标注 `@Autowired`、`@Resource`、`@Inject`**。
- **禁止 Setter 注入**，除非有明确的循环依赖（此时应优先重构）。
- 配置属性类使用**构造器绑定**（不可变 `final` 字段或 Java Record），禁止 `@Data` + setter 绑定。

### 13.3 Spring Boot 4 配置属性写法

```java
// 正确 —— 不可变构造器绑定（Spring Boot 3+/4，无需 @ConstructorBinding）
@ConfigurationProperties(prefix = "cacch.security")
public class SecurityProperties {
    private final String jwtSecret;
    private final Long accessTokenExpire;
    public SecurityProperties(String jwtSecret, Long accessTokenExpire) {
        this.jwtSecret = jwtSecret;
        this.accessTokenExpire = accessTokenExpire;
    }
}

// 正确 —— 嵌套 POJO 使用 @Value 保证不可变
@Value
public class WeComAppConfig {
    String corpid;
    String appKey;
    String secret;
}

// 错误（禁止 —— setter 绑定）
@Data
@Component
@ConfigurationProperties(prefix = "cacch.security")
public class SecurityProperties { ... }
```

### 13.4 特殊情况处理

| 场景 | 处理方式 |
|------|----------|
| 出现循环依赖编译报错 | **优先重构代码，消除循环依赖**，禁止用 `@Lazy` 掩盖问题 |
| `@Configuration` 类内部 `@Bean` 方法 | 通过方法参数注入，不使用字段注入 |
| 测试类 | 使用构造器注入或 `@MockBean` + 构造器，禁止字段 `@Autowired` |

---

## 十四、缓存规范

| 缓存 | 适用场景 | 模块 |
|------|----------|------|
| **Caffeine** | 进程内高频只读：字典、参数、菜单树、Dashboard 统计卡片 | web.config |
| **Redis** | 分布式共享：登录态、验证码、Token、限流计数、跨实例缓存 | service 层读写 |

- Redis Key 命名：`enterprise:{domain}:{identifier}`，前缀定义在 `RedisConstants.KEY_PREFIX`，业务 Key 组装方法放在对应业务 Constants 中（如 `AuthCommonConstants.loginTokenRedisKey()`）。
- Token 类缓存 TTL 应略小于第三方有效期（如企微 7200s → 缓存 7000s）。
- 缓存未命中时的回源逻辑放在 Service 层，Manager 负责编排；字典、菜单缓存变更后必须主动 evict。

---

## 十五、测试规范

- 单元测试类命名：`{被测类名}Test`，放在对应模块的 `src/test/java` 下。
- 集成测试类命名：`{被测类名}IT`。
- 单元测试使用构造器注入 + mock 依赖，禁止字段 `@Autowired`。
- 新增 Flyway 脚本须在集成测试中验证可正常执行。
- 推荐集成测试使用 Testcontainers 启动 PostgreSQL / Redis。

---

## 十六、接口与文档注释规范（强制）

**核心原则：所有对外暴露的类与方法必须配有中文 Javadoc，且每个参数、返回值、异常均须说明含义与约束。**

### 16.1 适用范围

| 类型 | 类注释 | 方法注释 | 参数 `@param` | 返回值 `@return` | 异常 `@throws` |
|------|--------|----------|---------------|------------------|----------------|
| Service / Manager **接口**（`*.api`） | **必须** | 所有 public 方法 **必须** | **必须**（每个参数） | 非 `void` **必须** | 声明或可抛出时 **必须** |
| Service / Manager **实现类**（`*.api.impl`） | **必须** | 与接口一致；重写方法可省略重复说明，**业务逻辑与接口不同时须补充** | 同接口 | 同接口 | 同接口 |
| **Controller** | **必须** | 所有接口方法 **必须** | **必须**（含路径变量、查询参数、请求体） | **必须**（`Result<T>` 中 `T` 的含义） | 可预期业务异常时 **必须** |
| Integration **Client / Adapter** | **必须** | 所有 public 方法 **必须** | **必须** | 非 `void` **必须** | 第三方调用失败时 **必须** |
| **Mapper** 自定义方法 | 接口类 **必须** | 自定义 SQL 方法 **必须** | **必须** | 非 `void` **必须** | — |
| **DO / DTO / VO / Request** | **必须** | — | 非显而易见字段 **必须**用字段注释说明 | — | — |
| **枚举** | **必须** | 每个枚举常量建议一行说明 | — | — | — |
| **定时任务 / 事件监听器** | **必须** | 入口方法（如 `run`、`onApplicationEvent`）**必须** | 有参数时 **必须** | 非 `void` **必须** | — |
| 工具类 / 常量类 | **必须** | 所有 public 静态方法 **必须** | **必须** | 非 `void` **必须** | — |
| `private` 方法 | 不要求 | 逻辑复杂或非显而易见时建议补充 | — | — | — |

- 继承 `BaseMapper` 的默认 CRUD 方法**无需**重复编写 Javadoc。
- Lombok 生成的 getter/setter **无需**编写 Javadoc。

### 16.2 注释格式要求

- **语言**：描述使用简体中文，术语（如 `access_token`、`refresh_token`）可保留英文。
- **类注释**：说明职责边界、所属业务域；泛型类使用 `@param <T>` 说明类型参数。
- **方法注释**：首行一句话概括「做什么」；第二行起可补充业务规则、幂等性、缓存策略、调用约束等。
- **参数注释**：`@param 参数名 含义`；需说明单位、格式、取值范围、是否可空、与配置项/第三方字段的对应关系。
- **返回值**：`@return 含义`；集合需说明元素类型或空集合语义。
- **异常**：`@throws 异常类型 触发条件`。
- **作者**：所有公开 API 类必须添加 `@author hongfu_zhou@cacch.com`，禁止使用其他作者标识。
- **禁止**：无意义注释（如 `/** 获取 id */` 对 `getById(Long id)`）、复制参数名当说明、注释与实现不一致。

```java
/**
 * 用户服务接口
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISysUserService {

    /**
     * 按用户名查询用户（仅返回未逻辑删除数据）
     *
     * @param username 登录账号，全局唯一
     * @return 用户实体；不存在时返回 null
     * @throws BizException 参数为空时抛出
     */
    SysUserDO getByUsername(String username);
}
```

### 16.3 各层示例

**Controller：**

```java
/**
 * 用户管理 REST 接口
 *
 * @author hongfu_zhou@cacch.com
 */
@RestController
@RequestMapping("/api/v1/users")
public class SysUserController {

    /**
     * 按 ID 查询用户
     *
     * @param id 用户主键，对应 sys_user.id
     * @return 用户视图对象；不存在时由全局异常处理返回 404 业务码
     */
    @GetMapping("/{id}")
    public Result<SysUserVO> getById(@PathVariable Long id) { ... }
}
```

**Manager / Service 实现类：**

```java
/**
 * 用户服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {
    // 简单委托接口的方法可不重复 Javadoc；下列方法因含额外业务规则须补充说明

    /**
     * 按部门查询启用状态用户列表（仅返回未逻辑删除数据，按 create_time 倒序）
     *
     * @param deptId 部门 ID，对应 sys_dept.id
     * @return 用户列表，无数据时返回空列表（非 null）
     */
    @Override
    public List<SysUserDO> listEnabledByDept(Long deptId) { ... }
}
```

**Integration Client：**

```java
/**
 * 企业微信扫码登录 HTTP 客户端
 *
 * @author hongfu_zhou@cacch.com
 */
public class WeComAuthClient {

    /**
     * 通过扫码授权 code 换取用户身份
     *
     * @param code 扫码授权码，一次性有效，5 分钟内有效
     * @return 企微用户身份响应（userid、name 等）
     * @throws BizException HTTP 非 2xx 或企微 errcode 非 0 时抛出
     */
    public WeComUserInfoResponse getUserByCode(String code) { ... }
}
```

**DO / Request 字段：**

```java
/**
 * 用户实体，映射表 sys_user
 *
 * @author hongfu_zhou@cacch.com
 */
@TableName("sys_user")
public class SysUserDO {

    /** 登录账号，全局唯一 */
    private String username;

    /** 账号状态：0 启用 / 1 停用，见 SysUserStatusEnum */
    private Integer status;
}
```

### 16.4 维护要求

- **新增**公开类或方法时，须同步编写完整 Javadoc，不得先提交裸接口再补文档。
- **修改**方法签名（增删改参数、改返回值、改异常）时，须同步更新对应 `@param` / `@return` / `@throws`。
- **重构**仅改实现不改契约时，检查注释是否仍准确；行为变更须更新方法说明。
- AI 生成代码时，Service/Manager 接口方法、Controller 端点、Client 公开方法**默认必须带完整 Javadoc**。

---

## 十七、Git 提交说明规范（Conventional Commits）

**遵循 [Conventional Commits](https://www.conventionalcommits.org/) 主流规范**，便于生成 CHANGELOG、语义化版本与 Code Review。

### 17.1 提交信息结构

```
<type>(<scope>): <subject>

<body>

<footer>
```

| 组成部分 | 是否必填 | 说明 |
|----------|----------|------|
| **type** | **必填** | 变更类型，见 17.2 |
| **scope** | 建议填 | 影响范围：模块名或业务域，见 17.3 |
| **subject** | **必填** | 简短摘要，中文，祈使语气，≤ 50 字，句末不加句号 |
| **body** | 可选 | 详细说明「为什么改」「改了什么影响」；与 subject 空一行 |
| **footer** | 可选 | 关联 Issue、Breaking Change、Co-authored-by 等 |

**单行提交（小改动）**：仅写 `<type>(<scope>): <subject>` 即可。

**多行提交（推荐用于功能/修复）**：subject 概括意图，body 补充细节，footer 关联追踪项。

### 17.2 type 类型（固定枚举）

| type | 含义 | 典型场景 |
|------|------|----------|
| `feat` | 新功能 | 新增 API、定时任务、对接能力 |
| `fix` | 缺陷修复 | 修复 Bug、异常处理、边界条件 |
| `docs` | 文档 | README、Rule、Javadoc、SQL 说明 |
| `style` | 格式 | 缩进、import 排序；**不含**逻辑变更 |
| `refactor` | 重构 | 结构调整，不改变外部行为 |
| `perf` | 性能优化 | 缓存、SQL、批处理优化 |
| `test` | 测试 | 新增/修改单元测试、集成测试 |
| `build` | 构建 | Maven、依赖版本、打包配置 |
| `ci` | CI/CD | GitHub Actions、流水线脚本 |
| `chore` | 杂项 | 不影响源码逻辑的维护性改动 |
| `revert` | 回滚 | 撤销某次提交 |

- **禁止**自造 type（如 `update`、`dev`、`修改`）。
- 一次 commit 只做一类事项；功能与格式化混改应拆分提交。

### 17.3 scope 范围

优先使用**业务域**，跨模块改动可使用**模块名**：

| 分类 | 可选值 | 示例 |
|------|--------|------|
| 业务域 | `system`、`auth`、`org`、`notice`、`log` | `feat(auth): 支持企业微信扫码登录` |
| 模块 | `common`、`dao`、`service`、`manager`、`integration`、`async`、`web` | `fix(dao): 修正用户逻辑删除条件` |
| 基础设施 | `db`、`config`、`deps` | `build(deps): 父 POM 统一声明 Hutool 版本` |

- 影响多域时 scope 可省略：`refactor: 统一 Result 异常转换逻辑`。
- scope 使用小写英文，与包名 `{biz}` 保持一致。

### 17.4 subject 主题行规则

- 使用**中文**描述业务意图，type/scope 保持英文。
- 使用**祈使语气**（「添加」「修复」「移除」，而非「添加了」「修复了」）。
- 说明**做了什么**，而非仅罗列文件名（❌ `YML文件配置修改` → ✅ `chore(config): 调整 test 环境 Flyway 开关`）。
- 长度 ≤ 50 个字符（含标点）；超长细节放 body。
- 句末**不加**句号、感叹号。

### 17.5 body 正文（可选）

- 与 subject **空一行**书写。
- 说明：**变更动机**、**实现要点**、**对调用方/数据的影响**。
- 多条目使用 `-` 列表。
- 关联数据库变更时注明 Flyway 版本号。

```
feat(auth): 支持企业微信扫码登录

- 新增 WeComAuthClient 对接企微扫码授权接口
- 新增 Flyway V1.2.0 sys_user 表新增 wecom_userid 字段
- 登录成功后签发 JWT 双令牌并写入 Redis 登录态
```

### 17.6 footer 页脚（可选）

| 标记 | 用途 | 示例 |
|------|------|------|
| `Closes #123` / `Fixes #123` | 合并后自动关闭 Issue | `Closes #42` |
| `Refs #123` | 仅关联，不自动关闭 | `Refs #42` |
| `BREAKING CHANGE:` | 不兼容变更（触发 major 版本） | 见下方示例 |
| `Co-authored-by:` | 多人协作 | `Co-authored-by: name <email>` |

```
feat(web)!: 用户 API 路径调整为 /api/v2/users

BREAKING CHANGE: 原 /api/v1/users 已废弃，调用方须同步修改。
```

- 破坏性变更可在 type 后加 `!`：`feat(scope)!: subject`。

### 17.7 本项目示例

```
feat(auth): 支持企业微信扫码登录

fix(system): 修复重置密码后未清理登录态

docs: 补充 Git 提交说明与 Javadoc 规范

refactor(integration): 抽取钉钉组织同步为 Adapter

build(deps): 父 POM 统一声明 Hutool 版本

chore(config): 拆分 test/prod 环境安全配置

revert: feat(auth): 支持企业微信扫码登录
```

### 17.8 禁止事项

- ❌ `开发`、`修改`、`规则修改`、`日志修改` 等无 type 的模糊说明
- ❌ 一次 commit 混入多个不相关变更（功能 + 大规模格式化 + 依赖升级）
- ❌ subject 仅写文件名或模块名，不说明业务意图
- ❌ 提交 `target/`、`.class`、本地 IDE 配置等构建产物（应加入 `.gitignore`）

### 17.9 规范落地文件对照表

各项内容应写入的项目文件如下（按职责分层）：

| 规范内容 | 应写入的文件 | 作用 |
|----------|--------------|------|
| **完整提交规范**（结构、type、scope、示例、禁止项） | 本文件（`project_rules.md`）**第十七节** | 项目总规范；AI 与开发者共同遵循 |
| **提交时快速检查清单** | `.trae/rules/git-commit.md` | Trae 执行 commit 时自动加载的精简规则 |
| **贡献者可读说明**（可选） | `CONTRIBUTING.md` | 新人入职、对外协作的人类文档 |
| **`git commit` 编辑器模板**（可选） | `.gitmessage` | 执行 `git commit` 时预填结构提示 |
| **提交信息自动校验规则**（可选） | `commitlint.config.js` | commit-msg 钩子校验 type/格式 |
| **Git 钩子脚本**（可选） | `.husky/commit-msg` | 提交前调用 commitlint |
| **PR 描述模板**（可选） | `.github/pull_request_template.md` | Pull Request 的 Summary / Test plan |
| **版本发布变更记录**（可选） | `CHANGELOG.md` | 按版本汇总的对外变更日志 |

- **当前项目已落地**：本文件第十七节、`.trae/rules/git-commit.md`。
- **可选增强**：配置 `.gitmessage` + commitlint 可在本地强制校验；`CONTRIBUTING.md` 便于团队 onboarding。

---

## 十八、代码生成约束

生成代码时，必须严格遵守以下规则：

1. **确认模块归属**：根据功能判断代码应放在哪个模块，不得跨模块随意放置。
2. **确定业务域包名**：新增类必须先确定 `{biz}` 业务域（仅限 system/auth/org/notice/log），按 3.3 节包路径模式放置，禁止平铺在顶层 `api`/`controller` 包。
3. **检查依赖方向**：新增依赖前确认不违反单向依赖规则（尤其 manager/async 不得依赖 web）。
4. **使用规范类名**：严格按第八节命名规范生成类名和包名。
5. **禁止直接 new 实体赋值**：DO ↔ DTO 转换必须通过 MapStruct Converter。
6. **禁止硬编码配置**：数据库连接、Redis 地址、JWT 密钥、第三方系统密钥等均通过配置文件注入。
7. **新建表必须有 Flyway 脚本**：每新增一张表，必须同步生成对应的 Flyway 迁移 SQL。
8. **禁止修改已执行的 Flyway 脚本**：只能新增版本号更高的脚本。
9. **接口优先**：Service 与 Manager 均先定义接口（`{biz}.api` 包），再编写实现（`{biz}.api.impl` 包）。
10. **构造器注入**：所有 Bean 依赖必须使用构造器注入（`@RequiredArgsConstructor` + `private final`）。
11. **配置 POJO 放 common.config.{biz}**：可被 manager/async 引用的配置类定义在对应业务域包，注册在 `web.config.{biz}`。
12. **常量按业务域与维度拆分**：新增常量按 `{业务域}CommonConstants`（公共请求参数/签名/响应码等）、`{业务域}PathConstants`（接口 PATH）、`{业务域}{模块}Constants`（接口专有参数）三维度放入 `common.constant.{biz}` 对应类，禁止新建「大杂烩」常量类，禁止在 Common 类中混入单一接口专有参数；历史业务域常量类不强制迁移。
13. **日志以 logback-spring.xml 为准**：禁止在 yml 配置 logging.level；TraceId 由 TraceIdFilter + MDC 输出。
14. **日志前缀用中文方括号**：所有日志前缀使用 `【业务标识】`，禁止使用 `[业务标识]`。
15. **全局异常处理**：新增 Controller 时确保异常由 `GlobalExceptionHandler` 统一处理，Controller 不 catch 业务异常。
16. **事务仅限 DB**：Redis / HTTP 操作不加 `@Transactional`；含 DB 写操作的方法按第十二节显式声明。
17. **接口文档注释**：新增或修改公开 API 时，必须按第十六节为类、方法、参数补充 Javadoc，禁止无说明的裸方法签名。
18. **Git 提交说明**：创建 commit 时必须按第十七节编写提交信息，禁止使用「开发」「修改」等无意义说明。
19. **作者标识**：类级 Javadoc 的 `@author` 统一为 `hongfu_zhou@cacch.com`，见第十六节 16.2。
20. **前端代码归属**：新增页面放 `src/views/{biz}/`（与后端业务域同名），接口文件放 `src/api/{biz}.ts`，路由 `meta` 必须配置权限标识；禁止以前端隐藏按钮代替后端接口鉴权。

---

## 十九、任务三步确认流程（默认工作流，强制）

**用户每次输入问题或需求后，默认按以下三步顺序执行，每一步结束必须等待用户明确确认后方可进入下一步**。三步顺序固定，不得跳步、不得合并。

### 第一步 — 问题转译

将用户的原始输入转化为结构化问题描述，输出后等待用户确认。转译结果**必须包含**：

- **目标**：用户希望达成什么
- **范围**：涉及哪些模块、哪些文件、哪些业务域
- **约束**：用户明确提出或上下文推导出的限制条件
- **验收标准**：如何判断任务完成

输出后显式请求确认，例如：「以上问题转译是否准确？确认无误请回复，我将进入下一步」。

### 第二步 — 方案输出

用户确认问题转译无误后，输出实施计划 / 解决方案。

- **本阶段只允许输出方案文本**，**严禁修改任何文件**（新建 / 编辑 / 删除均禁止）
- 方案应覆盖：改动点清单、关键实现思路、风险或权衡、验证方式
- 输出后显式请求确认，例如：「以上方案是否可行？确认后我将开始执行」

### 第三步 — 执行产出

用户确认方案没问题后，才正式输出文档或代码。

- **只有本阶段才允许写文件**
- 按方案逐项实施，每项完成可同步进度

### 判定要点

- 三步顺序固定，不得跳步、不得合并（例如不得将「问题转译」与「方案」在同一条回复中一起输出）
- 每一步结束必须显式向用户请求确认
- 用户确认方式：任意明确认可表述（「确认」「没问题」「可以」「执行」等）均视为通过；提出修改意见则停留在当前步骤修订后重新等待确认
- **例外**：仅当用户在当前任务中明确说「跳过流程」「直接做」「不用确认」时，方可按用户指示省略对应步骤
- **适用范围**：凡将产出文件 / 代码 / 文档 / 规则 / 配置修改的任务（含小改动），一律走本三步流程；用户直接下达指令式请求（如"直接做""添加到规则"）**不构成**跳过例外，唯有用户**显式说出**"跳过流程 / 直接做 / 不用确认"方可省略对应步骤
- **步骤标注**：任务回复开头须声明当前所处步骤，格式为 `【第N步 · 步骤名称】`（如 `【第一步 · 问题转译】`、`【第二步 · 方案输出】`、`【第三步 · 执行产出】`），使流程遵守情况可被用户直接核查

---

## 二十、通用编码与协作规范（贯穿全程）

以下规则贯穿任务执行全过程（尤其约束第三步「执行产出」阶段的产出质量），默认遵守。

### A. 变更纪律

- **A1 最小变更原则**：只做任务要求的事，不顺手重构、格式化无关代码。
- **A2 先读后改**：修改任何文件前必须先读取并理解上下文与现有约定。
- **A3 禁止过度设计**：不添加未要求的功能、抽象、防御性代码和多余注释。
- **A4 依赖需审批**：新增第三方依赖前说明理由并征得用户同意，优先复用已有依赖。

### B. 安全与保护

- **B1 破坏性操作红线**：不执行 force push、reset --hard、删除分支 / 文件等破坏性操作，除非用户明确要求。
- **B2 秘密保护**：不将密钥、凭证、token 写入代码、日志或提交记录。
- **B3 不主动提交**：仅当用户明确要求时才执行 git commit / push。

### C. 质量与验证

- **C1 改动后验证**：代码改动后运行相关测试 / 构建并报告结果。
- **C2 不掩盖错误**：失败时报告根因，不静默吞异常、不靠反复重试掩盖问题。

### D. 沟通协作

- **D1 不确定先问**：需求模糊时先澄清，不靠猜测推进。
- **D2 引用给出来源**：提及代码 / 文件时附可点击的文件路径与行号。
- **D3 语言跟随**：回复与代码注释语言始终跟随用户当前语言。

### E. 环境适配

- **E1 环境兼容**：命令与脚本需兼容当前 Windows / PowerShell 环境。

---

## 二十一、部署形态

- **后端打包**：`cacch-enterprise-web` 模块以 **War** 打包（`<packaging>war</packaging>`，内嵌 Tomcat 依赖设为 `provided`），启动类 `CacchEnterpriseApplication` 须继承 `SpringBootServletInitializer`；生产部署到外部 Tomcat 10.1.x，同一 Tomcat 可挂载多个应用。
- **本地开发**：直接 main 方法启动，使用内嵌容器，无需本地安装 Tomcat。
- **配置外置**：生产通过 Tomcat `bin/setenv.sh` 注入 `-Dspring.profiles.active=prod` 与 `-Dspring.config.location=/opt/cacch/config/`，避免修改 War 包内配置；外置配置目录放置 `application-prod.yml` 与 War 内同名配置互补覆盖。
- **前端部署**：`cacch-enterprise-ui` 独立 `pnpm build`，产物 `dist/` 由 Nginx 托管；Nginx 反向代理 `/api/**` 到 Tomcat 应用上下文（`/cacch-enterprise-web/`），WebSocket 代理须设置 `Upgrade` / `Connection` 头。
- **WebSocket 多实例**：多实例部署时须接入 Redis Pub/Sub 或 Sticky Session，保证站内信广播在所有节点可达。
- **Tomcat 运维**：关闭应用自动 JSP 编译与热部署（`reloadable="false"`）；发版仅替换 War 包并重启应用上下文。
- 部署细节见 `README.md` 第八章。

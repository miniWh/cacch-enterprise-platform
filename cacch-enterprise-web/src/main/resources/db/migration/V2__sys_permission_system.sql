-- =====================================================================
-- V2__sys_permission_system.sql
-- 权限管理体系：sys_user / sys_role / sys_menu / sys_user_role / sys_role_menu / sys_dept
-- 命名规范：V{版本号}__sys_{业务}.sql（本项目与多系统集成项目共用同一数据库，
--           版本号与描述前缀需避免冲突，新增脚本前先核对 Flyway Schema History）
-- 版本协调：V2 版本号已与集成项目确认无占用；后续新增脚本版本号需双方协调递增。
-- 幂等策略：PostgreSQL 12 禁用 MERGE，初始化统一使用 INSERT ... ON CONFLICT DO NOTHING。
-- 初始密码：admin 账号初始密码 BCrypt 密文见下方（明文记录于设计文档 3.4 节，
--           为交付临时值，上线前必须修改密码）。
-- =====================================================================

-- ============ 1. 部门表 sys_dept ============
CREATE TABLE sys_dept (
    id          BIGINT PRIMARY KEY,
    parent_id   BIGINT       NOT NULL DEFAULT 0,
    ancestors   VARCHAR(500) NOT NULL DEFAULT '',
    dept_name   VARCHAR(64)  NOT NULL,
    order_num   INT          NOT NULL DEFAULT 0,
    leader_id   BIGINT,
    status      SMALLINT     NOT NULL DEFAULT 0,
    source      VARCHAR(32)  NOT NULL DEFAULT 'LOCAL',
    external_id VARCHAR(64),
    create_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   BIGINT,
    update_by   BIGINT,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_dept IS '部门表';
COMMENT ON COLUMN sys_dept.ancestors   IS '祖先链：从根(0)到父部门 ID 逗号分隔，不含自身，根部门为 0';
COMMENT ON COLUMN sys_dept.status      IS '状态：0 正常 / 1 停用（停用后不可向该部门及下级新增用户）';
COMMENT ON COLUMN sys_dept.source      IS '数据来源：LOCAL 本地维护 / IHR 人事系统同步（预留）';
COMMENT ON COLUMN sys_dept.external_id IS 'iHR 部门唯一编号（预留）';

CREATE UNIQUE INDEX uk_sys_dept_parent_name ON sys_dept (parent_id, dept_name) WHERE deleted = 0;
CREATE INDEX idx_sys_dept_parent ON sys_dept (parent_id);

-- ============ 2. 用户表 sys_user ============
CREATE TABLE sys_user (
    id              BIGINT PRIMARY KEY,
    username        VARCHAR(64)  NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    password        VARCHAR(100) NOT NULL,
    real_name       VARCHAR(64)  NOT NULL,
    dept_id         BIGINT,
    email           VARCHAR(128),
    status          SMALLINT     NOT NULL DEFAULT 0,
    source          VARCHAR(32)  NOT NULL DEFAULT 'LOCAL',
    external_id     VARCHAR(64),
    wecom_userid    VARCHAR(64),
    last_login_time TIMESTAMP,
    create_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       BIGINT,
    update_by       BIGINT,
    deleted         SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_user IS '用户表';
COMMENT ON COLUMN sys_user.username        IS '工号';
COMMENT ON COLUMN sys_user.phone           IS '手机号';
COMMENT ON COLUMN sys_user.password        IS '登录密码';
COMMENT ON COLUMN sys_user.status          IS '账号状态：0 启用 / 1 停用';
COMMENT ON COLUMN sys_user.source          IS '数据来源：LOCAL 本地创建 / IHR 人事系统同步（预留）';
COMMENT ON COLUMN sys_user.external_id     IS 'iHR 人员唯一编号（预留）';
COMMENT ON COLUMN sys_user.wecom_userid    IS '企微 UserId';
COMMENT ON COLUMN sys_user.last_login_time IS '最近登录时间（登录事件异步回写）';

CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_sys_user_phone    ON sys_user (phone)    WHERE deleted = 0;
CREATE UNIQUE INDEX uk_sys_user_wecom    ON sys_user (wecom_userid) WHERE deleted = 0 AND wecom_userid IS NOT NULL;
CREATE INDEX idx_sys_user_dept ON sys_user (dept_id);

-- ============ 3. 角色表 sys_role ============
CREATE TABLE sys_role (
    id         BIGINT PRIMARY KEY,
    role_code  VARCHAR(64)  NOT NULL,
    role_name  VARCHAR(64)  NOT NULL,
    data_scope SMALLINT     NOT NULL DEFAULT 4,
    built_in   SMALLINT     NOT NULL DEFAULT 0,
    status     SMALLINT     NOT NULL DEFAULT 0,
    remark     VARCHAR(255),
    create_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by  BIGINT,
    update_by  BIGINT,
    deleted    SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_role IS '角色表';
COMMENT ON COLUMN sys_role.role_code  IS '角色编码';
COMMENT ON COLUMN sys_role.data_scope IS '数据权限档位：1 全部 / 2 本部门 / 3 本部门及下级 / 4 仅本人';
COMMENT ON COLUMN sys_role.built_in   IS '是否内置角色：0 否 / 1 是';
COMMENT ON COLUMN sys_role.status     IS '状态：0 正常 / 1 停用';

CREATE UNIQUE INDEX uk_sys_role_code ON sys_role (role_code) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_sys_role_name ON sys_role (role_name) WHERE deleted = 0;

-- ============ 4. 菜单/按钮表 sys_menu ============
CREATE TABLE sys_menu (
    id        BIGINT PRIMARY KEY,
    parent_id BIGINT       NOT NULL DEFAULT 0,
    menu_name VARCHAR(64)  NOT NULL,
    menu_type VARCHAR(8)   NOT NULL,
    order_num INT          NOT NULL DEFAULT 0,
    path      VARCHAR(128),
    component VARCHAR(255),
    perms     VARCHAR(128),
    icon      VARCHAR(64),
    visible   SMALLINT     NOT NULL DEFAULT 0,
    status    SMALLINT     NOT NULL DEFAULT 0,
    create_time TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by BIGINT,
    update_by BIGINT,
    deleted   SMALLINT     NOT NULL DEFAULT 0
);

COMMENT ON TABLE  sys_menu IS '菜单/按钮表';
COMMENT ON COLUMN sys_menu.menu_type IS '节点类型：D 目录 / M 菜单 / B 按钮';
COMMENT ON COLUMN sys_menu.path      IS '前端路由地址（目录/菜单填写）';
COMMENT ON COLUMN sys_menu.component IS '前端组件路径（菜单填写，相对 src/views 的组件标识）';
COMMENT ON COLUMN sys_menu.perms     IS '权限标识（按钮必填），全局唯一，形如 system:user:add';
COMMENT ON COLUMN sys_menu.visible   IS '是否可见：0 显示 / 1 隐藏（隐藏路由不注册、菜单不渲染）';
COMMENT ON COLUMN sys_menu.status    IS '状态：0 正常 / 1 停用';

CREATE UNIQUE INDEX uk_sys_menu_perms ON sys_menu (perms) WHERE deleted = 0 AND perms IS NOT NULL;
CREATE INDEX idx_sys_menu_parent ON sys_menu (parent_id);

-- ============ 5. 用户-角色关联表 sys_user_role ============
CREATE TABLE sys_user_role (
    id      BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by  BIGINT,
    update_by  BIGINT,
    deleted    SMALLINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_user_role IS '用户-角色关联表';

CREATE UNIQUE INDEX uk_sys_user_role ON sys_user_role (user_id, role_id) WHERE deleted = 0;
CREATE INDEX idx_sys_user_role_user ON sys_user_role (user_id);

-- ============ 6. 角色-菜单授权关联表 sys_role_menu ============
CREATE TABLE sys_role_menu (
    id      BIGINT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by  BIGINT,
    update_by  BIGINT,
    deleted    SMALLINT NOT NULL DEFAULT 0
);

COMMENT ON TABLE sys_role_menu IS '角色-菜单授权关联表';

CREATE UNIQUE INDEX uk_sys_role_menu ON sys_role_menu (role_id, menu_id) WHERE deleted = 0;
CREATE INDEX idx_sys_role_menu_role ON sys_role_menu (role_id);

-- =====================================================================
-- 初始化数据（全部固定主键，ON CONFLICT DO NOTHING 幂等）
-- =====================================================================

-- ---- 1. 内置角色（不可删除；SUPER_ADMIN 数据权限为「全部」） ----
INSERT INTO sys_role (id, role_code, role_name, data_scope, built_in, status, remark)
VALUES (1, 'SUPER_ADMIN', '超级管理员', 1, 1, 0, '系统内置，拥有全部功能权限与数据权限')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role (id, role_code, role_name, data_scope, built_in, status, remark)
VALUES (2, 'SYS_ADMIN', '系统管理员', 1, 1, 0, '系统内置，负责系统管理域日常维护')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role (id, role_code, role_name, data_scope, built_in, status, remark)
VALUES (3, 'DEPT_ADMIN', '部门管理员', 3, 1, 0, '系统内置，管理本部门及下级部门人员')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role (id, role_code, role_name, data_scope, built_in, status, remark)
VALUES (4, 'AUDITOR', '审计员', 1, 1, 0, '系统内置，只读查看全部数据')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_role (id, role_code, role_name, data_scope, built_in, status, remark)
VALUES (5, 'USER', '普通员工', 4, 1, 0, '系统内置，默认角色，仅本人数据')
ON CONFLICT (id) DO NOTHING;

-- ---- 2. 根部门 ----
INSERT INTO sys_dept (id, parent_id, ancestors, dept_name, order_num, status, source)
VALUES (100, 0, '0', 'Cacch 总部', 0, 0, 'LOCAL')
ON CONFLICT (id) DO NOTHING;

-- ---- 3. 超级管理员账号（初始密码明文见设计文档 3.4，交付临时值，上线前必须修改） ----
INSERT INTO sys_user (id, username, phone, password, real_name, dept_id, status, source)
VALUES (1, 'admin', '13800000000', '$2a$10$M7rqRqql7t64GM/JwTNKkuAMfTCBaZRrsLEC.dHwankYLIOXUilm2', '超级管理员', 100, 0, 'LOCAL')
ON CONFLICT (id) DO NOTHING;

INSERT INTO sys_user_role (id, user_id, role_id)
VALUES (1, 1, 1)
ON CONFLICT (id) DO NOTHING;

-- ---- 4. 预置菜单树（目录 D → 菜单 M → 按钮 B） ----
-- 首页
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, component, visible, status)
VALUES (1000, 0, '首页', 'M', 0, '/dashboard', 'workbench/Workbench', 0, 0)
ON CONFLICT (id) DO NOTHING;

-- 系统管理（目录）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, icon, visible, status)
VALUES (1100, 0, '系统管理', 'D', 1, '/system', NULL, 0, 0)
ON CONFLICT (id) DO NOTHING;

-- 用户管理（菜单 + 按钮组）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, component, visible, status)
VALUES (1110, 1100, '用户管理', 'M', 1, '/system/users', 'system/users/Users', 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1111, 1110, '用户查询', 'B', 1, 'system:user:list', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1112, 1110, '用户新增', 'B', 2, 'system:user:add', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1113, 1110, '用户编辑', 'B', 3, 'system:user:edit', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1114, 1110, '用户删除', 'B', 4, 'system:user:delete', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1115, 1110, '用户启停', 'B', 5, 'system:user:status', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1116, 1110, '重置密码', 'B', 6, 'system:user:resetPwd', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1117, 1110, '强制下线', 'B', 7, 'system:user:offline', 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 角色管理（菜单 + 按钮组）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, component, visible, status)
VALUES (1120, 1100, '角色管理', 'M', 2, '/system/roles', 'system/roles/Roles', 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1121, 1120, '角色查询', 'B', 1, 'system:role:list', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1122, 1120, '角色新增', 'B', 2, 'system:role:add', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1123, 1120, '角色编辑', 'B', 3, 'system:role:edit', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1124, 1120, '角色删除', 'B', 4, 'system:role:delete', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1125, 1120, '菜单授权', 'B', 5, 'system:role:grant', 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 菜单管理（菜单 + 按钮组）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, component, visible, status)
VALUES (1130, 1100, '菜单管理', 'M', 3, '/system/menus', 'system/menus/Menus', 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1131, 1130, '菜单查询', 'B', 1, 'system:menu:list', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1132, 1130, '菜单新增', 'B', 2, 'system:menu:add', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1133, 1130, '菜单编辑', 'B', 3, 'system:menu:edit', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1134, 1130, '菜单删除', 'B', 4, 'system:menu:delete', 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 部门管理（菜单 + 按钮组）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, component, visible, status)
VALUES (1140, 1100, '部门管理', 'M', 4, '/system/depts', 'system/depts/Depts', 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1141, 1140, '部门查询', 'B', 1, 'system:dept:list', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1142, 1140, '部门新增', 'B', 2, 'system:dept:add', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1143, 1140, '部门编辑', 'B', 3, 'system:dept:edit', 1, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1144, 1140, '部门删除', 'B', 4, 'system:dept:delete', 1, 0)
ON CONFLICT (id) DO NOTHING;

-- 个人中心（菜单 + 按钮组）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, component, visible, status)
VALUES (1200, 0, '个人中心', 'M', 2, '/profile', 'profile/Profile', 0, 0)
ON CONFLICT (id) DO NOTHING;
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, perms, visible, status)
VALUES (1201, 1200, '修改密码', 'B', 1, 'system:profile:resetPwd', 1, 0)
ON CONFLICT (id) DO NOTHING;

-- ---- 5. 角色授权初始化 ----
-- SUPER_ADMIN(1)：代码层全量放行，无需授权行。
-- SYS_ADMIN(2)：系统管理全量
INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (2101, 2, 1100), (2102, 2, 1110), (2103, 2, 1111), (2104, 2, 1112), (2105, 2, 1113),
       (2106, 2, 1114), (2107, 2, 1115), (2108, 2, 1116), (2109, 2, 1117),
       (2110, 2, 1120), (2111, 2, 1121), (2112, 2, 1122), (2113, 2, 1123), (2114, 2, 1124), (2115, 2, 1125),
       (2116, 2, 1130), (2117, 2, 1131), (2118, 2, 1132), (2119, 2, 1133), (2120, 2, 1134),
       (2121, 2, 1140), (2122, 2, 1141), (2123, 2, 1142), (2124, 2, 1143), (2125, 2, 1144)
ON CONFLICT (id) DO NOTHING;

-- DEPT_ADMIN(3)：用户管理（查询/新增/编辑/启停/重置密码，不含删除与强制下线、不可分配角色）
INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (2201, 3, 1100), (2202, 3, 1110), (2203, 3, 1111), (2204, 3, 1112),
       (2205, 3, 1113), (2206, 3, 1115), (2207, 3, 1116)
ON CONFLICT (id) DO NOTHING;

-- AUDITOR(4)：仅查询
INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (2301, 4, 1100), (2302, 4, 1110), (2303, 4, 1111), (2304, 4, 1120), (2305, 4, 1121),
       (2306, 4, 1130), (2307, 4, 1131), (2308, 4, 1140), (2309, 4, 1141)
ON CONFLICT (id) DO NOTHING;

-- USER(5)：首页 + 个人中心
INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (2401, 5, 1000), (2402, 5, 1200), (2403, 5, 1201)
ON CONFLICT (id) DO NOTHING;

-- 首页与个人中心补授权（SYS_ADMIN / DEPT_ADMIN / AUDITOR）
INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (2501, 2, 1000), (2502, 2, 1200), (2503, 2, 1201),
       (2504, 3, 1000), (2505, 3, 1200), (2506, 3, 1201),
       (2507, 4, 1200), (2508, 4, 1201)
ON CONFLICT (id) DO NOTHING;

-- =====================================================================
-- V6__sys_menu_keruyun_sync.sql
-- 新增「客如云订单同步」二级菜单（位于「客如云订单」上方），并授予 SYS_ADMIN 角色
-- 同时将「客如云订单」order_num 调整为 2，保证排序：客如云订单同步 → 客如云订单
-- =====================================================================

-- 客如云订单同步（二级菜单，component 对应 src/views/tianyuhe/KeruyunSyncTasks.vue）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, component, visible, status)
VALUES (1305, 1300, '客如云订单同步', 'M', 1, '/tianyuhe/keruyun-sync-tasks', 'tianyuhe/KeruyunSyncTasks', 0, 0)
ON CONFLICT (id) DO NOTHING;

-- 将客如云订单的排序调整为 2（原 order_num=1，现让同步菜单排在前面）
UPDATE sys_menu SET order_num = 2 WHERE id = 1310 AND order_num = 1;

-- SYS_ADMIN(role_id=2) 授权：客如云订单同步菜单
INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (2603, 2, 1305)
ON CONFLICT (id) DO NOTHING;

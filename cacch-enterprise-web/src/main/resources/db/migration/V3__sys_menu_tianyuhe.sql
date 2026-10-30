-- =====================================================================
-- V3__sys_menu_tianyuhe.sql
-- 新增「田裕禾」一级目录及「客如云订单」二级菜单，并授予 SYS_ADMIN 角色
-- 幂等策略：PostgreSQL 12 禁用 MERGE，统一使用 INSERT ... ON CONFLICT DO NOTHING
-- ID 段说明：1300 段为田裕禾业务域菜单，与 V2 的 1000/1100/1200 段及 2100-2599 授权段无冲突
-- =====================================================================

-- 田裕禾（一级目录）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, visible, status)
VALUES (1300, 0, '田裕禾', 'D', 3, '/tianyuhe', 0, 0)
ON CONFLICT (id) DO NOTHING;

-- 客如云订单（二级菜单，component 对应 src/views/tianyuhe/KeruyunOrders.vue）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_type, order_num, path, component, visible, status)
VALUES (1310, 1300, '客如云订单', 'M', 1, '/tianyuhe/keruyun-orders', 'tianyuhe/KeruyunOrders', 0, 0)
ON CONFLICT (id) DO NOTHING;

-- SYS_ADMIN(role_id=2) 授权：田裕禾目录 + 客如云订单菜单
INSERT INTO sys_role_menu (id, role_id, menu_id)
VALUES (2601, 2, 1300), (2602, 2, 1310)
ON CONFLICT (id) DO NOTHING;

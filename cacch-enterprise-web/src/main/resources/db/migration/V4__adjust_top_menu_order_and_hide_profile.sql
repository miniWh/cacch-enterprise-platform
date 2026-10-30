-- =====================================================================
-- V4 菜单调整：一级菜单排序 + 隐藏「个人中心」一级菜单
-- ---------------------------------------------------------------------
-- 前置：V3__sys_menu_tianyuhe.sql 已补录田裕禾目录(1300, order_num=3)及
--       客如云订单菜单(1310)，并完成 SYS_ADMIN 授权。
-- 1. 一级菜单排序调整为：首页(1000)=0 → 田裕禾(1300)=1 → 系统管理(1100)=2；
--    首页 order_num 已为 0，无需更新。
-- 2. 「个人中心」(1200) 入口改为侧边栏底部当前用户区跳转（前端静态注册 /profile
--    路由），此处仅隐藏菜单不再作为一级导航展示；保留其下「修改密码」按钮(1201)
--    及角色授权关系，普通员工修改密码权限不受影响。
-- 幂等性：均为 UPDATE 语句，可重复执行。
-- =====================================================================

UPDATE sys_menu
SET order_num = 1, update_time = CURRENT_TIMESTAMP
WHERE id = 1300 AND deleted = 0;

UPDATE sys_menu
SET order_num = 2, update_time = CURRENT_TIMESTAMP
WHERE id = 1100 AND deleted = 0;

UPDATE sys_menu
SET visible = 1, update_time = CURRENT_TIMESTAMP
WHERE id = 1200 AND deleted = 0;

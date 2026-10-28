package com.cacch.enterprise.common.constant.system;

/**
 * 系统管理域权限标识常量
 *
 * <p>命名规范 {@code {biz}:{资源}:{操作}}，全小写，与 sys_menu 表预置按钮的 perms 字段一一对应；
 * Controller 通过 {@code @RequiresPermission} 引用，前端通过 v-permission 指令引用。
 *
 * @author hongfu_zhou@cacch.com
 */
public final class SystemPermissionConstants {

    private SystemPermissionConstants() {
    }

    /**
     * 用户管理：查询
     */
    public static final String USER_LIST = "system:user:list";

    /**
     * 用户管理：新增
     */
    public static final String USER_ADD = "system:user:add";

    /**
     * 用户管理：编辑
     */
    public static final String USER_EDIT = "system:user:edit";

    /**
     * 用户管理：删除
     */
    public static final String USER_DELETE = "system:user:delete";

    /**
     * 用户管理：启用/停用
     */
    public static final String USER_STATUS = "system:user:status";

    /**
     * 用户管理：重置密码
     */
    public static final String USER_RESET_PWD = "system:user:resetPwd";

    /**
     * 用户管理：在线用户查看与强制下线
     */
    public static final String USER_OFFLINE = "system:user:offline";

    /**
     * 角色管理：查询
     */
    public static final String ROLE_LIST = "system:role:list";

    /**
     * 角色管理：新增
     */
    public static final String ROLE_ADD = "system:role:add";

    /**
     * 角色管理：编辑
     */
    public static final String ROLE_EDIT = "system:role:edit";

    /**
     * 角色管理：删除
     */
    public static final String ROLE_DELETE = "system:role:delete";

    /**
     * 角色管理：菜单授权
     */
    public static final String ROLE_GRANT = "system:role:grant";


    /**
     * 菜单管理：查询
     */
    public static final String MENU_LIST = "system:menu:list";

    /**
     * 菜单管理：新增
     */
    public static final String MENU_ADD = "system:menu:add";

    /**
     * 菜单管理：编辑
     */
    public static final String MENU_EDIT = "system:menu:edit";

    /**
     * 菜单管理：删除
     */
    public static final String MENU_DELETE = "system:menu:delete";

    /**
     * 部门管理：查询
     */
    public static final String DEPT_LIST = "system:dept:list";

    /**
     * 部门管理：新增
     */
    public static final String DEPT_ADD = "system:dept:add";

    /**
     * 部门管理：编辑
     */
    public static final String DEPT_EDIT = "system:dept:edit";

    /**
     * 部门管理：删除
     */
    public static final String DEPT_DELETE = "system:dept:delete";

    /**
     * 个人中心：修改密码（所有登录用户）
     */
    public static final String PROFILE_RESET_PWD = "system:profile:resetPwd";
}

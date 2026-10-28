package com.cacch.enterprise.common.constant.system;

/**
 * 系统管理业务域接口 PATH 常量
 *
 * @author hongfu_zhou@cacch.com
 */
public final class SystemPathConstants {

    private SystemPathConstants() {
    }

    /** 用户管理根路径 */
    public static final String PATH_USERS = "/api/v1/system/users";
    /** 在线用户子路径 */
    public static final String PATH_USERS_ONLINE = "/api/v1/system/users/online";
    /** 角色管理根路径 */
    public static final String PATH_ROLES = "/api/v1/system/roles";
    /** 菜单管理根路径 */
    public static final String PATH_MENUS = "/api/v1/system/menus";
    /** 菜单树子路径 */
    public static final String PATH_MENUS_TREE = "/api/v1/system/menus/tree";
    /** 部门管理根路径 */
    public static final String PATH_DEPTS = "/api/v1/system/depts";
    /** 部门树子路径 */
    public static final String PATH_DEPTS_TREE = "/api/v1/system/depts/tree";
}

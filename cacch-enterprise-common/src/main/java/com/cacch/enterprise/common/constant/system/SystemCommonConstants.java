package com.cacch.enterprise.common.constant.system;

import com.cacch.enterprise.common.constant.redis.RedisConstants;

/**
 * 系统管理业务域公共常量
 *
 * <p>承载内置角色编码、权限缓存 Key 组装方法等系统管理域跨接口共享常量。
 *
 * @author hongfu_zhou@cacch.com
 */
public final class SystemCommonConstants {

    private SystemCommonConstants() {
    }

    /**
     * 内置角色编码：超级管理员（拥有全部权限与数据权限，代码层全量放行）
     */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    /**
     * 内置角色编码：系统管理员
     */
    public static final String ROLE_SYS_ADMIN = "SYS_ADMIN";

    /**
     * 内置角色编码：部门管理员
     */
    public static final String ROLE_DEPT_ADMIN = "DEPT_ADMIN";

    /**
     * 内置角色编码：审计员
     */
    public static final String ROLE_AUDITOR = "AUDITOR";

    /**
     * 内置角色编码：普通员工
     */
    public static final String ROLE_USER = "USER";

    /**
     * 权限缓存 TTL（秒），与访问令牌有效期一致（12 小时）
     */
    public static final int PERMISSION_CACHE_TTL_SECONDS = 12 * 3600;

    /**
     * 组装用户权限标识集合缓存 Key（与登录态分离缓存，授权变更后 evict）
     *
     * @param userId 用户 ID
     * @return 形如 enterprise:auth:perms:{userId} 的 Key
     */
    public static String userPermsRedisKey(Long userId) {
        return RedisConstants.KEY_PREFIX + "auth:perms:" + userId;
    }

    /**
     * 组装用户动态菜单树缓存 Key（与权限标识缓存同步 evict）
     *
     * @param userId 用户 ID
     * @return 形如 enterprise:auth:routes:{userId} 的 Key
     */
    public static String userRoutesRedisKey(Long userId) {
        return RedisConstants.KEY_PREFIX + "auth:routes:" + userId;
    }
}

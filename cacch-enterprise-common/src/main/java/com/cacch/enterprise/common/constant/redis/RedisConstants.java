package com.cacch.enterprise.common.constant.redis;

/**
 * Redis 通用常量
 *
 * <p>所有业务 Redis Key 统一以 {@link #KEY_PREFIX} 开头，命名规则 {@code enterprise:{domain}:{identifier}}；
 * 具体 Key 组装方法定义在各业务域常量类中（如 AuthCommonConstants、SystemCommonConstants）。
 *
 * @author hongfu_zhou@cacch.com
 */
public final class RedisConstants {

    private RedisConstants() {
    }

    /** Redis Key 全局前缀 */
    public static final String KEY_PREFIX = "enterprise:";
}

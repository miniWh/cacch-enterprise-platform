package com.cacch.enterprise.manager.auth.api;

import com.cacch.enterprise.service.system.model.UserPermissionModel;

import java.util.Collection;

/**
 * 权限缓存编排接口（Redis 缓存读写 + 数据库回源）
 *
 * <p>权限数据（perms/routes）与登录态分离缓存：授权变更后按用户 evict，
 * 下次请求回源重建，用户无需重新登录。
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IAuthPermissionManager {

    /**
     * 加载用户权限（优先 Redis 缓存，未命中时回源并写缓存，TTL 与访问令牌一致）
     *
     * @param userId 用户主键
     * @return 权限模型（菜单扁平列表 + 权限标识集合）
     */
    UserPermissionModel loadWithCache(Long userId);

    /**
     * 清除指定用户的权限缓存（授权变更后由系统管理编排调用）
     *
     * @param userIds 用户 ID 集合，空集合时不处理
     */
    void evictUsers(Collection<Long> userIds);
}

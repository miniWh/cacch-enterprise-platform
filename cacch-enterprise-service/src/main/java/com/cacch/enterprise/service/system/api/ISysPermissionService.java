package com.cacch.enterprise.service.system.api;

import com.cacch.enterprise.service.system.model.UserPermissionModel;

/**
 * 权限加载服务接口（用户 → 角色 → 菜单回源）
 *
 * <p>仅负责从数据库回源构建权限模型，缓存读写由 Manager 编排。
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISysPermissionService {

    /**
     * 加载用户权限模型（SUPER_ADMIN 全量放行；其余按启用角色关联菜单回源）
     *
     * @param userId 用户主键
     * @return 权限模型（启用状态菜单扁平列表 + 按钮权限标识集合），无角色时为空模型
     */
    UserPermissionModel loadUserPermission(Long userId);
}

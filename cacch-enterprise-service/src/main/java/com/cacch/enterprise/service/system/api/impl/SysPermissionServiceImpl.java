package com.cacch.enterprise.service.system.api.impl;

import com.cacch.enterprise.common.constant.system.SystemCommonConstants;
import com.cacch.enterprise.dao.entity.system.SysMenuDO;
import com.cacch.enterprise.dao.entity.system.SysRoleDO;
import com.cacch.enterprise.service.system.api.ISysMenuService;
import com.cacch.enterprise.service.system.api.ISysPermissionService;
import com.cacch.enterprise.service.system.api.ISysRoleService;
import com.cacch.enterprise.service.system.model.UserPermissionModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限加载服务实现（用户 → 角色 → 菜单回源，缓存由 Manager 编排）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionServiceImpl implements ISysPermissionService {

    private final ISysRoleService sysRoleService;
    private final ISysMenuService sysMenuService;

    /**
     * 加载用户权限模型（SUPER_ADMIN 全量放行；其余按启用角色关联菜单回源）
     *
     * @param userId 用户主键
     * @return 权限模型；无启用角色时为空模型（菜单与权限标识均为空）
     */
    @Override
    public UserPermissionModel loadUserPermission(Long userId) {
        List<SysRoleDO> roles = sysRoleService.listEnabledByUserId(userId);
        if (roles.isEmpty()) {
            log.info("【SysPermission】用户无启用角色，权限为空，userId={}", userId);
            return new UserPermissionModel(List.of(), Set.of());
        }
        boolean superAdmin = roles.stream()
                .anyMatch(r -> SystemCommonConstants.ROLE_SUPER_ADMIN.equals(r.getRoleCode()));
        List<SysMenuDO> menus = superAdmin
                ? sysMenuService.listAllEnabled()
                : sysMenuService.listEnabledByRoleIds(roles.stream().map(SysRoleDO::getId).toList());
        Set<String> perms = menus.stream()
                .filter(m -> "B".equals(m.getMenuType()))
                .filter(m -> m.getPerms() != null && !m.getPerms().isBlank())
                .map(SysMenuDO::getPerms)
                .collect(Collectors.toSet());
        log.info("【SysPermission】加载用户权限完成，userId={}, superAdmin={}, menus={}, perms={}",
                userId, superAdmin, menus.size(), perms.size());
        return new UserPermissionModel(menus, perms);
    }
}

package com.cacch.enterprise.manager.system.api;

import com.cacch.enterprise.manager.system.model.RoleSaveCommand;

import java.util.List;

/**
 * 角色管理编排接口（角色 + 菜单授权跨聚合；授权变更后 evict 关联用户权限缓存）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISysRoleManager {

    /**
     * 新增角色
     *
     * @param command 角色命令
     * @return 新角色 ID
     * @throws BizException 编码/名称重复或编码格式非法时抛出
     */
    Long createRole(RoleSaveCommand command);

    /**
     * 编辑角色（内置角色编码/名称不可修改，SUPER_ADMIN 数据权限档位不可修改）
     *
     * @param roleId  角色主键
     * @param command 角色命令
     * @throws BizException 角色不存在、内置保护或编码/名称重复时抛出
     */
    void updateRole(Long roleId, RoleSaveCommand command);

    /**
     * 逻辑删除角色（内置角色或已分配用户的角色禁止删除；同时清空菜单授权并 evict 关联用户缓存）
     *
     * @param roleId 角色主键
     * @throws BizException 角色不存在、内置保护或已分配用户时抛出
     */
    void deleteRole(Long roleId);

    /**
     * 重新授权角色菜单（旧关联逻辑删除后插入新关联，evict 关联用户权限缓存）
     *
     * @param roleId  角色主键
     * @param menuIds 目标菜单/按钮 ID 列表（空列表表示清空授权）
     * @throws BizException 角色或菜单不存在时抛出
     */
    void grantMenus(Long roleId, List<Long> menuIds);
}

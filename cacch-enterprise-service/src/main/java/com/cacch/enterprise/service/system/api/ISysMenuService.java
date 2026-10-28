package com.cacch.enterprise.service.system.api;

import com.cacch.enterprise.dao.entity.system.SysMenuDO;

import java.util.List;

/**
 * 菜单/按钮服务接口（sys_menu 聚合）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISysMenuService {

    /**
     * 查询全部未删除菜单/按钮（含停用，供菜单管理树展示），按 orderNum、id 升序
     *
     * @return 菜单扁平列表
     */
    List<SysMenuDO> listAll();

    /**
     * 查询全部启用状态菜单/按钮（权限回源用），按 orderNum、id 升序
     *
     * @return 启用状态菜单扁平列表
     */
    List<SysMenuDO> listAllEnabled();

    /**
     * 按 ID 查询菜单（仅未删除）
     *
     * @param menuId 菜单主键
     * @return 菜单实体；不存在时返回 null
     */
    SysMenuDO getById(Long menuId);

    /**
     * 按角色 ID 集合查询关联的启用状态菜单（含目录/菜单/按钮）
     *
     * @param roleIds 角色 ID 集合，为空时返回空列表
     * @return 去重后的菜单扁平列表
     */
    List<SysMenuDO> listEnabledByRoleIds(List<Long> roleIds);

    /**
     * 校验菜单是否存在子节点（删除前置校验）
     *
     * @param menuId 菜单主键
     * @return 存在子节点返回 true
     */
    boolean hasChildren(Long menuId);

    /**
     * 校验权限标识是否已存在
     *
     * @param perms         权限标识
     * @param excludeMenuId 排除的菜单 ID（编辑场景传本人），可空
     * @return 已存在返回 true
     */
    boolean existsPerms(String perms, Long excludeMenuId);

    /**
     * 新增菜单/按钮（参与调用方事务）
     *
     * @param menu 菜单实体
     */
    void createMenu(SysMenuDO menu);

    /**
     * 编辑菜单/按钮（按非空字段更新，参与调用方事务）
     *
     * @param menu 菜单实体（仅携带需更新字段与主键）
     */
    void updateMenu(SysMenuDO menu);

    /**
     * 逻辑删除菜单/按钮（参与调用方事务）
     *
     * @param menuId 菜单主键
     */
    void deleteMenu(Long menuId);
}

package com.cacch.enterprise.service.system.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.enterprise.dao.entity.system.SysRoleDO;

import java.util.List;

/**
 * 角色服务接口（sys_role / sys_role_menu 聚合）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISysRoleService {

    /**
     * 分页查询角色列表（按角色名称或编码模糊）
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @param keyword  关键词（匹配角色名称或编码），可空
     * @return 角色分页数据
     */
    IPage<SysRoleDO> pageRoles(long pageNum, long pageSize, String keyword);

    /**
     * 按 ID 查询角色（仅未删除）
     *
     * @param roleId 角色主键
     * @return 角色实体；不存在时返回 null
     */
    SysRoleDO getById(Long roleId);

    /**
     * 查询用户持有的全部启用状态角色
     *
     * @param userId 用户主键
     * @return 启用状态角色列表，无则空列表
     */
    List<SysRoleDO> listEnabledByUserId(Long userId);

    /**
     * 查询全部启用状态角色（用户分配角色下拉）
     *
     * @return 启用状态角色列表
     */
    List<SysRoleDO> listEnabled();

    /**
     * 校验角色编码是否已存在
     *
     * @param roleCode 角色编码
     * @param excludeRoleId 排除的角色 ID（编辑场景传本人），可空
     * @return 已存在返回 true
     */
    boolean existsCode(String roleCode, Long excludeRoleId);

    /**
     * 校验角色名称是否已存在
     *
     * @param roleName 角色名称
     * @param excludeRoleId 排除的角色 ID（编辑场景传本人），可空
     * @return 已存在返回 true
     */
    boolean existsName(String roleName, Long excludeRoleId);

    /**
     * 校验角色是否已分配用户（角色删除前置校验）
     *
     * @param roleId 角色主键
     * @return 已分配用户返回 true
     */
    boolean hasUsers(Long roleId);

    /**
     * 新增角色（参与调用方事务）
     *
     * @param role 角色实体
     */
    void createRole(SysRoleDO role);

    /**
     * 编辑角色（按非空字段更新，参与调用方事务）
     *
     * @param role 角色实体（仅携带需更新字段与主键）
     */
    void updateRole(SysRoleDO role);

    /**
     * 逻辑删除角色（参与调用方事务）
     *
     * @param roleId 角色主键
     */
    void deleteRole(Long roleId);

    /**
     * 查询角色已授权的菜单/按钮 ID 列表
     *
     * @param roleId 角色主键
     * @return 菜单 ID 列表，无则空列表
     */
    List<Long> listMenuIdsByRoleId(Long roleId);

    /**
     * 重新授权角色菜单（旧关联逻辑删除后插入新关联，参与调用方事务）
     *
     * @param roleId  角色主键
     * @param menuIds 目标菜单/按钮 ID 列表，空列表表示清空授权
     */
    void replaceRoleMenus(Long roleId, List<Long> menuIds);

    /**
     * 校验菜单是否已被任意角色授权（菜单删除前置校验）
     *
     * @param menuId 菜单主键
     * @return 已被授权返回 true
     */
    boolean isMenuReferenced(Long menuId);
}

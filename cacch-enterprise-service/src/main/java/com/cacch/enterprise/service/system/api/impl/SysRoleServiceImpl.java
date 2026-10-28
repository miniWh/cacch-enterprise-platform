package com.cacch.enterprise.service.system.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cacch.enterprise.common.utils.StringUtils;
import com.cacch.enterprise.dao.entity.system.SysRoleDO;
import com.cacch.enterprise.dao.entity.system.SysRoleMenuDO;
import com.cacch.enterprise.dao.entity.system.SysUserRoleDO;
import com.cacch.enterprise.dao.mapper.system.SysRoleMapper;
import com.cacch.enterprise.dao.mapper.system.SysRoleMenuMapper;
import com.cacch.enterprise.dao.mapper.system.SysUserRoleMapper;
import com.cacch.enterprise.service.system.api.ISysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 角色服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements ISysRoleService {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /**
     * 分页查询角色列表（名称/编码模糊）
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  关键词，可空
     * @return 角色分页数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10)
    public IPage<SysRoleDO> pageRoles(long pageNum, long pageSize, String keyword) {
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<SysRoleDO>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysRoleDO::getRoleCode, keyword).or().like(SysRoleDO::getRoleName, keyword));
        }
        wrapper.orderByDesc(SysRoleDO::getCreateTime);
        return sysRoleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 按 ID 查询角色（仅未删除）
     *
     * @param roleId 角色主键
     * @return 角色实体；不存在时返回 null
     */
    @Override
    public SysRoleDO getById(Long roleId) {
        return sysRoleMapper.selectById(roleId);
    }

    /**
     * 查询用户持有的全部启用状态角色
     *
     * @param userId 用户主键
     * @return 启用状态角色列表
     */
    @Override
    public List<SysRoleDO> listEnabledByUserId(Long userId) {
        List<Long> roleIds = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleDO>()
                        .eq(SysUserRoleDO::getUserId, userId))
                .stream().map(SysUserRoleDO::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleDO>()
                .in(SysRoleDO::getId, roleIds)
                .eq(SysRoleDO::getStatus, 0));
    }

    /**
     * 查询全部启用状态角色
     *
     * @return 启用状态角色列表
     */
    @Override
    public List<SysRoleDO> listEnabled() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getStatus, 0)
                .orderByAsc(SysRoleDO::getId));
    }

    /**
     * 校验角色编码是否已存在
     *
     * @param roleCode      角色编码
     * @param excludeRoleId 排除的角色 ID
     * @return 已存在返回 true
     */
    @Override
    public boolean existsCode(String roleCode, Long excludeRoleId) {
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getRoleCode, roleCode);
        if (excludeRoleId != null) {
            wrapper.ne(SysRoleDO::getId, excludeRoleId);
        }
        return sysRoleMapper.selectCount(wrapper) > 0;
    }

    /**
     * 校验角色名称是否已存在
     *
     * @param roleName      角色名称
     * @param excludeRoleId 排除的角色 ID
     * @return 已存在返回 true
     */
    @Override
    public boolean existsName(String roleName, Long excludeRoleId) {
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<SysRoleDO>()
                .eq(SysRoleDO::getRoleName, roleName);
        if (excludeRoleId != null) {
            wrapper.ne(SysRoleDO::getId, excludeRoleId);
        }
        return sysRoleMapper.selectCount(wrapper) > 0;
    }

    /**
     * 校验角色是否已分配用户
     *
     * @param roleId 角色主键
     * @return 已分配用户返回 true
     */
    @Override
    public boolean hasUsers(Long roleId) {
        return sysUserRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRoleDO>()
                .eq(SysUserRoleDO::getRoleId, roleId)) > 0;
    }

    /**
     * 新增角色（参与调用方事务）
     *
     * @param role 角色实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void createRole(SysRoleDO role) {
        sysRoleMapper.insert(role);
        log.info("【SysRole】新增角色，roleId={}, roleCode={}", role.getId(), role.getRoleCode());
    }

    /**
     * 编辑角色（按非空字段更新，参与调用方事务）
     *
     * @param role 角色实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void updateRole(SysRoleDO role) {
        sysRoleMapper.updateById(role);
        log.info("【SysRole】编辑角色，roleId={}", role.getId());
    }

    /**
     * 逻辑删除角色（参与调用方事务）
     *
     * @param roleId 角色主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void deleteRole(Long roleId) {
        sysRoleMapper.deleteById(roleId);
        log.info("【SysRole】逻辑删除角色，roleId={}", roleId);
    }

    /**
     * 查询角色已授权的菜单/按钮 ID 列表
     *
     * @param roleId 角色主键
     * @return 菜单 ID 列表，无则空列表
     */
    @Override
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenuDO>()
                        .eq(SysRoleMenuDO::getRoleId, roleId))
                .stream().map(SysRoleMenuDO::getMenuId).toList();
    }

    /**
     * 重新授权角色菜单（旧关联逻辑删除后插入新关联）
     *
     * @param roleId  角色主键
     * @param menuIds 目标菜单 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenuDO>()
                .eq(SysRoleMenuDO::getRoleId, roleId));
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds.stream().distinct().toList()) {
                SysRoleMenuDO relation = new SysRoleMenuDO();
                relation.setRoleId(roleId);
                relation.setMenuId(menuId);
                sysRoleMenuMapper.insert(relation);
            }
        }
        log.info("【SysRole】重新授权角色菜单，roleId={}, menuIds={}", roleId, menuIds);
    }

    /**
     * 校验菜单是否已被任意角色授权
     *
     * @param menuId 菜单主键
     * @return 已被授权返回 true
     */
    @Override
    public boolean isMenuReferenced(Long menuId) {
        return sysRoleMenuMapper.selectCount(new LambdaQueryWrapper<SysRoleMenuDO>()
                .eq(SysRoleMenuDO::getMenuId, menuId)) > 0;
    }
}

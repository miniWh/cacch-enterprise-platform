package com.cacch.enterprise.service.system.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.enterprise.dao.entity.system.SysMenuDO;
import com.cacch.enterprise.dao.entity.system.SysRoleMenuDO;
import com.cacch.enterprise.dao.mapper.system.SysMenuMapper;
import com.cacch.enterprise.dao.mapper.system.SysRoleMenuMapper;
import com.cacch.enterprise.service.system.api.ISysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜单/按钮服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    /**
     * 查询全部未删除菜单/按钮（含停用，供管理树展示）
     *
     * @return 菜单扁平列表（orderNum、id 升序）
     */
    @Override
    public List<SysMenuDO> listAll() {
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                .orderByAsc(SysMenuDO::getOrderNum)
                .orderByAsc(SysMenuDO::getId));
    }

    /**
     * 查询全部启用状态菜单/按钮（权限回源用）
     *
     * @return 启用状态菜单扁平列表
     */
    @Override
    public List<SysMenuDO> listAllEnabled() {
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                .eq(SysMenuDO::getStatus, 0)
                .orderByAsc(SysMenuDO::getOrderNum)
                .orderByAsc(SysMenuDO::getId));
    }

    /**
     * 按 ID 查询菜单（仅未删除）
     *
     * @param menuId 菜单主键
     * @return 菜单实体；不存在时返回 null
     */
    @Override
    public SysMenuDO getById(Long menuId) {
        return sysMenuMapper.selectById(menuId);
    }

    /**
     * 按角色 ID 集合查询关联的启用状态菜单
     *
     * @param roleIds 角色 ID 集合
     * @return 去重后的菜单扁平列表；角色集合为空时返回空列表
     */
    @Override
    public List<SysMenuDO> listEnabledByRoleIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        List<Long> menuIds = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenuDO>()
                        .in(SysRoleMenuDO::getRoleId, roleIds))
                .stream().map(SysRoleMenuDO::getMenuId).distinct().toList();
        if (menuIds.isEmpty()) {
            return List.of();
        }
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenuDO>()
                .in(SysMenuDO::getId, menuIds)
                .eq(SysMenuDO::getStatus, 0)
                .orderByAsc(SysMenuDO::getOrderNum)
                .orderByAsc(SysMenuDO::getId));
    }

    /**
     * 校验菜单是否存在子节点
     *
     * @param menuId 菜单主键
     * @return 存在子节点返回 true
     */
    @Override
    public boolean hasChildren(Long menuId) {
        return sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenuDO>()
                .eq(SysMenuDO::getParentId, menuId)) > 0;
    }

    /**
     * 校验权限标识是否已存在
     *
     * @param perms         权限标识
     * @param excludeMenuId 排除的菜单 ID
     * @return 已存在返回 true
     */
    @Override
    public boolean existsPerms(String perms, Long excludeMenuId) {
        LambdaQueryWrapper<SysMenuDO> wrapper = new LambdaQueryWrapper<SysMenuDO>()
                .eq(SysMenuDO::getPerms, perms);
        if (excludeMenuId != null) {
            wrapper.ne(SysMenuDO::getId, excludeMenuId);
        }
        return sysMenuMapper.selectCount(wrapper) > 0;
    }

    /**
     * 新增菜单/按钮（参与调用方事务）
     *
     * @param menu 菜单实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public void createMenu(SysMenuDO menu) {
        sysMenuMapper.insert(menu);
        log.info("【SysMenu】新增菜单，menuId={}, menuName={}, type={}", menu.getId(), menu.getMenuName(), menu.getMenuType());
    }

    /**
     * 编辑菜单/按钮（按非空字段更新，参与调用方事务）
     *
     * @param menu 菜单实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public void updateMenu(SysMenuDO menu) {
        sysMenuMapper.updateById(menu);
        log.info("【SysMenu】编辑菜单，menuId={}", menu.getId());
    }

    /**
     * 逻辑删除菜单/按钮（参与调用方事务）
     *
     * @param menuId 菜单主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, timeout = 30)
    public void deleteMenu(Long menuId) {
        sysMenuMapper.deleteById(menuId);
        log.info("【SysMenu】逻辑删除菜单，menuId={}", menuId);
    }
}

package com.cacch.enterprise.manager.system.api.impl;

import com.cacch.enterprise.common.constant.system.SystemCommonConstants;
import com.cacch.enterprise.common.enums.system.DataScopeEnum;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.dao.entity.system.SysMenuDO;
import com.cacch.enterprise.dao.entity.system.SysRoleDO;
import com.cacch.enterprise.manager.auth.api.IAuthPermissionManager;
import com.cacch.enterprise.manager.system.api.ISysRoleManager;
import com.cacch.enterprise.manager.system.model.RoleSaveCommand;
import com.cacch.enterprise.service.system.api.ISysMenuService;
import com.cacch.enterprise.service.system.api.ISysRoleService;
import com.cacch.enterprise.service.system.api.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 角色管理编排实现（角色 + 菜单授权 + 关联用户权限缓存 evict）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleManagerImpl implements ISysRoleManager {

    /** 角色编码格式：大写字母开头，仅含大写字母/数字/下划线，2~64 位 */
    private static final Pattern ROLE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

    private final ISysRoleService sysRoleService;
    private final ISysMenuService sysMenuService;
    private final ISysUserService sysUserService;
    private final IAuthPermissionManager authPermissionManager;

    /**
     * 新增角色
     *
     * @param command 角色命令
     * @return 新角色 ID
     * @throws BizException 编码/名称重复或编码格式非法时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public Long createRole(RoleSaveCommand command) {
        assertCodeFormat(command.getRoleCode());
        if (sysRoleService.existsCode(command.getRoleCode(), null)) {
            log.info("【SysRole】新增角色终止：角色编码已存在，roleCode={}", command.getRoleCode());
            throw new BizException(ResultCode.SYSTEM_ROLE_CODE_DUPLICATE);
        }
        if (sysRoleService.existsName(command.getRoleName(), null)) {
            log.info("【SysRole】新增角色终止：角色名称已存在，roleName={}", command.getRoleName());
            throw new BizException(ResultCode.SYSTEM_ROLE_NAME_DUPLICATE);
        }
        assertDataScopeValid(command.getDataScope());
        SysRoleDO role = new SysRoleDO();
        role.setRoleCode(command.getRoleCode());
        role.setRoleName(command.getRoleName());
        role.setDataScope(command.getDataScope());
        role.setBuiltIn(0);
        role.setStatus(command.getStatus() == null ? 0 : command.getStatus());
        role.setRemark(command.getRemark());
        sysRoleService.createRole(role);
        log.info("【SysRole】新增角色成功，roleId={}, roleCode={}", role.getId(), role.getRoleCode());
        return role.getId();
    }

    /**
     * 编辑角色（内置角色编码/名称不可修改；SUPER_ADMIN 数据权限档位不可修改）
     *
     * @param roleId  角色主键
     * @param command 角色命令
     * @throws BizException 角色不存在、内置保护或编码/名称重复时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void updateRole(Long roleId, RoleSaveCommand command) {
        SysRoleDO existing = requireRole(roleId);
        boolean builtIn = existing.getBuiltIn() != null && existing.getBuiltIn() == 1;
        if (builtIn && (command.getRoleCode() != null && !command.getRoleCode().equals(existing.getRoleCode())
                || command.getRoleName() != null && !command.getRoleName().equals(existing.getRoleName()))) {
            log.info("【SysRole】编辑角色终止：内置角色编码/名称不可修改，roleId={}", roleId);
            throw new BizException(ResultCode.SYSTEM_ROLE_PROTECTED, "内置角色的编码与名称不可修改");
        }
        boolean superAdmin = SystemCommonConstants.ROLE_SUPER_ADMIN.equals(existing.getRoleCode());
        if (superAdmin && command.getDataScope() != null && command.getDataScope() != DataScopeEnum.ALL.getCode()) {
            log.info("【SysRole】编辑角色终止：SUPER_ADMIN 数据权限档位不可修改，roleId={}", roleId);
            throw new BizException(ResultCode.SYSTEM_ROLE_PROTECTED, "超级管理员的数据权限档位不可修改");
        }
        if (command.getRoleName() != null && sysRoleService.existsName(command.getRoleName(), roleId)) {
            log.info("【SysRole】编辑角色终止：角色名称已存在，roleName={}", command.getRoleName());
            throw new BizException(ResultCode.SYSTEM_ROLE_NAME_DUPLICATE);
        }
        if (command.getDataScope() != null) {
            assertDataScopeValid(command.getDataScope());
        }
        SysRoleDO update = new SysRoleDO();
        update.setId(roleId);
        update.setRoleName(command.getRoleName());
        update.setDataScope(command.getDataScope());
        update.setStatus(command.getStatus());
        update.setRemark(command.getRemark());
        sysRoleService.updateRole(update);
        registerAfterCommitEvict(roleId);
        log.info("【SysRole】编辑角色成功，roleId={}", roleId);
    }

    /**
     * 逻辑删除角色（内置/已分配用户禁止；事务提交后清空授权并 evict 关联用户缓存）
     *
     * @param roleId 角色主键
     * @throws BizException 角色不存在、内置保护或已分配用户时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void deleteRole(Long roleId) {
        SysRoleDO existing = requireRole(roleId);
        if (existing.getBuiltIn() != null && existing.getBuiltIn() == 1) {
            log.info("【SysRole】删除角色终止：内置角色禁止删除，roleId={}, roleCode={}", roleId, existing.getRoleCode());
            throw new BizException(ResultCode.SYSTEM_ROLE_PROTECTED, "内置角色禁止删除");
        }
        if (sysRoleService.hasUsers(roleId)) {
            log.info("【SysRole】删除角色终止：角色已分配用户，roleId={}", roleId);
            throw new BizException(ResultCode.SYSTEM_ROLE_HAS_USERS);
        }
        List<Long> userIds = sysUserService.listUserIdsByRoleId(roleId);
        sysRoleService.deleteRole(roleId);
        sysRoleService.replaceRoleMenus(roleId, List.of());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                authPermissionManager.evictUsers(userIds);
            }
        });
        log.info("【SysRole】删除角色成功，roleId={}, affectedUsers={}", roleId, userIds.size());
    }

    /**
     * 重新授权角色菜单（事务提交后 evict 关联用户权限缓存）
     *
     * @param roleId  角色主键
     * @param menuIds 目标菜单/按钮 ID 列表
     * @throws BizException 角色或菜单不存在时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void grantMenus(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                SysMenuDO menu = sysMenuService.getById(menuId);
                if (menu == null) {
                    log.info("【SysRole】授权终止：菜单不存在，menuId={}", menuId);
                    throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "菜单不存在：menuId=" + menuId);
                }
            }
        }
        List<Long> userIds = sysUserService.listUserIdsByRoleId(roleId);
        sysRoleService.replaceRoleMenus(roleId, menuIds == null ? List.of() : menuIds);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                authPermissionManager.evictUsers(userIds);
            }
        });
        log.info("【SysRole】重新授权角色菜单成功，roleId={}, menuCount={}, affectedUsers={}",
                roleId, menuIds == null ? 0 : menuIds.size(), userIds.size());
    }

    /**
     * 查询角色（不存在时抛出）
     *
     * @param roleId 角色主键
     * @return 角色实体
     * @throws BizException 角色不存在时抛出（40400）
     */
    private SysRoleDO requireRole(Long roleId) {
        SysRoleDO role = sysRoleService.getById(roleId);
        if (role == null) {
            log.info("【SysRole】操作终止：角色不存在，roleId={}", roleId);
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "角色不存在");
        }
        return role;
    }

    /**
     * 校验角色编码格式（大写字母开头，仅大写字母/数字/下划线，2~64 位）
     *
     * @param roleCode 角色编码
     * @throws BizException 格式非法时抛出
     */
    private void assertCodeFormat(String roleCode) {
        if (roleCode == null || !ROLE_CODE_PATTERN.matcher(roleCode).matches()) {
            log.info("【SysRole】校验终止：角色编码格式非法");
            throw new BizException(ResultCode.PARAM_INVALID, "角色编码须为大写字母开头，仅含大写字母/数字/下划线");
        }
    }

    /**
     * 校验数据权限档位取值合法
     *
     * @param dataScope 档位值
     * @throws BizException 取值非法时抛出
     */
    private void assertDataScopeValid(Integer dataScope) {
        if (DataScopeEnum.of(dataScope) == null) {
            log.info("【SysRole】校验终止：数据权限档位非法，dataScope={}", dataScope);
            throw new BizException(ResultCode.PARAM_INVALID, "数据权限档位取值非法");
        }
    }

    /**
     * 注册事务提交后回调：清除角色关联用户的权限缓存
     *
     * @param roleId 角色主键
     */
    private void registerAfterCommitEvict(Long roleId) {
        List<Long> userIds = sysUserService.listUserIdsByRoleId(roleId);
        if (userIds.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                authPermissionManager.evictUsers(userIds);
            }
        });
    }
}

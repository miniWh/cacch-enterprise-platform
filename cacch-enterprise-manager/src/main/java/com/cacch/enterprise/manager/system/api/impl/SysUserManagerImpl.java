package com.cacch.enterprise.manager.system.api.impl;

import com.cacch.enterprise.common.constant.system.SystemCommonConstants;
import com.cacch.enterprise.common.enums.system.DataScopeEnum;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.model.auth.LoginUser;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.common.utils.LoginUserHolder;
import com.cacch.enterprise.common.utils.PasswordAppSupport;
import com.cacch.enterprise.dao.entity.system.SysDeptDO;
import com.cacch.enterprise.dao.entity.system.SysRoleDO;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.manager.auth.api.IAuthLoginManager;
import com.cacch.enterprise.manager.auth.api.IAuthPermissionManager;
import com.cacch.enterprise.manager.system.api.ISysUserManager;
import com.cacch.enterprise.manager.system.model.CreateUserCommand;
import com.cacch.enterprise.manager.system.model.UpdateUserCommand;
import com.cacch.enterprise.service.system.api.ISysDeptService;
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
import java.util.Objects;

/**
 * 用户管理编排实现（用户 + 角色分配 + 会话清理 + 权限缓存 evict）
 *
 * <p>事务边界：DB 写操作聚合在一个 REQUIRED 事务内；Redis 会话删除与缓存 evict
 * 在事务提交后（方法内事务边界之后）执行。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserManagerImpl implements ISysUserManager {

    private final ISysUserService sysUserService;
    private final ISysRoleService sysRoleService;
    private final ISysDeptService sysDeptService;
    private final IAuthLoginManager authLoginManager;
    private final IAuthPermissionManager authPermissionManager;

    /**
     * 新增用户并分配角色（同一事务）
     *
     * @param command 新增用户命令
     * @return 新用户 ID
     * @throws BizException 工号/手机号重复、部门非法或停用、无权分配角色、密码不满足复杂度时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public Long createUser(CreateUserCommand command) {
        if (sysUserService.existsUsername(command.getUsername(), null)) {
            log.info("【SysUser】新增用户终止：工号已占用，username={}", command.getUsername());
            throw new BizException(ResultCode.SYSTEM_USERNAME_DUPLICATE);
        }
        if (sysUserService.existsPhone(command.getPhone(), null)) {
            log.info("【SysUser】新增用户终止：手机号已占用，phone={}", maskPhone(command.getPhone()));
            throw new BizException(ResultCode.SYSTEM_PHONE_DUPLICATE);
        }
        if (!PasswordAppSupport.matchesComplexity(command.getPassword())) {
            log.info("【SysUser】新增用户终止：初始密码不满足复杂度，username={}", command.getUsername());
            throw new BizException(ResultCode.PARAM_INVALID, "密码须为 8~64 位且同时包含字母与数字");
        }
        assertDeptAvailable(command.getDeptId());
        if (command.getRoleIds() != null && !command.getRoleIds().isEmpty()) {
            assertCanAssignRoles();
            assertRolesValid(command.getRoleIds());
        }
        SysUserDO user = new SysUserDO();
        user.setUsername(command.getUsername());
        user.setPassword(PasswordAppSupport.hash(command.getPassword()));
        user.setRealName(command.getRealName());
        user.setDeptId(command.getDeptId());
        user.setPhone(command.getPhone());
        user.setEmail(command.getEmail());
        user.setStatus(0);
        sysUserService.createUser(user);
        if (command.getRoleIds() != null && !command.getRoleIds().isEmpty()) {
            sysUserService.replaceUserRoles(user.getId(), command.getRoleIds());
        }
        log.info("【SysUser】新增用户成功，userId={}, username={}, roleIds={}", user.getId(), user.getUsername(), command.getRoleIds());
        return user.getId();
    }

    /**
     * 编辑用户（可选重新分配角色；角色变更后 evict 权限缓存）
     *
     * @param command 编辑用户命令
     * @throws BizException 用户不存在、内置保护、越界操作、手机号重复或角色非法时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void updateUser(UpdateUserCommand command) {
        SysUserDO target = requireUser(command.getUserId());
        assertNotSuperAdmin(target, "编辑");
        assertTargetInScope(target);
        if (sysUserService.existsPhone(command.getPhone(), command.getUserId())) {
            log.info("【SysUser】编辑用户终止：手机号已占用，userId={}", command.getUserId());
            throw new BizException(ResultCode.SYSTEM_PHONE_DUPLICATE);
        }
        assertDeptAvailable(command.getDeptId());
        boolean rolesChanged = command.getRoleIds() != null;
        if (rolesChanged && !command.getRoleIds().isEmpty()) {
            assertCanAssignRoles();
            assertRolesValid(command.getRoleIds());
        }
        SysUserDO update = new SysUserDO();
        update.setId(command.getUserId());
        update.setRealName(command.getRealName());
        update.setDeptId(command.getDeptId());
        update.setPhone(command.getPhone());
        update.setEmail(command.getEmail());
        sysUserService.updateUser(update);
        if (rolesChanged) {
            sysUserService.replaceUserRoles(command.getUserId(), command.getRoleIds());
            registerAfterCommitEvict(command.getUserId());
        }
        log.info("【SysUser】编辑用户成功，userId={}, rolesChanged={}", command.getUserId(), rolesChanged);
    }

    /**
     * 启用/停用用户（停用后删除其会话强制下线）
     *
     * @param userId 用户主键
     * @param status 目标状态：0 启用 / 1 停用
     * @throws BizException 用户不存在、内置保护或越界操作时抛出
     */
    @Override
    public void updateStatus(Long userId, Integer status) {
        SysUserDO target = requireUser(userId);
        if (status != null && status == 1) {
            assertNotSuperAdmin(target, "停用");
        }
        assertTargetInScope(target);
        sysUserService.updateStatus(userId, status);
        authLoginManager.forceOffline(userId);
        log.info("【SysUser】更新用户状态完成，userId={}, status={}", userId, status);
    }

    /**
     * 重置密码为指定新密码（成功后删除目标用户会话）
     *
     * @param userId     用户主键
     * @param newPassword 新密码明文
     * @throws BizException 用户不存在、越界操作或密码不满足复杂度时抛出
     */
    @Override
    public void resetPassword(Long userId, String newPassword) {
        SysUserDO target = requireUser(userId);
        assertTargetInScope(target);
        if (!PasswordAppSupport.matchesComplexity(newPassword)) {
            log.info("【SysUser】重置密码终止：新密码不满足复杂度，userId={}", userId);
            throw new BizException(ResultCode.PARAM_INVALID, "密码须为 8~64 位且同时包含字母与数字");
        }
        sysUserService.updatePassword(userId, PasswordAppSupport.hash(newPassword));
        authLoginManager.forceOffline(userId);
        log.info("【SysUser】重置密码完成，userId={}", userId);
    }

    /**
     * 逻辑删除用户（清空角色关联 + 删除会话 + evict 权限缓存）
     *
     * @param userId 用户主键
     * @throws BizException 用户不存在、内置保护或越界操作时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void deleteById(Long userId) {
        SysUserDO target = requireUser(userId);
        assertNotSuperAdmin(target, "删除");
        assertTargetInScope(target);
        sysUserService.deleteById(userId);
        sysUserService.replaceUserRoles(userId, List.of());
        registerAfterCommitSync(userId);
        log.info("【SysUser】删除用户完成（事务提交后清理会话与权限缓存），userId={}", userId);
    }

    /**
     * 强制下线指定用户
     *
     * @param userId 用户主键
     * @throws BizException 用户不存在或越界操作时抛出
     */
    @Override
    public void forceOffline(Long userId) {
        SysUserDO target = requireUser(userId);
        assertTargetInScope(target);
        authLoginManager.forceOffline(userId);
        log.info("【SysUser】强制下线完成，userId={}", userId);
    }

    /**
     * 注册事务提交后回调：删除目标用户会话（登出态）
     *
     * @param userId 用户主键
     */
    private void registerAfterCommitSync(Long userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                authLoginManager.forceOffline(userId);
                authPermissionManager.evictUsers(List.of(userId));
            }
        });
    }

    /**
     * 注册事务提交后回调：清除目标用户权限缓存
     *
     * @param userId 用户主键
     */
    private void registerAfterCommitEvict(Long userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                authPermissionManager.evictUsers(List.of(userId));
            }
        });
    }

    /**
     * 查询用户（不存在时抛出）
     *
     * @param userId 用户主键
     * @return 用户实体
     * @throws BizException 用户不存在时抛出（40400）
     */
    private SysUserDO requireUser(Long userId) {
        SysUserDO user = sysUserService.getById(userId);
        if (user == null) {
            log.info("【SysUser】操作终止：用户不存在，userId={}", userId);
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 内置保护校验：SUPER_ADMIN 账号不可编辑/停用/删除
     *
     * @param target 目标用户
     * @param action 操作名称（日志用）
     * @throws BizException 目标为 SUPER_ADMIN 时抛出（40301）
     */
    private void assertNotSuperAdmin(SysUserDO target, String action) {
        boolean superAdmin = sysRoleService.listEnabledByUserId(target.getId()).stream()
                .anyMatch(r -> SystemCommonConstants.ROLE_SUPER_ADMIN.equals(r.getRoleCode()));
        if (superAdmin) {
            log.info("【SysUser】{}操作被拒绝：SUPER_ADMIN 账号受保护，userId={}", action, target.getId());
            throw new BizException(ResultCode.SYSTEM_USER_PROTECTED);
        }
    }

    /**
     * 数据权限越界校验：目标用户必须在操作者数据范围内
     *
     * @param target 目标用户
     * @throws BizException 越界时抛出（40300）
     */
    private void assertTargetInScope(SysUserDO target) {
        LoginUser operator = LoginUserHolder.get();
        if (operator == null || operator.getDataScopes() == null
                || operator.getDataScopes().contains(DataScopeEnum.ALL)) {
            return;
        }
        boolean inScope = false;
        for (DataScopeEnum scope : operator.getDataScopes()) {
            switch (scope) {
                case DEPT -> inScope = Objects.equals(operator.getDeptId(), target.getDeptId());
                case DEPT_AND_CHILD -> inScope = isDeptInOperatorSubtree(operator, target.getDeptId());
                case SELF -> inScope = Objects.equals(operator.getUserId(), target.getId());
                default -> inScope = false;
            }
            if (inScope) {
                break;
            }
        }
        if (!inScope) {
            log.info("【SysUser】操作被拒绝：目标用户超出操作者数据范围，operator={}, targetUserId={}, targetDeptId={}",
                    operator.getUserId(), target.getId(), target.getDeptId());
            throw new BizException(ResultCode.AUTH_PERMISSION_DENIED, "目标用户不在您的数据权限范围内");
        }
    }

    /**
     * 判断目标部门是否位于操作者部门子树内
     *
     * @param operator     操作者登录上下文
     * @param targetDeptId 目标部门 ID
     * @return 在子树内返回 true
     */
    private boolean isDeptInOperatorSubtree(LoginUser operator, Long targetDeptId) {
        if (operator.getDeptId() == null || targetDeptId == null) {
            return false;
        }
        if (operator.getDeptId().equals(targetDeptId)) {
            return true;
        }
        SysDeptDO targetDept = sysDeptService.getById(targetDeptId);
        if (targetDept == null) {
            return false;
        }
        String prefix = (operator.getDeptAncestors() == null ? "" : operator.getDeptAncestors() + ",") + operator.getDeptId();
        return targetDept.getAncestors().equals(prefix) || targetDept.getAncestors().startsWith(prefix + ",");
    }

    /**
     * 校验目标部门存在且启用链完整（停用部门及其下级不可新增用户）
     *
     * @param deptId 部门 ID
     * @throws BizException 部门不存在或链路停用时抛出
     */
    private void assertDeptAvailable(Long deptId) {
        if (deptId == null) {
            return;
        }
        if (sysDeptService.getById(deptId) == null) {
            log.info("【SysUser】部门校验终止：部门不存在，deptId={}", deptId);
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "部门不存在");
        }
        if (!sysDeptService.isDeptEnabledChain(deptId)) {
            log.info("【SysUser】部门校验终止：部门或其上级已停用，deptId={}", deptId);
            throw new BizException(ResultCode.PARAM_INVALID, "目标部门已停用，不可向其新增或移入用户");
        }
    }

    /**
     * 角色分配权限校验：仅 SUPER_ADMIN / SYS_ADMIN 可分配角色（R-1：DEPT_ADMIN 不可分配）
     *
     * @throws BizException 操作者无分配角色权限时抛出（40300）
     */
    private void assertCanAssignRoles() {
        LoginUser operator = LoginUserHolder.get();
        boolean allowed = operator != null && operator.getRoleCodes() != null && (
                operator.getRoleCodes().contains(SystemCommonConstants.ROLE_SUPER_ADMIN)
                        || operator.getRoleCodes().contains(SystemCommonConstants.ROLE_SYS_ADMIN));
        if (!allowed) {
            log.info("【SysUser】角色分配被拒绝：操作者无分配角色权限，operator={}",
                    operator == null ? null : operator.getUserId());
            throw new BizException(ResultCode.AUTH_PERMISSION_DENIED, "无权分配角色");
        }
    }

    /**
     * 校验角色 ID 列表均为存在的启用状态角色
     *
     * @param roleIds 角色 ID 列表
     * @throws BizException 任一角色不存在或已停用时抛出
     */
    private void assertRolesValid(List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysRoleDO role = sysRoleService.getById(roleId);
            if (role == null || (role.getStatus() != null && role.getStatus() == 1)) {
                log.info("【SysUser】角色校验终止：角色不存在或已停用，roleId={}", roleId);
                throw new BizException(ResultCode.PARAM_INVALID, "角色不存在或已停用：roleId=" + roleId);
            }
        }
    }

    /**
     * 手机号脱敏（日志输出用）：保留前 3 位与后 4 位
     *
     * @param phone 手机号原文
     * @return 脱敏后的手机号
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}

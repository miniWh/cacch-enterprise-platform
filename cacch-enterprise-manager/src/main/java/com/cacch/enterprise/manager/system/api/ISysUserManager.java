package com.cacch.enterprise.manager.system.api;

import com.cacch.enterprise.manager.system.model.CreateUserCommand;
import com.cacch.enterprise.manager.system.model.UpdateUserCommand;

/**
 * 用户管理编排接口（用户 + 角色分配跨聚合；操作者越界/内置保护校验）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISysUserManager {

    /**
     * 新增用户并分配角色（同一事务；工号/手机号唯一校验、部门启用链校验、角色分配权限校验）
     *
     * @param command 新增用户命令
     * @return 新用户 ID
     * @throws BizException 工号/手机号重复、部门非法或停用、无权分配角色、密码不满足复杂度时抛出
     */
    Long createUser(CreateUserCommand command);

    /**
     * 编辑用户（重置手机号/姓名/部门/邮箱，可选重新分配角色）
     *
     * @param command 编辑用户命令
     * @throws BizException 用户不存在、内置保护、越界操作、手机号重复或角色非法时抛出
     */
    void updateUser(UpdateUserCommand command);

    /**
     * 启用/停用用户（停用后立即删除其会话，强制下线）
     *
     * @param userId 用户主键
     * @param status 目标状态：0 启用 / 1 停用
     * @throws BizException 用户不存在、内置保护或越界操作时抛出
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 重置密码为指定新密码（成功后删除目标用户会话）
     *
     * @param userId     用户主键
     * @param newPassword 新密码明文
     * @throws BizException 用户不存在、越界操作或密码不满足复杂度时抛出
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 逻辑删除用户（同时逻辑删除其角色关联并清除会话与权限缓存）
     *
     * @param userId 用户主键
     * @throws BizException 用户不存在、内置保护或越界操作时抛出
     */
    void deleteById(Long userId);

    /**
     * 强制下线指定用户（删除其登录态与刷新凭证）
     *
     * @param userId 用户主键
     * @throws BizException 用户不存在或越界操作时抛出
     */
    void forceOffline(Long userId);
}

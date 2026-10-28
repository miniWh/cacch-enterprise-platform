package com.cacch.enterprise.service.system.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.dao.query.system.SysUserPageQuery;

import java.util.List;

/**
 * 用户服务接口（sys_user 聚合）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISysUserService {

    /**
     * 按工号查询用户（仅未删除）
     *
     * @param username 工号，全局唯一
     * @return 用户实体；不存在时返回 null
     */
    SysUserDO getByUsername(String username);

    /**
     * 按手机号查询用户（仅未删除）
     *
     * @param phone 手机号，全局唯一
     * @return 用户实体；不存在时返回 null
     */
    SysUserDO getByPhone(String phone);

    /**
     * 按 ID 查询用户（仅未删除）
     *
     * @param userId 用户主键
     * @return 用户实体；不存在或已删除时返回 null
     */
    SysUserDO getById(Long userId);

    /**
     * 分页查询用户列表（数据权限按操作者档位由拦截器自动追加）
     *
     * @param pageNum  页码，从 1 开始
     * @param pageSize 每页条数
     * @param query    查询条件（工号/姓名/手机号模糊，部门含下级/状态精确）
     * @return 用户分页数据
     */
    IPage<SysUserDO> pageUsers(long pageNum, long pageSize, SysUserPageQuery query);

    /**
     * 校验工号是否已被占用（逻辑删除后可复用，不算占用）
     *
     * @param username      待校验工号
     * @param excludeUserId 排除的用户 ID（编辑场景传本人），可空
     * @return 已被其他用户占用返回 true
     */
    boolean existsUsername(String username, Long excludeUserId);

    /**
     * 校验手机号是否已被占用（逻辑删除后可复用，不算占用）
     *
     * @param phone         待校验手机号
     * @param excludeUserId 排除的用户 ID（编辑场景传本人），可空
     * @return 已被其他用户占用返回 true
     */
    boolean existsPhone(String phone, Long excludeUserId);

    /**
     * 统计归属指定部门的未删除用户数（部门删除前置校验）
     *
     * @param deptId 部门 ID
     * @return 未删除用户数，无则 0
     */
    long countByDeptId(Long deptId);

    /**
     * 新增用户（参与调用方事务）
     *
     * @param user 用户实体（密码须为 BCrypt 密文）
     */
    void createUser(SysUserDO user);

    /**
     * 编辑用户（按非空字段更新，参与调用方事务）
     *
     * @param user 用户实体（仅携带需更新字段与主键）
     */
    void updateUser(SysUserDO user);

    /**
     * 更新用户状态（启用/停用，参与调用方事务）
     *
     * @param userId 用户主键
     * @param status 目标状态：0 启用 / 1 停用
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 更新用户密码（独立事务，供修改密码/重置密码场景）
     *
     * @param userId          用户主键
     * @param encodedPassword BCrypt 密文
     */
    void updatePassword(Long userId, String encodedPassword);

    /**
     * 回写最近登录时间（独立事务，登录事件异步消费时调用）
     *
     * @param userId 用户主键
     */
    void updateLastLoginTime(Long userId);

    /**
     * 逻辑删除用户（参与调用方事务）
     *
     * @param userId 用户主键
     */
    void deleteById(Long userId);

    /**
     * 查询用户持有的全部角色 ID（含停用角色关联，逻辑删除关联除外）
     *
     * @param userId 用户主键
     * @return 角色 ID 列表，无则空列表
     */
    List<Long> listRoleIdsByUserId(Long userId);

    /**
     * 重新分配用户角色（旧关联逻辑删除后插入新关联，参与调用方事务）
     *
     * @param userId  用户主键
     * @param roleIds 目标角色 ID 列表，空列表表示清空角色
     */
    void replaceUserRoles(Long userId, List<Long> roleIds);

    /**
     * 查询已关联指定角色的用户 ID 列表（角色删除/授权变更 evict 依据）
     *
     * @param roleId 角色主键
     * @return 用户 ID 列表，无则空列表
     */
    List<Long> listUserIdsByRoleId(Long roleId);
}

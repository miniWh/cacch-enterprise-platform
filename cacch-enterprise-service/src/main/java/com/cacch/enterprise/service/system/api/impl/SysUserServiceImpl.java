package com.cacch.enterprise.service.system.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.dao.entity.system.SysUserRoleDO;
import com.cacch.enterprise.dao.mapper.system.SysUserMapper;
import com.cacch.enterprise.dao.mapper.system.SysUserRoleMapper;
import com.cacch.enterprise.dao.query.system.SysUserPageQuery;
import com.cacch.enterprise.service.system.api.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    /**
     * 按工号查询用户（仅未删除）
     *
     * @param username 工号
     * @return 用户实体；不存在时返回 null
     */
    @Override
    public SysUserDO getByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, username));
    }

    /**
     * 按手机号查询用户（仅未删除）
     *
     * @param phone 手机号
     * @return 用户实体；不存在时返回 null
     */
    @Override
    public SysUserDO getByPhone(String phone) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getPhone, phone));
    }

    /**
     * 按 ID 查询用户（仅未删除）
     *
     * @param userId 用户主键
     * @return 用户实体；不存在或已删除时返回 null
     */
    @Override
    public SysUserDO getById(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    /**
     * 分页查询用户列表（数据权限条件由拦截器按 @DataScope 追加）
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param query    查询条件
     * @return 用户分页数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.SUPPORTS, readOnly = true, timeout = 10)
    public IPage<SysUserDO> pageUsers(long pageNum, long pageSize, SysUserPageQuery query) {
        return sysUserMapper.selectUserPage(new Page<>(pageNum, pageSize), query);
    }

    /**
     * 校验工号是否已被占用（仅统计未删除数据）
     *
     * @param username      工号
     * @param excludeUserId 排除的用户 ID
     * @return 已被占用返回 true
     */
    @Override
    public boolean existsUsername(String username, Long excludeUserId) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getUsername, username);
        if (excludeUserId != null) {
            wrapper.ne(SysUserDO::getId, excludeUserId);
        }
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    /**
     * 校验手机号是否已被占用（仅统计未删除数据）
     *
     * @param phone         手机号
     * @param excludeUserId 排除的用户 ID
     * @return 已被占用返回 true
     */
    @Override
    public boolean existsPhone(String phone, Long excludeUserId) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getPhone, phone);
        if (excludeUserId != null) {
            wrapper.ne(SysUserDO::getId, excludeUserId);
        }
        return sysUserMapper.selectCount(wrapper) > 0;
    }

    /**
     * 统计归属指定部门的未删除用户数
     *
     * @param deptId 部门 ID
     * @return 用户数
     */
    @Override
    public long countByDeptId(Long deptId) {
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUserDO>()
                .eq(SysUserDO::getDeptId, deptId));
    }

    /**
     * 新增用户（参与调用方事务）
     *
     * @param user 用户实体（密码为 BCrypt 密文）
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void createUser(SysUserDO user) {
        sysUserMapper.insert(user);
        log.info("【SysUser】新增用户，userId={}, username={}", user.getId(), user.getUsername());
    }

    /**
     * 编辑用户（按非空字段更新，参与调用方事务）
     *
     * @param user 用户实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void updateUser(SysUserDO user) {
        sysUserMapper.updateById(user);
        log.info("【SysUser】编辑用户，userId={}", user.getId());
    }

    /**
     * 更新用户状态（参与调用方事务）
     *
     * @param userId 用户主键
     * @param status 目标状态：0 启用 / 1 停用
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void updateStatus(Long userId, Integer status) {
        SysUserDO update = new SysUserDO();
        update.setId(userId);
        update.setStatus(status);
        sysUserMapper.updateById(update);
        log.info("【SysUser】更新用户状态，userId={}, status={}", userId, status);
    }

    /**
     * 更新用户密码（参与调用方事务）
     *
     * @param userId          用户主键
     * @param encodedPassword BCrypt 密文
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void updatePassword(Long userId, String encodedPassword) {
        SysUserDO update = new SysUserDO();
        update.setId(userId);
        update.setPassword(encodedPassword);
        sysUserMapper.updateById(update);
        log.info("【SysUser】更新用户密码，userId={}", userId);
    }

    /**
     * 回写最近登录时间（独立事务，不受外部事务影响）
     *
     * @param userId 用户主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW, readOnly = false, timeout = 10)
    public void updateLastLoginTime(Long userId) {
        SysUserDO update = new SysUserDO();
        update.setId(userId);
        update.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(update);
    }

    /**
     * 逻辑删除用户（参与调用方事务）
     *
     * @param userId 用户主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void deleteById(Long userId) {
        sysUserMapper.deleteById(userId);
        log.info("【SysUser】逻辑删除用户，userId={}", userId);
    }

    /**
     * 查询用户持有的全部角色 ID
     *
     * @param userId 用户主键
     * @return 角色 ID 列表，无则空列表
     */
    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleDO>()
                        .eq(SysUserRoleDO::getUserId, userId))
                .stream().map(SysUserRoleDO::getRoleId).toList();
    }

    /**
     * 重新分配用户角色（旧关联逻辑删除后插入新关联）
     *
     * @param userId  用户主键
     * @param roleIds 目标角色 ID 列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void replaceUserRoles(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRoleDO>()
                .eq(SysUserRoleDO::getUserId, userId));
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds.stream().distinct().toList()) {
                SysUserRoleDO relation = new SysUserRoleDO();
                relation.setUserId(userId);
                relation.setRoleId(roleId);
                sysUserRoleMapper.insert(relation);
            }
        }
        log.info("【SysUser】重新分配用户角色，userId={}, roleIds={}", userId, roleIds);
    }

    /**
     * 查询已关联指定角色的用户 ID 列表
     *
     * @param roleId 角色主键
     * @return 用户 ID 列表，无则空列表
     */
    @Override
    public List<Long> listUserIdsByRoleId(Long roleId) {
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRoleDO>()
                        .eq(SysUserRoleDO::getRoleId, roleId))
                .stream().map(SysUserRoleDO::getUserId).toList();
    }
}

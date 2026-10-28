package com.cacch.enterprise.controller.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.enterprise.common.annotation.RequiresPermission;
import com.cacch.enterprise.common.constant.system.SystemPathConstants;
import com.cacch.enterprise.common.constant.system.SystemPermissionConstants;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.PageResult;
import com.cacch.enterprise.common.result.Result;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.convert.auth.AuthLoginConverter;
import com.cacch.enterprise.convert.system.SysUserConverter;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.dao.query.system.SysUserPageQuery;
import com.cacch.enterprise.dto.system.request.CreateSysUserRequest;
import com.cacch.enterprise.dto.system.request.ResetPasswordRequest;
import com.cacch.enterprise.dto.system.request.UpdateSysUserRequest;
import com.cacch.enterprise.dto.system.request.UserStatusRequest;
import com.cacch.enterprise.dto.auth.vo.OnlineUserVO;
import com.cacch.enterprise.dto.system.vo.SysUserVO;
import com.cacch.enterprise.manager.auth.api.IAuthLoginManager;
import com.cacch.enterprise.manager.system.api.ISysUserManager;
import com.cacch.enterprise.service.system.api.ISysUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理 REST 接口（单聚合读走 Service，写/编排走 Manager）
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping(SystemPathConstants.PATH_USERS)
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService sysUserService;

    private final ISysUserManager sysUserManager;

    private final IAuthLoginManager authLoginManager;

    private final SysUserConverter sysUserConverter;

    private final AuthLoginConverter authLoginConverter;

    /**
     * 分页查询用户列表（数据权限按操作者档位由拦截器自动追加）
     *
     * @param pageNum  页码，从 1 开始，默认 1
     * @param pageSize 每页条数，1~100，默认 10
     * @param username 工号模糊搜索，可空
     * @param realName 姓名模糊搜索，可空
     * @param phone    手机号模糊搜索，可空
     * @param deptId   按部门筛选（含下级部门），可空
     * @param status   状态筛选：0 启用 / 1 停用，可空
     * @return 用户分页数据
     * @throws BizException 参数非法（40002）时抛出
     */
    @GetMapping
    @RequiresPermission(SystemPermissionConstants.USER_LIST)
    public Result<PageResult<SysUserVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为 1") long pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数最小为 1")
            @Max(value = 100, message = "每页条数最大为 100") long pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status) {
        SysUserPageQuery query = SysUserPageQuery.builder()
                .username(username).realName(realName).phone(phone).deptId(deptId).status(status)
                .build();
        IPage<SysUserDO> page = sysUserService.pageUsers(pageNum, pageSize, query);
        return Result.success(sysUserConverter.toPageVO(page));
    }

    /**
     * 查询在线用户列表（扫描 Redis 登录态）
     *
     * @return 在线用户视图对象列表，无在线用户时为空列表
     */
    @GetMapping("/online")
    @RequiresPermission(SystemPermissionConstants.USER_OFFLINE)
    public Result<List<OnlineUserVO>> online() {
        return Result.success(authLoginManager.listOnlineUsers().stream()
                .map(authLoginConverter::toOnlineUserVO)
                .toList());
    }

    /**
     * 查询用户详情（含持有角色 ID 列表，编辑回显用）
     *
     * @param id 用户主键，对应 sys_user.id
     * @return 用户详情视图对象
     * @throws BizException 用户不存在时抛出
     */
    @GetMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.USER_LIST)
    public Result<SysUserVO> detail(@PathVariable("id") Long id) {
        SysUserDO user = sysUserService.getById(id);
        if (user == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return Result.success(sysUserConverter.toDetailVO(user, sysUserService.listRoleIdsByUserId(id)));
    }

    /**
     * 新增用户并分配角色
     *
     * @param request 新增用户请求
     * @return 新用户 ID
     * @throws BizException 工号/手机号重复、部门非法或停用、无权分配角色、密码不满足复杂度时抛出
     */
    @PostMapping
    @RequiresPermission(SystemPermissionConstants.USER_ADD)
    public Result<Long> create(@Valid @RequestBody CreateSysUserRequest request) {
        return Result.success(sysUserManager.createUser(sysUserConverter.toCreateCommand(request)));
    }

    /**
     * 编辑用户（重置手机号/姓名/部门/邮箱，可选重新分配角色）
     *
     * @param id      用户主键，对应 sys_user.id
     * @param request 编辑用户请求
     * @return 成功返回体（无数据）
     * @throws BizException 用户不存在、内置保护、越界操作、手机号重复或角色非法时抛出
     */
    @PutMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.USER_EDIT)
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody UpdateSysUserRequest request) {
        sysUserManager.updateUser(sysUserConverter.toUpdateCommand(request, id));
        return Result.success();
    }

    /**
     * 启用/停用用户（停用后立即删除其会话强制下线）
     *
     * @param id      用户主键，对应 sys_user.id
     * @param request 状态请求（0 启用 / 1 停用）
     * @return 成功返回体（无数据）
     * @throws BizException 用户不存在、内置保护或越界操作时抛出
     */
    @PutMapping("/{id}/status")
    @RequiresPermission(SystemPermissionConstants.USER_STATUS)
    public Result<Void> updateStatus(@PathVariable("id") Long id,
                                     @Valid @RequestBody UserStatusRequest request) {
        sysUserManager.updateStatus(id, request.getStatus());
        return Result.success();
    }

    /**
     * 重置用户密码（成功后清除目标用户全部会话）
     *
     * @param id      用户主键，对应 sys_user.id
     * @param request 重置密码请求（新密码明文）
     * @return 成功返回体（无数据）
     * @throws BizException 用户不存在、越界操作或密码不满足复杂度时抛出
     */
    @PutMapping("/{id}/password")
    @RequiresPermission(SystemPermissionConstants.USER_RESET_PWD)
    public Result<Void> resetPassword(@PathVariable("id") Long id,
                                      @Valid @RequestBody ResetPasswordRequest request) {
        sysUserManager.resetPassword(id, request.getNewPassword());
        return Result.success();
    }

    /**
     * 强制下线指定用户（删除其登录态与刷新凭证，不改动账号状态）
     *
     * @param id 用户主键，对应 sys_user.id
     * @return 成功返回体（无数据）
     * @throws BizException 用户不存在或越界操作时抛出
     */
    @PutMapping("/{id}/offline")
    @RequiresPermission(SystemPermissionConstants.USER_OFFLINE)
    public Result<Void> offline(@PathVariable("id") Long id) {
        sysUserManager.forceOffline(id);
        return Result.success();
    }

    /**
     * 删除用户（逻辑删除，同时清除其角色关联、会话与权限缓存）
     *
     * @param id 用户主键，对应 sys_user.id
     * @return 成功返回体（无数据）
     * @throws BizException 用户不存在、内置保护或越界操作时抛出
     */
    @DeleteMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.USER_DELETE)
    public Result<Void> delete(@PathVariable("id") Long id) {
        sysUserManager.deleteById(id);
        return Result.success();
    }
}

package com.cacch.enterprise.controller.system;

import com.cacch.enterprise.common.annotation.RequiresPermission;
import com.cacch.enterprise.common.constant.system.SystemPathConstants;
import com.cacch.enterprise.common.constant.system.SystemPermissionConstants;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.PageResult;
import com.cacch.enterprise.common.result.Result;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.convert.system.SysRoleConverter;
import com.cacch.enterprise.dto.system.request.GrantMenusRequest;
import com.cacch.enterprise.dto.system.request.RoleSaveRequest;
import com.cacch.enterprise.dto.system.vo.SysRoleSimpleVO;
import com.cacch.enterprise.dto.system.vo.SysRoleVO;
import com.cacch.enterprise.manager.system.api.ISysRoleManager;
import com.cacch.enterprise.service.system.api.ISysRoleService;
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
 * 角色管理 REST 接口（分页/下拉读走 Service，保存/授权走 Manager）
 *
 * @author hongfu_zhou@cacch.com
 */
@Validated
@RestController
@RequestMapping(SystemPathConstants.PATH_ROLES)
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService sysRoleService;

    private final ISysRoleManager sysRoleManager;

    private final SysRoleConverter sysRoleConverter;

    /**
     * 分页查询角色列表
     *
     * @param pageNum  页码，从 1 开始，默认 1
     * @param pageSize 每页条数，1~100，默认 10
     * @param keyword  关键词（匹配角色名称或编码），可空
     * @return 角色分页数据
     * @throws BizException 参数非法（40002）时抛出
     */
    @GetMapping
    @RequiresPermission(SystemPermissionConstants.ROLE_LIST)
    public Result<PageResult<SysRoleVO>> page(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为 1") long pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数最小为 1")
            @Max(value = 100, message = "每页条数最大为 100") long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(sysRoleConverter.toPageVO(
                sysRoleService.pageRoles(pageNum, pageSize, keyword)));
    }

    /**
     * 查询全部启用状态角色（用户分配角色下拉，仅需登录态）
     *
     * @return 精简角色列表
     */
    @GetMapping("/enabled")
    public Result<List<SysRoleSimpleVO>> enabled() {
        return Result.success(sysRoleService.listEnabled().stream()
                .map(sysRoleConverter::toSimpleVO)
                .toList());
    }

    /**
     * 查询角色已授权的菜单/按钮 ID 列表（授权树回显）
     *
     * @param id 角色主键，对应 sys_role.id
     * @return 菜单 ID 列表，无授权时为空列表
     * @throws BizException 角色不存在时抛出
     */
    @GetMapping("/{id}/menus")
    @RequiresPermission(SystemPermissionConstants.ROLE_LIST)
    public Result<List<Long>> menuIds(@PathVariable("id") Long id) {
        if (sysRoleService.getById(id) == null) {
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND);
        }
        return Result.success(sysRoleService.listMenuIdsByRoleId(id));
    }

    /**
     * 新增角色
     *
     * @param request 角色保存请求
     * @return 新角色 ID
     * @throws BizException 编码/名称重复或编码格式非法时抛出
     */
    @PostMapping
    @RequiresPermission(SystemPermissionConstants.ROLE_ADD)
    public Result<Long> create(@Valid @RequestBody RoleSaveRequest request) {
        return Result.success(sysRoleManager.createRole(sysRoleConverter.toCommand(request)));
    }

    /**
     * 编辑角色（内置角色编码/名称不可修改，SUPER_ADMIN 数据权限档位不可修改）
     *
     * @param id      角色主键，对应 sys_role.id
     * @param request 角色保存请求
     * @return 成功返回体（无数据）
     * @throws BizException 角色不存在、内置保护或编码/名称重复时抛出
     */
    @PutMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.ROLE_EDIT)
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody RoleSaveRequest request) {
        sysRoleManager.updateRole(id, sysRoleConverter.toCommand(request));
        return Result.success();
    }

    /**
     * 删除角色（内置角色或已分配用户的角色禁止删除）
     *
     * @param id 角色主键，对应 sys_role.id
     * @return 成功返回体（无数据）
     * @throws BizException 角色不存在、内置保护或已分配用户时抛出
     */
    @DeleteMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.ROLE_DELETE)
    public Result<Void> delete(@PathVariable("id") Long id) {
        sysRoleManager.deleteRole(id);
        return Result.success();
    }

    /**
     * 重新授权角色菜单（授权变更后 evict 关联用户权限缓存）
     *
     * @param id      角色主键，对应 sys_role.id
     * @param request 授权请求（菜单/按钮 ID 列表，空列表即清空）
     * @return 成功返回体（无数据）
     * @throws BizException 角色或菜单不存在时抛出
     */
    @PutMapping("/{id}/menus")
    @RequiresPermission(SystemPermissionConstants.ROLE_GRANT)
    public Result<Void> grantMenus(@PathVariable("id") Long id,
                                   @Valid @RequestBody GrantMenusRequest request) {
        sysRoleManager.grantMenus(id, request.getMenuIds());
        return Result.success();
    }
}

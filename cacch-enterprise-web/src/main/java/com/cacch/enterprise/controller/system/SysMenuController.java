package com.cacch.enterprise.controller.system;

import com.cacch.enterprise.common.annotation.RequiresPermission;
import com.cacch.enterprise.common.constant.system.SystemPathConstants;
import com.cacch.enterprise.common.constant.system.SystemPermissionConstants;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.Result;
import com.cacch.enterprise.convert.system.SysMenuConverter;
import com.cacch.enterprise.dao.entity.system.SysMenuDO;
import com.cacch.enterprise.dto.system.request.MenuSaveRequest;
import com.cacch.enterprise.dto.system.vo.MenuTreeVO;
import com.cacch.enterprise.service.system.api.ISysMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 菜单管理 REST 接口（单聚合操作，Controller → Service）
 *
 * @author hongfu_zhou@cacch.com
 */
@RestController
@RequestMapping(SystemPathConstants.PATH_MENUS)
@RequiredArgsConstructor
public class SysMenuController {

    private final ISysMenuService sysMenuService;

    private final SysMenuConverter sysMenuConverter;

    /**
     * 查询全部菜单树（含目录/菜单/按钮与停用节点，管理页展示与角色授权树）
     *
     * @return 菜单树根节点列表，无数据时为空列表
     */
    @GetMapping("/tree")
    @RequiresPermission(SystemPermissionConstants.MENU_LIST)
    public Result<List<MenuTreeVO>> tree() {
        return Result.success(sysMenuConverter.buildTree(sysMenuService.listAll()));
    }

    /**
     * 新增菜单/按钮
     *
     * @param request 菜单保存请求
     * @return 成功返回体（无数据）
     * @throws BizException 父节点不存在或权限标识重复时抛出
     */
    @PostMapping
    @RequiresPermission(SystemPermissionConstants.MENU_ADD)
    public Result<Void> create(@Valid @RequestBody MenuSaveRequest request) {
        sysMenuService.createMenu(sysMenuConverter.toDO(request, null));
        return Result.success();
    }

    /**
     * 编辑菜单/按钮
     *
     * @param id      菜单主键，对应 sys_menu.id
     * @param request 菜单保存请求
     * @return 成功返回体（无数据）
     * @throws BizException 菜单不存在或权限标识重复时抛出
     */
    @PutMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.MENU_EDIT)
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody MenuSaveRequest request) {
        SysMenuDO menu = sysMenuConverter.toDO(request, id);
        sysMenuService.updateMenu(menu);
        return Result.success();
    }

    /**
     * 删除菜单/按钮（存在子节点或已被角色授权时禁止删除）
     *
     * @param id 菜单主键，对应 sys_menu.id
     * @return 成功返回体（无数据）
     * @throws BizException 存在子节点（40020）或已被角色授权（40021）时抛出
     */
    @DeleteMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.MENU_DELETE)
    public Result<Void> delete(@PathVariable("id") Long id) {
        sysMenuService.deleteMenu(id);
        return Result.success();
    }
}

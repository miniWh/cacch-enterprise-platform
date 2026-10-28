package com.cacch.enterprise.controller.system;

import com.cacch.enterprise.common.annotation.RequiresPermission;
import com.cacch.enterprise.common.constant.system.SystemPathConstants;
import com.cacch.enterprise.common.constant.system.SystemPermissionConstants;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.Result;
import com.cacch.enterprise.convert.system.SysDeptConverter;
import com.cacch.enterprise.dao.entity.system.SysDeptDO;
import com.cacch.enterprise.dto.system.request.DeptSaveRequest;
import com.cacch.enterprise.dto.system.vo.DeptTreeVO;
import com.cacch.enterprise.service.system.api.ISysDeptService;
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
 * 部门管理 REST 接口（单聚合操作，Controller → Service）
 *
 * @author hongfu_zhou@cacch.com
 */
@RestController
@RequestMapping(SystemPathConstants.PATH_DEPTS)
@RequiredArgsConstructor
public class SysDeptController {

    private final ISysDeptService sysDeptService;

    private final SysDeptConverter sysDeptConverter;

    /**
     * 查询全部部门树（含停用部门，管理页展示与用户归属下拉）
     *
     * @return 部门树根节点列表，无数据时为空列表
     */
    @GetMapping("/tree")
    @RequiresPermission(SystemPermissionConstants.DEPT_LIST)
    public Result<List<DeptTreeVO>> tree() {
        return Result.success(sysDeptConverter.buildTree(sysDeptService.listAll()));
    }

    /**
     * 新增部门（自动计算祖先链）
     *
     * @param request 部门保存请求
     * @return 成功返回体（无数据）
     * @throws BizException 父部门不存在或同级名称重复时抛出
     */
    @PostMapping
    @RequiresPermission(SystemPermissionConstants.DEPT_ADD)
    public Result<Void> create(@Valid @RequestBody DeptSaveRequest request) {
        sysDeptService.createDept(sysDeptConverter.toDO(request, null));
        return Result.success();
    }

    /**
     * 编辑部门（变更父部门时重算自身与全部下级的祖先链）
     *
     * @param id      部门主键，对应 sys_dept.id
     * @param request 部门保存请求
     * @return 成功返回体（无数据）
     * @throws BizException 部门不存在、同级名称重复、环引用或父部门非法时抛出
     */
    @PutMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.DEPT_EDIT)
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody DeptSaveRequest request) {
        SysDeptDO dept = sysDeptConverter.toDO(request, id);
        sysDeptService.updateDept(dept);
        return Result.success();
    }

    /**
     * 删除部门（存在下级部门或部门下存在用户时禁止删除）
     *
     * @param id 部门主键，对应 sys_dept.id
     * @return 成功返回体（无数据）
     * @throws BizException 存在下级部门（40023）或存在归属用户（40024）时抛出
     */
    @DeleteMapping("/{id}")
    @RequiresPermission(SystemPermissionConstants.DEPT_DELETE)
    public Result<Void> delete(@PathVariable("id") Long id) {
        sysDeptService.deleteDept(id);
        return Result.success();
    }
}

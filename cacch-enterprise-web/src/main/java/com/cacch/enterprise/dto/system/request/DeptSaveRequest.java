package com.cacch.enterprise.dto.system.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 部门新增/编辑请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class DeptSaveRequest implements Serializable {

    /** 父部门 ID，0 表示根部门（仅初始化数据允许） */
    @NotNull(message = "父部门不能为空")
    @Min(value = 0, message = "父部门 ID 非法")
    private Long parentId;

    /** 部门名称，同级唯一 */
    @NotBlank(message = "部门名称不能为空")
    @Size(max = 32, message = "部门名称不能超过 32 个字符")
    private String deptName;

    /** 排序号，同层级内升序 */
    @NotNull(message = "排序号不能为空")
    private Integer orderNum;

    /** 负责人用户 ID，对应 sys_user.id，可空 */
    private Long leaderId;

    /** 状态：0 正常 / 1 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值非法")
    @Max(value = 1, message = "状态取值非法")
    private Integer status;
}

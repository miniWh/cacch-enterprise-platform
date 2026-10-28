package com.cacch.enterprise.dto.system.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色新增/编辑请求
 *
 * <p>新增时 roleCode 必填且全局唯一；编辑内置角色时编码与名称由后端拒绝变更。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class RoleSaveRequest implements Serializable {

    /** 角色编码：大写字母+下划线，全局唯一，新增必填 */
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{1,31}$", message = "角色编码须为大写字母开头，仅含大写字母、数字与下划线，长度 2~32")
    private String roleCode;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 32, message = "角色名称不能超过 32 个字符")
    private String roleName;

    /** 数据权限档位：1 全部 / 2 本部门 / 3 本部门及下级 / 4 仅本人 */
    @NotNull(message = "数据权限档位不能为空")
    @Min(value = 1, message = "数据权限档位非法")
    @Max(value = 4, message = "数据权限档位非法")
    private Integer dataScope;

    /** 状态：0 正常 / 1 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值非法")
    @Max(value = 1, message = "状态取值非法")
    private Integer status;

    /** 备注，可空 */
    @Size(max = 200, message = "备注不能超过 200 个字符")
    private String remark;
}

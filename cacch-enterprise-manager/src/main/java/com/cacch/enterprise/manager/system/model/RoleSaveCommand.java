package com.cacch.enterprise.manager.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 角色新增/编辑命令（服务层内部模型，经 web 层 MapStruct 转换自请求 DTO）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleSaveCommand implements Serializable {

    /** 角色编码（新增必填，编辑内置角色时不可变更） */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 数据权限档位：1 全部 / 2 本部门 / 3 本部门及下级 / 4 仅本人 */
    private Integer dataScope;

    /** 状态：0 正常 / 1 停用 */
    private Integer status;

    /** 备注 */
    private String remark;
}

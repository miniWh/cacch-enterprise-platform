package com.cacch.enterprise.dto.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色视图对象（分页列表）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysRoleVO implements Serializable {

    /** 角色主键，对应 sys_role.id */
    private Long id;

    /** 角色编码，全局唯一 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 数据权限档位：1 全部 / 2 本部门 / 3 本部门及下级 / 4 仅本人 */
    private Integer dataScope;

    /** 是否内置角色：0 否 / 1 是 */
    private Integer builtIn;

    /** 状态：0 正常 / 1 停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}

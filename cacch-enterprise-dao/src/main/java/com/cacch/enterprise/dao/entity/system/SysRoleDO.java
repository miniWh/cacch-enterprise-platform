package com.cacch.enterprise.dao.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.enterprise.dao.entity.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体，映射表 sys_role
 *
 * <p>角色是一组功能权限（菜单/按钮授权）与一个数据权限档位的命名集合；
 * role_code 创建后不可修改，内置角色不可删除。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRoleDO extends BaseDO {

    /**
     * 角色编码，全局唯一（逻辑删除后可复用），大写字母+下划线，创建后不可修改
     */
    private String roleCode;

    /**
     * 角色名称，全局唯一
     */
    private String roleName;

    /**
     * 数据权限档位：1 全部 / 2 本部门 / 3 本部门及下级 / 4 仅本人，见 DataScopeEnum
     */
    private Integer dataScope;

    /**
     * 是否内置角色：0 否 / 1 是（内置角色编码名称不可改、不可删除）
     */
    private Integer builtIn;

    /**
     * 状态：0 正常 / 1 停用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}

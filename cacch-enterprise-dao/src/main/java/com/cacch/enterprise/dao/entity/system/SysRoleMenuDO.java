package com.cacch.enterprise.dao.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.enterprise.dao.entity.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色-菜单授权关联实体，映射表 sys_role_menu
 *
 * <p>重新授权时对旧关联行做逻辑删除并插入新行，保留授权历史便于审计回溯。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
public class SysRoleMenuDO extends BaseDO {

    /** 角色 ID，对应 sys_role.id */
    private Long roleId;

    /** 菜单/按钮节点 ID，对应 sys_menu.id */
    private Long menuId;
}

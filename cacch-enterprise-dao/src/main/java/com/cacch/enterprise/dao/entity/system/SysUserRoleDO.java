package com.cacch.enterprise.dao.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.enterprise.dao.entity.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户-角色关联实体，映射表 sys_user_role
 *
 * <p>重新分配角色时对旧关联行做逻辑删除并插入新行，保留授权历史便于审计回溯。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRoleDO extends BaseDO {

    /**
     * 用户 ID，对应 sys_user.id
     */
    private Long userId;

    /**
     * 角色 ID，对应 sys_role.id
     */
    private Long roleId;
}

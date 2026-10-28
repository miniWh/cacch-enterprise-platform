package com.cacch.enterprise.manager.auth.model;

import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.service.system.model.UserPermissionModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录用户信息模型（服务层内部模型，经 web 层 MapStruct 转换为 UserProfileVO）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileModel implements Serializable {

    /**
     * 用户实体（不含密码）
     */
    private SysUserDO user;

    /**
     * 用户权限模型（菜单扁平列表 + 权限标识集合）
     */
    private UserPermissionModel permission;
}

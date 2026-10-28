package com.cacch.enterprise.service.system.model;

import com.cacch.enterprise.dao.entity.system.SysMenuDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 用户权限模型（服务层内部模型）
 *
 * <p>由权限加载服务回源构建：菜单列表供前端动态路由（构建树），
 * perms 供后端按钮鉴权与前端按钮显隐。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionModel implements Serializable {

    /** 启用状态菜单/按钮扁平列表（构建树前），无权限时为空列表 */
    private List<SysMenuDO> menus;

    /** 按钮权限标识集合，无权限时为空集合 */
    private Set<String> perms;
}

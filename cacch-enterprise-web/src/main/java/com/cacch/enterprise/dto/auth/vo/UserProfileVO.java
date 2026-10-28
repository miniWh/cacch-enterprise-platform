package com.cacch.enterprise.dto.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * 当前登录用户信息视图对象（用户资料 + 动态菜单树 + 按钮权限标识）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO implements Serializable {

    /**
     * 用户主键，对应 sys_user.id
     */
    private Long userId;

    /**
     * 工号（登录账号）
     */
    private String username;

    /**
     * 手机号（登录账号）
     */
    private String phone;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 归属部门 ID，对应 sys_dept.id，可为 null
     */
    private Long deptId;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 账号状态：0 启用 / 1 停用
     */
    private Integer status;

    /**
     * 动态菜单树（目录/菜单两级结构，按钮节点不在树中）
     */
    private List<MenuVO> menus;

    /**
     * 按钮权限标识集合（v-permission 指令显隐依据），无权限时为空集合
     */
    private Set<String> perms;
}

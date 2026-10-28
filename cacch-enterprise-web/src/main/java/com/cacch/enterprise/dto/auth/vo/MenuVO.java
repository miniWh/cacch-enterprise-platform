package com.cacch.enterprise.dto.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单/按钮树节点视图对象（当前登录用户动态菜单）
 *
 * <p>由 SysMenuDO 扁平列表按 parentId 组装为树；当前用户菜单树仅含目录与菜单节点，
 * 按钮节点不出现在路由树中（按钮权限走 perms 标识）。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuVO implements Serializable {

    /** 菜单主键，对应 sys_menu.id */
    private Long id;

    /** 父节点 ID，0 表示根节点 */
    private Long parentId;

    /** 名称（目录名/菜单名） */
    private String menuName;

    /** 节点类型：D 目录 / M 菜单 */
    private String menuType;

    /** 前端路由地址，如 /system/users */
    private String path;

    /** 前端组件路径，如 system/users/Users */
    private String component;

    /** 图标 */
    private String icon;

    /** 是否可见：0 显示 / 1 隐藏 */
    private Integer visible;

    /** 排序号，同层级内升序 */
    private Integer orderNum;

    /** 子节点列表，叶子节点为空列表 */
    private List<MenuVO> children;
}

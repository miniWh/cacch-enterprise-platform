package com.cacch.enterprise.dto.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单/按钮树节点视图对象（菜单管理页，含目录/菜单/按钮全部类型）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuTreeVO implements Serializable {

    /** 菜单主键，对应 sys_menu.id */
    private Long id;

    /** 父节点 ID，0 表示根节点 */
    private Long parentId;

    /** 名称（目录名/菜单名/按钮名） */
    private String menuName;

    /** 节点类型：D 目录 / M 菜单 / B 按钮 */
    private String menuType;

    /** 排序号，同层级内升序 */
    private Integer orderNum;

    /** 前端路由地址（目录/菜单填写） */
    private String path;

    /** 前端组件路径（菜单填写） */
    private String component;

    /** 权限标识（按钮填写） */
    private String perms;

    /** 图标 */
    private String icon;

    /** 是否可见：0 显示 / 1 隐藏 */
    private Integer visible;

    /** 状态：0 正常 / 1 停用 */
    private Integer status;

    /** 子节点列表，叶子节点为空列表 */
    private List<MenuTreeVO> children;
}

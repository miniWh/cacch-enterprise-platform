package com.cacch.enterprise.dao.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.enterprise.dao.entity.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单/按钮实体，映射表 sys_menu
 *
 * <p>以 目录(D) → 菜单(M) → 按钮(B) 三级树形组织；按钮节点 perms 为权限标识，全局唯一。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenuDO extends BaseDO {

    /** 父节点 ID，0 表示根节点 */
    private Long parentId;

    /** 名称（目录名/菜单名/按钮名） */
    private String menuName;

    /** 节点类型：D 目录 / M 菜单 / B 按钮 */
    private String menuType;

    /** 排序号，同层级内升序 */
    private Integer orderNum;

    /** 前端路由地址（目录/菜单填写，如 /system/users） */
    private String path;

    /** 前端组件路径（菜单填写，相对 src/views 的组件标识，如 system/users/Users） */
    private String component;

    /** 权限标识（按钮必填），全局唯一，形如 system:user:add */
    private String perms;

    /** 图标（目录/菜单可选） */
    private String icon;

    /** 是否可见：0 显示 / 1 隐藏（隐藏路由不注册、菜单不渲染） */
    private Integer visible;

    /** 状态：0 正常 / 1 停用 */
    private Integer status;
}

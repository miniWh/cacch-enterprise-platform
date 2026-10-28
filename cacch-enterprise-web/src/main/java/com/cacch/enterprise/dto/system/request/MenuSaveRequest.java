package com.cacch.enterprise.dto.system.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 菜单/按钮新增/编辑请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class MenuSaveRequest implements Serializable {

    /** 父节点 ID，0 表示根节点 */
    @NotNull(message = "父节点不能为空")
    @Min(value = 0, message = "父节点 ID 非法")
    private Long parentId;

    /** 名称（目录名/菜单名/按钮名） */
    @NotBlank(message = "名称不能为空")
    @Size(max = 32, message = "名称不能超过 32 个字符")
    private String menuName;

    /** 节点类型：D 目录 / M 菜单 / B 按钮 */
    @NotBlank(message = "节点类型不能为空")
    @Pattern(regexp = "^[DMB]$", message = "节点类型非法")
    private String menuType;

    /** 排序号，同层级内升序 */
    @NotNull(message = "排序号不能为空")
    private Integer orderNum;

    /** 前端路由地址（目录/菜单填写），可空 */
    @Size(max = 100, message = "路由地址不能超过 100 个字符")
    private String path;

    /** 前端组件路径（菜单填写），可空 */
    @Size(max = 100, message = "组件路径不能超过 100 个字符")
    private String component;

    /** 权限标识（按钮必填），全局唯一，形如 system:user:add */
    @Size(max = 64, message = "权限标识不能超过 64 个字符")
    private String perms;

    /** 图标（目录/菜单可选） */
    @Size(max = 64, message = "图标不能超过 64 个字符")
    private String icon;

    /** 是否可见：0 显示 / 1 隐藏 */
    @NotNull(message = "可见性不能为空")
    @Min(value = 0, message = "可见性取值非法")
    @Max(value = 1, message = "可见性取值非法")
    private Integer visible;

    /** 状态：0 正常 / 1 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值非法")
    @Max(value = 1, message = "状态取值非法")
    private Integer status;
}

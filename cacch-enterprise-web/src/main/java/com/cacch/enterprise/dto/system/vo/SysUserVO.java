package com.cacch.enterprise.dto.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图对象（列表/详情通用）
 *
 * <p>不含密码密文等敏感字段；详情场景（编辑回显）额外携带角色 ID 列表。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUserVO implements Serializable {

    /**
     * 用户主键，对应 sys_user.id
     */
    private Long id;

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
     * 归属部门 ID，对应 sys_dept.id
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
     * 数据来源：LOCAL 本地创建 / IHR 人事系统同步（预留）
     */
    private String source;

    /**
     * 持有角色 ID 列表（详情场景返回，列表场景为 null）
     */
    private List<Long> roleIds;

    /**
     * 最近登录时间
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

package com.cacch.enterprise.manager.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 编辑用户命令（服务层内部模型，经 web 层 MapStruct 转换自请求 DTO）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserCommand implements Serializable {

    /**
     * 用户主键
     */
    private Long userId;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 归属部门 ID
     */
    private Long deptId;

    /**
     * 手机号（登录账号）
     */
    private String phone;

    /**
     * 邮箱，可选
     */
    private String email;

    /**
     * 角色 ID 列表：null 表示不修改角色；非 null（含空列表）表示重新分配（空列表即清空角色）
     */
    private List<Long> roleIds;
}

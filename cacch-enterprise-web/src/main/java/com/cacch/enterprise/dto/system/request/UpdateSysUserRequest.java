package com.cacch.enterprise.dto.system.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 编辑用户请求
 *
 * <p>工号创建后不可修改；roleIds 为 null 表示不修改角色，非 null（含空列表）表示重新分配。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class UpdateSysUserRequest implements Serializable {

    /**
     * 姓名
     */
    @Size(max = 32, message = "姓名不能超过 32 个字符")
    private String realName;

    /**
     * 归属部门 ID，对应 sys_dept.id
     */
    private Long deptId;

    /**
     * 手机号（登录账号），11 位手机号格式，可空表示不修改
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱，可空
     */
    @Size(max = 64, message = "邮箱不能超过 64 个字符")
    private String email;

    /**
     * 角色 ID 列表：null 表示不修改角色；非 null（含空列表）表示重新分配
     */
    private List<Long> roleIds;
}

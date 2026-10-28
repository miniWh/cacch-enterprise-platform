package com.cacch.enterprise.dto.system.request;

import com.cacch.enterprise.common.constant.auth.AuthCommonConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 新增用户请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class CreateSysUserRequest implements Serializable {

    /**
     * 工号（登录账号），2~32 位字母/数字/下划线，全局唯一，创建后不可修改
     */
    @NotBlank(message = "工号不能为空")
    @Pattern(regexp = "^[A-Za-z0-9_]{2,32}$", message = "工号须为 2~32 位字母、数字或下划线")
    private String username;

    /**
     * 初始密码明文：8~64 位，须同时包含字母与数字
     */
    @NotBlank(message = "初始密码不能为空")
    @Pattern(regexp = AuthCommonConstants.PASSWORD_REGEX, message = "初始密码须为 8~64 位且同时包含字母与数字")
    private String password;

    /**
     * 姓名
     */
    @NotBlank(message = "姓名不能为空")
    @Size(max = 32, message = "姓名不能超过 32 个字符")
    private String realName;

    /**
     * 归属部门 ID，对应 sys_dept.id
     */
    @NotNull(message = "归属部门不能为空")
    private Long deptId;

    /**
     * 手机号（登录账号），11 位手机号格式，全局唯一，可空
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 邮箱，可空
     */
    @Size(max = 64, message = "邮箱不能超过 64 个字符")
    private String email;

    /**
     * 初始角色 ID 列表，可空（表示不分配角色）
     */
    private List<Long> roleIds;
}

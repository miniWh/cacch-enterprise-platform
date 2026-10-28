package com.cacch.enterprise.manager.system.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 新增用户命令（服务层内部模型，经 web 层 MapStruct 转换自请求 DTO）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserCommand implements Serializable {

    /** 工号（登录账号，创建后不可修改） */
    private String username;

    /** 明文密码，须满足复杂度要求，仅用于本次哈希，禁止落日志 */
    private String password;

    /** 姓名 */
    private String realName;

    /** 归属部门 ID */
    private Long deptId;

    /** 手机号（登录账号） */
    private String phone;

    /** 邮箱，可选 */
    private String email;

    /** 初始角色 ID 列表，可空（表示不分配角色） */
    private List<Long> roleIds;
}

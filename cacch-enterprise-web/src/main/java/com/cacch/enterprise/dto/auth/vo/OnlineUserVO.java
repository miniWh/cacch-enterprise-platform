package com.cacch.enterprise.dto.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 在线用户视图对象
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineUserVO implements Serializable {

    /** 用户主键，对应 sys_user.id */
    private Long userId;

    /** 工号（登录账号） */
    private String username;

    /** 姓名 */
    private String realName;

    /** 归属部门 ID */
    private Long deptId;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 登录来源 IP */
    private String loginIp;
}

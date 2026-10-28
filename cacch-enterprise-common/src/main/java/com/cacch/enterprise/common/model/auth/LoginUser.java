package com.cacch.enterprise.common.model.auth;

import com.cacch.enterprise.common.enums.system.DataScopeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 登录用户上下文
 *
 * <p>登录成功后写入 Redis 登录态（JSON）并在请求处理期经 {@code LoginUserHolder} 贯穿各层；
 * 仅携带鉴权必需字段，不含密码等敏感信息，payload 不携带权限集合（权限独立缓存，避免改权限需换发令牌）。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    /** 用户 ID，对应 sys_user.id */
    private Long userId;

    /** 工号（登录账号） */
    private String username;

    /** 姓名 */
    private String realName;

    /** 归属部门 ID，对应 sys_dept.id，顶级管理员可为 null */
    private Long deptId;

    /** 归属部门祖先链（含根 0、不含自身，逗号分隔），供「本部门及下级」数据权限查询 */
    private String deptAncestors;

    /** 持有的启用状态角色编码集合 */
    private Set<String> roleCodes;

    /** 持有角色的数据权限档位集合（多角色并集） */
    private Set<DataScopeEnum> dataScopes;

    /** 当前会话访问令牌 jti（互踢/强制下线比对依据） */
    private String jti;

    /** 登录时间 */
    private LocalDateTime loginTime;

    /** 登录来源 IP */
    private String loginIp;
}

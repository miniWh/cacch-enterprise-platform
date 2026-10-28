package com.cacch.enterprise.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一返回码枚举
 *
 * <p>号段约定：0 成功；400xx 客户端/参数错误；401xx 认证错误；403xx 权限错误；
 * 429xx 限流；404xx 资源不存在；500xx 第三方集成错误；999xx 系统内部错误。
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(0, "成功"),

    /** 参数错误 */
    PARAM_ERROR(40000, "参数错误"),
    /** 缺少必填参数 */
    PARAM_MISSING(40001, "缺少必填参数"),
    /** 参数格式不正确 */
    PARAM_INVALID(40002, "参数格式不正确"),

    /** 工号已被占用 */
    SYSTEM_USERNAME_DUPLICATE(40010, "工号已被占用"),
    /** 手机号已被占用 */
    SYSTEM_PHONE_DUPLICATE(40011, "手机号已被占用"),
    /** 角色编码已存在 */
    SYSTEM_ROLE_CODE_DUPLICATE(40012, "角色编码已存在"),
    /** 角色名称已存在 */
    SYSTEM_ROLE_NAME_DUPLICATE(40013, "角色名称已存在"),
    /** 权限标识已存在 */
    SYSTEM_PERMS_DUPLICATE(40014, "权限标识已存在"),
    /** 同级部门名称已存在 */
    SYSTEM_DEPT_NAME_DUPLICATE(40015, "同级部门名称已存在"),

    /** 菜单存在子节点，禁止删除 */
    SYSTEM_MENU_HAS_CHILDREN(40020, "存在子节点，禁止删除"),
    /** 菜单已被角色关联，禁止删除 */
    SYSTEM_MENU_IN_USE(40021, "菜单已被角色授权，禁止删除"),
    /** 角色已分配用户，禁止删除 */
    SYSTEM_ROLE_HAS_USERS(40022, "角色已分配用户，禁止删除"),
    /** 部门存在下级部门，禁止删除 */
    SYSTEM_DEPT_HAS_CHILDREN(40023, "存在下级部门，禁止删除"),
    /** 部门下存在归属用户，禁止删除 */
    SYSTEM_DEPT_HAS_USERS(40024, "部门下存在归属用户，禁止删除"),
    /** 工号或手机号均不能为空 */
    SYSTEM_ACCOUNT_EMPTY(40025, "工号与手机号不能为空"),

    /** 未登录或会话不存在 */
    AUTH_NOT_LOGIN(40100, "未登录或登录已过期"),
    /** 账号或密码错误（含账号不存在，防账号枚举） */
    AUTH_CREDENTIALS_INVALID(40101, "账号或密码错误"),
    /** 图形验证码错误或已过期 */
    AUTH_CAPTCHA_INVALID(40102, "验证码错误或已过期"),
    /** 账号已被停用/删除 */
    AUTH_ACCOUNT_DISABLED(40103, "账号已被停用"),
    /** 登录凭证已过期 */
    AUTH_TOKEN_EXPIRED(40104, "登录已过期，请重新登录"),
    /** 登录凭证无效 */
    AUTH_TOKEN_INVALID(40105, "无效的登录凭证"),
    /** 账号已在别处登录或被强制下线（互踢） */
    AUTH_KICKED_OUT(40106, "您的账号已在别处登录或被强制下线，请重新登录"),
    /** 原密码不正确 */
    AUTH_OLD_PASSWORD_INVALID(40107, "原密码不正确"),
    /** 刷新凭证无效或已过期 */
    AUTH_REFRESH_INVALID(40108, "登录已过期，请重新登录"),

    /** 没有该操作的权限 */
    AUTH_PERMISSION_DENIED(40300, "没有操作权限"),
    /** 超级管理员账号受保护 */
    SYSTEM_USER_PROTECTED(40301, "超级管理员账号受保护，禁止该操作"),
    /** 内置角色受保护 */
    SYSTEM_ROLE_PROTECTED(40302, "内置角色受保护，禁止该操作"),
    /** 内置菜单受保护 */
    SYSTEM_MENU_PROTECTED(40303, "内置菜单受保护，禁止删除"),

    /** 请求过于频繁（限流） */
    AUTH_RATE_LIMIT_EXCEEDED(42900, "请求过于频繁，请稍后再试"),

    /** 资源不存在 */
    RESOURCE_NOT_FOUND(40400, "资源不存在"),

    /** 第三方系统调用失败 */
    INTEGRATION_ERROR(50001, "第三方系统调用失败"),
    /** 第三方系统调用超时 */
    INTEGRATION_TIMEOUT(50002, "第三方系统调用超时"),

    /** 系统内部错误 */
    SYSTEM_ERROR(99900, "系统内部错误，请稍后再试");

    /** 业务返回码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;
}

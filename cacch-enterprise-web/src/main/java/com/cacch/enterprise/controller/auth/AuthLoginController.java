package com.cacch.enterprise.controller.auth;

import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.Result;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.common.utils.LoginUserHolder;
import com.cacch.enterprise.convert.auth.AuthLoginConverter;
import com.cacch.enterprise.dto.auth.request.ChangePasswordRequest;
import com.cacch.enterprise.dto.auth.request.LoginRequest;
import com.cacch.enterprise.dto.auth.request.RefreshTokenRequest;
import com.cacch.enterprise.dto.auth.vo.CaptchaVO;
import com.cacch.enterprise.dto.auth.vo.LoginTokenVO;
import com.cacch.enterprise.dto.auth.vo.UserProfileVO;
import com.cacch.enterprise.manager.auth.api.IAuthLoginManager;
import com.cacch.enterprise.service.auth.api.IAuthCaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证授权 REST 接口（验证码/登录/刷新/登出/个人信息）
 *
 * @author hongfu_zhou@cacch.com
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthLoginController {

    private final IAuthCaptchaService authCaptchaService;

    private final IAuthLoginManager authLoginManager;

    private final AuthLoginConverter authLoginConverter;

    /**
     * 获取图形验证码
     *
     * @param request HTTP 请求（取客户端 IP 做限流）
     * @return 验证码标识与图片 Base64
     */
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha(HttpServletRequest request) {
        return Result.success(authLoginConverter.toCaptchaVO(
                authCaptchaService.generateCaptcha(resolveClientIp(request))));
    }

    /**
     * 账号密码登录
     *
     * @param request        登录请求（账号、密码、验证码）
     * @param servletRequest HTTP 请求（取客户端 IP 做限流与登录留痕）
     * @return 双令牌对
     * @throws BizException 限流（42900）、验证码无效（40102）、凭据错误（40101）、账号停用（40103）时抛出
     */
    @PostMapping("/login")
    public Result<LoginTokenVO> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletRequest servletRequest) {
        var command = authLoginConverter.toCommand(request, resolveClientIp(servletRequest));
        var tokens = authLoginManager.login(command, request.getCaptchaId(), request.getCaptchaCode());
        return Result.success(authLoginConverter.toTokenVO(tokens));
    }

    /**
     * 静默刷新双令牌（旧 refresh 立即失效）
     *
     * @param request 刷新请求（携带 refreshToken）
     * @return 新的双令牌对
     * @throws BizException 刷新令牌无效或已过期（40108）时抛出
     */
    @PostMapping("/refresh")
    public Result<LoginTokenVO> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Result.success(authLoginConverter.toTokenVO(authLoginManager.refresh(request.getRefreshToken())));
    }

    /**
     * 登出（删除当前用户 Redis 登录态与刷新凭证）
     *
     * @return 成功返回体（无数据）
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = LoginUserHolder.getUserId();
        if (userId == null) {
            throw new BizException(ResultCode.AUTH_NOT_LOGIN);
        }
        authLoginManager.logout(userId);
        return Result.success();
    }

    /**
     * 查询当前登录用户信息（资料 + 动态菜单树 + 权限标识）
     *
     * @return 当前用户视图对象
     * @throws BizException 用户不存在（已被删除）时抛出
     */
    @GetMapping("/profile")
    public Result<UserProfileVO> profile() {
        return Result.success(authLoginConverter.toProfileVO(
                authLoginManager.loadProfile(LoginUserHolder.getUserId())));
    }

    /**
     * 修改密码（校验原密码，成功后清除全部会话强制重新登录）
     *
     * @param request 修改密码请求（原密码、新密码）
     * @return 成功返回体（无数据）
     * @throws BizException 原密码错误（40107）或新密码不满足复杂度（40002）时抛出
     */
    @PutMapping("/profile/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authLoginManager.changePassword(LoginUserHolder.getUserId(),
                request.getOldPassword(), request.getNewPassword());
        return Result.success();
    }

    /**
     * 解析客户端 IP（优先 X-Forwarded-For 首段，回退 remoteAddr）
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

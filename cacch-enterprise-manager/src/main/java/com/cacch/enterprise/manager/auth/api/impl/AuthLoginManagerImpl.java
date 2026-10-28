package com.cacch.enterprise.manager.auth.api.impl;

import com.cacch.enterprise.common.constant.auth.AuthCommonConstants;
import com.cacch.enterprise.common.constant.system.SystemCommonConstants;
import com.cacch.enterprise.common.enums.auth.AuthChannelEnum;
import com.cacch.enterprise.common.enums.system.DataScopeEnum;
import com.cacch.enterprise.common.event.auth.AuthLoginEvent;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.model.auth.LoginUser;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.common.security.JwtTokenSupport;
import com.cacch.enterprise.common.utils.PasswordAppSupport;
import com.cacch.enterprise.dao.entity.system.SysDeptDO;
import com.cacch.enterprise.dao.entity.system.SysRoleDO;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.manager.auth.api.IAuthLoginManager;
import com.cacch.enterprise.manager.auth.api.IAuthPermissionManager;
import com.cacch.enterprise.manager.auth.model.ProfileModel;
import com.cacch.enterprise.manager.auth.model.TokenPairModel;
import com.cacch.enterprise.service.auth.api.IAuthCaptchaService;
import com.cacch.enterprise.service.auth.api.IAuthChannelService;
import com.cacch.enterprise.service.auth.model.AuthLoginCommand;
import com.cacch.enterprise.service.system.api.ISysDeptService;
import com.cacch.enterprise.service.system.api.ISysRoleService;
import com.cacch.enterprise.service.system.api.ISysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录认证编排实现（验证码 → 渠道认证 → 登录态 → 双令牌 → 事件）
 *
 * <p>多端登录策略 KICK_OUT：登录态 Key 按用户维度单会话，重复登录覆盖旧 Key 实现互踢。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLoginManagerImpl implements IAuthLoginManager {

    private final List<IAuthChannelService> authChannels;
    private final IAuthCaptchaService authCaptchaService;
    private final ISysUserService sysUserService;
    private final ISysRoleService sysRoleService;
    private final ISysDeptService sysDeptService;
    private final IAuthPermissionManager authPermissionManager;
    private final JwtTokenSupport jwtTokenSupport;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 账号密码登录编排
     *
     * @param command     登录命令
     * @param captchaId   图形验证码标识
     * @param captchaCode 用户输入验证码
     * @return 双令牌对
     * @throws BizException 限流/验证码无效/凭据错误/账号停用时抛出
     */
    @Override
    public TokenPairModel login(AuthLoginCommand command, String captchaId, String captchaCode) {
        assertLoginRateLimit(command.getLoginIp());
        authCaptchaService.verifyCaptcha(captchaId, captchaCode);
        IAuthChannelService channel = resolveChannel(AuthChannelEnum.PASSWORD);
        SysUserDO user;
        try {
            user = channel.authenticate(command);
        } catch (BizException e) {
            publishLoginEvent(false, command, null, e.getMessage());
            throw e;
        }
        LoginUser loginUser = buildLoginUser(user, command.getLoginIp());
        TokenPairModel tokens = issueTokens(loginUser);
        publishLoginEvent(true, command, user.getId(), null);
        log.info("【AuthLogin】登录成功，userId={}, username={}, ip={}", user.getId(), user.getUsername(), command.getLoginIp());
        return tokens;
    }

    /**
     * 静默刷新双令牌（轮换：旧 refresh jti 立即失效）
     *
     * @param refreshToken 刷新令牌
     * @return 新的双令牌对
     * @throws BizException 刷新令牌无效、会话不存在或 jti 不匹配时抛出
     */
    @Override
    public TokenPairModel refresh(String refreshToken) {
        JwtTokenSupport.ParsedToken parsed = jwtTokenSupport.parse(refreshToken);
        if (!JwtTokenSupport.TYPE_REFRESH.equals(parsed.type())) {
            log.info("【AuthLogin】刷新失败：令牌类型错误，userId={}", parsed.userId());
            throw new BizException(ResultCode.AUTH_REFRESH_INVALID);
        }
        String refreshKey = AuthCommonConstants.refreshTokenRedisKey(parsed.userId());
        String storedJti = stringRedisTemplate.opsForValue().get(refreshKey);
        if (storedJti == null || !storedJti.equals(parsed.jti())) {
            log.info("【AuthLogin】刷新失败：刷新凭证无效或已过期，userId={}", parsed.userId());
            throw new BizException(ResultCode.AUTH_REFRESH_INVALID);
        }
        LoginUser loginUser = readLoginUser(parsed.userId());
        if (loginUser == null) {
            log.info("【AuthLogin】刷新失败：登录态已不存在，userId={}", parsed.userId());
            throw new BizException(ResultCode.AUTH_REFRESH_INVALID);
        }
        String newAccessJti = JwtTokenSupport.newJti();
        String newRefreshJti = JwtTokenSupport.newJti();
        loginUser.setJti(newAccessJti);
        writeLoginState(loginUser, newRefreshJti);
        log.info("【AuthLogin】刷新双令牌成功（轮换），userId={}", parsed.userId());
        return new TokenPairModel(
                jwtTokenSupport.createAccessToken(loginUser.getUserId(), loginUser.getUsername(), newAccessJti),
                jwtTokenSupport.createRefreshToken(loginUser.getUserId(), loginUser.getUsername(), newRefreshJti),
                getSecurityExpireSeconds());
    }

    /**
     * 登出：删除登录态与刷新凭证
     *
     * @param userId 当前登录用户 ID
     */
    @Override
    public void logout(Long userId) {
        deleteSessionKeys(userId);
        log.info("【AuthLogin】用户登出，userId={}", userId);
    }

    /**
     * 加载当前登录用户信息（用户资料 + 权限模型，权限走缓存）
     *
     * @param userId 当前登录用户 ID
     * @return 用户信息模型
     * @throws BizException 用户不存在时抛出
     */
    @Override
    public ProfileModel loadProfile(Long userId) {
        SysUserDO user = sysUserService.getById(userId);
        if (user == null) {
            log.info("【AuthLogin】加载用户信息终止：用户不存在，userId={}", userId);
            throw new BizException(ResultCode.AUTH_NOT_LOGIN);
        }
        user.setPassword(null);
        return new ProfileModel(user, authPermissionManager.loadWithCache(userId));
    }

    /**
     * 修改密码（校验原密码 → 独立事务更新密文 → 删除全部会话）
     *
     * @param userId      当前登录用户 ID
     * @param oldPassword 原密码明文
     * @param newPassword 新密码明文
     * @throws BizException 原密码错误或新密码不满足复杂度时抛出
     */
    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUserDO user = sysUserService.getById(userId);
        if (user == null) {
            log.info("【AuthLogin】修改密码终止：用户不存在，userId={}", userId);
            throw new BizException(ResultCode.AUTH_NOT_LOGIN);
        }
        if (!PasswordAppSupport.verify(oldPassword, user.getPassword())) {
            log.info("【AuthLogin】修改密码终止：原密码错误，userId={}", userId);
            throw new BizException(ResultCode.AUTH_OLD_PASSWORD_INVALID);
        }
        if (!PasswordAppSupport.matchesComplexity(newPassword)) {
            log.info("【AuthLogin】修改密码终止：新密码不满足复杂度，userId={}", userId);
            throw new BizException(ResultCode.PARAM_INVALID, "新密码须为 8~64 位且同时包含字母与数字");
        }
        sysUserService.updatePassword(userId, PasswordAppSupport.hash(newPassword));
        deleteSessionKeys(userId);
        log.info("【AuthLogin】修改密码成功，全部会话已清除，userId={}", userId);
    }

    /**
     * 查询在线用户列表（SCAN 登录态 Key，逐条读取）
     *
     * @return 在线用户上下文列表
     */
    @Override
    public List<LoginUser> listOnlineUsers() {
        List<LoginUser> users = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(AuthCommonConstants.loginTokenScanPattern()).count(200).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                LoginUser user = readLoginUserJson(stringRedisTemplate.opsForValue().get(cursor.next()));
                if (user != null) {
                    users.add(user);
                }
            }
        }
        return users;
    }

    /**
     * 强制下线指定用户
     *
     * @param userId 目标用户 ID
     */
    @Override
    public void forceOffline(Long userId) {
        deleteSessionKeys(userId);
        log.info("【AuthLogin】强制下线完成，userId={}", userId);
    }

    /**
     * 登录接口 IP 限流校验（窗口 60 秒，超限抛 42900）
     *
     * @param ip 客户端 IP
     * @throws BizException 超出阈值时抛出
     */
    private void assertLoginRateLimit(String ip) {
        String limitKey = AuthCommonConstants.loginRateLimitRedisKey(ip);
        Long count = stringRedisTemplate.opsForValue().increment(limitKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(limitKey, Duration.ofSeconds(AuthCommonConstants.RATE_LIMIT_WINDOW_SECONDS));
        }
        if (count != null && count > AuthCommonConstants.LOGIN_RATE_LIMIT_COUNT) {
            log.info("【AuthLogin】登录接口触发限流，ip={}, count={}", ip, count);
            throw new BizException(ResultCode.AUTH_RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * 按渠道枚举解析渠道实现（策略模式扩展点）
     *
     * @param channelEnum 渠道枚举
     * @return 渠道实现
     * @throws BizException 渠道未实现时抛出
     */
    private IAuthChannelService resolveChannel(AuthChannelEnum channelEnum) {
        return authChannels.stream()
                .filter(c -> c.channel() == channelEnum)
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.AUTH_TOKEN_INVALID, "登录渠道暂未开放"));
    }

    /**
     * 构建登录上下文（角色编码、数据权限档位并集、部门祖先链）
     *
     * @param user 认证通过的用户实体
     * @param ip   登录来源 IP
     * @return 登录上下文
     */
    private LoginUser buildLoginUser(SysUserDO user, String ip) {
        List<SysRoleDO> roles = sysRoleService.listEnabledByUserId(user.getId());
        Set<String> roleCodes = roles.stream().map(SysRoleDO::getRoleCode).collect(Collectors.toSet());
        Set<DataScopeEnum> scopes = roleCodes.contains(SystemCommonConstants.ROLE_SUPER_ADMIN)
                ? Set.of(DataScopeEnum.ALL)
                : roles.stream()
                        .map(r -> DataScopeEnum.of(r.getDataScope()))
                        .filter(s -> s != null)
                        .collect(Collectors.toSet());
        if (scopes.isEmpty()) {
            scopes = Set.of(DataScopeEnum.SELF);
        }
        String deptAncestors = "";
        if (user.getDeptId() != null) {
            SysDeptDO dept = sysDeptService.getById(user.getDeptId());
            if (dept != null) {
                deptAncestors = dept.getAncestors();
            }
        }
        return LoginUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .deptAncestors(deptAncestors)
                .roleCodes(roleCodes)
                .dataScopes(scopes)
                .jti(JwtTokenSupport.newJti())
                .loginTime(LocalDateTime.now())
                .loginIp(ip)
                .build();
    }

    /**
     * 写入 Redis 登录态并签发双令牌（KICK_OUT：覆盖旧 Key 实现互踢）
     *
     * @param loginUser 登录上下文（含 access jti）
     * @return 双令牌对
     */
    private TokenPairModel issueTokens(LoginUser loginUser) {
        String refreshJti = JwtTokenSupport.newJti();
        writeLoginState(loginUser, refreshJti);
        return new TokenPairModel(
                jwtTokenSupport.createAccessToken(loginUser.getUserId(), loginUser.getUsername(), loginUser.getJti()),
                jwtTokenSupport.createRefreshToken(loginUser.getUserId(), loginUser.getUsername(), refreshJti),
                getSecurityExpireSeconds());
    }

    /**
     * 写入登录态与刷新凭证 Key（覆盖写，TTL 分别为访问/刷新令牌有效期）
     *
     * @param loginUser  登录上下文
     * @param refreshJti 刷新令牌 jti
     */
    private void writeLoginState(LoginUser loginUser, String refreshJti) {
        try {
            stringRedisTemplate.opsForValue().set(AuthCommonConstants.loginTokenRedisKey(loginUser.getUserId()),
                    objectMapper.writeValueAsString(loginUser),
                    Duration.ofSeconds(getSecurityExpireSeconds()));
            stringRedisTemplate.opsForValue().set(AuthCommonConstants.refreshTokenRedisKey(loginUser.getUserId()),
                    refreshJti, Duration.ofSeconds(getRefreshExpireSeconds()));
        } catch (Exception e) {
            log.error("【AuthLogin】写入 Redis 登录态失败，userId={}", loginUser.getUserId(), e);
            throw new BizException(ResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 读取 Redis 登录态并反序列化
     *
     * @param userId 用户 ID
     * @return 登录上下文；不存在时返回 null
     */
    private LoginUser readLoginUser(Long userId) {
        return readLoginUserJson(stringRedisTemplate.opsForValue().get(AuthCommonConstants.loginTokenRedisKey(userId)));
    }

    /**
     * 反序列化登录态 JSON
     *
     * @param json 登录态 JSON
     * @return 登录上下文；JSON 为空或解析失败时返回 null
     */
    private LoginUser readLoginUserJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, LoginUser.class);
        } catch (Exception e) {
            log.error("【AuthLogin】登录态 JSON 反序列化失败", e);
            return null;
        }
    }

    /**
     * 删除指定用户的登录态与刷新凭证（登出/强制下线/停用/改密/删号共用）
     *
     * @param userId 用户 ID
     */
    private void deleteSessionKeys(Long userId) {
        stringRedisTemplate.delete(AuthCommonConstants.loginTokenRedisKey(userId));
        stringRedisTemplate.delete(AuthCommonConstants.refreshTokenRedisKey(userId));
    }

    /**
     * 发布登录事件（成功/失败统一），事件异步消费
     *
     * @param success   是否成功
     * @param command   登录命令
     * @param userId    用户 ID（失败且账号不存在时为 null）
     * @param failReason 失败原因（成功时为 null）
     */
    private void publishLoginEvent(boolean success, AuthLoginCommand command, Long userId, String failReason) {
        eventPublisher.publishEvent(AuthLoginEvent.builder()
                .success(success)
                .channel(AuthChannelEnum.PASSWORD)
                .userId(userId)
                .account(command.getAccount())
                .loginIp(command.getLoginIp())
                .failReason(failReason)
                .eventTime(LocalDateTime.now())
                .build());
    }

    /**
     * 访问令牌有效期（秒）
     *
     * @return 有效期
     */
    private long getSecurityExpireSeconds() {
        return jwtTokenSupport.getSecurityProperties().getAccessTokenExpire();
    }

    /**
     * 刷新令牌有效期（秒）
     *
     * @return 有效期
     */
    private long getRefreshExpireSeconds() {
        return jwtTokenSupport.getSecurityProperties().getRefreshTokenExpire();
    }
}

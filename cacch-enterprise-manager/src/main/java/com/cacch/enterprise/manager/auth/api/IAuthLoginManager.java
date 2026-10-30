package com.cacch.enterprise.manager.auth.api;

import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.model.auth.LoginUser;
import com.cacch.enterprise.manager.auth.model.ProfileModel;
import com.cacch.enterprise.manager.auth.model.TokenPairModel;
import com.cacch.enterprise.service.auth.model.AuthLoginCommand;

import java.util.List;

/**
 * 登录认证编排接口（验证码 → 渠道认证 → 登录态 → 双令牌）
 *
 * <p>登录链路无数据库写操作（Redis 与事件除外），不声明数据库事务。
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IAuthLoginManager {

    /**
     * 账号密码登录编排
     *
     * <p>流程：IP 限流校验 → 图形验证码校验（一次性）→ 渠道身份认证 → 构建登录上下文
     * → 写入 Redis 登录态（KICK_OUT 互踢：旧会话 Key 被覆盖）→ 签发双令牌 → 发布登录事件。
     *
     * @param command     登录命令（账号、密码、来源 IP）
     * @param captchaId   图形验证码标识
     * @param captchaCode 用户输入验证码
     * @return 双令牌对
     * @throws BizException 限流（42900）、验证码无效（40102）、凭据错误（40101）、账号停用（40103）时抛出
     */
    TokenPairModel login(AuthLoginCommand command, String captchaId, String captchaCode);

    /**
     * 企业微信自建应用免登编排
     *
     * <p>流程：企微授权码认证（不走验证码/限流）→ 构建登录上下文
     * → 写入 Redis 登录态（KICK_OUT 互踢）→ 签发双令牌 → 发布登录事件。
     *
     * @param code    企微一次性授权码，由企微工作台打开应用主页 URL 时携带
     * @param loginIp 登录来源 IP
     * @return 双令牌对
     * @throws BizException 授权码无效（40002）、企微账号未绑定（40109）、账号停用（40103）时抛出
     */
    TokenPairModel loginByWecom(String code, String loginIp);

    /**
     * 静默刷新双令牌（轮换：旧 refresh 立即失效）
     *
     * @param refreshToken 刷新令牌
     * @return 新的双令牌对
     * @throws BizException 刷新令牌无效或已过期（40108）时抛出
     */
    TokenPairModel refresh(String refreshToken);

    /**
     * 登出：删除当前用户 Redis 登录态与刷新凭证
     *
     * @param userId 当前登录用户 ID
     */
    void logout(Long userId);

    /**
     * 加载当前登录用户信息（用户资料 + 菜单 + 权限标识，权限走缓存回源）
     *
     * @param userId 当前登录用户 ID
     * @return 用户信息模型
     * @throws BizException 用户不存在（已被删除）时抛出
     */
    ProfileModel loadProfile(Long userId);

    /**
     * 修改密码（校验原密码 → 更新密文 → 删除全部会话，强制重新登录）
     *
     * @param userId      当前登录用户 ID
     * @param oldPassword 原密码明文
     * @param newPassword 新密码明文
     * @throws BizException 原密码错误（40107）或新密码不满足复杂度（40002）时抛出
     */
    void changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 查询在线用户列表（扫描 Redis 登录态 Key）
     *
     * @return 在线用户上下文列表（含登录时间与来源 IP）
     */
    List<LoginUser> listOnlineUsers();

    /**
     * 强制下线指定用户（删除其登录态与刷新凭证）
     *
     * @param userId 目标用户 ID
     */
    void forceOffline(Long userId);
}

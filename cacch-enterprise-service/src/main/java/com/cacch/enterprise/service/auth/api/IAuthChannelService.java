package com.cacch.enterprise.service.auth.api;

import com.cacch.enterprise.common.enums.auth.AuthChannelEnum;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.service.auth.model.AuthLoginCommand;

/**
 * 登录渠道策略接口（策略模式扩展点）
 *
 * <p>每种登录渠道一个实现（本期仅 {@code PasswordAuthChannelServiceImpl}），
 * 登录编排层按渠道标识分派调用；后续企微扫码/自建应用免登新增实现类即可，核心编排不变。
 * 实现类只负责身份认证与账号状态校验，登录态/令牌组装由 Manager 编排。
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IAuthChannelService {

    /**
     * 当前实现对应的登录渠道
     *
     * @return 渠道枚举，登录编排按此分派
     */
    AuthChannelEnum channel();

    /**
     * 执行渠道身份认证
     *
     * @param command 登录命令（账号、密码、来源 IP 等）
     * @return 认证通过的用户实体（不含令牌与登录态组装）
     * @throws BizException 认证失败（凭据错误/账号停用/验证码无效等）时抛出
     */
    SysUserDO authenticate(AuthLoginCommand command);
}

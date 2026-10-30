package com.cacch.enterprise.service.auth.api.impl;

import com.cacch.enterprise.common.enums.auth.AuthChannelEnum;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.integration.wecom.client.WeComAuthClient;
import com.cacch.enterprise.integration.wecom.client.dto.WeComUserInfoResponse;
import com.cacch.enterprise.service.auth.api.IAuthChannelService;
import com.cacch.enterprise.service.auth.model.AuthLoginCommand;
import com.cacch.enterprise.service.system.api.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 企业微信自建应用免登渠道实现
 *
 * <p>用户从企微工作台点击应用进入，企微携带一次性授权码 code 打开应用主页；
 * 本渠道用 code 换取企微 userid，再按 sys_user.wecom_userid 查找绑定用户，
 * 完成身份认证。不走验证码与 IP 限流。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeComInAppChannelServiceImpl implements IAuthChannelService {

    private final WeComAuthClient weComAuthClient;
    private final ISysUserService sysUserService;

    /**
     * 渠道标识：企微自建应用免登
     *
     * @return WECOM_INAPP
     */
    @Override
    public AuthChannelEnum channel() {
        return AuthChannelEnum.WECOM_INAPP;
    }

    /**
     * 企微授权码认证（code → userid → 绑定用户）
     *
     * @param command 登录命令（携带 code）
     * @return 认证通过的用户实体
     * @throws BizException code 为空、企微 API 调用失败、未绑定用户、账号停用时抛出
     */
    @Override
    public SysUserDO authenticate(AuthLoginCommand command) {
        String code = command.getCode();
        if (code == null || code.isBlank()) {
            log.info("【AuthChannel】企微免登终止：授权码为空");
            throw new BizException(ResultCode.PARAM_INVALID, "企微授权码不能为空");
        }
        WeComUserInfoResponse userInfo = weComAuthClient.getUserByCode(code);
        String wecomUserid = userInfo.getUserid();
        if (wecomUserid == null || wecomUserid.isBlank()) {
            log.info("【AuthChannel】企微免登终止：未获取到企微 userid");
            throw new BizException(ResultCode.AUTH_WECOM_NOT_BOUND);
        }
        SysUserDO user = sysUserService.getByWecomUserid(wecomUserid);
        if (user == null) {
            log.info("【AuthChannel】企微免登终止：企微账号未绑定管理平台用户，wecomUserid={}", wecomUserid);
            throw new BizException(ResultCode.AUTH_WECOM_NOT_BOUND);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            log.info("【AuthChannel】企微免登终止：账号已停用，userId={}", user.getId());
            throw new BizException(ResultCode.AUTH_ACCOUNT_DISABLED);
        }
        log.info("【AuthChannel】企微免登认证通过，userId={}, wecomUserid={}", user.getId(), wecomUserid);
        return user;
    }
}

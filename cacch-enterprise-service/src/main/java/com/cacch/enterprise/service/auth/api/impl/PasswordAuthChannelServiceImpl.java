package com.cacch.enterprise.service.auth.api.impl;

import com.cacch.enterprise.common.constant.auth.AuthCommonConstants;
import com.cacch.enterprise.common.enums.auth.AuthChannelEnum;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.common.utils.PasswordAppSupport;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.service.auth.api.IAuthChannelService;
import com.cacch.enterprise.service.auth.model.AuthLoginCommand;
import com.cacch.enterprise.service.system.api.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 账号密码登录渠道实现
 *
 * <p>按账号格式自动识别工号/手机号（11 位手机号正则），仅做身份认证与账号状态校验；
 * 登录态写入、令牌签发由 Manager 编排。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordAuthChannelServiceImpl implements IAuthChannelService {

    private final ISysUserService sysUserService;

    /**
     * 渠道标识：账号密码登录
     *
     * @return PASSWORD
     */
    @Override
    public AuthChannelEnum channel() {
        return AuthChannelEnum.PASSWORD;
    }

    /**
     * 工号/手机号 + 密码认证
     *
     * @param command 登录命令（账号、密码、来源 IP）
     * @return 认证通过的用户实体
     * @throws BizException 账号不存在或密码错误（统一 40101，防账号枚举）、账号停用（40103）
     */
    @Override
    public SysUserDO authenticate(AuthLoginCommand command) {
        String account = command.getAccount();
        boolean isPhone = account.matches(AuthCommonConstants.PHONE_REGEX);
        SysUserDO user = isPhone ? sysUserService.getByPhone(account) : sysUserService.getByUsername(account);
        if (user == null) {
            log.info("【AuthChannel】账号密码登录失败：账号不存在，accountType={}", isPhone ? "PHONE" : "USERNAME");
            throw new BizException(ResultCode.AUTH_CREDENTIALS_INVALID);
        }
        if (PasswordAppSupport.verify(command.getPassword(), user.getPassword())) {
            log.info("【AuthChannel】账号密码登录失败：密码错误，userId={}", user.getId());
            throw new BizException(ResultCode.AUTH_CREDENTIALS_INVALID);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            log.info("【AuthChannel】账号密码登录终止：账号已停用，userId={}", user.getId());
            throw new BizException(ResultCode.AUTH_ACCOUNT_DISABLED);
        }
        log.info("【AuthChannel】账号密码认证通过，userId={}, accountType={}", user.getId(), isPhone ? "PHONE" : "USERNAME");
        return user;
    }
}

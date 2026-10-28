package com.cacch.enterprise.service.auth.api;

import com.cacch.enterprise.service.auth.model.CaptchaResult;

/**
 * 图形验证码服务接口
 *
 * <p>验证码一次性有效：生成后缓存 300 秒，校验时无论对错立即删除；
 * 生成接口按客户端 IP 限流。
 *
 * @author hongfu_zhou@cacch.com
 */
public interface IAuthCaptchaService {

    /**
     * 生成图形验证码并缓存
     *
     * @param clientIp 客户端 IP，用于生成接口限流计数
     * @return 验证码结果（captchaId 与 Base64 图片数据，data URI 格式）
     * @throws BizException 超出 IP 限流阈值时抛出（42900）
     */
    CaptchaResult generateCaptcha(String clientIp);

    /**
     * 校验验证码（一次性，校验后立即删除缓存）
     *
     * @param captchaId   验证码唯一标识，登录时回传
     * @param captchaCode 用户输入的验证码，忽略大小写
     * @throws BizException 验证码为空、已过期或输入错误时抛出（40102）
     */
    void verifyCaptcha(String captchaId, String captchaCode);
}

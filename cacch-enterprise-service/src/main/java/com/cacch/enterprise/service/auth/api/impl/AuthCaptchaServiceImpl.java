package com.cacch.enterprise.service.auth.api.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.cacch.enterprise.common.constant.auth.AuthCommonConstants;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.service.auth.api.IAuthCaptchaService;
import com.cacch.enterprise.service.auth.model.CaptchaResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 图形验证码服务实现
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthCaptchaServiceImpl implements IAuthCaptchaService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成图形验证码并写入 Redis（TTL 300 秒，一次性）
     *
     * @param clientIp 客户端 IP
     * @return 验证码结果
     * @throws BizException 超出 IP 限流阈值时抛出
     */
    @Override
    public CaptchaResult generateCaptcha(String clientIp) {
        String limitKey = AuthCommonConstants.captchaRateLimitRedisKey(clientIp);
        Long count = stringRedisTemplate.opsForValue().increment(limitKey);
        if (count != null && count == 1) {
            stringRedisTemplate.expire(limitKey, Duration.ofSeconds(AuthCommonConstants.RATE_LIMIT_WINDOW_SECONDS));
        }
        if (count != null && count > AuthCommonConstants.CAPTCHA_RATE_LIMIT_COUNT) {
            log.info("【AuthCaptcha】验证码接口触发限流，ip={}, count={}", clientIp, count);
            throw new BizException(ResultCode.AUTH_RATE_LIMIT_EXCEEDED);
        }
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(130, 48, 4, 30);
        String captchaId = IdUtil.fastSimpleUUID();
        stringRedisTemplate.opsForValue().set(AuthCommonConstants.captchaRedisKey(captchaId), captcha.getCode(),
                Duration.ofSeconds(AuthCommonConstants.CAPTCHA_EXPIRE_SECONDS));
        log.info("【AuthCaptcha】生成验证码成功，captchaId={}, ip={}", captchaId, clientIp);
        return new CaptchaResult(captchaId, captcha.getImageBase64Data());
    }

    /**
     * 校验验证码（无论结果如何先删除缓存，保证一次性）
     *
     * @param captchaId   验证码唯一标识
     * @param captchaCode 用户输入验证码
     * @throws BizException 验证码为空、已过期或错误时抛出
     */
    @Override
    public void verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaId.isBlank() || captchaCode == null || captchaCode.isBlank()) {
            log.info("【AuthCaptcha】验证码参数缺失，校验直接失败");
            throw new BizException(ResultCode.AUTH_CAPTCHA_INVALID);
        }
        String key = AuthCommonConstants.captchaRedisKey(captchaId);
        String stored = stringRedisTemplate.opsForValue().get(key);
        stringRedisTemplate.delete(key);
        if (stored == null || !stored.equalsIgnoreCase(captchaCode)) {
            log.info("【AuthCaptcha】验证码校验失败（错误或已过期），captchaId={}", captchaId);
            throw new BizException(ResultCode.AUTH_CAPTCHA_INVALID);
        }
        log.info("【AuthCaptcha】验证码校验通过，captchaId={}", captchaId);
    }
}

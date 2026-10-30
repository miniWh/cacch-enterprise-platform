package com.cacch.enterprise.integration.wecom.client;

import com.cacch.enterprise.common.config.auth.WeComProperties;
import com.cacch.enterprise.common.constant.auth.WeComAuthConstants;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.integration.wecom.client.dto.WeComAccessTokenResponse;
import com.cacch.enterprise.integration.wecom.client.dto.WeComUserInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 企业微信自建应用 HTTP 客户端
 *
 * <p>封装 access_token 获取（带 Redis 缓存）与根据授权码换取用户身份两个核心接口。
 * access_token 缓存 TTL 略短于企微官方有效期（7200s），避免边界失效。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeComAuthClient {

    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final WeComProperties weComProperties;

    /**
     * 获取企业微信 access_token（优先 Redis 缓存，miss 时调企微 API）
     *
     * @return access_token 字符串
     * @throws BizException 企微 API 调用失败或返回 errcode 非 0 时抛出
     */
    public String getAccessToken() {
        String cacheKey = WeComAuthConstants.accessTokenRedisKey(weComProperties.corpid());
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        WeComAccessTokenResponse response = restTemplate.getForObject(
                WeComAuthConstants.URL_GET_TOKEN,
                WeComAccessTokenResponse.class,
                weComProperties.corpid(), weComProperties.secret());
        log.info("【WeCom】获取 access_token 出参, errcode={}, errmsg={}", response == null ? null : response.getErrcode(),
                response == null ? null : response.getErrmsg());
        if (response == null || response.isSuccess()) {
            log.info("【WeCom】获取 access_token 失败，errcode={}", response == null ? null : response.getErrcode());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "企业微信 access_token 获取失败");
        }
        stringRedisTemplate.opsForValue().set(cacheKey, response.getAccess_token(),
                Duration.ofSeconds(WeComAuthConstants.ACCESS_TOKEN_CACHE_SECONDS));
        return response.getAccess_token();
    }

    /**
     * 通过扫码授权 code 换取用户身份
     *
     * @param code 扫码授权码，一次性有效，5 分钟内有效
     * @return 企微用户身份响应（userid、openid 等）
     * @throws BizException HTTP 非 2xx 或企微 errcode 非 0 时抛出
     */
    public WeComUserInfoResponse getUserByCode(String code) {
        String accessToken = getAccessToken();
        WeComUserInfoResponse response = restTemplate.getForObject(
                WeComAuthConstants.URL_GET_USER_INFO,
                WeComUserInfoResponse.class,
                accessToken, code);
        log.info("【WeCom】根据 code 获取用户身份出参, errcode={}, errmsg={}, userid={}",
                response == null ? null : response.getErrcode(),
                response == null ? null : response.getErrmsg(),
                response == null ? null : response.getUserid());
        if (response == null || response.isSuccess()) {
            log.info("【WeCom】根据 code 获取用户身份失败，errcode={}", response == null ? null : response.getErrcode());
            throw new BizException(ResultCode.INTEGRATION_ERROR, "企业微信用户身份获取失败");
        }
        return response;
    }
}

package com.cacch.enterprise.service.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 验证码生成结果（服务层内部模型，经 web 层 MapStruct 转换为 VO）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResult implements Serializable {

    /**
     * 验证码唯一标识，登录请求时回传
     */
    private String captchaId;

    /**
     * 验证码图片 Base64 数据（data URI 格式，可直接用于 img src）
     */
    private String imageBase64;
}

package com.cacch.enterprise.dto.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 图形验证码视图对象
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVO implements Serializable {

    /** 验证码唯一标识，登录请求时回传 */
    private String captchaId;

    /** 验证码图片 Base64 数据（data URI 格式，可直接用于 img src） */
    private String imageBase64;
}

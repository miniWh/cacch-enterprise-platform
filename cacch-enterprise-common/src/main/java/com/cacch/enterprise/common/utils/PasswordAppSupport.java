package com.cacch.enterprise.common.utils;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cacch.enterprise.common.constant.auth.AuthCommonConstants;

import java.util.regex.Pattern;

/**
 * 密码安全工具（BCrypt 封装）
 *
 * <p>统一密码哈希、校验与复杂度校验入口；cost 固定 10，任何界面与日志不得输出明文密码。
 *
 * @author hongfu_zhou@cacch.com
 */
public final class PasswordAppSupport {

    private PasswordAppSupport() {
    }

    private static final int BCRYPT_COST = 10;

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(AuthCommonConstants.PASSWORD_REGEX);

    /**
     * 对明文密码进行 BCrypt 哈希（cost 10）
     *
     * @param rawPassword 明文密码，禁止写入日志
     * @return BCrypt 密文（$2a 开头，60 字符）
     */
    public static String hash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, rawPassword.toCharArray());
    }

    /**
     * 校验明文密码与 BCrypt 密文是否匹配
     *
     * @param rawPassword 明文密码
     * @param hashedPassword BCrypt 密文（数据库存储值）
     * @return 匹配返回 true；密文为空或格式非法时返回 false
     */
    public static boolean verify(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null || hashedPassword.isBlank()) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword);
        return result.verified;
    }

    /**
     * 校验密码复杂度：长度 8~64 位，须同时包含字母与数字
     *
     * @param rawPassword 待校验的明文密码
     * @return 满足复杂度要求返回 true
     */
    public static boolean matchesComplexity(String rawPassword) {
        return rawPassword != null && PASSWORD_PATTERN.matcher(rawPassword).matches();
    }
}

package com.cacch.enterprise.common.utils;

/**
 * 字符串工具（精简版，仅封装本工程常用判断）
 *
 * @author hongfu_zhou@cacch.com
 */
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 判断字符串是否非空且含非空白字符
     *
     * @param value 待判断字符串
     * @return 非空且含非空白字符返回 true
     */
    public static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}

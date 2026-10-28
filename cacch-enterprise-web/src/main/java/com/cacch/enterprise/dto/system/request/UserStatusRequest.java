package com.cacch.enterprise.dto.system.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户启用/停用请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class UserStatusRequest implements Serializable {

    /** 目标状态：0 启用 / 1 停用 */
    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值非法")
    @Max(value = 1, message = "状态取值非法")
    private Integer status;
}

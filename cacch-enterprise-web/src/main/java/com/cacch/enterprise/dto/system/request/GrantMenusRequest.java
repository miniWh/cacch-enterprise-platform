package com.cacch.enterprise.dto.system.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 角色菜单授权请求
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public class GrantMenusRequest implements Serializable {

    /**
     * 目标菜单/按钮 ID 列表，空列表表示清空授权
     */
    @NotNull(message = "菜单列表不能为空")
    private List<Long> menuIds;
}

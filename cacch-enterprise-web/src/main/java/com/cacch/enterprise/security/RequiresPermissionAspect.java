package com.cacch.enterprise.security;

import com.cacch.enterprise.common.annotation.RequiresPermission;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.model.auth.LoginUser;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.common.utils.LoginUserHolder;
import com.cacch.enterprise.manager.auth.api.IAuthPermissionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 按钮级权限校验切面（@RequiresPermission）
 *
 * <p>从权限缓存（Redis，授权变更后 evict 回源）读取当前用户权限标识集合进行校验；
 * 登录态由 JwtAuthenticationFilter 保证。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequiresPermissionAspect {

    private final IAuthPermissionManager authPermissionManager;

    /**
     * 方法执行前校验当前用户是否持有目标权限标识
     *
     * @param requiresPermission 权限注解（携带权限标识）
     */
    @Before("@annotation(requiresPermission)")
    public void checkPermission(RequiresPermission requiresPermission) {
        LoginUser user = LoginUserHolder.get();
        if (user == null) {
            throw new BizException(ResultCode.AUTH_NOT_LOGIN);
        }
        String required = requiresPermission.value();
        Set<String> perms = authPermissionManager.loadWithCache(user.getUserId()).getPerms();
        if (!perms.contains(required)) {
            log.warn("【Security】权限校验未通过, userId={}, required={}", user.getUserId(), required);
            throw new BizException(ResultCode.AUTH_PERMISSION_DENIED);
        }
    }
}

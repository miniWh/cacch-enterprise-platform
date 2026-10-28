package com.cacch.enterprise.async.log.listener;

import com.cacch.enterprise.common.event.auth.AuthLoginEvent;
import com.cacch.enterprise.service.system.api.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 登录行为事件监听器
 *
 * <p>本期以 INFO 日志留痕（sys_login_log 表由日志域立项后落库）；
 * 登录成功时异步回写最近登录时间（独立事务）。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginLogListener {

    private final ISysUserService sysUserService;

    /**
     * 消费登录事件：日志留痕 + 登录成功回写最近登录时间
     *
     * @param event 登录事件
     */
    @Async("eventExecutor")
    @EventListener
    public void onLoginEvent(AuthLoginEvent event) {
        try {
            if (event.isSuccess()) {
                log.info("【LoginLog】登录成功留痕，userId={}, account={}, ip={}, time={}",
                        event.getUserId(), event.getAccount(), event.getLoginIp(), event.getEventTime());
                sysUserService.updateLastLoginTime(event.getUserId());
            } else {
                log.info("【LoginLog】登录失败留痕，account={}, ip={}, reason={}, time={}",
                        event.getAccount(), event.getLoginIp(), event.getFailReason(), event.getEventTime());
            }
        } catch (Exception e) {
            log.error("【LoginLog】登录事件处理失败，account={}", event.getAccount(), e);
        }
    }
}

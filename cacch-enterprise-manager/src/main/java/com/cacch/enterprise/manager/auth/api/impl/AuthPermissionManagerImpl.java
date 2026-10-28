package com.cacch.enterprise.manager.auth.api.impl;

import com.cacch.enterprise.common.constant.system.SystemCommonConstants;
import com.cacch.enterprise.manager.auth.api.IAuthPermissionManager;
import com.cacch.enterprise.service.system.api.ISysPermissionService;
import com.cacch.enterprise.service.system.model.UserPermissionModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 权限缓存编排实现（Redis 缓存 + 数据库回源）
 *
 * <p>缓存与登录态分离：授权变更 evict 后用户无需重新登录，下次请求回源重建。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthPermissionManagerImpl implements IAuthPermissionManager {

    private final ISysPermissionService sysPermissionService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 加载用户权限（优先缓存；未命中回源并写缓存）
     *
     * @param userId 用户主键
     * @return 权限模型（菜单扁平列表 + 权限标识集合）
     */
    @Override
    public UserPermissionModel loadWithCache(Long userId) {
        String permsKey = SystemCommonConstants.userPermsRedisKey(userId);
        String routesKey = SystemCommonConstants.userRoutesRedisKey(userId);
        UserPermissionModel cached = readCache(permsKey, routesKey);
        if (cached != null) {
            return cached;
        }
        UserPermissionModel model = sysPermissionService.loadUserPermission(userId);
        writeCache(permsKey, routesKey, model);
        log.info("【AuthPermission】权限缓存未命中，已回源重建，userId={}, menus={}, perms={}",
                userId, model.getMenus().size(), model.getPerms().size());
        return model;
    }

    /**
     * 清除指定用户的权限缓存
     *
     * @param userIds 用户 ID 集合
     */
    @Override
    public void evictUsers(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            stringRedisTemplate.delete(SystemCommonConstants.userPermsRedisKey(userId));
            stringRedisTemplate.delete(SystemCommonConstants.userRoutesRedisKey(userId));
        }
        log.info("【AuthPermission】已清除权限缓存，userIds={}", userIds);
    }

    /**
     * 读取权限缓存
     *
     * @param permsKey  权限标识 Key
     * @param routesKey 菜单列表 Key
     * @return 权限模型；任一 Key 缺失或解析失败时返回 null（视为未命中）
     */
    private UserPermissionModel readCache(String permsKey, String routesKey) {
        String permsJson = stringRedisTemplate.opsForValue().get(permsKey);
        String routesJson = stringRedisTemplate.opsForValue().get(routesKey);
        if (permsJson == null || routesJson == null) {
            return null;
        }
        try {
            Set<String> perms = objectMapper.readValue(permsJson, new TypeReference<>() {
            });
            List<com.cacch.enterprise.dao.entity.system.SysMenuDO> menus =
                    objectMapper.readValue(routesJson, new TypeReference<>() {
                    });
            return new UserPermissionModel(menus, perms);
        } catch (Exception e) {
            log.error("【AuthPermission】权限缓存反序列化失败，按未命中回源处理", e);
            return null;
        }
    }

    /**
     * 写入权限缓存（TTL 与访问令牌有效期一致）
     *
     * @param permsKey  权限标识 Key
     * @param routesKey 菜单列表 Key
     * @param model     权限模型
     */
    private void writeCache(String permsKey, String routesKey, UserPermissionModel model) {
        try {
            Duration ttl = Duration.ofSeconds(SystemCommonConstants.PERMISSION_CACHE_TTL_SECONDS);
            stringRedisTemplate.opsForValue().set(permsKey, objectMapper.writeValueAsString(model.getPerms()), ttl);
            stringRedisTemplate.opsForValue().set(routesKey, objectMapper.writeValueAsString(model.getMenus()), ttl);
        } catch (Exception e) {
            log.error("【AuthPermission】权限缓存写入失败（下次请求将回源重建）", e);
        }
    }
}

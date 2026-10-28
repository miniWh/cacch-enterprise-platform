package com.cacch.enterprise.dao.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cacch.enterprise.common.utils.LoginUserHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 标准审计字段自动填充
 *
 * <p>insert 填充 createTime/updateTime/createBy/updateBy；update 填充 updateTime/updateBy；
 * createBy/updateBy 取当前登录用户 ID（匿名上下文中为 null，交由数据库默认值）。
 *
 * @author hongfu_zhou@cacch.com
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入时填充审计字段
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = LoginUserHolder.getUserId();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "createBy", Long.class, userId);
        this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
    }

    /**
     * 更新时填充审计字段
     *
     * @param metaObject 实体元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", Long.class, LoginUserHolder.getUserId());
    }
}

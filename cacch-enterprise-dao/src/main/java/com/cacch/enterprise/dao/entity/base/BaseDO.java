package com.cacch.enterprise.dao.entity.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：主键与标准审计字段
 *
 * <p>主键为雪花算法生成（BIGINT，禁止数据库自增）；
 * create_time/update_time/create_by/update_by 由 MyMetaObjectHandler 自动填充；
 * deleted 为逻辑删除标记（0 正常 / 1 删除），配合全局逻辑删除配置。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
public abstract class BaseDO implements Serializable {

    /**
     * 主键，雪花算法生成
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人（用户 ID）
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人（用户 ID）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 逻辑删除标记：0 正常 / 1 删除
     */
    @TableLogic
    private Integer deleted;
}

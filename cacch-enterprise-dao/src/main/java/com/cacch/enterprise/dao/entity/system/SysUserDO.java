package com.cacch.enterprise.dao.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.enterprise.dao.entity.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户实体，映射表 sys_user
 *
 * <p>工号与手机号均为登录账号，未删除范围内全局唯一（部分唯一索引）；
 * 企微/iHR 相关字段为本期预留，不参与业务逻辑。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUserDO extends BaseDO {

    /**
     * 工号，登录账号之一，全局唯一（逻辑删除后可复用），创建后不可修改
     */
    private String username;

    /**
     * 手机号，登录账号之一，全局唯一（逻辑删除后可复用），11 位手机号格式
     */
    private String phone;

    /**
     * 登录密码 BCrypt 密文（at.favre，cost 10），禁止明文存储与日志输出
     */
    private String password;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 归属部门 ID，对应 sys_dept.id
     */
    private Long deptId;

    /**
     * 邮箱，可选
     */
    private String email;

    /**
     * 账号状态：0 启用 / 1 停用
     */
    private Integer status;

    /**
     * 数据来源：LOCAL 本地创建 / IHR 人事系统同步（预留）
     */
    private String source;

    /**
     * iHR 人员唯一编号（预留）
     */
    private String externalId;

    /**
     * 企微 UserId（预留，扫码/免登绑定依据），唯一（未删除且非空范围）
     */
    private String wecomUserid;

    /**
     * 最近登录时间
     */
    private LocalDateTime lastLoginTime;
}

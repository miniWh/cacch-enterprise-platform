package com.cacch.enterprise.dao.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cacch.enterprise.dao.entity.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门实体，映射表 sys_dept
 *
 * <p>树形结构；ancestors 为祖先链冗余字段（格式：从根到父部门的 ID 逗号分隔，不含自身，
 * 根部门为 "0"），供「本部门及下级」数据权限按前缀查询。
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDeptDO extends BaseDO {

    /** 父部门 ID，0 表示根部门 */
    private Long parentId;

    /** 祖先链：如 1002 的父链为 "0,1001"；根部门为 "0" */
    private String ancestors;

    /** 部门名称，同级唯一（逻辑删除后可复用） */
    private String deptName;

    /** 排序号，同层级内升序 */
    private Integer orderNum;

    /** 负责人用户 ID，对应 sys_user.id，可选 */
    private Long leaderId;

    /** 状态：0 正常 / 1 停用（停用后不可向该部门及下级新增用户） */
    private Integer status;

    /** 数据来源：LOCAL 本地维护 / IHR 人事系统同步（预留） */
    private String source;

    /** iHR 部门唯一编号（预留） */
    private String externalId;
}

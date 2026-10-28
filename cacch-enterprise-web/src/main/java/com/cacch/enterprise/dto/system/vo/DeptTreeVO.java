package com.cacch.enterprise.dto.system.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 部门树节点视图对象（部门管理页与部门下拉）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptTreeVO implements Serializable {

    /** 部门主键，对应 sys_dept.id */
    private Long id;

    /** 父部门 ID，0 表示根部门 */
    private Long parentId;

    /** 祖先链：从根到父部门的 ID 逗号分隔，根部门为 "0" */
    private String ancestors;

    /** 部门名称 */
    private String deptName;

    /** 排序号，同层级内升序 */
    private Integer orderNum;

    /** 负责人用户 ID，可空 */
    private Long leaderId;

    /** 状态：0 正常 / 1 停用 */
    private Integer status;

    /** 子部门列表，叶子节点为空列表 */
    private List<DeptTreeVO> children;
}

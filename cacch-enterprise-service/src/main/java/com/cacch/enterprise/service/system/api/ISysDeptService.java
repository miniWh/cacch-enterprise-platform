package com.cacch.enterprise.service.system.api;

import com.cacch.enterprise.dao.entity.system.SysDeptDO;

import java.util.List;

/**
 * 部门服务接口（sys_dept 聚合）
 *
 * @author hongfu_zhou@cacch.com
 */
public interface ISysDeptService {

    /**
     * 查询全部未删除部门（含停用，供部门树展示），按 orderNum、id 升序
     *
     * @return 部门扁平列表
     */
    List<SysDeptDO> listAll();

    /**
     * 按 ID 查询部门（仅未删除）
     *
     * @param deptId 部门主键
     * @return 部门实体；不存在时返回 null
     */
    SysDeptDO getById(Long deptId);

    /**
     * 校验同级部门名称是否已存在
     *
     * @param deptName       部门名称
     * @param parentId       父部门 ID
     * @param excludeDeptId  排除的部门 ID（编辑场景传本人），可空
     * @return 已存在返回 true
     */
    boolean existsNameInSiblings(String deptName, Long parentId, Long excludeDeptId);

    /**
     * 校验部门是否存在下级部门（删除前置校验）
     *
     * @param deptId 部门主键
     * @return 存在下级返回 true
     */
    boolean hasChildren(Long deptId);

    /**
     * 校验部门启用链：部门自身及全部祖先部门均为启用状态
     * （「停用后不可向该部门及下级新增用户」的判定依据）
     *
     * @param deptId 部门主键
     * @return 链路全部启用返回 true；部门不存在返回 false
     */
    boolean isDeptEnabledChain(Long deptId);

    /**
     * 新增部门（自动计算祖先链，参与调用方事务）
     *
     * @param dept 部门实体（parentId、deptName 必填）
     */
    void createDept(SysDeptDO dept);

    /**
     * 编辑部门（名称唯一校验；变更父部门时重算自身与全部下级的祖先链，参与调用方事务）
     *
     * @param dept 部门实体（携带主键与需更新字段）
     */
    void updateDept(SysDeptDO dept);

    /**
     * 逻辑删除部门（参与调用方事务）
     *
     * @param deptId 部门主键
     */
    void deleteDept(Long deptId);
}

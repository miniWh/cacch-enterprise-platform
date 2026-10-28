package com.cacch.enterprise.service.system.api.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cacch.enterprise.common.exception.BizException;
import com.cacch.enterprise.common.result.ResultCode;
import com.cacch.enterprise.dao.entity.system.SysDeptDO;
import com.cacch.enterprise.dao.mapper.system.SysDeptMapper;
import com.cacch.enterprise.service.system.api.ISysDeptService;
import com.cacch.enterprise.service.system.api.ISysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 部门服务实现（祖先链维护在本聚合内完成）
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements ISysDeptService {

    private final SysDeptMapper sysDeptMapper;
    private final ISysUserService sysUserService;

    /**
     * 查询全部未删除部门（含停用，供部门树展示）
     *
     * @return 部门扁平列表（orderNum、id 升序）
     */
    @Override
    public List<SysDeptDO> listAll() {
        return sysDeptMapper.selectList(new LambdaQueryWrapper<SysDeptDO>()
                .orderByAsc(SysDeptDO::getOrderNum)
                .orderByAsc(SysDeptDO::getId));
    }

    /**
     * 按 ID 查询部门（仅未删除）
     *
     * @param deptId 部门主键
     * @return 部门实体；不存在时返回 null
     */
    @Override
    public SysDeptDO getById(Long deptId) {
        return sysDeptMapper.selectById(deptId);
    }

    /**
     * 校验同级部门名称是否已存在
     *
     * @param deptName      部门名称
     * @param parentId      父部门 ID
     * @param excludeDeptId 排除的部门 ID
     * @return 已存在返回 true
     */
    @Override
    public boolean existsNameInSiblings(String deptName, Long parentId, Long excludeDeptId) {
        LambdaQueryWrapper<SysDeptDO> wrapper = new LambdaQueryWrapper<SysDeptDO>()
                .eq(SysDeptDO::getDeptName, deptName)
                .eq(SysDeptDO::getParentId, parentId);
        if (excludeDeptId != null) {
            wrapper.ne(SysDeptDO::getId, excludeDeptId);
        }
        return sysDeptMapper.selectCount(wrapper) > 0;
    }

    /**
     * 校验部门是否存在下级部门
     *
     * @param deptId 部门主键
     * @return 存在下级返回 true
     */
    @Override
    public boolean hasChildren(Long deptId) {
        return sysDeptMapper.selectCount(new LambdaQueryWrapper<SysDeptDO>()
                .eq(SysDeptDO::getParentId, deptId)) > 0;
    }

    /**
     * 校验部门启用链（自身及全部祖先均为启用状态）
     *
     * @param deptId 部门主键
     * @return 链路全部启用返回 true；部门不存在返回 false
     */
    @Override
    public boolean isDeptEnabledChain(Long deptId) {
        SysDeptDO dept = getById(deptId);
        if (dept == null) {
            return false;
        }
        if (dept.getStatus() != null && dept.getStatus() == 1) {
            return false;
        }
        List<Long> ancestorIds = Arrays.stream(dept.getAncestors().split(","))
                .filter(s -> !"0".equals(s))
                .map(Long::valueOf)
                .toList();
        if (ancestorIds.isEmpty()) {
            return true;
        }
        return sysDeptMapper.selectList(new LambdaQueryWrapper<SysDeptDO>()
                        .in(SysDeptDO::getId, ancestorIds))
                .stream().allMatch(d -> d.getStatus() == null || d.getStatus() == 0);
    }

    /**
     * 新增部门（自动计算祖先链）
     *
     * @param dept 部门实体（parentId、deptName 必填）
     * @throws BizException 父部门不存在或同级名称重复时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void createDept(SysDeptDO dept) {
        Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
        String ancestors;
        if (parentId != 0) {
            SysDeptDO parent = getById(parentId);
            if (parent == null) {
                log.info("【SysDept】新增部门终止：父部门不存在，parentId={}", parentId);
                throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "父部门不存在");
            }
            ancestors = parent.getAncestors() + "," + parentId;
        } else {
            ancestors = "0";
        }
        if (existsNameInSiblings(dept.getDeptName(), parentId, null)) {
            log.info("【SysDept】新增部门终止：同级名称已存在，deptName={}, parentId={}", dept.getDeptName(), parentId);
            throw new BizException(ResultCode.SYSTEM_DEPT_NAME_DUPLICATE);
        }
        dept.setParentId(parentId);
        dept.setAncestors(ancestors);
        sysDeptMapper.insert(dept);
        log.info("【SysDept】新增部门，deptId={}, deptName={}, ancestors={}", dept.getId(), dept.getDeptName(), ancestors);
    }

    /**
     * 编辑部门（名称唯一；变更父部门时重算自身与下级的祖先链）
     *
     * @param dept 部门实体（携带主键与需更新字段）
     * @throws BizException 部门不存在、同级名称重复、父部门非法或形成环时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void updateDept(SysDeptDO dept) {
        Long deptId = dept.getId();
        SysDeptDO existing = getById(deptId);
        if (existing == null) {
            log.info("【SysDept】编辑部门终止：部门不存在，deptId={}", deptId);
            throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "部门不存在");
        }
        Long newParentId = dept.getParentId() == null ? existing.getParentId() : dept.getParentId();
        if (existsNameInSiblings(dept.getDeptName(), newParentId, deptId)) {
            log.info("【SysDept】编辑部门终止：同级名称已存在，deptName={}, parentId={}", dept.getDeptName(), newParentId);
            throw new BizException(ResultCode.SYSTEM_DEPT_NAME_DUPLICATE);
        }
        boolean parentChanged = !newParentId.equals(existing.getParentId());
        String newAncestors = existing.getAncestors();
        if (parentChanged) {
            newAncestors = resolveNewAncestors(deptId, existing, newParentId);
            reassignDescendants(existing, newAncestors);
        }
        dept.setParentId(newParentId);
        dept.setAncestors(newAncestors);
        sysDeptMapper.updateById(dept);
        log.info("【SysDept】编辑部门，deptId={}, parentChanged={}", deptId, parentChanged);
    }

    /**
     * 逻辑删除部门（参与调用方事务；前置校验由 Manager 完成）
     *
     * @param deptId 部门主键
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED, readOnly = false, timeout = 30)
    public void deleteDept(Long deptId) {
        sysDeptMapper.deleteById(deptId);
        log.info("【SysDept】逻辑删除部门，deptId={}", deptId);
    }

    /**
     * 解析移动后的新祖先链（含环校验：目标父部门不得为自身或自身下级）
     *
     * @param deptId    部门主键
     * @param existing  现有部门实体
     * @param newParentId 新父部门 ID（0 表示根）
     * @return 新祖先链字符串
     * @throws BizException 目标父部门不存在或形成环时抛出
     */
    private String resolveNewAncestors(Long deptId, SysDeptDO existing, Long newParentId) {
        if (newParentId.equals(deptId)) {
            throw new BizException(ResultCode.PARAM_INVALID, "父部门不能为自身");
        }
        String newAncestors;
        if (newParentId == 0) {
            newAncestors = "0";
        } else {
            SysDeptDO newParent = getById(newParentId);
            if (newParent == null) {
                log.info("【SysDept】编辑部门终止：目标父部门不存在，newParentId={}", newParentId);
                throw new BizException(ResultCode.RESOURCE_NOT_FOUND, "目标父部门不存在");
            }
            boolean formsCycle = Arrays.asList(newParent.getAncestors().split(",")).contains(String.valueOf(deptId))
                    || newParentId.equals(existing.getId());
            if (formsCycle) {
                log.info("【SysDept】编辑部门终止：目标父部门为自身下级（形成环），deptId={}, newParentId={}", deptId, newParentId);
                throw new BizException(ResultCode.PARAM_INVALID, "不能将部门移动到其自身下级");
            }
            newAncestors = newParent.getAncestors() + "," + newParentId;
        }
        return newAncestors;
    }

    /**
     * 将全部下级部门的祖先链由旧前缀重写为新前缀
     *
     * @param existing     现有部门实体（携带旧祖先链）
     * @param newAncestors 部门自身的新祖先链
     */
    private void reassignDescendants(SysDeptDO existing, String newAncestors) {
        String oldPrefix = existing.getAncestors() + "," + existing.getId();
        String newPrefix = newAncestors + "," + existing.getId();
        List<SysDeptDO> descendants = sysDeptMapper.selectList(new LambdaQueryWrapper<SysDeptDO>()
                .and(w -> w.eq(SysDeptDO::getAncestors, oldPrefix)
                        .or().likeRight(SysDeptDO::getAncestors, oldPrefix + ",")));
        for (SysDeptDO child : descendants) {
            String suffix = child.getAncestors().substring(oldPrefix.length());
            SysDeptDO update = new SysDeptDO();
            update.setId(child.getId());
            update.setAncestors(newPrefix + suffix);
            sysDeptMapper.updateById(update);
        }
        if (!descendants.isEmpty()) {
            log.info("【SysDept】部门移动后重写下级祖先链，deptId={}, affected={}", existing.getId(), descendants.size());
        }
    }
}

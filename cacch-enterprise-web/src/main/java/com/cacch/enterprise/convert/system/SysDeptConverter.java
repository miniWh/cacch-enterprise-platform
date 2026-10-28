package com.cacch.enterprise.convert.system;

import com.cacch.enterprise.dao.entity.system.SysDeptDO;
import com.cacch.enterprise.dto.system.request.DeptSaveRequest;
import com.cacch.enterprise.dto.system.vo.DeptTreeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门业务域对象转换器（部门 DO/树 VO、保存命令）
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface SysDeptConverter {

    /**
     * 部门实体转树节点视图对象
     *
     * @param dept 部门实体
     * @return 树节点视图对象（children 为空列表，树组装见 buildTree）
     */
    @Mapping(target = "children", ignore = true)
    DeptTreeVO toTreeVO(SysDeptDO dept);

    /**
     * 部门保存请求转部门实体（编辑场景附加主键，新增传 null）
     *
     * @param request 部门保存请求
     * @param deptId  部门主键，编辑时非空
     * @return 部门实体
     */
    @Mapping(target = "id", source = "deptId")
    SysDeptDO toDO(DeptSaveRequest request, Long deptId);

    /**
     * 将部门扁平列表组装为树
     *
     * <p>按 parentId 分组递归挂载，同层级按 orderNum、id 升序；根节点为 parentId=0。
     *
     * @param depts 部门扁平列表（含停用）
     * @return 部门树根节点列表，无数据时为空列表
     */
    default List<DeptTreeVO> buildTree(List<SysDeptDO> depts) {
        Map<Long, List<SysDeptDO>> byParent = depts.stream()
                .collect(Collectors.groupingBy(SysDeptDO::getParentId));
        return buildChildren(0L, byParent);
    }

    /**
     * 递归构建指定父节点的子树
     *
     * @param parentId 父节点 ID
     * @param byParent 按父节点 ID 分组的部门映射
     * @return 子树节点列表
     */
    private List<DeptTreeVO> buildChildren(Long parentId, Map<Long, List<SysDeptDO>> byParent) {
        List<SysDeptDO> children = byParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        List<SysDeptDO> sorted = children.stream()
                .sorted(Comparator.comparing(SysDeptDO::getOrderNum,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SysDeptDO::getId))
                .toList();
        List<DeptTreeVO> result = new ArrayList<>(sorted.size());
        for (SysDeptDO dept : sorted) {
            DeptTreeVO node = toTreeVO(dept);
            node.setChildren(buildChildren(dept.getId(), byParent));
            result.add(node);
        }
        return result;
    }
}

package com.cacch.enterprise.convert.system;

import com.cacch.enterprise.dao.entity.system.SysMenuDO;
import com.cacch.enterprise.dto.system.request.MenuSaveRequest;
import com.cacch.enterprise.dto.system.vo.MenuTreeVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单业务域对象转换器（菜单 DO/树 VO、保存命令）
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface SysMenuConverter {

    /**
     * 菜单实体转管理页树节点视图对象
     *
     * @param menu 菜单实体
     * @return 树节点视图对象（children 为空列表，树组装见 buildTree）
     */
    @Mapping(target = "children", ignore = true)
    MenuTreeVO toTreeVO(SysMenuDO menu);

    /**
     * 菜单保存请求转菜单实体（编辑场景附加主键，新增传 null）
     *
     * @param request 菜单保存请求
     * @param menuId  菜单主键，编辑时非空
     * @return 菜单实体
     */
    @Mapping(target = "id", source = "menuId")
    SysMenuDO toDO(MenuSaveRequest request, Long menuId);

    /**
     * 将菜单扁平列表组装为管理页树（含目录/菜单/按钮全部类型）
     *
     * <p>按 parentId 分组递归挂载，同层级按 orderNum、id 升序；根节点为 parentId=0。
     *
     * @param menus 菜单扁平列表（含停用）
     * @return 菜单树根节点列表，无数据时为空列表
     */
    default List<MenuTreeVO> buildTree(List<SysMenuDO> menus) {
        Map<Long, List<SysMenuDO>> byParent = menus.stream()
                .collect(Collectors.groupingBy(SysMenuDO::getParentId));
        return buildChildren(0L, byParent);
    }

    /**
     * 递归构建指定父节点的子树
     *
     * @param parentId 父节点 ID
     * @param byParent 按父节点 ID 分组的菜单映射
     * @return 子树节点列表
     */
    private List<MenuTreeVO> buildChildren(Long parentId, Map<Long, List<SysMenuDO>> byParent) {
        List<SysMenuDO> children = byParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        List<SysMenuDO> sorted = children.stream()
                .sorted(Comparator.comparing(SysMenuDO::getOrderNum,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SysMenuDO::getId))
                .toList();
        List<MenuTreeVO> result = new ArrayList<>(sorted.size());
        for (SysMenuDO menu : sorted) {
            MenuTreeVO node = toTreeVO(menu);
            node.setChildren(buildChildren(menu.getId(), byParent));
            result.add(node);
        }
        return result;
    }
}

package com.cacch.enterprise.convert.auth;

import com.cacch.enterprise.common.model.auth.LoginUser;
import com.cacch.enterprise.dto.auth.request.LoginRequest;
import com.cacch.enterprise.dto.auth.vo.CaptchaVO;
import com.cacch.enterprise.dto.auth.vo.LoginTokenVO;
import com.cacch.enterprise.dto.auth.vo.MenuVO;
import com.cacch.enterprise.dto.auth.vo.OnlineUserVO;
import com.cacch.enterprise.dto.auth.vo.UserProfileVO;
import com.cacch.enterprise.manager.auth.model.ProfileModel;
import com.cacch.enterprise.manager.auth.model.TokenPairModel;
import com.cacch.enterprise.service.auth.model.AuthLoginCommand;
import com.cacch.enterprise.service.auth.model.CaptchaResult;
import com.cacch.enterprise.dao.entity.system.SysMenuDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证业务域对象转换器（登录请求/令牌/验证码/用户信息/在线用户）
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface AuthLoginConverter {

    /**
     * 登录请求转登录命令（附加来源 IP）
     *
     * @param request 登录请求
     * @param loginIp 登录来源 IP
     * @return 登录命令
     */
    @Mapping(target = "account", source = "request.account")
    @Mapping(target = "password", source = "request.password")
    @Mapping(target = "loginIp", source = "loginIp")
    AuthLoginCommand toCommand(LoginRequest request, String loginIp);

    /**
     * 双令牌模型转视图对象
     *
     * @param model 双令牌模型
     * @return 双令牌视图对象（tokenType 固定 Bearer）
     */
    @Mapping(target = "tokenType", constant = "Bearer")
    LoginTokenVO toTokenVO(TokenPairModel model);

    /**
     * 验证码结果转视图对象
     *
     * @param result 验证码生成结果
     * @return 验证码视图对象
     */
    CaptchaVO toCaptchaVO(CaptchaResult result);

    /**
     * 菜单实体转树节点视图对象
     *
     * @param menu 菜单实体
     * @return 树节点视图对象（children 为空列表，树组装见 buildMenuTree）
     */
    @Mapping(target = "children", ignore = true)
    MenuVO toMenuVO(SysMenuDO menu);

    /**
     * 登录用户上下文转在线用户视图对象
     *
     * @param user 登录用户上下文
     * @return 在线用户视图对象
     */
    OnlineUserVO toOnlineUserVO(LoginUser user);

    /**
     * 用户信息模型转当前用户视图对象（资料 + 动态菜单树 + 权限标识）
     *
     * @param model 用户信息模型（user + permission）
     * @return 当前用户视图对象；模型为 null 时返回 null
     */
    default UserProfileVO toProfileVO(ProfileModel model) {
        if (model == null || model.getUser() == null) {
            return null;
        }
        var user = model.getUser();
        var permission = model.getPermission();
        return UserProfileVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .phone(user.getPhone())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .email(user.getEmail())
                .status(user.getStatus())
                .menus(buildMenuTree(permission == null ? List.of() : permission.getMenus()))
                .perms(permission == null ? Set.of() : permission.getPerms())
                .build();
    }

    /**
     * 将菜单扁平列表组装为树（仅目录 D 与菜单 M，按钮节点不入路由树）
     *
     * <p>按 parentId 分组递归挂载，同层级按 orderNum、id 升序；根节点为 parentId=0。
     *
     * @param menus 菜单扁平列表（启用状态）
     * @return 菜单树根节点列表，无数据时为空列表
     */
    default List<MenuVO> buildMenuTree(List<SysMenuDO> menus) {
        List<SysMenuDO> routable = menus.stream()
                .filter(m -> "D".equals(m.getMenuType()) || "M".equals(m.getMenuType()))
                .toList();
        Map<Long, List<SysMenuDO>> byParent = routable.stream()
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
    private List<MenuVO> buildChildren(Long parentId, Map<Long, List<SysMenuDO>> byParent) {
        List<SysMenuDO> children = byParent.get(parentId);
        if (children == null || children.isEmpty()) {
            return List.of();
        }
        List<SysMenuDO> sorted = children.stream()
                .sorted(Comparator.comparing(SysMenuDO::getOrderNum,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(SysMenuDO::getId))
                .toList();
        List<MenuVO> result = new ArrayList<>(sorted.size());
        for (SysMenuDO menu : sorted) {
            MenuVO node = toMenuVO(menu);
            node.setChildren(buildChildren(menu.getId(), byParent));
            result.add(node);
        }
        return result;
    }
}

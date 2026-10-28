package com.cacch.enterprise.convert.system;

import com.cacch.enterprise.common.result.PageResult;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.dto.system.request.CreateSysUserRequest;
import com.cacch.enterprise.dto.system.request.UpdateSysUserRequest;
import com.cacch.enterprise.dto.system.vo.SysUserVO;
import com.cacch.enterprise.manager.system.model.CreateUserCommand;
import com.cacch.enterprise.manager.system.model.UpdateUserCommand;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 用户业务域对象转换器（用户 DO/VO、创建/编辑命令）
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface SysUserConverter {

    /**
     * 用户实体转视图对象（不含 roleIds，详情场景见 toDetailVO）
     *
     * @param user 用户实体
     * @return 用户视图对象
     */
    SysUserVO toVO(SysUserDO user);

    /**
     * 用户实体转详情视图对象（携带角色 ID 列表，编辑回显用）
     *
     * @param user    用户实体
     * @param roleIds 持有角色 ID 列表
     * @return 用户详情视图对象
     */
    @Mapping(target = "roleIds", source = "roleIds")
    SysUserVO toDetailVO(SysUserDO user, List<Long> roleIds);

    /**
     * 新增用户请求转创建命令
     *
     * @param request 新增用户请求
     * @return 创建用户命令
     */
    CreateUserCommand toCreateCommand(CreateSysUserRequest request);

    /**
     * 编辑用户请求转编辑命令（附加用户主键）
     *
     * @param request 编辑用户请求
     * @param userId  用户主键
     * @return 编辑用户命令
     */
    @Mapping(target = "userId", source = "userId")
    UpdateUserCommand toUpdateCommand(UpdateSysUserRequest request, Long userId);

    /**
     * 用户分页数据转分页返回对象
     *
     * @param page 用户分页数据
     * @return 分页返回对象（列表元素为用户视图对象）
     */
    default PageResult<SysUserVO> toPageVO(IPage<SysUserDO> page) {
        List<SysUserVO> list = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), list);
    }
}

package com.cacch.enterprise.convert.system;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.enterprise.common.result.PageResult;
import com.cacch.enterprise.dao.entity.system.SysRoleDO;
import com.cacch.enterprise.dto.system.request.RoleSaveRequest;
import com.cacch.enterprise.dto.system.vo.SysRoleSimpleVO;
import com.cacch.enterprise.dto.system.vo.SysRoleVO;
import com.cacch.enterprise.manager.system.model.RoleSaveCommand;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 角色业务域对象转换器（角色 DO/VO、保存命令）
 *
 * @author hongfu_zhou@cacch.com
 */
@Mapper(componentModel = "spring")
public interface SysRoleConverter {

    /**
     * 角色实体转视图对象
     *
     * @param role 角色实体
     * @return 角色视图对象
     */
    SysRoleVO toVO(SysRoleDO role);

    /**
     * 角色实体转精简视图对象（下拉用）
     *
     * @param role 角色实体
     * @return 精简视图对象
     */
    SysRoleSimpleVO toSimpleVO(SysRoleDO role);

    /**
     * 角色保存请求转命令
     *
     * @param request 角色保存请求
     * @return 角色保存命令
     */
    RoleSaveCommand toCommand(RoleSaveRequest request);

    /**
     * 角色分页数据转分页返回对象
     *
     * @param page 角色分页数据
     * @return 分页返回对象（列表元素为角色视图对象）
     */
    default PageResult<SysRoleVO> toPageVO(IPage<SysRoleDO> page) {
        List<SysRoleVO> list = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), list);
    }
}

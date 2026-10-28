package com.cacch.enterprise.dao.query.system;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户分页查询参数（仅作为 SysUserMapper.selectUserPage 的 Mapper 参数对象）
 *
 * @author hongfu_zhou@cacch.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUserPageQuery {

    /**
     * 工号模糊搜索，可空
     */
    private String username;

    /**
     * 姓名模糊搜索，可空
     */
    private String realName;

    /**
     * 手机号模糊搜索，可空
     */
    private String phone;

    /**
     * 按部门筛选（含下级部门），对应 sys_dept.id，可空
     */
    private Long deptId;

    /**
     * 状态筛选：0 启用 / 1 停用，可空
     */
    private Integer status;
}

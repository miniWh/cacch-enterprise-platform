package com.cacch.enterprise.dao.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cacch.enterprise.common.annotation.DataScope;
import com.cacch.enterprise.dao.entity.system.SysUserDO;
import com.cacch.enterprise.dao.query.system.SysUserPageQuery;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 *
 * @author hongfu_zhou@cacch.com
 */
public interface SysUserMapper extends BaseMapper<SysUserDO> {

    /**
     * 分页查询用户列表（按操作者数据权限档位过滤，仅返回未删除数据）
     *
     * <p>部门筛选含所选部门全部下级；数据权限条件由 DataPermissionInterceptor 依据
     * {@code @DataScope(tableAlias = "u")} 自动追加，禁止在本 SQL 手写部门过滤。
     *
     * @param page     分页参数（MyBatis-Plus 分页插件）
     * @param query    查询条件（工号/姓名/手机号模糊，部门/状态精确）
     * @return 用户分页数据，含 total 与记录列表
     */
    @DataScope(tableAlias = "u")
    @Select("""
            <script>
            SELECT u.* FROM sys_user u
            <where>
                u.deleted = 0
                <if test="q.username != null and q.username != ''">
                    AND u.username LIKE CONCAT('%', #{q.username}, '%')
                </if>
                <if test="q.realName != null and q.realName != ''">
                    AND u.real_name LIKE CONCAT('%', #{q.realName}, '%')
                </if>
                <if test="q.phone != null and q.phone != ''">
                    AND u.phone LIKE CONCAT('%', #{q.phone}, '%')
                </if>
                <if test="q.deptId != null">
                    AND (u.dept_id = #{q.deptId} OR u.dept_id IN (
                        SELECT d2.id FROM sys_dept d1
                        JOIN sys_dept d2
                          ON d2.deleted = 0
                         AND (d2.id = d1.id
                              OR d2.ancestors = CONCAT(d1.ancestors, ',', d1.id)
                              OR d2.ancestors LIKE CONCAT(d1.ancestors, ',', d1.id, ',%'))
                        WHERE d1.id = #{q.deptId} AND d1.deleted = 0
                    ))
                </if>
                <if test="q.status != null">
                    AND u.status = #{q.status}
                </if>
            </where>
            ORDER BY u.create_time DESC
            </script>
            """)
    IPage<SysUserDO> selectUserPage(IPage<SysUserDO> page, @Param("q") SysUserPageQuery query);
}

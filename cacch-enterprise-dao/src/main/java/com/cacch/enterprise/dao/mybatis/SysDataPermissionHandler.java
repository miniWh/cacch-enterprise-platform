package com.cacch.enterprise.dao.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.cacch.enterprise.common.annotation.DataScope;
import com.cacch.enterprise.common.enums.system.DataScopeEnum;
import com.cacch.enterprise.common.model.auth.LoginUser;
import com.cacch.enterprise.common.utils.LoginUserHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据权限处理器（MyBatis-Plus DataPermissionHandler 实现）
 *
 * <p>仅对标记 {@code @DataScope} 的 Mapper 方法生效：按当前登录用户角色的数据权限档位
 * （多角色并集）向原 WHERE 追加部门过滤条件；档位为「全部」时不追加任何条件。
 * 禁止在业务 SQL 中手写部门过滤条件。
 *
 * @author hongfu_zhou@cacch.com
 */
@Slf4j
@Component
public class SysDataPermissionHandler implements DataPermissionHandler {

    /** mappedStatementId → @DataScope 注解缓存（反射解析结果，避免重复反射） */
    private static final Map<String, Optional<DataScope>> ANNOTATION_CACHE = new ConcurrentHashMap<>();

    /**
     * 向原查询条件追加数据权限过滤表达式
     *
     * @param where              原 SQL 的 WHERE 表达式，可能为 null
     * @param mappedStatementId  MyBatis 语句全限定 ID（接口全名.方法名）
     * @return 追加数据权限后的 WHERE 表达式；无需过滤时原样返回
     */
    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        DataScope dataScope = resolveAnnotation(mappedStatementId);
        if (dataScope == null) {
            return where;
        }
        LoginUser user = LoginUserHolder.get();
        if (user == null) {
            log.info("【DataScope】语句 {} 未标记数据权限上下文（无登录用户），不追加部门过滤", mappedStatementId);
            return where;
        }
        String scopeCondition = buildScopeCondition(user, dataScope.tableAlias());
        if (scopeCondition == null) {
            return where;
        }
        try {
            Expression scopeExpression = net.sf.jsqlparser.parser.CCJSqlParserUtil.parseCondExpression(scopeCondition);
            return where == null ? scopeExpression : new AndExpression(where, scopeExpression);
        } catch (Exception e) {
            log.error("【DataScope】数据权限条件解析失败，语句={}，条件={}", mappedStatementId, scopeCondition, e);
            throw new IllegalStateException("数据权限条件解析失败: " + mappedStatementId, e);
        }
    }

    /**
     * 解析语句对应 Mapper 方法上的 @DataScope 注解（带缓存）
     *
     * @param mappedStatementId MyBatis 语句全限定 ID
     * @return 注解实例；方法未标注时返回 null
     */
    private DataScope resolveAnnotation(String mappedStatementId) {
        return ANNOTATION_CACHE.computeIfAbsent(mappedStatementId, id -> {
            try {
                int idx = id.lastIndexOf('.');
                String className = id.substring(0, idx);
                String methodName = id.substring(idx + 1);
                Class<?> mapperInterface = Class.forName(className);
                for (Method method : mapperInterface.getMethods()) {
                    if (method.getName().equals(methodName) && method.isAnnotationPresent(DataScope.class)) {
                        return Optional.of(method.getAnnotation(DataScope.class));
                    }
                }
                return Optional.empty();
            } catch (Exception e) {
                log.info("【DataScope】语句 {} 无法解析 @DataScope 注解，按无数据权限处理", mappedStatementId);
                return Optional.empty();
            }
        }).orElse(null);
    }

    /**
     * 按数据权限档位集合（多角色并集）构建过滤条件字符串
     *
     * @param user       当前登录用户
     * @param tableAlias 目标表别名（用户表含 dept_id/create_by 列）
     * @return 形如 (a.dept_id = 1 OR a.create_by = 2) 的条件；「全部」档位或无有效档位时返回 null（不过滤）
     */
    private String buildScopeCondition(LoginUser user, String tableAlias) {
        Set<DataScopeEnum> scopes = user.getDataScopes();
        if (scopes == null || scopes.isEmpty()) {
            log.info("【DataScope】用户 userId={} 无有效数据权限档位，不追加部门过滤", user.getUserId());
            return null;
        }
        String columnPrefix = (tableAlias == null || tableAlias.isBlank()) ? "" : tableAlias + ".";
        StringBuilder sb = new StringBuilder("(");
        boolean hasCondition = false;
        for (DataScopeEnum scope : scopes) {
            String condition = switch (scope) {
                case ALL -> null; // 全部：不限制
                case DEPT -> buildDeptCondition(columnPrefix, user.getDeptId());
                case DEPT_AND_CHILD -> buildDeptAndChildCondition(columnPrefix, user.getDeptId(), user.getDeptAncestors());
                case SELF -> buildSelfCondition(columnPrefix, user.getUserId());
            };
            if (condition != null) {
                if (hasCondition) {
                    sb.append(" OR ");
                }
                sb.append(condition);
                hasCondition = true;
            }
        }
        if (!hasCondition) {
            return null;
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * 构建「本部门」条件
     *
     * @param columnPrefix 列前缀（含点号）
     * @param deptId       用户归属部门 ID
     * @return 条件字符串；部门为空时返回 null（无法限定则不过滤）
     */
    private String buildDeptCondition(String columnPrefix, Long deptId) {
        return deptId == null ? null : columnPrefix + "dept_id = " + deptId;
    }

    /**
     * 构建「本部门及下级」条件（依赖 sys_dept.ancestors 祖先链前缀匹配）
     *
     * @param columnPrefix  列前缀（含点号）
     * @param deptId        用户归属部门 ID
     * @param deptAncestors 用户归属部门的祖先链（不含自身，根部门为 "0"）
     * @return 条件字符串；部门为空时返回 null
     */
    private String buildDeptAndChildCondition(String columnPrefix, Long deptId, String deptAncestors) {
        if (deptId == null) {
            return null;
        }
        String prefix = (deptAncestors == null || deptAncestors.isBlank() ? "" : deptAncestors + ",") + deptId;
        return columnPrefix + "dept_id = " + deptId
                + " OR " + columnPrefix + "dept_id IN (SELECT id FROM sys_dept WHERE deleted = 0"
                + " AND (ancestors = '" + prefix + "' OR ancestors LIKE '" + prefix + ",%'))";
    }

    /**
     * 构建「仅本人」条件
     *
     * @param columnPrefix 列前缀（含点号）
     * @param userId       用户 ID
     * @return 条件字符串
     */
    private String buildSelfCondition(String columnPrefix, Long userId) {
        return columnPrefix + "create_by = " + userId;
    }
}

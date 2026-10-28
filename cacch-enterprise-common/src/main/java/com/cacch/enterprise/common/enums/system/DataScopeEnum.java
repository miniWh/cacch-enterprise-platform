package com.cacch.enterprise.common.enums.system;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 数据权限档位枚举
 *
 * <p>挂在角色上（sys_role.data_scope），决定用户可见数据行范围；多角色取并集。
 *
 * @author hongfu_zhou@cacch.com
 */
@Getter
@AllArgsConstructor
public enum DataScopeEnum {

    /**
     * 全部数据
     */
    ALL(1),

    /**
     * 本部门数据
     */
    DEPT(2),

    /**
     * 本部门及下级部门数据
     */
    DEPT_AND_CHILD(3),

    /**
     * 仅本人数据
     */
    SELF(4);

    /**
     * 档位值，与 sys_role.data_scope 字段一致
     */
    private final int code;

    /**
     * 按档位值解析枚举
     *
     * @param code 档位值（1~4）
     * @return 对应枚举；无法识别时返回 null
     */
    public static DataScopeEnum of(Integer code) {
        if (code == null) {
            return null;
        }
        for (DataScopeEnum scope : values()) {
            if (scope.code == code) {
                return scope;
            }
        }
        return null;
    }
}

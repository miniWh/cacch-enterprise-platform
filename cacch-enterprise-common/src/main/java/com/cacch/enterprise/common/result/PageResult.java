package com.cacch.enterprise.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 统一分页返回对象
 *
 * @param <T> 列表元素类型
 * @author hongfu_zhou@cacch.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    /** 总记录数 */
    private long total;

    /** 当前页数据列表，无数据时为空列表（非 null） */
    private List<T> list;

    /**
     * 构建分页返回对象
     *
     * @param total 总记录数
     * @param list  当前页数据列表，传 null 时按空列表处理
     * @param <T>   列表元素类型
     * @return 分页返回对象
     */
    public static <T> PageResult<T> of(long total, List<T> list) {
        return new PageResult<>(total, list == null ? List.of() : list);
    }

    /**
     * 构建空分页返回对象
     *
     * @param <T> 列表元素类型
     * @return total=0 且列表为空的分页对象
     */
    public static <T> PageResult<T> empty() {
        return new PageResult<>(0, List.of());
    }
}

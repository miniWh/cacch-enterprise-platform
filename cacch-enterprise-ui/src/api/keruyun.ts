import { http } from './http'
import type { ApiResult } from './codes'

/**
 * 客如云订单对象（字段同接口文档第四节）
 *
 * 金额字段已由服务端从「分」转换为「元」，保留 2 位小数
 */
export interface KeruyunOrderVO {
  /** 内部主键（雪花算法） */
  id: number
  /** 品牌 ID */
  brandId?: string
  /** 门店 ID */
  shopId?: string
  /** 客如云订单 ID（业务唯一键） */
  orderId?: string
  /** 业务订单号 */
  busiOrderNo?: string
  /** 订单状态（客如云官方枚举值，原样展示） */
  orderStatus?: string
  /** 订单类型（客如云官方枚举值，原样展示） */
  orderType?: string
  /** 订单金额（元） */
  orderAmtYuan?: number
  /** 优惠金额（元） */
  promoAmtYuan?: number
  /** 实收金额（元） */
  orderReceivedAmtYuan?: number
  /** 下单时间（ISO-8601） */
  openTime?: string
  /** 完结时间（ISO-8601） */
  finishTime?: string
  /** 第三方订单号 */
  thirdOrderNo?: string
  /** 订单流水号 */
  serialNo?: string
  /** 第三方流水号 */
  thirdSerialNo?: string
  /** 订单模式标签 */
  orderModeTag?: string
  /** 订单模式 */
  orderMode?: string
  /** 同步批次号 */
  syncBatch?: string
  /** 最近一次同步成功时间 */
  syncedAt?: string
  /** 记录创建时间 */
  createdAt?: string
  /** 记录更新时间 */
  updatedAt?: string
}

/** 分页查询返回结构（MyBatis-Plus IPage） */
export interface KeruyunOrderPageVO {
  records: KeruyunOrderVO[]
  total: number
  size: number
  current: number
  pages: number
}

/** 订单查询条件（分页与导出共用） */
export interface KeruyunOrderQuery {
  /** 客如云订单 ID，精确匹配 */
  orderId?: string
  /** 门店 ID，精确匹配 */
  shopId?: string
  /** 订单状态，精确匹配 */
  orderStatus?: string
  /** 订单类型，精确匹配 */
  orderType?: string
  /** 下单时间起始（含），格式 yyyy-MM-ddTHH:mm:ss */
  openTimeFrom?: string
  /** 下单时间结束（含），格式 yyyy-MM-ddTHH:mm:ss */
  openTimeTo?: string
  /** 记录创建时间起始（含） */
  createdAtFrom?: string
  /** 记录创建时间结束（含） */
  createdAtTo?: string
}

/**
 * 分页查询客如云订单
 *
 * 默认按 openTime 倒序，pageSize 最大 100
 *
 * @param query 过滤条件
 * @param pageNum 页码，从 1 开始
 * @param pageSize 每页条数
 * @returns 分页结果
 */
export function pageOrders(query: KeruyunOrderQuery, pageNum = 1, pageSize = 20) {
  return http
    .get<ApiResult<KeruyunOrderPageVO>>('/v1/keruyun/orders', {
      params: { ...query, pageNum, pageSize }
    })
    .then((r) => r.data.data)
}

/**
 * 全量导出客如云订单（Excel 导出专用）
 *
 * 按 openTime 升序，单次最多返回 50000 条，超出返回 code=40001
 *
 * @param query 过滤条件（不含分页参数）
 * @returns 订单数组
 */
export function exportOrders(query: KeruyunOrderQuery) {
  return http
    .get<ApiResult<KeruyunOrderVO[]>>('/v1/keruyun/orders/export', { params: query })
    .then((r) => r.data.data)
}

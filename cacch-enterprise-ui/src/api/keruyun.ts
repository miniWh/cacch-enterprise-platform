import { http } from './http'
import type { ApiResult } from './codes'

/**
 * 客如云订单菜品明细行（字段同接口文档第十二节）
 *
 * 一行 = 一个订单 × 一个 SKU；金额字段服务端已转为「元」并保留 2 位小数
 */
export interface KeruyunOrderItemVO {
  /** 客如云订单 ID（同订单多行相同） */
  orderId?: string
  /** 门店 ID */
  shopId?: string
  /** 业务订单号 */
  busiOrderNo?: string
  /** 订单状态（服务端已转中文展示值，未识别枚举返回原始值） */
  orderStatus?: string
  /** 订单类型（服务端已转中文展示值，未识别枚举返回原始值） */
  orderType?: string
  /** 下单时间（ISO-8601） */
  openTime?: string
  /** 完结时间（ISO-8601，未完结订单为 null） */
  finishTime?: string
  /** SKU 编码 */
  itemCode?: string
  /** 菜品名称 */
  itemName?: string
  /** 菜品大类 */
  bigTypeName?: string
  /** 菜品中类 */
  midTypeName?: string
  /** 单位 */
  unitName?: string
  /** 规格（拼接展示值） */
  specName?: string
  /** 数量 */
  quantity?: number
  /** 是否赠菜：0 否 / 1 是 */
  giftFlag?: number
  /** 售卖状态：NORMAL 销售 / DISCARD 退菜 */
  saleStatusTypeCode?: string
  /** 原始单价（元，赠菜行可能为 null） */
  itemPriceYuan?: number
  /** 售价（元，赠菜行可能为 null） */
  salePriceYuan?: number
  /** 销售金额（元） */
  itemSaleAmtYuan?: number
}

/** 菜品明细分页返回结构（MyBatis-Plus IPage） */
export interface KeruyunOrderItemPageVO {
  records: KeruyunOrderItemVO[]
  total: number
  size: number
  current: number
  pages: number
}

/** 菜品明细查询条件（分页与导出共用） */
export interface KeruyunOrderItemQuery {
  /** 客如云订单 ID，精确匹配 */
  orderId?: string
  /** SKU 编码，精确匹配 */
  itemCode?: string
  /** 菜品名称，模糊匹配 */
  itemName?: string
  /** 订单状态，精确匹配（传客如云原始枚举值） */
  orderStatus?: string
  /** 订单类型，精确匹配（传客如云原始枚举值） */
  orderType?: string
  /** 下单时间起始（含），格式 yyyy-MM-ddTHH:mm:ss */
  openTimeFrom?: string
  /** 下单时间结束（含），格式 yyyy-MM-ddTHH:mm:ss */
  openTimeTo?: string
}

/**
 * 分页查询订单菜品明细（SKU 维度）
 *
 * 按下单时间倒序，同一订单的 SKU 行相邻；pageSize 最大 100
 *
 * @param query 过滤条件
 * @param pageNum 页码，从 1 开始
 * @param pageSize 每页条数
 * @returns 分页结果
 */
export function pageOrderItems(query: KeruyunOrderItemQuery, pageNum = 1, pageSize = 20) {
  return http
    .get<ApiResult<KeruyunOrderItemPageVO>>('/v1/keruyun/order-items', {
      params: { ...query, pageNum, pageSize }
    })
    .then((r) => r.data.data)
}

/**
 * 全量导出订单菜品明细（Excel 导出专用）
 *
 * 排序与分页接口一致，单次最多返回 50000 条，超出返回 code=40001 提示缩小范围
 *
 * @param query 过滤条件（不含分页参数）
 * @returns 明细行数组
 */
export function exportOrderItems(query: KeruyunOrderItemQuery) {
  return http
    .get<ApiResult<KeruyunOrderItemVO[]>>('/v1/keruyun/order-items/export', { params: query })
    .then((r) => r.data.data)
}

// =====================================================================
// 客如云订单同步任务
// =====================================================================

/** 同步任务状态枚举 */
export type KeruyunSyncTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'SUCCESS' | 'FAILED' | 'PARTIAL'

/** 客如云同步任务对象（字段同接口文档第九节） */
export interface KeruyunSyncTaskVO {
  /** 内部主键（雪花算法） */
  id: number
  /** 同步日期（startTime 的日期部分） */
  syncDate: string
  /** 查询时间类型，固定 OPEN_TIME */
  dateType: string
  /** 查询窗口起始时间 */
  syncWindowStart: string
  /** 查询窗口结束时间 */
  syncWindowEnd: string
  /** 任务状态 */
  status: KeruyunSyncTaskStatus
  /** 已重试次数 */
  retryCount: number
  /** 已拉取到的最后页码 */
  lastPage?: number
  /** 总页数 */
  totalPage?: number
  /** 总订单数 */
  totalCount?: number
  /** 落库成功数 */
  successCount?: number
  /** 落库失败数 */
  failureCount?: number
  /** 失败原因（成功时为 null） */
  errorMessage?: string
  /** 任务创建时间 */
  createdAt: string
  /** 任务最近更新时间 */
  updatedAt: string
  /** 任务执行完成时间（未完成时为 null） */
  syncedAt?: string
}

/** 同步任务分页返回结构 */
export interface KeruyunSyncTaskPageVO {
  records: KeruyunSyncTaskVO[]
  total: number
  size: number
  current: number
  pages: number
}

/** 手动同步响应 data */
export interface KeruyunManualSyncResultVO {
  /** 本次新建任务数（手动同步固定为 0） */
  createdTaskCount: number
  /** 本次待执行任务数（固定为 1） */
  pendingTaskCount: number
  /** 任务 ID 列表 */
  taskIds: number[]
  /** 执行概览描述 */
  message: string
}

/**
 * 分页查询同步任务记录
 *
 * 按 syncDate 倒序，pageSize 最大 100
 *
 * @param pageNum 页码，从 1 开始
 * @param pageSize 每页条数
 * @returns 分页结果
 */
export function pageSyncTasks(pageNum = 1, pageSize = 20) {
  return http
    .get<ApiResult<KeruyunSyncTaskPageVO>>('/v1/keruyun/orders/sync/tasks', {
      params: { pageNum, pageSize }
    })
    .then((r) => r.data.data)
}

/**
 * 手动触发跨天同步
 *
 * dateType 固定 OPEN_TIME，异步执行，接口立即返回；
 * 若同日已存在任务则重置查询窗口并重新执行，否则新建任务。
 *
 * @param startTime 查询窗口起始时间，格式 yyyy-MM-ddTHH:mm:ss
 * @param endTime 查询窗口结束时间，须晚于 startTime
 * @returns 触发结果
 */
export function triggerManualSync(startTime: string, endTime: string) {
  return http
    .post<ApiResult<KeruyunManualSyncResultVO>>('/v1/keruyun/orders/sync/tasks/manual', {
      startTime,
      endTime
    })
    .then((r) => r.data.data)
}

<template>
  <div class="p-6">
    <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <!-- 查询条件 -->
      <div class="flex flex-wrap items-center gap-3">
        <el-input v-model="query.orderId" placeholder="订单ID" clearable class="!w-44" @keyup.enter="onSearch" />
        <el-input v-model="query.itemCode" placeholder="SKU编码" clearable class="!w-36" @keyup.enter="onSearch" />
        <el-input v-model="query.itemName" placeholder="菜品名称" clearable class="!w-36" @keyup.enter="onSearch" />
        <el-select v-model="query.orderStatus" placeholder="订单状态" clearable class="!w-36" @change="onSearch">
          <el-option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.orderType" placeholder="订单类型" clearable class="!w-36" @change="onSearch">
          <el-option v-for="item in ORDER_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="openTimeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="下单开始"
          end-placeholder="下单结束"
          class="!w-[380px]"
        />
        <el-button type="primary" :icon="Search" :loading="loading" @click="onSearch">查询</el-button>
        <el-button :icon="RefreshLeft" @click="onReset">重置</el-button>
        <div class="ml-auto">
          <el-button type="success" :icon="Download" :loading="exporting" @click="onExport">导出 Excel</el-button>
        </div>
      </div>

      <!-- 列表：SKU 维度明细行，同一订单的行相邻、不合并单元格 -->
      <el-table :data="rows" v-loading="loading" class="mt-4" stripe border>
        <el-table-column prop="orderId" label="订单ID" min-width="160" show-overflow-tooltip />
        <el-table-column prop="shopId" label="门店ID" width="110" />
        <el-table-column prop="busiOrderNo" label="业务订单号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderStatus" label="订单状态" width="100">
          <template #default="{ row }">{{ row.orderStatus ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="orderType" label="订单类型" width="100">
          <template #default="{ row }">{{ row.orderType ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">
            <span class="whitespace-nowrap">{{ formatDateTime(row.openTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="完结时间" width="170">
          <template #default="{ row }">
            <span class="whitespace-nowrap">{{ formatDateTime(row.finishTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="itemCode" label="SKU编码" min-width="110" show-overflow-tooltip />
        <el-table-column prop="itemName" label="菜品名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="bigTypeName" label="大类" width="90" show-overflow-tooltip />
        <el-table-column prop="midTypeName" label="中类" width="90" show-overflow-tooltip />
        <el-table-column prop="unitName" label="单位" width="70" />
        <el-table-column prop="specName" label="规格" min-width="130" show-overflow-tooltip />
        <el-table-column label="数量" width="80" align="right">
          <template #default="{ row }">{{ formatQuantity(row.quantity) }}</template>
        </el-table-column>
        <el-table-column label="赠菜" width="70" align="center">
          <template #default="{ row }">{{ formatGiftFlag(row.giftFlag) }}</template>
        </el-table-column>
        <el-table-column label="售卖状态" width="90" align="center">
          <template #default="{ row }">{{ formatSaleStatus(row.saleStatusTypeCode) }}</template>
        </el-table-column>
        <el-table-column label="原始单价(元)" width="110" align="right">
          <template #default="{ row }">{{ formatAmount(row.itemPriceYuan) }}</template>
        </el-table-column>
        <el-table-column label="售价(元)" width="100" align="right">
          <template #default="{ row }">{{ formatAmount(row.salePriceYuan) }}</template>
        </el-table-column>
        <el-table-column label="销售金额(元)" width="110" align="right">
          <template #default="{ row }">{{ formatAmount(row.itemSaleAmtYuan) }}</template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        class="mt-4 justify-end"
        layout="total, prev, pager, next, sizes"
        :total="total"
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        @change="fetchPage"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Search, RefreshLeft, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import {
  pageOrderItems,
  exportOrderItems,
  type KeruyunOrderItemQuery,
  type KeruyunOrderItemVO
} from '../../api/keruyun'

// ---------- 筛选枚举选项（按接口文档 12.3，value 传客如云原始枚举值） ----------
const ORDER_STATUS_OPTIONS: { value: string; label: string }[] = [
  { value: 'WAIT_PROCESSED', label: '待处理' },
  { value: 'ORDER_RECEIVED', label: '已接单' },
  { value: 'SUCCESS', label: '成功' },
  { value: 'WAIT_SETTLED', label: '待结账' },
  { value: 'SETTLED', label: '已结账' },
  { value: 'REFUND', label: '已退单' },
  { value: 'CLOSED', label: '已关闭' },
  { value: 'INVALID', label: '已作废' },
  { value: 'CANCELLED', label: '已取消' },
  { value: 'REJECTED', label: '已拒单' },
  { value: 'ANTI_SETTLED', label: '已反结' }
]

const ORDER_TYPE_OPTIONS: { value: string; label: string }[] = [
  { value: 'FOR_HERE', label: '堂食' },
  { value: 'PLATFORM_TAKE_OUT', label: '平台外卖' },
  { value: 'SELF_TAKE_OUT', label: '门店外带' },
  { value: 'SELF_TAKE', label: '自提' },
  { value: 'NO_ORDER_CASHIER', label: '无单收银' },
  { value: 'MEMBER_STORE', label: '会员储值' },
  { value: 'MEMBER_MANUAL_STORE', label: '会员手工储值' },
  { value: 'REPAYMENT_ORDER', label: '销账订单' }
]

// ---------- 展示格式化 ----------
// 订单状态/类型：服务端已转中文展示值，未识别枚举原样返回，前端直接展示

/** 赠菜标记转展示文案 */
function formatGiftFlag(flag?: number): string {
  if (flag === null || flag === undefined) return '-'
  return flag === 1 ? '是' : '否'
}

/** 售卖状态转展示文案 */
function formatSaleStatus(code?: string): string {
  if (!code) return '-'
  if (code === 'NORMAL') return '销售'
  if (code === 'DISCARD') return '退菜'
  return code
}

// ---------- 查询条件 ----------
const query = reactive<KeruyunOrderItemQuery>({
  orderId: '',
  itemCode: '',
  itemName: '',
  orderStatus: '',
  orderType: '',
  openTimeFrom: undefined,
  openTimeTo: undefined
})

/** 下单时间区间选择器绑定值（Date 对象数组） */
const openTimeRange = ref<[Date, Date] | null>(null)

/** 将 Date 格式化为 yyyy-MM-ddTHH:mm:ss */
function formatToLocalDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 将区间选择器的 Date 值同步到 query，格式化为接口要求的 yyyy-MM-ddTHH:mm:ss */
function syncRanges() {
  query.openTimeFrom = openTimeRange.value?.[0] ? formatToLocalDateTime(openTimeRange.value[0]) : undefined
  query.openTimeTo = openTimeRange.value?.[1] ? formatToLocalDateTime(openTimeRange.value[1]) : undefined
}

/** 构造实际请求参数（剔除空值） */
function buildQueryParams(): KeruyunOrderItemQuery {
  syncRanges()
  const params: KeruyunOrderItemQuery = {}
  ;(Object.keys(query) as (keyof KeruyunOrderItemQuery)[]).forEach((k) => {
    const v = query[k]
    if (v !== '' && v !== undefined && v !== null) {
      ;(params as Record<string, unknown>)[k] = v
    }
  })
  return params
}

// ---------- 列表与分页 ----------
const rows = ref<KeruyunOrderItemVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const loading = ref(false)

async function fetchPage() {
  loading.value = true
  try {
    const res = await pageOrderItems(buildQueryParams(), pageNum.value, pageSize.value)
    rows.value = res.records ?? []
    total.value = res.total ?? 0
  } catch (e) {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  fetchPage()
}

function onReset() {
  query.orderId = ''
  query.itemCode = ''
  query.itemName = ''
  query.orderStatus = ''
  query.orderType = ''
  openTimeRange.value = null
  pageNum.value = 1
  fetchPage()
}

// ---------- 导出 ----------
const exporting = ref(false)

/** Excel 列定义（与表格展示列一致） */
const EXPORT_COLUMNS: { key: keyof KeruyunOrderItemVO; label: string }[] = [
  { key: 'orderId', label: '订单ID' },
  { key: 'shopId', label: '门店ID' },
  { key: 'busiOrderNo', label: '业务订单号' },
  { key: 'orderStatus', label: '订单状态' },
  { key: 'orderType', label: '订单类型' },
  { key: 'openTime', label: '下单时间' },
  { key: 'finishTime', label: '完结时间' },
  { key: 'itemCode', label: 'SKU编码' },
  { key: 'itemName', label: '菜品名称' },
  { key: 'bigTypeName', label: '大类' },
  { key: 'midTypeName', label: '中类' },
  { key: 'unitName', label: '单位' },
  { key: 'specName', label: '规格' },
  { key: 'quantity', label: '数量' },
  { key: 'giftFlag', label: '赠菜' },
  { key: 'saleStatusTypeCode', label: '售卖状态' },
  { key: 'itemPriceYuan', label: '原始单价(元)' },
  { key: 'salePriceYuan', label: '售价(元)' },
  { key: 'itemSaleAmtYuan', label: '销售金额(元)' }
]

async function onExport() {
  exporting.value = true
  try {
    const list = await exportOrderItems(buildQueryParams())
    if (!list || list.length === 0) {
      ElMessage.warning('当前条件下无数据可导出')
      return
    }
    const aoa: unknown[][] = [
      EXPORT_COLUMNS.map((c) => c.label),
      ...list.map((row) =>
        EXPORT_COLUMNS.map((c) => {
          const v = row[c.key]
          if (c.key === 'openTime' || c.key === 'finishTime') {
            return formatDateTime(v as string)
          }
          if (c.key === 'giftFlag') {
            return formatGiftFlag(v as number)
          }
          if (c.key === 'saleStatusTypeCode') {
            return formatSaleStatus(v as string)
          }
          return v ?? ''
        })
      )
    ]
    const ws = XLSX.utils.aoa_to_sheet(aoa)
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, '客如云订单明细')
    const fileName = `客如云订单明细_${formatFileTimestamp()}.xlsx`
    XLSX.writeFile(wb, fileName)
    ElMessage.success(`已导出 ${list.length} 条数据`)
  } catch (e) {
    // 超限（code=40001）等业务错误由 http 拦截器 reject，消息取自 response.message
    const msg = e instanceof Error ? e.message : '导出失败'
    ElMessage.error(msg)
  } finally {
    exporting.value = false
  }
}

// ---------- 工具方法 ----------
/** 格式化 LocalDateTime 字符串为 yyyy-MM-dd HH:mm:ss */
function formatDateTime(value?: string): string {
  if (!value) return '-'
  // 接口返回 ISO-8601，如 2026-10-21T12:30:00
  return value.replace('T', ' ').substring(0, 19)
}

/** 数量格式化：空值显示 - */
function formatQuantity(value?: number): string {
  if (value === null || value === undefined) return '-'
  return String(value)
}

/** 金额格式化：保留 2 位小数，空值显示 - */
function formatAmount(value?: number): string {
  if (value === null || value === undefined) return '-'
  return value.toFixed(2)
}

/** 导出文件名时间戳：yyyyMMddHHmmss */
function formatFileTimestamp(): string {
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}${pad(d.getHours())}${pad(d.getMinutes())}${pad(d.getSeconds())}`
}

// 初始加载
fetchPage()
</script>

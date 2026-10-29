/**
 * 通用格式化工具
 */

/**
 * 将后端 ISO 时间字符串格式化为 yyyy-MM-dd HH:mm:ss
 *
 * <p>后端 LocalDateTime 经 Jackson 序列化为 ISO 格式（如 2026-10-29T15:51:45，可能携带毫秒/微秒），
 * 此处直接做字符串变换，不经过 Date 解析，避免时区歧义。
 *
 * @param value ISO 时间字符串，可空
 * @return 形如 2026-10-29 15:51:45 的字符串；入参为空时返回占位符 —
 */
export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—'
  }
  return value.replace('T', ' ').slice(0, 19)
}

/**
 * 格式化文件大小为人类可读格式
 * @param bytes 文件大小（字节）
 * @returns 格式化后的字符串，如 "1.5 MB"
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  if (bytes < 0) return '未知'

  const units = ['B', 'KB', 'MB', 'GB']
  const k = 1024
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(k)), units.length - 1)
  const size = bytes / Math.pow(k, i)

  return `${size.toFixed(i === 0 ? 0 : 2)} ${units[i]}`
}

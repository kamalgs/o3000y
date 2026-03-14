export function formatNumber(v: unknown): string {
  if (v == null) return ''
  const n = Number(v)
  if (isNaN(n)) return String(v)
  if (Number.isInteger(n)) return n.toLocaleString()
  return n.toLocaleString(undefined, { maximumFractionDigits: 2 })
}

export function formatDuration(us: number): string {
  if (us < 1_000) return `${us} us`
  if (us < 1_000_000) return `${(us / 1_000).toFixed(1)} ms`
  return `${(us / 1_000_000).toFixed(2)} s`
}

export function formatTimestamp(v: string): string {
  try {
    const d = new Date(v)
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch {
    return v
  }
}

export function isNumericColumn(rows: unknown[][], colIndex: number): boolean {
  let numeric = 0
  let total = 0
  const sample = Math.min(rows.length, 20)
  for (let i = 0; i < sample; i++) {
    const v = rows[i][colIndex]
    if (v == null) continue
    total++
    if (typeof v === 'number') numeric++
  }
  return total > 0 && numeric / total > 0.5
}

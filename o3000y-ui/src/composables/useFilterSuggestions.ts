import { ref, watch, type Ref } from 'vue'
import { executeQuery } from '@/api/client'
import { FILTER_COLUMNS } from '@/lib/queryBuilder'

export function useFilterSuggestions(column: Ref<string>) {
  const suggestions = ref<string[]>([])
  const loading = ref(false)

  async function fetch() {
    if (!FILTER_COLUMNS.includes(column.value)) {
      suggestions.value = []
      return
    }
    loading.value = true
    try {
      const result = await executeQuery(
        `SELECT DISTINCT ${column.value}::VARCHAR FROM spans ORDER BY 1 LIMIT 50`,
      )
      suggestions.value = result.rows.map((r) => String(r[0] ?? '')).filter(Boolean)
    } catch {
      suggestions.value = []
    } finally {
      loading.value = false
    }
  }

  watch(column, fetch, { immediate: true })
  return { suggestions, loading, refresh: fetch }
}

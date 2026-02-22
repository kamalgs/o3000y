<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { QueryResponse } from '../api/client'

const props = defineProps<{ result: QueryResponse }>()
const router = useRouter()

const sortColumn = ref<number | null>(null)
const sortAsc = ref(true)
const page = ref(0)
const pageSize = 50

const sortedRows = computed(() => {
  const rows = [...props.result.rows]
  if (sortColumn.value !== null) {
    const col = sortColumn.value
    const asc = sortAsc.value
    rows.sort((a, b) => {
      const va = a[col]
      const vb = b[col]
      if (va == null && vb == null) return 0
      if (va == null) return asc ? -1 : 1
      if (vb == null) return asc ? 1 : -1
      if (typeof va === 'number' && typeof vb === 'number') return asc ? va - vb : vb - va
      return asc ? String(va).localeCompare(String(vb)) : String(vb).localeCompare(String(va))
    })
  }
  return rows
})

const pagedRows = computed(() => {
  const start = page.value * pageSize
  return sortedRows.value.slice(start, start + pageSize)
})

const totalPages = computed(() => Math.ceil(sortedRows.value.length / pageSize))

function toggleSort(col: number) {
  if (sortColumn.value === col) {
    sortAsc.value = !sortAsc.value
  } else {
    sortColumn.value = col
    sortAsc.value = true
  }
}

function isTraceIdColumn(colName: string): boolean {
  return colName === 'trace_id'
}

function navigateToTrace(value: unknown) {
  if (value) {
    router.push(`/trace/${value}`)
  }
}
</script>

<template>
  <div class="bg-white rounded-lg border border-gray-200">
    <div class="px-4 py-3 border-b border-gray-200 flex items-center justify-between">
      <span class="text-sm text-gray-600">
        {{ result.rowCount }} rows in {{ result.elapsedMs }}ms
      </span>
      <div v-if="totalPages > 1" class="flex items-center gap-2 text-sm">
        <button
          @click="page = Math.max(0, page - 1)"
          :disabled="page === 0"
          class="px-2 py-1 rounded border disabled:opacity-30"
        >
          Prev
        </button>
        <span>{{ page + 1 }} / {{ totalPages }}</span>
        <button
          @click="page = Math.min(totalPages - 1, page + 1)"
          :disabled="page >= totalPages - 1"
          class="px-2 py-1 rounded border disabled:opacity-30"
        >
          Next
        </button>
      </div>
    </div>
    <div class="overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-gray-50 border-b border-gray-200">
            <th
              v-for="(col, i) in result.columns"
              :key="col"
              @click="toggleSort(i)"
              class="px-4 py-2 text-left font-medium text-gray-600 cursor-pointer hover:bg-gray-100 whitespace-nowrap select-none"
            >
              {{ col }}
              <span v-if="sortColumn === i" class="ml-1">{{ sortAsc ? '\u25B2' : '\u25BC' }}</span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(row, ri) in pagedRows"
            :key="ri"
            class="border-b border-gray-100 hover:bg-gray-50"
          >
            <td
              v-for="(val, ci) in row"
              :key="ci"
              class="px-4 py-2 whitespace-nowrap font-mono text-xs"
            >
              <a
                v-if="isTraceIdColumn(result.columns[ci]) && val"
                @click.prevent="navigateToTrace(val)"
                class="text-blue-600 hover:underline cursor-pointer"
              >
                {{ val }}
              </a>
              <span v-else>{{ val ?? '' }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

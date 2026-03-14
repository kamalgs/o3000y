<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type QueryResponse } from '@/api/client'
import { formatNumber, isNumericColumn } from '@/lib/formatting'

const props = defineProps<{ result: QueryResponse }>()
const router = useRouter()

const sortCol = ref(-1)
const sortAsc = ref(true)
const page = ref(0)
const PAGE_SIZE = 50

const numericCols = computed(() => {
  const set = new Set<number>()
  for (let i = 0; i < props.result.columns.length; i++) {
    if (isNumericColumn(props.result.rows, i)) set.add(i)
  }
  return set
})

const statusCodeIdx = computed(() => props.result.columns.indexOf('status_code'))
const traceIdIdx = computed(() => props.result.columns.indexOf('trace_id'))

const sortedRows = computed(() => {
  if (sortCol.value < 0) return props.result.rows
  const col = sortCol.value
  const dir = sortAsc.value ? 1 : -1
  return [...props.result.rows].sort((a, b) => {
    const va = a[col]
    const vb = b[col]
    if (va == null && vb == null) return 0
    if (va == null) return dir
    if (vb == null) return -dir
    if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
    return String(va).localeCompare(String(vb)) * dir
  })
})

const pagedRows = computed(() =>
  sortedRows.value.slice(page.value * PAGE_SIZE, (page.value + 1) * PAGE_SIZE),
)

const totalPages = computed(() => Math.ceil(sortedRows.value.length / PAGE_SIZE))

function toggleSort(col: number) {
  if (sortCol.value === col) {
    sortAsc.value = !sortAsc.value
  } else {
    sortCol.value = col
    sortAsc.value = true
  }
  page.value = 0
}

function isErrorRow(row: unknown[]): boolean {
  if (statusCodeIdx.value < 0) return false
  const v = row[statusCodeIdx.value]
  return v === 2 || v === '2'
}

function cellDisplay(value: unknown, colIdx: number): string {
  if (value == null) return ''
  if (numericCols.value.has(colIdx)) return formatNumber(value)
  return String(value)
}

function statusChip(value: unknown): string | null {
  if (value === 2 || value === '2') return 'ERROR'
  if (value === 1 || value === '1') return 'OK'
  return null
}
</script>

<template>
  <div class="card p-0 overflow-hidden">
    <div class="flex items-center justify-between px-3 py-2" style="border-bottom: 1px solid var(--color-border-light)">
      <span class="text-xs" style="color: var(--color-text-secondary)">
        {{ result.rowCount }} rows &middot; {{ result.elapsedMs }} ms
      </span>
      <div v-if="totalPages > 1" class="flex items-center gap-2">
        <button class="btn btn-ghost text-xs" :disabled="page === 0" @click="page--">&larr;</button>
        <span class="text-xs" style="color: var(--color-text-secondary)">
          {{ page + 1 }} / {{ totalPages }}
        </span>
        <button class="btn btn-ghost text-xs" :disabled="page >= totalPages - 1" @click="page++">&rarr;</button>
      </div>
    </div>

    <div class="overflow-x-auto">
      <table class="w-full">
        <thead>
          <tr>
            <th
              v-for="(col, i) in result.columns"
              :key="col"
              class="th"
              :class="{ 'th--numeric': numericCols.has(i) }"
              @click="toggleSort(i)"
            >
              {{ col }}
              <span v-if="sortCol === i" class="ml-1">{{ sortAsc ? '▲' : '▼' }}</span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(row, ri) in pagedRows"
            :key="ri"
            :class="{ 'row--error': isErrorRow(row) }"
          >
            <td
              v-for="(val, ci) in row"
              :key="ci"
              class="td"
              :class="{
                'td--numeric': numericCols.has(ci),
                'td--mono': ci === traceIdIdx,
                'td--link': ci === traceIdIdx,
              }"
              @click="ci === traceIdIdx && val ? router.push('/trace/' + val) : undefined"
            >
              <template v-if="ci === statusCodeIdx && statusChip(val)">
                <span class="chip" :class="statusChip(val) === 'ERROR' ? 'chip--error' : 'chip--ok'">
                  {{ statusChip(val) }}
                </span>
              </template>
              <template v-else>
                {{ cellDisplay(val, ci) }}
              </template>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

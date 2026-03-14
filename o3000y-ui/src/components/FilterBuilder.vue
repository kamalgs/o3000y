<script setup lang="ts">
import { type Filter, FILTER_COLUMNS, OPERATORS } from '@/lib/queryBuilder'

const filters = defineModel<Filter[]>({ required: true })

function addFilter() {
  filters.value = [...filters.value, { column: 'service_name', operator: '=', value: '' }]
}

function removeFilter(index: number) {
  filters.value = filters.value.filter((_, i) => i !== index)
}

function updateFilter(index: number, field: keyof Filter, value: string) {
  const updated = [...filters.value]
  updated[index] = { ...updated[index], [field]: value }
  filters.value = updated
}
</script>

<template>
  <div class="space-y-2">
    <div class="flex items-center justify-between">
      <label class="text-sm font-medium text-gray-700">WHERE</label>
      <button
        class="text-xs text-indigo-600 hover:text-indigo-800 font-medium"
        @click="addFilter"
      >
        + Add filter
      </button>
    </div>

    <div v-if="filters.length === 0" class="text-xs text-gray-400 italic py-1">
      No filters — showing all spans in time range
    </div>

    <div
      v-for="(filter, index) in filters"
      :key="index"
      class="flex items-center gap-2"
    >
      <span v-if="index > 0" class="text-xs text-gray-500 w-8 text-right shrink-0">AND</span>
      <span v-else class="w-8 shrink-0"></span>

      <select
        :value="filter.column"
        class="block w-40 rounded border-gray-300 text-sm py-1 px-2 border focus:ring-indigo-500 focus:border-indigo-500"
        @change="updateFilter(index, 'column', ($event.target as HTMLSelectElement).value)"
      >
        <option v-for="col in FILTER_COLUMNS" :key="col" :value="col">{{ col }}</option>
      </select>

      <select
        :value="filter.operator"
        class="block w-20 rounded border-gray-300 text-sm py-1 px-2 border focus:ring-indigo-500 focus:border-indigo-500"
        @change="updateFilter(index, 'operator', ($event.target as HTMLSelectElement).value)"
      >
        <option v-for="op in OPERATORS" :key="op.value" :value="op.value">{{ op.label }}</option>
      </select>

      <input
        :value="filter.value"
        type="text"
        placeholder="value"
        class="block flex-1 rounded border-gray-300 text-sm py-1 px-2 border focus:ring-indigo-500 focus:border-indigo-500"
        @input="updateFilter(index, 'value', ($event.target as HTMLInputElement).value)"
      />

      <button
        class="text-gray-400 hover:text-red-500 text-sm shrink-0"
        @click="removeFilter(index)"
        title="Remove filter"
      >
        ✕
      </button>
    </div>
  </div>
</template>

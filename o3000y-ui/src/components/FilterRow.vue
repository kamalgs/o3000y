<script setup lang="ts">
import { computed, toRef } from 'vue'
import AutocompleteInput from './AutocompleteInput.vue'
import { useFilterSuggestions } from '@/composables/useFilterSuggestions'
import { type Filter, FILTER_COLUMNS, OPERATORS } from '@/lib/queryBuilder'

const props = defineProps<{ filter: Filter }>()
const emit = defineEmits<{ update: [filter: Filter]; remove: [] }>()

const columnRef = computed(() => props.filter.column)
const { suggestions } = useFilterSuggestions(columnRef)

function update(field: keyof Filter, value: string) {
  emit('update', { ...props.filter, [field]: value })
}
</script>

<template>
  <div class="flex items-center gap-2">
    <select
      :value="filter.column"
      class="form-select w-36"
      @change="update('column', ($event.target as HTMLSelectElement).value)"
    >
      <option v-for="col in FILTER_COLUMNS" :key="col" :value="col">{{ col }}</option>
    </select>

    <select
      :value="filter.operator"
      class="form-select w-16"
      @change="update('operator', ($event.target as HTMLSelectElement).value)"
    >
      <option v-for="op in OPERATORS" :key="op.value" :value="op.value">{{ op.label }}</option>
    </select>

    <AutocompleteInput
      :model-value="filter.value"
      :suggestions="suggestions"
      @update:model-value="update('value', $event)"
    />

    <button
      class="text-[var(--color-text-muted)] hover:text-[var(--color-text-error)] shrink-0 text-sm"
      @click="$emit('remove')"
      title="Remove filter"
    >
      &times;
    </button>
  </div>
</template>

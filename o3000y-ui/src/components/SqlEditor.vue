<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{ loading: boolean; initialSql?: string }>(),
  { initialSql: 'SELECT * FROM spans LIMIT 100' },
)
const emit = defineEmits<{ execute: [sql: string] }>()

const sql = ref(props.initialSql)

watch(
  () => props.initialSql,
  (v) => {
    sql.value = v
  },
)

function execute() {
  if (sql.value.trim()) {
    emit('execute', sql.value.trim())
  }
}

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    execute()
  }
}
</script>

<template>
  <div class="bg-white rounded-lg border border-gray-200 p-4">
    <label class="block text-sm font-medium text-gray-700 mb-2">SQL Query</label>
    <textarea
      v-model="sql"
      @keydown="onKeydown"
      class="w-full h-32 font-mono text-sm border border-gray-300 rounded p-3 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 resize-y"
      placeholder="SELECT * FROM spans LIMIT 100"
      spellcheck="false"
    />
    <div class="mt-2 flex items-center justify-between">
      <span class="text-xs text-gray-400">Ctrl+Enter to execute</span>
      <button
        @click="execute"
        :disabled="loading"
        class="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700 disabled:opacity-50 text-sm"
      >
        {{ loading ? 'Running...' : 'Execute' }}
      </button>
    </div>
  </div>
</template>

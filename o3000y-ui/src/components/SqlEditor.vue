<script setup lang="ts">
import { ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{ loading: boolean; initialSql?: string }>(),
  { initialSql: 'SELECT * FROM spans ORDER BY start_time DESC LIMIT 100' },
)
const emit = defineEmits<{ execute: [sql: string] }>()

const sql = ref(props.initialSql)
watch(() => props.initialSql, (v) => { sql.value = v })

function execute() {
  if (sql.value.trim()) emit('execute', sql.value.trim())
}

function onKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') execute()
}
</script>

<template>
  <div class="card">
    <label class="form-label">SQL Query</label>
    <textarea
      v-model="sql"
      @keydown="onKeydown"
      class="form-input w-full mt-1"
      style="height: 8rem; resize: vertical; font-family: var(--font-mono); font-size: 0.8125rem"
      placeholder="SELECT * FROM spans LIMIT 100"
      spellcheck="false"
    />
    <div class="flex items-center justify-between mt-2">
      <span class="text-xs" style="color: var(--color-text-muted)">Ctrl+Enter to execute</span>
      <button class="btn btn-primary" :disabled="loading" @click="execute">
        {{ loading ? 'Running\u2026' : 'Execute' }}
      </button>
    </div>
  </div>
</template>

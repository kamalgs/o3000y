<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SqlEditor from '../components/SqlEditor.vue'
import ResultsTable from '../components/ResultsTable.vue'
import { executeQuery, type QueryResponse } from '../api/client'

const router = useRouter()
const route = useRoute()

const result = ref<QueryResponse | null>(null)
const error = ref('')
const loading = ref(false)
const initialSQL = ref('SELECT * FROM spans LIMIT 100')

onMounted(() => {
  const sqlParam = route.query.sql as string
  if (sqlParam) {
    initialSQL.value = sqlParam
    onExecute(sqlParam)
  }
})

async function onExecute(sql: string) {
  error.value = ''
  loading.value = true
  router.replace({ path: '/query', query: { sql } })
  try {
    result.value = await executeQuery(sql)
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
    result.value = null
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="space-y-4">
    <SqlEditor @execute="onExecute" :loading="loading" :initial-sql="initialSQL" />
    <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
      {{ error }}
    </div>
    <ResultsTable v-if="result" :result="result" />
  </div>
</template>

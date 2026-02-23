<script setup lang="ts">
import { ref } from 'vue'
import SearchForm from '../components/SearchForm.vue'
import ResultsTable from '../components/ResultsTable.vue'
import DurationHistogram from '../components/DurationHistogram.vue'
import { searchSpans, type QueryResponse, type SearchParams } from '../api/client'

const result = ref<QueryResponse | null>(null)
const error = ref('')
const loading = ref(false)

async function onSearch(params: SearchParams) {
  error.value = ''
  loading.value = true
  try {
    result.value = await searchSpans(params)
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
    <SearchForm @search="onSearch" :loading="loading" />
    <div v-if="error" class="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
      {{ error }}
    </div>
    <DurationHistogram v-if="result" :result="result" />
    <ResultsTable v-if="result" :result="result" />
  </div>
</template>

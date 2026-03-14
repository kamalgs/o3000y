<script setup lang="ts">
import { onMounted } from 'vue'
import { useExploreQuery } from '@/composables/useExploreQuery'
import QueryBuilder from '@/components/QueryBuilder.vue'
import ErrorBanner from '@/components/ErrorBanner.vue'
import SummaryCard from '@/components/SummaryCard.vue'
import TimeChart from '@/components/TimeChart.vue'
import BreakdownTable from '@/components/BreakdownTable.vue'

const {
  query,
  loading,
  error,
  summaryValue,
  chartSeries,
  breakdownGroups,
  generatedSQL,
  label,
  initFromURL,
  run,
  drillDown,
} = useExploreQuery()

onMounted(() => {
  initFromURL()
  run()
})

function copyLink() {
  navigator.clipboard.writeText(window.location.href)
}
</script>

<template>
  <div class="space-y-4">
    <QueryBuilder v-model="query" :loading="loading" @run="run" />

    <ErrorBanner :message="error" />

    <SummaryCard
      v-if="summaryValue != null"
      :value="summaryValue"
      :label="label"
      :sql="generatedSQL"
      @copy-link="copyLink"
    />

    <TimeChart
      v-if="chartSeries.length > 0"
      :series="chartSeries"
      :label="label"
    />

    <BreakdownTable
      v-if="breakdownGroups.length > 0"
      :groups="breakdownGroups"
      :label="label"
      :group-label="query.groupBy"
      @select-group="drillDown"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatNumber } from '@/lib/formatting'

const props = defineProps<{
  groups: { key: string; value: number }[]
  label: string
  groupLabel: string
}>()

defineEmits<{ selectGroup: [key: string] }>()

const maxValue = computed(() =>
  props.groups.reduce((max, g) => Math.max(max, g.value), 0),
)
</script>

<template>
  <div class="card p-0 overflow-hidden" v-if="groups.length > 0">
    <table class="w-full">
      <thead>
        <tr>
          <th class="th">{{ groupLabel }}</th>
          <th class="th th--numeric">{{ label }}</th>
          <th class="th" style="width: 40%"></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="g in groups"
          :key="g.key"
          class="hover:bg-[var(--color-hover)] cursor-pointer"
          @click="$emit('selectGroup', g.key)"
        >
          <td class="td td--link">{{ g.key }}</td>
          <td class="td td--numeric">{{ formatNumber(g.value) }}</td>
          <td class="td">
            <div
              class="pct-bar"
              :style="{ width: (g.value / maxValue * 100) + '%' }"
            />
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

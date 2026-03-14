<script setup lang="ts">
defineProps<{ message: string }>()
</script>

<template>
  <div v-if="message" class="error-banner">{{ message }}</div>
</template>

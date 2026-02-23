<script setup lang="ts">
import ServiceSidebar from './components/ServiceSidebar.vue'
</script>

<template>
  <div class="flex h-screen">
    <ServiceSidebar class="w-64 flex-shrink-0" />
    <div class="flex-1 flex flex-col overflow-hidden">
      <header class="bg-white border-b border-gray-200 px-6 py-3 flex items-center gap-4">
        <h1 class="text-xl font-bold text-gray-800">o3000y</h1>
        <nav class="flex gap-2">
          <router-link
            to="/query"
            class="px-3 py-1 rounded text-sm hover:bg-gray-100"
            active-class="bg-blue-100 text-blue-700"
          >
            SQL Query
          </router-link>
          <router-link
            to="/search"
            class="px-3 py-1 rounded text-sm hover:bg-gray-100"
            active-class="bg-blue-100 text-blue-700"
          >
            Search
          </router-link>
        </nav>
      </header>
      <main class="flex-1 overflow-auto p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>

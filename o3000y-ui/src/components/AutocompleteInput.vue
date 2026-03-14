<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps<{
  modelValue: string
  suggestions: string[]
  loading?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  focus: []
}>()

const open = ref(false)
const activeIndex = ref(-1)
const inputRef = ref<HTMLInputElement>()

const filtered = computed(() => {
  const q = props.modelValue.toLowerCase()
  if (!q) return props.suggestions.slice(0, 30)
  return props.suggestions.filter((s) => s.toLowerCase().includes(q)).slice(0, 30)
})

watch(
  () => props.suggestions,
  () => {
    if (props.suggestions.length > 0 && document.activeElement === inputRef.value) {
      open.value = true
    }
  },
)

function onFocus() {
  open.value = true
  activeIndex.value = -1
  emit('focus')
}

function onBlur() {
  // delay to allow click on dropdown item
  setTimeout(() => (open.value = false), 150)
}

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLInputElement).value)
  open.value = true
  activeIndex.value = -1
}

function select(val: string) {
  emit('update:modelValue', val)
  open.value = false
}

function onKeydown(e: KeyboardEvent) {
  if (!open.value || filtered.value.length === 0) return
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    activeIndex.value = Math.min(activeIndex.value + 1, filtered.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    activeIndex.value = Math.max(activeIndex.value - 1, 0)
  } else if (e.key === 'Enter' && activeIndex.value >= 0) {
    e.preventDefault()
    select(filtered.value[activeIndex.value])
  } else if (e.key === 'Escape') {
    open.value = false
  }
}
</script>

<template>
  <div class="relative flex-1">
    <input
      ref="inputRef"
      :value="modelValue"
      :placeholder="placeholder || 'value'"
      class="form-input w-full"
      @input="onInput"
      @focus="onFocus"
      @blur="onBlur"
      @keydown="onKeydown"
    />
    <div v-if="open && filtered.length > 0" class="dropdown">
      <div
        v-for="(item, i) in filtered"
        :key="item"
        class="dropdown-item"
        :class="{ 'dropdown-item--active': i === activeIndex }"
        @mousedown.prevent="select(item)"
      >
        {{ item }}
      </div>
    </div>
  </div>
</template>

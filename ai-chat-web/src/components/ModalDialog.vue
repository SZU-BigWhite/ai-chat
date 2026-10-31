<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import AppIcon from './AppIcon.vue'
defineProps<{ title: string; wide?: boolean }>()
const emit = defineEmits<{ close: [] }>()
const panel = ref<HTMLElement>()
const previous = document.activeElement as HTMLElement | null
// Tab 焦点保持在弹窗内部，关闭后恢复到原触发控件。
function key(event: KeyboardEvent) {
  if (event.key === 'Escape') emit('close')
  if (event.key !== 'Tab') return
  const items = panel.value?.querySelectorAll<HTMLElement>(
    'button:not(:disabled), input:not(:disabled), textarea:not(:disabled), select:not(:disabled), a[href], [tabindex="0"]',
  )
  if (!items?.length) return
  const first = items[0]!
  const last = items[items.length - 1]!
  if (event.shiftKey && document.activeElement === first) {
    last.focus()
    event.preventDefault()
  } else if (!event.shiftKey && document.activeElement === last) {
    first.focus()
    event.preventDefault()
  }
}
onMounted(async () => {
  document.addEventListener('keydown', key)
  await nextTick()
  panel.value?.querySelector<HTMLElement>('input, textarea, button')?.focus()
})
onUnmounted(() => {
  document.removeEventListener('keydown', key)
  previous?.focus()
})
</script>
<template>
  <Teleport to="body">
    <div class="modal-backdrop" @mousedown.self="emit('close')">
      <section
        ref="panel"
        class="modal"
        :class="{ wide }"
        role="dialog"
        aria-modal="true"
        aria-labelledby="dialog-title"
      >
        <header class="modal-header">
          <h2 id="dialog-title">{{ title }}</h2>
          <button class="icon-button" aria-label="关闭弹窗" @click="emit('close')">
            <AppIcon name="X" />
          </button>
        </header>
        <slot />
      </section>
    </div>
  </Teleport>
</template>

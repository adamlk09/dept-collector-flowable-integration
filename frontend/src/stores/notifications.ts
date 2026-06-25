import { ref } from 'vue'
import { defineStore } from 'pinia'

interface Toast { id: number; message: string; tone: 'success' | 'error' | 'info' }

export const useNotificationsStore = defineStore('notifications', () => {
  const items = ref<Toast[]>([])
  function push(message: string, tone: Toast['tone'] = 'success') {
    const toast = { id:Date.now(), message, tone }
    items.value.push(toast)
    window.setTimeout(() => remove(toast.id), 3500)
  }
  function remove(id: number) { items.value = items.value.filter((item) => item.id !== id) }
  return { items, push, remove }
})

import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/services/api'
import type { CollectionCase } from '@/types/domain'

export const useCasesStore = defineStore('cases', () => {
  const items = ref<CollectionCase[]>([])
  const loading = ref(false)
  const query = ref('')
  const status = ref('Tous')

  const filtered = computed(() => items.value.filter((item) => {
    const matchesQuery = `${item.id} ${item.customerName} ${item.customerCode} ${item.segment}`.toLowerCase().includes(query.value.toLowerCase())
    return matchesQuery && (status.value === 'Tous' || item.status === status.value)
  }))

  async function load() {
    if (items.value.length) return
    loading.value = true
    try { items.value = await api.getCases() } finally { loading.value = false }
  }

  async function refresh() {
    loading.value = true
    try { items.value = await api.getCases() } finally { loading.value = false }
  }

  return { items, filtered, loading, query, status, load, refresh }
})

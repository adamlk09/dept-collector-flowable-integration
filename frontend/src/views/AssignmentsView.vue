<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '@/services/api'
import type { Agent } from '@/types/domain'
import { useCasesStore } from '@/stores/cases'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
const agents = ref<Agent[]>([])
const running = ref(false)
const casesStore = useCasesStore()
const notifications = useNotificationsStore()
const session = useSessionStore()
onMounted(async () => { agents.value = await api.getAgents() })
async function executeAssignments() {
  running.value = true
  try {
    await casesStore.refresh()
    const pending = casesStore.items.filter((item) => !item.assignee)
    for (const item of pending) {
      await api.executeDecision(item.id)
      await api.assignCase(item.id)
    }
    agents.value = await api.getAgents()
    await casesStore.refresh()
    notifications.push(`${pending.length} dossier(s) affecté(s)`)
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Affectation impossible', 'error') }
  finally { running.value = false }
}
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Moteur d’affectation</span><h1>Capacité des équipes</h1><p>Visualisez la charge utilisée par la décision d’affectation.</p></div><button v-if="session.can('assignment:execute')" class="primary-button" :disabled="running" @click="executeAssignments">{{ running ? 'Exécution…' : 'Exécuter l’affectation' }}</button></div>
  <section class="agent-grid">
    <article v-for="agent in agents" :key="agent.id" class="panel agent-card">
      <div class="agent-head"><div class="agent-avatar">{{ agent.name.split(' ').map(x => x[0]).join('') }}</div><div><h3>{{ agent.name }}</h3><span>{{ agent.team }}</span></div><i :class="{ off:!agent.available }"></i></div>
      <div class="capacity-label"><span>Charge</span><b>{{ agent.activeCases }} / {{ agent.capacity }}</b></div>
      <div class="capacity"><span :style="{ width:`${agent.activeCases / agent.capacity * 100}%` }"></span></div>
      <div class="skill-list"><span v-for="skill in agent.skills" :key="skill">{{ skill }}</span></div>
    </article>
  </section>
</template>

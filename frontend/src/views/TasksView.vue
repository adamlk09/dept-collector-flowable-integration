<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import type { WorkflowTask } from '@/types/domain'

const tasks = ref<WorkflowTask[]>([])
const filter = ref<'Ouvertes' | 'Terminées' | 'Toutes'>('Ouvertes')
const notifications = useNotificationsStore()
const session = useSessionStore()
const visible = computed(() => tasks.value.filter((task) => filter.value === 'Toutes' || (filter.value === 'Terminées' ? task.status === 'Terminée' : task.status !== 'Terminée')))
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })

async function load() { tasks.value = await api.getTasks() }
async function complete(id: string) {
  await api.completeTask(id)
  await load()
  notifications.push('Tâche Flowable terminée')
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Flowable BPMN</span><h1>Mes tâches</h1><p>Traitez les tâches humaines créées par les processus de recouvrement.</p></div><div class="filter-tabs"><button v-for="item in ['Ouvertes','Terminées','Toutes'] as const" :key="item" :class="{ active:filter === item }" @click="filter = item">{{ item }}</button></div></div>
  <section class="panel">
    <div class="task-inbox">
      <article v-for="task in visible" :key="task.id">
        <div class="task-state" :class="{ done:task.status === 'Terminée' }">{{ task.status === 'Terminée' ? '✓' : '!' }}</div>
        <div class="task-main"><span>{{ task.id }}</span><h3>{{ task.name }}</h3><p>{{ task.assignee }} · échéance {{ date.format(new Date(task.dueAt)) }}</p></div>
        <RouterLink :to="`/cases/${task.caseId}`" class="secondary-button">Ouvrir le dossier</RouterLink>
        <button v-if="task.status !== 'Terminée' && session.can('task:complete')" class="primary-button" @click="complete(task.id)">Terminer</button>
      </article>
      <div v-if="!visible.length" class="empty-state">Aucune tâche dans cette vue.</div>
    </div>
  </section>
</template>

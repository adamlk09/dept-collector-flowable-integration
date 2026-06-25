<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppModal from '@/components/ui/AppModal.vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import type { WorkflowInstance, WorkflowInstanceStatus, WorkflowTask } from '@/types/domain'

const instances = ref<WorkflowInstance[]>([])
const tasks = ref<WorkflowTask[]>([])
const query = ref('')
const status = ref<'Toutes' | WorkflowInstanceStatus>('Toutes')
const selected = ref<WorkflowInstance | null>(null)
const detailsOpen = ref(false)
const working = ref(false)
const notifications = useNotificationsStore()
const session = useSessionStore()
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })
const visible = computed(() => instances.value.filter((instance) => {
  const matchesQuery = `${instance.id} ${instance.caseId} ${instance.customerName} ${instance.currentActivity}`.toLowerCase().includes(query.value.toLowerCase())
  return matchesQuery && (status.value === 'Toutes' || instance.status === status.value)
}))
const activeTasks = computed(() => selected.value ? tasks.value.filter((task) => task.caseId === selected.value?.caseId) : [])
const incidentCount = computed(() => instances.value.filter((instance) => instance.status === 'Incident').length)

async function load() {
  ;[instances.value, tasks.value] = await Promise.all([api.getWorkflowInstances(), api.getTasks()])
}
function openDetails(instance: WorkflowInstance) {
  selected.value = instance
  detailsOpen.value = true
}
async function suspend(instance: WorkflowInstance) {
  working.value = true
  try { await api.suspendWorkflowInstance(instance.id); await load(); detailsOpen.value = false; notifications.push('Instance Flowable suspendue', 'info') }
  catch (error) { notifications.push(error instanceof Error ? error.message : 'Suspension impossible', 'error') }
  finally { working.value = false }
}
async function resume(instance: WorkflowInstance) {
  working.value = true
  try { await api.resumeWorkflowInstance(instance.id); await load(); detailsOpen.value = false; notifications.push('Instance Flowable reprise') }
  catch (error) { notifications.push(error instanceof Error ? error.message : 'Reprise impossible', 'error') }
  finally { working.value = false }
}
async function retry(instance: WorkflowInstance) {
  working.value = true
  try { await api.retryWorkflowIncident(instance.id); await load(); detailsOpen.value = false; notifications.push('Incident relancé avec succès') }
  catch (error) { notifications.push(error instanceof Error ? error.message : 'Relance impossible', 'error') }
  finally { working.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Flowable BPMN</span><h1>Supervision des processus</h1><p>Suivez les instances de recouvrement, les activités courantes et les incidents techniques.</p></div></div>
  <section class="stats-grid workflow-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="blue">⌘</div><div><span>Instances</span><strong>{{ instances.length }}</strong><small>Processus démarrés</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="green">▶</div><div><span>Actives</span><strong>{{ instances.filter(x => x.status === 'Active').length }}</strong><small>En cours d’exécution</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="red">!</div><div><span>Incidents</span><strong>{{ incidentCount }}</strong><small>Intervention requise</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">Ⅱ</div><div><span>Suspendues</span><strong>{{ instances.filter(x => x.status === 'Suspendue').length }}</strong><small>Exécution interrompue</small></div></article>
  </section>
  <section class="panel">
    <div class="filters"><label class="search-field"><span>⌕</span><input v-model="query" placeholder="Rechercher une instance, un dossier ou une activité" /></label><div class="filter-tabs"><button v-for="value in ['Toutes','Active','Incident','Suspendue','Terminée'] as const" :key="value" :class="{active:status === value}" @click="status = value">{{ value }}</button></div></div>
    <div class="table-shell"><table><thead><tr><th>Instance</th><th>Dossier / client</th><th>Définition</th><th>Activité courante</th><th>Démarrée</th><th>Statut</th><th></th></tr></thead><tbody><tr v-for="instance in visible" :key="instance.id"><td><code>{{ instance.id }}</code></td><td><RouterLink :to="`/cases/${instance.caseId}`" class="primary-cell"><b>{{ instance.customerName }}</b><span>{{ instance.caseId }}</span></RouterLink></td><td><b>{{ instance.processDefinitionKey }}</b><small class="table-subline">Version {{ instance.processDefinitionVersion }}</small></td><td><b>{{ instance.currentActivity }}</b><small v-if="instance.incident" class="incident-message">{{ instance.incident.message }}</small></td><td>{{ date.format(new Date(instance.startedAt)) }}</td><td><span class="workflow-status" :data-status="instance.status">{{ instance.status }}</span></td><td><button class="text-button" @click="openDetails(instance)">Inspecter →</button></td></tr></tbody></table></div>
    <div v-if="!visible.length" class="empty-state">Aucune instance ne correspond aux filtres.</div>
  </section>

  <AppModal :open="detailsOpen" :title="selected?.id ?? 'Instance Flowable'" :description="`${selected?.processDefinitionKey ?? ''} · business key ${selected?.businessKey ?? ''}`" @close="detailsOpen = false">
    <div v-if="selected" class="workflow-inspector">
      <div class="instance-summary"><div><span>Statut</span><b><span class="workflow-status" :data-status="selected.status">{{ selected.status }}</span></b></div><div><span>Activité courante</span><b>{{ selected.currentActivity }}</b></div><div><span>Démarrée le</span><b>{{ date.format(new Date(selected.startedAt)) }}</b></div><div><span>Dernière évolution</span><b>{{ date.format(new Date(selected.updatedAt)) }}</b></div></div>
      <div class="process-path"><div class="process-node done"><i>✓</i><b>Création</b></div><span></span><div class="process-node done"><i>✓</i><b>Décision DMN</b></div><span></span><div class="process-node current"><i>3</i><b>{{ selected.currentActivity }}</b></div><span></span><div class="process-node" :class="{done:selected.status === 'Terminée'}"><i>{{ selected.status === 'Terminée' ? '✓' : '4' }}</i><b>Résolution</b></div></div>
      <section v-if="selected.incident" class="incident-card"><header><i>!</i><div><span>Incident technique</span><b>{{ selected.incident.message }}</b></div></header><dl><div><dt>Activité</dt><dd>{{ selected.incident.activityId }}</dd></div><div><dt>Tentatives</dt><dd>{{ selected.incident.retries }}</dd></div><div><dt>Survenu le</dt><dd>{{ date.format(new Date(selected.incident.occurredAt)) }}</dd></div></dl></section>
      <section class="instance-tasks"><h3>Tâches humaines</h3><article v-for="task in activeTasks" :key="task.id"><div><b>{{ task.name }}</b><small>{{ task.assignee }} · échéance {{ date.format(new Date(task.dueAt)) }}</small></div><span class="status-badge" :data-status="task.status">{{ task.status }}</span></article><p v-if="!activeTasks.length">Aucune tâche humaine rattachée.</p></section>
      <div v-if="session.can('workflow:manage')" class="form-actions"><button v-if="selected.status === 'Active'" class="secondary-button" :disabled="working" @click="suspend(selected)">Suspendre</button><button v-if="selected.status === 'Suspendue'" class="primary-button" :disabled="working" @click="resume(selected)">Reprendre</button><button v-if="selected.status === 'Incident'" class="primary-button" :disabled="working" @click="retry(selected)">Relancer l’incident</button></div>
    </div>
  </AppModal>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/services/api'
import type { AlertSeverity, OperationalAlert } from '@/types/domain'

const alerts = ref<OperationalAlert[]>([])
const filter = ref<'Toutes' | 'Non lues' | AlertSeverity>('Toutes')
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })
const visible = computed(() => alerts.value.filter((alert) => filter.value === 'Toutes' || (filter.value === 'Non lues' ? !alert.read : alert.severity === filter.value)))
const unread = computed(() => alerts.value.filter((alert) => !alert.read).length)

async function load() {
  alerts.value = await api.getOperationalAlerts()
}
async function markRead(alert: OperationalAlert) {
  if (alert.read) return
  await api.markAlertRead(alert.id)
  alert.read = true
}
async function markAllRead() {
  await api.markAllAlertsRead(alerts.value.filter((alert) => !alert.read).map((alert) => alert.id))
  alerts.value.forEach((alert) => { alert.read = true })
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact">
    <div><span class="eyebrow">Supervision</span><h1>Notifications opérationnelles</h1><p>Centralisez les échéances Flowable, risques critiques et engagements non honorés.</p></div>
    <button v-if="unread" class="secondary-button" @click="markAllRead">Tout marquer comme lu</button>
  </div>
  <section class="stats-grid notification-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="blue">♢</div><div><span>Alertes actives</span><strong>{{ alerts.length }}</strong><small>{{ unread }} non lue(s)</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="red">!</div><div><span>Critiques</span><strong>{{ alerts.filter(x => x.severity === 'Critique').length }}</strong><small>Action immédiate requise</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">⌛</div><div><span>À surveiller</span><strong>{{ alerts.filter(x => x.severity === 'Attention').length }}</strong><small>Échéances proches</small></div></article>
  </section>
  <section class="panel">
    <div class="filters"><div class="filter-tabs"><button v-for="value in ['Toutes','Non lues','Critique','Attention','Information'] as const" :key="value" :class="{active:filter === value}" @click="filter = value">{{ value }}</button></div></div>
    <div class="alert-list">
      <RouterLink v-for="alert in visible" :key="alert.id" :to="alert.route" class="alert-row" :class="{unread:!alert.read}" @click="markRead(alert)">
        <i :data-severity="alert.severity">{{ alert.severity === 'Critique' ? '!' : alert.severity === 'Attention' ? '⌛' : 'i' }}</i>
        <div><div class="alert-meta"><span>{{ alert.category }}</span><small>{{ date.format(new Date(alert.occurredAt)) }}</small></div><h3>{{ alert.title }}</h3><p>{{ alert.description }}</p><code>{{ alert.caseId }}</code></div>
        <span v-if="!alert.read" class="unread-dot"></span><b class="alert-arrow">→</b>
      </RouterLink>
      <div v-if="!visible.length" class="empty-state">Aucune notification dans cette vue.</div>
    </div>
  </section>
</template>

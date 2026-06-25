<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useCasesStore } from '@/stores/cases'
import { useSessionStore } from '@/stores/session'
import { api } from '@/services/api'
import StatCard from '@/components/ui/StatCard.vue'
import CaseTable from '@/components/cases/CaseTable.vue'
import type { DashboardActivity, PortfolioMetrics } from '@/types/domain'

const store = useCasesStore()
const session = useSessionStore()
const metrics = ref<PortfolioMetrics | null>(null)
const activity = ref<DashboardActivity[]>([])
const currency = new Intl.NumberFormat('fr-MA', { notation:'compact', style:'currency', currency:'MAD', maximumFractionDigits:1 })
const today = new Intl.DateTimeFormat('fr-FR', { dateStyle:'full' }).format(new Date())
const priorities = computed(() => [...store.items].sort((a,b) => {
  const risk = { Critique:4, Élevé:3, Modéré:2, Faible:1 }
  return risk[b.risk] - risk[a.risk] || b.overdueDays - a.overdueDays || b.amount - a.amount
}).slice(0,4))
const activityIcon = (type: DashboardActivity['type']) => type === 'DMN' ? '◇' : type === 'PAIEMENT' ? '₿' : type === 'PROMESSE' ? '✓' : type === 'COMMUNICATION' ? '✉' : type === 'WORKFLOW' ? '!' : '•'
const relativeTime = (value: string) => {
  const minutes = Math.max(0, Math.round((Date.now() - new Date(value).getTime()) / 60000))
  if (minutes < 1) return 'À l’instant'
  if (minutes < 60) return `Il y a ${minutes} min`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `Il y a ${hours} h`
  return new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' }).format(new Date(value))
}
onMounted(async () => {
  await store.refresh()
  ;[metrics.value, activity.value] = await Promise.all([api.getPortfolioMetrics(), api.getDashboardActivity()])
})
</script>

<template>
  <div class="page-heading"><div><span class="eyebrow">{{ today }}</span><h1>Bonjour {{ session.firstName }}.</h1><p>Voici la situation consolidée du portefeuille et des traitements opérationnels.</p></div><div class="heading-actions"><RouterLink to="/reports" class="secondary-button">Voir les indicateurs</RouterLink><RouterLink to="/cases" class="primary-button">Ouvrir le portefeuille →</RouterLink></div></div>
  <section class="stats-grid dashboard-stats">
    <StatCard label="Encours à recouvrer" :value="currency.format(metrics?.totalOutstanding ?? 0)" :detail="`${metrics?.totalCases ?? 0} dossiers actifs`" tone="blue">₿</StatCard>
    <StatCard label="Dossiers critiques" :value="String(metrics?.criticalCases ?? 0)" :detail="`${metrics?.workflowIncidents ?? 0} incident(s) Flowable`" tone="red">!</StatCard>
    <StatCard label="Paiements reçus" :value="currency.format(metrics?.paymentsReceived ?? 0)" :detail="`${metrics?.recoveryRate ?? 0} % de taux de recouvrement`" tone="green">₿</StatCard>
    <StatCard label="SLA des tâches" :value="`${metrics?.slaComplianceRate ?? 100} %`" :detail="`${metrics?.overdueTasks ?? 0} tâche(s) en retard`" tone="orange">⌛</StatCard>
    <StatCard label="Non affectés" :value="String(metrics?.unassignedCases ?? 0)" detail="Moteur d’affectation disponible" tone="blue">⇄</StatCard>
  </section>
  <section class="dashboard-grid">
    <article class="panel span-2">
      <div class="panel-title"><div><span>Priorités calculées</span><h3>Dossiers à traiter</h3></div><RouterLink to="/cases">Voir tout</RouterLink></div>
      <CaseTable :items="priorities" />
    </article>
    <aside class="panel activity-panel">
      <div class="panel-title"><div><span>Temps réel</span><h3>Activité récente</h3></div><RouterLink to="/reports">Historique</RouterLink></div>
      <RouterLink v-for="item in activity.slice(0,6)" :key="item.id" :to="`/cases/${item.caseId}`" class="activity"><i :class="item.tone">{{ activityIcon(item.type) }}</i><div><b>{{ item.title }}</b><p>{{ item.description }}</p><small>{{ relativeTime(item.occurredAt) }}</small></div></RouterLink>
      <div v-if="!activity.length" class="empty-state compact-empty">Aucune activité récente.</div>
    </aside>
  </section>
</template>

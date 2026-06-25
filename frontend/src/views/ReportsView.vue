<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import type { DashboardActivity, PortfolioMetrics } from '@/types/domain'

const metrics = ref<PortfolioMetrics | null>(null)
const activity = ref<DashboardActivity[]>([])
const exporting = ref(false)
const notifications = useNotificationsStore()
const currency = new Intl.NumberFormat('fr-MA', { style:'currency', currency:'MAD', maximumFractionDigits:0 })
const maxStatusAmount = computed(() => Math.max(1, ...(metrics.value?.statusDistribution.map((item) => item.amount) ?? [1])))
const maxRiskAmount = computed(() => Math.max(1, ...(metrics.value?.riskDistribution.map((item) => item.amount) ?? [1])))
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })

async function exportCsv() {
  exporting.value = true
  try {
    const csv = await api.exportPortfolioCsv()
    const blob = new Blob(['\ufeff', csv], { type:'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `portefeuille-recouvrement-${new Date().toISOString().slice(0,10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    notifications.push('Export du portefeuille généré')
  } finally { exporting.value = false }
}
onMounted(async () => { [metrics.value, activity.value] = await Promise.all([api.getPortfolioMetrics(), api.getDashboardActivity()]) })
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Pilotage</span><h1>Indicateurs et rapports</h1><p>Analysez la performance du portefeuille, les SLA et la charge des équipes.</p></div><button class="primary-button" :disabled="exporting" @click="exportCsv">{{ exporting ? 'Génération…' : '↓ Exporter le portefeuille' }}</button></div>
  <section class="stats-grid report-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="blue">%</div><div><span>Taux de recouvrement</span><strong>{{ metrics?.recoveryRate ?? 0 }} %</strong><small>{{ currency.format(metrics?.paymentsReceived ?? 0) }} encaissés</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="green">⌛</div><div><span>Respect SLA</span><strong>{{ metrics?.slaComplianceRate ?? 100 }} %</strong><small>{{ metrics?.overdueTasks ?? 0 }} tâche(s) en retard</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">↗</div><div><span>Retard moyen</span><strong>{{ metrics?.averageOverdueDays ?? 0 }} j</strong><small>Sur {{ metrics?.totalCases ?? 0 }} dossiers</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="red">!</div><div><span>Promesses rompues</span><strong>{{ metrics?.brokenPromises ?? 0 }}</strong><small>{{ metrics?.activePromises ?? 0 }} encore actives</small></div></article>
  </section>
  <div class="report-grid">
    <section class="panel distribution-panel"><div class="panel-title"><div><span>Portefeuille</span><h3>Encours par statut</h3></div></div><div class="bar-list"><article v-for="item in metrics?.statusDistribution" :key="item.label"><header><b>{{ item.label }}</b><span>{{ item.count }} dossier(s) · {{ currency.format(item.amount) }}</span></header><div><i :style="{width:`${item.amount / maxStatusAmount * 100}%`}"></i></div></article></div></section>
    <section class="panel distribution-panel"><div class="panel-title"><div><span>Exposition</span><h3>Encours par niveau de risque</h3></div></div><div class="bar-list risk-bars"><article v-for="item in metrics?.riskDistribution" :key="item.label"><header><b>{{ item.label }}</b><span>{{ item.count }} dossier(s) · {{ currency.format(item.amount) }}</span></header><div><i :data-risk="item.label" :style="{width:`${item.amount / maxRiskAmount * 100}%`}"></i></div></article></div></section>
    <section class="panel"><div class="panel-title"><div><span>Capacité</span><h3>Charge des agents</h3></div></div><div class="performance-list"><article v-for="agent in metrics?.agentPerformance" :key="agent.name"><div><b>{{ agent.name }}</b><small>{{ agent.activeCases }} / {{ agent.capacity }} dossiers</small></div><div class="performance-meter"><i :class="{warning:agent.loadRate >= 90}" :style="{width:`${agent.loadRate}%`}"></i></div><strong>{{ agent.loadRate }} %</strong></article></div></section>
    <section class="panel"><div class="panel-title"><div><span>Journal consolidé</span><h3>Dernières opérations</h3></div></div><div class="report-activity"><RouterLink v-for="item in activity.slice(0,8)" :key="item.id" :to="`/cases/${item.caseId}`"><i :data-tone="item.tone"></i><div><b>{{ item.title }}</b><p>{{ item.description }}</p><small>{{ date.format(new Date(item.occurredAt)) }}</small></div><span>→</span></RouterLink><div v-if="!activity.length" class="empty-state compact-empty">Aucune opération enregistrée.</div></div></section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { api } from '@/services/api'
import { useSessionStore } from '@/stores/session'
import type { OperationalAlert } from '@/types/domain'

const collapsed = ref(false)
const accountOpen = ref(false)
const notificationsOpen = ref(false)
const alerts = ref<OperationalAlert[]>([])
const session = useSessionStore()
const router = useRouter()
const unreadAlerts = computed(() => alerts.value.filter((alert) => !alert.read))
const navigation = [
  { to:'/', label:'Vue d’ensemble', icon:'⌂' },
  { to:'/cases', label:'Dossiers', icon:'▦' },
  { to:'/tasks', label:'Mes tâches', icon:'✓' },
  { to:'/workflows', label:'Processus BPMN', icon:'⌘' },
  { to:'/payments', label:'Paiements', icon:'₿' },
  { to:'/reports', label:'Pilotage & rapports', icon:'▥' },
  { to:'/organization', label:'Organisation', icon:'◎' },
  { to:'/communications', label:'Communications', icon:'✉' },
  { to:'/documents', label:'Documents', icon:'▤' },
  { to:'/legal', label:'Contentieux', icon:'⚖' },
  { to:'/rules', label:'Règles DMN', icon:'◇' },
  { to:'/assignments', label:'Affectation', icon:'⇄' },
  { to:'/access', label:'Utilisateurs & rôles', icon:'♙', permission:'access:manage' },
]
async function loadAlerts() {
  alerts.value = await api.getOperationalAlerts()
}
function toggleNotifications() {
  notificationsOpen.value = !notificationsOpen.value
  accountOpen.value = false
}
function toggleAccount() {
  accountOpen.value = !accountOpen.value
  notificationsOpen.value = false
}
async function openAlert(alert: OperationalAlert) {
  await api.markAlertRead(alert.id)
  alert.read = true
  notificationsOpen.value = false
  await router.push(alert.route)
}
async function markAllRead() {
  await api.markAllAlertsRead(unreadAlerts.value.map((alert) => alert.id))
  alerts.value.forEach((alert) => { alert.read = true })
}
onMounted(async () => {
  await Promise.all([session.initialize(), loadAlerts()])
})
</script>

<template>
  <div class="app-shell" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="brand"><span class="brand-mark">C</span><div><b>Collect</b><small>Recovery workspace</small></div></div>
      <nav>
        <span class="nav-heading">Pilotage</span>
        <RouterLink v-for="item in navigation.filter(entry => !entry.permission || session.can(entry.permission))" :key="item.to" :to="item.to" class="nav-item">
          <i>{{ item.icon }}</i><span>{{ item.label }}</span>
        </RouterLink>
      </nav>
      <div class="tenant-card"><span>Tenant actif</span><b>{{ session.user.tenantName }}</b><small>{{ session.user.tenantId }}</small></div>
    </aside>
    <div class="workspace">
      <header class="app-header">
        <button class="icon-button" aria-label="Réduire le menu" @click="collapsed = !collapsed">☰</button>
        <div class="header-context"><span>Plateforme de recouvrement</span><b>Centre opérationnel</b></div>
        <div class="header-actions">
          <button class="icon-button notification-button" aria-label="Notifications" @click="toggleNotifications">♢<span v-if="unreadAlerts.length">{{ unreadAlerts.length > 9 ? '9+' : unreadAlerts.length }}</span></button>
          <button class="user user-button" @click="toggleAccount"><span>{{ session.user.initials }}</span><div><b>{{ session.user.displayName }}</b><small>{{ session.user.role }}</small></div><i>⌄</i></button>
          <div v-if="notificationsOpen" class="notification-menu">
            <header><div><span>Centre opérationnel</span><b>Notifications</b></div><button v-if="unreadAlerts.length" class="text-button" @click="markAllRead">Tout marquer comme lu</button></header>
            <button v-for="alert in alerts.slice(0,5)" :key="alert.id" class="notification-item" :class="{unread:!alert.read}" @click="openAlert(alert)"><i :data-severity="alert.severity">{{ alert.severity === 'Critique' ? '!' : alert.severity === 'Attention' ? '⌛' : 'i' }}</i><span><b>{{ alert.title }}</b><small>{{ alert.description }}</small></span></button>
            <div v-if="!alerts.length" class="notification-empty">Aucune alerte opérationnelle.</div>
            <RouterLink to="/notifications" class="notification-footer" @click="notificationsOpen = false">Afficher toutes les notifications →</RouterLink>
          </div>
          <div v-if="accountOpen" class="account-menu"><span>Compte connecté</span><b>{{ session.user.displayName }}</b><small>{{ session.user.tenantName }} · {{ session.user.tenantId }}</small><RouterLink v-if="session.can('access:manage')" to="/access" @click="accountOpen = false">Gérer les accès</RouterLink></div>
        </div>
      </header>
      <main class="page"><slot /></main>
    </div>
  </div>
</template>

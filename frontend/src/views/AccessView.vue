<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppModal from '@/components/ui/AppModal.vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import type { ApplicationRole, ApplicationUser, ApplicationUserInput } from '@/types/domain'

const users = ref<ApplicationUser[]>([])
const roles = ref<ApplicationRole[]>([])
const query = ref('')
const status = ref<'Tous' | 'Actifs' | 'Inactifs'>('Tous')
const modalOpen = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const notifications = useNotificationsStore()
const form = reactive<ApplicationUserInput>({ displayName:'', email:'', roleId:'ROLE-AGENT', team:'Amiable', active:true })
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })

const filtered = computed(() => users.value.filter((user) => {
  const matchesQuery = `${user.displayName} ${user.email} ${user.team} ${roleName(user.roleId)}`.toLowerCase().includes(query.value.toLowerCase())
  const matchesStatus = status.value === 'Tous' || (status.value === 'Actifs' ? user.active : !user.active)
  return matchesQuery && matchesStatus
}))
const activeUsers = computed(() => users.value.filter((user) => user.active).length)

function roleName(roleId: string) {
  return roles.value.find((role) => role.id === roleId)?.name ?? roleId
}
async function load() {
  ;[users.value, roles.value] = await Promise.all([api.getUsers(), api.getRoles()])
}
function openCreate() {
  editingId.value = null
  Object.assign(form, { displayName:'', email:'', roleId:'ROLE-AGENT', team:'Amiable', active:true })
  modalOpen.value = true
}
function openEdit(user: ApplicationUser) {
  editingId.value = user.id
  Object.assign(form, { displayName:user.displayName, email:user.email, roleId:user.roleId, team:user.team, active:user.active })
  modalOpen.value = true
}
async function save() {
  saving.value = true
  try {
    if (editingId.value) await api.updateUser(editingId.value, form)
    else await api.createUser(form)
    await load()
    modalOpen.value = false
    notifications.push(editingId.value ? 'Compte utilisateur mis à jour' : 'Compte utilisateur créé')
  } catch (error) {
    notifications.push(error instanceof Error ? error.message : 'Enregistrement impossible', 'error')
  } finally { saving.value = false }
}
async function toggle(user: ApplicationUser) {
  if (user.id === 'USR-001') {
    notifications.push('Le compte administrateur connecté ne peut pas être désactivé', 'error')
    return
  }
  await api.updateUser(user.id, { active:!user.active })
  await load()
  notifications.push(user.active ? 'Compte désactivé' : 'Compte réactivé')
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact">
    <div><span class="eyebrow">Sécurité</span><h1>Utilisateurs et rôles</h1><p>Administrez les comptes, leurs périmètres opérationnels et leurs habilitations.</p></div>
    <button class="primary-button" @click="openCreate">＋ Nouvel utilisateur</button>
  </div>

  <section class="stats-grid access-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="blue">♙</div><div><span>Utilisateurs</span><strong>{{ users.length }}</strong><small>{{ activeUsers }} comptes actifs</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="purple">◇</div><div><span>Rôles applicatifs</span><strong>{{ roles.length }}</strong><small>Profils d’habilitation</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="green">✓</div><div><span>Couverture</span><strong>100 %</strong><small>Comptes rattachés à un rôle</small></div></article>
  </section>

  <section class="panel access-layout">
    <div class="access-main">
      <div class="filters"><label class="search-field"><span>⌕</span><input v-model="query" placeholder="Rechercher un utilisateur, rôle ou équipe" /></label><div class="filter-tabs"><button v-for="value in ['Tous','Actifs','Inactifs'] as const" :key="value" :class="{active:status === value}" @click="status = value">{{ value }}</button></div></div>
      <div class="table-shell">
        <table>
          <thead><tr><th>Utilisateur</th><th>Rôle</th><th>Équipe</th><th>Dernière connexion</th><th>Statut</th><th></th></tr></thead>
          <tbody><tr v-for="user in filtered" :key="user.id"><td><div class="user-cell"><span>{{ user.displayName.split(' ').map(x => x[0]).join('').slice(0,2) }}</span><div><b>{{ user.displayName }}</b><small>{{ user.email }} · {{ user.id }}</small></div></div></td><td><b>{{ roleName(user.roleId) }}</b></td><td>{{ user.team }}</td><td>{{ user.lastLoginAt ? date.format(new Date(user.lastLoginAt)) : 'Jamais' }}</td><td><span class="status-badge" :data-status="user.active ? 'Actif' : 'Inactif'">{{ user.active ? 'Actif' : 'Inactif' }}</span></td><td><div class="inline-actions"><button class="text-button" @click="openEdit(user)">Modifier</button><button class="text-button danger-text" @click="toggle(user)">{{ user.active ? 'Désactiver' : 'Réactiver' }}</button></div></td></tr></tbody>
        </table>
      </div>
      <div v-if="!filtered.length" class="empty-state">Aucun utilisateur ne correspond aux filtres.</div>
    </div>
    <aside class="role-catalog"><div class="panel-title"><div><span>RBAC</span><h3>Rôles disponibles</h3></div></div><article v-for="role in roles" :key="role.id"><header><b>{{ role.name }}</b><span>{{ users.filter(x => x.roleId === role.id).length }}</span></header><p>{{ role.description }}</p><div class="permission-list"><span v-if="!role.permissions.length">Consultation seule</span><span v-for="permission in role.permissions" :key="permission">{{ permission }}</span></div></article></aside>
  </section>

  <AppModal :open="modalOpen" :title="editingId ? 'Modifier un utilisateur' : 'Créer un utilisateur'" description="Les habilitations sont héritées du rôle sélectionné." @close="modalOpen = false">
    <form class="form-grid" @submit.prevent="save">
      <label class="span-2">Nom complet<input v-model="form.displayName" required /></label>
      <label class="span-2">Adresse e-mail professionnelle<input v-model="form.email" type="email" required /></label>
      <label>Rôle<select v-model="form.roleId"><option v-for="role in roles" :key="role.id" :value="role.id">{{ role.name }}</option></select></label>
      <label>Équipe<input v-model="form.team" required /></label>
      <div class="role-preview span-2"><span>Habilitations accordées</span><p>{{ roles.find(x => x.id === form.roleId)?.description }}</p><div class="permission-list"><span v-for="permission in roles.find(x => x.id === form.roleId)?.permissions" :key="permission">{{ permission }}</span><span v-if="!roles.find(x => x.id === form.roleId)?.permissions.length">Consultation seule</span></div></div>
      <label class="toggle-field span-2"><input v-model="form.active" type="checkbox" /><span>Compte actif et autorisé à se connecter</span></label>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="modalOpen = false">Annuler</button><button class="primary-button" :disabled="saving">{{ saving ? 'Enregistrement…' : 'Enregistrer le compte' }}</button></div>
    </form>
  </AppModal>
</template>

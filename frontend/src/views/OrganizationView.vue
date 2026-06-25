<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import AppModal from '@/components/ui/AppModal.vue'
import type { Agent, AgentInput } from '@/types/domain'

const agents = ref<Agent[]>([])
const query = ref('')
const team = ref('Toutes')
const modalOpen = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const notifications = useNotificationsStore()
const session = useSessionStore()
const form = reactive<AgentInput>({ name:'', team:'Amiable', capacity:20, skills:[], available:true })
const skillText = ref('')
const teams = computed(() => ['Toutes', ...new Set(agents.value.map((agent) => agent.team))])
const filtered = computed(() => agents.value.filter((agent) => {
  const matchesQuery = `${agent.name} ${agent.team} ${agent.skills.join(' ')}`.toLowerCase().includes(query.value.toLowerCase())
  return matchesQuery && (team.value === 'Toutes' || agent.team === team.value)
}))
const totalCapacity = computed(() => agents.value.reduce((sum,item) => sum + item.capacity, 0))
const totalLoad = computed(() => agents.value.reduce((sum,item) => sum + item.activeCases, 0))

async function load() { agents.value = await api.getAgents() }
function openCreate() {
  editingId.value = null
  Object.assign(form, { name:'', team:'Amiable', capacity:20, skills:[], available:true })
  skillText.value = ''
  modalOpen.value = true
}
function openEdit(agent: Agent) {
  editingId.value = agent.id
  Object.assign(form, { name:agent.name, team:agent.team, capacity:agent.capacity, skills:[...agent.skills], available:agent.available })
  skillText.value = agent.skills.join(', ')
  modalOpen.value = true
}
async function save() {
  saving.value = true
  form.skills = skillText.value.split(',').map((item) => item.trim()).filter(Boolean)
  try {
    if (editingId.value) await api.updateAgent(editingId.value, form)
    else await api.createAgent(form)
    await load()
    modalOpen.value = false
    notifications.push(editingId.value ? 'Agent mis à jour' : 'Agent créé')
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Enregistrement impossible', 'error') }
  finally { saving.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Organisation</span><h1>Agents et capacités</h1><p>Administrez les ressources utilisées par le moteur d’affectation.</p></div><button v-if="session.can('organization:manage')" class="primary-button" @click="openCreate">+ Ajouter un agent</button></div>
  <section class="stats-grid organization-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="blue">◎</div><div><span>Agents</span><strong>{{ agents.length }}</strong><small>{{ agents.filter(x => x.available).length }} disponibles</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="green">▦</div><div><span>Capacité totale</span><strong>{{ totalCapacity }}</strong><small>Dossiers simultanés</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">⇄</div><div><span>Charge actuelle</span><strong>{{ totalLoad }}</strong><small>{{ totalCapacity ? Math.round(totalLoad / totalCapacity * 100) : 0 }} % utilisé</small></div></article>
  </section>
  <section class="panel">
    <div class="filters"><label class="search-field"><span>⌕</span><input v-model="query" placeholder="Rechercher un agent, une équipe ou une compétence" /></label><div class="filter-tabs"><button v-for="item in teams" :key="item" :class="{active:team === item}" @click="team = item">{{ item }}</button></div></div>
    <div class="organization-grid">
      <article v-for="agent in filtered" :key="agent.id" class="organization-card">
        <header><div class="agent-avatar">{{ agent.name.split(' ').map(x => x[0]).join('') }}</div><div><h3>{{ agent.name }}</h3><span>{{ agent.team }} · {{ agent.id }}</span></div><i :class="{off:!agent.available}"></i></header>
        <div class="capacity-label"><span>Charge</span><b>{{ agent.activeCases }} / {{ agent.capacity }}</b></div><div class="capacity"><span :style="{width:`${Math.min(100,agent.activeCases / agent.capacity * 100)}%`}"></span></div>
        <div class="skill-list"><span v-for="skill in agent.skills" :key="skill">{{ skill }}</span></div>
        <footer><span :class="['availability', {off:!agent.available}]">{{ agent.available ? 'Disponible' : 'Indisponible' }}</span><button v-if="session.can('organization:manage')" class="text-button" @click="openEdit(agent)">Modifier</button></footer>
      </article>
    </div>
    <div v-if="!filtered.length" class="empty-state">Aucun agent ne correspond aux filtres.</div>
  </section>
  <AppModal :open="modalOpen" :title="editingId ? 'Modifier un agent' : 'Ajouter un agent'" description="Ces paramètres sont pris en compte lors de la prochaine affectation." @close="modalOpen = false">
    <form class="form-grid" @submit.prevent="save">
      <label class="span-2">Nom complet<input v-model="form.name" required /></label>
      <label>Équipe<select v-model="form.team"><option>Amiable</option><option>Négociation</option><option>Senior</option><option>Juridique</option></select></label>
      <label>Capacité maximale<input v-model.number="form.capacity" type="number" min="1" required /></label>
      <label class="span-2">Compétences séparées par des virgules<input v-model="skillText" placeholder="Négociation, PME, Garanties" /></label>
      <label class="toggle-field span-2"><input v-model="form.available" type="checkbox" /><span>Agent disponible pour de nouvelles affectations</span></label>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="modalOpen = false">Annuler</button><button class="primary-button" :disabled="saving">{{ saving ? 'Enregistrement…' : 'Enregistrer' }}</button></div>
    </form>
  </AppModal>
</template>

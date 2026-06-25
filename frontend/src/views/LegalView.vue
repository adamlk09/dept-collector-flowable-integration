<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppModal from '@/components/ui/AppModal.vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import type { CollectionCase, LegalEventType, LegalProcedure, LegalProcedureStatus } from '@/types/domain'

const procedures = ref<LegalProcedure[]>([])
const cases = ref<CollectionCase[]>([])
const query = ref('')
const status = ref<'Toutes' | LegalProcedureStatus>('Toutes')
const selected = ref<LegalProcedure | null>(null)
const createOpen = ref(false)
const eventOpen = ref(false)
const detailsOpen = ref(false)
const saving = ref(false)
const session = useSessionStore()
const notifications = useNotificationsStore()
const currency = new Intl.NumberFormat('fr-MA', { style:'currency', currency:'MAD', maximumFractionDigits:0 })
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })
const form = reactive({ caseId:'', jurisdiction:'Tribunal de commerce de Casablanca', courtReference:'', lawyer:'', claimAmount:0, legalFees:0 })
const eventForm = reactive({ type:'Audience' as LegalEventType, title:'', scheduledAt:'', notes:'' })
const updateForm = reactive({ status:'Préparation' as LegalProcedureStatus, legalFees:0 })
const legalCases = computed(() => cases.value.filter((item) => item.status === 'Juridique' && !procedures.value.some((procedure) => procedure.caseId === item.id && procedure.status !== 'Clôturée')))
const visible = computed(() => procedures.value.filter((item) => `${item.customerName} ${item.caseId} ${item.courtReference} ${item.lawyer} ${item.jurisdiction}`.toLowerCase().includes(query.value.toLowerCase()) && (status.value === 'Toutes' || item.status === status.value)))
const totalExposure = computed(() => procedures.value.filter((item) => item.status !== 'Clôturée').reduce((sum,item) => sum + item.claimAmount,0))

async function load() { [procedures.value, cases.value] = await Promise.all([api.getLegalProcedures(), api.getCases()]) }
function openCreate() {
  const candidate = legalCases.value[0]
  Object.assign(form, { caseId:candidate?.id ?? '', jurisdiction:'Tribunal de commerce de Casablanca', courtReference:'', lawyer:'', claimAmount:candidate?.amount ?? 0, legalFees:0 })
  createOpen.value = true
}
function chooseCase() { form.claimAmount = cases.value.find((item) => item.id === form.caseId)?.amount ?? 0 }
async function createProcedure() {
  saving.value = true
  try { await api.createLegalProcedure({ ...form, openedBy:session.user.displayName }); await load(); createOpen.value = false; notifications.push('Procédure juridique ouverte') }
  catch (error) { notifications.push(error instanceof Error ? error.message : 'Ouverture impossible','error') }
  finally { saving.value = false }
}
function inspect(procedure: LegalProcedure) {
  selected.value = procedure
  Object.assign(updateForm, { status:procedure.status, legalFees:procedure.legalFees })
  detailsOpen.value = true
}
function openEvent(procedure: LegalProcedure) {
  selected.value = procedure
  const due = new Date(Date.now() + 14 * 86400000)
  Object.assign(eventForm, { type:'Audience', title:'', scheduledAt:due.toISOString().slice(0,16), notes:'' })
  eventOpen.value = true
}
async function addEvent() {
  if (!selected.value) return
  saving.value = true
  try { await api.addLegalEvent(selected.value.id, { ...eventForm, scheduledAt:new Date(eventForm.scheduledAt).toISOString(), createdBy:session.user.displayName }); await load(); eventOpen.value = false; notifications.push('Événement juridique planifié') }
  catch (error) { notifications.push(error instanceof Error ? error.message : 'Planification impossible','error') }
  finally { saving.value = false }
}
async function updateProcedure() {
  if (!selected.value) return
  saving.value = true
  try { await api.updateLegalProcedure(selected.value.id, { ...updateForm, actor:session.user.displayName }); await load(); detailsOpen.value = false; notifications.push('Procédure mise à jour') }
  catch (error) { notifications.push(error instanceof Error ? error.message : 'Mise à jour impossible','error') }
  finally { saving.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Contentieux</span><h1>Procédures juridiques</h1><p>Suivez les dépôts, audiences, jugements, frais et échéances judiciaires.</p></div><button v-if="session.can('legal:manage')" class="primary-button" :disabled="!legalCases.length" @click="openCreate">＋ Ouvrir une procédure</button></div>
  <section class="stats-grid legal-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="red">⚖</div><div><span>Procédures actives</span><strong>{{ procedures.filter(x => x.status !== 'Clôturée').length }}</strong><small>{{ procedures.length }} au total</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">⌛</div><div><span>Audiences à venir</span><strong>{{ procedures.filter(x => x.nextHearingAt && new Date(x.nextHearingAt) > new Date()).length }}</strong><small>Échéances judiciaires</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="blue">₿</div><div><span>Exposition contentieuse</span><strong>{{ currency.format(totalExposure) }}</strong><small>Capital réclamé</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="purple">+</div><div><span>Frais engagés</span><strong>{{ currency.format(procedures.reduce((sum,x) => sum + x.legalFees,0)) }}</strong><small>Honoraires et débours</small></div></article>
  </section>
  <section class="panel">
    <div class="filters"><label class="search-field"><span>⌕</span><input v-model="query" placeholder="Rechercher un dossier, tribunal, avocat ou référence" /></label><div class="filter-tabs"><button v-for="value in ['Toutes','Préparation','Déposée','Audience planifiée','Jugement rendu','Exécution','Clôturée'] as const" :key="value" :class="{active:status === value}" @click="status = value">{{ value }}</button></div></div>
    <div class="legal-grid"><article v-for="procedure in visible" :key="procedure.id" class="legal-card"><header><div><code>{{ procedure.courtReference }}</code><h3>{{ procedure.customerName }}</h3><RouterLink :to="`/cases/${procedure.caseId}`">{{ procedure.caseId }}</RouterLink></div><span class="legal-status" :data-status="procedure.status">{{ procedure.status }}</span></header><dl><div><dt>Juridiction</dt><dd>{{ procedure.jurisdiction }}</dd></div><div><dt>Avocat</dt><dd>{{ procedure.lawyer }}</dd></div><div><dt>Montant réclamé</dt><dd>{{ currency.format(procedure.claimAmount) }}</dd></div><div><dt>Prochaine audience</dt><dd>{{ procedure.nextHearingAt ? date.format(new Date(procedure.nextHearingAt)) : 'Non planifiée' }}</dd></div></dl><div class="legal-events-mini"><span v-for="event in procedure.events.slice(-3)" :key="event.id" :class="{done:event.completed}"><i>{{ event.completed ? '✓' : '•' }}</i>{{ event.title }}</span></div><footer><button class="secondary-button" @click="inspect(procedure)">Inspecter</button><button v-if="session.can('legal:manage')" class="text-button" @click="openEvent(procedure)">＋ Événement</button></footer></article></div>
    <div v-if="!visible.length" class="empty-state">Aucune procédure ne correspond aux filtres.</div>
  </section>

  <AppModal :open="createOpen" title="Ouvrir une procédure" description="La procédure démarre le sous-processus contentieux dans Flowable." @close="createOpen = false"><form class="form-grid" @submit.prevent="createProcedure"><label class="span-2">Dossier<select v-model="form.caseId" required @change="chooseCase"><option v-for="item in legalCases" :key="item.id" :value="item.id">{{ item.id }} · {{ item.customerName }}</option></select></label><label class="span-2">Juridiction<input v-model="form.jurisdiction" required /></label><label>Référence tribunal<input v-model="form.courtReference" required /></label><label>Avocat / cabinet<input v-model="form.lawyer" required /></label><label>Montant réclamé<input v-model.number="form.claimAmount" type="number" min="1" required /></label><label>Frais initiaux<input v-model.number="form.legalFees" type="number" min="0" /></label><div class="form-actions span-2"><button type="button" class="secondary-button" @click="createOpen = false">Annuler</button><button class="primary-button" :disabled="saving">Ouvrir la procédure</button></div></form></AppModal>
  <AppModal :open="eventOpen" title="Planifier un événement juridique" :description="selected?.customerName" @close="eventOpen = false"><form class="form-grid" @submit.prevent="addEvent"><label>Type<select v-model="eventForm.type"><option>Dépôt</option><option>Audience</option><option>Jugement</option><option>Signification</option><option>Exécution</option><option>Note</option></select></label><label>Date et heure<input v-model="eventForm.scheduledAt" type="datetime-local" required /></label><label class="span-2">Intitulé<input v-model="eventForm.title" required /></label><label class="span-2">Notes<textarea v-model="eventForm.notes" rows="4" /></label><div class="form-actions span-2"><button type="button" class="secondary-button" @click="eventOpen = false">Annuler</button><button class="primary-button" :disabled="saving">Planifier</button></div></form></AppModal>
  <AppModal :open="detailsOpen" title="Mettre à jour la procédure" :description="selected?.courtReference" @close="detailsOpen = false"><form class="form-grid" @submit.prevent="updateProcedure"><label class="span-2">Statut<select v-model="updateForm.status"><option>Préparation</option><option>Déposée</option><option>Audience planifiée</option><option>Jugement rendu</option><option>Exécution</option><option>Clôturée</option></select></label><label class="span-2">Frais cumulés (MAD)<input v-model.number="updateForm.legalFees" type="number" min="0" /></label><div class="legal-timeline span-2"><article v-for="event in selected?.events" :key="event.id"><i>{{ event.completed ? '✓' : '•' }}</i><div><b>{{ event.title }}</b><small>{{ event.type }} · {{ date.format(new Date(event.scheduledAt)) }}</small></div></article></div><div class="form-actions span-2"><button type="button" class="secondary-button" @click="detailsOpen = false">Annuler</button><button class="primary-button" :disabled="saving">Enregistrer</button></div></form></AppModal>
</template>

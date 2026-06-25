<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppModal from '@/components/ui/AppModal.vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import type { DmnRule, DmnRuleInput } from '@/types/domain'

const rules = ref<DmnRule[]>([])
const simulatorOpen = ref(false)
const editorOpen = ref(false)
const selectedRule = ref<DmnRule | null>(null)
const editingId = ref<string | null>(null)
const saving = ref(false)
const result = ref('')
const reasons = ref<string[]>([])
const input = reactive<DmnRuleInput>({ amount:100000, overdueDays:75, probabilityOfDefault:65, guaranteeCoverage:40 })
const form = reactive({
  name:'',
  key:'',
  description:'',
  hit:'FIRST' as DmnRule['hit'],
  thresholds:{ legalOverdueDays:90, legalProbabilityOfDefault:80, negotiationAmount:50000, negotiationProbabilityOfDefault:60, lowGuaranteeCoverage:30 },
  outputs:{ legal:'RECOUVREMENT_JURIDIQUE', negotiation:'NEGOCIATION_RENFORCEE', amicable:'RELANCE_AMIABLE' },
})
const notifications = useNotificationsStore()
const session = useSessionStore()
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })
const activeCount = computed(() => rules.value.filter((rule) => rule.status === 'Active').length)
const latestVersion = computed(() => Math.max(0, ...rules.value.map((rule) => rule.version)))

async function load() { rules.value = await api.getDmnRules() }
function resetForm() {
  Object.assign(form, { name:'', key:'', description:'', hit:'FIRST' })
  Object.assign(form.thresholds, { legalOverdueDays:90, legalProbabilityOfDefault:80, negotiationAmount:50000, negotiationProbabilityOfDefault:60, lowGuaranteeCoverage:30 })
  Object.assign(form.outputs, { legal:'RECOUVREMENT_JURIDIQUE', negotiation:'NEGOCIATION_RENFORCEE', amicable:'RELANCE_AMIABLE' })
}
function openCreate() {
  editingId.value = null
  resetForm()
  editorOpen.value = true
}
function openEdit(rule: DmnRule) {
  editingId.value = rule.id
  Object.assign(form, { name:rule.name, key:rule.key, description:rule.description, hit:rule.hit })
  Object.assign(form.thresholds, rule.thresholds)
  Object.assign(form.outputs, rule.outputs)
  editorOpen.value = true
}
function openSimulator(rule: DmnRule) {
  selectedRule.value = rule
  result.value = ''
  reasons.value = []
  simulatorOpen.value = true
}
async function save() {
  saving.value = true
  try {
    const payload = { name:form.name, key:form.key, description:form.description, hit:form.hit, thresholds:{...form.thresholds}, outputs:{...form.outputs} }
    if (editingId.value) await api.updateDmnRule(editingId.value, payload)
    else await api.createDmnRule(payload)
    await load()
    editorOpen.value = false
    notifications.push(editingId.value ? 'Nouvelle version enregistrée en brouillon' : 'Décision DMN créée en brouillon')
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Enregistrement impossible', 'error') }
  finally { saving.value = false }
}
async function activate(rule: DmnRule) {
  await api.activateDmnRule(rule.id)
  await load()
  notifications.push(`Version ${rule.version} activée dans Flowable`, 'info')
}
async function simulate() {
  if (!selectedRule.value) return
  const decision = await api.simulateDmnRule(selectedRule.value.key, input)
  result.value = decision.result
  reasons.value = decision.reasons
  notifications.push('Simulation DMN terminée', 'info')
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact">
    <div><span class="eyebrow">Flowable DMN</span><h1>Tables de décision</h1><p>Versionnez, testez et activez les règles consommées par les parcours métier.</p></div>
    <button v-if="session.can('rule:manage')" class="primary-button" @click="openCreate">＋ Nouvelle décision</button>
  </div>
  <section class="stats-grid dmn-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="purple">◇</div><div><span>Décisions</span><strong>{{ rules.length }}</strong><small>Référentiel Flowable</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="green">✓</div><div><span>Versions actives</span><strong>{{ activeCount }}</strong><small>Disponibles à l’exécution</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">V</div><div><span>Version maximale</span><strong>{{ latestVersion }}</strong><small>Historique fonctionnel</small></div></article>
  </section>
  <section class="rules-grid">
    <article v-for="rule in rules" :key="rule.id" class="panel rule-card">
      <div class="rule-top"><span class="dmn-icon">◇</span><span class="engine-dot" :data-status="rule.status">{{ rule.status }}</span></div>
      <h3>{{ rule.name }}</h3><code>{{ rule.key }}</code><p class="rule-description">{{ rule.description }}</p>
      <dl><div><dt>Version</dt><dd>{{ rule.version }}</dd></div><div><dt>Hit policy</dt><dd>{{ rule.hit }}</dd></div><div class="span-2"><dt>Modifiée</dt><dd>{{ date.format(new Date(rule.updatedAt)) }}</dd></div></dl>
      <div class="rule-thresholds"><span>Juridique : &gt; {{ rule.thresholds.legalOverdueDays }} j ou risque ≥ {{ rule.thresholds.legalProbabilityOfDefault }} %</span><span>Négociation : &gt; {{ rule.thresholds.negotiationAmount.toLocaleString('fr-FR') }} MAD</span></div>
      <div class="rule-actions"><button class="secondary-button" @click="openSimulator(rule)">Simuler</button><div><button v-if="session.can('rule:manage')" class="text-button" @click="openEdit(rule)">Modifier</button><button v-if="session.can('rule:manage') && rule.status === 'Brouillon'" class="text-button activate-button" @click="activate(rule)">Activer</button></div></div>
    </article>
  </section>

  <AppModal :open="simulatorOpen" :title="selectedRule?.name ?? 'Simulation'" :description="`Simulation de ${selectedRule?.key ?? ''} · version ${selectedRule?.version ?? ''}`" @close="simulatorOpen = false">
    <form class="form-grid" @submit.prevent="simulate">
      <label>Encours (MAD)<input v-model.number="input.amount" type="number" min="0" /></label><label>Jours de retard<input v-model.number="input.overdueDays" type="number" min="0" /></label><label>Probabilité de défaut (%)<input v-model.number="input.probabilityOfDefault" type="number" min="0" max="100" /></label><label>Couverture garantie (%)<input v-model.number="input.guaranteeCoverage" type="number" min="0" max="200" /></label>
      <div class="form-actions span-2"><button class="primary-button">Exécuter la décision</button></div>
    </form>
    <div v-if="result" class="simulation-result"><span>Résultat</span><strong>{{ result }}</strong><ul><li v-for="reason in reasons" :key="reason">{{ reason }}</li></ul></div>
  </AppModal>

  <AppModal :open="editorOpen" :title="editingId ? 'Créer une nouvelle version' : 'Nouvelle décision DMN'" description="Toute modification crée une version en brouillon avant activation." @close="editorOpen = false">
    <form class="form-grid dmn-form" @submit.prevent="save">
      <label>Nom fonctionnel<input v-model="form.name" required /></label><label>Clé technique<input v-model="form.key" required :disabled="Boolean(editingId)" /></label>
      <label class="span-2">Description<textarea v-model="form.description" rows="3" required /></label>
      <label>Hit policy<select v-model="form.hit"><option>FIRST</option><option>COLLECT</option></select></label>
      <div class="form-section span-2"><b>Seuils de décision</b><span>Critères exécutés dans l’ordre par la hit policy.</span></div>
      <label>Retard juridique (jours)<input v-model.number="form.thresholds.legalOverdueDays" type="number" min="1" required /></label><label>Risque juridique (%)<input v-model.number="form.thresholds.legalProbabilityOfDefault" type="number" min="0" max="100" required /></label>
      <label>Encours négociation (MAD)<input v-model.number="form.thresholds.negotiationAmount" type="number" min="0" required /></label><label>Risque négociation (%)<input v-model.number="form.thresholds.negotiationProbabilityOfDefault" type="number" min="0" max="100" required /></label>
      <label>Garantie faible (%)<input v-model.number="form.thresholds.lowGuaranteeCoverage" type="number" min="0" max="100" required /></label>
      <div class="form-section span-2"><b>Résultats DMN</b><span>Codes retournés au processus BPMN.</span></div>
      <label>Résultat juridique<input v-model="form.outputs.legal" required /></label><label>Résultat négociation<input v-model="form.outputs.negotiation" required /></label><label class="span-2">Résultat amiable<input v-model="form.outputs.amicable" required /></label>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="editorOpen = false">Annuler</button><button class="primary-button" :disabled="saving">{{ saving ? 'Enregistrement…' : 'Enregistrer le brouillon' }}</button></div>
    </form>
  </AppModal>
</template>

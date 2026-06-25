<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppModal from '@/components/ui/AppModal.vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import type { BankTransaction, CollectionCase, Payment, PaymentPlan, PaymentPromise } from '@/types/domain'

const payments = ref<Payment[]>([])
const promises = ref<PaymentPromise[]>([])
const plans = ref<PaymentPlan[]>([])
const bankTransactions = ref<BankTransaction[]>([])
const cases = ref<CollectionCase[]>([])
const tab = ref<'payments' | 'promises' | 'plans' | 'reconciliation'>('payments')
const query = ref('')
const reconciliationOpen = ref(false)
const selectedTransaction = ref<BankTransaction | null>(null)
const reconciling = ref(false)
const reconciliation = reactive({ decision:'Rapprocher' as 'Rapprocher' | 'Rejeter', caseId:'', reason:'' })
const notifications = useNotificationsStore()
const session = useSessionStore()
const currency = new Intl.NumberFormat('fr-MA', { style:'currency', currency:'MAD', maximumFractionDigits:0 })
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })
const filteredPayments = computed(() => payments.value.filter((item) => `${item.customerName} ${item.caseId} ${item.reference}`.toLowerCase().includes(query.value.toLowerCase())))
const filteredPromises = computed(() => promises.value.filter((item) => `${item.customerName} ${item.caseId} ${item.status}`.toLowerCase().includes(query.value.toLowerCase())))
const filteredPlans = computed(() => plans.value.filter((item) => `${item.customerName} ${item.caseId} ${item.status}`.toLowerCase().includes(query.value.toLowerCase())))
const filteredTransactions = computed(() => bankTransactions.value.filter((item) => `${item.bankReference} ${item.label} ${item.suggestedCaseId ?? ''} ${item.status}`.toLowerCase().includes(query.value.toLowerCase())))
const totalPaid = computed(() => payments.value.reduce((sum,item) => sum + item.amount, 0))
const activePromises = computed(() => promises.value.filter((item) => item.status === 'Active').reduce((sum,item) => sum + item.amount, 0))
async function load() {
  ;[payments.value, promises.value, plans.value, bankTransactions.value, cases.value] = await Promise.all([api.getPayments(), api.getPromises(), api.getPaymentPlans(), api.getBankTransactions(), api.getCases()])
}
function openReconciliation(transaction: BankTransaction) {
  selectedTransaction.value = transaction
  Object.assign(reconciliation, { decision:'Rapprocher', caseId:transaction.suggestedCaseId ?? '', reason:'' })
  reconciliationOpen.value = true
}
async function submitReconciliation() {
  if (!selectedTransaction.value) return
  reconciling.value = true
  try {
    await api.reconcileBankTransaction(selectedTransaction.value.id, { ...reconciliation, actor:session.user.displayName })
    await load()
    reconciliationOpen.value = false
    notifications.push(reconciliation.decision === 'Rapprocher' ? 'Transaction rapprochée et imputée' : 'Transaction rejetée', reconciliation.decision === 'Rapprocher' ? 'success' : 'info')
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Rapprochement impossible', 'error') }
  finally { reconciling.value = false }
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Suivi financier</span><h1>Paiements et promesses</h1><p>Suivez les encaissements, les rapprochements et les engagements de paiement.</p></div></div>
  <section class="stats-grid finance-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="green">₿</div><div><span>Paiements reçus</span><strong>{{ currency.format(totalPaid) }}</strong><small>{{ payments.length }} opération(s)</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="blue">✓</div><div><span>Promesses actives</span><strong>{{ currency.format(activePromises) }}</strong><small>{{ promises.filter(x => x.status === 'Active').length }} engagement(s)</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">!</div><div><span>À rapprocher</span><strong>{{ payments.filter(x => x.status === 'À rapprocher').length }}</strong><small>Référence à contrôler</small></div></article>
  </section>
  <section class="panel">
    <div class="filters"><label class="search-field"><span>⌕</span><input v-model="query" placeholder="Rechercher un client, dossier ou une référence" /></label><div class="filter-tabs"><button :class="{active:tab === 'payments'}" @click="tab = 'payments'">Paiements</button><button :class="{active:tab === 'promises'}" @click="tab = 'promises'">Promesses</button><button :class="{active:tab === 'plans'}" @click="tab = 'plans'">Échéanciers</button><button :class="{active:tab === 'reconciliation'}" @click="tab = 'reconciliation'">Rapprochement <span v-if="bankTransactions.filter(x => x.status === 'À rapprocher').length" class="tab-count">{{ bankTransactions.filter(x => x.status === 'À rapprocher').length }}</span></button></div></div>
    <div class="table-shell">
      <table v-if="tab === 'payments'"><thead><tr><th>Référence</th><th>Client / dossier</th><th>Montant</th><th>Mode</th><th>Reçu le</th><th>Statut</th></tr></thead><tbody><tr v-for="item in filteredPayments" :key="item.id"><td><code>{{ item.reference }}</code></td><td><RouterLink :to="`/cases/${item.caseId}`" class="primary-cell"><b>{{ item.customerName }}</b><span>{{ item.caseId }}</span></RouterLink></td><td><b>{{ currency.format(item.amount) }}</b></td><td>{{ item.method }}</td><td>{{ date.format(new Date(item.receivedAt)) }}</td><td><span class="status-badge" :data-status="item.status">{{ item.status }}</span></td></tr></tbody></table>
      <table v-else-if="tab === 'promises'"><thead><tr><th>Client / dossier</th><th>Montant promis</th><th>Échéance</th><th>Créée le</th><th>Statut</th></tr></thead><tbody><tr v-for="item in filteredPromises" :key="item.id"><td><RouterLink :to="`/cases/${item.caseId}`" class="primary-cell"><b>{{ item.customerName }}</b><span>{{ item.caseId }}</span></RouterLink></td><td><b>{{ currency.format(item.amount) }}</b></td><td>{{ date.format(new Date(item.promisedAt)) }}</td><td>{{ date.format(new Date(item.createdAt)) }}</td><td><span class="status-badge" :data-status="item.status">{{ item.status }}</span></td></tr></tbody></table>
      <table v-else-if="tab === 'plans'"><thead><tr><th>Client / dossier</th><th>Montant</th><th>Fréquence</th><th>Progression</th><th>Prochaine échéance</th><th>Statut</th></tr></thead><tbody><tr v-for="plan in filteredPlans" :key="plan.id"><td><RouterLink :to="`/cases/${plan.caseId}`" class="primary-cell"><b>{{ plan.customerName }}</b><span>{{ plan.caseId }} · {{ plan.id }}</span></RouterLink></td><td><b>{{ currency.format(plan.totalAmount) }}</b></td><td>{{ plan.frequency }}</td><td><div class="plan-progress"><span><i :style="{width:`${plan.installments.filter(x => x.status === 'Payée').length / plan.installments.length * 100}%`}"></i></span><small>{{ plan.installments.filter(x => x.status === 'Payée').length }} / {{ plan.installments.length }}</small></div></td><td>{{ plan.installments.find(x => x.status !== 'Payée') ? date.format(new Date(plan.installments.find(x => x.status !== 'Payée')!.dueAt)) : '—' }}</td><td><span class="plan-status" :data-status="plan.status">{{ plan.status }}</span></td></tr></tbody></table>
      <table v-else><thead><tr><th>Référence bancaire</th><th>Libellé</th><th>Montant</th><th>Date de valeur</th><th>Suggestion</th><th>Confiance</th><th>Statut</th><th></th></tr></thead><tbody><tr v-for="transaction in filteredTransactions" :key="transaction.id"><td><code>{{ transaction.bankReference }}</code></td><td><b>{{ transaction.label }}</b><small class="table-subline">{{ transaction.id }}</small></td><td><b>{{ currency.format(transaction.amount) }}</b></td><td>{{ date.format(new Date(transaction.valueDate)) }}</td><td><RouterLink v-if="transaction.suggestedCaseId" :to="`/cases/${transaction.suggestedCaseId}`" class="primary-cell"><b>{{ cases.find(x => x.id === transaction.suggestedCaseId)?.customerName }}</b><span>{{ transaction.suggestedCaseId }}</span></RouterLink><span v-else>—</span></td><td><span class="confidence-badge" :data-level="transaction.confidence >= 90 ? 'high' : 'medium'">{{ transaction.confidence }} %</span></td><td><span class="bank-status" :data-status="transaction.status">{{ transaction.status }}</span><small v-if="transaction.rejectionReason" class="rejection-reason">{{ transaction.rejectionReason }}</small></td><td><button v-if="transaction.status === 'À rapprocher' && session.can('payment:reconcile')" class="primary-button compact-button" @click="openReconciliation(transaction)">Traiter</button><span v-else class="done-label">{{ transaction.reconciledBy ?? 'Traité' }}</span></td></tr></tbody></table>
    </div>
    <div v-if="(tab === 'payments' && !filteredPayments.length) || (tab === 'promises' && !filteredPromises.length) || (tab === 'plans' && !filteredPlans.length) || (tab === 'reconciliation' && !filteredTransactions.length)" class="empty-state">Aucune opération dans cette vue.</div>
  </section>
  <AppModal :open="reconciliationOpen" title="Rapprocher la transaction" :description="selectedTransaction?.bankReference" @close="reconciliationOpen = false">
    <form class="form-grid" @submit.prevent="submitReconciliation">
      <div class="bank-summary span-2"><span>Montant reçu</span><b>{{ currency.format(selectedTransaction?.amount ?? 0) }}</b><small>{{ selectedTransaction?.label }}</small></div>
      <label class="span-2">Décision<select v-model="reconciliation.decision"><option>Rapprocher</option><option>Rejeter</option></select></label>
      <label v-if="reconciliation.decision === 'Rapprocher'" class="span-2">Dossier d’imputation<select v-model="reconciliation.caseId" required><option value="">Sélectionner un dossier</option><option v-for="item in cases" :key="item.id" :value="item.id">{{ item.id }} · {{ item.customerName }} · {{ currency.format(item.amount) }}</option></select></label>
      <label v-else class="span-2">Motif du rejet<textarea v-model="reconciliation.reason" rows="4" required /></label>
      <div v-if="reconciliation.decision === 'Rapprocher' && reconciliation.caseId" class="reconciliation-impact span-2"><span>Impact après validation</span><b>Nouvel encours : {{ currency.format(Math.max(0,(cases.find(x => x.id === reconciliation.caseId)?.amount ?? 0) - (selectedTransaction?.amount ?? 0))) }}</b><small>L’encaissement sera également imputé à la première échéance ouverte.</small></div>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="reconciliationOpen = false">Annuler</button><button class="primary-button" :disabled="reconciling">{{ reconciling ? 'Traitement…' : 'Confirmer la décision' }}</button></div>
    </form>
  </AppModal>
</template>

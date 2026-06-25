<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCasesStore } from '@/stores/cases'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import { api } from '@/services/api'
import CaseTable from '@/components/cases/CaseTable.vue'
import AppModal from '@/components/ui/AppModal.vue'

const store = useCasesStore()
const notifications = useNotificationsStore()
const session = useSessionStore()
const router = useRouter()
const creating = ref(false)
const saving = ref(false)
const form = reactive({ customerName:'', customerCode:'', contractId:'', amount:0, overdueDays:0, phone:'', email:'', probabilityOfDefault:0, guaranteeCoverage:0 })
onMounted(store.load)
const statuses = ['Tous','À traiter','En cours','Promesse','Juridique']
async function createCase() {
  saving.value = true
  try {
    const item = await api.createCase(form)
    await store.refresh()
    notifications.push(`Dossier ${item.id} créé`)
    creating.value = false
    await router.push(`/cases/${item.id}`)
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Création impossible', 'error') }
  finally { saving.value = false }
}
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Portefeuille</span><h1>Dossiers de recouvrement</h1><p>Recherchez, priorisez et ouvrez un dossier pour consulter la vue à 360°.</p></div><button v-if="session.can('case:create')" class="primary-button" @click="creating = true">+ Nouveau dossier</button></div>
  <section class="panel">
    <div class="filters"><label class="search-field"><span>⌕</span><input v-model="store.query" placeholder="Rechercher un client, un dossier ou un segment" /></label><div class="filter-tabs"><button v-for="item in statuses" :key="item" :class="{ active:store.status === item }" @click="store.status = item">{{ item }}</button></div></div>
    <CaseTable :items="store.filtered" />
    <div v-if="!store.filtered.length" class="empty-state">Aucun dossier ne correspond aux filtres.</div>
  </section>
  <AppModal :open="creating" title="Créer un dossier" description="Les champs de risque alimenteront la décision Flowable DMN." @close="creating = false">
    <form class="form-grid" @submit.prevent="createCase">
      <label class="span-2">Nom du client<input v-model="form.customerName" required /></label>
      <label>Code client<input v-model="form.customerCode" required /></label>
      <label>Identifiant contrat<input v-model="form.contractId" required /></label>
      <label>Encours (MAD)<input v-model.number="form.amount" min="0" type="number" required /></label>
      <label>Jours de retard<input v-model.number="form.overdueDays" min="0" type="number" required /></label>
      <label>Téléphone<input v-model="form.phone" required /></label>
      <label>E-mail<input v-model="form.email" type="email" required /></label>
      <label>Probabilité de défaut (%)<input v-model.number="form.probabilityOfDefault" min="0" max="100" type="number" required /></label>
      <label>Couverture garantie (%)<input v-model.number="form.guaranteeCoverage" min="0" max="200" type="number" required /></label>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="creating = false">Annuler</button><button class="primary-button" :disabled="saving">{{ saving ? 'Création…' : 'Créer le dossier' }}</button></div>
    </form>
  </AppModal>
</template>

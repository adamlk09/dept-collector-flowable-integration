<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AppModal from '@/components/ui/AppModal.vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import type { CaseDocument, CollectionCase, DocumentCategory, DocumentStatus } from '@/types/domain'

const documents = ref<CaseDocument[]>([])
const cases = ref<CollectionCase[]>([])
const query = ref('')
const status = ref<'Tous' | DocumentStatus>('Tous')
const uploadOpen = ref(false)
const reviewOpen = ref(false)
const selected = ref<CaseDocument | null>(null)
const saving = ref(false)
const file = ref<File | null>(null)
const form = reactive({ caseId:'', category:'Contrat' as DocumentCategory })
const review = reactive({ status:'Validé' as 'Validé' | 'Rejeté', rejectionReason:'' })
const categories: DocumentCategory[] = ['Contrat','Mise en demeure','Justificatif de paiement','Garantie','Pièce juridique','Autre']
const notifications = useNotificationsStore()
const session = useSessionStore()
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })
const visible = computed(() => documents.value.filter((item) => `${item.name} ${item.customerName} ${item.caseId} ${item.category}`.toLowerCase().includes(query.value.toLowerCase()) && (status.value === 'Tous' || item.status === status.value)))
const totalSize = computed(() => documents.value.reduce((sum,item) => sum + item.size, 0))
const formatSize = (size: number) => size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} Mo` : `${Math.round(size / 1024)} Ko`

async function load() { [documents.value, cases.value] = await Promise.all([api.getDocuments(), api.getCases()]) }
function openUpload() {
  form.caseId = cases.value[0]?.id ?? ''
  form.category = 'Contrat'
  file.value = null
  uploadOpen.value = true
}
function selectFile(event: Event) { file.value = (event.target as HTMLInputElement).files?.[0] ?? null }
function fileDataUrl(value: File) {
  if (value.size > 750 * 1024) return Promise.resolve(undefined)
  return new Promise<string | undefined>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : undefined)
    reader.onerror = () => reject(new Error('Lecture du fichier impossible'))
    reader.readAsDataURL(value)
  })
}
async function upload() {
  if (!file.value) return
  saving.value = true
  try {
    const dataUrl = await fileDataUrl(file.value)
    await api.addDocument({ caseId:form.caseId, name:file.value.name, category:form.category, mimeType:file.value.type || 'application/octet-stream', size:file.value.size, uploadedBy:session.user.displayName, dataUrl })
    await load()
    uploadOpen.value = false
    notifications.push('Document ajouté et envoyé au contrôle')
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Ajout impossible', 'error') }
  finally { saving.value = false }
}
function openReview(document: CaseDocument) {
  selected.value = document
  Object.assign(review, { status:'Validé', rejectionReason:'' })
  reviewOpen.value = true
}
async function submitReview() {
  if (!selected.value) return
  saving.value = true
  try {
    await api.reviewDocument(selected.value.id, { status:review.status, actor:session.user.displayName, rejectionReason:review.rejectionReason })
    await load()
    reviewOpen.value = false
    notifications.push(review.status === 'Validé' ? 'Document validé' : 'Document rejeté', review.status === 'Validé' ? 'success' : 'info')
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Contrôle impossible', 'error') }
  finally { saving.value = false }
}
function preview(document: CaseDocument) {
  if (document.dataUrl) window.open(document.dataUrl, '_blank', 'noopener')
  else notifications.push('Le contenu sera disponible après branchement au stockage documentaire', 'info')
}
onMounted(load)
</script>

<template>
  <div class="page-heading compact"><div><span class="eyebrow">Gestion documentaire</span><h1>Documents des dossiers</h1><p>Centralisez les pièces, leurs versions et leur statut de contrôle.</p></div><button v-if="session.can('document:manage')" class="primary-button" @click="openUpload">＋ Ajouter un document</button></div>
  <section class="stats-grid document-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="blue">▤</div><div><span>Documents</span><strong>{{ documents.length }}</strong><small>{{ formatSize(totalSize) }} référencés</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">⌛</div><div><span>À contrôler</span><strong>{{ documents.filter(x => x.status === 'À contrôler').length }}</strong><small>Validation requise</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="green">✓</div><div><span>Validés</span><strong>{{ documents.filter(x => x.status === 'Validé').length }}</strong><small>Pièces conformes</small></div></article>
  </section>
  <section class="panel">
    <div class="filters"><label class="search-field"><span>⌕</span><input v-model="query" placeholder="Rechercher un document, dossier, client ou catégorie" /></label><div class="filter-tabs"><button v-for="value in ['Tous','À contrôler','Validé','Rejeté'] as const" :key="value" :class="{active:status === value}" @click="status = value">{{ value }}</button></div></div>
    <div class="table-shell"><table><thead><tr><th>Document</th><th>Dossier / client</th><th>Catégorie</th><th>Version</th><th>Taille</th><th>Ajouté par</th><th>Statut</th><th></th></tr></thead><tbody><tr v-for="document in visible" :key="document.id"><td><button class="document-name" @click="preview(document)"><i>{{ document.mimeType.includes('pdf') ? 'PDF' : 'DOC' }}</i><span><b>{{ document.name }}</b><small>{{ document.checksum }}</small></span></button></td><td><RouterLink :to="`/cases/${document.caseId}`" class="primary-cell"><b>{{ document.customerName }}</b><span>{{ document.caseId }}</span></RouterLink></td><td>{{ document.category }}</td><td>v{{ document.version }}</td><td>{{ formatSize(document.size) }}</td><td>{{ document.uploadedBy }}<small class="table-subline">{{ date.format(new Date(document.uploadedAt)) }}</small></td><td><span class="document-status" :data-status="document.status">{{ document.status }}</span><small v-if="document.rejectionReason" class="rejection-reason">{{ document.rejectionReason }}</small></td><td><button v-if="session.can('document:manage') && document.status === 'À contrôler'" class="text-button" @click="openReview(document)">Contrôler</button><button v-else class="text-button" @click="preview(document)">Consulter</button></td></tr></tbody></table></div>
    <div v-if="!visible.length" class="empty-state">Aucun document ne correspond aux filtres.</div>
  </section>

  <AppModal :open="uploadOpen" title="Ajouter un document" description="Les pièces de moins de 750 Ko restent prévisualisables dans la base locale." @close="uploadOpen = false">
    <form class="form-grid" @submit.prevent="upload">
      <label class="span-2">Dossier<select v-model="form.caseId" required><option v-for="item in cases" :key="item.id" :value="item.id">{{ item.id }} · {{ item.customerName }}</option></select></label>
      <label class="span-2">Catégorie<select v-model="form.category"><option v-for="category in categories" :key="category">{{ category }}</option></select></label>
      <label class="file-drop span-2"><input type="file" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg" required @change="selectFile" /><i>⇧</i><b>{{ file?.name ?? 'Sélectionner une pièce' }}</b><span>{{ file ? formatSize(file.size) : 'PDF, Word ou image · 10 Mo maximum' }}</span></label>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="uploadOpen = false">Annuler</button><button class="primary-button" :disabled="saving || !file">{{ saving ? 'Ajout…' : 'Ajouter au dossier' }}</button></div>
    </form>
  </AppModal>
  <AppModal :open="reviewOpen" title="Contrôler le document" :description="selected?.name" @close="reviewOpen = false">
    <form class="form-grid" @submit.prevent="submitReview"><label class="span-2">Décision<select v-model="review.status"><option>Validé</option><option>Rejeté</option></select></label><label v-if="review.status === 'Rejeté'" class="span-2">Motif du rejet<textarea v-model="review.rejectionReason" rows="4" required /></label><div class="document-integrity span-2"><span>Empreinte</span><code>{{ selected?.checksum }}</code><small>{{ selected?.storageKey }}</small></div><div class="form-actions span-2"><button type="button" class="secondary-button" @click="reviewOpen = false">Annuler</button><button class="primary-button" :disabled="saving">{{ saving ? 'Contrôle…' : 'Confirmer' }}</button></div></form>
  </AppModal>
</template>

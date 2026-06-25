<script setup lang="ts">
import { reactive, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '@/services/api'
import type { AuditEvent, CaseActionType, CaseDocument, CaseNote, CollectionCase, Communication, CommunicationChannel, DecisionTrace, DocumentCategory, LegalProcedure, MessageTemplate, PaymentPlan, WorkflowInstance, WorkflowTask } from '@/types/domain'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import DecisionPanel from '@/components/cases/DecisionPanel.vue'
import WorkflowPanel from '@/components/cases/WorkflowPanel.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import AppModal from '@/components/ui/AppModal.vue'

const route = useRoute()
const item = ref<CollectionCase | null>(null)
const decision = ref<DecisionTrace | null>(null)
const tasks = ref<WorkflowTask[]>([])
const workflowInstance = ref<WorkflowInstance | null>(null)
const notes = ref<CaseNote[]>([])
const audit = ref<AuditEvent[]>([])
const communications = ref<Communication[]>([])
const documents = ref<CaseDocument[]>([])
const paymentPlans = ref<PaymentPlan[]>([])
const legalProcedures = ref<LegalProcedure[]>([])
const templates = ref<MessageTemplate[]>([])
const noteOpen = ref(false)
const editOpen = ref(false)
const actionOpen = ref(false)
const messageOpen = ref(false)
const documentOpen = ref(false)
const planOpen = ref(false)
const working = ref(false)
const noteText = ref('')
const editForm = reactive({ phone:'', email:'', probabilityOfDefault:0, guaranteeCoverage:0 })
const actionForm = reactive<{ type: CaseActionType; comment: string; amount: number; promisedAt: string; paymentMethod: 'Virement' | 'Prélèvement' | 'Espèces' | 'Carte' | 'Autre'; paymentReference: string }>({ type:'CONTACT', comment:'', amount:0, promisedAt:'', paymentMethod:'Virement', paymentReference:'' })
const messageForm = reactive({ channel:'SMS' as CommunicationChannel, templateId:'', recipient:'', subject:'', content:'' })
const documentForm = reactive({ category:'Contrat' as DocumentCategory })
const documentFile = ref<File | null>(null)
const documentCategories: DocumentCategory[] = ['Contrat','Mise en demeure','Justificatif de paiement','Garantie','Pièce juridique','Autre']
const planForm = reactive({ totalAmount:0, installmentCount:3, firstDueAt:'', frequency:'Mensuelle' as PaymentPlan['frequency'] })
const notifications = useNotificationsStore()
const session = useSessionStore()
const currency = new Intl.NumberFormat('fr-MA', { style:'currency', currency:'MAD', maximumFractionDigits:0 })

onMounted(async () => {
  const id = String(route.params.id)
  const workflowPromise = api.getWorkflowInstances(id)
  ;[item.value, decision.value, tasks.value, notes.value, audit.value, communications.value, templates.value, documents.value, paymentPlans.value, legalProcedures.value] = await Promise.all([api.getCase(id), api.getDecision(id), api.getTasks(id), api.getNotes(id), api.getAudit(id), api.getCommunications(id), api.getMessageTemplates(), api.getDocuments(id), api.getPaymentPlans(id), api.getLegalProcedures(id)])
  workflowInstance.value = (await workflowPromise)[0] ?? null
  if (item.value) Object.assign(editForm, { phone:item.value.phone, email:item.value.email, probabilityOfDefault:item.value.probabilityOfDefault, guaranteeCoverage:item.value.guaranteeCoverage })
})

function openMessage() {
  if (!item.value) return
  Object.assign(messageForm, { channel:'SMS', templateId:'', recipient:item.value.phone, subject:'', content:'' })
  messageOpen.value = true
}

function openPlan() {
  if (!item.value) return
  const firstDue = new Date()
  firstDue.setDate(firstDue.getDate() + 7)
  Object.assign(planForm, { totalAmount:item.value.amount, installmentCount:3, firstDueAt:firstDue.toISOString().slice(0,10), frequency:'Mensuelle' })
  planOpen.value = true
}
async function createPlan() {
  if (!item.value) return
  const caseId = item.value.id
  working.value = true
  try {
    await api.createPaymentPlan({ caseId, ...planForm, firstDueAt:new Date(`${planForm.firstDueAt}T10:00:00`).toISOString(), createdBy:session.user.displayName })
    ;[paymentPlans.value, tasks.value, audit.value] = await Promise.all([api.getPaymentPlans(caseId), api.getTasks(caseId), api.getAudit(caseId)])
    item.value = await api.getCase(caseId)
    workflowInstance.value = (await api.getWorkflowInstances(caseId))[0] ?? null
    planOpen.value = false
    notifications.push('Échéancier créé et tâches Flowable planifiées')
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Création impossible', 'error') }
  finally { working.value = false }
}

function selectDocument(event: Event) { documentFile.value = (event.target as HTMLInputElement).files?.[0] ?? null }
function formatDocumentSize(size: number) { return size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} Mo` : `${Math.round(size / 1024)} Ko` }
async function addDocument() {
  if (!item.value || !documentFile.value) return
  working.value = true
  try {
    let dataUrl: string | undefined
    if (documentFile.value.size <= 750 * 1024) {
      dataUrl = await new Promise<string | undefined>((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = () => resolve(typeof reader.result === 'string' ? reader.result : undefined)
        reader.onerror = () => reject(new Error('Lecture du fichier impossible'))
        reader.readAsDataURL(documentFile.value as File)
      })
    }
    await api.addDocument({ caseId:item.value.id, name:documentFile.value.name, category:documentForm.category, mimeType:documentFile.value.type || 'application/octet-stream', size:documentFile.value.size, uploadedBy:session.user.displayName, dataUrl })
    documents.value = await api.getDocuments(item.value.id)
    audit.value = await api.getAudit(item.value.id)
    documentOpen.value = false
    documentFile.value = null
    notifications.push('Document ajouté au dossier')
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Ajout impossible', 'error') }
  finally { working.value = false }
}
function previewDocument(document: CaseDocument) {
  if (document.dataUrl) window.open(document.dataUrl, '_blank', 'noopener')
  else notifications.push('Le contenu sera disponible après branchement au stockage documentaire', 'info')
}
function changeMessageChannel() {
  if (!item.value) return
  Object.assign(messageForm, { templateId:'', recipient:messageForm.channel === 'SMS' ? item.value.phone : item.value.email, subject:'', content:'' })
}
function useMessageTemplate() {
  const template = templates.value.find((entry) => entry.id === messageForm.templateId)
  if (!template || !item.value) return
  const resolve = (value = '') => value.split('{{client}}').join(item.value?.customerName ?? '').split('{{caseId}}').join(item.value?.id ?? '')
  messageForm.subject = resolve(template.subject)
  messageForm.content = resolve(template.content)
}
async function sendMessage() {
  if (!item.value) return
  working.value = true
  try {
    const result = await api.sendCommunication({ caseId:item.value.id, channel:messageForm.channel, recipient:messageForm.recipient, subject:messageForm.subject || undefined, content:messageForm.content, sentBy:session.user.displayName })
    communications.value.unshift(result)
    audit.value = await api.getAudit(item.value.id)
    item.value = await api.getCase(item.value.id)
    messageOpen.value = false
    notifications.push(`${messageForm.channel} envoyé et historisé`)
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Envoi impossible', 'error') }
  finally { working.value = false }
}

async function runDecision() {
  if (!item.value) return
  const caseId = item.value.id
  working.value = true
  try { decision.value = await api.executeDecision(caseId); item.value = await api.getCase(caseId); workflowInstance.value = (await api.getWorkflowInstances(caseId))[0] ?? null; notifications.push('Décision DMN exécutée') }
  catch (error) { notifications.push(error instanceof Error ? error.message : 'Décision impossible', 'error') }
  finally { working.value = false }
}
async function assign() {
  if (!item.value) return
  const caseId = item.value.id
  working.value = true
  try { const result = await api.assignCase(caseId); item.value = await api.getCase(caseId); workflowInstance.value = (await api.getWorkflowInstances(caseId))[0] ?? null; notifications.push(`Dossier affecté à ${result.agentName}`) }
  catch (error) { notifications.push(error instanceof Error ? error.message : 'Affectation impossible', 'error') }
  finally { working.value = false }
}
async function addNote() {
  if (!item.value || !noteText.value.trim()) return
  notes.value.unshift(await api.addNote(item.value.id, noteText.value.trim(), session.user.displayName))
  noteText.value = ''; noteOpen.value = false; notifications.push('Note ajoutée')
}
async function saveCustomer() {
  if (!item.value) return
  item.value = await api.updateCase(item.value.id, editForm)
  editOpen.value = false; notifications.push('Informations mises à jour')
}
async function completeTask(taskId: string) {
  await api.completeTask(taskId)
  if (item.value) tasks.value = await api.getTasks(item.value.id)
  if (item.value) workflowInstance.value = (await api.getWorkflowInstances(item.value.id))[0] ?? null
  notifications.push('Tâche Flowable terminée')
}
async function executeAction() {
  if (!item.value) return
  working.value = true
  try {
    const result = await api.executeCaseAction(item.value.id, actionForm, session.user.displayName)
    item.value = result.case
    audit.value = await api.getAudit(item.value.id)
    tasks.value = await api.getTasks(item.value.id)
    workflowInstance.value = (await api.getWorkflowInstances(item.value.id))[0] ?? null
    legalProcedures.value = await api.getLegalProcedures(item.value.id)
    actionOpen.value = false
    Object.assign(actionForm, { type:'CONTACT', comment:'', amount:0, promisedAt:'', paymentMethod:'Virement', paymentReference:'' })
    notifications.push('Action enregistrée dans le dossier')
  } catch (error) { notifications.push(error instanceof Error ? error.message : 'Action impossible', 'error') }
  finally { working.value = false }
}
</script>

<template>
  <div v-if="item">
    <div class="detail-back"><RouterLink to="/cases">← Retour au portefeuille</RouterLink></div>
    <div class="case-hero">
      <div class="case-identity"><span>{{ item.id }}</span><h1>{{ item.customerName }}</h1><p>{{ item.customerCode }} · Contrat {{ item.contractId }}</p></div>
      <div class="case-actions"><StatusBadge :label="item.status" /><button v-if="session.can('communication:send')" class="secondary-button" @click="openMessage">Envoyer un message</button><button class="secondary-button" @click="noteOpen = true">Ajouter une note</button><button v-if="item.assignee && session.can('case:update')" class="secondary-button" @click="actionOpen = true">Enregistrer une action</button><button v-if="session.can(item.assignee ? 'decision:execute' : 'assignment:execute')" class="primary-button" :disabled="working" @click="item.assignee ? runDecision() : assign()">{{ working ? 'Traitement…' : item.assignee ? 'Exécuter la décision' : 'Affecter le dossier' }}</button></div>
    </div>
    <section class="detail-stats">
      <div><span>Encours</span><b>{{ currency.format(item.amount) }}</b></div><div><span>Retard</span><b>{{ item.overdueDays }} jours</b></div><div><span>Probabilité de défaut</span><b>{{ item.probabilityOfDefault }} %</b></div><div><span>Couverture garantie</span><b>{{ item.guaranteeCoverage }} %</b></div>
    </section>
    <div class="case-grid">
      <section class="panel customer-card">
        <div class="panel-title"><div><span>Client</span><h3>Informations principales</h3></div><button class="text-button" @click="editOpen = true">Modifier</button></div>
        <dl><div><dt>Téléphone</dt><dd>{{ item.phone }}</dd></div><div><dt>E-mail</dt><dd>{{ item.email }}</dd></div><div><dt>Segment</dt><dd>{{ item.segment }}</dd></div><div><dt>Risque</dt><dd>{{ item.risk }}</dd></div><div><dt>Affectataire</dt><dd>{{ item.assignee ?? 'Non affecté' }}</dd></div><div><dt>Prochaine action</dt><dd>{{ item.nextAction }}</dd></div></dl>
      </section>
      <DecisionPanel :decision="decision" :can-execute="session.can('decision:execute')" @execute="runDecision" />
      <WorkflowPanel class="span-2" :tasks="tasks" :instance="workflowInstance" :can-complete="session.can('task:complete')" @complete="completeTask" />
      <section v-if="item.status === 'Juridique' || legalProcedures.length" class="panel span-2">
        <div class="panel-title"><div><span>Contentieux</span><h3>Procédure juridique</h3></div><RouterLink to="/legal">Ouvrir le registre</RouterLink></div>
        <div v-if="legalProcedures.length" class="case-legal"><article v-for="procedure in legalProcedures" :key="procedure.id"><span class="legal-status" :data-status="procedure.status">{{ procedure.status }}</span><div><b>{{ procedure.courtReference }} · {{ procedure.jurisdiction }}</b><p>{{ procedure.lawyer }} · Réclamation {{ currency.format(procedure.claimAmount) }}</p><small>{{ procedure.nextHearingAt ? `Prochaine audience : ${new Date(procedure.nextHearingAt).toLocaleString('fr-FR')}` : 'Aucune audience planifiée' }}</small></div></article></div>
        <div v-else class="empty-state compact-empty">Escalade enregistrée, procédure en préparation.</div>
      </section>
      <section class="panel span-2">
        <div class="panel-title"><div><span>Négociation</span><h3>Échéanciers de règlement</h3></div><button v-if="session.can('case:update')" class="text-button" @click="openPlan">Nouvel échéancier</button></div>
        <div v-if="paymentPlans.length" class="case-plans"><article v-for="plan in paymentPlans" :key="plan.id"><header><div><b>{{ currency.format(plan.totalAmount) }}</b><small>{{ plan.frequency }} · {{ plan.installments.length }} échéances</small></div><span class="plan-status" :data-status="plan.status">{{ plan.status }}</span></header><div class="installment-strip"><div v-for="(installment,index) in plan.installments" :key="installment.id" :data-status="installment.status"><i>{{ installment.status === 'Payée' ? '✓' : index + 1 }}</i><span><b>{{ currency.format(installment.amount) }}</b><small>{{ new Date(installment.dueAt).toLocaleDateString('fr-FR') }}</small></span></div></div></article></div>
        <div v-else class="empty-state compact-empty">Aucun échéancier négocié.</div>
      </section>
      <section class="panel span-2">
        <div class="panel-title"><div><span>Collaboration</span><h3>Notes du dossier</h3></div><button class="text-button" @click="noteOpen = true">Ajouter</button></div>
        <div v-if="notes.length" class="notes-list"><article v-for="note in notes" :key="note.id"><b>{{ note.author }}</b><p>{{ note.content }}</p><small>{{ new Date(note.createdAt).toLocaleString('fr-FR') }}</small></article></div>
        <div v-else class="empty-state compact-empty">Aucune note ajoutée.</div>
      </section>
      <section class="panel span-2">
        <div class="panel-title"><div><span>Relation client</span><h3>Communications</h3></div><button v-if="session.can('communication:send')" class="text-button" @click="openMessage">Nouveau message</button></div>
        <div v-if="communications.length" class="communication-list"><article v-for="communication in communications" :key="communication.id"><span class="channel-badge" :data-channel="communication.channel">{{ communication.channel === 'SMS' ? 'S' : '@' }}</span><div><b>{{ communication.subject || communication.channel }}</b><p>{{ communication.content }}</p><small>{{ communication.recipient }} · {{ communication.sentBy }} · {{ new Date(communication.sentAt).toLocaleString('fr-FR') }}</small></div><span class="status-badge" :data-status="communication.status">{{ communication.status }}</span></article></div>
        <div v-else class="empty-state compact-empty">Aucun message envoyé pour ce dossier.</div>
      </section>
      <section class="panel span-2">
        <div class="panel-title"><div><span>Pièces du dossier</span><h3>Documents</h3></div><button v-if="session.can('document:manage')" class="text-button" @click="documentOpen = true">Ajouter une pièce</button></div>
        <div v-if="documents.length" class="case-documents"><button v-for="document in documents" :key="document.id" @click="previewDocument(document)"><i>{{ document.mimeType.includes('pdf') ? 'PDF' : 'DOC' }}</i><span><b>{{ document.name }}</b><small>{{ document.category }} · v{{ document.version }} · {{ formatDocumentSize(document.size) }}</small></span><em class="document-status" :data-status="document.status">{{ document.status }}</em></button></div>
        <div v-else class="empty-state compact-empty">Aucune pièce rattachée au dossier.</div>
      </section>
      <section class="panel span-2">
        <div class="panel-title"><div><span>Audit</span><h3>Timeline du dossier</h3></div></div>
        <div v-if="audit.length" class="timeline-list"><article v-for="event in audit" :key="event.id"><i>{{ event.type === 'PAYMENT' ? '₿' : event.type === 'PROMISE' ? '✓' : event.type === 'ESCALATION' ? '!' : '•' }}</i><div><b>{{ event.description }}</b><p>{{ event.actor }} · {{ new Date(event.occurredAt).toLocaleString('fr-FR') }}</p></div></article></div>
        <div v-else class="empty-state compact-empty">Aucune action métier enregistrée.</div>
      </section>
    </div>
    <AppModal :open="noteOpen" title="Ajouter une note" @close="noteOpen = false"><form @submit.prevent="addNote"><label class="field-block">Contenu<textarea v-model="noteText" rows="5" required placeholder="Saisir une information utile au suivi du dossier"></textarea></label><div class="form-actions"><button type="button" class="secondary-button" @click="noteOpen = false">Annuler</button><button class="primary-button">Enregistrer</button></div></form></AppModal>
    <AppModal :open="editOpen" title="Modifier le client" @close="editOpen = false"><form class="form-grid" @submit.prevent="saveCustomer"><label>Téléphone<input v-model="editForm.phone" required /></label><label>E-mail<input v-model="editForm.email" type="email" required /></label><label>Probabilité de défaut (%)<input v-model.number="editForm.probabilityOfDefault" type="number" min="0" max="100" /></label><label>Couverture garantie (%)<input v-model.number="editForm.guaranteeCoverage" type="number" min="0" max="200" /></label><div class="form-actions span-2"><button type="button" class="secondary-button" @click="editOpen = false">Annuler</button><button class="primary-button">Enregistrer</button></div></form></AppModal>
    <AppModal :open="actionOpen" title="Enregistrer une action" description="L’action met à jour le dossier et son historique d’audit." @close="actionOpen = false">
      <form class="form-grid" @submit.prevent="executeAction">
        <label class="span-2">Type d’action<select v-model="actionForm.type"><option value="CONTACT">Contact client</option><option value="PROMISE">Promesse de paiement</option><option value="PAYMENT">Paiement reçu</option><option value="ESCALATION">Escalade juridique</option></select></label>
        <label v-if="actionForm.type === 'PROMISE' || actionForm.type === 'PAYMENT'">Montant (MAD)<input v-model.number="actionForm.amount" min="1" type="number" required /></label>
        <label v-if="actionForm.type === 'PROMISE'">Date promise<input v-model="actionForm.promisedAt" type="datetime-local" required /></label>
        <label v-if="actionForm.type === 'PAYMENT'">Mode de paiement<select v-model="actionForm.paymentMethod"><option>Virement</option><option>Prélèvement</option><option>Espèces</option><option>Carte</option><option>Autre</option></select></label>
        <label v-if="actionForm.type === 'PAYMENT'">Référence<input v-model="actionForm.paymentReference" placeholder="Référence bancaire" /></label>
        <label class="span-2">Commentaire<textarea v-model="actionForm.comment" rows="4" :required="actionForm.type !== 'PAYMENT'" /></label>
        <div class="form-actions span-2"><button type="button" class="secondary-button" @click="actionOpen = false">Annuler</button><button class="primary-button" :disabled="working">{{ working ? 'Enregistrement…' : 'Valider l’action' }}</button></div>
      </form>
    </AppModal>
    <AppModal :open="messageOpen" title="Envoyer un message" description="L’envoi sera automatiquement ajouté à la timeline du dossier." @close="messageOpen = false">
      <form class="form-grid" @submit.prevent="sendMessage">
        <label>Canal<select v-model="messageForm.channel" @change="changeMessageChannel"><option>SMS</option><option>E-mail</option></select></label>
        <label>Modèle<select v-model="messageForm.templateId" @change="useMessageTemplate"><option value="">Message libre</option><option v-for="template in templates.filter(x => x.channel === messageForm.channel)" :key="template.id" :value="template.id">{{ template.name }}</option></select></label>
        <label class="span-2">Destinataire<input v-model="messageForm.recipient" required /></label>
        <label v-if="messageForm.channel === 'E-mail'" class="span-2">Objet<input v-model="messageForm.subject" required /></label>
        <label class="span-2">Message<textarea v-model="messageForm.content" rows="7" required placeholder="Rédiger le message à envoyer"></textarea><small>{{ messageForm.content.length }} caractère(s)</small></label>
        <div class="form-actions span-2"><button type="button" class="secondary-button" @click="messageOpen = false">Annuler</button><button class="primary-button" :disabled="working">{{ working ? 'Envoi…' : 'Envoyer et historiser' }}</button></div>
      </form>
    </AppModal>
    <AppModal :open="documentOpen" title="Ajouter une pièce au dossier" description="Le document sera envoyé au circuit de contrôle documentaire." @close="documentOpen = false">
      <form class="form-grid" @submit.prevent="addDocument">
        <label class="span-2">Catégorie<select v-model="documentForm.category"><option v-for="category in documentCategories" :key="category">{{ category }}</option></select></label>
        <label class="file-drop span-2"><input type="file" accept=".pdf,.doc,.docx,.png,.jpg,.jpeg" required @change="selectDocument" /><i>⇧</i><b>{{ documentFile?.name ?? 'Sélectionner une pièce' }}</b><span>{{ documentFile ? formatDocumentSize(documentFile.size) : 'PDF, Word ou image · 10 Mo maximum' }}</span></label>
        <div class="form-actions span-2"><button type="button" class="secondary-button" @click="documentOpen = false">Annuler</button><button class="primary-button" :disabled="working || !documentFile">{{ working ? 'Ajout…' : 'Ajouter au dossier' }}</button></div>
      </form>
    </AppModal>
    <AppModal :open="planOpen" title="Négocier un échéancier" description="Une tâche Flowable sera créée pour chaque échéance." @close="planOpen = false">
      <form class="form-grid" @submit.prevent="createPlan">
        <label>Montant total (MAD)<input v-model.number="planForm.totalAmount" type="number" min="1" :max="item.amount" required /></label>
        <label>Nombre d’échéances<input v-model.number="planForm.installmentCount" type="number" min="2" max="24" required /></label>
        <label>Première échéance<input v-model="planForm.firstDueAt" type="date" required /></label>
        <label>Fréquence<select v-model="planForm.frequency"><option>Mensuelle</option><option>Hebdomadaire</option></select></label>
        <div class="plan-preview span-2"><span>Simulation</span><b>{{ planForm.installmentCount ? currency.format(Math.floor(planForm.totalAmount / planForm.installmentCount)) : '—' }} par échéance</b><small>La dernière échéance absorbera l’éventuel reliquat d’arrondi.</small></div>
        <div class="form-actions span-2"><button type="button" class="secondary-button" @click="planOpen = false">Annuler</button><button class="primary-button" :disabled="working">{{ working ? 'Planification…' : 'Créer l’échéancier' }}</button></div>
      </form>
    </AppModal>
  </div>
  <div v-else class="loading-state">Chargement du dossier…</div>
</template>

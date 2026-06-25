<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AppModal from '@/components/ui/AppModal.vue'
import { api } from '@/services/api'
import { useNotificationsStore } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'
import type { CollectionCase, Communication, CommunicationChannel, MessageTemplate } from '@/types/domain'

const communications = ref<Communication[]>([])
const cases = ref<CollectionCase[]>([])
const templates = ref<MessageTemplate[]>([])
const query = ref('')
const channelFilter = ref<'Tous' | CommunicationChannel>('Tous')
const composeOpen = ref(false)
const sending = ref(false)
const form = reactive({
  caseId: '',
  channel: 'SMS' as CommunicationChannel,
  templateId: '',
  recipient: '',
  subject: '',
  content: '',
})
const notifications = useNotificationsStore()
const session = useSessionStore()
const date = new Intl.DateTimeFormat('fr-FR', { dateStyle:'medium', timeStyle:'short' })

const availableTemplates = computed(() => templates.value.filter((item) => item.channel === form.channel))
const selectedCase = computed(() => cases.value.find((item) => item.id === form.caseId))
const filtered = computed(() => communications.value.filter((item) => {
  const matchesQuery = `${item.customerName} ${item.caseId} ${item.recipient} ${item.content}`.toLowerCase().includes(query.value.toLowerCase())
  return matchesQuery && (channelFilter.value === 'Tous' || item.channel === channelFilter.value)
}))
const delivered = computed(() => communications.value.filter((item) => item.status === 'Livré').length)
const deliveryRate = computed(() => communications.value.length ? Math.round(delivered.value / communications.value.length * 100) : 0)

onMounted(async () => {
  ;[communications.value, cases.value, templates.value] = await Promise.all([
    api.getCommunications(),
    api.getCases(),
    api.getMessageTemplates(),
  ])
})

watch(() => form.caseId, () => {
  if (!selectedCase.value) return
  form.recipient = form.channel === 'SMS' ? selectedCase.value.phone : selectedCase.value.email
  refreshTemplate()
})

watch(() => form.channel, () => {
  form.templateId = ''
  form.subject = ''
  form.content = ''
  if (selectedCase.value) form.recipient = form.channel === 'SMS' ? selectedCase.value.phone : selectedCase.value.email
})

watch(() => form.templateId, refreshTemplate)

function resolveVariables(value = '') {
  return value
    .split('{{client}}').join(selectedCase.value?.customerName ?? '')
    .split('{{caseId}}').join(selectedCase.value?.id ?? '')
}

function refreshTemplate() {
  const template = templates.value.find((item) => item.id === form.templateId)
  if (!template) return
  form.subject = resolveVariables(template.subject)
  form.content = resolveVariables(template.content)
}

function openComposer() {
  Object.assign(form, { caseId:cases.value[0]?.id ?? '', channel:'SMS', templateId:'', recipient:cases.value[0]?.phone ?? '', subject:'', content:'' })
  composeOpen.value = true
}

async function send() {
  if (!form.caseId) return
  sending.value = true
  try {
    const communication = await api.sendCommunication({
      caseId:form.caseId,
      channel:form.channel,
      recipient:form.recipient,
      subject:form.subject || undefined,
      content:form.content,
      sentBy:session.user.displayName,
    })
    communications.value.unshift(communication)
    composeOpen.value = false
    notifications.push(`${form.channel} envoyé à ${communication.customerName}`)
  } catch (error) {
    notifications.push(error instanceof Error ? error.message : 'Envoi impossible', 'error')
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="page-heading compact">
    <div><span class="eyebrow">Relation client</span><h1>Centre de communications</h1><p>Préparez, envoyez et tracez les relances rattachées aux dossiers.</p></div>
    <button v-if="session.can('communication:send')" class="primary-button" @click="openComposer">＋ Nouveau message</button>
  </div>

  <section class="stats-grid communication-stats">
    <article class="stat-card"><div class="stat-icon" data-tone="blue">✉</div><div><span>Messages envoyés</span><strong>{{ communications.length }}</strong><small>Tous canaux</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="green">✓</div><div><span>Taux de livraison</span><strong>{{ deliveryRate }} %</strong><small>{{ delivered }} message(s) livré(s)</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="orange">S</div><div><span>SMS</span><strong>{{ communications.filter(x => x.channel === 'SMS').length }}</strong><small>Relances mobiles</small></div></article>
    <article class="stat-card"><div class="stat-icon" data-tone="purple">@</div><div><span>E-mails</span><strong>{{ communications.filter(x => x.channel === 'E-mail').length }}</strong><small>Messages détaillés</small></div></article>
  </section>

  <section class="panel">
    <div class="filters">
      <label class="search-field"><span>⌕</span><input v-model="query" placeholder="Rechercher un client, dossier ou destinataire" /></label>
      <div class="filter-tabs">
        <button v-for="value in ['Tous', 'SMS', 'E-mail'] as const" :key="value" :class="{active:channelFilter === value}" @click="channelFilter = value">{{ value }}</button>
      </div>
    </div>
    <div class="table-shell">
      <table v-if="filtered.length">
        <thead><tr><th>Canal</th><th>Client / dossier</th><th>Destinataire</th><th>Message</th><th>Envoyé par</th><th>Date</th><th>Statut</th></tr></thead>
        <tbody>
          <tr v-for="item in filtered" :key="item.id">
            <td><span class="channel-badge" :data-channel="item.channel">{{ item.channel === 'SMS' ? 'S' : '@' }}</span> {{ item.channel }}</td>
            <td><RouterLink :to="`/cases/${item.caseId}`" class="primary-cell"><b>{{ item.customerName }}</b><span>{{ item.caseId }}</span></RouterLink></td>
            <td>{{ item.recipient }}</td>
            <td class="message-preview"><b v-if="item.subject">{{ item.subject }}</b><span>{{ item.content }}</span></td>
            <td>{{ item.sentBy }}</td>
            <td>{{ date.format(new Date(item.sentAt)) }}</td>
            <td><span class="status-badge" :data-status="item.status">{{ item.status }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-if="!filtered.length" class="empty-state">Aucune communication dans cette vue.</div>
  </section>

  <AppModal :open="composeOpen" title="Nouveau message" description="Le message sera historisé dans le dossier client." @close="composeOpen = false">
    <form class="form-grid" @submit.prevent="send">
      <label class="span-2">Dossier
        <select v-model="form.caseId" required><option v-for="item in cases" :key="item.id" :value="item.id">{{ item.id }} · {{ item.customerName }}</option></select>
      </label>
      <label>Canal<select v-model="form.channel"><option>SMS</option><option>E-mail</option></select></label>
      <label>Modèle<select v-model="form.templateId"><option value="">Message libre</option><option v-for="item in availableTemplates" :key="item.id" :value="item.id">{{ item.name }}</option></select></label>
      <label class="span-2">Destinataire<input v-model="form.recipient" required /></label>
      <label v-if="form.channel === 'E-mail'" class="span-2">Objet<input v-model="form.subject" required /></label>
      <label class="span-2">Message<textarea v-model="form.content" rows="7" required placeholder="Rédiger le message à envoyer"></textarea><small>{{ form.content.length }} caractère(s)</small></label>
      <div class="message-context span-2"><span>Dossier lié</span><b>{{ selectedCase?.customerName ?? '—' }}</b><small v-if="selectedCase">Encours : {{ selectedCase.amount.toLocaleString('fr-FR') }} MAD · {{ selectedCase.overdueDays }} jours de retard</small></div>
      <div class="form-actions span-2"><button type="button" class="secondary-button" @click="composeOpen = false">Annuler</button><button class="primary-button" :disabled="sending">{{ sending ? 'Envoi…' : 'Envoyer le message' }}</button></div>
    </form>
  </AppModal>
</template>

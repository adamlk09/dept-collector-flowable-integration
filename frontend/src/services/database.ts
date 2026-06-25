import { agents, cases, decisions, tasks } from '@/data/mockData'
import type { Agent, ApplicationRole, ApplicationUser, AuditEvent, BankTransaction, CaseDocument, CaseNote, CollectionCase, Communication, DecisionTrace, DmnRule, LegalProcedure, MessageTemplate, Payment, PaymentPlan, PaymentPromise, WorkflowInstance, WorkflowTask } from '@/types/domain'

interface Database {
  cases: CollectionCase[]
  decisions: DecisionTrace[]
  tasks: WorkflowTask[]
  agents: Agent[]
  notes: CaseNote[]
  audit: AuditEvent[]
  payments: Payment[]
  bankTransactions: BankTransaction[]
  promises: PaymentPromise[]
  paymentPlans: PaymentPlan[]
  communications: Communication[]
  templates: MessageTemplate[]
  roles: ApplicationRole[]
  users: ApplicationUser[]
  notificationReads: string[]
  dmnRules: DmnRule[]
  workflowInstances: WorkflowInstance[]
  documents: CaseDocument[]
  legalProcedures: LegalProcedure[]
}

const storageKey = 'collect-poc-database-v1'

const seed = (): Database => ({
  cases: structuredClone(cases),
  decisions: structuredClone(decisions),
  tasks: structuredClone(tasks),
  agents: structuredClone(agents),
  notes: [],
  audit: [],
  payments: [],
  bankTransactions: [
    { id:'BANK-001', bankReference:'VIR-20260620-88421', label:'ATLAS DISTRIBUTION REGLEMENT CT88421', amount:85000, valueDate:'2026-06-20T08:00:00.000Z', receivedAt:'2026-06-20T08:12:00.000Z', suggestedCaseId:'DC-2026-001', confidence:96, status:'À rapprocher' },
    { id:'BANK-002', bankReference:'VIR-20260620-77410', label:'RIAD SERVICES ECHEANCE 1', amount:30000, valueDate:'2026-06-20T09:00:00.000Z', receivedAt:'2026-06-20T09:06:00.000Z', suggestedCaseId:'DC-2026-003', confidence:89, status:'À rapprocher' },
  ],
  promises: [],
  paymentPlans: [
    {
      id:'PLAN-001', caseId:'DC-2026-003', customerName:'Riad Services', totalAmount:90000, frequency:'Mensuelle', status:'Actif', createdAt:'2026-06-17T10:30:00.000Z', createdBy:'Salma Idrissi',
      installments:[
        { id:'INST-001-1', dueAt:'2026-06-25T10:00:00.000Z', amount:30000, paidAmount:0, status:'À venir' },
        { id:'INST-001-2', dueAt:'2026-07-25T10:00:00.000Z', amount:30000, paidAmount:0, status:'À venir' },
        { id:'INST-001-3', dueAt:'2026-08-25T10:00:00.000Z', amount:30000, paidAmount:0, status:'À venir' },
      ],
    },
  ],
  communications: [],
  templates: [
    { id:'TPL-SMS-REMINDER', name:'Rappel amiable', channel:'SMS', content:'Bonjour {{client}}, nous vous invitons à régulariser votre échéance liée au dossier {{caseId}}. Merci de nous contacter.' },
    { id:'TPL-EMAIL-PROMISE', name:'Confirmation de promesse', channel:'E-mail', subject:'Confirmation de votre engagement de paiement', content:'Bonjour {{client}}, nous confirmons votre engagement concernant le dossier {{caseId}}. Notre équipe reste à votre disposition.' },
    { id:'TPL-EMAIL-LEGAL', name:'Notification avant escalade', channel:'E-mail', subject:'Action requise concernant votre dossier', content:'Bonjour {{client}}, votre dossier {{caseId}} nécessite une régularisation immédiate afin d’éviter une escalade du traitement.' },
  ],
  roles: [
    { id:'ROLE-ADMIN', name:'Administrateur plateforme', description:'Administration fonctionnelle et paramétrage de la plateforme.', permissions:['case:create','case:update','decision:execute','assignment:execute','task:complete','rule:manage','workflow:manage','document:manage','payment:reconcile','legal:manage','organization:manage','communication:send','access:manage'] },
    { id:'ROLE-SUPERVISOR', name:'Superviseur recouvrement', description:'Pilotage des équipes et exécution des traitements métier.', permissions:['case:create','case:update','decision:execute','assignment:execute','task:complete','workflow:manage','document:manage','payment:reconcile','legal:manage','organization:manage','communication:send'] },
    { id:'ROLE-AGENT', name:'Agent de recouvrement', description:'Traitement quotidien des dossiers affectés.', permissions:['case:update','task:complete','document:manage','communication:send'] },
    { id:'ROLE-AUDITOR', name:'Auditeur', description:'Consultation transverse sans droit de modification.', permissions:[] },
  ],
  users: [
    { id:'USR-001', displayName:'Oussama El Amrani', email:'oussama.elamrani@collect.ma', roleId:'ROLE-ADMIN', team:'Plateforme', tenantName:'Banque Démo', tenantId:'tenant-demo', active:true, lastLoginAt:'2026-06-19T08:12:00.000Z' },
    { id:'USR-002', displayName:'Salma Idrissi', email:'salma.idrissi@collect.ma', roleId:'ROLE-SUPERVISOR', team:'Supervision', tenantName:'Banque Démo', tenantId:'tenant-demo', active:true, lastLoginAt:'2026-06-18T15:45:00.000Z' },
    { id:'USR-003', displayName:'Yasmine Benali', email:'yasmine.benali@collect.ma', roleId:'ROLE-AGENT', team:'Juridique', tenantName:'Banque Démo', tenantId:'tenant-demo', active:true, lastLoginAt:'2026-06-19T07:30:00.000Z' },
    { id:'USR-004', displayName:'Karim El Fassi', email:'karim.elfassi@collect.ma', roleId:'ROLE-AUDITOR', team:'Conformité', tenantName:'Banque Démo', tenantId:'tenant-demo', active:false },
  ],
  notificationReads: [],
  dmnRules: [
    {
      id:'DMN-RULE-001',
      name:'Segmentation principale',
      key:'customer-segmentation',
      version:3,
      status:'Active',
      updatedAt:'2026-06-19T09:42:00.000Z',
      hit:'FIRST',
      description:'Détermine la stratégie de recouvrement selon le retard, l’exposition, le risque et les garanties.',
      thresholds:{ legalOverdueDays:90, legalProbabilityOfDefault:80, negotiationAmount:50000, negotiationProbabilityOfDefault:60, lowGuaranteeCoverage:30 },
      outputs:{ legal:'RECOUVREMENT_JURIDIQUE', negotiation:'NEGOCIATION_RENFORCEE', amicable:'RELANCE_AMIABLE' },
    },
    {
      id:'DMN-RULE-002',
      name:'Priorité juridique',
      key:'legal-priority',
      version:1,
      status:'Brouillon',
      updatedAt:'2026-06-18T16:20:00.000Z',
      hit:'FIRST',
      description:'Priorise les dossiers à transmettre aux équipes juridiques.',
      thresholds:{ legalOverdueDays:120, legalProbabilityOfDefault:85, negotiationAmount:250000, negotiationProbabilityOfDefault:70, lowGuaranteeCoverage:20 },
      outputs:{ legal:'PRIORITE_IMMEDIATE', negotiation:'REVUE_RENFORCEE', amicable:'SURVEILLANCE' },
    },
  ],
  workflowInstances: cases.map((item, index) => ({
    id:`PROC-${String(index + 1).padStart(5,'0')}`,
    caseId:item.id,
    customerName:item.customerName,
    processDefinitionKey:'debt-collection',
    processDefinitionVersion:4,
    businessKey:item.id,
    status:item.id === 'DC-2026-005' ? 'Incident' : 'Active',
    currentActivity:item.id === 'DC-2026-001' ? 'Revue juridique' : item.id === 'DC-2026-003' ? 'Attente paiement promis' : item.assignee ? 'Traitement agent' : 'Affectation automatique',
    startedAt:`2026-06-${String(14 + index).padStart(2,'0')}T08:30:00.000Z`,
    updatedAt:`2026-06-19T${String(9 + index).padStart(2,'0')}:15:00.000Z`,
    ...(item.id === 'DC-2026-005' ? { incident:{ message:'Groupe candidat juridique introuvable', activityId:'legalReview', occurredAt:'2026-06-19T13:15:00.000Z', retries:2 } } : {}),
  })) as WorkflowInstance[],
  documents: [
    { id:'DOC-001', caseId:'DC-2026-001', customerName:'Atlas Distribution', name:'Contrat_CT-88421.pdf', category:'Contrat', mimeType:'application/pdf', size:1284000, version:1, status:'Validé', storageKey:'tenant-demo/DC-2026-001/DOC-001', checksum:'sha256:9bc2a4f1', uploadedAt:'2026-06-14T08:35:00.000Z', uploadedBy:'Oussama El Amrani', validatedAt:'2026-06-14T09:10:00.000Z', validatedBy:'Salma Idrissi' },
    { id:'DOC-002', caseId:'DC-2026-001', customerName:'Atlas Distribution', name:'Mise_en_demeure_2026-06-18.pdf', category:'Mise en demeure', mimeType:'application/pdf', size:346000, version:1, status:'À contrôler', storageKey:'tenant-demo/DC-2026-001/DOC-002', checksum:'sha256:16d98ac7', uploadedAt:'2026-06-18T14:20:00.000Z', uploadedBy:'Yasmine Benali' },
    { id:'DOC-003', caseId:'DC-2026-003', customerName:'Riad Services', name:'Promesse_signee.pdf', category:'Pièce juridique', mimeType:'application/pdf', size:518000, version:1, status:'Validé', storageKey:'tenant-demo/DC-2026-003/DOC-003', checksum:'sha256:723cf9d2', uploadedAt:'2026-06-17T11:05:00.000Z', uploadedBy:'Salma Idrissi', validatedAt:'2026-06-17T11:42:00.000Z', validatedBy:'Oussama El Amrani' },
  ],
  legalProcedures: [
    {
      id:'LEGAL-001', caseId:'DC-2026-001', customerName:'Atlas Distribution', jurisdiction:'Tribunal de commerce de Casablanca', courtReference:'2026/8201/1542', lawyer:'Cabinet El Mansouri & Associés', claimAmount:486000, legalFees:12500, status:'Audience planifiée', openedAt:'2026-06-18T09:00:00.000Z', openedBy:'Yasmine Benali', nextHearingAt:'2026-07-08T09:30:00.000Z',
      events:[
        { id:'LEV-001', type:'Dépôt', title:'Dépôt de la requête', scheduledAt:'2026-06-18T09:00:00.000Z', completed:true, createdAt:'2026-06-18T09:00:00.000Z', createdBy:'Yasmine Benali' },
        { id:'LEV-002', type:'Audience', title:'Première audience', scheduledAt:'2026-07-08T09:30:00.000Z', completed:false, createdAt:'2026-06-19T11:20:00.000Z', createdBy:'Yasmine Benali' },
      ],
    },
  ],
})

function load(): Database {
  const saved = localStorage.getItem(storageKey)
  if (!saved) return seed()
  try {
    const parsed = JSON.parse(saved) as Partial<Database>
    const defaults = seed()
    const roles = (parsed.roles ?? defaults.roles).map((role) => {
      const defaultRole = defaults.roles.find((entry) => entry.id === role.id)
      return defaultRole ? { ...role, permissions:[...new Set([...role.permissions, ...defaultRole.permissions])] } : role
    })
    return { ...defaults, ...parsed, payments:parsed.payments ?? [], bankTransactions:parsed.bankTransactions ?? defaults.bankTransactions, promises:parsed.promises ?? [], paymentPlans:parsed.paymentPlans ?? defaults.paymentPlans, communications:parsed.communications ?? [], templates:parsed.templates ?? defaults.templates, roles, users:parsed.users ?? defaults.users, notificationReads:parsed.notificationReads ?? [], dmnRules:parsed.dmnRules ?? defaults.dmnRules, workflowInstances:parsed.workflowInstances ?? defaults.workflowInstances, documents:parsed.documents ?? defaults.documents, legalProcedures:parsed.legalProcedures ?? defaults.legalProcedures, notes:parsed.notes ?? [], audit:parsed.audit ?? [] }
  } catch { return seed() }
}

let database = load()

export function getDatabase() {
  return database
}

export function commit() {
  localStorage.setItem(storageKey, JSON.stringify(database))
}

export function resetDatabase() {
  database = seed()
  commit()
}

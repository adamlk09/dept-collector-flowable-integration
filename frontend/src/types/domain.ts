export type CaseStatus = 'À traiter' | 'En cours' | 'Promesse' | 'Juridique'
export type RiskBand = 'Faible' | 'Modéré' | 'Élevé' | 'Critique'

export interface CollectionCase {
  id: string
  customerName: string
  customerCode: string
  contractId: string
  amount: number
  overdueDays: number
  status: CaseStatus
  segment: string
  risk: RiskBand
  assignee: string | null
  nextAction: string
  phone: string
  email: string
  probabilityOfDefault: number
  guaranteeCoverage: number
}

export interface DecisionTrace {
  id: string
  caseId: string
  decisionKey: string
  version: number
  result: string
  reasons: string[]
  executedAt: string
}

export interface WorkflowTask {
  id: string
  caseId: string
  name: string
  assignee: string
  dueAt: string
  status: 'Prête' | 'En cours' | 'Terminée'
}

export type WorkflowInstanceStatus = 'Active' | 'Suspendue' | 'Terminée' | 'Incident'

export interface WorkflowInstance {
  id: string
  caseId: string
  customerName: string
  processDefinitionKey: string
  processDefinitionVersion: number
  businessKey: string
  status: WorkflowInstanceStatus
  currentActivity: string
  startedAt: string
  updatedAt: string
  completedAt?: string
  incident?: {
    message: string
    activityId: string
    occurredAt: string
    retries: number
  }
}

export interface Agent {
  id: string
  name: string
  team: string
  capacity: number
  activeCases: number
  skills: string[]
  available: boolean
}

export interface AgentInput {
  name: string
  team: string
  capacity: number
  skills: string[]
  available: boolean
}

export interface OrganizationTeam {
  name: string
  manager: string
  region: string
  memberCount: number
  capacity: number
  activeCases: number
}

export interface ApplicationRole {
  id: string
  name: string
  description: string
  permissions: string[]
}

export interface ApplicationUser {
  id: string
  displayName: string
  email: string
  roleId: string
  team: string
  tenantName: string
  tenantId: string
  active: boolean
  lastLoginAt?: string
}

export interface ApplicationUserInput {
  displayName: string
  email: string
  roleId: string
  team: string
  active: boolean
}

export type AlertSeverity = 'Information' | 'Attention' | 'Critique'

export interface OperationalAlert {
  id: string
  severity: AlertSeverity
  category: 'Tâche' | 'Dossier' | 'Promesse'
  title: string
  description: string
  caseId: string
  route: string
  occurredAt: string
  read: boolean
}

export type CommunicationChannel = 'SMS' | 'E-mail'
export type CommunicationStatus = 'Envoyé' | 'Livré' | 'Échec'

export interface Communication {
  id: string
  caseId: string
  customerName: string
  channel: CommunicationChannel
  recipient: string
  subject?: string
  content: string
  status: CommunicationStatus
  sentAt: string
  sentBy: string
}

export interface MessageTemplate {
  id: string
  name: string
  channel: CommunicationChannel
  subject?: string
  content: string
}

export type DocumentCategory = 'Contrat' | 'Mise en demeure' | 'Justificatif de paiement' | 'Garantie' | 'Pièce juridique' | 'Autre'
export type DocumentStatus = 'À contrôler' | 'Validé' | 'Rejeté'

export interface CaseDocument {
  id: string
  caseId: string
  customerName: string
  name: string
  category: DocumentCategory
  mimeType: string
  size: number
  version: number
  status: DocumentStatus
  storageKey: string
  checksum: string
  uploadedAt: string
  uploadedBy: string
  validatedAt?: string
  validatedBy?: string
  rejectionReason?: string
  dataUrl?: string
}

export interface CaseDocumentInput {
  caseId: string
  name: string
  category: DocumentCategory
  mimeType: string
  size: number
  uploadedBy: string
  dataUrl?: string
}

export interface CaseNote {
  id: string
  caseId: string
  content: string
  author: string
  createdAt: string
}

export interface AuditEvent {
  id: string
  caseId: string
  type: string
  description: string
  actor: string
  occurredAt: string
}

export type CaseActionType = 'CONTACT' | 'PROMISE' | 'PAYMENT' | 'ESCALATION'

export interface CaseActionInput {
  type: CaseActionType
  comment: string
  amount?: number
  promisedAt?: string
  paymentMethod?: Payment['method']
  paymentReference?: string
}

export interface CaseActionResult {
  case: CollectionCase
  audit: AuditEvent
  task?: WorkflowTask
}

export interface Payment {
  id: string
  caseId: string
  customerName: string
  amount: number
  receivedAt: string
  method: 'Virement' | 'Prélèvement' | 'Espèces' | 'Carte' | 'Autre'
  reference: string
  status: 'Confirmé' | 'À rapprocher' | 'Rejeté'
  reconciledAt?: string
  reconciledBy?: string
}

export interface BankTransaction {
  id: string
  bankReference: string
  label: string
  amount: number
  valueDate: string
  receivedAt: string
  suggestedCaseId?: string
  confidence: number
  status: 'À rapprocher' | 'Rapproché' | 'Rejeté'
  paymentId?: string
  reconciledAt?: string
  reconciledBy?: string
  rejectionReason?: string
}

export interface PaymentPromise {
  id: string
  caseId: string
  customerName: string
  amount: number
  promisedAt: string
  createdAt: string
  status: 'Active' | 'Honorée' | 'Rompue'
}

export interface PaymentInstallment {
  id: string
  dueAt: string
  amount: number
  paidAmount: number
  status: 'À venir' | 'Échue' | 'Payée' | 'Partielle'
  paidAt?: string
}

export interface PaymentPlan {
  id: string
  caseId: string
  customerName: string
  totalAmount: number
  frequency: 'Hebdomadaire' | 'Mensuelle'
  status: 'Actif' | 'Honoré' | 'En retard' | 'Annulé'
  createdAt: string
  createdBy: string
  installments: PaymentInstallment[]
}

export interface PaymentPlanInput {
  caseId: string
  totalAmount: number
  installmentCount: number
  firstDueAt: string
  frequency: PaymentPlan['frequency']
  createdBy: string
}

export type LegalProcedureStatus = 'Préparation' | 'Déposée' | 'Audience planifiée' | 'Jugement rendu' | 'Exécution' | 'Clôturée'
export type LegalEventType = 'Dépôt' | 'Audience' | 'Jugement' | 'Signification' | 'Exécution' | 'Note'

export interface LegalEvent {
  id: string
  type: LegalEventType
  title: string
  scheduledAt: string
  completed: boolean
  notes?: string
  createdAt: string
  createdBy: string
}

export interface LegalProcedure {
  id: string
  caseId: string
  customerName: string
  jurisdiction: string
  courtReference: string
  lawyer: string
  claimAmount: number
  legalFees: number
  status: LegalProcedureStatus
  openedAt: string
  openedBy: string
  nextHearingAt?: string
  events: LegalEvent[]
}

export interface LegalProcedureInput {
  caseId: string
  jurisdiction: string
  courtReference: string
  lawyer: string
  claimAmount: number
  legalFees: number
  openedBy: string
}

export interface DmnRule {
  id: string
  name: string
  key: string
  version: number
  status: 'Active' | 'Brouillon'
  updatedAt: string
  hit: 'FIRST' | 'COLLECT'
  description: string
  thresholds: {
    legalOverdueDays: number
    legalProbabilityOfDefault: number
    negotiationAmount: number
    negotiationProbabilityOfDefault: number
    lowGuaranteeCoverage: number
  }
  outputs: {
    legal: string
    negotiation: string
    amicable: string
  }
}

export type DmnRuleInput = Pick<CollectionCase, 'amount' | 'overdueDays' | 'probabilityOfDefault' | 'guaranteeCoverage'>

export interface DashboardActivity {
  id: string
  type: 'DMN' | 'AFFECTATION' | 'PAIEMENT' | 'PROMESSE' | 'COMMUNICATION' | 'ACTION' | 'WORKFLOW'
  title: string
  description: string
  caseId: string
  occurredAt: string
  tone: 'blue' | 'green' | 'orange' | 'purple' | 'red'
}

export interface PortfolioMetrics {
  totalOutstanding: number
  totalCases: number
  criticalCases: number
  unassignedCases: number
  paymentsReceived: number
  activePromises: number
  brokenPromises: number
  openTasks: number
  overdueTasks: number
  workflowIncidents: number
  recoveryRate: number
  slaComplianceRate: number
  averageOverdueDays: number
  statusDistribution: Array<{ label: string; count: number; amount: number }>
  riskDistribution: Array<{ label: string; count: number; amount: number }>
  agentPerformance: Array<{ name: string; activeCases: number; capacity: number; loadRate: number }>
}

export interface AssignmentResult {
  caseId: string
  agentId: string
  agentName: string
  score: number
  reasons: string[]
}

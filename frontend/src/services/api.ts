import { commit, getDatabase, resetDatabase } from '@/services/database'
import type { AgentInput, ApplicationUserInput, AssignmentResult, CaseActionInput, CaseActionResult, CaseDocumentInput, CaseNote, CollectionCase, CommunicationChannel, DashboardActivity, DecisionTrace, DmnRule, DmnRuleInput, LegalEventType, LegalProcedureInput, LegalProcedureStatus, OperationalAlert, PaymentPlanInput, PortfolioMetrics, WorkflowInstance, WorkflowTask } from '@/types/domain'

const wait = (ms = 180) => new Promise((resolve) => setTimeout(resolve, ms))

function evaluateDmnRule(rule: DmnRule, input: DmnRuleInput) {
  const reasons: string[] = []
  let result = rule.outputs.amicable
  if (input.overdueDays > rule.thresholds.legalOverdueDays || input.probabilityOfDefault >= rule.thresholds.legalProbabilityOfDefault) {
    result = rule.outputs.legal
    if (input.overdueDays > rule.thresholds.legalOverdueDays) reasons.push(`Retard supérieur à ${rule.thresholds.legalOverdueDays} jours`)
    if (input.probabilityOfDefault >= rule.thresholds.legalProbabilityOfDefault) reasons.push(`Probabilité de défaut supérieure ou égale à ${rule.thresholds.legalProbabilityOfDefault} %`)
    if (input.guaranteeCoverage < rule.thresholds.lowGuaranteeCoverage) reasons.push(`Couverture de garantie inférieure à ${rule.thresholds.lowGuaranteeCoverage} %`)
  } else if (input.amount > rule.thresholds.negotiationAmount || input.probabilityOfDefault >= rule.thresholds.negotiationProbabilityOfDefault) {
    result = rule.outputs.negotiation
    if (input.amount > rule.thresholds.negotiationAmount) reasons.push(`Encours supérieur à ${rule.thresholds.negotiationAmount.toLocaleString('fr-FR')} MAD`)
    if (input.probabilityOfDefault >= rule.thresholds.negotiationProbabilityOfDefault) reasons.push(`Probabilité de défaut supérieure ou égale à ${rule.thresholds.negotiationProbabilityOfDefault} %`)
  } else reasons.push('Les critères sont compatibles avec le traitement amiable')
  return { result, reasons, version:rule.version, decisionKey:rule.key }
}

function updateWorkflow(caseId: string, changes: Partial<WorkflowInstance>) {
  const instance = getDatabase().workflowInstances.find((entry) => entry.caseId === caseId)
  if (!instance || instance.status === 'Suspendue') return instance
  Object.assign(instance, changes, { updatedAt:new Date().toISOString() })
  return instance
}

// Replace these methods with HTTP calls when backend endpoints are available.
export const api = {
  async getCases(): Promise<CollectionCase[]> {
    await wait()
    return structuredClone(getDatabase().cases)
  },
  async getPortfolioMetrics(): Promise<PortfolioMetrics> {
    await wait()
    const database = getDatabase()
    const now = new Date()
    const totalOutstanding = database.cases.reduce((sum,item) => sum + item.amount, 0)
    const paymentsReceived = database.payments.reduce((sum,item) => sum + item.amount, 0)
    const totalManaged = totalOutstanding + paymentsReceived
    const openTasks = database.tasks.filter((task) => task.status !== 'Terminée')
    const groupCases = (field: 'status' | 'risk') => [...new Set(database.cases.map((item) => item[field]))].map((label) => {
      const values = database.cases.filter((item) => item[field] === label)
      return { label, count:values.length, amount:values.reduce((sum,item) => sum + item.amount, 0) }
    }).sort((a,b) => b.amount - a.amount)
    return structuredClone({
      totalOutstanding,
      totalCases:database.cases.length,
      criticalCases:database.cases.filter((item) => item.risk === 'Critique').length,
      unassignedCases:database.cases.filter((item) => !item.assignee).length,
      paymentsReceived,
      activePromises:database.promises.filter((item) => item.status === 'Active' && new Date(item.promisedAt) >= now).length,
      brokenPromises:database.promises.filter((item) => item.status === 'Rompue' || (item.status === 'Active' && new Date(item.promisedAt) < now)).length,
      openTasks:openTasks.length,
      overdueTasks:openTasks.filter((task) => new Date(task.dueAt) < now).length,
      workflowIncidents:database.workflowInstances.filter((item) => item.status === 'Incident').length,
      recoveryRate:totalManaged ? Math.round(paymentsReceived / totalManaged * 1000) / 10 : 0,
      slaComplianceRate:openTasks.length ? Math.round(openTasks.filter((task) => new Date(task.dueAt) >= now).length / openTasks.length * 100) : 100,
      averageOverdueDays:database.cases.length ? Math.round(database.cases.reduce((sum,item) => sum + item.overdueDays, 0) / database.cases.length) : 0,
      statusDistribution:groupCases('status'),
      riskDistribution:groupCases('risk'),
      agentPerformance:database.agents.map((agent) => ({ name:agent.name, activeCases:agent.activeCases, capacity:agent.capacity, loadRate:Math.round(agent.activeCases / agent.capacity * 100) })).sort((a,b) => b.loadRate - a.loadRate),
    })
  },
  async getDashboardActivity(): Promise<DashboardActivity[]> {
    await wait()
    const database = getDatabase()
    const caseName = (caseId: string) => database.cases.find((item) => item.id === caseId)?.customerName ?? caseId
    const activities: DashboardActivity[] = [
      ...database.audit.map((event) => ({
        id:event.id,
        type:(event.type === 'PAYMENT' ? 'PAIEMENT' : event.type === 'PROMISE' ? 'PROMESSE' : event.type === 'COMMUNICATION' ? 'COMMUNICATION' : 'ACTION') as DashboardActivity['type'],
        title:event.type === 'PAYMENT' ? 'Paiement enregistré' : event.type === 'PROMISE' ? 'Promesse enregistrée' : event.type === 'COMMUNICATION' ? 'Communication envoyée' : event.description,
        description:`${caseName(event.caseId)} · ${event.actor}`,
        caseId:event.caseId,
        occurredAt:event.occurredAt,
        tone:(event.type === 'PAYMENT' ? 'green' : event.type === 'PROMISE' ? 'orange' : event.type === 'COMMUNICATION' ? 'blue' : 'purple') as DashboardActivity['tone'],
      })),
      ...database.decisions.map((decision) => ({
        id:decision.id, type:'DMN' as const, title:'Décision DMN exécutée', description:`${caseName(decision.caseId)} · ${decision.result} · v${decision.version}`, caseId:decision.caseId, occurredAt:decision.executedAt, tone:'purple' as const,
      })),
      ...database.payments.map((payment) => ({
        id:payment.id, type:'PAIEMENT' as const, title:'Paiement reçu', description:`${payment.customerName} · ${payment.amount.toLocaleString('fr-FR')} MAD`, caseId:payment.caseId, occurredAt:payment.receivedAt, tone:'green' as const,
      })),
      ...database.workflowInstances.filter((instance) => instance.status === 'Incident').map((instance) => ({
        id:`ACT-${instance.id}`, type:'WORKFLOW' as const, title:'Incident Flowable', description:`${instance.customerName} · ${instance.incident?.message ?? instance.currentActivity}`, caseId:instance.caseId, occurredAt:instance.updatedAt, tone:'red' as const,
      })),
    ]
    const unique = [...new Map(activities.sort((a,b) => b.occurredAt.localeCompare(a.occurredAt)).map((item) => [`${item.type}-${item.caseId}-${item.occurredAt}`, item])).values()]
    return structuredClone(unique.slice(0,20))
  },
  async exportPortfolioCsv() {
    await wait()
    const header = ['Dossier','Client','Contrat','Encours MAD','Retard jours','Statut','Risque','Segment','Affectataire','Prochaine action']
    const escape = (value: string | number | null) => `"${String(value ?? '').replace(/"/g, '""')}"`
    return [header.map(escape).join(';'), ...getDatabase().cases.map((item) => [item.id,item.customerName,item.contractId,item.amount,item.overdueDays,item.status,item.risk,item.segment,item.assignee,item.nextAction].map(escape).join(';'))].join('\n')
  },
  async getCase(id: string) {
    await wait()
    return structuredClone(getDatabase().cases.find((item) => item.id === id) ?? null)
  },
  async getDecision(caseId: string) {
    await wait()
    return structuredClone(getDatabase().decisions.find((item) => item.caseId === caseId) ?? null)
  },
  async getTasks(caseId?: string) {
    await wait()
    const values = getDatabase().tasks
    return structuredClone(caseId ? values.filter((item) => item.caseId === caseId) : values)
  },
  async getWorkflowInstances(caseId?: string) {
    await wait()
    const values = caseId ? getDatabase().workflowInstances.filter((entry) => entry.caseId === caseId) : getDatabase().workflowInstances
    return structuredClone(values.sort((a,b) => b.updatedAt.localeCompare(a.updatedAt)))
  },
  async suspendWorkflowInstance(id: string) {
    await wait()
    const instance = getDatabase().workflowInstances.find((entry) => entry.id === id)
    if (!instance) throw new Error('Instance Flowable introuvable')
    if (instance.status === 'Terminée') throw new Error('Une instance terminée ne peut pas être suspendue')
    instance.status = 'Suspendue'
    instance.updatedAt = new Date().toISOString()
    commit()
    return structuredClone(instance)
  },
  async resumeWorkflowInstance(id: string) {
    await wait()
    const instance = getDatabase().workflowInstances.find((entry) => entry.id === id)
    if (!instance) throw new Error('Instance Flowable introuvable')
    if (instance.status !== 'Suspendue') throw new Error('Cette instance n’est pas suspendue')
    instance.status = 'Active'
    instance.updatedAt = new Date().toISOString()
    commit()
    return structuredClone(instance)
  },
  async retryWorkflowIncident(id: string) {
    await wait(350)
    const instance = getDatabase().workflowInstances.find((entry) => entry.id === id)
    if (!instance) throw new Error('Instance Flowable introuvable')
    if (instance.status !== 'Incident' || !instance.incident) throw new Error('Aucun incident à relancer')
    instance.status = 'Active'
    instance.currentActivity = 'Revue juridique'
    instance.updatedAt = new Date().toISOString()
    delete instance.incident
    commit()
    return structuredClone(instance)
  },
  async getAgents() {
    await wait()
    return structuredClone(getDatabase().agents)
  },
  async getRoles() {
    await wait()
    return structuredClone(getDatabase().roles)
  },
  async getUsers() {
    await wait()
    return structuredClone(getDatabase().users)
  },
  async getDmnRules() {
    await wait()
    return structuredClone(getDatabase().dmnRules.sort((a,b) => b.updatedAt.localeCompare(a.updatedAt)))
  },
  async createDmnRule(input: Omit<DmnRule, 'id' | 'version' | 'updatedAt' | 'status'>) {
    await wait()
    const database = getDatabase()
    const key = input.key.trim().toLowerCase().replace(/[^a-z0-9-]+/g, '-')
    if (!key) throw new Error('La clé technique est obligatoire')
    if (database.dmnRules.some((entry) => entry.key === key)) throw new Error('Cette clé de décision existe déjà')
    const rule: DmnRule = { ...input, key, id:`DMN-RULE-${Date.now()}`, version:1, status:'Brouillon', updatedAt:new Date().toISOString() }
    database.dmnRules.unshift(rule)
    commit()
    return structuredClone(rule)
  },
  async updateDmnRule(id: string, changes: Pick<DmnRule, 'name' | 'description' | 'hit' | 'thresholds' | 'outputs'>) {
    await wait()
    const rule = getDatabase().dmnRules.find((entry) => entry.id === id)
    if (!rule) throw new Error('Décision introuvable')
    Object.assign(rule, structuredClone(changes), { version:rule.version + 1, status:'Brouillon', updatedAt:new Date().toISOString() })
    commit()
    return structuredClone(rule)
  },
  async activateDmnRule(id: string) {
    await wait()
    const rule = getDatabase().dmnRules.find((entry) => entry.id === id)
    if (!rule) throw new Error('Décision introuvable')
    rule.status = 'Active'
    rule.updatedAt = new Date().toISOString()
    commit()
    return structuredClone(rule)
  },
  async simulateDmnRule(ruleKey: string, input: DmnRuleInput) {
    await wait(300)
    const rule = getDatabase().dmnRules.find((entry) => entry.key === ruleKey)
    if (!rule) throw new Error('Décision introuvable')
    return evaluateDmnRule(rule, input)
  },
  async createUser(input: ApplicationUserInput) {
    await wait()
    const database = getDatabase()
    if (database.users.some((entry) => entry.email.toLowerCase() === input.email.trim().toLowerCase())) throw new Error('Cette adresse e-mail est déjà utilisée')
    if (!database.roles.some((entry) => entry.id === input.roleId)) throw new Error('Rôle introuvable')
    const user = {
      ...input,
      id:`USR-${String(database.users.length + 1).padStart(3,'0')}`,
      email:input.email.trim().toLowerCase(),
      tenantName:'Banque Démo',
      tenantId:'tenant-demo',
    }
    database.users.push(user)
    commit()
    return structuredClone(user)
  },
  async updateUser(id: string, changes: Partial<ApplicationUserInput>) {
    await wait()
    const database = getDatabase()
    const user = database.users.find((entry) => entry.id === id)
    if (!user) throw new Error('Utilisateur introuvable')
    if (changes.email && database.users.some((entry) => entry.id !== id && entry.email.toLowerCase() === changes.email?.trim().toLowerCase())) throw new Error('Cette adresse e-mail est déjà utilisée')
    if (changes.roleId && !database.roles.some((entry) => entry.id === changes.roleId)) throw new Error('Rôle introuvable')
    Object.assign(user, changes, changes.email ? { email:changes.email.trim().toLowerCase() } : {})
    commit()
    return structuredClone(user)
  },
  async createAgent(input: AgentInput) {
    await wait()
    const agent = { ...input, id:`AG-${String(getDatabase().agents.length + 1).padStart(2,'0')}`, activeCases:0 }
    getDatabase().agents.push(agent)
    commit()
    return structuredClone(agent)
  },
  async updateAgent(id: string, changes: Partial<AgentInput>) {
    await wait()
    const agent = getDatabase().agents.find((entry) => entry.id === id)
    if (!agent) throw new Error('Agent introuvable')
    Object.assign(agent, changes)
    if (agent.activeCases > agent.capacity) throw new Error('La capacité ne peut pas être inférieure à la charge actuelle')
    commit()
    return structuredClone(agent)
  },
  async getPayments() {
    await wait()
    return structuredClone(getDatabase().payments.sort((a,b) => b.receivedAt.localeCompare(a.receivedAt)))
  },
  async getBankTransactions() {
    await wait()
    return structuredClone(getDatabase().bankTransactions.sort((a,b) => b.receivedAt.localeCompare(a.receivedAt)))
  },
  async reconcileBankTransaction(id: string, input: { decision: 'Rapprocher' | 'Rejeter'; caseId?: string; actor: string; reason?: string }) {
    await wait(350)
    const database = getDatabase()
    const transaction = database.bankTransactions.find((entry) => entry.id === id)
    if (!transaction) throw new Error('Transaction bancaire introuvable')
    if (transaction.status !== 'À rapprocher') throw new Error('Cette transaction a déjà été traitée')
    const now = new Date().toISOString()
    if (input.decision === 'Rejeter') {
      if (!input.reason?.trim()) throw new Error('Le motif de rejet est obligatoire')
      transaction.status = 'Rejeté'
      transaction.rejectionReason = input.reason.trim()
      transaction.reconciledAt = now
      transaction.reconciledBy = input.actor
      commit()
      return structuredClone(transaction)
    }
    if (!input.caseId) throw new Error('Le dossier d’imputation est obligatoire')
    const item = database.cases.find((entry) => entry.id === input.caseId)
    if (!item) throw new Error('Dossier introuvable')
    const paymentId = `PAY-${Date.now()}`
    database.payments.unshift({ id:paymentId, caseId:item.id, customerName:item.customerName, amount:transaction.amount, receivedAt:transaction.valueDate, method:'Virement', reference:transaction.bankReference, status:'Confirmé', reconciledAt:now, reconciledBy:input.actor })
    item.amount = Math.max(0, item.amount - transaction.amount)
    item.status = item.amount === 0 ? 'Promesse' : 'En cours'
    item.nextAction = item.amount === 0 ? 'Clôturer le dossier' : 'Poursuivre le recouvrement'
    const promise = database.promises.find((entry) => entry.caseId === item.id && entry.status === 'Active' && transaction.amount >= entry.amount)
    if (promise) promise.status = 'Honorée'
    const plan = database.paymentPlans.find((entry) => entry.caseId === item.id && (entry.status === 'Actif' || entry.status === 'En retard'))
    if (plan) {
      let remaining = transaction.amount
      for (const installment of plan.installments.filter((entry) => entry.status !== 'Payée')) {
        if (remaining <= 0) break
        const allocation = Math.min(remaining, installment.amount - installment.paidAmount)
        installment.paidAmount += allocation
        remaining -= allocation
        installment.status = installment.paidAmount >= installment.amount ? 'Payée' : 'Partielle'
        if (installment.status === 'Payée') installment.paidAt = now
      }
      plan.status = plan.installments.every((entry) => entry.status === 'Payée') ? 'Honoré' : 'Actif'
    }
    transaction.status = 'Rapproché'
    transaction.paymentId = paymentId
    transaction.reconciledAt = now
    transaction.reconciledBy = input.actor
    database.audit.unshift({ id:`AUD-${Date.now()}`, caseId:item.id, type:'PAYMENT_RECONCILED', description:`Virement ${transaction.bankReference} rapproché pour ${transaction.amount.toLocaleString('fr-FR')} MAD`, actor:input.actor, occurredAt:now })
    if (item.amount === 0) updateWorkflow(item.id, { status:'Terminée', currentActivity:'Dossier clôturé', completedAt:now })
    else updateWorkflow(item.id, { currentActivity:'Poursuite du recouvrement' })
    commit()
    return structuredClone(transaction)
  },
  async getPaymentPlans(caseId?: string) {
    await wait()
    const now = new Date()
    const plans = caseId ? getDatabase().paymentPlans.filter((plan) => plan.caseId === caseId) : getDatabase().paymentPlans
    plans.forEach((plan) => {
      plan.installments.forEach((installment) => {
        if (installment.status !== 'Payée' && new Date(installment.dueAt) < now) installment.status = installment.paidAmount ? 'Partielle' : 'Échue'
      })
      if (plan.status !== 'Annulé' && plan.status !== 'Honoré') plan.status = plan.installments.some((item) => item.status === 'Échue' || (item.status === 'Partielle' && new Date(item.dueAt) < now)) ? 'En retard' : 'Actif'
    })
    commit()
    return structuredClone(plans.sort((a,b) => b.createdAt.localeCompare(a.createdAt)))
  },
  async createPaymentPlan(input: PaymentPlanInput) {
    await wait(350)
    const database = getDatabase()
    const item = database.cases.find((entry) => entry.id === input.caseId)
    if (!item) throw new Error('Dossier introuvable')
    if (input.totalAmount <= 0 || input.totalAmount > item.amount) throw new Error('Le montant du plan doit être compris dans l’encours')
    if (input.installmentCount < 2 || input.installmentCount > 24) throw new Error('Le nombre d’échéances doit être compris entre 2 et 24')
    const baseAmount = Math.floor(input.totalAmount / input.installmentCount)
    const firstDate = new Date(input.firstDueAt)
    const planId = `PLAN-${Date.now()}`
    const installments = Array.from({ length:input.installmentCount }, (_, index) => {
      const dueAt = new Date(firstDate)
      if (input.frequency === 'Mensuelle') dueAt.setMonth(dueAt.getMonth() + index)
      else dueAt.setDate(dueAt.getDate() + index * 7)
      const amount = index === input.installmentCount - 1 ? input.totalAmount - baseAmount * index : baseAmount
      return { id:`${planId}-${index + 1}`, dueAt:dueAt.toISOString(), amount, paidAmount:0, status:'À venir' as const }
    })
    const createdAt = new Date().toISOString()
    const plan = { ...input, id:planId, customerName:item.customerName, status:'Actif' as const, createdAt, installments }
    database.paymentPlans.unshift(plan)
    installments.forEach((installment, index) => database.tasks.push({ id:`TASK-${Date.now()}-${index}`, caseId:item.id, name:`Suivre l’échéance ${index + 1}/${installments.length}`, assignee:item.assignee ?? input.createdBy, dueAt:installment.dueAt, status:'Prête' }))
    database.audit.unshift({ id:`AUD-${Date.now()}`, caseId:item.id, type:'PAYMENT_PLAN', description:`Échéancier de ${input.totalAmount.toLocaleString('fr-FR')} MAD sur ${input.installmentCount} échéances`, actor:input.createdBy, occurredAt:createdAt })
    item.status = 'Promesse'
    item.nextAction = 'Suivre l’échéancier de règlement'
    updateWorkflow(item.id, { currentActivity:'Suivi de l’échéancier' })
    commit()
    return structuredClone(plan)
  },
  async getPromises() {
    await wait()
    const now = new Date()
    getDatabase().promises.forEach((promise) => {
      if (promise.status === 'Active' && new Date(promise.promisedAt) < now) promise.status = 'Rompue'
    })
    commit()
    return structuredClone(getDatabase().promises.sort((a,b) => a.promisedAt.localeCompare(b.promisedAt)))
  },
  async getOperationalAlerts(): Promise<OperationalAlert[]> {
    await wait()
    const database = getDatabase()
    const now = new Date()
    const alerts: OperationalAlert[] = []
    database.tasks.filter((task) => task.status !== 'Terminée').forEach((task) => {
      const due = new Date(task.dueAt)
      const overdue = due.getTime() < now.getTime()
      const within24Hours = due.getTime() - now.getTime() <= 24 * 60 * 60 * 1000
      if (!overdue && !within24Hours) return
      const item = database.cases.find((entry) => entry.id === task.caseId)
      const id = `ALERT-TASK-${task.id}`
      alerts.push({
        id,
        severity:overdue ? 'Critique' : 'Attention',
        category:'Tâche',
        title:overdue ? 'Tâche Flowable en retard' : 'Échéance dans moins de 24 heures',
        description:`${task.name} · ${item?.customerName ?? task.caseId} · ${task.assignee}`,
        caseId:task.caseId,
        route:`/cases/${task.caseId}`,
        occurredAt:task.dueAt,
        read:database.notificationReads.includes(id),
      })
    })
    database.cases.filter((item) => item.risk === 'Critique' || (item.risk === 'Élevé' && item.overdueDays >= 90)).forEach((item) => {
      const id = `ALERT-CASE-${item.id}`
      alerts.push({
        id,
        severity:item.risk === 'Critique' ? 'Critique' : 'Attention',
        category:'Dossier',
        title:item.risk === 'Critique' ? 'Dossier à risque critique' : 'Retard supérieur à 90 jours',
        description:`${item.customerName} · ${item.amount.toLocaleString('fr-FR')} MAD · ${item.nextAction}`,
        caseId:item.id,
        route:`/cases/${item.id}`,
        occurredAt:new Date(now.getFullYear(), now.getMonth(), now.getDate(), 8).toISOString(),
        read:database.notificationReads.includes(id),
      })
    })
    database.promises.filter((promise) => promise.status === 'Rompue' || (promise.status === 'Active' && new Date(promise.promisedAt) < now)).forEach((promise) => {
      const id = `ALERT-PROMISE-${promise.id}`
      alerts.push({
        id,
        severity:'Critique',
        category:'Promesse',
        title:'Promesse de paiement non honorée',
        description:`${promise.customerName} · ${promise.amount.toLocaleString('fr-FR')} MAD`,
        caseId:promise.caseId,
        route:`/cases/${promise.caseId}`,
        occurredAt:promise.promisedAt,
        read:database.notificationReads.includes(id),
      })
    })
    const severityOrder = { Critique:0, Attention:1, Information:2 }
    return structuredClone(alerts.sort((a,b) => severityOrder[a.severity] - severityOrder[b.severity] || b.occurredAt.localeCompare(a.occurredAt)))
  },
  async markAlertRead(id: string) {
    await wait(80)
    const database = getDatabase()
    if (!database.notificationReads.includes(id)) database.notificationReads.push(id)
    commit()
  },
  async markAllAlertsRead(ids: string[]) {
    await wait(80)
    const database = getDatabase()
    database.notificationReads = [...new Set([...database.notificationReads, ...ids])]
    commit()
  },
  async getCommunications(caseId?: string) {
    await wait()
    const values = caseId ? getDatabase().communications.filter((item) => item.caseId === caseId) : getDatabase().communications
    return structuredClone(values.sort((a,b) => b.sentAt.localeCompare(a.sentAt)))
  },
  async getDocuments(caseId?: string) {
    await wait()
    const values = caseId ? getDatabase().documents.filter((item) => item.caseId === caseId) : getDatabase().documents
    return structuredClone(values.sort((a,b) => b.uploadedAt.localeCompare(a.uploadedAt)))
  },
  async getLegalProcedures(caseId?: string) {
    await wait()
    const values = caseId ? getDatabase().legalProcedures.filter((item) => item.caseId === caseId) : getDatabase().legalProcedures
    return structuredClone(values.sort((a,b) => (a.nextHearingAt ?? '9999').localeCompare(b.nextHearingAt ?? '9999')))
  },
  async createLegalProcedure(input: LegalProcedureInput) {
    await wait(350)
    const database = getDatabase()
    const item = database.cases.find((entry) => entry.id === input.caseId)
    if (!item) throw new Error('Dossier introuvable')
    if (database.legalProcedures.some((entry) => entry.caseId === input.caseId && entry.status !== 'Clôturée')) throw new Error('Une procédure active existe déjà pour ce dossier')
    const openedAt = new Date().toISOString()
    const procedure = { ...input, id:`LEGAL-${Date.now()}`, customerName:item.customerName, status:'Préparation' as const, openedAt, events:[] }
    database.legalProcedures.unshift(procedure)
    item.status = 'Juridique'
    item.segment = 'Recouvrement juridique'
    item.nextAction = 'Préparer le dépôt de la requête'
    database.tasks.push({ id:`TASK-${Date.now()}`, caseId:item.id, name:'Préparer le dossier contentieux', assignee:item.assignee ?? input.openedBy, dueAt:new Date(Date.now() + 3 * 86400000).toISOString(), status:'Prête' })
    database.audit.unshift({ id:`AUD-${Date.now()}`, caseId:item.id, type:'LEGAL_OPENED', description:`Procédure juridique ouverte · ${input.jurisdiction}`, actor:input.openedBy, occurredAt:openedAt })
    updateWorkflow(item.id, { currentActivity:'Préparation du contentieux' })
    commit()
    return structuredClone(procedure)
  },
  async addLegalEvent(procedureId: string, input: { type: LegalEventType; title: string; scheduledAt: string; notes?: string; createdBy: string }) {
    await wait()
    const database = getDatabase()
    const procedure = database.legalProcedures.find((entry) => entry.id === procedureId)
    if (!procedure) throw new Error('Procédure introuvable')
    const createdAt = new Date().toISOString()
    const event = { id:`LEV-${Date.now()}`, ...input, completed:false, createdAt }
    procedure.events.push(event)
    if (input.type === 'Audience') {
      procedure.status = 'Audience planifiée'
      procedure.nextHearingAt = input.scheduledAt
    }
    procedure.legalFees += 0
    database.tasks.push({ id:`TASK-${Date.now()}`, caseId:procedure.caseId, name:input.title, assignee:procedure.lawyer, dueAt:input.scheduledAt, status:'Prête' })
    database.audit.unshift({ id:`AUD-${Date.now()}`, caseId:procedure.caseId, type:'LEGAL_EVENT', description:`${input.type} planifié : ${input.title}`, actor:input.createdBy, occurredAt:createdAt })
    updateWorkflow(procedure.caseId, { currentActivity:input.type === 'Audience' ? 'Attente audience' : `Suivi juridique · ${input.type}` })
    commit()
    return structuredClone(event)
  },
  async updateLegalProcedure(id: string, input: { status: LegalProcedureStatus; legalFees: number; actor: string }) {
    await wait()
    const database = getDatabase()
    const procedure = database.legalProcedures.find((entry) => entry.id === id)
    if (!procedure) throw new Error('Procédure introuvable')
    procedure.status = input.status
    procedure.legalFees = Math.max(0,input.legalFees)
    const now = new Date().toISOString()
    database.audit.unshift({ id:`AUD-${Date.now()}`, caseId:procedure.caseId, type:'LEGAL_STATUS', description:`Procédure juridique : ${input.status}`, actor:input.actor, occurredAt:now })
    if (input.status === 'Clôturée') updateWorkflow(procedure.caseId, { currentActivity:'Procédure juridique clôturée' })
    else updateWorkflow(procedure.caseId, { currentActivity:`Juridique · ${input.status}` })
    commit()
    return structuredClone(procedure)
  },
  async addDocument(input: CaseDocumentInput) {
    await wait(350)
    const database = getDatabase()
    const item = database.cases.find((entry) => entry.id === input.caseId)
    if (!item) throw new Error('Dossier introuvable')
    if (!input.name.trim()) throw new Error('Le nom du document est obligatoire')
    if (input.size > 10 * 1024 * 1024) throw new Error('La taille maximale autorisée est de 10 Mo')
    const uploadedAt = new Date().toISOString()
    const sameName = database.documents.filter((entry) => entry.caseId === input.caseId && entry.name === input.name)
    const document = {
      ...input,
      id:`DOC-${Date.now()}`,
      customerName:item.customerName,
      version:Math.max(0, ...sameName.map((entry) => entry.version)) + 1,
      status:'À contrôler' as const,
      storageKey:`${database.users[0]?.tenantId ?? 'tenant'}/${input.caseId}/${Date.now()}-${input.name}`,
      checksum:`sha256:${Math.abs([...input.name].reduce((hash,char) => (hash * 31 + char.charCodeAt(0)) | 0, input.size)).toString(16)}`,
      uploadedAt,
    }
    database.documents.unshift(document)
    database.audit.unshift({ id:`AUD-${Date.now()}`, caseId:item.id, type:'DOCUMENT_ADDED', description:`Document ajouté : ${document.name} (${document.category})`, actor:input.uploadedBy, occurredAt:uploadedAt })
    commit()
    return structuredClone(document)
  },
  async reviewDocument(id: string, input: { status: 'Validé' | 'Rejeté'; actor: string; rejectionReason?: string }) {
    await wait()
    const database = getDatabase()
    const document = database.documents.find((entry) => entry.id === id)
    if (!document) throw new Error('Document introuvable')
    if (input.status === 'Rejeté' && !input.rejectionReason?.trim()) throw new Error('Le motif de rejet est obligatoire')
    document.status = input.status
    document.validatedAt = new Date().toISOString()
    document.validatedBy = input.actor
    document.rejectionReason = input.status === 'Rejeté' ? input.rejectionReason?.trim() : undefined
    database.audit.unshift({ id:`AUD-${Date.now()}`, caseId:document.caseId, type:'DOCUMENT_REVIEWED', description:`Document ${input.status.toLowerCase()} : ${document.name}`, actor:input.actor, occurredAt:document.validatedAt })
    commit()
    return structuredClone(document)
  },
  async getMessageTemplates(channel?: CommunicationChannel) {
    await wait()
    return structuredClone(channel ? getDatabase().templates.filter((item) => item.channel === channel) : getDatabase().templates)
  },
  async sendCommunication(input: { caseId: string; channel: CommunicationChannel; recipient: string; subject?: string; content: string; sentBy: string }) {
    await wait(450)
    const item = getDatabase().cases.find((entry) => entry.id === input.caseId)
    if (!item) throw new Error('Dossier introuvable')
    if (!input.recipient.trim() || !input.content.trim()) throw new Error('Destinataire et contenu obligatoires')
    const sentAt = new Date().toISOString()
    const communication = { id:`COM-${Date.now()}`, customerName:item.customerName, status:'Livré' as const, sentAt, ...input }
    getDatabase().communications.unshift(communication)
    getDatabase().audit.unshift({ id:`AUD-${Date.now()}`, caseId:item.id, type:'COMMUNICATION', description:`${input.channel} envoyé à ${input.recipient}`, actor:input.sentBy, occurredAt:sentAt })
    item.status = item.status === 'À traiter' ? 'En cours' : item.status
    item.nextAction = input.channel === 'SMS' ? 'Attendre le retour du client' : 'Suivre la relance e-mail'
    commit()
    return structuredClone(communication)
  },
  async getNotes(caseId: string): Promise<CaseNote[]> {
    await wait()
    return structuredClone(getDatabase().notes.filter((item) => item.caseId === caseId))
  },
  async getAudit(caseId: string) {
    await wait()
    return structuredClone(getDatabase().audit.filter((item) => item.caseId === caseId).sort((a,b) => b.occurredAt.localeCompare(a.occurredAt)))
  },
  async addNote(caseId: string, content: string, author: string): Promise<CaseNote> {
    await wait()
    const note = { id:`NOTE-${Date.now()}`, caseId, content, author, createdAt:new Date().toISOString() }
    getDatabase().notes.unshift(note)
    getDatabase().audit.unshift({ id:`AUD-${Date.now()}`, caseId, type:'NOTE_ADDED', description:'Note ajoutée au dossier', actor:author, occurredAt:note.createdAt })
    commit()
    return structuredClone(note)
  },
  async updateCase(id: string, changes: Partial<CollectionCase>): Promise<CollectionCase> {
    await wait()
    const item = getDatabase().cases.find((entry) => entry.id === id)
    if (!item) throw new Error('Dossier introuvable')
    Object.assign(item, changes)
    commit()
    return structuredClone(item)
  },
  async executeDecision(caseId: string): Promise<DecisionTrace> {
    await wait(350)
    const item = getDatabase().cases.find((entry) => entry.id === caseId)
    if (!item) throw new Error('Dossier introuvable')
    const rule = getDatabase().dmnRules.find((entry) => entry.key === 'customer-segmentation' && entry.status === 'Active')
    if (!rule) throw new Error('Aucune version active de customer-segmentation')
    const evaluation = evaluateDmnRule(rule, item)
    const trace: DecisionTrace = { id:`DMN-${Date.now()}`, caseId, decisionKey:rule.key, version:rule.version, result:evaluation.result, reasons:evaluation.reasons, executedAt:new Date().toISOString() }
    const index = getDatabase().decisions.findIndex((entry) => entry.caseId === caseId)
    if (index >= 0) getDatabase().decisions[index] = trace
    else getDatabase().decisions.push(trace)
    item.segment = evaluation.result === rule.outputs.legal ? 'Recouvrement juridique' : evaluation.result === rule.outputs.negotiation ? 'Négociation renforcée' : 'Relance amiable'
    updateWorkflow(caseId, { currentActivity:'Affectation automatique' })
    commit()
    return structuredClone(trace)
  },
  async simulateDecision(input: Pick<CollectionCase, 'amount' | 'overdueDays' | 'probabilityOfDefault' | 'guaranteeCoverage'>) {
    return this.simulateDmnRule('customer-segmentation', input)
  },
  async assignCase(caseId: string): Promise<AssignmentResult> {
    await wait(350)
    const item = getDatabase().cases.find((entry) => entry.id === caseId)
    if (!item) throw new Error('Dossier introuvable')
    const eligible = getDatabase().agents.filter((agent) => agent.available && agent.activeCases < agent.capacity)
    if (!eligible.length) throw new Error('Aucun agent disponible')
    const scored = eligible.map((agent) => {
      const loadScore = (1 - agent.activeCases / agent.capacity) * 60
      const skillScore = item.segment.includes('juridique') && agent.skills.includes('Juridique') ? 40 : item.segment.includes('Négociation') && agent.skills.includes('Négociation') ? 40 : 15
      return { agent, score:Math.round(loadScore + skillScore) }
    }).sort((a,b) => b.score - a.score)
    const selected = scored[0]
    if (!selected) throw new Error('Aucun agent éligible')
    selected.agent.activeCases += 1
    item.assignee = selected.agent.name
    item.status = 'En cours'
    item.nextAction = item.segment.includes('juridique') ? 'Revue juridique' : 'Prise de contact'
    updateWorkflow(caseId, { currentActivity:item.segment.includes('juridique') ? 'Revue juridique' : 'Traitement agent' })
    commit()
    return { caseId, agentId:selected.agent.id, agentName:selected.agent.name, score:selected.score, reasons:[`Capacité disponible : ${selected.agent.capacity - selected.agent.activeCases + 1}`,`Compétences : ${selected.agent.skills.join(', ')}`] }
  },
  async completeTask(taskId: string): Promise<WorkflowTask> {
    await wait()
    const task = getDatabase().tasks.find((entry) => entry.id === taskId)
    if (!task) throw new Error('Tâche introuvable')
    task.status = 'Terminée'
    const openTasks = getDatabase().tasks.filter((entry) => entry.caseId === task.caseId && entry.status !== 'Terminée')
    if (!openTasks.length) updateWorkflow(task.caseId, { currentActivity:'Attente résolution' })
    commit()
    return structuredClone(task)
  },
  async executeCaseAction(caseId: string, input: CaseActionInput, actor: string): Promise<CaseActionResult> {
    await wait(300)
    const item = getDatabase().cases.find((entry) => entry.id === caseId)
    if (!item) throw new Error('Dossier introuvable')
    const now = new Date().toISOString()
    let description = input.comment
    let task: WorkflowTask | undefined
    if (input.type === 'CONTACT') {
      item.status = 'En cours'
      item.nextAction = 'Suivi du contact'
      description = `Contact enregistré : ${input.comment}`
      updateWorkflow(caseId, { currentActivity:'Suivi du contact' })
    }
    if (input.type === 'PROMISE') {
      item.status = 'Promesse'
      item.nextAction = 'Contrôler le paiement promis'
      description = `Promesse de ${input.amount?.toLocaleString('fr-FR')} MAD pour le ${input.promisedAt}`
      task = { id:`TASK-${Date.now()}`, caseId, name:'Contrôler le paiement promis', assignee:item.assignee ?? actor, dueAt:input.promisedAt ? new Date(input.promisedAt).toISOString() : now, status:'Prête' }
      getDatabase().tasks.push(task)
      getDatabase().promises.unshift({ id:`PROM-${Date.now()}`, caseId, customerName:item.customerName, amount:Math.max(0,input.amount ?? 0), promisedAt:input.promisedAt ?? now, createdAt:now, status:'Active' })
      updateWorkflow(caseId, { currentActivity:'Attente paiement promis' })
    }
    if (input.type === 'PAYMENT') {
      const amount = Math.max(0, input.amount ?? 0)
      item.amount = Math.max(0, item.amount - amount)
      item.status = item.amount === 0 ? 'Promesse' : 'En cours'
      item.nextAction = item.amount === 0 ? 'Clôturer le dossier' : 'Poursuivre le recouvrement'
      description = `Paiement de ${amount.toLocaleString('fr-FR')} MAD enregistré. Solde : ${item.amount.toLocaleString('fr-FR')} MAD`
      getDatabase().payments.unshift({ id:`PAY-${Date.now()}`, caseId, customerName:item.customerName, amount, receivedAt:now, method:input.paymentMethod ?? 'Virement', reference:input.paymentReference?.trim() || `AUTO-${Date.now()}`, status:input.paymentReference?.trim() ? 'Confirmé' : 'À rapprocher' })
      const promise = getDatabase().promises.find((entry) => entry.caseId === caseId && entry.status === 'Active' && amount >= entry.amount)
      if (promise) promise.status = 'Honorée'
      const plan = getDatabase().paymentPlans.find((entry) => entry.caseId === caseId && (entry.status === 'Actif' || entry.status === 'En retard'))
      if (plan) {
        let remaining = amount
        for (const installment of plan.installments.filter((entry) => entry.status !== 'Payée')) {
          if (remaining <= 0) break
          const allocation = Math.min(remaining, installment.amount - installment.paidAmount)
          installment.paidAmount += allocation
          remaining -= allocation
          installment.status = installment.paidAmount >= installment.amount ? 'Payée' : 'Partielle'
          if (installment.status === 'Payée') installment.paidAt = now
        }
        plan.status = plan.installments.every((entry) => entry.status === 'Payée') ? 'Honoré' : 'Actif'
      }
      if (item.amount === 0) updateWorkflow(caseId, { status:'Terminée', currentActivity:'Dossier clôturé', completedAt:now })
      else updateWorkflow(caseId, { currentActivity:'Poursuite du recouvrement' })
    }
    if (input.type === 'ESCALATION') {
      item.status = 'Juridique'
      item.segment = 'Recouvrement juridique'
      item.nextAction = 'Revue juridique'
      description = `Escalade juridique : ${input.comment}`
      task = { id:`TASK-${Date.now()}`, caseId, name:'Valider la stratégie juridique', assignee:item.assignee ?? actor, dueAt:now, status:'Prête' }
      getDatabase().tasks.push(task)
      if (!getDatabase().legalProcedures.some((entry) => entry.caseId === caseId && entry.status !== 'Clôturée')) {
        getDatabase().legalProcedures.unshift({ id:`LEGAL-${Date.now()}`, caseId, customerName:item.customerName, jurisdiction:'À déterminer', courtReference:'BROUILLON', lawyer:'À affecter', claimAmount:item.amount, legalFees:0, status:'Préparation', openedAt:now, openedBy:actor, events:[] })
      }
      updateWorkflow(caseId, { currentActivity:'Revue juridique' })
    }
    const audit = { id:`AUD-${Date.now()}`, caseId, type:input.type, description, actor, occurredAt:now }
    getDatabase().audit.unshift(audit)
    commit()
    return structuredClone({ case:item, audit, task })
  },
  async createCase(input: Omit<CollectionCase, 'id' | 'status' | 'segment' | 'risk' | 'assignee' | 'nextAction'>): Promise<CollectionCase> {
    await wait()
    const item: CollectionCase = { ...input, id:`DC-2026-${String(getDatabase().cases.length + 1).padStart(3,'0')}`, status:'À traiter', segment:'À déterminer', risk:input.probabilityOfDefault >= 80 ? 'Critique' : input.probabilityOfDefault >= 60 ? 'Élevé' : input.probabilityOfDefault >= 35 ? 'Modéré' : 'Faible', assignee:null, nextAction:'Exécuter la segmentation' }
    getDatabase().cases.unshift(item)
    const now = new Date().toISOString()
    getDatabase().workflowInstances.unshift({ id:`PROC-${Date.now()}`, caseId:item.id, customerName:item.customerName, processDefinitionKey:'debt-collection', processDefinitionVersion:4, businessKey:item.id, status:'Active', currentActivity:'Segmentation DMN', startedAt:now, updatedAt:now })
    commit()
    return structuredClone(item)
  },
  async reset() {
    await wait()
    resetDatabase()
  },
}

import type { Agent, CollectionCase, DecisionTrace, WorkflowTask } from '@/types/domain'

export const cases: CollectionCase[] = [
  { id:'DC-2026-001', customerName:'Atlas Distribution', customerCode:'CL-10482', contractId:'CT-88421', amount:486000, overdueDays:118, status:'Juridique', segment:'Recouvrement juridique', risk:'Critique', assignee:'Yasmine Benali', nextAction:'Revue juridique', phone:'+212 522 44 18 10', email:'finance@atlas.ma', probabilityOfDefault:86, guaranteeCoverage:22 },
  { id:'DC-2026-002', customerName:'Nadia El Mansouri', customerCode:'CL-20991', contractId:'CT-90117', amount:73500, overdueDays:64, status:'En cours', segment:'Négociation renforcée', risk:'Élevé', assignee:'Karim Alaoui', nextAction:'Appel de négociation', phone:'+212 661 22 34 08', email:'nadia.m@example.ma', probabilityOfDefault:68, guaranteeCoverage:44 },
  { id:'DC-2026-003', customerName:'Riad Services', customerCode:'CL-30042', contractId:'CT-77410', amount:128000, overdueDays:37, status:'Promesse', segment:'Suivi promesse', risk:'Modéré', assignee:'Salma Idrissi', nextAction:'Contrôle du paiement', phone:'+212 537 61 72 19', email:'contact@riadservices.ma', probabilityOfDefault:42, guaranteeCoverage:81 },
  { id:'DC-2026-004', customerName:'Omar Berrada', customerCode:'CL-44108', contractId:'CT-66102', amount:28900, overdueDays:18, status:'À traiter', segment:'Relance amiable', risk:'Faible', assignee:null, nextAction:'Affectation automatique', phone:'+212 667 91 04 23', email:'omar.b@example.ma', probabilityOfDefault:21, guaranteeCoverage:100 },
  { id:'DC-2026-005', customerName:'Méditerranée Textile', customerCode:'CL-55181', contractId:'CT-55088', amount:264000, overdueDays:92, status:'En cours', segment:'Négociation renforcée', risk:'Élevé', assignee:'Yasmine Benali', nextAction:'Analyse des garanties', phone:'+212 539 31 72 60', email:'daf@medtext.ma', probabilityOfDefault:74, guaranteeCoverage:57 },
]

export const decisions: DecisionTrace[] = [
  { id:'DMN-991', caseId:'DC-2026-001', decisionKey:'customer-segmentation', version:3, result:'RECOUVREMENT_JURIDIQUE', reasons:['Retard supérieur à 90 jours','Probabilité de défaut supérieure à 80 %','Couverture de garantie inférieure à 30 %'], executedAt:'2026-06-19T09:42:00' },
  { id:'DMN-992', caseId:'DC-2026-002', decisionKey:'customer-segmentation', version:3, result:'NEGOCIATION_RENFORCEE', reasons:['Encours supérieur à 50 000 MAD','Risque client élevé'], executedAt:'2026-06-19T09:44:00' },
]

export const tasks: WorkflowTask[] = [
  { id:'TASK-71', caseId:'DC-2026-001', name:'Valider la stratégie juridique', assignee:'Yasmine Benali', dueAt:'2026-06-20T12:00:00', status:'En cours' },
  { id:'TASK-72', caseId:'DC-2026-002', name:'Appeler le client', assignee:'Karim Alaoui', dueAt:'2026-06-20T15:30:00', status:'Prête' },
  { id:'TASK-73', caseId:'DC-2026-003', name:'Contrôler le paiement promis', assignee:'Salma Idrissi', dueAt:'2026-06-21T10:00:00', status:'Prête' },
]

export const agents: Agent[] = [
  { id:'AG-01', name:'Yasmine Benali', team:'Senior', capacity:18, activeCases:16, skills:['Juridique','Garanties'], available:true },
  { id:'AG-02', name:'Karim Alaoui', team:'Négociation', capacity:24, activeCases:17, skills:['Négociation','PME'], available:true },
  { id:'AG-03', name:'Salma Idrissi', team:'Amiable', capacity:28, activeCases:21, skills:['Particuliers','Promesses'], available:true },
  { id:'AG-04', name:'Mehdi Tazi', team:'Amiable', capacity:28, activeCases:28, skills:['Particuliers'], available:false },
]

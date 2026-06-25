import { createRouter, createWebHistory } from 'vue-router'
import DashboardView from '@/views/DashboardView.vue'
import { useSessionStore } from '@/stores/session'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/cases', name: 'cases', component: () => import('@/views/CasesView.vue') },
    { path: '/cases/:id', name: 'case-detail', component: () => import('@/views/CaseDetailView.vue') },
    { path: '/tasks', name: 'tasks', component: () => import('@/views/TasksView.vue') },
    { path: '/workflows', name: 'workflows', component: () => import('@/views/WorkflowsView.vue') },
    { path: '/payments', name: 'payments', component: () => import('@/views/PaymentsView.vue') },
    { path: '/reports', name: 'reports', component: () => import('@/views/ReportsView.vue') },
    { path: '/organization', name: 'organization', component: () => import('@/views/OrganizationView.vue') },
    { path: '/communications', name: 'communications', component: () => import('@/views/CommunicationsView.vue') },
    { path: '/documents', name: 'documents', component: () => import('@/views/DocumentsView.vue') },
    { path: '/legal', name: 'legal', component: () => import('@/views/LegalView.vue') },
    { path: '/rules', name: 'rules', component: () => import('@/views/RulesView.vue') },
    { path: '/assignments', name: 'assignments', component: () => import('@/views/AssignmentsView.vue') },
    { path: '/access', name: 'access', component: () => import('@/views/AccessView.vue'), meta:{ permission:'access:manage' } },
    { path: '/notifications', name: 'notifications', component: () => import('@/views/NotificationsView.vue') },
    { path: '/:pathMatch(.*)*', component: () => import('@/views/NotFoundView.vue') },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach(async (to) => {
  const permission = to.meta.permission
  if (typeof permission !== 'string') return true
  const session = useSessionStore()
  await session.initialize()
  return session.can(permission) ? true : '/'
})

export default router

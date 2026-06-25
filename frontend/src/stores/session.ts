import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { api } from '@/services/api'

interface AuthenticatedUser {
  id: string
  displayName: string
  role: string
  tenantName: string
  tenantId: string
  initials: string
  permissions: string[]
}

export const useSessionStore = defineStore('session', () => {
  const user = ref<AuthenticatedUser>({
    id: 'USR-001',
    displayName: 'Oussama El Amrani',
    role: 'Administrateur plateforme',
    tenantName: 'Banque Démo',
    tenantId: 'tenant-demo',
    initials: 'OE',
    permissions: ['case:create', 'case:update', 'decision:execute', 'assignment:execute', 'task:complete', 'rule:manage', 'workflow:manage', 'document:manage', 'payment:reconcile', 'legal:manage', 'organization:manage', 'communication:send', 'access:manage'],
  })
  const initialized = ref(false)

  const firstName = computed(() => user.value.displayName.split(' ')[0])
  const can = (permission: string) => user.value.permissions.includes(permission)
  async function initialize() {
    if (initialized.value) return
    const [users, roles] = await Promise.all([api.getUsers(), api.getRoles()])
    const account = users.find((entry) => entry.id === user.value.id && entry.active)
    const role = roles.find((entry) => entry.id === account?.roleId)
    if (account && role) {
      user.value = {
        id:account.id,
        displayName:account.displayName,
        role:role.name,
        tenantName:account.tenantName,
        tenantId:account.tenantId,
        initials:account.displayName.split(' ').map((part) => part[0]).join('').slice(0,2).toUpperCase(),
        permissions:[...role.permissions],
      }
    }
    initialized.value = true
  }

  return { user, firstName, can, initialize }
})

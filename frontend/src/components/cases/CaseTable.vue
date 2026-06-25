<script setup lang="ts">
import type { CollectionCase } from '@/types/domain'
import StatusBadge from '@/components/ui/StatusBadge.vue'

defineProps<{ items: CollectionCase[] }>()
const currency = new Intl.NumberFormat('fr-MA', { style:'currency', currency:'MAD', maximumFractionDigits:0 })
</script>

<template>
  <div class="table-shell">
    <table>
      <thead><tr><th>Dossier / client</th><th>Encours</th><th>Retard</th><th>Segment</th><th>Affectataire</th><th>Statut</th><th></th></tr></thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td><RouterLink :to="`/cases/${item.id}`" class="primary-cell"><b>{{ item.customerName }}</b><span>{{ item.id }} · {{ item.customerCode }}</span></RouterLink></td>
          <td><b>{{ currency.format(item.amount) }}</b></td>
          <td><span class="delay" :class="{ critical:item.overdueDays > 90 }">{{ item.overdueDays }} j</span></td>
          <td><span>{{ item.segment }}</span><small class="risk">{{ item.risk }}</small></td>
          <td>{{ item.assignee ?? 'Non affecté' }}</td>
          <td><StatusBadge :label="item.status" /></td>
          <td><RouterLink :to="`/cases/${item.id}`" class="row-action" aria-label="Ouvrir le dossier">→</RouterLink></td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

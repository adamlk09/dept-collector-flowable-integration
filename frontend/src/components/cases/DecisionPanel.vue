<script setup lang="ts">
import type { DecisionTrace } from '@/types/domain'
defineProps<{ decision: DecisionTrace | null; canExecute?: boolean }>()
defineEmits<{ execute: [] }>()
</script>

<template>
  <section class="panel decision-panel">
    <div class="panel-title"><div><span>Flowable DMN</span><h3>Décision de segmentation</h3></div><div class="inline-actions"><span class="engine-dot">Moteur actif</span><button v-if="canExecute" class="text-button" @click="$emit('execute')">Réexécuter</button></div></div>
    <template v-if="decision">
      <div class="decision-result"><small>Résultat</small><strong>{{ decision.result }}</strong><span>Version {{ decision.version }} · {{ decision.decisionKey }}</span></div>
      <div class="reason-list"><div v-for="(reason,index) in decision.reasons" :key="reason"><i>{{ index + 1 }}</i><span>{{ reason }}</span></div></div>
    </template>
    <div v-else class="empty-state">Aucune décision exécutée pour ce dossier.</div>
  </section>
</template>

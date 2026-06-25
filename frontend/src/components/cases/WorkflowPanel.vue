<script setup lang="ts">
import type { WorkflowInstance, WorkflowTask } from '@/types/domain'
defineProps<{ tasks: WorkflowTask[]; instance?: WorkflowInstance | null; canComplete?: boolean }>()
defineEmits<{ complete: [taskId: string] }>()
</script>

<template>
  <section class="panel">
    <div class="panel-title"><div><span>Flowable BPMN</span><h3>Processus de recouvrement</h3></div><span class="workflow-status" :data-status="instance?.status ?? 'Active'">{{ instance?.status ?? 'Active' }}</span></div>
    <div class="workflow">
      <div class="workflow-step done"><i>✓</i><span><b>Dossier créé</b><small>Données validées</small></span></div>
      <div class="workflow-line"></div>
      <div class="workflow-step done"><i>✓</i><span><b>Segmentation</b><small>Décision DMN exécutée</small></span></div>
      <div class="workflow-line"></div>
      <div class="workflow-step" :class="{active:instance?.status !== 'Terminée',done:instance?.status === 'Terminée'}"><i>{{ instance?.status === 'Terminée' ? '✓' : '3' }}</i><span><b>Traitement</b><small>{{ instance?.currentActivity ?? tasks[0]?.name ?? 'En attente' }}</small></span></div>
      <div class="workflow-line"></div>
      <div class="workflow-step" :class="{done:instance?.status === 'Terminée'}"><i>{{ instance?.status === 'Terminée' ? '✓' : '4' }}</i><span><b>Résolution</b><small>{{ instance?.status === 'Terminée' ? 'Processus terminé' : 'Paiement ou escalade' }}</small></span></div>
    </div>
    <div v-if="tasks.length" class="task-list">
      <div v-for="task in tasks" :key="task.id"><span><b>{{ task.name }}</b><small>{{ task.assignee }} · {{ task.status }}</small></span><button v-if="task.status !== 'Terminée' && canComplete" class="secondary-button" @click="$emit('complete', task.id)">Terminer</button><span v-else-if="task.status === 'Terminée'" class="done-label">Terminée</span></div>
    </div>
  </section>
</template>

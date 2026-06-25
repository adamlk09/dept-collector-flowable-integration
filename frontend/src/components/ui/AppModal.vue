<script setup lang="ts">
defineProps<{ open: boolean; title: string; description?: string }>()
const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <div v-if="open" class="modal-backdrop" @click.self="emit('close')">
      <section class="app-modal" role="dialog" aria-modal="true">
        <header><div><h2>{{ title }}</h2><p v-if="description">{{ description }}</p></div><button class="icon-button" aria-label="Fermer" @click="emit('close')">×</button></header>
        <div class="modal-content"><slot /></div>
      </section>
    </div>
  </Teleport>
</template>

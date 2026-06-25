<script setup lang="ts">
defineProps<{ label: string; value: string; detail: string; tone?: string }>()
</script>

<template>
  <article class="stat-card">
    <div class="stat-icon" :data-tone="tone"><slot /></div>
    <div><span>{{ label }}</span><strong>{{ value }}</strong><small>{{ detail }}</small></div>
  </article>
</template>

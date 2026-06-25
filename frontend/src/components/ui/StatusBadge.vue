<script setup lang="ts">
defineProps<{ label: string }>()
</script>

<template><span class="status-badge" :data-status="label">{{ label }}</span></template>

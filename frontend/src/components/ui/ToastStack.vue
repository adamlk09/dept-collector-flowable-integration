<script setup lang="ts">
import { useNotificationsStore } from '@/stores/notifications'
const notifications = useNotificationsStore()
</script>

<template>
  <Teleport to="body"><div class="toast-stack"><button v-for="item in notifications.items" :key="item.id" class="toast" :data-tone="item.tone" @click="notifications.remove(item.id)"><i>{{ item.tone === 'success' ? '✓' : item.tone === 'error' ? '!' : 'i' }}</i>{{ item.message }}</button></div></Teleport>
</template>

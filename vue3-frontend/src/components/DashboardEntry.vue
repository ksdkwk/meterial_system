<template>
  <DashboardPage :user="currentUser" @logout="handleLogout" />
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import DashboardPage from './DashboardPage.vue'
import { clearAuthSession, getStoredUser } from '@/utils/auth'

const router = useRouter()

const currentUser = ref(getStoredUser())

const handleLogout = () => {
  clearAuthSession()
  currentUser.value = null
  router.replace('/login')
}
</script>

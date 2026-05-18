<template>
  <LoginForm :startForgot="startForgot" @login-success="handleLoginSuccess" />
</template>

<script setup>
import { useRouter } from 'vue-router'
import LoginForm from './LoginForm.vue'

defineProps({
  startForgot: {
    type: Boolean,
    default: false
  }
})

const router = useRouter()

const handleLoginSuccess = () => {
  router.replace('/dashboard')
}
</script>

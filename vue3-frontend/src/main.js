import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { restoreAuthOnAppInit } from '@/utils/auth'
import { sanitizeHtml } from '@/utils/sanitize'

restoreAuthOnAppInit()

// 创建并挂载Vue应用
const app = createApp(App)

app.directive('safe-html', {
	beforeMount(el, binding) {
		el.innerHTML = sanitizeHtml(binding.value)
	},
	updated(el, binding) {
		el.innerHTML = sanitizeHtml(binding.value)
	}
})

app.use(router).mount('#app')
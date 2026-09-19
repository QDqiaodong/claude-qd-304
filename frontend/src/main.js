import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './theme.css'
import App from './App.vue'
import router from './router'
import { autofocus, digits } from './directives'

const app = createApp(App)
app.use(ElementPlus).use(router)
app.directive('autofocus', autofocus)
app.directive('digits', digits)
app.mount('#app')

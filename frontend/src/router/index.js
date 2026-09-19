import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/cabinets' },
  { path: '/cabinets', component: () => import('../views/Cabinets.vue'), meta: { label: '试剂柜' } },
  { path: '/reagents', component: () => import('../views/Reagents.vue'), meta: { label: '试剂' } },
  { path: '/instruments', component: () => import('../views/Instruments.vue'), meta: { label: '仪器' } },
  { path: '/usages', component: () => import('../views/Usages.vue'), meta: { label: '领用登记' } }
]

export const navItems = routes
  .filter((r) => r.meta && r.meta.label)
  .map((r) => ({ path: r.path, label: r.meta.label }))

export default createRouter({
  history: createWebHistory(),
  routes
})

<template>
  <div class="lab-shell">
    <button class="floating-back" @click="menu = !menu">← {{ currentLabel }}</button>
    <transition name="drop">
      <nav v-if="menu" class="menu-card">
        <p class="menu-title">切到</p>
        <button v-for="n in navs" :key="n.path" class="menu-item"
                :class="{ on: $route.path === n.path }" @click="go(n.path)">{{ n.label }}</button>
      </nav>
    </transition>
    <main class="lab-body">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, provide, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { navItems } from './router'

const route = useRoute()
const router = useRouter()
const menu = ref(false)
const navs = navItems
const currentLabel = computed(() => (navs.find((n) => n.path === route.path) || {}).label || '菜单')

function go(path) {
  menu.value = false
  router.push(path)
}

// 统一的提示口子，页面里 inject('toast') 直接用
provide('toast', (msg, ok = true) => (ok ? ElMessage.success(msg) : ElMessage.error(msg)))
provide('today', new Date().toISOString().slice(0, 10))
</script>

<style>
html, body { margin: 0; }
body { background: #fdf7f4; font-family: -apple-system, 'PingFang SC', sans-serif; }
.lab-shell { min-height: 100vh; }
.floating-back { position: fixed; left: 18px; top: 16px; z-index: 20; border: none; cursor: pointer;
  background: #fff; border-radius: 22px; padding: 9px 18px; font-size: 13px; color: #6b5b55;
  box-shadow: 0 3px 14px rgba(0,0,0,.1); }
.menu-card { position: fixed; left: 18px; top: 62px; z-index: 20; background: #fff; border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,.14); padding: 10px; width: 168px; }
.menu-title { margin: 2px 0 8px; font-size: 11px; color: #bbb; padding-left: 8px; }
.menu-item { display: block; width: 100%; text-align: left; border: none; background: transparent;
  padding: 9px 10px; border-radius: 7px; font-size: 13px; color: #6b5b55; cursor: pointer; }
.menu-item:hover { background: #faf5f2; }
.menu-item.on { background: var(--el-color-primary-light-9); color: var(--el-color-primary-dark-2); font-weight: 600; }
.lab-body { padding: 74px 40px 40px; max-width: 1200px; margin: 0 auto; }
.drop-enter-active, .drop-leave-active { transition: opacity .16s; }
.drop-enter-from, .drop-leave-to { opacity: 0; }
</style>

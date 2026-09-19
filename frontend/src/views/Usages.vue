<template>
  <div class="pane">
    <header class="pane-head">
      <h2>领用登记</h2>
      <span class="sub">顶上一条录入栏，敲完直接回车就进一条；光标会自己回到输入框</span>
    </header>

    <div class="recorder" v-autofocus>
      <div class="seg">
        <button :class="{ on: form.direction === '领用' }" @click="form.direction = '领用'">领用</button>
        <button :class="{ on: form.direction === '退回' }" @click="form.direction = '退回'">退回</button>
      </div>
      <select v-model="form.reagentId" class="pick">
        <option :value="null">选一瓶试剂</option>
        <option v-for="r in usable" :key="r.id" :value="r.id">
          {{ r.reagentCode }} {{ r.reagentName }}（余 {{ r.balance }}）
        </option>
      </select>
      <input v-model="form.userName" class="who" placeholder="领用人" />
      <input v-model="form.quantity" class="qty" v-digits placeholder="数量" @keyup.enter="record" />
      <input v-model="form.purpose" class="why" placeholder="用途（可不填）" @keyup.enter="record" />
      <button class="prime" @click="record">记一条</button>
    </div>

    <div class="feed">
      <div class="feed-head">
        <span>登记单号</span><span>试剂</span><span>领用人</span>
        <span class="r">数量</span><span class="r">方向</span><span>日期</span><span>用途</span>
      </div>
      <div class="feed-row" v-for="l in logs" :key="l.id">
        <span class="mono">{{ l.logNo }}</span>
        <span>{{ reagentName(l.reagentId) }}</span>
        <span>{{ l.userName }}</span>
        <span class="r">{{ l.quantity }}</span>
        <span class="r" :class="l.direction === '领用' ? 'out' : 'in'">{{ l.direction }}</span>
        <span class="dim">{{ l.useDate }}</span>
        <span class="dim">{{ l.purpose || '—' }}</span>
      </div>
    </div>

    <p class="tip">小提示：领用量超过库存、或者试剂已经过期，后端会直接拦住并且告诉你原因。</p>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import api from '../api'

const toast = inject('toast')
const logs = ref([])
const reagents = ref([])
const form = ref({ direction: '领用', reagentId: null })

const usable = computed(() => reagents.value.filter((r) => r.reagentStatus !== '停用'))

function reagentName(id) {
  const r = reagents.value.find((x) => x.id === id)
  return r ? r.reagentCode + ' ' + r.reagentName : '—'
}

async function load() {
  logs.value = await api.usages.list()
  reagents.value = await api.reagents.list()
}

let seq = 0
async function record() {
  if (!form.value.reagentId) {
    toast('先选一瓶试剂', false)
    return
  }
  const body = {
    ...form.value,
    logNo: 'UL-' + Date.now().toString().slice(-8)
  }
  try {
    await api.usages.add(body)
    await load()
    toast('记好了')
    form.value = { direction: form.value.direction, reagentId: null }
  } catch (e) {
    toast(e.message, false)
  }
}

onMounted(load)
</script>

<style scoped>
.pane-head { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; }
.pane-head h2 { margin: 0; font-size: 20px; }
.sub { flex: 1; color: #b3a09a; font-size: 12px; }
.recorder { display: flex; align-items: center; gap: 10px; background: #fff; border: 1px solid #f5d9cb;
  border-radius: 12px; padding: 14px 16px; margin-bottom: 16px; flex-wrap: wrap; }
.seg { display: flex; gap: 6px; }
.seg button { border: 1px solid #eee0d9; background: #fff; border-radius: 8px; padding: 8px 16px;
  font-size: 13px; color: #8b7a74; cursor: pointer; }
.seg button.on { background: var(--el-color-primary); color: #fff; border-color: var(--el-color-primary); }
.pick, .who, .qty, .why { border: 1px solid #eee0d9; border-radius: 8px; padding: 8px 10px; font-size: 13px; }
.pick { min-width: 200px; }
.who { width: 110px; }
.qty { width: 80px; }
.why { width: 150px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 9px 20px; font-size: 13px; cursor: pointer; }
.feed { background: #fff; border: 1px solid #f4e6e0; border-radius: 12px; overflow: hidden; }
.feed-head, .feed-row { display: grid; grid-template-columns: 110px 1.4fr 84px 62px 62px 100px 1fr;
  gap: 10px; align-items: center; padding: 10px 14px; font-size: 13px; }
.feed-head { background: #fdf7f4; color: #b3a09a; font-size: 12px; }
.feed-row { border-top: 1px solid #f9f1ed; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #b3a09a; font-size: 12px; }
.r { text-align: right; }
.out { color: #c0392b; }
.in { color: #2e7d4f; }
.dim { color: #a89891; font-size: 12px; }
.tip { margin-top: 14px; font-size: 12px; color: #c2b2ac; }
</style>

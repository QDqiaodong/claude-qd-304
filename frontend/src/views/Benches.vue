<template>
  <div class="pane">
    <header class="pane-head">
      <h2>对照开台</h2>
      <span class="sub">一张开台单 = 一台仪器 + 一瓶对照试剂；柜型配不上、仪器不在岗，都开不出去</span>
    </header>

    <div class="recorder" v-autofocus>
      <select v-model="form.instrumentId" class="pick">
        <option :value="null">挑一台仪器</option>
        <option v-for="i in instruments" :key="i.id" :value="i.id" :disabled="i.instrumentStatus !== '可用'">
          {{ i.instrumentCode }} {{ i.instrumentName }}（{{ i.instrumentStatus }}）
        </option>
      </select>
      <select v-model="form.reagentId" class="pick">
        <option :value="null">挑一瓶对照试剂</option>
        <option v-for="r in reagents" :key="r.id" :value="r.id">
          {{ r.reagentCode }} {{ r.reagentName }}（{{ cabinetKind(r) }} · 可占 {{ avail(r) }}）
        </option>
      </select>
      <input v-model="form.userName" class="who" placeholder="开台人" @keyup.enter="open" />
      <button class="prime" @click="open">开台</button>
    </div>

    <div class="feed">
      <div class="feed-head">
        <span>开台单号</span><span>仪器</span><span>对照试剂</span><span>开台人</span>
        <span>开台时间</span><span class="r">状态</span><span class="r">收口</span>
      </div>
      <div class="feed-row" v-for="o in orders" :key="o.id">
        <span class="mono">{{ o.orderNo }}</span>
        <span>{{ instrumentName(o.instrumentId) }}</span>
        <span>{{ reagentName(o.reagentId) }}</span>
        <span>{{ o.userName }}</span>
        <span class="dim">{{ fmt(o.openTime) }}</span>
        <span class="r"><b class="st" :class="stClass(o)">{{ o.orderStatus }}</b></span>
        <span class="r">
          <template v-if="o.orderStatus === '开台中'">
            <p v-if="instrumentDown(o)" class="down">仪器已{{ instrumentStatusOf(o) }}，只能作废</p>
            <button class="mini" @click="complete(o)">完成</button>
            <button class="mini danger" @click="voidIt(o)">作废</button>
          </template>
          <span v-else class="dim">{{ fmt(o.closeTime) }}</span>
        </span>
      </div>
      <p v-if="!orders.length" class="none">还没有开台单</p>
    </div>

    <p class="tip">开台就把一瓶对照试剂从现库存里预占出来（不走领用流水）；作废整单吐回，完成才真正销掉。
      仪器中途被改成维修中或停用，开着的单只能整单作废，收不了口。</p>
  </div>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import api from '../api'

const toast = inject('toast')
const orders = ref([])
const instruments = ref([])
const reagents = ref([])
const cabinets = ref([])
const form = ref({ instrumentId: null, reagentId: null, userName: '' })

function instrumentOf(id) {
  return instruments.value.find((x) => x.id === id)
}
function instrumentName(id) {
  const i = instrumentOf(id)
  return i ? i.instrumentCode + ' ' + i.instrumentName : '—'
}
function instrumentStatusOf(o) {
  const i = instrumentOf(o.instrumentId)
  return i ? i.instrumentStatus : '找不到'
}
function instrumentDown(o) {
  return instrumentStatusOf(o) !== '可用'
}
function reagentName(id) {
  const r = reagents.value.find((x) => x.id === id)
  return r ? r.reagentCode + ' ' + r.reagentName : '—'
}
function cabinetKind(r) {
  const c = cabinets.value.find((x) => x.id === r.cabinetId)
  return c ? c.cabinetKind : '未入柜'
}
function avail(r) {
  return (r.balance || 0) - (r.reserved || 0)
}
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}
function stClass(o) {
  return o.orderStatus === '开台中' ? 'open' : o.orderStatus === '已完成' ? 'done' : 'void'
}

async function load() {
  orders.value = await api.benches.list()
  instruments.value = await api.instruments.list()
  reagents.value = await api.reagents.list()
  cabinets.value = await api.cabinets.list()
}

async function open() {
  if (!form.value.instrumentId) {
    toast('先挑一台仪器', false)
    return
  }
  if (!form.value.reagentId) {
    toast('先挑一瓶对照试剂', false)
    return
  }
  if (!form.value.userName || !form.value.userName.trim()) {
    toast('开台人不能空着', false)
    return
  }
  try {
    await api.benches.open({ ...form.value, orderNo: 'BO-' + Date.now().toString().slice(-8) })
    await load()
    toast('台开好了，对照试剂已预占')
    form.value = { instrumentId: null, reagentId: null, userName: form.value.userName }
  } catch (e) {
    toast(e.message, false)
  }
}

async function complete(o) {
  try {
    await api.benches.complete(o.id)
    await load()
    toast('这单收口了')
  } catch (e) {
    toast(e.message, false)
  }
}

async function voidIt(o) {
  try {
    await api.benches.void(o.id)
    await load()
    toast('整单作废，预占吐回')
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
.pick, .who { border: 1px solid #eee0d9; border-radius: 8px; padding: 8px 10px; font-size: 13px; }
.pick { min-width: 220px; }
.who { width: 110px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 9px 20px; font-size: 13px; cursor: pointer; }
.feed { background: #fff; border: 1px solid #f4e6e0; border-radius: 12px; overflow: hidden; }
.feed-head, .feed-row { display: grid; grid-template-columns: 110px 1.2fr 1.2fr 84px 130px 76px 150px;
  gap: 10px; align-items: center; padding: 10px 14px; font-size: 13px; }
.feed-head { background: #fdf7f4; color: #b3a09a; font-size: 12px; }
.feed-row { border-top: 1px solid #f9f1ed; }
.mono { font-family: ui-monospace, Menlo, monospace; color: #b3a09a; font-size: 12px; }
.r { text-align: right; }
.dim { color: #a89891; font-size: 12px; }
.st { font-weight: 600; font-size: 12px; }
.st.open { color: #b4761f; }
.st.done { color: #2e7d4f; }
.st.void { color: #a89891; }
.mini { background: #fff; border: 1px solid var(--el-color-primary-light-7); color: var(--el-color-primary-dark-2);
  border-radius: 7px; padding: 4px 12px; font-size: 12px; cursor: pointer; margin-left: 6px; }
.mini.danger { border-color: #ecc5bd; color: #c0392b; }
.down { margin: 0 0 4px; font-size: 11px; color: #c0392b; text-align: right; }
.none { color: #c9bab4; font-size: 12px; text-align: center; padding: 20px 0; margin: 0; }
.tip { margin-top: 14px; font-size: 12px; color: #c2b2ac; }
</style>

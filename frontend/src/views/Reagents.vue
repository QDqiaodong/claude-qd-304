<template>
  <div class="pane">
    <header class="pane-head">
      <h2>试剂</h2>
      <span class="sub">左上角那个圈是「还有几天到期」，越少越红；过期的整张卡发灰</span>
      <button class="prime" @click="openNew">登记试剂</button>
    </header>

    <div class="grid">
      <article v-for="r in items" :key="r.id" class="r-card" :class="level(r)">
        <div class="bubble">{{ daysLeft(r) }}</div>
        <div class="r-code">{{ r.reagentCode }}</div>
        <div class="r-name">{{ r.reagentName }}</div>
        <div class="r-meta">{{ r.specText || '未标规格' }}</div>
        <div class="r-bar">
          <div class="r-fill" :style="{ width: barWidth(r) + '%' }"></div>
        </div>
        <div class="r-foot">
          <span>库存 {{ r.balance }}<template v-if="r.reserved"> · 预占 {{ r.reserved }}</template></span>
          <span>{{ r.expireDate }}</span>
        </div>
        <button class="ghost" @click="openEdit(r)">修改</button>
      </article>
    </div>

    <el-dialog v-model="show" :title="form.id ? '修改试剂' : '登记试剂'" width="440px">
      <div class="fr"><label>编号</label><el-input v-model="form.reagentCode" /></div>
      <div class="fr"><label>名称</label><el-input v-model="form.reagentName" /></div>
      <div class="fr"><label>规格</label><el-input v-model="form.specText" /></div>
      <div class="fr">
        <label>存放柜</label>
        <el-select v-model="form.cabinetId" style="flex:1">
          <el-option v-for="c in cabinets" :key="c.id" :label="c.cabinetName" :value="c.id" />
        </el-select>
      </div>
      <div class="fr"><label>有效期至</label><el-input v-model="form.expireDate" placeholder="2027-03-01" /></div>
      <div class="fr"><label>状态</label><el-input v-model="form.reagentStatus" placeholder="可用 / 已用完 / 停用" /></div>
      <p class="note">库存不在这里改，靠「领用登记」进出。</p>
      <template #footer>
        <el-button @click="show = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { inject, onMounted, ref } from 'vue'
import api from '../api'

const toast = inject('toast')
const items = ref([])
const cabinets = ref([])
const show = ref(false)
const form = ref({})

const TODAY = new Date().toISOString().slice(0, 10)

function daysLeft(r) {
  if (!r.expireDate) return '∞'
  const d = Math.ceil((new Date(r.expireDate) - new Date(TODAY)) / 86400000)
  return d < 0 ? '过期' : d
}
function barWidth(r) {
  const d = daysLeft(r)
  if (typeof d !== 'number') return 100
  return Math.max(4, Math.min(100, Math.round((d / 365) * 100)))
}
function level(r) {
  const d = daysLeft(r)
  if (typeof d !== 'number') return ''
  return d < 0 ? 'over' : d < 30 ? 'warn' : ''
}
async function load() {
  items.value = await api.reagents.list()
  cabinets.value = await api.cabinets.list()
}
function openNew() {
  form.value = { reagentStatus: '可用' }
  show.value = true
}
function openEdit(row) {
  form.value = { ...row }
  show.value = true
}
async function submit() {
  try {
    if (form.value.id) await api.reagents.save(form.value.id, form.value)
    else await api.reagents.add(form.value)
    show.value = false
    await load()
    toast('保存好了')
  } catch (e) { toast(e.message, false) }
}
onMounted(load)
</script>

<style scoped>
.pane-head { display: flex; align-items: center; gap: 14px; margin-bottom: 18px; }
.pane-head h2 { margin: 0; font-size: 20px; }
.sub { flex: 1; color: #b3a09a; font-size: 12px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 8px 18px; font-size: 13px; cursor: pointer; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(214px, 1fr)); gap: 14px; }
.r-card { position: relative; background: #fff; border: 1px solid #f4e6e0; border-radius: 12px;
  padding: 16px 16px 14px; }
.r-card.warn { border-color: #f0d8b8; background: #fffdf7; }
.r-card.over { background: #fafafa; border-color: #e6e0dd; }
.bubble { position: absolute; right: 12px; top: 12px; width: 44px; height: 44px; border-radius: 50%;
  background: var(--el-color-primary-light-8); color: var(--el-color-primary-dark-2); display: flex;
  align-items: center; justify-content: center; font-size: 15px; font-weight: 700; }
.r-card.warn .bubble { background: #fdf0dd; color: #b4761f; }
.r-card.over .bubble { background: #e9e5e3; color: #8b7a74; font-size: 12px; }
.r-code { font-size: 12px; color: #b3a09a; }
.r-name { font-size: 15px; font-weight: 600; margin: 6px 0 4px; width: 62%; }
.r-meta { font-size: 12px; color: #a89891; margin-bottom: 12px; }
.r-bar { height: 7px; background: #f6efeb; border-radius: 4px; overflow: hidden; }
.r-fill { height: 100%; background: var(--el-color-primary); border-radius: 4px; }
.r-card.warn .r-fill { background: #e6a23c; }
.r-card.over .r-fill { background: #c9bab4; }
.r-foot { display: flex; justify-content: space-between; font-size: 12px; color: #a89891;
  margin: 8px 0 12px; }
.ghost { background: #fff; border: 1px solid var(--el-color-primary-light-7); color: var(--el-color-primary-dark-2);
  border-radius: 7px; padding: 5px 14px; font-size: 12px; cursor: pointer; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 76px; text-align: right; font-size: 13px; color: #8b7a74; }
.note { font-size: 12px; color: #c2b2ac; margin: 4px 0 0 86px; }
</style>

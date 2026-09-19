<template>
  <div class="pane">
    <header class="pane-head">
      <h2>试剂柜</h2>
      <span class="sub">左边点柜子，右边看这柜里放了什么</span>
      <input v-model="kw" class="search" placeholder="搜编号或名称" />
      <button class="prime" @click="openNew">新增柜子</button>
    </header>

    <div class="split">
      <aside class="tree">
        <div v-for="c in shown" :key="c.id" class="node" :class="{ on: cur && cur.id === c.id }"
             @click="cur = c">
          <span class="dot" :class="{ off: c.cabinetStatus === '停用' }"></span>
          <b>{{ c.cabinetName }}</b>
          <small>{{ c.cabinetCode }}</small>
        </div>
        <p v-if="!shown.length" class="none">没有匹配的柜子</p>
      </aside>
      <section class="detail" v-if="cur">
        <div class="d-head">
          <h3>{{ cur.cabinetName }}</h3>
          <span class="tag">{{ cur.cabinetKind || '未分类' }}</span>
          <span class="tag" :class="{ warn: cur.cabinetStatus === '停用' }">{{ cur.cabinetStatus }}</span>
        </div>
        <div class="reagent-list">
          <div v-for="r in reagentsOf(cur.id)" :key="r.id" class="reagent-row" :class="{ expired: isExpired(r) }">
            <span class="rc">{{ r.reagentCode }}</span>
            <span class="rn">{{ r.reagentName }}</span>
            <span class="rs">{{ r.balance }} 份</span>
            <span class="rd">{{ r.expireDate }}</span>
            <b v-if="isExpired(r)" class="flag">已过期</b>
          </div>
          <p v-if="!reagentsOf(cur.id).length" class="none">这柜子空着</p>
        </div>
        <button class="ghost" @click="openEdit(cur)">修改这柜</button>
      </section>
      <section class="detail empty" v-else>左边选一个柜子</section>
    </div>

    <el-dialog v-model="show" :title="form.id ? '修改试剂柜' : '新增试剂柜'" width="420px">
      <div class="fr"><label>编号</label><el-input v-model="form.cabinetCode" /></div>
      <div class="fr"><label>名称</label><el-input v-model="form.cabinetName" /></div>
      <div class="fr"><label>柜型</label><el-input v-model="form.cabinetKind" placeholder="通风柜 / 普通柜 / 防爆柜" /></div>
      <div class="fr"><label>状态</label><el-input v-model="form.cabinetStatus" placeholder="可用 / 停用" /></div>
      <template #footer>
        <el-button @click="show = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, inject, onMounted, ref } from 'vue'
import api from '../api'

const toast = inject('toast')
const all = ref([])
const reagents = ref([])
const cur = ref(null)
const kw = ref('')
const show = ref(false)
const form = ref({})

const shown = computed(() => {
  if (!kw.value) return all.value
  const k = kw.value.toLowerCase()
  return all.value.filter((c) => c.cabinetCode.toLowerCase().includes(k) || c.cabinetName.includes(k))
})

function reagentsOf(id) {
  return reagents.value.filter((r) => r.cabinetId === id)
}
function isExpired(r) {
  return r.expireDate && r.expireDate < new Date().toISOString().slice(0, 10)
}
async function load() {
  all.value = await api.cabinets.list()
  reagents.value = await api.reagents.list()
  if (!cur.value && all.value.length) cur.value = all.value[0]
}
function openNew() {
  form.value = { cabinetStatus: '可用' }
  show.value = true
}
function openEdit(row) {
  form.value = { ...row }
  show.value = true
}
async function submit() {
  try {
    if (form.value.id) await api.cabinets.save(form.value.id, form.value)
    else await api.cabinets.add(form.value)
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
.search { border: 1px solid #eee0d9; border-radius: 8px; padding: 8px 12px; font-size: 13px; width: 180px; }
.prime { background: var(--el-color-primary); color: #fff; border: none; border-radius: 8px;
  padding: 8px 18px; font-size: 13px; cursor: pointer; }
.split { display: grid; grid-template-columns: 240px 1fr; gap: 16px; }
.tree { background: #fff; border: 1px solid #f4e6e0; border-radius: 12px; padding: 8px; }
.node { display: flex; align-items: center; gap: 8px; padding: 10px 12px; border-radius: 8px;
  cursor: pointer; font-size: 13px; }
.node:hover { background: #fdf7f4; }
.node.on { background: var(--el-color-primary-light-9); color: var(--el-color-primary-dark-2); }
.node small { margin-left: auto; color: #c2b2ac; }
.dot { width: 7px; height: 7px; border-radius: 50%; background: var(--el-color-primary); }
.dot.off { background: #d0c4bf; }
.detail { background: #fff; border: 1px solid #f4e6e0; border-radius: 12px; padding: 18px; }
.detail.empty { display: flex; align-items: center; justify-content: center; color: #c9bab4; font-size: 13px; }
.d-head { display: flex; align-items: center; gap: 10px; margin-bottom: 14px; }
.d-head h3 { margin: 0; font-size: 17px; }
.tag { font-size: 11px; background: #f8efe9; border-radius: 10px; padding: 2px 10px; color: #9c8b84; }
.tag.warn { background: #fbe4e4; color: #c0392b; }
.reagent-row { display: grid; grid-template-columns: 90px 1fr 70px 110px 62px; align-items: center;
  padding: 10px 0; border-bottom: 1px solid #f9f1ed; font-size: 13px; }
.reagent-row.expired { background: #fffafa; }
.rc { color: #b3a09a; }
.rs { text-align: right; color: var(--el-color-primary-dark-2); }
.rd { color: #a89891; font-size: 12px; }
.flag { color: #c0392b; font-size: 12px; }
.none { color: #c9bab4; font-size: 12px; padding: 14px 0; text-align: center; }
.ghost { margin-top: 14px; background: #fff; border: 1px solid var(--el-color-primary-light-7);
  color: var(--el-color-primary-dark-2); border-radius: 8px; padding: 6px 16px; font-size: 13px; cursor: pointer; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 70px; text-align: right; font-size: 13px; color: #8b7a74; }
</style>

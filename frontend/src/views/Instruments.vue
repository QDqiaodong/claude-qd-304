<template>
  <div class="pane">
    <header class="pane-head">
      <h2>仪器</h2>
      <span class="sub">左边按保管人分堆，中间是清单，右边看单台明细</span>
      <button class="prime" @click="openNew">登记仪器</button>
    </header>

    <div class="three">
      <aside class="col keeper">
        <p class="col-title">保管人</p>
        <button class="k-item" :class="{ on: !keeper }" @click="keeper = ''">全部（{{ items.length }}）</button>
        <button v-for="k in keepers" :key="k.name" class="k-item" :class="{ on: keeper === k.name }"
                @click="keeper = k.name">{{ k.name }}（{{ k.n }}）</button>
      </aside>

      <section class="col list">
        <div v-for="i in mid" :key="i.id" class="i-row" :class="{ on: cur && cur.id === i.id }"
             @click="cur = i">
          <b>{{ i.instrumentName }}</b>
          <small>{{ i.instrumentCode }} · {{ i.instrumentStatus }}</small>
        </div>
        <p v-if="!mid.length" class="none">这里没有仪器</p>
      </section>

      <section class="col detail" v-if="cur">
        <h3>{{ cur.instrumentName }}</h3>
        <div class="d-row"><span>编号</span><b>{{ cur.instrumentCode }}</b></div>
        <div class="d-row"><span>型号</span><b>{{ cur.modelText || '未填' }}</b></div>
        <div class="d-row"><span>保管人</span><b>{{ cur.keeper || '未指定' }}</b></div>
        <div class="d-row"><span>状态</span><b>{{ cur.instrumentStatus }}</b></div>
        <button class="ghost" @click="openEdit(cur)">修改</button>
      </section>
      <section class="col detail empty" v-else>中间选一台看明细</section>
    </div>

    <el-dialog v-model="show" :title="form.id ? '修改仪器' : '登记仪器'" width="420px">
      <div class="fr"><label>编号</label><el-input v-model="form.instrumentCode" /></div>
      <div class="fr"><label>名称</label><el-input v-model="form.instrumentName" /></div>
      <div class="fr"><label>型号</label><el-input v-model="form.modelText" /></div>
      <div class="fr"><label>保管人</label><el-input v-model="form.keeper" /></div>
      <div class="fr"><label>状态</label><el-input v-model="form.instrumentStatus" placeholder="可用 / 维修中 / 停用" /></div>
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
const items = ref([])
const keeper = ref('')
const cur = ref(null)
const show = ref(false)
const form = ref({})

const keepers = computed(() => {
  const m = {}
  items.value.forEach((i) => {
    const k = i.keeper || '未指定'
    m[k] = (m[k] || 0) + 1
  })
  return Object.keys(m).map((name) => ({ name, n: m[name] }))
})
const mid = computed(() => (keeper.value ? items.value.filter((i) => (i.keeper || '未指定') === keeper.value) : items.value))

async function load() {
  items.value = await api.instruments.list()
  if (!cur.value && items.value.length) cur.value = items.value[0]
}
function openNew() {
  form.value = { instrumentStatus: '可用' }
  show.value = true
}
function openEdit(row) {
  form.value = { ...row }
  show.value = true
}
async function submit() {
  try {
    if (form.value.id) await api.instruments.save(form.value.id, form.value)
    else await api.instruments.add(form.value)
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
.three { display: grid; grid-template-columns: 180px 1fr 262px; gap: 12px; }
.col { background: #fff; border: 1px solid #f4e6e0; border-radius: 12px; padding: 12px; }
.col-title { margin: 0 0 8px; font-size: 11px; color: #c2b2ac; padding-left: 6px; }
.k-item { display: block; width: 100%; text-align: left; border: none; background: transparent;
  padding: 8px 10px; border-radius: 7px; font-size: 13px; color: #7a6a64; cursor: pointer; }
.k-item:hover { background: #fdf7f4; }
.k-item.on { background: var(--el-color-primary-light-9); color: var(--el-color-primary-dark-2); font-weight: 600; }
.list { max-height: 480px; overflow: auto; }
.i-row { padding: 10px; border-radius: 8px; cursor: pointer; display: flex; flex-direction: column; gap: 3px; }
.i-row:hover { background: #fdf7f4; }
.i-row.on { background: var(--el-color-primary-light-9); }
.i-row b { font-size: 13px; font-weight: 500; }
.i-row small { font-size: 11px; color: #b3a09a; }
.detail h3 { margin: 4px 0 14px; font-size: 16px; }
.detail.empty { display: flex; align-items: center; justify-content: center; color: #c9bab4; font-size: 13px; }
.d-row { display: flex; justify-content: space-between; font-size: 13px; color: #8b7a74;
  padding: 7px 0; border-bottom: 1px dashed #f7f1ed; }
.d-row b { color: #4a3f3a; }
.none { color: #c9bab4; font-size: 12px; text-align: center; padding: 20px 0; }
.ghost { margin-top: 14px; width: 100%; background: #fff; border: 1px solid var(--el-color-primary-light-7);
  color: var(--el-color-primary-dark-2); border-radius: 8px; padding: 7px 16px; font-size: 13px; cursor: pointer; }
.fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.fr label { width: 70px; text-align: right; font-size: 13px; color: #8b7a74; }
</style>

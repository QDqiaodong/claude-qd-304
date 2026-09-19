<template>
  <div class="pg">
    <div class="hd">
      <h2>领用登记</h2>
      <span class="hint">顶上一条录入栏，敲完回车就进一条，连着录很快。</span>
      <input class="search" v-model="kw" placeholder="搜索编号或名称" />
      <button class="btn solid" @click="openNew">新增</button>
    </div>
    <div class="qe">
      <input v-model="line" placeholder="编号,名称,后面依次填" @keyup.enter="add" />
      <button class="btn solid" @click="add">回车即录入</button>
      <span class="cnt">已录入 {{ rows.length }} 条</span>
    </div>
    <div class="feed">
      <div class="fi" v-for="it in rows" :key="it.id">
        <span class="fi-code">{{ it.code }}</span>
        <span class="fi-name">{{ it.name }}</span>
        <span class="fi-st">{{ it[ST] }}</span>
        <button class="btn sm" @click="openEdit(it)">改</button>
      </div>
    </div>
  </div>

    <el-dialog v-model="show" :title="form.id ? '修改' : '新增'" width="440px">
      <div class="frm">
        <div class="fr" v-for="fd in FORM_FIELDS" :key="fd.k">
          <label>{{ fd.l }}</label>
          <el-input v-model="form[fd.k]" :placeholder="'请填写' + fd.l" />
        </div>
      </div>
      <template #footer>
        <el-button @click="show = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { usageApi } from '../api'

const rows = ref([])
const show = ref(false)
const form = ref({})
const kw = ref('')
const FORM_FIELDS = [{"k":"code","l":"编号"},{"k":"reagentId","l":"领用试剂"},{"k":"userName","l":"领用人"},{"k":"useDate","l":"领用日期"},{"k":"quantity","l":"领用量"},{"k":"purpose","l":"用途"}]
const BODY_FIELDS = [{"k":"reagentId","l":"领用试剂"},{"k":"userName","l":"领用人"},{"k":"useDate","l":"领用日期"},{"k":"quantity","l":"领用量"},{"k":"purpose","l":"用途"}]
const ST = 'status'
const OPTS = ["正常","停用"]

const picked = ref([])
const filtered = computed(() => {
  if (!kw.value) return rows.value
  const k = kw.value.toLowerCase()
  return rows.value.filter(r => (r.code || '').toLowerCase().includes(k) || (r.name || '').toLowerCase().includes(k))
})

async function load() { rows.value = await usageApi.list() }
function openNew() { form.value = {}; show.value = true }
function openEdit(it) { form.value = { ...it }; show.value = true }
async function save() {
  try {
    if (form.value.id) await usageApi.update(form.value.id, form.value)
    else await usageApi.create(form.value)
    show.value = false
    await load()
    ElMessage.success('已保存')
  } catch (e) { ElMessage.error(e.message) }
}
async function patch(it, key, value) {
  try {
    await usageApi.update(it.id, { [key]: value })
    await load()
    ElMessage.success('已更新')
  } catch (e) { ElMessage.error(e.message); await load() }
}
function togglePick(id) {
  const i = picked.value.indexOf(id)
  if (i >= 0) picked.value.splice(i, 1)
  else picked.value.push(id)
}
onMounted(load)
const line = ref('')
async function add() {
  if (!line.value.trim()) return
  const parts = line.value.split(/[,，\t]/).map(s => s.trim())
  const payload = { code: parts[0], name: parts[1] }
  BODY_FIELDS.forEach((fd, i) => { if (parts[i + 2] !== undefined) payload[fd.k] = parts[i + 2] })
  try {
    await usageApi.create(payload)
    line.value = ''
    await load()
  } catch (e) { ElMessage.error(e.message) }
}
</script>
<style scoped>
.pg { padding: 4px 2px 40px; color: #303133; }
.pg h2 { margin: 0; font-size: 19px; }
.hd { display: flex; align-items: center; gap: 14px; margin-bottom: 16px; flex-wrap: wrap; }
.hd .hint { color: #888; font-size: 13px; flex: 1; }
.btn { border: 1px solid var(--el-color-primary); background: #fff; color: var(--el-color-primary);
  border-radius: 6px; padding: 6px 14px; cursor: pointer; font-size: 13px; }
.btn:hover { background: var(--el-color-primary-light-9); }
.btn.solid { background: var(--el-color-primary); color: #fff; }
.btn.sm { padding: 3px 10px; font-size: 12px; }
.frm .fr { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.frm .fr label { width: 88px; text-align: right; color: #666; font-size: 13px; }
.blank { color: #bbb; padding: 30px; text-align: center; }
.search { display: none; }
.qe { display: flex; gap: 10px; align-items: center; background: var(--el-color-primary-light-9);
  border: 1px solid var(--el-color-primary-light-7); border-radius: 10px; padding: 12px 14px;
  margin-bottom: 14px; }
.qe input { flex: 1; border: 1px solid #e3e3e3; border-radius: 6px; padding: 8px 12px; font-size: 14px; }
.cnt { font-size: 12px; color: var(--el-color-primary-dark-2); }
.feed { background: #fff; border: 1px solid #eee; border-radius: 10px; overflow: hidden; }
.fi { display: grid; grid-template-columns: 110px 1fr 100px 60px; gap: 10px; align-items: center;
  padding: 9px 14px; border-bottom: 1px solid #f7f7f7; font-size: 13px; }
.fi-code { font-family: ui-monospace, monospace; color: #aaa; }
.fi-st { color: var(--el-color-primary-dark-2); }

</style>

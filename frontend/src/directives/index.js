/**
 * 两个自己写的小指令：
 *  - v-autofocus：进页面就落在第一个输入框上，连着录入不用点鼠标
 *  - v-digits：输入框只让敲数字
 */
export const autofocus = {
  mounted(el) {
    const box = el.querySelector ? el.querySelector('input') : null
    const target = box || el
    if (target && target.focus) {
      setTimeout(() => target.focus(), 60)
    }
  }
}

export const digits = {
  mounted(el) {
    const input = el.tagName === 'INPUT' ? el : el.querySelector('input')
    if (!input) return
    input.addEventListener('input', () => {
      input.value = input.value.replace(/[^\d]/g, '')
    })
  }
}

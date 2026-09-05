export async function copyText(text) {
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(text)
      return { ok: true }
    } catch (e) {
      // 继续降级
    }
  }
  return fallbackCopy(text)
}

function fallbackCopy(text) {
  try {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    const successful = document.execCommand('copy')
    document.body.removeChild(textarea)
    if (successful) {
      return { ok: true }
    }
  } catch (e) {
    // ignore
  }
  return { ok: false, text }
}
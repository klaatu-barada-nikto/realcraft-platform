const FALLBACK_MESSAGES = {
  400: '输入不合法',
  413: '文件过大或数量超限',
  422: '数据解析失败',
  502: '上游推理失败',
  500: '存储失败',
}

const NETWORK_ERROR = '网络异常或服务不可用'

export class GenerateError extends Error {
  constructor(message, code = 0) {
    super(message)
    this.name = 'GenerateError'
    this.code = code
  }
}

export async function generateModel(files) {
  const formData = new FormData()
  for (const file of files) {
    formData.append('images', file)
  }

  let response
  try {
    response = await fetch('/api/generate', {
      method: 'POST',
      body: formData,
    })
  } catch (e) {
    throw new GenerateError(NETWORK_ERROR)
  }

  if (!response.ok) {
    throw new GenerateError(NETWORK_ERROR)
  }

  let payload
  try {
    payload = await response.json()
  } catch (e) {
    throw new GenerateError(NETWORK_ERROR)
  }

  if (payload && payload.code === 200) {
    return payload.data && payload.data.json_url
  }

  const code = payload ? payload.code : 0
  const message = (payload && payload.message) || FALLBACK_MESSAGES[code] || NETWORK_ERROR
  throw new GenerateError(message, code)
}

export async function fetchModel(jsonUrl) {
  let pathname
  try {
    pathname = new URL(jsonUrl).pathname
  } catch (e) {
    throw new GenerateError(NETWORK_ERROR)
  }

  let response
  try {
    response = await fetch(pathname)
  } catch (e) {
    throw new GenerateError(NETWORK_ERROR)
  }

  if (!response.ok) {
    throw new GenerateError(NETWORK_ERROR)
  }

  try {
    return await response.json()
  } catch (e) {
    throw new GenerateError(NETWORK_ERROR)
  }
}

export { FALLBACK_MESSAGES, NETWORK_ERROR }
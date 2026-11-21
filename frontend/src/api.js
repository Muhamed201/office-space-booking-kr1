const sessionKey = 'office-booking-session'
let refreshRequest

function readSession() {
  try {
    return JSON.parse(sessionStorage.getItem(sessionKey))
  } catch {
    return null
  }
}

export function setSession(session) {
  if (session) {
    sessionStorage.setItem(sessionKey, JSON.stringify({
      accessToken: session.accessToken, refreshToken: session.refreshToken
    }))
  } else {
    sessionStorage.removeItem(sessionKey)
    window.dispatchEvent(new Event('office-session-ended'))
  }
}

export function hasSession() {
  return Boolean(readSession()?.accessToken)
}

export function queryString(parameters) {
  const entries = Object.entries(parameters).filter(([, value]) => value !== '' && value != null)
  return new URLSearchParams(entries).toString()
}

async function request(path, { method = 'GET', body, authenticated = true, signal, raw = false } = {}) {
  const headers = new Headers()
  const token = authenticated && readSession()?.accessToken
  if (token) headers.set('Authorization', 'Bearer ' + token)
  if (body != null && !(body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
    body = JSON.stringify(body)
  }

  let response
  try {
    response = await fetch('/api' + path, { method, headers, body, signal })
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new Error('Нет связи с сервером. Попробуйте ещё раз.')
  }
  if (!response.ok) {
    const fallback = {
      401: 'Войдите в систему', 403: 'Недостаточно прав', 404: 'Запись не найдена',
      409: 'Данные изменились или конфликтуют с другой записью'
    }
    const payload = await response.json().catch(() => null)
    const error = new Error(payload?.message || fallback[response.status] || 'Не удалось выполнить запрос')
    error.status = response.status
    throw error
  }
  if (raw) return response
  return response.status === 204 ? null : response.json()
}

function refresh() {
  if (!refreshRequest) {
    const session = readSession()
    if (!session?.refreshToken) {
      return Promise.reject(new Error('Сессия завершилась'))
    }
    refreshRequest = request('/auth/refresh', {
      method: 'POST', body: { refreshToken: session.refreshToken }, authenticated: false
    }).then(tokens => {
      if (readSession()?.refreshToken === session.refreshToken) setSession(tokens)
    }).finally(() => { refreshRequest = null })
  }
  return refreshRequest
}

export async function api(path, options = {}) {
  const previousToken = readSession()?.accessToken
  try {
    return await request(path, options)
  } catch (error) {
    if (error.status !== 401 || options.authenticated === false || !previousToken) throw error
    try {
      if (readSession()?.accessToken === previousToken) await refresh()
    } catch (refreshError) {
      if (refreshError.status === 401) setSession(null)
      throw refreshError
    }
    return request(path, options)
  }
}

export async function login(email, password) {
  const session = await request('/auth/login', { method: 'POST', body: { email, password }, authenticated: false })
  setSession(session)
}

export async function logout() {
  if (refreshRequest) await refreshRequest.catch(() => {})
  const token = readSession()?.refreshToken
  if (token) {
    try {
      await request('/auth/logout', { method: 'POST', body: { refreshToken: token }, authenticated: false })
    } catch (error) {
      if (error.status !== 401) throw error
    }
  }
  setSession(null)
}

import React, { useEffect, useState } from 'react'
import { api } from './api'

export const statuses = {
  ACTIVE: 'Активна', CANCELLED: 'Отменена', COMPLETED: 'Завершена', NO_SHOW: 'Неявка'
}
export const workspaceStatuses = {
  AVAILABLE: 'Доступно', MAINTENANCE: 'Обслуживание', DECOMMISSIONED: 'Списано'
}
export const types = {
  OPEN_DESK: 'Открытый стол', MEETING_ROOM: 'Переговорная', PHONE_BOOTH: 'Телефонная будка'
}
export const roles = {
  EMPLOYEE: 'Сотрудник', OFFICE_MANAGER: 'Офис-менеджер', ADMIN: 'Администратор'
}

export function useResource(path, revision = 0) {
  const [result, setResult] = useState({ data: null, error: '', loading: Boolean(path) })
  useEffect(() => {
    if (!path) return
    const abort = new AbortController()
    setResult(previous => ({ ...previous, error: '', loading: true }))
    api(path, { signal: abort.signal }).then(data => {
      if (!abort.signal.aborted) setResult({ data, error: '', loading: false })
    }).catch(error => {
      if (!abort.signal.aborted) setResult({ data: null, error: error.message, loading: false })
    })
    return () => abort.abort()
  }, [path, revision])
  return result
}

export function Header({ title, children }) {
  return <header className="page-heading"><h1>{title}</h1>{children && <div className="actions">{children}</div>}</header>
}

export function Field({ label, children, wide = false }) {
  return <label className={wide ? 'field span-all' : 'field'}><span>{label}</span>{children}</label>
}

export function Notice({ error, children }) {
  if (!error && !children) return null
  return <p className={error ? 'notice error' : 'notice'} role={error ? 'alert' : 'status'}>{error || children}</p>
}

export function Pager({ page, onPage }) {
  if (!page || page.totalPages < 2) return null
  return <nav className="pager" aria-label="Страницы">
    <button disabled={page.number <= 0} onClick={() => onPage(page.number - 1)}>Назад</button>
    <span>Страница {page.number + 1} из {page.totalPages}</span>
    <button disabled={page.number + 1 >= page.totalPages} onClick={() => onPage(page.number + 1)}>Вперёд</button>
  </nav>
}

export function Options({ values }) {
  return Object.entries(values).map(([value, label]) => <option key={value} value={value}>{label}</option>)
}

export function validatePassword(password) {
  if (password.trim() === '' || password.length < 8 || new TextEncoder().encode(password).length > 72) {
    throw new Error('Пароль должен содержать минимум 8 символов и не больше 72 байт.')
  }
}

export function formatTime(value) {
  if (!value) return '—'
  const [date, time] = value.split('T')
  const [year, month, day] = date.split('-')
  return [day + '.' + month + '.' + year, time?.slice(0, 5)].filter(Boolean).join(' · ')
}

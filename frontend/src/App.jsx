import React, { useEffect, useState } from 'react'
import { BrowserRouter, Link, Navigate, NavLink, Route, Routes } from 'react-router-dom'
import { api, hasSession, logout, setSession } from './api'
import { Field, Notice, roles, validatePassword } from './ui'
import Login from './pages/Login'
import Workspaces from './pages/Workspaces'
import Bookings from './pages/Bookings'
import Analytics from './pages/Analytics'
import { Audit, Users } from './pages/Admin'

function PasswordForm({ required, onSuccess, onClose, onLogout }) {
  const [passwords, setPasswords] = useState({ currentPassword: '', newPassword: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      validatePassword(passwords.newPassword)
      setSession(await api('/auth/change-password', { method: 'POST', body: passwords }))
      await onSuccess()
    } catch (error) {
      setError(error.message)
    } finally {
      setBusy(false)
    }
  }

  return <section className={required ? 'panel auth-panel' : 'panel editor'}>
    <h2>{required ? 'Смените временный пароль' : 'Смена пароля'}</h2>
    <Notice error={error} />
    <form className="stack" onSubmit={submit}>
      <Field label="Текущий пароль"><input type="password" autoComplete="current-password" required
        value={passwords.currentPassword} onChange={event => setPasswords({ ...passwords, currentPassword: event.target.value })} /></Field>
      <Field label="Новый пароль"><input type="password" autoComplete="new-password" required minLength={8}
        value={passwords.newPassword} onChange={event => setPasswords({ ...passwords, newPassword: event.target.value })} /></Field>
      <div className="actions">
        <button className="primary" disabled={busy}>Сохранить пароль</button>
        {required
          ? <button type="button" onClick={onLogout}>Выйти</button>
          : <button type="button" onClick={onClose}>Отмена</button>}
      </div>
    </form>
  </section>
}

export default function App() {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(hasSession)
  const [error, setError] = useState('')
  const [changingPassword, setChangingPassword] = useState(false)

  async function loadUser() {
    setError('')
    try {
      if (hasSession()) setUser(await api('/auth/me'))
    } catch (error) {
      if (error.status === 401) setSession(null)
      else setError(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function signOut() {
    try {
      await logout()
    } catch (error) {
      setError(error.message)
    }
  }

  useEffect(() => {
    function endSession() {
      setUser(null)
      setChangingPassword(false)
    }
    window.addEventListener('office-session-ended', endSession)
    loadUser()
    return () => window.removeEventListener('office-session-ended', endSession)
  }, [])

  if (loading) return <div className="auth-shell"><p role="status">Загрузка…</p></div>
  if (!user) return <div className="auth-shell">
    <Login onLogin={loadUser} />
    {error && <div className="auth-panel"><Notice error={error} /><button onClick={loadUser}>Повторить</button></div>}
  </div>
  if (user.temporaryPassword) return <div className="auth-shell">
    <Notice error={error} /><PasswordForm required onSuccess={loadUser} onLogout={signOut} />
  </div>

  const admin = user.role === 'ADMIN'
  return <BrowserRouter>
    <div className="app-shell">
      <aside className="sidebar">
        <Link className="brand" to="/bookings">Office Booking</Link>
        <nav aria-label="Разделы">
          <NavLink to="/bookings">Бронирования</NavLink>
          <NavLink to="/workspaces">Рабочие места</NavLink>
          <NavLink to="/analytics">Аналитика</NavLink>
          {admin && <>
            <NavLink to="/users">Пользователи</NavLink>
            <NavLink to="/audit">Журнал аудита</NavLink>
          </>}
        </nav>
        <div className="account">
          <strong>{user.name}</strong><span>{roles[user.role]}</span>
          <button className="sidebar-button" onClick={() => setChangingPassword(true)}>Сменить пароль</button>
          <button className="sidebar-button" onClick={signOut}>Выйти</button>
        </div>
      </aside>
      <main className="main">
        <Notice error={error} />
        {changingPassword && <PasswordForm onClose={() => setChangingPassword(false)} onSuccess={async () => {
          await loadUser()
          setChangingPassword(false)
        }} />}
        <Routes>
          <Route path="/bookings" element={<Bookings user={user} />} />
          <Route path="/workspaces" element={<Workspaces user={user} />} />
          <Route path="/analytics" element={<Analytics />} />
          {admin && <>
            <Route path="/users" element={<Users />} />
            <Route path="/audit" element={<Audit />} />
          </>}
          <Route path="*" element={<Navigate to="/bookings" replace />} />
        </Routes>
      </main>
    </div>
  </BrowserRouter>
}

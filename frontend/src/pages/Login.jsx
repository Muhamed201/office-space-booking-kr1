import React, { useState } from 'react'
import { login } from '../api'
import { Field, Notice } from '../ui'

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await login(email, password)
      await onLogin()
    } catch (error) {
      setError(error.message)
    } finally {
      setLoading(false)
    }
  }

  return <section className="panel auth-panel">
    <p className="auth-brand">Office Booking</p>
    <h1>Вход в систему</h1>
    <Notice error={error} />
    <form className="stack" onSubmit={submit}>
      <Field label="Электронная почта"><input type="email" autoComplete="username" required maxLength={150}
        value={email} onChange={event => setEmail(event.target.value)} /></Field>
      <Field label="Пароль"><input type="password" autoComplete="current-password" required maxLength={72}
        value={password} onChange={event => setPassword(event.target.value)} /></Field>
      <button className="primary" disabled={loading}>{loading ? 'Вход…' : 'Войти'}</button>
    </form>
  </section>
}

import React, { useState } from 'react'
import { api, queryString } from '../api'
import { Field, formatTime, Header, Notice, Options, Pager, roles, useResource, validatePassword } from '../ui'

const emptyUser = { id: '', name: '', email: '', role: 'EMPLOYEE', active: true, temporaryPassword: '' }

export function Users() {
  const [page, setPage] = useState(0)
  const [revision, refresh] = useState(0)
  const [draft, setDraft] = useState(null)
  const [resetUser, setResetUser] = useState(null)
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const users = useResource('/admin/users?' + queryString({ page }), revision)

  async function submit(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      if (resetUser) {
        validatePassword(password)
        await api('/admin/users/' + resetUser.id + '/reset-password', { method: 'POST', body: { temporaryPassword: password } })
      } else {
        const { id, name, email, role, active, temporaryPassword } = draft
        if (!id) validatePassword(temporaryPassword)
        await api(id ? '/admin/users/' + id : '/admin/users', {
          method: id ? 'PUT' : 'POST',
          body: id ? { name, email, role, active } : { name, email, role, temporaryPassword }
        })
      }
      dismiss()
      refresh(value => value + 1)
    } catch (error) {
      setError(error.message)
    } finally {
      setBusy(false)
    }
  }

  function dismiss() {
    setDraft(null)
    setResetUser(null)
    setPassword('')
    setError('')
  }

  function edit(user) {
    dismiss()
    setDraft(user)
  }

  return <>
    <Header title="Пользователи"><button className="primary" onClick={() => edit({ ...emptyUser })}>Добавить пользователя</button></Header>
    <Notice error={error || users.error}>{users.loading ? 'Загрузка пользователей…' : ''}</Notice>
    {users.error && <button onClick={() => refresh(value => value + 1)}>Повторить</button>}
    {(draft || resetUser) && <section className="panel editor">
      <h2>{resetUser ? 'Сброс пароля · ' + resetUser.name : draft.id ? 'Редактирование пользователя' : 'Новый пользователь'}</h2>
      <form className="form-grid" onSubmit={submit}>
        {resetUser ? <Field label="Новый временный пароль"><input type="password" autoComplete="new-password" required
          minLength={8} value={password} onChange={e => setPassword(e.target.value)} /></Field> : <>
          <Field label="Имя"><input required maxLength={150} value={draft.name} onChange={e => setDraft({ ...draft, name: e.target.value })} /></Field>
          <Field label="Электронная почта"><input required type="email" maxLength={150} value={draft.email} onChange={e => setDraft({ ...draft, email: e.target.value })} /></Field>
          <Field label="Роль"><select value={draft.role} onChange={e => setDraft({ ...draft, role: e.target.value })}><Options values={roles} /></select></Field>
          {draft.id ? <label className="check"><input type="checkbox" checked={draft.active} onChange={e => setDraft({ ...draft, active: e.target.checked })} /> Учётная запись активна</label>
            : <Field label="Временный пароль"><input type="password" autoComplete="new-password" required minLength={8}
              value={draft.temporaryPassword} onChange={e => setDraft({ ...draft, temporaryPassword: e.target.value })} /></Field>}
        </>}
        <div className="actions span-all">
          <button className="primary" disabled={busy}>Сохранить</button><button type="button" onClick={dismiss}>Отмена</button>
        </div>
      </form>
    </section>}
    {users.data && <section className="panel">
      <div className="table-wrap"><table>
        <thead><tr><th>Имя</th><th>Электронная почта</th><th>Роль</th><th>Статус</th><th>Действия</th></tr></thead>
        <tbody>{users.data.content.map(user => <tr key={user.id}>
          <th scope="row">{user.name}</th><td>{user.email}</td><td>{roles[user.role]}</td>
          <td>{!user.active ? 'Отключён' : user.temporaryPassword ? 'Временный пароль' : 'Активен'}</td>
          <td><div className="actions">
            <button onClick={() => edit(user)}>Изменить</button>
            <button onClick={() => { dismiss(); setResetUser(user) }}>Сбросить пароль</button>
          </div></td>
        </tr>)}</tbody>
      </table></div>
      <Pager page={users.data.page} onPage={setPage} />
    </section>}
  </>
}

const events = {
  LOGIN: 'Вход', LOGOUT: 'Выход', CREATE: 'Создание', UPDATE: 'Изменение',
  ARCHIVE: 'Архивирование', RESTORE: 'Восстановление', DELETE: 'Удаление', LOGIN_FAILED: 'Неудачный вход'
}

export function Audit() {
  const [form, setForm] = useState({ q: '', type: '', sort: 'createdAt,desc' })
  const [filter, setFilter] = useState(form)
  const [page, setPage] = useState(0)
  const [revision, refresh] = useState(0)
  const result = useResource('/admin/audit?' + queryString({ ...filter, page }), revision)
  return <>
    <Header title="Журнал аудита" />
    <form className="panel filter-bar" onSubmit={e => { e.preventDefault(); setPage(0); setFilter(form) }}>
      <Field label="Поиск"><input value={form.q} onChange={e => setForm({ ...form, q: e.target.value })} /></Field>
      <Field label="Событие"><select value={form.type} onChange={e => setForm({ ...form, type: e.target.value })}>
        <option value="">Все</option><Options values={events} /></select></Field>
      <Field label="Сортировка"><select value={form.sort} onChange={e => setForm({ ...form, sort: e.target.value })}>
        <option value="createdAt,desc">Сначала новые</option><option value="createdAt,asc">Сначала старые</option>
        <option value="eventType,asc">По типу события</option></select></Field>
      <button className="primary">Показать</button>
    </form>
    <Notice error={result.error}>{result.loading ? 'Загрузка журнала…' : ''}</Notice>
    {result.error && <button onClick={() => refresh(value => value + 1)}>Повторить</button>}
    {result.data && <section className="panel">
      {result.data.content.length === 0 ? <Notice>События не найдены.</Notice> : <div className="table-wrap"><table>
        <thead><tr><th>Время</th><th>Событие</th><th>Описание</th><th>Пользователь</th><th>Запрос</th></tr></thead>
        <tbody>{result.data.content.map(entry => <tr key={entry.id}>
          <td>{formatTime(entry.createdAt)}</td><td>{events[entry.eventType] || entry.eventType}</td>
          <td>{entry.description}<small>{entry.entityType} {entry.entityId}</small></td>
          <td className="id">{entry.userId || '—'}</td><td className="id">{entry.correlationId || '—'}</td>
        </tr>)}</tbody>
      </table></div>}
      <Pager page={result.data.page} onPage={setPage} />
    </section>}
  </>
}

import React, { useState } from 'react'
import { api } from '../api'
import { Field, formatTime, Notice, useResource } from '../ui'

const empty = { content: '', isPinned: false }

export default function BookingNotes({ booking, user, onClose }) {
  const [draft, setDraft] = useState(empty)
  const [revision, refresh] = useState(0)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const path = '/bookings/' + booking.id + '/notes'
  const notes = useResource(path, revision)

  async function change(operation) {
    setBusy(true)
    setError('')
    try {
      await operation()
      setDraft(empty)
      refresh(value => value + 1)
    } catch (error) {
      setError(error.message)
    } finally {
      setBusy(false)
    }
  }

  return <section className="panel editor">
    <div className="section-heading"><h2>Заметки · {booking.bookingNumber}</h2><button type="button" onClick={onClose}>Закрыть</button></div>
    <Notice error={error || notes.error}>{notes.loading ? 'Загрузка заметок…' : ''}</Notice>
    {notes.error && <button onClick={() => refresh(value => value + 1)}>Повторить</button>}
    {notes.data?.length === 0 && <p className="muted">Заметок пока нет.</p>}
    <ul className="notes">{notes.data?.map(note => <li key={note.id}>
      <strong>{note.authorName}</strong>{note.isPinned && <span className="status inactive">Закреплена</span>}
      <small>{formatTime(note.createdAt)}</small>
      <p>{note.content}</p>
      {(note.authorId === user.id || user.role === 'ADMIN') && <div className="actions">
        <button type="button" onClick={() => setDraft(note)}>Изменить</button>
        <button type="button" className="danger" disabled={busy} onClick={() => {
          if (confirm('Удалить заметку?')) change(() => api('/notes/' + note.id, { method: 'DELETE' }))
        }}>Удалить</button>
      </div>}
    </li>)}</ul>
    <form className="stack" onSubmit={e => {
      e.preventDefault()
      change(() => api(draft.id ? '/notes/' + draft.id : path, {
        method: draft.id ? 'PUT' : 'POST', body: { content: draft.content, isPinned: draft.isPinned }
      }))
    }}>
      <Field label={draft.id ? 'Редактировать заметку' : 'Новая заметка'}><textarea required maxLength={2000} rows={3}
        value={draft.content} onChange={e => setDraft({ ...draft, content: e.target.value })} /></Field>
      <label className="check"><input type="checkbox" checked={draft.isPinned} onChange={e => setDraft({ ...draft, isPinned: e.target.checked })} /> Закрепить</label>
      <div className="actions"><button className="primary" disabled={busy}>{draft.id ? 'Сохранить' : 'Добавить заметку'}</button>
        {draft.id && <button type="button" onClick={() => setDraft(empty)}>Отмена</button>}</div>
    </form>
  </section>
}

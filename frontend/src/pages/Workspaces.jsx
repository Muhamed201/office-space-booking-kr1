import React, { useState } from 'react'
import { api } from '../api'
import { Field, Header, Notice, Options, types, useResource, workspaceStatuses } from '../ui'

const empty = { code: '', floor: 1, type: 'OPEN_DESK', status: 'AVAILABLE', hasMonitor: false, capacity: 1 }

export default function Workspaces({ user }) {
  const [revision, refresh] = useState(0)
  const [draft, setDraft] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const places = useResource('/workspaces', revision)
  const admin = user.role === 'ADMIN'

  function edit(next) {
    setError('')
    setDraft(next)
  }

  async function save(event) {
    event.preventDefault()
    await mutate(async () => {
      const { id, code, floor, type, status, hasMonitor, capacity } = draft
      await api(id ? '/workspaces/' + id : '/workspaces', {
        method: id ? 'PUT' : 'POST',
        body: { code, floor: Number(floor), type, status, hasMonitor, capacity: Number(capacity) }
      })
      setDraft(null)
    })
  }

  async function mutate(operation) {
    setBusy(true)
    setError('')
    try {
      await operation()
      refresh(value => value + 1)
    } catch (error) {
      setError(error.message)
    } finally {
      setBusy(false)
    }
  }

  const set = (key, value) => setDraft({ ...draft, [key]: value })

  return <>
    <Header title="Рабочие места">
      {admin && <button className="primary" onClick={() => edit({ ...empty })}>Добавить место</button>}
    </Header>
    <Notice error={error || places.error} />
    {places.error && <button onClick={() => refresh(value => value + 1)}>Повторить</button>}
    {draft && <section className="panel editor">
      <h2>{draft.id ? 'Редактирование места' : 'Новое рабочее место'}</h2>
      <form className="form-grid" onSubmit={save}>
        <Field label="Код"><input required maxLength={7} pattern="[A-Z]{1,3}-[0-9]{1,3}" title="Формат A-14" value={draft.code} onChange={e => set('code', e.target.value)} /></Field>
        <Field label="Этаж"><input required type="number" min={-2} max={50} value={draft.floor} onChange={e => set('floor', e.target.value)} /></Field>
        <Field label="Тип"><select value={draft.type} onChange={e => setDraft({ ...draft, type: e.target.value, capacity: e.target.value === 'MEETING_ROOM' ? draft.capacity : 1 })}><Options values={types} /></select></Field>
        <Field label="Статус"><select value={draft.status} onChange={e => set('status', e.target.value)}><Options values={workspaceStatuses} /></select></Field>
        <Field label="Вместимость"><input required type="number" min={1} max={100} disabled={draft.type !== "MEETING_ROOM"} value={draft.capacity} onChange={e => set('capacity', e.target.value)} /></Field>
        <label className="check"><input type="checkbox" checked={draft.hasMonitor} onChange={e => set('hasMonitor', e.target.checked)} /> Есть монитор</label>
        <div className="actions span-all">
          <button className="primary" disabled={busy}>Сохранить</button>
          <button type="button" onClick={() => edit(null)}>Отмена</button>
        </div>
      </form>
    </section>}
    <section className="panel">
      {places.loading && <Notice>Загрузка рабочих мест…</Notice>}
      {!places.loading && places.data?.length === 0 && <Notice>Рабочих мест пока нет.</Notice>}
      {places.data?.length > 0 && <div className="table-wrap"><table>
        <thead><tr><th>Код</th><th>Тип</th><th>Этаж</th><th>Вместимость</th><th>Монитор</th><th>Статус</th>
          {admin && <th>Действия</th>}</tr></thead>
        <tbody>{places.data.map(place => <tr key={place.id}>
          <th scope="row">{place.code}</th><td>{types[place.type]}</td><td>{place.floor}</td>
          <td>{place.capacity}</td><td>{place.hasMonitor ? 'Есть' : '—'}</td>
          <td><span className={'status ' + (place.enabled ? place.status.toLowerCase() : 'inactive')}>
            {place.enabled ? workspaceStatuses[place.status] : 'Отключено'}</span></td>
          {admin && <td><div className="actions">
            <button onClick={() => edit(place)}>Изменить</button>
            <button disabled={busy} onClick={() => mutate(() => api('/workspaces/' + place.id + (place.enabled ? '/disable' : '/enable'),
              { method: 'POST' }))}>{place.enabled ? 'Отключить' : 'Включить'}</button>
          </div></td>}
        </tr>)}</tbody>
      </table></div>}
    </section>
  </>
}

import React, { useEffect, useState } from 'react'
import { api, queryString } from '../api'
import { Field, formatTime, Header, Notice, Options, Pager, statuses, useResource } from '../ui'
import BookingEditor from './BookingEditor'
import BookingNotes from './BookingNotes'
import BookingCsv from './BookingCsv'

const initialFilters = {
  q: '', archived: 'false', status: '', workspaceId: '', sort: 'createdAt,desc', tag: '',
  isRecurring: '', requiresEquipment: '', priority: '', minAttendees: '', minDuration: '', maxDuration: ''
}
const bulkActions = {
  ARCHIVE: 'Архивировать', RESTORE: 'Восстановить', CHANGE_STATUS: 'Изменить статус',
  ADD_TAG: 'Добавить тег', REMOVE_TAG: 'Удалить тег'
}

function Filters({ workspaces, onApply }) {
  const [form, setForm] = useState(initialFilters)
  const change = key => ({ value: form[key], onChange: event => setForm({ ...form, [key]: event.target.value }) })
  return <form className="panel filters" onSubmit={e => { e.preventDefault(); onApply(form) }}>
    <div className="filter-bar">
      <Field label="Поиск"><input {...change('q')} maxLength={200} placeholder="Цель, описание или сотрудник" /></Field>
      <Field label="Статус"><select {...change('status')}><option value="">Все статусы</option><Options values={statuses} /></select></Field>
      <Field label="Архив"><select {...change('archived')}>
        <option value="false">Текущие</option><option value="true">Архивные</option><option value="">Все брони</option>
      </select></Field>
      <Field label="Сортировка"><select {...change('sort')}>
        <option value="createdAt,desc">По созданию · новые</option><option value="createdAt,asc">По созданию · старые</option>
        <option value="startTime,asc">По началу брони</option><option value="attendees,desc">По числу участников</option>
      </select></Field>
      <button className="primary">Показать</button>
    </div>
    <details>
      <summary>Дополнительные фильтры</summary>
      <div className="form-grid filter-details">
        <Field label="Рабочее место"><select {...change('workspaceId')}><option value="">Все места</option>
          {workspaces.map(place => <option key={place.id} value={place.id}>{place.code}</option>)}</select></Field>
        <Field label="Тег"><input {...change('tag')} maxLength={50} /></Field>
        <Field label="Повторяющаяся"><select {...change('isRecurring')}><option value="">Любая</option><option value="true">Да</option><option value="false">Нет</option></select></Field>
        <Field label="Нужно оборудование"><select {...change('requiresEquipment')}><option value="">Любая</option><option value="true">Да</option><option value="false">Нет</option></select></Field>
        <Field label="Приоритет"><input type="number" min={1} {...change('priority')} /></Field>
        <Field label="Минимум участников"><input type="number" min={1} {...change('minAttendees')} /></Field>
        <Field label="Длительность от, мин."><input type="number" min={0} {...change('minDuration')} /></Field>
        <Field label="Длительность до, мин."><input type="number" min={0} {...change('maxDuration')} /></Field>
      </div>
    </details>
  </form>
}

export default function Bookings({ user }) {
  const [filter, setFilter] = useState(initialFilters)
  const [page, setPage] = useState(0)
  const [revision, refresh] = useState(0)
  const [editor, setEditor] = useState(null)
  const [notes, setNotes] = useState(null)
  const [importOpen, setImportOpen] = useState(false)
  const [selection, setSelection] = useState([])
  const [bulk, setBulk] = useState({ operation: 'ARCHIVE', tag: '', status: 'CANCELLED' })
  const [result, setResult] = useState(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const manager = user.role !== 'EMPLOYEE'
  const places = useResource('/workspaces')
  const list = useResource('/bookings?' + queryString({ ...filter, page }), revision)
  const rows = list.data?.content || []

  function changePage(value) {
    setPage(value)
    setSelection([])
  }

  useEffect(() => {
    if (!list.loading && list.data && list.data.page.number === page && !list.data.content.length && page > 0) {
      changePage(Math.max(0, list.data.page.totalPages - 1))
    }
  }, [list, page])

  function reload() {
    setSelection([])
    refresh(value => value + 1)
  }

  async function run(operation) {
    setBusy(true)
    setError('')
    setResult(null)
    try {
      await operation()
    } catch (error) {
      setError(error.message)
    } finally {
      setBusy(false)
    }
  }

  function mutate(path, method, message) {
    if (message && !confirm(message)) return
    run(async () => {
      await api(path, { method })
      reload()
    })
  }

  function exportCsv() {
    run(async () => {
      const response = await api('/bookings/export?' + queryString(filter), { raw: true })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(await response.blob())
      link.download = 'bookings.csv'
      document.body.appendChild(link)
      link.click()
      link.remove()
      setTimeout(() => URL.revokeObjectURL(link.href), 1000)
    })
  }

  function showEditor(booking) {
    setNotes(null)
    setEditor({ booking })
  }

  return <>
    <Header title="Бронирования">
      <button disabled={busy} onClick={exportCsv}>Экспорт CSV</button>
      {manager && <button aria-expanded={importOpen} onClick={() => setImportOpen(!importOpen)}>Импорт CSV</button>}
      <button className="primary" disabled={places.loading || Boolean(places.error)} onClick={() => showEditor(null)}>Создать бронь</button>
    </Header>
    <Notice error={error || places.error || list.error} />
    {list.error && <button onClick={reload}>Повторить</button>}
    {importOpen && <BookingCsv onApplied={reload} />}
    {editor && places.data && <BookingEditor key={editor.booking?.id || 'new'} booking={editor.booking}
      workspaces={places.data} onClose={() => setEditor(null)} onSave={() => { setEditor(null); reload() }} />}
    {notes && <BookingNotes key={notes.id} booking={notes} user={user} onClose={() => setNotes(null)} />}
    <Filters workspaces={places.data || []} onApply={next => { setFilter(next); changePage(0) }} />

    {manager && selection.length > 0 && <form className="panel filter-bar" onSubmit={e => {
      e.preventDefault()
      run(async () => {
        const response = await api('/bookings/bulk', { method: 'POST', body: { ids: selection, ...bulk } })
        setResult(response)
        reload()
      })
    }}>
      <Field label={'Выбрано: ' + selection.length}><select value={bulk.operation}
        onChange={e => setBulk({ ...bulk, operation: e.target.value })}><Options values={bulkActions} /></select></Field>
      {bulk.operation === 'CHANGE_STATUS' && <Field label="Статус"><select value={bulk.status}
        onChange={e => setBulk({ ...bulk, status: e.target.value })}><Options values={statuses} /></select></Field>}
      {(bulk.operation === 'ADD_TAG' || bulk.operation === 'REMOVE_TAG') && <Field label="Тег"><input required maxLength={50}
        value={bulk.tag} onChange={e => setBulk({ ...bulk, tag: e.target.value })} /></Field>}
      <button disabled={busy}>Применить</button>
    </form>}
    {result && <section className="panel"><Notice>Выполнено: {result.successCount}. Ошибок: {result.errorCount}.</Notice>
      {result.errors.length > 0 && <ul className="result-errors">{result.errors.map(line => <li key={line}>{line}</li>)}</ul>}
    </section>}

    <section className="panel" aria-busy={list.loading}>
      <Notice>{list.loading ? 'Загрузка броней…' : ''}</Notice>
      {!list.loading && list.data && rows.length === 0 && <Notice>Брони не найдены.</Notice>}
      {rows.length > 0 && <div className="table-wrap"><table>
        <thead><tr>
          {manager && <th><input type="checkbox" aria-label="Выбрать все на странице" checked={rows.every(row => selection.includes(row.id))}
            onChange={e => setSelection(e.target.checked ? rows.map(row => row.id) : [])} /></th>}
          <th>Бронь</th><th>Сотрудник</th><th>Место</th><th>Интервал, МСК</th><th>Статус</th><th>Действия</th>
        </tr></thead>
        <tbody>{rows.map(booking => <tr key={booking.id}>
          {manager && <td><input type="checkbox" aria-label={'Выбрать ' + booking.bookingNumber} checked={selection.includes(booking.id)}
            onChange={e => setSelection(e.target.checked ? [...selection, booking.id] : selection.filter(id => id !== booking.id))} /></td>}
          <th scope="row">{booking.bookingNumber}
            {booking.purpose && <small>{booking.purpose}</small>}
            {booking.tags.length > 0 && <small>{booking.tags.join(', ')}</small>}
          </th>
          <td>{booking.employeeName}</td><td>{booking.workspaceCode}</td>
          <td className="time">{formatTime(booking.startTime)}<small>{formatTime(booking.endTime)}</small></td>
          <td><span className={'status ' + booking.status.toLowerCase()}>{statuses[booking.status]}</span>{booking.archived && <small>В архиве</small>}</td>
          <td><div className="actions">
            <button onClick={() => { setEditor(null); setNotes(booking) }}>Заметки</button>
            {manager && (booking.archived
              ? <button disabled={busy} onClick={() => mutate('/bookings/' + booking.id + '/restore', 'POST')}>Восстановить</button>
              : <><button onClick={() => showEditor(booking)}>Изменить</button>
                <button disabled={busy} onClick={() => mutate('/bookings/' + booking.id, 'DELETE', 'Архивировать бронь?')}>В архив</button></>)}
            {user.canDeleteBookings && <button className="danger" disabled={busy}
              onClick={() => mutate('/bookings/' + booking.id + '/permanent', 'DELETE', 'Удалить бронь окончательно?')}>Удалить</button>}
          </div></td>
        </tr>)}</tbody>
      </table></div>}
      <Pager page={list.data?.page} onPage={changePage} />
    </section>
  </>
}

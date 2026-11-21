import React, { useState } from 'react'
import { api } from '../api'
import { Field, Notice, Options, statuses } from '../ui'

export default function BookingEditor({ booking, workspaces, onSave, onClose }) {
  const [form, setForm] = useState({
    workspaceId: booking?.workspaceId || '', startTime: booking?.startTime?.slice(0, 16) || '',
    endTime: booking?.endTime?.slice(0, 16) || '', status: booking?.status || 'ACTIVE',
    attendees: booking?.attendees ?? '', purpose: booking?.purpose || '', description: booking?.description || '',
    isRecurring: booking?.isRecurring || false, requiresEquipment: booking?.requiresEquipment || false,
    tags: booking?.tags?.join('\n') || ''
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const workspace = workspaces.find(place => place.id === form.workspaceId)
  const editableTime = !booking || booking.status === 'ACTIVE'
  const set = (key, value) => setForm({ ...form, [key]: value })
  const available = workspaces.filter(place => (place.enabled && place.status === 'AVAILABLE') || place.id === booking?.workspaceId)

  async function save(event) {
    event.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (!workspace) throw new Error('Выберите рабочее место.')
      const { tags, attendees, ...values } = form
      const body = {
        ...values,
        attendees: workspace?.type === 'MEETING_ROOM' && attendees !== '' ? Number(attendees) : null,
        tags: [...new Set(tags.split(/\r?\n/).map(tag => tag.trim()).filter(Boolean))]
      }
      const scheduleChanged = !booking || workspace.id !== booking.workspaceId
        || values.startTime !== booking.startTime.slice(0, 16) || values.endTime !== booking.endTime.slice(0, 16)
      if (scheduleChanged) {
        const [startDate, startHour] = values.startTime.split('T')
        const [endDate, endHour] = values.endTime.split('T')
        if (startDate !== endDate || startHour >= endHour || startHour < '08:00' || endHour > '22:00'
          || !startHour.endsWith(':00') || !endHour.endsWith(':00')) {
          throw new Error('Укажите целые часы в одни сутки с 08:00 до 22:00.')
        }
        const maximumHours = { OPEN_DESK: 10, MEETING_ROOM: 4, PHONE_BOOTH: 1 }[workspace.type]
        if (Number(endHour.slice(0, 2)) - Number(startHour.slice(0, 2)) > maximumHours) {
          throw new Error('Максимальная длительность для выбранного места: ' + maximumHours + ' ч.')
        }
      }
      if (body.tags.some(tag => tag.length > 50)) throw new Error('Каждый тег должен быть не длиннее 50 символов.')
      if (booking) body.version = booking.version
      else delete body.status
      await api(booking ? '/bookings/' + booking.id : '/bookings', { method: booking ? 'PUT' : 'POST', body })
      onSave()
    } catch (error) {
      setError(error.message)
    } finally {
      setBusy(false)
    }
  }

  return <section className="panel editor">
    <h2>{booking ? 'Редактирование · ' + booking.bookingNumber : 'Новая бронь'}</h2>
    <Notice error={error} />
    <form className="form-grid" onSubmit={save}>
      <Field label="Рабочее место"><select required disabled={!editableTime} value={form.workspaceId} onChange={e => {
        setForm({ ...form, workspaceId: e.target.value, attendees: '' })
      }}>
        <option value="">Выберите место</option>
        {available.map(place => <option key={place.id} value={place.id}>{place.code} · этаж {place.floor}</option>)}
      </select></Field>
      {booking && <Field label="Статус"><select disabled={booking.status !== 'ACTIVE'} value={form.status} onChange={e => set('status', e.target.value)}>
        <Options values={statuses} /></select></Field>}
      <Field label="Начало"><input type="datetime-local" step={3600} required disabled={!editableTime}
        value={form.startTime} onChange={e => set('startTime', e.target.value)} /></Field>
      <Field label="Окончание"><input type="datetime-local" step={3600} required disabled={!editableTime}
        value={form.endTime} onChange={e => set('endTime', e.target.value)} /></Field>
      {workspace?.type === 'MEETING_ROOM' && <Field label={'Участников · до ' + workspace.capacity}><input type="number" min={1}
        max={workspace.capacity} required disabled={!editableTime} value={form.attendees} onChange={e => set('attendees', e.target.value)} /></Field>}
      <Field label="Цель" wide><input maxLength={500} value={form.purpose} onChange={e => set('purpose', e.target.value)} /></Field>
      <Field label="Описание" wide><textarea maxLength={2000} rows={3} value={form.description} onChange={e => set('description', e.target.value)} /></Field>
      <Field label="Теги · по одному на строку" wide><textarea rows={2} value={form.tags} onChange={e => set('tags', e.target.value)} /></Field>
      <label className="check"><input type="checkbox" checked={form.isRecurring} onChange={e => set('isRecurring', e.target.checked)} /> Повторяющаяся</label>
      <label className="check"><input type="checkbox" checked={form.requiresEquipment} onChange={e => set('requiresEquipment', e.target.checked)} /> Нужно оборудование</label>
      <div className="actions span-all">
        <button className="primary" disabled={busy}>Сохранить</button><button type="button" onClick={onClose}>Отмена</button>
      </div>
    </form>
  </section>
}

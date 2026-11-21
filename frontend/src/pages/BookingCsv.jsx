import React, { useState } from 'react'
import { api } from '../api'
import { Field, Notice } from '../ui'

export default function BookingCsv({ onApplied }) {
  const [file, setFile] = useState(null)
  const [mode, setMode] = useState('CREATE')
  const [preview, setPreview] = useState(null)
  const [applied, setApplied] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function send(step) {
    setBusy(true)
    setError('')
    try {
      const body = new FormData()
      body.append('file', file)
      body.append('mode', mode)
      const result = await api('/bookings/import/' + step, { method: 'POST', body })
      if (step === 'preview') {
        setPreview(result)
        setApplied(null)
      } else {
        setApplied(result)
        setPreview(null)
        onApplied()
      }
    } catch (error) {
      setError(error.message)
    } finally {
      setBusy(false)
    }
  }

  const result = applied || preview
  return <section className="panel editor">
    <h2>Импорт CSV</h2>
    <p className="muted">Используйте заголовки из экспортированного файла. Обновление выполняется по номеру брони.</p>
    <Notice error={error} />
    <form className="form-grid" onSubmit={e => { e.preventDefault(); send('preview') }}>
      <Field label="Файл"><input type="file" accept=".csv,text/csv" required disabled={busy}
        onChange={e => { setFile(e.target.files[0]); setPreview(null); setApplied(null) }} /></Field>
      <Field label="Режим"><select value={mode} disabled={busy} onChange={e => {
        setMode(e.target.value); setPreview(null); setApplied(null)
      }}>
        <option value="CREATE">Создать новые брони</option><option value="UPDATE">Обновить существующие</option>
      </select></Field>
      <div className="actions span-all">
        <button disabled={busy || !file}>Проверить файл</button>
        {preview?.successCount > 0 && <button type="button" className="primary" disabled={busy} onClick={() => send('apply')}>Применить импорт</button>}
      </div>
    </form>
    {result && <><Notice>{applied ? 'Применено' : 'Корректных строк'}: {result.successCount}. Ошибок: {result.errorCount}.</Notice>
      {result.errors.length > 0 && <ul className="result-errors">{result.errors.map((line, index) => <li key={index}>{line}</li>)}</ul>}</>}
  </section>
}

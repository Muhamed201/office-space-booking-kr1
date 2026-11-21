import React, { useState } from 'react'
import { Header, Notice, statuses, useResource } from '../ui'

function Breakdown({ title, group, labels = {} }) {
  const entries = Object.entries(group).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
  const maximum = entries[0]?.[1] || 1
  return <section className="panel breakdown">
    <h2>{title}</h2>
    {entries.length === 0 ? <p className="muted">Нет данных</p> : <ul>
      {entries.map(([label, count]) => <li key={label}>
        <span>{labels[label] || label}</span><strong>{count}</strong>
        <div className="bar-track" aria-hidden="true"><div className="bar" style={{ width: (count / maximum * 100) + '%' }} /></div>
      </li>)}
    </ul>}
  </section>
}

export default function Analytics() {
  const [revision, refresh] = useState(0)
  const { data, error, loading } = useResource('/analytics/summary', revision)
  return <>
    <Header title="Аналитика" />
    <Notice error={error}>{loading ? 'Загрузка аналитики…' : ''}</Notice>
    {error && <button onClick={() => refresh(value => value + 1)}>Повторить</button>}
    {data && <>
      <dl className="metrics">
        <div className="panel"><dt>Всего броней</dt><dd>{data.totalBookings}</dd></div>
        <div className="panel"><dt>Активных</dt><dd>{data.activeBookings}</dd></div>
        <div className="panel"><dt>В архиве</dt><dd>{data.archivedBookings}</dd></div>
        <div className="panel"><dt>Повторяющихся</dt><dd>{data.recurringBookings}</dd></div>
        <div className="panel"><dt>В среднем участников</dt><dd>{data.averageAttendees.toLocaleString('ru', { maximumFractionDigits: 1 })}</dd></div>
      </dl>
      <div className="breakdowns">
        <Breakdown title="По статусам" group={data.byStatus} labels={statuses} />
        <Breakdown title="По рабочим местам" group={data.byWorkspace} />
        <Breakdown title="По приоритетам" group={data.byPriority} />
      </div>
    </>}
  </>
}

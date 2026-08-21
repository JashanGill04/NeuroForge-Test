// frontend/src/components/settings/MonitoringTargetsPanel.jsx
import { useState, useEffect } from 'react'
import { Activity, Plus, Trash2, CheckCircle2, XCircle } from 'lucide-react'
import { monitoringTargetsService } from '../../services/monitoringTargetsService'
import { Alert, EmptyState } from '../ui'

const ENVIRONMENTS = ['DEVELOPMENT', 'TESTING', 'STAGING', 'PRODUCTION']
const STRATEGIES = [
  { value: 'HTTP_PING', label: 'HTTP ping (works with any host)' },
  { value: 'PROMETHEUS_SCRAPE', label: 'Prometheus scrape (needs /metrics + scrape job)' }
]

const emptyForm = {
  label: '', environment: 'PRODUCTION', baseUrl: '', healthCheckPath: '/health',
  probeStrategy: 'HTTP_PING', prometheusJobName: '', metricsToken: '', providerLabel: '', enabled: true
}

export default function MonitoringTargetsPanel({ projectId, canEdit }) {
  const [targets, setTargets] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

  const load = () => {
    setLoading(true)
    monitoringTargetsService.getAll(projectId)
      .then(setTargets)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [projectId])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(''); setSuccess(''); setSaving(true)
    try {
      await monitoringTargetsService.create(projectId, form)
      setSuccess('Monitoring target added.')
      setForm(emptyForm)
      setShowForm(false)
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const handleToggle = async (target) => {
    setError('')
    try {
      await monitoringTargetsService.update(projectId, target.id, {
        label: target.label,
        environment: target.environment,
        baseUrl: target.baseUrl,
        healthCheckPath: target.healthCheckPath,
        probeStrategy: target.probeStrategy,
        prometheusJobName: target.prometheusJobName,
        providerLabel: target.providerLabel,
        enabled: !target.enabled
      })
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const handleDelete = async (target) => {
    if (!window.confirm(`Remove monitoring target "${target.label}"?`)) return
    setError('')
    try {
      await monitoringTargetsService.remove(projectId, target.id)
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h2><Activity size={16} /> Monitoring Targets</h2>
        {canEdit && (
          <button className="btn-ghost-sm" onClick={() => setShowForm((v) => !v)}>
            <Plus size={14} /> {showForm ? 'Cancel' : 'Add target'}
          </button>
        )}
      </div>

      <Alert onClose={() => setError('')}>{error}</Alert>
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>

      {showForm && (
        <form onSubmit={handleSubmit} className="modal-form" style={{ marginBottom: 16 }}>
          <div style={{ display: 'flex', gap: 10 }}>
            <label className="field" style={{ flex: 1 }}>
              <span>Label</span>
              <input
                value={form.label}
                onChange={(e) => setForm((f) => ({ ...f, label: e.target.value }))}
                placeholder="e.g. Render production"
                required
              />
            </label>
            <label className="field" style={{ flex: 1 }}>
              <span>Environment</span>
              <select className="inline-select" value={form.environment} onChange={(e) => setForm((f) => ({ ...f, environment: e.target.value }))}>
                {ENVIRONMENTS.map((e) => <option key={e} value={e}>{e}</option>)}
              </select>
            </label>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            <label className="field" style={{ flex: 2 }}>
              <span>Base URL</span>
              <input
                value={form.baseUrl}
                onChange={(e) => setForm((f) => ({ ...f, baseUrl: e.target.value }))}
                placeholder="https://your-app.onrender.com"
                required
              />
            </label>
            <label className="field" style={{ flex: 1 }}>
              <span>Health check path</span>
              <input
                value={form.healthCheckPath}
                onChange={(e) => setForm((f) => ({ ...f, healthCheckPath: e.target.value }))}
                placeholder="/health"
              />
            </label>
          </div>

          <label className="field">
            <span>Probe strategy</span>
            <select className="inline-select" value={form.probeStrategy} onChange={(e) => setForm((f) => ({ ...f, probeStrategy: e.target.value }))}>
              {STRATEGIES.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </select>
          </label>

          {form.probeStrategy === 'PROMETHEUS_SCRAPE' && (
            <div style={{ display: 'flex', gap: 10 }}>
              <label className="field" style={{ flex: 1 }}>
                <span>Prometheus job name</span>
                <input
                  value={form.prometheusJobName}
                  onChange={(e) => setForm((f) => ({ ...f, prometheusJobName: e.target.value }))}
                  placeholder="render-nodejs-backend"
                />
              </label>
              <label className="field" style={{ flex: 1 }}>
                <span>Metrics token</span>
                <input
                  type="password"
                  value={form.metricsToken}
                  onChange={(e) => setForm((f) => ({ ...f, metricsToken: e.target.value }))}
                  placeholder="matches METRICS_TOKEN"
                />
              </label>
            </div>
          )}

          <label className="field">
            <span>Provider (optional label)</span>
            <input
              value={form.providerLabel}
              onChange={(e) => setForm((f) => ({ ...f, providerLabel: e.target.value }))}
              placeholder="render, vercel, aws…"
            />
          </label>

          <button className="btn-primary" type="submit" disabled={saving}>
            {saving ? 'Adding…' : 'Add target'}
          </button>
        </form>
      )}

      {loading ? (
        <div className="empty-sub">Loading targets…</div>
      ) : targets.length === 0 ? (
        <EmptyState title="No monitoring targets yet" subtitle="Add one above to start tracking real uptime." />
      ) : (
        <ul className="list">
          {targets.map((t) => (
            <li key={t.id} className="list-item">
              <div>
                <div className="list-item-title">
                  {t.lastUp === true && <CheckCircle2 size={14} style={{ color: 'var(--success)' }} />}
                  {t.lastUp === false && <XCircle size={14} style={{ color: 'var(--danger)' }} />}
                  {t.label}
                  {!t.enabled && <span className="badge badge-hold" style={{ marginLeft: 6 }}>Paused</span>}
                </div>
                <div className="list-item-sub">
                  {t.baseUrl}{t.healthCheckPath} · {t.probeStrategy === 'PROMETHEUS_SCRAPE' ? 'Prometheus' : 'HTTP ping'}
                  {t.lastResponseTimeMs != null && ` · ${t.lastResponseTimeMs}ms`}
                </div>
              </div>
              {canEdit && (
                <div style={{ display: 'flex', gap: 8 }}>
                  <button className="btn-ghost-sm" onClick={() => handleToggle(t)}>
                    {t.enabled ? 'Pause' : 'Resume'}
                  </button>
                  <button className="btn-ghost-sm" onClick={() => handleDelete(t)} title="Delete target">
                    <Trash2 size={14} />
                  </button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
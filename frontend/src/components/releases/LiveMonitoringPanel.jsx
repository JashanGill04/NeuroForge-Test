// frontend/src/components/releases/LiveMonitoringPanel.jsx
import { Activity, CheckCircle2, XCircle, Clock } from 'lucide-react'
import { EmptyState } from '../ui'

function timeAgo(isoString) {
  if (!isoString) return '—'
  const diffMs = Date.now() - new Date(isoString).getTime()
  const seconds = Math.floor(diffMs / 1000)
  if (seconds < 60) return `${seconds}s ago`
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.floor(minutes / 60)
  return `${hours}h ago`
}

export default function LiveMonitoringPanel({ targets, loading }) {
  const enabledTargets = targets.filter((t) => t.enabled)

  return (
    <div className="panel">
      <div className="panel-header">
        <h2><Activity size={16} /> Live Monitoring</h2>
      </div>

      {loading ? (
        <div className="empty-sub">Checking targets…</div>
      ) : enabledTargets.length === 0 ? (
        <EmptyState
          title="No monitoring targets configured"
          subtitle="Add one from this project's Settings tab to see real uptime here."
        />
      ) : (
        <div className="stat-grid">
          {enabledTargets.map((t) => {
            const isUp = t.lastUp === true
            const isDown = t.lastUp === false
            const isUnknown = t.lastUp == null
            return (
              <div className="stat-card" key={t.id}>
                <div className="stat-label">{t.label}</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 6 }}>
                  {isUp && <CheckCircle2 size={16} style={{ color: 'var(--success)' }} />}
                  {isDown && <XCircle size={16} style={{ color: 'var(--danger)' }} />}
                  <span
                    className="stat-value stat-value-sm"
                    style={{ color: isUp ? 'var(--success)' : isDown ? 'var(--danger)' : 'var(--ink-faint)' }}
                  >
                    {isUp ? 'Healthy' : isDown ? 'Down' : 'No data yet'}
                  </span>
                </div>
                <div className="stat-foot" style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <Clock size={11} />
                  {isUnknown ? 'Waiting for first probe…' : timeAgo(t.lastCheckedAt)}
                  {t.lastResponseTimeMs != null && ` · ${t.lastResponseTimeMs}ms`}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
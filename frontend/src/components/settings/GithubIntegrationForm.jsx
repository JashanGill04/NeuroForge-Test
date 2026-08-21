import { useState, useEffect } from 'react'
import { Github, RefreshCw, Copy, Webhook } from 'lucide-react'
import { projectIntegrationApi } from '../../api/ProjectIntegration'
import { Alert } from '../ui'

const ENVIRONMENTS = ['DEVELOPMENT', 'TESTING', 'STAGING', 'PRODUCTION']
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:9000/api'

export default function GithubIntegrationForm({ projectId, canEdit }) {
  const [integration, setIntegration] = useState(null)
  const [notConnected, setNotConnected] = useState(false)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [saving, setSaving] = useState(false)

  const [form, setForm] = useState({
    githubOwner: '', githubRepo: '', githubBranch: 'main',
    workflowFile: 'ci-cd.yml', githubToken: '', deployHookUrl: ''
  })

  const [webhookEnv, setWebhookEnv] = useState('PRODUCTION')

  const load = () => {
    setLoading(true)
    projectIntegrationApi.get(projectId)
      .then((data) => {
        setIntegration(data)
        setNotConnected(false)
        setForm((f) => ({
          ...f,
          githubOwner: data.githubOwner || '',
          githubRepo: data.githubRepo || '',
          githubBranch: data.githubBranch || 'main',
          workflowFile: data.workflowFile || 'ci-cd.yml',
          deployHookUrl: data.deployHookUrl || ''
        }))
      })
      .catch(() => setNotConnected(true))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [projectId])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setSuccess('')
    setSaving(true)
    try {
      await projectIntegrationApi.connect(projectId, form)
      setSuccess('GitHub repository connected.')
      setForm((f) => ({ ...f, githubToken: '' }))
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const handleRegenerateSecret = async () => {
    if (!window.confirm('Regenerating invalidates the old webhook secret — update it in your GitHub Actions secrets too.')) return
    try {
      await projectIntegrationApi.regenerateSecret(projectId)
      setSuccess('Webhook secret regenerated.')
      load()
    } catch (err) {
      setError(err.message)
    }
  }

  const copySecret = () => {
    navigator.clipboard.writeText(integration.webhookSecret)
    setSuccess('Webhook secret copied to clipboard.')
  }

  const inboundWebhookUrl = `${API_BASE}/deploy-webhooks/generic?projectId=${projectId}&environment=${webhookEnv}`

  const copyInboundUrl = () => {
    navigator.clipboard.writeText(inboundWebhookUrl)
    setSuccess('Inbound webhook URL copied to clipboard.')
  }

  if (loading) return <div className="empty-sub">Loading GitHub connection…</div>

  return (
    <div className="panel">
      <div className="panel-header">
        <h2><Github size={16} /> GitHub Repository</h2>
      </div>

      <Alert onClose={() => setError('')}>{error}</Alert>
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>

      {notConnected && (
        <p className="page-subtitle-inline" style={{ marginBottom: 16 }}>
          No repository connected yet — connect one below to enable build triggers and rollbacks for this project.
        </p>
      )}

      {canEdit ? (
        <form onSubmit={handleSubmit} className="modal-form">
          <div style={{ display: 'flex', gap: 10 }}>
            <label className="field" style={{ flex: 1 }}>
              <span>Repo owner</span>
              <input
                value={form.githubOwner}
                onChange={(e) => setForm((f) => ({ ...f, githubOwner: e.target.value }))}
                placeholder="e.g. RajanGill04"
                required
              />
            </label>
            <label className="field" style={{ flex: 1 }}>
              <span>Repo name</span>
              <input
                value={form.githubRepo}
                onChange={(e) => setForm((f) => ({ ...f, githubRepo: e.target.value }))}
                placeholder="e.g. NeuroForge"
                required
              />
            </label>
          </div>

          <div style={{ display: 'flex', gap: 10 }}>
            <label className="field" style={{ flex: 1 }}>
              <span>Branch</span>
              <input
                value={form.githubBranch}
                onChange={(e) => setForm((f) => ({ ...f, githubBranch: e.target.value }))}
              />
            </label>
            <label className="field" style={{ flex: 1 }}>
              <span>Workflow file</span>
              <input
                value={form.workflowFile}
                onChange={(e) => setForm((f) => ({ ...f, workflowFile: e.target.value }))}
              />
            </label>
          </div>

          <label className="field">
            <span>Personal access token {integration?.tokenConfigured && '(leave blank to keep current token)'}</span>
            <input
              type="password"
              value={form.githubToken}
              onChange={(e) => setForm((f) => ({ ...f, githubToken: e.target.value }))}
              placeholder={integration?.tokenConfigured ? '••••••••••••' : 'ghp_...'}
              required={!integration?.tokenConfigured}
            />
          </label>

          <label className="field">
            <span>Deploy hook URL (optional)</span>
            <input
              value={form.deployHookUrl}
              onChange={(e) => setForm((f) => ({ ...f, deployHookUrl: e.target.value }))}
              placeholder="https://your-host.example.com/deploy-hooks/..."
            />
            <span className="field-hint">
              The "deploy hook" / "build hook" URL from your hosting provider (Render, Railway, Fly.io,
              Vercel, Netlify all expose one). CI calls this after a successful build so a real deploy
              actually happens. Also add this same value as a <code>DEPLOY_HOOK_URL</code> repository
              secret on GitHub — workflows can't read it from here directly.
            </span>
          </label>

          <button className="btn-primary btn-block" type="submit" disabled={saving}>
            {saving ? 'Saving…' : integration ? 'Update connection' : 'Connect repository'}
          </button>
        </form>
      ) : (
        <p className="empty-sub">Only Admins and Project Managers can manage this connection.</p>
      )}

      {integration && (
        <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--line-soft)' }}>
          <div className="field" style={{ marginBottom: 10 }}>
            <span>Webhook secret</span>
            <div style={{ display: 'flex', gap: 8 }}>
              <input value={integration.webhookSecret} readOnly style={{ flex: 1, fontFamily: 'monospace', fontSize: 12 }} />
              <button type="button" className="btn-ghost-sm" onClick={copySecret} title="Copy">
                <Copy size={14} />
              </button>
              {canEdit && (
                <button type="button" className="btn-ghost-sm" onClick={handleRegenerateSecret} title="Regenerate">
                  <RefreshCw size={14} />
                </button>
              )}
            </div>
          </div>
          <p className="page-subtitle-inline">
            Add this as the <code>WEBHOOK_SECRET</code> Actions secret in your repo, alongside your existing{' '}
            <code>CONTROLLER_URL</code>, so NeuroForge can verify builds are really coming from your workflow.
          </p>
        </div>
      )}

      <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--line-soft)' }}>
        <div className="field" style={{ marginBottom: 10 }}>
          <span><Webhook size={13} style={{ verticalAlign: -2 }} /> Inbound deploy webhook (paste into your hosting provider)</span>
          <div className="field field-inline" style={{ marginBottom: 8 }}>
            <select className="inline-select" value={webhookEnv} onChange={(e) => setWebhookEnv(e.target.value)}>
              {ENVIRONMENTS.map((e) => <option key={e} value={e}>{e}</option>)}
            </select>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <input value={inboundWebhookUrl} readOnly style={{ flex: 1, fontFamily: 'monospace', fontSize: 12 }} />
            <button type="button" className="btn-ghost-sm" onClick={copyInboundUrl} title="Copy">
              <Copy size={14} />
            </button>
          </div>
        </div>
        <p className="page-subtitle-inline">
          Paste this into your host's "deploy notification" / "webhook" setting for the environment you want to
          track (pick the environment above first — each environment needs its own URL). When your host reports
          a deploy finished, NeuroForge records it and, on success, automatically cuts a release for it. This
          endpoint currently accepts the generic <code>{'{ status, commitHash, deployId }'}</code> payload shape —
          if your host sends something else, its payload just needs a small adapter (see{' '}
          <code>GenericDeployAdapter</code> / <code>RenderDeployAdapter</code> in the backend) to normalize the field names.
        </p>
      </div>
    </div>
  )
}
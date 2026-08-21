import { useEffect, useState } from 'react'
import { useNavigate, useOutletContext } from 'react-router-dom'
import { projectsApi } from '../../api/projects'
import { teamsApi } from '../../api/teams'
import { useAuth } from '../../context/AuthContext'
import { canManage } from '../../utils/roles'
import { Alert } from '../../components/ui'
import ProjectSettingsForm from '../../components/settings/ProjectSettingsForm'
import DangerZone from '../../components/settings/DangerZone'
import ProjectOverviewCard from '../../components/settings/ProjectOverviewCard'
import GithubIntegrationForm from '../../components/settings/GithubIntegrationForm'
import './Settings.css'
import MonitoringTargetsPanel from '../../components/settings/MonitoringTargetsPanel'

export default function Settings() {
  const { project, reloadProject } = useOutletContext()
  const navigate = useNavigate()
  const { roles } = useAuth()
  const canEdit = canManage(roles?.[0])

  const [teams, setTeams] = useState([])
  const [status, setStatus] = useState('')
  const [teamId, setTeamId] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [saving, setSaving] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [confirmingDelete, setConfirmingDelete] = useState(false)

  useEffect(() => {
    teamsApi.getAll().then(setTeams).catch(() => {})
  }, [])

  useEffect(() => {
    if (project) {
      setStatus(project.status || 'ACTIVE')
      setTeamId(project.teamId ? String(project.teamId) : '')
    }
  }, [project])

  if (!project) return null

  const isDirty = status !== (project.status || 'ACTIVE') || String(teamId) !== String(project.teamId || '')
  const currentTeamName = teams.find((t) => String(t.id) === String(teamId))?.name || 'Unassigned'

  const handleSave = async (e) => {
    e.preventDefault()
    if (!isDirty) return
    setError('')
    setSuccess('')
    setSaving(true)
    try {
      if (status !== project.status) {
        await projectsApi.updateStatus(project.id, status)
      }
      if (teamId && String(teamId) !== String(project.teamId || '')) {
        await projectsApi.assignTeam(project.id, Number(teamId))
      }
      await reloadProject()
      setSuccess('Changes saved.')
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async () => {
    setDeleting(true)
    try {
      await projectsApi.remove(project.id)
      navigate('/projects')
    } catch (err) {
      setError(err.message)
      setDeleting(false)
      setConfirmingDelete(false)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Project Settings</h1>
          <p className="page-subtitle">Reassign team, change status, or delete {project.name}.</p>
        </div>
      </div>

      <Alert onClose={() => setError('')}>{error}</Alert>
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>

      <GithubIntegrationForm projectId={project.id} canEdit={canEdit} />
       <MonitoringTargetsPanel projectId={project.id} canEdit={canEdit} />

      <div className="ps-layout">
        <div className="panel">
          <ProjectSettingsForm
            project={project}
            teams={teams}
            status={status}
            setStatus={setStatus}
            teamId={teamId}
            setTeamId={setTeamId}
            saving={saving}
            isDirty={isDirty}
            onSave={handleSave}
          />

          <DangerZone
            project={project}
            confirmingDelete={confirmingDelete}
            setConfirmingDelete={setConfirmingDelete}
            deleting={deleting}
            onDelete={handleDelete}
          />
        </div>

        <ProjectOverviewCard project={project} status={status} currentTeamName={currentTeamName} />
      </div>
    </div>
  )
}
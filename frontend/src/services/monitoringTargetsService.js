// frontend/src/api/monitoringTargets.js
import client from '../api/client'

export const monitoringTargetsService = {
  getAll: (projectId) => client.get(`/projects/${projectId}/monitoring-targets`).then((r) => r.data),
  create: (projectId, payload) => client.post(`/projects/${projectId}/monitoring-targets`, payload).then((r) => r.data),
  update: (projectId, id, payload) => client.put(`/projects/${projectId}/monitoring-targets/${id}`, payload).then((r) => r.data),
  remove: (projectId, id) => client.delete(`/projects/${projectId}/monitoring-targets/${id}`)
}
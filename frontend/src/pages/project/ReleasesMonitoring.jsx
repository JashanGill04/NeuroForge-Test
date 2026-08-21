import { useState, useEffect, useCallback } from "react";
import { useOutletContext, useSearchParams } from "react-router-dom";
import { Rocket, Plus } from "lucide-react";
import { Alert, EmptyState } from "../../components/ui";
import { useAuth } from "../../context/AuthContext";
import { canManage } from "../../utils/roles";
import { releaseService } from "../../services/ReleaseService";
import { alertService } from "../../services/alertService";
import { kpiHistoryService } from "../../services/kpiHistoryService";
import { ENVIRONMENTS } from "../../components/releases/releaseConstants";


import ReleaseKpiStats from "../../components/releases/ReleaseKpiStats";
import EnvironmentHealthPanel from "../../components/releases/EnvironmentHealthPanel";
import ReleasesTable from "../../components/releases/ReleasesTable";
import CreateReleaseModal from "../../components/releases/CreateReleaseModal";
import ReleaseDetailModal from "../../components/releases/ReleaseDetailModal";
import AlertsPanel from "../../components/releases/AlertsPanel";
import KpiTrendChart from "../../components/releases/KpiTrendChart";
import AlertRulesPanel from "../../components/releases/AlertRulesPanel";
import LiveMonitoringPanel from "../../components/releases/LiveMonitoringPanel";
import {monitoringTargetsService} from "../../services/monitoringTargetsService";
export default function ReleasesMonitoring() {
  const { project } = useOutletContext();
  const { roles } = useAuth();
  const canEdit = canManage(roles?.[0]);

  const [searchParams, setSearchParams] = useSearchParams();

  const [kpis, setKpis] = useState(null);
  const [releases, setReleases] = useState([]);
  const [envHealth, setEnvHealth] = useState({});
  const [history, setHistory] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [envLoading, setEnvLoading] = useState(true);
  const [loadingAlerts, setLoadingAlerts] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [rules, setRules] = useState([]);
  const [loadingRules, setLoadingRules] = useState(true);

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [prefillDeploymentId, setPrefillDeploymentId] = useState(null);

  const [selectedReleaseId, setSelectedReleaseId] = useState(null);
  const [releaseDetails, setReleaseDetails] = useState(null);
  const [loadingDetails, setLoadingDetails] = useState(false);
  const [rollingBack, setRollingBack] = useState(false);
const [monitoringTargets, setMonitoringTargets] = useState([]);
const [loadingTargets, setLoadingTargets] = useState(true);


 const fetchReleasesAndKpis = useCallback(async (silent = false) => {
  if (!project?.id) return
  try {
    if (!silent) setLoading(true)
    const [releaseList, kpiData] = await Promise.all([
      releaseService.getHistory(project.id),
      releaseService.getKpis(project.id)
    ])
    setReleases(releaseList || [])
    setKpis(kpiData || null)
  } catch (err) {
    setError(err.message || 'Failed to load releases.')
  } finally {
    if (!silent) setLoading(false)
  }
}, [project?.id])

const fetchMonitoringTargets = useCallback(async (silent = false) => {
  if (!project?.id) return;
  if (!silent) setLoadingTargets(true);
  try {
    const data = await  monitoringTargetsService.getAll(project.id);
    setMonitoringTargets(data || []);
  } catch (err) {
    console.error("Failed to load monitoring targets", err);
  } finally {
    if (!silent) setLoadingTargets(false);
  }
}, [project?.id]);




const fetchEnvHealth = useCallback(async (silent = false) => {
  if (!project?.id) return
  if (!silent) setEnvLoading(true)
      if (!silent) setEnvLoading(true);
      const results = await Promise.allSettled(
        ENVIRONMENTS.map((env) =>
          releaseService.getActiveRelease(project.id, env),
        ),
      );
      const next = {};
      ENVIRONMENTS.forEach((env, i) => {
        if (results[i].status === "fulfilled" && results[i].value) {
          next[env] = results[i].value;
        }
      });
      setEnvHealth(next);
      if (!silent) setEnvLoading(false);
    },
    [project?.id],
  );

 const fetchAlerts = useCallback(async (silent = false) => {
  if (!project?.id) return
  if (!silent) setLoadingAlerts(true)
  try {
    const data = await alertService.getAlerts(project.id)
    setAlerts(data || [])
  } catch (err) {
    console.error('Failed to load alerts', err)
  } finally {
    if (!silent) setLoadingAlerts(false)
  }
}, [project?.id])

const fetchRules = useCallback(async (silent = false) => {
  if (!project?.id) return
  if (!silent) setLoadingRules(true)
  try {
    const data = await alertService.getRules(project.id)
    setRules(data || [])
  } catch (err) {
    console.error('Failed to load alert rules', err)
  } finally {
    if (!silent) setLoadingRules(false)
  }
}, [project?.id])

const fetchHistory = useCallback(
    async (silent = false) => {
      if (!project?.id) return;
      try {
        const data = await kpiHistoryService.getHistory(project.id, 24);
        setHistory(data || []);
      } catch (err) {
        console.error("Failed to load KPI history", err);
      }
    },
    [project?.id],
  );

  const refetchAll = useCallback(
    (silent = false) => {
      fetchReleasesAndKpis(silent);
      fetchEnvHealth(silent);
      fetchAlerts(silent);
      fetchRules(silent);
      fetchHistory(silent);
      fetchMonitoringTargets(silent);  

    },
    [
      fetchReleasesAndKpis,
      fetchEnvHealth,
      fetchAlerts,
      fetchRules,
      fetchHistory,
    ],
  );

  // Was `useEffect(..., [])` — ran exactly once, on the render where
  // `project` was still null (before the parent layout finished loading
  // it), and captured that stale closure for good. The interval kept
  // re-calling the same project-is-null version of refetchAll forever,
  // which is why this crashed on every 15s tick, not just once. Now it
  // waits for a real project.id and re-subscribes if the project changes
  // (e.g. navigating between projects without a full remount).
  useEffect(() => {
    if (!project?.id) return;
    refetchAll();
    const interval = setInterval(() => refetchAll(true), 15000);
    return () => clearInterval(interval);
  }, [project?.id, refetchAll]);

  useEffect(() => {
    const deploymentId = searchParams.get("deploymentId");
    if (deploymentId) {
      setPrefillDeploymentId(Number(deploymentId));
      setIsCreateModalOpen(true);
      const next = new URLSearchParams(searchParams);
      next.delete("deploymentId");
      setSearchParams(next, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams]);

  useEffect(() => {
    if (!selectedReleaseId) {
      setReleaseDetails(null);
      return;
    }
    let cancelled = false;
    setLoadingDetails(true);
    releaseService
      .getDetail(selectedReleaseId)
      .then((data) => {
        if (!cancelled) setReleaseDetails(data);
      })
      .catch((err) => {
        if (!cancelled)
          setError(err.message || "Failed to load release details.");
      })
      .finally(() => {
        if (!cancelled) setLoadingDetails(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedReleaseId]);

  const handleOpenCreateModal = () => {
    setPrefillDeploymentId(null);
    setIsCreateModalOpen(true);
  };

  const handleCloseCreateModal = () => {
    setIsCreateModalOpen(false);
    setPrefillDeploymentId(null);
  };

  const handleCreated = () => {
    setIsCreateModalOpen(false);
    setPrefillDeploymentId(null);
    setSuccess("Release cut successfully.");
    refetchAll();
  };

  const handleRollback = async (releaseId) => {
    setSuccess("");
    setRollingBack(true);
    try {
      await releaseService.rollbackRelease(releaseId);
      setSuccess(
        "Rollback initiated — the previous release is being restored.",
      );
      setSelectedReleaseId(null);
      refetchAll();
    } catch (err) {
      setError(err.message || "Rollback failed.");
    } finally {
      setRollingBack(false);
    }
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1>Releases &amp; Monitoring</h1>
          <p className="page-subtitle">
            Blue-green releases, environment health, and rollback for{" "}
            {project?.name || "this project"}.
          </p>
        </div>
        {canEdit && (
          <button className="btn-primary" onClick={handleOpenCreateModal}>
            <Plus size={16} /> Cut release
          </button>
        )}
      </div>

      {error && <Alert onClose={() => setError("")}>{error}</Alert>}
      {success && (
        <Alert type="success" onClose={() => setSuccess("")}>
          {success}
        </Alert>
      )}

      {!project?.id || loading || !kpis ? (
        <EmptyState title="Loading release data…" icon={Rocket} />
      ) : (
        <>
          <ReleaseKpiStats kpis={kpis} />
          <EnvironmentHealthPanel envHealth={envHealth} loading={envLoading} />
          <LiveMonitoringPanel targets={monitoringTargets} loading={loadingTargets} />
          <AlertRulesPanel
  rules={rules}
  loading={loadingRules}
  onRulesChanged={() => fetchRules(true)}
  canEdit={canEdit}
  projectId={project?.id}
/>
          <AlertsPanel alerts={alerts} loading={loadingAlerts} />
          <KpiTrendChart history={history} />
          <div className="panel">
            <div className="panel-header">
              <h2>Release history</h2>
            </div>
            <ReleasesTable
              releases={releases}
              onSelectRelease={setSelectedReleaseId}
            />
          </div>
        </>
      )}

      {isCreateModalOpen && (
        <CreateReleaseModal
          onClose={handleCloseCreateModal}
          onCreated={handleCreated}
          initialDeploymentId={prefillDeploymentId}
        />
      )}

      {selectedReleaseId && (
        <ReleaseDetailModal
          releaseId={selectedReleaseId}
          releaseDetails={releaseDetails}
          loading={loadingDetails}
          onClose={() => setSelectedReleaseId(null)}
          canEdit={canEdit}
          onRollback={handleRollback}
          rollingBack={rollingBack}
        />
      )}
    </div>
  );
}

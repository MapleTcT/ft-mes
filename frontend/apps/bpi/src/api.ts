import type {
  ApiEnvelope,
  Batch,
  BatchRelease,
  Candidate,
  CandidateConfirmation,
  DataQualityIncident,
  DataQualityIncidentDetail,
  DataQualityIncidentState,
  DataQualitySummary,
  DatasetDefinition,
  DatasetDefinitionCreateCommand,
  DatasetCatalogPublication,
  DatasetMaterialization,
  DatasetMlflowRegistration,
  DatasetRetentionArchive,
  DatasetSnapshot,
  DatasetSnapshotCommand,
  DatasetTrainingReadinessAssessment,
  Evidence,
  FeatureFlag,
  FeatureFlagOverrideCommand,
  FeatureFlagScopeType,
  ForceCloseCommand,
  ForceCloseTask,
  LineLiveEvidence,
  LineState,
  PointCatalogSnapshotCommand,
  PointCatalogView,
  PointCalibration,
  PointCalibrationSubmitCommand,
  ProblemDetail,
  RuleSimulation,
  RuleSimulationCommand,
  RuleDraftCommand,
  RuleVersion,
  ShadowRun,
  ShadowRunBatchReview,
  ShadowRunBatchReviewCommand,
  ShadowRunCreateCommand,
  ShadowRunReviewResult,
  ShadowRunState,
  StateEvent,
  TopologyVersion,
  TopologyDraftCommand,
  VersionComparison,
  WmsInboundReversalCommand,
  WmsInboundReversalTask,
} from './types';

const API_ROOT = '/bpi-api';
const TIMEOUT_MS = 12_000;

export class ApiProblem extends Error {
  constructor(public readonly problem: ProblemDetail) {
    super(problem.detail || problem.title);
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<ApiEnvelope<T>> {
  const controller = new AbortController();
  const timer = window.setTimeout(() => controller.abort(), TIMEOUT_MS);
  const ticket = localStorage.getItem('ticket');
  try {
    const response = await fetch(`${API_ROOT}${path}`, {
      ...init,
      credentials: 'same-origin',
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        ...(ticket ? { Authorization: `Bearer ${ticket}` } : {}),
        ...(init.body ? { 'Content-Type': 'application/json' } : {}),
        ...init.headers,
      },
    });
    const body = await response.json();
    if (!response.ok) throw new ApiProblem(body as ProblemDetail);
    return body as ApiEnvelope<T>;
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw new Error('请求超时，请按 traceId 查询命令结果后重试。');
    }
    throw error;
  } finally {
    window.clearTimeout(timer);
  }
}

export const bpiApi = {
  overview: (plantId: string, onlyAbnormal: boolean) =>
    request<LineState[]>(`/overview?plantId=${encodeURIComponent(plantId)}&onlyAbnormal=${onlyAbnormal}`),
  featureFlags: (plantId: string, lineId: string, scopeType: FeatureFlagScopeType) => {
    const parameters = new URLSearchParams({ plantId, lineId, scopeType });
    return request<FeatureFlag[]>(`/feature-flags?${parameters.toString()}`);
  },
  changeFeatureFlag: (flag: FeatureFlag, command: FeatureFlagOverrideCommand, key: string) =>
    request<FeatureFlag>(`/feature-flags/${encodeURIComponent(flag.flagKey)}`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(flag.overrideRevision) },
      body: JSON.stringify(command),
    }),
  line: (lineId: string, plantId: string) => {
    const parameters = new URLSearchParams({ plantId });
    return request<LineState>(
      `/lines/${encodeURIComponent(lineId)}/current-state?${parameters.toString()}`,
    );
  },
  lineEvidence: (lineId: string, plantId: string, windowMinutes = 15, limit = 120) => {
    const parameters = new URLSearchParams({
      plantId,
      windowMinutes: String(windowMinutes),
      limit: String(limit),
    });
    return request<LineLiveEvidence>(
      `/lines/${encodeURIComponent(lineId)}/live-evidence?${parameters.toString()}`,
    );
  },
  candidates: (plantId: string) =>
    request<Candidate[]>(`/candidates?plantId=${encodeURIComponent(plantId)}&state=PENDING&limit=100`),
  candidate: (id: string) => request<Candidate>(`/candidates/${encodeURIComponent(id)}`),
  confirmCandidate: (candidate: Candidate, reason: string, key: string) =>
    request<CandidateConfirmation>(`/candidates/${encodeURIComponent(candidate.id)}/confirm`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(candidate.revision) },
      body: JSON.stringify({ reason }),
    }),
  rejectCandidate: (candidate: Candidate, reason: string, key: string) =>
    request<Candidate>(`/candidates/${encodeURIComponent(candidate.id)}/reject`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(candidate.revision) },
      body: JSON.stringify({ reason }),
    }),
  batches: (plantId: string) =>
    request<Batch[]>(`/batches?plantId=${encodeURIComponent(plantId)}&limit=100`),
  batch: (id: string) => request<Batch>(`/batches/${encodeURIComponent(id)}`),
  batchRelease: (id: string) => request<BatchRelease>(`/batches/${encodeURIComponent(id)}/release`),
  wmsInboundReversalTask: (id: string) =>
    request<WmsInboundReversalTask | null>(`/batches/${encodeURIComponent(id)}/wms/reversal`),
  commandWmsInboundReversal: (
    batch: Batch,
    command: WmsInboundReversalCommand,
    key: string,
  ) => request<WmsInboundReversalTask>(`/batches/${encodeURIComponent(batch.id)}/wms/reversal`, {
    method: 'POST',
    headers: { 'Idempotency-Key': key, 'If-Match': String(batch.revision) },
    body: JSON.stringify(command),
  }),
  reconcileWmsInbound: (release: BatchRelease, reason: string, key: string) => {
    if (!release.wmsInbound) throw new Error('当前批次没有可核对的 WMS 入库命令。');
    return request<BatchRelease>(`/batches/${encodeURIComponent(release.batch.id)}/wms/reconcile`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(release.wmsInbound.revision) },
      body: JSON.stringify({ reason }),
    });
  },
  suspendBatch: (batch: Batch, reason: string, key: string) =>
    request<Batch>(`/batches/${encodeURIComponent(batch.id)}/suspend`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(batch.revision) },
      body: JSON.stringify({ reason }),
    }),
  resumeBatch: (batch: Batch, reason: string, key: string) =>
    request<Batch>(`/batches/${encodeURIComponent(batch.id)}/resume`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(batch.revision) },
      body: JSON.stringify({ reason }),
    }),
  forceCloseTask: (id: string) =>
    request<ForceCloseTask | null>(`/batches/${encodeURIComponent(id)}/force-close`),
  forceCloseBatch: (batch: Batch, command: ForceCloseCommand, key: string) =>
    request<ForceCloseTask>(`/batches/${encodeURIComponent(batch.id)}/force-close`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(batch.revision) },
      body: JSON.stringify(command),
    }),
  shadowRuns: (plantId: string, options?: { lineId?: string; state?: ShadowRunState | '' }) => {
    const parameters = new URLSearchParams({ plantId, limit: '100' });
    if (options?.lineId) parameters.set('lineId', options.lineId);
    if (options?.state) parameters.set('state', options.state);
    return request<ShadowRun[]>(`/shadow-runs?${parameters.toString()}`);
  },
  shadowRun: (id: string) => request<ShadowRun>(`/shadow-runs/${encodeURIComponent(id)}`),
  shadowRunReviews: (id: string, includeSuperseded = false) =>
    request<ShadowRunBatchReview[]>(`/shadow-runs/${encodeURIComponent(id)}/batch-reviews?includeSuperseded=${includeSuperseded}`),
  createShadowRun: (command: ShadowRunCreateCommand, key: string) =>
    request<ShadowRun>('/shadow-runs', {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': '0' },
      body: JSON.stringify(command),
    }),
  reviewShadowRunBatch: (run: ShadowRun, command: ShadowRunBatchReviewCommand, key: string) =>
    request<ShadowRunReviewResult>(`/shadow-runs/${encodeURIComponent(run.id)}/batch-reviews`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(run.revision) },
      body: JSON.stringify(command),
    }),
  startShadowRun: (run: ShadowRun, reason: string, key: string) =>
    request<ShadowRun>(`/shadow-runs/${encodeURIComponent(run.id)}/start`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(run.revision) },
      body: JSON.stringify({ reason }),
    }),
  completeShadowRun: (run: ShadowRun, reason: string, key: string) =>
    request<ShadowRun>(`/shadow-runs/${encodeURIComponent(run.id)}/complete`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(run.revision) },
      body: JSON.stringify({ reason }),
    }),
  approveShadowRun: (run: ShadowRun, reason: string, key: string) =>
    request<ShadowRun>(`/shadow-runs/${encodeURIComponent(run.id)}/approve`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(run.revision) },
      body: JSON.stringify({ reason }),
    }),
  rejectShadowRun: (run: ShadowRun, reason: string, key: string) =>
    request<ShadowRun>(`/shadow-runs/${encodeURIComponent(run.id)}/reject`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(run.revision) },
      body: JSON.stringify({ reason }),
    }),
  cancelShadowRun: (run: ShadowRun, reason: string, key: string) =>
    request<ShadowRun>(`/shadow-runs/${encodeURIComponent(run.id)}/cancel`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(run.revision) },
      body: JSON.stringify({ reason }),
    }),
  evidence: (id: string) => request<{ start: Evidence[]; end: Evidence[] }>(`/batches/${encodeURIComponent(id)}/evidence`),
  timeline: (id: string) => request<StateEvent[]>(`/batches/${encodeURIComponent(id)}/timeline`),
  dataQualityIncidents: (
    plantId: string,
    options?: { lineId?: string; state?: DataQualityIncidentState; search?: string; cursor?: string | null; limit?: number },
  ) => {
    const parameters = new URLSearchParams({ plantId });
    if (options?.lineId) parameters.set('lineId', options.lineId);
    if (options?.state) parameters.set('state', options.state);
    if (options?.search) parameters.set('search', options.search);
    if (options?.cursor) parameters.set('cursor', options.cursor);
    if (options?.limit !== undefined) parameters.set('limit', String(options.limit));
    return request<DataQualityIncident[]>(`/data-quality/incidents?${parameters.toString()}`);
  },
  dataQualitySummary: (plantId: string, lineId?: string) => {
    const parameters = new URLSearchParams({ plantId });
    if (lineId) parameters.set('lineId', lineId);
    return request<DataQualitySummary>(`/data-quality/summary?${parameters.toString()}`);
  },
  dataQualityIncident: (id: string) =>
    request<DataQualityIncidentDetail>(`/data-quality/incidents/${encodeURIComponent(id)}`),
  acknowledgeDataQuality: (incident: DataQualityIncident, assignee: string, reason: string, key: string) =>
    request<DataQualityIncident>(`/data-quality/incidents/${encodeURIComponent(incident.id)}/acknowledge`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(incident.revision) },
      body: JSON.stringify({ assignee, reason }),
    }),
  resolveDataQuality: (incident: DataQualityIncident, reason: string, key: string) =>
    request<DataQualityIncident>(`/data-quality/incidents/${encodeURIComponent(incident.id)}/resolve`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(incident.revision) },
      body: JSON.stringify({ reason }),
    }),
  datasets: (plantId: string) =>
    request<DatasetDefinition[]>(`/datasets?plantId=${encodeURIComponent(plantId)}&limit=100`),
  createDatasetDefinition: (command: DatasetDefinitionCreateCommand, key: string) =>
    request<DatasetDefinition>('/datasets', {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': '0' },
      body: JSON.stringify(command),
    }),
  createDatasetSnapshot: (definition: DatasetDefinition, command: DatasetSnapshotCommand, key: string) =>
    request<DatasetSnapshot>(`/datasets/${encodeURIComponent(definition.id)}/snapshots`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(definition.revision) },
      body: JSON.stringify(command),
    }),
  datasetSnapshot: (snapshotId: string) =>
    request<DatasetSnapshot>(`/dataset-snapshots/${encodeURIComponent(snapshotId)}`),
  requestDatasetMaterialization: (snapshot: DatasetSnapshot, reason: string, key: string) =>
    request<DatasetMaterialization>(`/dataset-snapshots/${encodeURIComponent(snapshot.id)}/materializations`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(snapshot.revision) },
      body: JSON.stringify({ artifactFormat: 'PARQUET', reason }),
    }),
  datasetMaterialization: (materializationId: string) =>
    request<DatasetMaterialization>(`/dataset-materializations/${encodeURIComponent(materializationId)}`),
  retryDatasetMaterialization: (materialization: DatasetMaterialization, reason: string, key: string) =>
    request<DatasetMaterialization>(`/dataset-materializations/${encodeURIComponent(materialization.id)}/retry`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(materialization.revision) },
      body: JSON.stringify({ reason }),
    }),
  datasetCatalogPublicationForMaterialization: (materializationId: string) =>
    request<DatasetCatalogPublication | null>(`/dataset-materializations/${encodeURIComponent(materializationId)}/catalog-publications`),
  requestDatasetCatalogPublication: (materialization: DatasetMaterialization, reason: string, key: string) =>
    request<DatasetCatalogPublication>(`/dataset-materializations/${encodeURIComponent(materialization.id)}/catalog-publications`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(materialization.revision) },
      body: JSON.stringify({ reason }),
    }),
  datasetCatalogPublication: (publicationId: string) =>
    request<DatasetCatalogPublication>(`/dataset-catalog-publications/${encodeURIComponent(publicationId)}`),
  retryDatasetCatalogPublication: (publication: DatasetCatalogPublication, reason: string, key: string) =>
    request<DatasetCatalogPublication>(`/dataset-catalog-publications/${encodeURIComponent(publication.id)}/retry`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(publication.revision) },
      body: JSON.stringify({ reason }),
    }),
  datasetRetentionArchiveForPublication: (publicationId: string) =>
    request<DatasetRetentionArchive | null>(`/dataset-catalog-publications/${encodeURIComponent(publicationId)}/retention-archives`),
  requestDatasetRetentionArchive: (publication: DatasetCatalogPublication, reason: string, key: string) =>
    request<DatasetRetentionArchive>(`/dataset-catalog-publications/${encodeURIComponent(publication.id)}/retention-archives`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(publication.revision) },
      body: JSON.stringify({ reason }),
    }),
  datasetRetentionArchive: (archiveId: string) =>
    request<DatasetRetentionArchive>(`/dataset-retention-archives/${encodeURIComponent(archiveId)}`),
  retryDatasetRetentionArchive: (archive: DatasetRetentionArchive, reason: string, key: string) =>
    request<DatasetRetentionArchive>(`/dataset-retention-archives/${encodeURIComponent(archive.id)}/retry`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(archive.revision) },
      body: JSON.stringify({ reason }),
    }),
  datasetMlflowRegistrationForArchive: (archiveId: string) =>
    request<DatasetMlflowRegistration | null>(`/dataset-retention-archives/${encodeURIComponent(archiveId)}/mlflow-registrations`),
  requestDatasetMlflowRegistration: (archive: DatasetRetentionArchive, reason: string, key: string) =>
    request<DatasetMlflowRegistration>(`/dataset-retention-archives/${encodeURIComponent(archive.id)}/mlflow-registrations`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(archive.revision) },
      body: JSON.stringify({ reason }),
    }),
  datasetMlflowRegistration: (registrationId: string) =>
    request<DatasetMlflowRegistration>(`/dataset-mlflow-registrations/${encodeURIComponent(registrationId)}`),
  retryDatasetMlflowRegistration: (registration: DatasetMlflowRegistration, reason: string, key: string) =>
    request<DatasetMlflowRegistration>(`/dataset-mlflow-registrations/${encodeURIComponent(registration.id)}/retry`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(registration.revision) },
      body: JSON.stringify({ reason }),
    }),
  datasetTrainingReadinessForRegistration: (registrationId: string) =>
    request<DatasetTrainingReadinessAssessment | null>(`/dataset-mlflow-registrations/${encodeURIComponent(registrationId)}/training-readiness-assessments`),
  assessDatasetTrainingReadiness: (registration: DatasetMlflowRegistration, reason: string, key: string) =>
    request<DatasetTrainingReadinessAssessment>(`/dataset-mlflow-registrations/${encodeURIComponent(registration.id)}/training-readiness-assessments`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(registration.revision) },
      body: JSON.stringify({
        objectiveCode: 'BATCH_START_BOUNDARY_REVIEW_RISK',
        reason,
      }),
    }),
  topologies: (plantId: string) =>
    request<TopologyVersion[]>(`/topologies?plantId=${encodeURIComponent(plantId)}`),
  currentPointCatalog: (
    plantId: string,
    lineId: string,
    options?: { cursor?: string | null; limit?: number; search?: string },
  ) => {
    const parameters = new URLSearchParams({ plantId, lineId });
    if (options?.cursor) parameters.set('cursor', options.cursor);
    if (options?.limit !== undefined) parameters.set('limit', String(options.limit));
    if (options?.search) parameters.set('search', options.search);
    return request<PointCatalogView | null>(`/point-catalog/current?${parameters.toString()}`);
  },
  importPointCatalog: (command: PointCatalogSnapshotCommand, key: string) =>
    request<PointCatalogView>('/point-catalog/snapshots', {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': '0' },
      body: JSON.stringify(command),
    }),
  listPointCalibrations: (plantId: string, lineId: string, cursor?: string | null, limit = 50) => {
    const parameters = new URLSearchParams({ plantId, lineId, limit: String(limit) });
    if (cursor) parameters.set('cursor', cursor);
    return request<PointCalibration[]>(`/point-calibrations?${parameters.toString()}`);
  },
  submitPointCalibration: (command: PointCalibrationSubmitCommand, key: string) =>
    request<PointCalibration>('/point-calibrations', {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': '0' },
      body: JSON.stringify(command),
    }),
  approvePointCalibration: (calibration: PointCalibration, reason: string, key: string) =>
    request<PointCalibration>(`/point-calibrations/${encodeURIComponent(calibration.id)}/approve`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(calibration.revision) },
      body: JSON.stringify({ reason }),
    }),
  rejectPointCalibration: (calibration: PointCalibration, reason: string, key: string) =>
    request<PointCalibration>(`/point-calibrations/${encodeURIComponent(calibration.id)}/reject`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(calibration.revision) },
      body: JSON.stringify({ reason }),
    }),
  revokePointCalibration: (calibration: PointCalibration, reason: string, key: string) =>
    request<PointCalibration>(`/point-calibrations/${encodeURIComponent(calibration.id)}/revoke`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(calibration.revision) },
      body: JSON.stringify({ reason }),
    }),
  topology: (id: string) => request<TopologyVersion>(`/topologies/${encodeURIComponent(id)}`),
  compareTopologies: (id: string, against: string) =>
    request<VersionComparison>(`/topologies/${encodeURIComponent(id)}/compare?against=${encodeURIComponent(against)}`),
  createTopologyDraft: (command: TopologyDraftCommand, key: string, revision = 0) =>
    request<TopologyVersion>('/topologies/drafts', {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(revision) },
      body: JSON.stringify(command),
    }),
  validateTopology: (topology: TopologyVersion, reason: string, key: string) =>
    request<TopologyVersion>(`/topologies/${encodeURIComponent(topology.id)}/validate`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(topology.revision) },
      body: JSON.stringify({ reason }),
    }),
  publishTopology: (topology: TopologyVersion, reason: string, key: string) =>
    request<TopologyVersion>(`/topologies/${encodeURIComponent(topology.id)}/publish`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(topology.revision) },
      body: JSON.stringify({ reason }),
    }),
  rules: (plantId: string) => request<RuleVersion[]>(`/rules?plantId=${encodeURIComponent(plantId)}`),
  rule: (id: string) => request<RuleVersion>(`/rules/${encodeURIComponent(id)}`),
  compareRules: (id: string, against: string) =>
    request<VersionComparison>(`/rules/${encodeURIComponent(id)}/compare?against=${encodeURIComponent(against)}`),
  createRuleDraft: (command: RuleDraftCommand, key: string, revision = 0) =>
    request<RuleVersion>('/rules/drafts', {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(revision) },
      body: JSON.stringify(command),
    }),
  simulation: (id: string) => request<RuleSimulation>(`/rule-simulations/${encodeURIComponent(id)}`),
  simulateRule: (rule: RuleVersion, command: RuleSimulationCommand, key: string) =>
    request<RuleSimulation>(`/rules/${encodeURIComponent(rule.id)}/simulate`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(rule.revision) },
      body: JSON.stringify(command),
    }),
  publishRule: (rule: RuleVersion, simulation: RuleSimulation, reason: string, key: string) =>
    request<RuleVersion>(`/rules/${encodeURIComponent(rule.id)}/publish`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(rule.revision) },
      body: JSON.stringify({ reason, simulationId: simulation.id, simulationChecksum: simulation.checksum }),
    }),
  submitRuleApproval: (rule: RuleVersion, simulation: RuleSimulation, reason: string, key: string) =>
    request<RuleVersion>(`/rules/${encodeURIComponent(rule.id)}/submit-approval`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(rule.revision) },
      body: JSON.stringify({ reason, simulationId: simulation.id, simulationChecksum: simulation.checksum }),
    }),
  rejectRuleApproval: (rule: RuleVersion, reason: string, key: string) =>
    request<RuleVersion>(`/rules/${encodeURIComponent(rule.id)}/reject-approval`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(rule.revision) },
      body: JSON.stringify({ reason }),
    }),
  retryRulePublication: (rule: RuleVersion, reason: string, key: string) =>
    request<RuleVersion>(`/rules/${encodeURIComponent(rule.id)}/publication/retry`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(rule.publicationRevision) },
      body: JSON.stringify({ reason }),
    }),
  retireRule: (rule: RuleVersion, reason: string, key: string) =>
    request<RuleVersion>(`/rules/${encodeURIComponent(rule.id)}/retire`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(rule.revision) },
      body: JSON.stringify({ reason }),
    }),
};

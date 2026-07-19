export type BpiStatus = 'RUNNING' | 'ACTIVE' | 'PENDING' | 'CONFIRMED' | 'PARTIAL' | 'GOOD' | string;

export interface ResponseMeta {
  traceId: string;
  generatedAt: string;
  snapshotAt: string;
  nextCursor?: string | null;
}

export interface ApiEnvelope<T> {
  data: T;
  meta: ResponseMeta;
}

export interface ProblemDetail {
  title: string;
  status: number;
  detail: string;
  traceId?: string;
  currentRevision?: number;
}

export type DataQualityIncidentState = 'OPEN' | 'ACKNOWLEDGED' | 'RESOLVED';

export interface DataQualityIncident {
  id: string;
  issueCode: string;
  severity: string;
  state: DataQualityIncidentState;
  revision: number;
  plantId: string;
  lineId: string;
  source: string;
  deviceId?: string | null;
  propertyId?: string | null;
  affectedLines: string[];
  affectedRules: string[];
  affectedBatches: string[];
  affectedBatchCount: number;
  eventCount: number;
  firstSeen: string;
  lastSeen: string;
  lastDetail: string;
  assignee?: string | null;
  acknowledgedBy?: string | null;
  acknowledgedAt?: string | null;
  acknowledgmentReason?: string | null;
  resolvedBy?: string | null;
  resolvedAt?: string | null;
  resolutionReason?: string | null;
}

export interface DataQualityEvent {
  eventId: string;
  sourceEventId: string;
  severity: string;
  detail: string;
  detectedAt: string;
  receivedAt: string;
  headers: Record<string, string>;
}

export interface DataQualityLifecycle {
  revision: number;
  action: string;
  fromState?: string | null;
  toState: string;
  actorId: string;
  assignee?: string | null;
  reason?: string | null;
  at: string;
}

export interface DataQualityIncidentDetail {
  incident: DataQualityIncident;
  events: DataQualityEvent[];
  lifecycle: DataQualityLifecycle[];
  recommendedActions: string[];
}

export interface DataQualitySummary {
  open: number;
  acknowledged: number;
  resolved: number;
  critical: number;
  affectedBatches: number;
  issueCounts: Record<string, number>;
}

export interface LineState {
  lineId: string;
  lineName?: string;
  status: BpiStatus;
  stageCode: string;
  orderId?: string | null;
  currentBatchId?: string | null;
  confidence: number | null;
  instantFlow: number | null;
  totalizedQuantity: number;
  dataHealth: BpiStatus;
  pendingCandidates: number;
  affectedRules?: number;
  lastEventTime: string;
}

export interface Evidence {
  eventId: string;
  signal: string;
  classification: 'REQUIRED' | 'QUORUM' | 'OPTIONAL';
  satisfied: boolean;
  value: unknown;
  unit?: string | null;
  quality: string;
  eventTime: string;
  source: string;
}

export interface Candidate {
  id: string;
  candidateKey: string;
  boundaryType: 'START' | 'END';
  lineId: string;
  orderId?: string | null;
  batchId?: string | null;
  boundaryTime: string;
  state: BpiStatus;
  revision: number;
  confidence: number;
  ruleVersion: string;
  topologyVersion: string;
  missingSignals: string[];
  evidence: Evidence[];
}

export interface Batch {
  id: string;
  batchNo: string;
  lineId: string;
  stageCode: string;
  orderId?: string | null;
  materialCode?: string | null;
  state: BpiStatus;
  revision: number;
  shadow: boolean;
  startTime: string;
  endTime?: string | null;
  quantity: number;
  quantityUnit: string;
  dryMatter?: number | null;
  qualityGate: string;
  wmsStatus: string;
  ruleVersion: string;
  topologyVersion: string;
}

export interface CandidateConfirmation {
  candidate: Candidate;
  batch: Batch;
}

export interface StateEvent {
  revision: number;
  action: string;
  at?: string;
  eventTime?: string;
  actor?: string;
  actorId?: string;
  reason?: string;
}

export interface TopologyVersion {
  id: string;
  code: string;
  version: string;
  state: BpiStatus;
  revision: number;
  plantId: string;
  lineId: string;
  checksum: string;
  validationStatus: 'NOT_VALIDATED' | 'PASSED' | 'FAILED';
  validationErrors: TopologyValidationIssue[];
  validationWarnings: TopologyValidationIssue[];
  validatedBy?: string | null;
  validatedAt?: string | null;
  validatedPointCatalogSnapshotId?: string | null;
  validatedPointCatalogChecksum?: string | null;
  publishedBy?: string | null;
  publishedAt?: string | null;
  definition: {
    localityGroup?: string;
    stages?: Array<{ code: string; name: string }>;
    nodes?: Array<{ code: string; type: string; name?: string }>;
    edges?: Array<{ from: string; to: string }>;
    bindings?: Array<{
      signal: string;
      productId: string;
      deviceId: string;
      propertyId: string;
      expectedUnit?: string;
      unit?: string;
      calibrationVersion: string;
      allocationKey?: string;
    }>;
    requiredSignals?: string[];
  };
}

export interface PointCatalogSnapshot {
  id: string;
  source: string;
  sourceInstance: string;
  sourceRevision: string;
  plantId: string;
  lineId: string;
  checksum: string;
  observedAt: string;
  pointCount: number;
  readyPointCount: number;
  importedBy: string;
  importedAt: string;
}

export interface PointCatalogPoint {
  id: string;
  snapshotId: string;
  plantId: string;
  lineId: string;
  localityGroup?: string | null;
  productId: string;
  deviceId: string;
  propertyId: string;
  sourcePropertyId?: string | null;
  pointName?: string | null;
  unit?: string | null;
  dataType?: string | null;
  deviceState: 'ACTIVE' | 'INACTIVE' | 'UNKNOWN';
  registered: boolean;
  propertyPresent: boolean;
  calibrationVersion?: string | null;
  sourceCalibrationStatus: 'VERIFIED' | 'UNVERIFIED' | 'MISSING';
  calibrationStatus: 'VERIFIED' | 'UNVERIFIED' | 'MISSING';
  calibrationEvidenceId?: string | null;
  calibrationValidUntil?: string | null;
  sourceSequenceEnabled: boolean;
  ready: boolean;
  readinessIssues: string[];
}

export interface PointCalibration {
  id: string;
  plantId: string;
  lineId: string;
  productId: string;
  deviceId: string;
  propertyId: string;
  calibrationVersion: string;
  certificateReference: string;
  certificateChecksum: string;
  validFrom: string;
  validUntil: string;
  state: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED';
  revision: number;
  submittedBy: string;
  submittedAt: string;
  submitReason: string;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionReason?: string | null;
  revokedBy?: string | null;
  revokedAt?: string | null;
  revokeReason?: string | null;
  effective: boolean;
  effectivenessStatus: 'PENDING' | 'REJECTED' | 'REVOKED' | 'NOT_YET_EFFECTIVE' | 'EXPIRED' | 'EFFECTIVE';
}

export interface PointCalibrationSubmitCommand {
  plantId: string;
  lineId: string;
  productId: string;
  deviceId: string;
  propertyId: string;
  calibrationVersion: string;
  certificateReference: string;
  certificateChecksum: string;
  validFrom: string;
  validUntil: string;
  reason: string;
}

export interface PointCatalogView {
  snapshot: PointCatalogSnapshot;
  points: PointCatalogPoint[];
}

export interface PointCatalogPointCommand {
  localityGroup?: string | null;
  productId: string;
  deviceId: string;
  propertyId: string;
  sourcePropertyId?: string | null;
  pointName?: string | null;
  unit?: string | null;
  dataType?: string | null;
  deviceState: 'ACTIVE' | 'INACTIVE' | 'UNKNOWN';
  registered: boolean;
  propertyPresent: boolean;
  calibrationVersion?: string | null;
  calibrationStatus: 'VERIFIED' | 'UNVERIFIED' | 'MISSING';
  sourceSequenceEnabled: boolean;
}

export interface PointCatalogSnapshotCommand {
  source: string;
  sourceInstance: string;
  sourceRevision: string;
  plantId: string;
  lineId: string;
  observedAt: string;
  points: PointCatalogPointCommand[];
  reason: string;
}

export interface TopologyValidationIssue {
  code: string;
  path: string;
  severity: 'ERROR' | 'WARNING';
  message: string;
}

export interface TopologyDraftCommand {
  code: string;
  version: string;
  plantId: string;
  lineId: string;
  baseVersionId?: string | null;
  definition: TopologyVersion['definition'];
  reason: string;
}

export interface RuleVersion {
  id: string;
  code: string;
  version: string;
  state: BpiStatus;
  revision: number;
  plantId: string;
  lineId: string;
  topologyVersion: string;
  checksum: string;
  ast: Record<string, unknown>;
  latestSimulationId?: string | null;
  approvalId?: string | null;
  approvalStatus: 'NOT_REQUESTED' | 'PENDING' | 'APPROVED' | 'REJECTED';
  approvalRevision: number;
  approvalSubmittedBy?: string | null;
  approvalSubmittedAt?: string | null;
  approvalDecidedBy?: string | null;
  approvalDecidedAt?: string | null;
  lifecycleAction: 'NOT_PUBLISHED' | 'ACTIVATE' | 'RETIRE';
  lifecycleSequence: number;
  lifecycleActive: boolean;
  publicationStatus: 'NOT_PUBLISHED' | 'NOT_TRACKED' | 'PENDING' | 'DISPATCHING' | 'PUBLISHED' | 'FAILED';
  publicationRevision: number;
  publicationAttemptCount: number;
  publicationTotalAttemptCount: number;
  publicationManualRetryCount: number;
  publicationPublishedAt?: string | null;
  publicationLastRequeuedAt?: string | null;
  publicationLastError?: string | null;
  applicationStatus: 'NOT_PUBLISHED' | 'NOT_TRACKED' | 'WAITING' | 'REJECTED' | 'APPLIED';
  applicationDeploymentId?: string | null;
  applicationObservedAt?: string | null;
  applicationReceivedAt?: string | null;
  applicationErrorCode?: string | null;
  applicationErrorDetail?: string | null;
  runtimeReadinessStatus: 'NOT_PUBLISHED' | 'NOT_TRACKED' | 'WAITING' | 'READY' | 'DEGRADED' | 'INACTIVE';
  runtimeReadinessDeploymentId?: string | null;
  runtimeReadinessObservedAt?: string | null;
  runtimeReadinessReceivedAt?: string | null;
  runtimeReadinessReasonCode?: string | null;
  runtimeReadinessDetail?: string | null;
  runtimePointCatalogEventId?: string | null;
  runtimePointCatalogSourceRevision?: string | null;
}

export interface VersionReference {
  id: string;
  code: string;
  version: string;
  state: string;
  checksum: string;
}

export interface VersionChange {
  path: string;
  changeType: 'ADDED' | 'REMOVED' | 'CHANGED';
  beforeValue?: unknown;
  afterValue?: unknown;
}

export interface VersionComparison {
  objectType: 'TOPOLOGY_VERSION' | 'RULE_VERSION';
  base: VersionReference;
  target: VersionReference;
  identical: boolean;
  changeCount: number;
  truncated: boolean;
  changes: VersionChange[];
}

export interface RuleDraftCommand {
  code: string;
  version: string;
  lineId: string;
  topologyVersion: string;
  baseVersionId?: string | null;
  ast: Record<string, unknown>;
  reason: string;
}

export interface RuleSimulationCommand {
  lineId: string;
  from: string;
  to: string;
  topologyVersion: string;
  calibrationVersion: string;
  goldenSetId: string;
}

export interface RuleSimulation {
  id: string;
  ruleId: string;
  state: 'PASSED' | 'FAILED' | 'QUEUED' | 'RUNNING';
  checksum: string;
  metrics: {
    matched: number;
    missed: number;
    falsePositive: number;
    meanBoundaryErrorSeconds: number;
  };
  inputManifest: RuleSimulationCommand & {
    observationCount?: number;
    goldenBoundaryCount?: number;
  };
  emittedBoundaries: string[];
  failureReason?: string | null;
}

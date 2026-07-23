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

export type FeatureFlagScopeType = 'TENANT' | 'PLANT' | 'LINE';

export interface FeatureFlag {
  flagKey: string;
  displayName: string;
  description: string;
  riskLevel: 'MEDIUM' | 'HIGH' | 'CRITICAL' | string;
  effectiveEnabled: boolean;
  effectiveScopeType: string;
  effectiveScopeKey: string;
  effectiveRevision?: number | null;
  selectedScopeType: FeatureFlagScopeType;
  selectedScopeKey: string;
  overrideExists: boolean;
  overrideActive: boolean;
  overrideEnabled?: boolean | null;
  overrideRevision: number;
  updatedBy?: string | null;
  updatedAt?: string | null;
  lastReason?: string | null;
  enforcementStatus: 'ENFORCED' | 'PHASE_LOCKED' | 'CODE_INVARIANT' | 'PENDING_SHELL_INTEGRATION' | string;
  editable: boolean;
  blockedReason?: string | null;
}

export interface FeatureFlagOverrideCommand {
  scopeType: FeatureFlagScopeType;
  plantId: string;
  lineId: string;
  mode: 'SET' | 'INHERIT';
  enabled?: boolean;
  reason: string;
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

export interface ForceCloseTask {
  taskId: string;
  batchId: string;
  state: 'PENDING_APPROVAL' | 'COMPLETED';
  revision: number;
  batchRevision: number;
  sourceState: 'ACTIVE' | 'SUSPENDED';
  boundaryTime: string;
  requestedBy: string;
  requestedAt: string;
  requestReason: string;
  requestComment?: string | null;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionReason?: string | null;
  decisionComment?: string | null;
}

export interface ForceCloseCommand {
  reason: string;
  boundaryTime: string;
  approvalMode: 'REQUEST' | 'APPROVE';
  comment?: string;
}

export type QualityInspectionDisposition = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface QualityInspection {
  inspectionCode: string;
  inspectionRecordId: string;
  required: boolean;
  disposition: QualityInspectionDisposition;
  finalResult: boolean;
  observedAt: string;
}

export interface QualityGate {
  id: string;
  externalGateId: string;
  externalRevision: number;
  sourceEventId: string;
  state: 'WAITING' | 'ACCEPTED' | 'REJECTED';
  releaseQuantity?: number | null;
  quantityUnit?: string | null;
  materialCode?: string | null;
  observedAt: string;
  inspections: QualityInspection[];
}

export interface WmsInbound {
  id: string;
  commandEventId: string;
  idempotencyKey: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  receiptEventId?: string | null;
  documentId?: string | null;
  errorCode?: string | null;
  detail?: string | null;
  observedAt?: string | null;
  revision: number;
  outboxStatus: 'PENDING' | 'DISPATCHING' | 'PUBLISHED' | 'FAILED';
  deliveryAttemptCount: number;
  reconciliationCount: number;
  commandPublishedAt?: string | null;
  lastReconciledAt?: string | null;
  lastReconciledBy?: string | null;
  reconcileAfter: string;
  reconciliationAllowed: boolean;
  reconciliationBlockedReason?: 'BATCH_NOT_RELEASED' | 'WMS_RECEIPT_TERMINAL'
    | 'ADMIN_ROLE_REQUIRED' | 'PHASE2_DISABLED' | 'COMMANDS_DISABLED'
    | 'WMS_LINK_DISABLED' | 'OUTBOX_BUSY' | 'SAFETY_DELAY_ACTIVE' | null;
}

export type WmsInboundReversalState = 'PENDING_APPROVAL' | 'PENDING_WMS' | 'COMPLETED' | 'FAILED';

export interface WmsInboundReversalTask {
  taskId: string;
  batchId: string;
  state: WmsInboundReversalState;
  revision: number;
  batchRevision: number;
  originalInboundLinkId: string;
  originalCommandEventId: string;
  originalIdempotencyKey: string;
  originalDocumentId: string;
  requestedBy: string;
  requestedAt: string;
  requestReason: string;
  requestComment?: string | null;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionReason?: string | null;
  decisionComment?: string | null;
  reversalCommandEventId?: string | null;
  reversalIdempotencyKey?: string | null;
  reversalReceiptEventId?: string | null;
  reversalDocumentId?: string | null;
  errorCode?: string | null;
  detail?: string | null;
  observedAt?: string | null;
  outboxStatus?: 'NOT_CREATED' | 'PENDING' | 'DISPATCHING' | 'PUBLISHED' | 'FAILED' | null;
  deliveryAttemptCount: number;
}

export interface WmsInboundReversalCommand {
  reason: string;
  comment?: string;
  approvalMode: 'REQUEST' | 'APPROVE';
}

export interface BatchRelease {
  batch: Batch;
  qualityGate: QualityGate | null;
  wmsInbound: WmsInbound | null;
  wmsInboundReversal: WmsInboundReversalTask | null;
}

export type ShadowRunState = 'DRAFT' | 'RUNNING' | 'EVALUATING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface ShadowRunReadiness {
  rulePublished: boolean;
  ruleActive: boolean;
  publicationConfirmed: boolean;
  applicationApplied: boolean;
  runtimeReady: boolean;
  topologyPublished: boolean;
  topologySnapshotPinned: boolean;
  pointCatalogCurrent: boolean;
  pointCatalogReady: boolean;
  ready: boolean;
}

export interface ShadowRunMetrics {
  observedDurationSeconds: number;
  reviewedBatchCount: number;
  acceptedBoundaryCount: number;
  totalBoundaryCount: number;
  boundaryAgreement: number;
  quantitySampleCount: number;
  automaticQuantityTotal: number;
  referenceQuantityTotal: number;
  quantityUnit?: string | null;
  cumulativeQuantityDeviationPercent: number;
  meanQuantityDeviationPercent: number;
  maximumQuantityDeviationPercent: number;
  unresolvedCriticalIncidentCount: number;
  durationGatePassed: boolean;
  reviewCountGatePassed: boolean;
  boundaryAgreementGatePassed: boolean;
  quantityGatePassed: boolean;
  dataQualityGatePassed: boolean;
}

export interface ShadowRun {
  id: string;
  runCode: string;
  name: string;
  tenantId: string;
  plantId: string;
  lineId: string;
  state: ShadowRunState;
  revision: number;
  ruleVersionId: string;
  ruleVersion: string;
  topologyVersionId: string;
  topologyVersion: string;
  pointCatalogSnapshotId: string;
  pointCatalogChecksum: string;
  minimumDurationDays: number;
  minimumReviewedBatches: number;
  boundaryToleranceSeconds: number;
  minimumBoundaryAgreement: number;
  quantityTolerancePercent: number;
  createdBy: string;
  createdAt: string;
  startedBy?: string | null;
  startedAt?: string | null;
  completedBy?: string | null;
  completedAt?: string | null;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionReason?: string | null;
  cancelledBy?: string | null;
  cancelledAt?: string | null;
  cancellationReason?: string | null;
  readiness: ShadowRunReadiness;
  metrics: ShadowRunMetrics;
  blockers: string[];
  readyForApproval: boolean;
}

export interface ShadowRunCreateCommand {
  runCode: string;
  name: string;
  plantId: string;
  lineId: string;
  ruleVersionId: string;
  minimumDurationDays: number;
  minimumReviewedBatches: number;
  boundaryToleranceSeconds: number;
  minimumBoundaryAgreement: number;
  quantityTolerancePercent: number;
  reason: string;
}

export interface ShadowRunBatchReviewCommand {
  batchId: string;
  manualStartTime: string;
  manualEndTime: string;
  referenceQuantity: number;
  quantityUnit: string;
  reason: string;
}

export interface ShadowRunBatchReview {
  id: string;
  shadowRunId: string;
  batchId: string;
  batchNo: string;
  reviewSequence: number;
  state: 'ACTIVE' | 'SUPERSEDED';
  automaticStartTime: string;
  automaticEndTime: string;
  manualStartTime: string;
  manualEndTime: string;
  startDeviationSeconds: number;
  endDeviationSeconds: number;
  startBoundaryAccepted: boolean;
  endBoundaryAccepted: boolean;
  automaticQuantity: number;
  referenceQuantity: number;
  quantityUnit: string;
  quantityDeviationPercent: number;
  quantityWithinTolerance: boolean;
  reviewedBy: string;
  reviewReason: string;
  reviewedAt: string;
  supersededAt?: string | null;
}

export interface ShadowRunReviewResult {
  run: ShadowRun;
  review: ShadowRunBatchReview;
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
  sourceSequenceRequired: boolean;
  sourceSequenceOrigin?: 'DEVICE' | 'GATEWAY' | null;
  sourceSequenceBindingFingerprint?: string | null;
  sourceSequenceQualified: boolean;
  sourceSequenceEvidenceStatus?: 'DISABLED' | 'MISSING' | 'PENDING' | 'QUALIFIED' | 'EXPIRED' | null;
  sourceSequenceEpoch?: number | null;
  sourceSequenceFirst?: number | null;
  sourceSequenceLast?: number | null;
  sourceSequenceObservationCount?: number | null;
  sourceSequenceFirstObservedAt?: string | null;
  sourceSequenceLastObservedAt?: string | null;
  sourceSequenceValidUntil?: string | null;
  sourceSequenceEvidenceEventId?: string | null;
  sourceSequenceEvidenceRevision?: number | null;
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
  sourceSequenceRequired: boolean;
  sourceSequenceOrigin?: 'DEVICE' | 'GATEWAY' | null;
  sourceSequenceBindingFingerprint?: string | null;
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

export type DatasetSnapshotState = 'QUEUED' | 'BUILDING' | 'MANIFEST_READY' | 'FAILED';
export type DatasetMaterializationState = 'QUEUED' | 'WRITING' | 'READY' | 'FAILED';
export type DatasetMaterializationDisplayState = 'NOT_STARTED' | DatasetMaterializationState;
export type DatasetCatalogPublicationState = 'QUEUED' | 'COMMITTING' | 'VERIFYING' | 'READY' | 'FAILED';
export type DatasetRetentionArchiveState = 'QUEUED' | 'ARCHIVING' | 'VERIFYING' | 'LOCKED' | 'FAILED';
export type DatasetMlflowRegistrationState = 'QUEUED' | 'REGISTERING' | 'REGISTERED' | 'FAILED';
export type DatasetTrainingReadinessState = 'ELIGIBLE' | 'BLOCKED';
export type ProcessSignalValueType = 'NUMERIC' | 'BOOLEAN';
export type ProcessSignalMetric = 'MEAN' | 'MIN' | 'MAX' | 'LAST' | 'DELTA' | 'SLOPE' | 'TRUE_RATIO';
export type ProcessSignalQualityCode = 'GOOD' | 'SUBSTITUTED';

export interface ProcessSignalWindowDefinitionCommand {
  featureRef: string;
  signal: string;
  valueType: ProcessSignalValueType;
  metric: ProcessSignalMetric;
  startOffsetSeconds: number;
  endOffsetSeconds: number;
  minimumSamples: number;
  maximumGapSeconds: number;
  expectedUnit: string;
  requireCalibration: boolean;
  acceptedQualityCodes: ProcessSignalQualityCode[];
}

export interface ProcessSignalWindowDefinition extends ProcessSignalWindowDefinitionCommand {
  checksum: string;
}

export interface DatasetProcessSignalWindowEvidence {
  featureRef: string;
  signal: string;
  metric: ProcessSignalMetric;
  valueType: ProcessSignalValueType;
  windowStart: string;
  windowEnd: string;
  predictionTime: string;
  physicalPoint: {
    productId?: string | null;
    deviceId?: string | null;
    propertyId?: string | null;
  };
  expectedUnit: string;
  minimumSamples: number;
  maximumGapSeconds: number;
  sourcePointCount: number;
  acceptedSampleCount: number;
  rejectedQualityCount: number;
  lateAvailabilityCount: number;
  unitMismatchCount: number;
  valueTypeMismatchCount: number;
  calibrationMismatchCount: number;
  maximumObservedGapSeconds?: number | null;
  numericValue?: number | null;
  state: 'READY' | 'BLOCKED';
  blockerCodes: string[];
  sourceFingerprint: string;
  factChecksum: string;
}

export interface DatasetSnapshotSummary {
  id: string;
  snapshotVersion: number;
  state: DatasetSnapshotState;
  revision: number;
  freezeAt: string;
  manifestChecksum?: string | null;
  includedCount?: number | null;
  excludedCount?: number | null;
  materializationState: DatasetMaterializationDisplayState;
  createdAt: string;
  completedAt?: string | null;
  failureCode?: string | null;
  failureDetail?: string | null;
}

export interface DatasetDefinition {
  id: string;
  datasetCode: string;
  version: string;
  name: string;
  tenantId: string;
  plantId: string;
  lineIds: string[];
  state: 'ACTIVE';
  revision: number;
  predictionTimePolicy: 'AUTOMATIC_BATCH_START';
  featureCutoffPolicy: 'AT_OR_BEFORE_PREDICTION_TIME';
  featureRefs: string[];
  processSignalWindows: ProcessSignalWindowDefinition[];
  labelRefs: string[];
  maxLabelDelayHours: number;
  minimumConfidence: number;
  splitPolicy: 'PRODUCTION_TIME';
  checksum: string;
  createdBy: string;
  createReason: string;
  createdAt: string;
  latestSnapshot?: DatasetSnapshotSummary | null;
}

export interface DatasetDefinitionCreateCommand {
  datasetCode: string;
  version: string;
  name: string;
  plantId: string;
  lineIds: string[];
  predictionTimePolicy: 'AUTOMATIC_BATCH_START';
  featureCutoffPolicy: 'AT_OR_BEFORE_PREDICTION_TIME';
  featureRefs: string[];
  processSignalWindows: ProcessSignalWindowDefinitionCommand[];
  labelRefs: string[];
  maxLabelDelayHours: number;
  minimumConfidence: number;
  splitPolicy: 'PRODUCTION_TIME';
  reason: string;
}

export interface DatasetSnapshotCommand {
  freezeAt: string;
  lineIds: string[];
  predictionTimePolicy: 'AUTOMATIC_BATCH_START';
  ruleVersionIds: string[];
  excludeLowConfidence: boolean;
  reason: string;
}

export interface DatasetMaterialization {
  id: string;
  snapshotId: string;
  datasetId: string;
  datasetCode: string;
  datasetVersion: string;
  tenantId: string;
  plantId: string;
  lineIds: string[];
  artifactFormat: 'PARQUET';
  artifactSchemaVersion: string;
  materializerVersion: string;
  state: DatasetMaterializationState;
  revision: number;
  manifestChecksum: string;
  requestedBy: string;
  requestReason: string;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  attemptCount: number;
  artifactUri?: string | null;
  objectBucket?: string | null;
  objectKey?: string | null;
  contentSha256?: string | null;
  byteSize?: number | null;
  rowCount?: number | null;
  schema?: Record<string, unknown> | null;
  artifactMetadata?: Record<string, unknown> | null;
  failureCode?: string | null;
  failureDetail?: string | null;
}

export interface DatasetCatalogPublication {
  id: string;
  materializationId: string;
  snapshotId: string;
  datasetId: string;
  datasetCode: string;
  datasetVersion: string;
  tenantId: string;
  plantId: string;
  lineIds: string[];
  catalogName: string;
  catalogNamespace: string;
  tableName: string;
  tableIdentifier: string;
  publisherVersion: string;
  state: DatasetCatalogPublicationState;
  revision: number;
  manifestChecksum: string;
  sourceContentSha256: string;
  sourceObjectVersionId: string;
  sourceByteSize: number;
  sourceRowCount: number;
  sourceSchema: Record<string, unknown>;
  requestedBy: string;
  requestReason: string;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  attemptCount: number;
  icebergSnapshotId?: string | null;
  icebergMetadataLocation?: string | null;
  icebergSchemaId?: number | null;
  icebergPartitionSpecId?: number | null;
  verifiedRowCount?: number | null;
  semanticChecksum?: string | null;
  catalogMetadata?: Record<string, unknown> | null;
  failureCode?: string | null;
  failureDetail?: string | null;
}

export interface DatasetRetentionArchive {
  id: string;
  catalogPublicationId: string;
  materializationId: string;
  snapshotId: string;
  datasetId: string;
  datasetCode: string;
  datasetVersion: string;
  tenantId: string;
  plantId: string;
  lineIds: string[];
  archiverVersion: string;
  archiveProfile: 'bpi-dataset-recovery-v1';
  state: DatasetRetentionArchiveState;
  revision: number;
  manifestChecksum: string;
  sourceContentSha256: string;
  sourceObjectVersionId: string;
  sourceByteSize: number;
  sourceRowCount: number;
  sourceSchema: Record<string, unknown>;
  tableIdentifier: string;
  icebergSnapshotId: string;
  icebergMetadataLocation: string;
  icebergSchemaId: number;
  icebergPartitionSpecId: number;
  catalogVerifiedRowCount: number;
  catalogSemanticChecksum: string;
  requestedBy: string;
  requestReason: string;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  attemptCount: number;
  retentionMode?: 'GOVERNANCE' | 'COMPLIANCE' | null;
  retainUntil?: string | null;
  legalHoldEnabled?: boolean | null;
  archiveBucket?: string | null;
  archivePrefix?: string | null;
  sourceArchiveObjectKey?: string | null;
  sourceArchiveVersionId?: string | null;
  archiveManifestObjectKey?: string | null;
  archiveManifestVersionId?: string | null;
  archiveManifestSha256?: string | null;
  archiveObjectCount?: number | null;
  archiveTotalBytes?: number | null;
  verifiedRowCount?: number | null;
  verifiedSemanticChecksum?: string | null;
  archiveMetadata?: Record<string, unknown> | null;
  failureCode?: string | null;
  failureDetail?: string | null;
}

export interface DatasetMlflowRegistration {
  id: string;
  retentionArchiveId: string;
  catalogPublicationId: string;
  materializationId: string;
  snapshotId: string;
  datasetId: string;
  datasetCode: string;
  datasetVersion: string;
  tenantId: string;
  plantId: string;
  lineIds: string[];
  registrarVersion: string;
  trackingProfile: 'bpi-mlflow-dataset-v1';
  state: DatasetMlflowRegistrationState;
  revision: number;
  manifestChecksum: string;
  sourceContentSha256: string;
  sourceObjectVersionId: string;
  sourceByteSize: number;
  sourceRowCount: number;
  sourceSchema: Record<string, unknown>;
  tableIdentifier: string;
  icebergSnapshotId: string;
  catalogSemanticChecksum: string;
  archiveBucket: string;
  sourceArchiveObjectKey: string;
  sourceArchiveVersionId: string;
  archiveManifestObjectKey: string;
  archiveManifestVersionId: string;
  archiveManifestSha256: string;
  experimentName: string;
  datasetName: string;
  datasetDigest: string;
  requestedBy: string;
  requestReason: string;
  createdAt: string;
  startedAt?: string | null;
  completedAt?: string | null;
  attemptCount: number;
  mlflowExperimentId?: string | null;
  mlflowRunId?: string | null;
  mlflowArtifactUri?: string | null;
  mlflowDatasetSource?: string | null;
  registrationMetadata?: Record<string, unknown> | null;
  failureCode?: string | null;
  failureDetail?: string | null;
}

export interface DatasetTrainingReadinessGate {
  code: string;
  passed: boolean;
  expected: unknown;
  observed: unknown;
  detail: string;
}

export interface DatasetTrainingReadinessAssessment {
  id: string;
  mlflowRegistrationId: string;
  sourceSnapshotId: string;
  datasetId: string;
  datasetCode: string;
  datasetVersion: string;
  tenantId: string;
  plantId: string;
  lineIds: string[];
  objectiveCode: 'BATCH_START_BOUNDARY_REVIEW_RISK';
  policyVersion: 'bpi-training-readiness/batch-start-boundary-v2';
  assessmentSequence: number;
  state: DatasetTrainingReadinessState;
  revision: 1;
  sourceRegistrationRevision: number;
  manifestChecksum: string;
  datasetDigest: string;
  requiredThresholds: Record<string, unknown>;
  observedMetrics: Record<string, unknown>;
  gateResults: DatasetTrainingReadinessGate[];
  blockerCodes: string[];
  phaseBoundary: {
    assessmentOnly: true;
    trainingStarted: false;
    modelCreated: false;
    modelRegistered: false;
    onlineInferenceEnabled: false;
    productionActivationAllowed: false;
  };
  assessmentChecksum: string;
  assessedBy: string;
  assessmentReason: string;
  assessedAt: string;
}

export interface DatasetManifestSample {
  reviewId: string;
  shadowRunId: string;
  batchId: string;
  batchNo: string;
  lineId: string;
  included: boolean;
  exclusionReasons: string[];
  predictionTime: string;
  featureCutoff: string;
  labelAvailableAt: string;
  confidence: number;
  splitKey: string;
  featurePayload: Record<string, unknown>;
  labelPayload: Record<string, unknown>;
  sourcePayload: Record<string, unknown> & {
    processSignalWindows?: DatasetProcessSignalWindowEvidence[];
  };
  features?: Record<string, unknown>;
  labels?: Record<string, unknown>;
  source?: Record<string, unknown>;
  featureCutoffTime?: string;
}

export interface DatasetManifest {
  schemaVersion: 'bpi.dataset-manifest.v1';
  definition: Record<string, unknown> & {
    featureRefs?: string[];
    processSignalWindows?: ProcessSignalWindowDefinition[];
    labelRefs?: string[];
  };
  selection: Record<string, unknown>;
  phaseBoundary: {
    deliveryState: 'MANIFEST_ONLY';
    materializationState: 'NOT_STARTED';
    artifactUri: null;
    icebergReady: false;
    mlflowRegistered: false;
    modelTrained: false;
  };
  counts: {
    total: number;
    included: number;
    excluded: number;
    exclusionSummary: Record<string, number>;
  };
  samples: DatasetManifestSample[];
}

export interface DatasetSnapshot extends DatasetSnapshotSummary {
  datasetId: string;
  datasetCode: string;
  datasetVersion: string;
  datasetName: string;
  tenantId: string;
  plantId: string;
  lineIds: string[];
  predictionTimePolicy: 'AUTOMATIC_BATCH_START';
  ruleVersionIds: string[];
  excludeLowConfidence: boolean;
  definitionChecksum: string;
  manifestSchemaVersion?: string | null;
  manifest?: DatasetManifest | null;
  exclusionSummary?: Record<string, number> | null;
  artifactUri?: string | null;
  requestedBy: string;
  requestReason: string;
  startedAt?: string | null;
  attemptCount: number;
  latestMaterialization?: DatasetMaterialization | null;
}

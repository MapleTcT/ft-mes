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
  definition: {
    stages?: Array<{ code: string; name: string }>;
    nodes?: Array<{ code: string; type: string; name: string }>;
    bindings?: Array<{ signal: string; propertyId: string; unit: string; calibrationVersion: string }>;
  };
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
  publicationStatus: 'NOT_PUBLISHED' | 'NOT_TRACKED' | 'PENDING' | 'DISPATCHING' | 'PUBLISHED' | 'FAILED';
  publicationRevision: number;
  publicationAttemptCount: number;
  publicationTotalAttemptCount: number;
  publicationManualRetryCount: number;
  publicationPublishedAt?: string | null;
  publicationLastRequeuedAt?: string | null;
  publicationLastError?: string | null;
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

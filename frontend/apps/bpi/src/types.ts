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

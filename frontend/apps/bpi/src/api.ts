import type {
  ApiEnvelope,
  Batch,
  Candidate,
  CandidateConfirmation,
  Evidence,
  LineState,
  ProblemDetail,
  RuleSimulation,
  RuleSimulationCommand,
  RuleVersion,
  StateEvent,
  TopologyVersion,
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
  line: (lineId: string) => request<LineState>(`/lines/${encodeURIComponent(lineId)}/current-state`),
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
  evidence: (id: string) => request<{ start: Evidence[]; end: Evidence[] }>(`/batches/${encodeURIComponent(id)}/evidence`),
  timeline: (id: string) => request<StateEvent[]>(`/batches/${encodeURIComponent(id)}/timeline`),
  topologies: (plantId: string) =>
    request<TopologyVersion[]>(`/topologies?plantId=${encodeURIComponent(plantId)}`),
  topology: (id: string) => request<TopologyVersion>(`/topologies/${encodeURIComponent(id)}`),
  rules: (plantId: string) => request<RuleVersion[]>(`/rules?plantId=${encodeURIComponent(plantId)}`),
  rule: (id: string) => request<RuleVersion>(`/rules/${encodeURIComponent(id)}`),
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
  retryRulePublication: (rule: RuleVersion, reason: string, key: string) =>
    request<RuleVersion>(`/rules/${encodeURIComponent(rule.id)}/publication/retry`, {
      method: 'POST',
      headers: { 'Idempotency-Key': key, 'If-Match': String(rule.publicationRevision) },
      body: JSON.stringify({ reason }),
    }),
};

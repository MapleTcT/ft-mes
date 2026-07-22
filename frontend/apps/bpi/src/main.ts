import {
  Activity,
  Archive,
  Boxes,
  CheckCircle2,
  ChevronRight,
  ChevronsDown,
  CircleAlert,
  Clock3,
  Database,
  Factory,
  Filter,
  FlaskConical,
  Gauge,
  ListChecks,
  LockKeyhole,
  Network,
  Play,
  Plus,
  Power,
  RefreshCw,
  RotateCcw,
  Search,
  Settings2,
  ShieldCheck,
  Upload,
  X,
  createIcons,
} from 'lucide';
import { ApiProblem, bpiApi } from './api';
import type {
  Batch,
  BatchRelease,
  Candidate,
  DataQualityIncident,
  DataQualityIncidentDetail,
  DataQualityIncidentState,
  DataQualitySummary,
  DatasetCatalogPublication,
  DatasetDefinition,
  DatasetDefinitionCreateCommand,
  DatasetMaterialization,
  DatasetSnapshot,
  DatasetSnapshotCommand,
  Evidence,
  FeatureFlag,
  FeatureFlagOverrideCommand,
  FeatureFlagScopeType,
  ForceCloseTask,
  LineState,
  PointCatalogPoint,
  PointCatalogPointCommand,
  PointCatalogSnapshotCommand,
  PointCatalogView,
  PointCalibration,
  PointCalibrationSubmitCommand,
  ResponseMeta,
  RuleSimulation,
  RuleSimulationCommand,
  RuleDraftCommand,
  RuleVersion,
  ShadowRun,
  ShadowRunBatchReview,
  ShadowRunBatchReviewCommand,
  ShadowRunCreateCommand,
  ShadowRunState,
  StateEvent,
  TopologyVersion,
  TopologyDraftCommand,
  VersionComparison,
  WmsInbound,
} from './types';
import './styles.css';

type View = 'overview' | 'candidates' | 'batches' | 'shadowRuns' | 'dataQuality' | 'points' | 'rules' | 'datasets' | 'featureFlags';
const CALIBRATION_PAGE_SIZE = 50;
const POINT_CATALOG_PAGE_SIZE = 100;
const POINT_SEARCH_DEBOUNCE_MS = 250;
const DATA_QUALITY_PAGE_SIZE = 50;

const appRoot = document.querySelector<HTMLDivElement>('#app');
if (!appRoot) throw new Error('BPI app root is missing');
const app: HTMLDivElement = appRoot;

const state = {
  view: 'overview' as View,
  plantId: localStorage.getItem('bpi.plantId') || 'PLANT-01',
  pointLineId: localStorage.getItem('bpi.lineId') || 'LINE-S07-01',
  onlyAbnormal: localStorage.getItem('bpi.onlyAbnormal') === 'true',
  loading: false,
  meta: null as ResponseMeta | null,
  lines: [] as LineState[],
  candidates: [] as Candidate[],
  batches: [] as Batch[],
  shadowRuns: [] as ShadowRun[],
  shadowRunState: (localStorage.getItem('bpi.shadowRunState') || '') as ShadowRunState | '',
  shadowRunLineId: localStorage.getItem('bpi.shadowRunLineId') || '',
  shadowRunReviews: [] as ShadowRunBatchReview[],
  dataQualityIncidents: [] as DataQualityIncident[],
  dataQualitySummary: null as DataQualitySummary | null,
  dataQualityState: (localStorage.getItem('bpi.dataQualityState') || 'OPEN') as DataQualityIncidentState | '',
  dataQualityLineId: localStorage.getItem('bpi.dataQualityLineId') || '',
  dataQualitySearch: '',
  dataQualityNextCursor: null as string | null,
  dataQualitySnapshotAt: null as string | null,
  loadingMoreDataQuality: false,
  rules: [] as RuleVersion[],
  topologies: [] as TopologyVersion[],
  pointCatalog: null as PointCatalogView | null,
  calibrations: [] as PointCalibration[],
  featureFlags: [] as FeatureFlag[],
  datasets: [] as DatasetDefinition[],
  featureFlagLineId: localStorage.getItem('bpi.featureFlagLineId') || 'LINE-S07-01',
  featureFlagScopeType: (localStorage.getItem('bpi.featureFlagScopeType') || 'LINE') as FeatureFlagScopeType,
  pointSearch: '',
  pointCatalogNextCursor: null as string | null,
  pointCatalogSnapshotAt: null as string | null,
  loadingMorePointCatalog: false,
  calibrationNextCursor: null as string | null,
  calibrationSnapshotAt: null as string | null,
  loadingMoreCalibrations: false,
  selectedCandidate: null as Candidate | null,
  selectedBatch: null as Batch | null,
  selectedBatchRelease: null as BatchRelease | null,
  selectedForceCloseTask: null as ForceCloseTask | null,
  selectedShadowRun: null as ShadowRun | null,
  selectedRule: null as RuleVersion | null,
  selectedTopology: null as TopologyVersion | null,
  selectedSimulation: null as RuleSimulation | null,
  selectedCalibration: null as PointCalibration | null,
  selectedDataQualityIncident: null as DataQualityIncident | null,
  selectedDataQualityDetail: null as DataQualityIncidentDetail | null,
  selectedFeatureFlag: null as FeatureFlag | null,
  selectedDataset: null as DatasetDefinition | null,
  selectedDatasetSnapshot: null as DatasetSnapshot | null,
  selectedDatasetMaterialization: null as DatasetMaterialization | null,
  selectedDatasetCatalogPublication: null as DatasetCatalogPublication | null,
  datasetMaterializationCommand: null as 'request' | 'retry' | null,
  datasetCatalogPublicationCommand: null as 'request' | 'retry' | null,
  candidateCommand: null as 'confirm' | 'reject' | null,
  batchCommand: null as 'suspend' | 'resume' | 'reconcileWms' | 'forceCloseRequest' | 'forceCloseApprove' | 'requestWmsReversal' | 'approveWmsReversal' | null,
  shadowRunCommand: null as 'start' | 'complete' | 'approve' | 'reject' | 'cancel' | null,
  ruleCommand: null as 'submit' | 'approve' | 'reject' | 'retry' | 'retire' | null,
  topologyCommand: null as 'validate' | 'publish' | null,
  calibrationCommand: null as 'approve' | 'reject' | 'revoke' | null,
  dataQualityCommand: null as 'acknowledge' | 'resolve' | null,
  featureFlagCommand: null as { mode: 'SET' | 'INHERIT'; enabled?: boolean } | null,
  batchEvidence: { start: [], end: [] } as { start: Evidence[]; end: Evidence[] },
  batchReleaseLoading: false,
  batchReleaseError: null as Error | null,
  timeline: [] as StateEvent[],
  error: null as Error | null,
};

let pointCatalogRequestGeneration = 0;
let batchRequestGeneration = 0;
let datasetSnapshotRequestGeneration = 0;
let activeDrawerKey: string | null = null;
let pointSearchTimer: number | null = null;

function escapeHtml(value: unknown): string {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function formatTime(value?: string | null): string {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).format(date);
}

function versionComparisonHtml(comparison: VersionComparison | null, subject: '拓扑' | '规则'): string {
  if (!comparison) {
    return `<div class="section-title"><h3>版本差异</h3><span>无可比版本</span></div><p>同一${subject}代码和作用域出现第二个版本后，可在这里查看受控内容差异。</p>`;
  }
  const changes = comparison.changes.map((change) => `<li><code>${escapeHtml(change.path)}</code><span>${escapeHtml(change.changeType)}</span><small>${escapeHtml(JSON.stringify(change.beforeValue) ?? '-')} → ${escapeHtml(JSON.stringify(change.afterValue) ?? '-')}</small></li>`).join('');
  return `<div class="section-title"><h3>版本差异</h3><span>${comparison.changeCount} 项</span></div><p>对比 ${escapeHtml(comparison.base.code)}@${escapeHtml(comparison.base.version)} → ${escapeHtml(comparison.target.code)}@${escapeHtml(comparison.target.version)}</p>${comparison.identical ? '<div class="simulation-empty"><span>受控内容一致</span></div>' : `<ul class="version-diff">${changes}</ul>`}${comparison.truncated ? '<p>差异超过 500 项，页面仅展示前 500 项。</p>' : ''}`;
}

function number(value: number | null | undefined, digits = 1): string {
  return value === null || value === undefined ? '-' : value.toFixed(digits);
}

function formatBytes(value: number | null | undefined): string {
  if (value === null || value === undefined) return '-';
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KiB`;
  return `${(value / 1024 / 1024).toFixed(1)} MiB`;
}

function formatDuration(from: string, to: string): string {
  const durationMs = Math.max(0, Date.parse(to) - Date.parse(from));
  if (!Number.isFinite(durationMs)) return '-';
  const minutes = Math.floor(durationMs / 60_000);
  if (minutes < 60) return `${minutes} 分钟`;
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  if (hours < 24) return `${hours} 小时${remainder ? ` ${remainder} 分` : ''}`;
  const days = Math.floor(hours / 24);
  return `${days} 天 ${hours % 24} 小时`;
}

function dataQualityIssueLabel(code: string): string {
  const labels: Record<string, string> = {
    REQUIRED_SIGNAL_UNAVAILABLE: '必需信号不可用',
    POINT_CATALOG_BINDING_MISSING: '点位绑定缺失',
    RULE_BINDING_MISSING: '规则绑定缺失',
    CLOCK_DRIFT: '设备时钟漂移',
    UNKNOWN_UNIT: '未知计量单位',
    UNIT_MISSING: '计量单位缺失',
    UNIT_MISMATCH: '计量单位不匹配',
    SEQUENCE_GAP: '事件序列缺口',
    SHARED_METER_UNALLOCATED: '共享仪表未分摊',
    BUFFER_ALERT: '消费缓冲告警',
    BACKPRESSURE: '流处理反压',
  };
  return labels[code] || code;
}

function normalizedDataQualityLine(): string {
  return state.dataQualityLineId.trim();
}

function normalizedDataQualitySearch(): string {
  return state.dataQualitySearch.trim();
}

function dataQualityCategoryCount(patterns: string[]): number {
  const counts = state.dataQualitySummary?.issueCounts || {};
  return Object.entries(counts).reduce((sum, [code, count]) => (
    patterns.some((pattern) => code.toUpperCase().includes(pattern)) ? sum + count : sum
  ), 0);
}

function statusTone(status: string): string {
  if (['RUNNING', 'ACTIVE', 'READY', 'PASS', 'CONFIRMED', 'GOOD', 'RELEASED', 'PUBLISHED', 'APPLIED', 'EFFECTIVE', 'APPROVED', 'RESOLVED', 'QUALIFIED', 'COMPLETED', 'INBOUNDED', 'INBOUND_REVERSED', 'REVERSED', 'MANIFEST_READY', 'VERIFIED'].includes(status)) return 'ok';
  if (['PENDING', 'PENDING_APPROVAL', 'PENDING_WMS', 'INBOUND_REVERSING', 'REVERSAL_PENDING', 'EVALUATING', 'DISPATCHING', 'WAITING', 'PARTIAL', 'WAIT_QA', 'DEGRADED', 'NOT_YET_EFFECTIVE', 'ACKNOWLEDGED', 'WARNING', 'QUEUED', 'BUILDING', 'WRITING', 'COMMITTING', 'VERIFYING', 'SIMULATED'].includes(status)) return 'warn';
  if (['FAILED', 'REVERSAL_FAILED', 'FAIL', 'BAD', 'REJECTED', 'BLOCKED', 'SUSPENDED', 'REVOKED', 'EXPIRED', 'OPEN', 'CRITICAL', 'ERROR', 'MISSING', 'DISABLED'].includes(status)) return 'danger';
  return 'neutral';
}

function statusChip(status: string): string {
  return `<span class="status status--${statusTone(status)}">${escapeHtml(status)}</span>`;
}

function publicationChip(status: RuleVersion['publicationStatus']): string {
  const labels: Record<RuleVersion['publicationStatus'], string> = {
    NOT_PUBLISHED: '未发布',
    NOT_TRACKED: '未跟踪',
    PENDING: '待分发',
    DISPATCHING: '分发中',
    PUBLISHED: 'Kafka 已确认',
    FAILED: '分发失败',
  };
  return `<span class="status status--${statusTone(status)}">${labels[status]}</span>`;
}

function publicationExplanation(rule: RuleVersion): string {
  if (rule.publicationStatus === 'PUBLISHED') {
    return rule.lifecycleAction === 'RETIRE'
      ? '停用事件已获 Kafka broker 确认；规则是否真正退出评估器仍以 Flink APPLIED 与 INACTIVE 回执为准。'
      : '发布事件已获 Kafka broker 确认；是否进入运行态仍以 Flink 应用回执为准。';
  }
  if (rule.publicationStatus === 'PENDING') {
    return rule.lifecycleAction === 'RETIRE'
      ? '退役状态与 active=false 停用事件已同事务落库，等待 Kafka 分发。'
      : '发布事件已与规则版本同事务落库，等待 Kafka 分发。';
  }
  if (rule.publicationStatus === 'DISPATCHING') return '服务正在向 Kafka 分发规则事件。';
  if (rule.publicationStatus === 'FAILED') return '规则事件已达到重试上限；需排查 Kafka，并按运维流程重新入队。';
  if (rule.publicationStatus === 'NOT_TRACKED') return '该版本缺少 outbox 发布证据，不能视为在线生效。';
  return '规则版本尚未提交发布。';
}

function applicationChip(status: RuleVersion['applicationStatus']): string {
  const labels: Record<RuleVersion['applicationStatus'], string> = {
    NOT_PUBLISHED: '控制面未发布',
    NOT_TRACKED: '控制面未跟踪',
    WAITING: '控制面等待',
    REJECTED: '控制面 REJECTED',
    APPLIED: '控制面 APPLIED',
  };
  return `<span class="status status--${statusTone(status)}">${labels[status]}</span>`;
}

function applicationExplanation(rule: RuleVersion): string {
  if (rule.applicationStatus === 'APPLIED') {
    return rule.lifecycleAction === 'RETIRE'
      ? '控制面已接受停用指令；只有运行态继续返回 INACTIVE，才能确认该版本已退出新的边界计算。'
      : '控制面已接受该规则版本，APPLIED 回执经 checkpoint 提交后完成作用域与 checksum 校验并写入 PostgreSQL；这不代表流式评估器已 READY。';
  }
  if (rule.applicationStatus === 'REJECTED') {
    return 'Flink 已拒绝该规则版本；排除拒绝原因并收到 APPLIED 回执前，该版本不能视为在线生效。';
  }
  if (rule.applicationStatus === 'WAITING') {
    return rule.lifecycleAction === 'RETIRE'
      ? '尚未收到 Flink 对停用事件的应用回执；当前不能确认规则已经退出评估器。'
      : '尚未收到 Flink 应用回执；即使 Kafka 已确认，也不能将该规则标记为在线生效。';
  }
  if (rule.applicationStatus === 'NOT_TRACKED') {
    return '该已发布版本没有可核验的应用回执链路，运行态状态未知。';
  }
  return '规则尚未发布，不存在运行态应用回执。';
}

function runtimeReadinessChip(status: RuleVersion['runtimeReadinessStatus']): string {
  const labels: Record<RuleVersion['runtimeReadinessStatus'], string> = {
    NOT_PUBLISHED: '运行时未发布',
    NOT_TRACKED: '运行时未跟踪',
    WAITING: '运行时 WAITING',
    READY: '运行时 READY',
    DEGRADED: '运行时 DEGRADED',
    INACTIVE: '运行时 INACTIVE',
  };
  return `<span class="status status--${statusTone(status)}">${labels[status]}</span>`;
}

function runtimeReadinessExplanation(rule: RuleVersion): string {
  if (rule.runtimeReadinessStatus === 'READY') {
    return '流式评估器已激活该精确规则版本，并已记录运行部署和点位目录证据。';
  }
  if (rule.runtimeReadinessStatus === 'DEGRADED') {
    return '该精确规则版本未进入评估器；运行准入条件已降级，排除原因并收到更新的 READY 回执前不会参与边界计算。';
  }
  if (rule.runtimeReadinessStatus === 'INACTIVE') {
    return '该精确规则版本已从评估器停用，不会参与新的边界计算。';
  }
  if (rule.runtimeReadinessStatus === 'WAITING') {
    return '控制面可能已经 APPLIED，但尚未收到评估器对该精确规则版本的运行就绪回执。';
  }
  if (rule.runtimeReadinessStatus === 'NOT_TRACKED') {
    return '该已发布版本没有可核验的评估器就绪回执，不能推断为 READY。';
  }
  return '规则尚未发布，不存在评估器运行就绪证据。';
}

function icon(name: string, label: string): string {
  return `<i data-lucide="${name}" aria-hidden="true"></i><span>${label}</span>`;
}

function shell(): void {
  app.innerHTML = `
    <div class="console-shell">
      <aside class="side-nav" aria-label="智能批次导航">
        <div class="brand-mark"><span class="brand-code">BPI</span><span>智能批次</span></div>
        <nav>
          <button class="nav-item" data-view="overview" title="实时生产态势">${icon('activity', '实时生产态势')}</button>
          <button class="nav-item" data-view="candidates" title="候选批次">${icon('list-checks', '候选批次')}<b id="candidate-count">0</b></button>
          <button class="nav-item" data-view="batches" title="批次档案">${icon('archive', '批次档案')}</button>
          <button class="nav-item" data-view="shadowRuns" title="影子验收">${icon('flask-conical', '影子验收')}</button>
          <button class="nav-item" data-view="dataQuality" title="数据质量">${icon('circle-alert', '数据质量')}</button>
          <button class="nav-item" data-view="points" title="点位准入">${icon('database', '点位准入')}</button>
          <button class="nav-item" data-view="rules" title="规则与拓扑">${icon('network', '规则与拓扑')}</button>
          <button class="nav-item" data-view="datasets" title="数据集清单">${icon('boxes', '数据集清单')}</button>
          <button class="nav-item" data-view="featureFlags" title="运行开关">${icon('settings-2', '运行开关')}</button>
        </nav>
        <div class="mode-flag"><i data-lucide="shield-check"></i><div><strong>SHADOW</strong><span>外部写入关闭</span></div></div>
      </aside>
      <section class="workspace">
        <header class="topbar">
          <div class="context-title"><span id="view-kicker">生产运行</span><h1 id="view-title">实时生产态势</h1></div>
          <div class="topbar-actions">
            <label class="plant-select"><i data-lucide="factory"></i><span class="sr-only">工厂</span><select id="plant-id"><option value="PLANT-01">PLANT-01</option></select></label>
            <span id="snapshot" class="snapshot"><i data-lucide="clock-3"></i>尚未同步</span>
            <button id="refresh" class="icon-button" title="刷新" aria-label="刷新"><i data-lucide="refresh-cw"></i></button>
          </div>
        </header>
        <div id="banner" class="banner" role="alert" hidden></div>
        <main id="content" class="content" tabindex="-1"></main>
      </section>
      <aside id="detail-drawer" class="detail-drawer" aria-label="详情" aria-hidden="true"></aside>
      <dialog id="confirm-dialog" class="command-dialog">
        <form method="dialog" id="confirm-form">
          <header><div><span id="command-kicker">候选批次</span><h2 id="command-title">审核候选边界</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="command-summary" id="command-summary"></div>
          <label id="command-assignee-field" hidden><span>分派给</span><input id="command-assignee" maxlength="128" autocomplete="off" placeholder="输入责任人账号或岗位" /></label>
          <label id="command-boundary-field" hidden><span>强制结束边界时间</span><input id="command-boundary-time" type="datetime-local" step="1" /></label>
          <label><span id="command-reason-label">审核原因</span><textarea id="confirm-reason" minlength="3" maxlength="500" required placeholder="填写现场审核依据"></textarea></label>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="confirm-submit" value="default" class="button button--primary">提交</button></footer>
        </form>
      </dialog>
      <dialog id="simulation-dialog" class="command-dialog simulation-dialog">
        <form method="dialog" id="simulation-form">
          <header><div><span>规则验证</span><h2>运行历史回放</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="simulation-fields">
            <label><span>开始时间</span><input id="simulation-from" type="datetime-local" step="1" required /></label>
            <label><span>结束时间</span><input id="simulation-to" type="datetime-local" step="1" required /></label>
            <label><span>校准版本</span><input id="simulation-calibration" value="CAL-1" required /></label>
            <label><span>金标准集</span><input id="simulation-golden" value="GOLDEN-S07-2026Q2" required /></label>
          </div>
          <div class="simulation-guard"><i data-lucide="shield-check"></i><span>回放读取 PostgreSQL 校准测点并使用同源规则运行时；最多 100,000 个观测值。</span></div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="simulation-submit" value="default" class="button button--primary">开始回放</button></footer>
        </form>
      </dialog>
      <dialog id="topology-editor-dialog" class="command-dialog editor-dialog">
        <form method="dialog" id="topology-editor-form">
          <header><div><span>工艺建模</span><h2>新建拓扑版本</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="editor-fields">
            <label><span>起始版本</span><select id="topology-base"><option value="">全新拓扑</option></select></label>
            <label><span>拓扑编码</span><input id="topology-code" required maxlength="128" pattern="[A-Za-z0-9](?:[A-Za-z0-9._:]|-)*" /></label>
            <label><span>版本</span><input id="topology-version" required maxlength="64" pattern="[A-Za-z0-9](?:[A-Za-z0-9._]|-)*" /></label>
            <label><span>产线</span><input id="topology-line" required maxlength="128" /></label>
            <label class="editor-field--wide"><span>拓扑定义 JSON</span><textarea id="topology-definition" required spellcheck="false"></textarea></label>
            <label class="editor-field--wide"><span>创建原因</span><textarea id="topology-reason" required minlength="3" maxlength="500"></textarea></label>
          </div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="topology-editor-submit" value="default" class="button button--primary">创建草稿</button></footer>
        </form>
      </dialog>
      <dialog id="rule-editor-dialog" class="command-dialog editor-dialog">
        <form method="dialog" id="rule-editor-form">
          <header><div><span>边界治理</span><h2 id="rule-editor-title">新建规则版本</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="editor-fields">
            <label><span>起始版本</span><select id="rule-base"><option value="">全新规则</option></select></label>
            <label><span>规则编码</span><input id="rule-code" required maxlength="128" pattern="[A-Za-z0-9](?:[A-Za-z0-9._:]|-)*" /></label>
            <label><span>版本</span><input id="rule-version" required maxlength="64" pattern="[A-Za-z0-9](?:[A-Za-z0-9._]|-)*" /></label>
            <label><span>产线</span><input id="rule-line" required maxlength="128" /></label>
            <label class="editor-field--wide"><span>已发布拓扑</span><select id="rule-topology" required></select></label>
            <label class="editor-field--wide"><span>规则 AST JSON</span><textarea id="rule-ast" required spellcheck="false"></textarea></label>
            <label class="editor-field--wide"><span>创建原因</span><textarea id="rule-reason" required minlength="3" maxlength="500"></textarea></label>
          </div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="rule-editor-submit" value="default" class="button button--primary">创建草稿</button></footer>
        </form>
      </dialog>
      <dialog id="point-catalog-dialog" class="command-dialog editor-dialog">
        <form method="dialog" id="point-catalog-form">
          <header><div><span>数据准入</span><h2>导入点位目录快照</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="editor-fields">
            <label><span>来源</span><input id="point-source" value="JETLINKS" required maxlength="64" /></label>
            <label><span>来源实例</span><input id="point-source-instance" required maxlength="128" /></label>
            <label><span>来源修订</span><input id="point-source-revision" required maxlength="128" /></label>
            <label><span>产线</span><input id="point-import-line" required maxlength="128" /></label>
            <label class="editor-field--wide"><span>观测时间</span><input id="point-observed-at" type="datetime-local" step="1" required /></label>
            <label class="editor-field--wide"><span>点位 JSON</span><textarea id="point-import-json" required spellcheck="false">[]</textarea></label>
            <label class="editor-field--wide"><span>导入原因</span><textarea id="point-import-reason" required minlength="3" maxlength="500"></textarea></label>
          </div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="point-catalog-submit" value="default" class="button button--primary">导入快照</button></footer>
        </form>
      </dialog>
      <dialog id="point-calibration-dialog" class="command-dialog editor-dialog">
        <form method="dialog" id="point-calibration-form">
          <header><div><span>校准治理</span><h2>提交点位校准证据</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="editor-fields">
            <label><span>产品</span><input id="calibration-product" required maxlength="128" /></label>
            <label><span>设备</span><input id="calibration-device" required maxlength="128" /></label>
            <label><span>属性</span><input id="calibration-property" required maxlength="128" /></label>
            <label><span>校准版本</span><input id="calibration-version" required maxlength="128" /></label>
            <label class="editor-field--wide"><span>证书引用</span><input id="calibration-certificate" required maxlength="512" placeholder="受控文档编号、MinIO URI 或证书 URI" /></label>
            <label class="editor-field--wide"><span>证书 SHA-256</span><input id="calibration-checksum" required minlength="64" maxlength="64" pattern="[a-f0-9]{64}" class="mono-input" /></label>
            <label><span>有效开始</span><input id="calibration-valid-from" type="datetime-local" step="1" required /></label>
            <label><span>有效截止</span><input id="calibration-valid-until" type="datetime-local" step="1" required /></label>
            <label class="editor-field--wide"><span>提交原因</span><textarea id="calibration-reason" required minlength="3" maxlength="500"></textarea></label>
          </div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="point-calibration-submit" value="default" class="button button--primary">提交复核</button></footer>
        </form>
      </dialog>
      <dialog id="shadow-run-dialog" class="command-dialog editor-dialog">
        <form method="dialog" id="shadow-run-form">
          <header><div><span>运行验收</span><h2>新建影子运行</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="editor-fields">
            <label><span>验收编码</span><input id="shadow-run-code" required maxlength="128" pattern="[A-Za-z0-9](?:[A-Za-z0-9._:]|-)*" /></label>
            <label><span>验收名称</span><input id="shadow-run-name" required maxlength="256" /></label>
            <label><span>产线</span><input id="shadow-run-line" required maxlength="128" /></label>
            <label><span>已发布规则版本</span><select id="shadow-run-rule" required></select></label>
            <label><span>最短运行天数</span><input id="shadow-run-duration" type="number" min="7" max="14" value="7" required /></label>
            <label><span>最少复核批次</span><input id="shadow-run-batches" type="number" min="10" max="10000" value="10" required /></label>
            <label><span>边界容差（秒）</span><input id="shadow-run-boundary-tolerance" type="number" min="0" max="3600" value="60" required /></label>
            <label><span>最低边界一致率</span><input id="shadow-run-boundary-agreement" type="number" min="0.95" max="1" step="0.001" value="0.95" required /></label>
            <label><span>累计数量偏差容差（%）</span><input id="shadow-run-quantity-tolerance" type="number" min="0.000001" max="100" step="0.000001" value="2" required /></label>
            <label class="editor-field--wide"><span>创建依据</span><textarea id="shadow-run-reason" required minlength="3" maxlength="500"></textarea></label>
          </div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="shadow-run-submit" value="default" class="button button--primary">创建验收任务</button></footer>
        </form>
      </dialog>
      <dialog id="shadow-review-dialog" class="command-dialog editor-dialog">
        <form method="dialog" id="shadow-review-form">
          <header><div><span>逐批比对</span><h2>复核影子批次</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="editor-fields">
            <label class="editor-field--wide"><span>已关闭影子批次</span><select id="shadow-review-batch" required></select></label>
            <label><span>人工开始时间</span><input id="shadow-review-start" type="datetime-local" step="1" required /></label>
            <label><span>人工结束时间</span><input id="shadow-review-end" type="datetime-local" step="1" required /></label>
            <label><span>参考数量</span><input id="shadow-review-quantity" type="number" min="0.000001" step="0.000001" required /></label>
            <label><span>数量单位</span><input id="shadow-review-unit" required maxlength="32" /></label>
            <label class="editor-field--wide"><span>复核依据</span><textarea id="shadow-review-reason" required minlength="3" maxlength="500"></textarea></label>
          </div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="shadow-review-submit" value="default" class="button button--primary">提交批次复核</button></footer>
        </form>
      </dialog>
      <dialog id="dataset-definition-dialog" class="command-dialog editor-dialog dataset-dialog">
        <form method="dialog" id="dataset-definition-form">
          <header><div><span>训练数据治理</span><h2>新建数据集定义</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="editor-fields">
            <label><span>数据集编码</span><input id="dataset-code" required maxlength="128" pattern="[A-Za-z0-9](?:[A-Za-z0-9._:]|-)*" /></label>
            <label><span>版本</span><input id="dataset-version" required maxlength="64" pattern="[A-Za-z0-9](?:[A-Za-z0-9._]|-)*" /></label>
            <label class="editor-field--wide"><span>名称</span><input id="dataset-name" required maxlength="256" /></label>
            <label class="editor-field--wide"><span>产线（逗号分隔）</span><input id="dataset-lines" required maxlength="2048" /></label>
            <label><span>预测时点</span><input id="dataset-prediction-policy" value="AUTOMATIC_BATCH_START" readonly /></label>
            <label><span>特征截止</span><input id="dataset-cutoff-policy" value="AT_OR_BEFORE_PREDICTION_TIME" readonly /></label>
            <label><span>最大标签延迟（小时）</span><input id="dataset-label-delay" type="number" min="1" max="2160" value="48" required /></label>
            <label><span>最低置信度</span><input id="dataset-confidence" type="number" min="0" max="1" step="0.01" value="0.8" required /></label>
            <fieldset class="dataset-ref-fieldset editor-field--wide"><legend>特征字段</legend><div>
              <label><input type="checkbox" data-dataset-feature value="batch.order_id" checked />生产指令</label>
              <label><input type="checkbox" data-dataset-feature value="batch.material_code" checked />物料编码</label>
              <label><input type="checkbox" data-dataset-feature value="batch.stage_code" checked />工段编码</label>
              <label><input type="checkbox" data-dataset-feature value="batch.quantity_unit" checked />数量单位</label>
              <label><input type="checkbox" data-dataset-feature value="rule.version_id" checked />规则版本</label>
              <label><input type="checkbox" data-dataset-feature value="topology.version_id" checked />拓扑版本</label>
              <label><input type="checkbox" data-dataset-feature value="point_catalog.snapshot_id" checked />点位目录快照</label>
            </div></fieldset>
            <fieldset class="dataset-ref-fieldset editor-field--wide"><legend>标签字段</legend><div>
              <label><input type="checkbox" data-dataset-label value="review.manual_start_time" checked />人工开始时间</label>
              <label><input type="checkbox" data-dataset-label value="review.manual_end_time" checked />人工结束时间</label>
              <label><input type="checkbox" data-dataset-label value="review.reference_quantity" checked />参考数量</label>
              <label><input type="checkbox" data-dataset-label value="review.boundary_acceptance" checked />边界验收</label>
              <label><input type="checkbox" data-dataset-label value="review.quantity_acceptance" checked />数量验收</label>
              <label><input type="checkbox" data-dataset-label value="batch.automatic_end_time" checked />自动结束时间</label>
              <label><input type="checkbox" data-dataset-label value="batch.automatic_quantity" checked />自动累计量</label>
            </div></fieldset>
            <label class="editor-field--wide"><span>创建依据</span><textarea id="dataset-reason" required maxlength="500"></textarea></label>
          </div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="dataset-definition-submit" value="default" class="button button--primary">创建定义</button></footer>
        </form>
      </dialog>
      <dialog id="dataset-snapshot-dialog" class="command-dialog editor-dialog">
        <form method="dialog" id="dataset-snapshot-form">
          <header><div><span>Point-in-time</span><h2>生成数据集清单</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div id="dataset-snapshot-summary" class="command-summary"></div>
          <div class="editor-fields dataset-snapshot-fields">
            <label><span>冻结时间</span><input id="dataset-freeze-at" type="datetime-local" step="1" required /></label>
            <label><span>产线（逗号分隔）</span><input id="dataset-snapshot-lines" required maxlength="2048" /></label>
            <label class="editor-field--wide"><span>规则版本 ID（可选，逗号分隔）</span><input id="dataset-rule-versions" maxlength="4096" /></label>
            <label class="dataset-checkbox editor-field--wide"><input id="dataset-exclude-low-confidence" type="checkbox" checked /><span>排除低置信度样本</span></label>
            <label class="editor-field--wide"><span>生成依据</span><textarea id="dataset-snapshot-reason" required maxlength="500"></textarea></label>
          </div>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="dataset-snapshot-submit" value="default" class="button button--primary">生成清单</button></footer>
        </form>
      </dialog>
      <dialog id="dataset-materialization-dialog" class="command-dialog">
        <form method="dialog" id="dataset-materialization-form">
          <header><div><span>Parquet 物化</span><h2 id="dataset-materialization-title">生成版本锁定对象</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div id="dataset-materialization-summary" class="command-summary"></div>
          <label><span>操作依据</span><textarea id="dataset-materialization-reason" required minlength="3" maxlength="500"></textarea></label>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="dataset-materialization-submit" value="default" class="button button--primary">生成 Parquet</button></footer>
        </form>
      </dialog>
      <dialog id="dataset-catalog-publication-dialog" class="command-dialog">
        <form method="dialog" id="dataset-catalog-publication-form">
          <header><div><span>Iceberg 目录发布</span><h2 id="dataset-catalog-publication-title">发布版本锁定对象</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div id="dataset-catalog-publication-summary" class="command-summary"></div>
          <label><span>操作依据</span><textarea id="dataset-catalog-publication-reason" required minlength="3" maxlength="500"></textarea></label>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="dataset-catalog-publication-submit" value="default" class="button button--primary">发布 Iceberg</button></footer>
        </form>
      </dialog>
      <dialog id="feature-flag-dialog" class="command-dialog">
        <form method="dialog" id="feature-flag-form">
          <header><div><span>运行治理</span><h2 id="feature-flag-dialog-title">变更运行开关</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="command-summary" id="feature-flag-command-summary"></div>
          <label><span>变更依据</span><textarea id="feature-flag-reason" minlength="8" maxlength="500" required placeholder="填写变更单号、验证范围和回滚依据"></textarea></label>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="feature-flag-submit" value="default" class="button button--primary">确认变更</button></footer>
        </form>
      </dialog>
      <div id="toast" class="toast" role="status" aria-live="polite"></div>
    </div>`;
  document.querySelector<HTMLSelectElement>('#plant-id')!.value = state.plantId;
  bindShellEvents();
  refreshIcons();
}

function refreshIcons(): void {
  createIcons({ icons: { Activity, Archive, Boxes, CheckCircle2, ChevronRight, ChevronsDown, CircleAlert, Clock3, Database, Factory, Filter, FlaskConical, Gauge, ListChecks, LockKeyhole, Network, Play, Plus, Power, RefreshCw, RotateCcw, Search, Settings2, ShieldCheck, Upload, X } });
}

function commandId(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6]! & 0x0f) | 0x40;
  bytes[8] = (bytes[8]! & 0x3f) | 0x80;
  const value = Array.from(bytes, (item) => item.toString(16).padStart(2, '0')).join('');
  return `${value.slice(0, 8)}-${value.slice(8, 12)}-${value.slice(12, 16)}-${value.slice(16, 20)}-${value.slice(20)}`;
}

function bindShellEvents(): void {
  document.querySelectorAll<HTMLButtonElement>('[data-view]').forEach((button) => {
    button.addEventListener('click', () => navigate(button.dataset.view as View));
  });
  document.querySelector('#refresh')?.addEventListener('click', () => void loadView());
  document.querySelector<HTMLSelectElement>('#plant-id')?.addEventListener('change', (event) => {
    state.plantId = (event.target as HTMLSelectElement).value;
    localStorage.setItem('bpi.plantId', state.plantId);
    void loadView();
  });
  document.querySelector<HTMLFormElement>('#confirm-form')?.addEventListener('submit', handleConfirm);
  document.querySelector<HTMLFormElement>('#simulation-form')?.addEventListener('submit', handleRuleSimulation);
  document.querySelector<HTMLFormElement>('#topology-editor-form')?.addEventListener('submit', handleTopologyDraft);
  document.querySelector<HTMLFormElement>('#rule-editor-form')?.addEventListener('submit', handleRuleDraft);
  document.querySelector<HTMLFormElement>('#point-catalog-form')?.addEventListener('submit', handlePointCatalogImport);
  document.querySelector<HTMLFormElement>('#point-calibration-form')?.addEventListener('submit', handlePointCalibrationSubmit);
  document.querySelector<HTMLFormElement>('#shadow-run-form')?.addEventListener('submit', handleShadowRunCreate);
  document.querySelector<HTMLFormElement>('#shadow-review-form')?.addEventListener('submit', handleShadowRunBatchReview);
  document.querySelector<HTMLFormElement>('#dataset-definition-form')?.addEventListener('submit', handleDatasetDefinitionCreate);
  document.querySelector<HTMLFormElement>('#dataset-snapshot-form')?.addEventListener('submit', handleDatasetSnapshotCreate);
  document.querySelector<HTMLFormElement>('#dataset-materialization-form')?.addEventListener('submit', handleDatasetMaterializationCommand);
  document.querySelector<HTMLFormElement>('#dataset-catalog-publication-form')?.addEventListener('submit', handleDatasetCatalogPublicationCommand);
  document.querySelector<HTMLFormElement>('#feature-flag-form')?.addEventListener('submit', handleFeatureFlagChange);
  document.querySelector<HTMLSelectElement>('#shadow-review-batch')?.addEventListener('change', applyShadowReviewBatch);
  document.querySelector<HTMLDialogElement>('#confirm-dialog')?.addEventListener('close', () => {
    setCommandAssigneeVisible(false);
    setCommandBoundaryVisible(false);
    state.dataQualityCommand = null;
    state.shadowRunCommand = null;
  });
  document.querySelector<HTMLDialogElement>('#feature-flag-dialog')?.addEventListener('close', () => {
    state.selectedFeatureFlag = null;
    state.featureFlagCommand = null;
  });
  document.querySelector<HTMLSelectElement>('#topology-base')?.addEventListener('change', applyTopologyBase);
  document.querySelector<HTMLSelectElement>('#rule-base')?.addEventListener('change', applyRuleBase);
}

function setCommandAssigneeVisible(visible: boolean, value = ''): void {
  const field = document.querySelector<HTMLElement>('#command-assignee-field');
  const input = document.querySelector<HTMLInputElement>('#command-assignee');
  if (!field || !input) return;
  field.hidden = !visible;
  input.required = visible;
  input.value = visible ? value : '';
}

function setCommandBoundaryVisible(visible: boolean, value = ''): void {
  const field = document.querySelector<HTMLElement>('#command-boundary-field');
  const input = document.querySelector<HTMLInputElement>('#command-boundary-time');
  if (!field || !input) return;
  field.hidden = !visible;
  input.required = visible;
  input.value = visible ? value : '';
}

function navigate(view: View): void {
  if (pointSearchTimer !== null) {
    window.clearTimeout(pointSearchTimer);
    pointSearchTimer = null;
  }
  if (view !== 'points') pointCatalogRequestGeneration += 1;
  state.view = view;
  state.error = null;
  closeDrawer();
  history.replaceState(null, '', `#/${view}`);
  void loadView();
}

async function loadView(silent = false): Promise<void> {
  if (state.loading) return;
  state.loading = true;
  state.error = null;
  if (!silent) renderLoading();
  try {
    if (state.view === 'overview') {
      const response = await bpiApi.overview(state.plantId, state.onlyAbnormal);
      state.lines = response.data;
      state.meta = response.meta;
    } else if (state.view === 'candidates') {
      const response = await bpiApi.candidates(state.plantId);
      state.candidates = response.data;
      state.meta = response.meta;
    } else if (state.view === 'batches') {
      const response = await bpiApi.batches(state.plantId);
      state.batches = response.data;
      state.meta = response.meta;
    } else if (state.view === 'shadowRuns') {
      const [runs, rules, batches] = await Promise.all([
        bpiApi.shadowRuns(state.plantId, {
          lineId: state.shadowRunLineId.trim() || undefined,
          state: state.shadowRunState,
        }),
        bpiApi.rules(state.plantId),
        bpiApi.batches(state.plantId),
      ]);
      state.shadowRuns = runs.data;
      state.rules = rules.data;
      state.batches = batches.data;
      state.meta = runs.meta;
    } else if (state.view === 'dataQuality') {
      const [incidents, summary] = await Promise.all([
        bpiApi.dataQualityIncidents(state.plantId, {
          lineId: normalizedDataQualityLine() || undefined,
          state: state.dataQualityState || undefined,
          search: normalizedDataQualitySearch() || undefined,
          limit: DATA_QUALITY_PAGE_SIZE,
        }),
        bpiApi.dataQualitySummary(state.plantId, normalizedDataQualityLine() || undefined),
      ]);
      state.dataQualityIncidents = incidents.data;
      state.dataQualityNextCursor = incidents.meta.nextCursor || null;
      state.dataQualitySnapshotAt = incidents.meta.snapshotAt;
      state.dataQualitySummary = summary.data;
      state.meta = incidents.meta;
    } else if (state.view === 'points') {
      if (pointSearchTimer !== null) {
        window.clearTimeout(pointSearchTimer);
        pointSearchTimer = null;
      }
      const requestGeneration = ++pointCatalogRequestGeneration;
      const [catalog, calibrations] = await Promise.all([
        bpiApi.currentPointCatalog(state.plantId, state.pointLineId, {
          limit: POINT_CATALOG_PAGE_SIZE,
          search: normalizedPointSearch() || undefined,
        }),
        bpiApi.listPointCalibrations(state.plantId, state.pointLineId, null, CALIBRATION_PAGE_SIZE),
      ]);
      if (requestGeneration === pointCatalogRequestGeneration) {
        applyPointCatalogPage(catalog);
      }
      state.calibrations = calibrations.data;
      state.calibrationNextCursor = calibrations.meta.nextCursor || null;
      state.calibrationSnapshotAt = calibrations.meta.snapshotAt;
      state.meta = catalog.meta;
    } else if (state.view === 'featureFlags') {
      const response = await bpiApi.featureFlags(
        state.plantId,
        state.featureFlagLineId,
        state.featureFlagScopeType,
      );
      state.featureFlags = response.data;
      state.meta = response.meta;
    } else if (state.view === 'datasets') {
      const response = await bpiApi.datasets(state.plantId);
      state.datasets = response.data;
      state.meta = response.meta;
    } else {
      const [rules, topologies] = await Promise.all([
        bpiApi.rules(state.plantId),
        bpiApi.topologies(state.plantId),
      ]);
      state.rules = rules.data;
      state.topologies = topologies.data;
      state.meta = rules.meta;
      state.pointCatalog = (await bpiApi.currentPointCatalog(state.plantId, state.pointLineId)).data;
    }
  } catch (error) {
    state.error = error instanceof Error ? error : new Error(String(error));
  } finally {
    state.loading = false;
    renderView();
  }
}

function renderLoading(): void {
  const content = document.querySelector('#content');
  if (!content) return;
  content.innerHTML = `<div class="loading-table"><div></div><div></div><div></div><div></div><div></div></div>`;
}

function renderView(): void {
  const titles: Record<View, [string, string]> = {
    overview: ['生产运行', '实时生产态势'],
    candidates: ['边界审核', '候选批次'],
    batches: ['生产事实', '批次档案'],
    shadowRuns: ['运行验收', '影子运行验收'],
    dataQuality: ['运行治理', '数据质量事件'],
    points: ['数据准入', '点位目录'],
    rules: ['边界治理', '规则与拓扑'],
    datasets: ['训练数据治理', '数据集清单'],
    featureFlags: ['运行治理', '运行开关'],
  };
  document.querySelector('#view-kicker')!.textContent = titles[state.view][0];
  document.querySelector('#view-title')!.textContent = titles[state.view][1];
  document.querySelectorAll('[data-view]').forEach((item) => item.classList.toggle('is-active', (item as HTMLElement).dataset.view === state.view));
  document.querySelector('#candidate-count')!.textContent = String(state.candidates.filter((item) => item.state === 'PENDING').length || state.lines.reduce((sum, item) => sum + item.pendingCandidates, 0));
  const snapshot = document.querySelector('#snapshot');
  if (snapshot) snapshot.innerHTML = `<i data-lucide="clock-3"></i>${state.meta ? formatTime(state.meta.snapshotAt) : '尚未同步'}`;
  renderBanner();
  if (state.error) renderError();
  else if (state.view === 'overview') renderOverview();
  else if (state.view === 'candidates') renderCandidates();
  else if (state.view === 'batches') renderBatches();
  else if (state.view === 'shadowRuns') renderShadowRuns();
  else if (state.view === 'dataQuality') renderDataQuality();
  else if (state.view === 'points') renderPoints();
  else if (state.view === 'datasets') renderDatasets();
  else if (state.view === 'featureFlags') renderFeatureFlags();
  else renderRules();
  refreshIcons();
}

function renderBanner(): void {
  const banner = document.querySelector<HTMLDivElement>('#banner');
  if (!banner) return;
  if (!state.error) {
    banner.hidden = true;
    return;
  }
  const problem = state.error instanceof ApiProblem ? state.error.problem : null;
  banner.hidden = false;
  banner.innerHTML = `<i data-lucide="circle-alert"></i><div><strong>${escapeHtml(problem?.title || '数据加载失败')}</strong><span>${escapeHtml(state.error.message)}${problem?.traceId ? ` · traceId ${escapeHtml(problem.traceId)}` : ''}</span></div><button id="banner-retry" class="button button--secondary">重试</button>`;
  banner.querySelector('#banner-retry')?.addEventListener('click', () => void loadView());
}

function renderError(): void {
  document.querySelector('#content')!.innerHTML = `<div class="empty-state"><i data-lucide="circle-alert"></i><strong>当前数据不可用</strong><span>保留筛选条件，修复连接后可直接重试。</span></div>`;
}

function overviewToolbar(): string {
  return `<div class="toolbar"><div class="segmented" role="group" aria-label="显示范围"><button data-abnormal="false" class="${state.onlyAbnormal ? '' : 'is-selected'}">全部产线</button><button data-abnormal="true" class="${state.onlyAbnormal ? 'is-selected' : ''}">仅异常</button></div><div class="toolbar-note"><i data-lucide="shield-check"></i>影子模式</div></div>`;
}

function renderOverview(): void {
  const content = document.querySelector('#content')!;
  if (!state.lines.length) {
    content.innerHTML = `${overviewToolbar()}<div class="empty-state"><i data-lucide="gauge"></i><strong>暂无产线上下文</strong><span>等待候选事件或生产指令进入 BPI。</span></div>`;
  } else {
    const rows = state.lines.map((line) => `
      <tr data-line-id="${escapeHtml(line.lineId)}" tabindex="0">
        <td><strong>${escapeHtml(line.lineName || line.lineId)}</strong><small>${escapeHtml(line.lineId)}</small></td>
        <td>${statusChip(line.status)}</td><td>${escapeHtml(line.orderId || '-')}</td><td>${escapeHtml(line.stageCode)}</td>
        <td class="metric"><b>${number(line.instantFlow)}</b><small>t/h</small></td>
        <td class="metric"><b>${number(line.totalizedQuantity)}</b><small>t</small></td>
        <td><span class="confidence"><i style="--value:${Math.round((line.confidence || 0) * 100)}%"></i><b>${number((line.confidence || 0) * 100, 0)}%</b></span></td>
        <td>${statusChip(line.dataHealth)}</td><td><button class="count-link" data-open-candidates="${escapeHtml(line.lineId)}">${line.pendingCandidates}</button></td>
        <td>${formatTime(line.lastEventTime)}</td><td><i data-lucide="chevron-right"></i></td>
      </tr>`).join('');
    content.innerHTML = `${overviewToolbar()}<div class="process-strip"><span class="is-complete">原料接收</span><span class="is-active">蒸发浓缩</span><span>结晶</span><span>分蜜</span><span>包装入库</span></div><div class="table-frame"><table><thead><tr><th>产线</th><th>运行状态</th><th>生产指令</th><th>当前工段</th><th>瞬时流量</th><th>累计量</th><th>边界置信度</th><th>数据健康</th><th>待审核</th><th>最后事件</th><th></th></tr></thead><tbody>${rows}</tbody></table></div>`;
  }
  content.querySelectorAll<HTMLButtonElement>('[data-abnormal]').forEach((button) => button.addEventListener('click', () => {
    state.onlyAbnormal = button.dataset.abnormal === 'true';
    localStorage.setItem('bpi.onlyAbnormal', String(state.onlyAbnormal));
    void loadView();
  }));
  content.querySelectorAll('[data-line-id]').forEach((row) => row.addEventListener('click', () => openLine(String((row as HTMLElement).dataset.lineId))));
  content.querySelectorAll('[data-open-candidates]').forEach((button) => button.addEventListener('click', (event) => {
    event.stopPropagation(); navigate('candidates');
  }));
}

async function openLine(lineId: string): Promise<void> {
  try {
    const response = await bpiApi.line(lineId);
    const line = response.data;
    openDrawer(`
      <header><div><span>实时证据</span><h2>${escapeHtml(line.lineName || line.lineId)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header>
      <div class="drawer-section facts-grid"><div><span>运行状态</span>${statusChip(line.status)}</div><div><span>生产指令</span><b>${escapeHtml(line.orderId || '-')}</b></div><div><span>工段</span><b>${escapeHtml(line.stageCode)}</b></div><div><span>待审核</span><b>${line.pendingCandidates}</b></div></div>
      <div class="drawer-section"><h3>最近 15 分钟</h3><div class="trend-plot" role="img" aria-label="瞬时流量趋势"><i style="height:28%"></i><i style="height:42%"></i><i style="height:38%"></i><i style="height:55%"></i><i style="height:71%"></i><i style="height:68%"></i><i style="height:79%"></i><i style="height:74%"></i><i style="height:84%"></i><i style="height:82%"></i><i style="height:88%"></i><i style="height:86%"></i></div><div class="trend-scale"><span>15 分钟前</span><b>${number(line.instantFlow)} t/h</b><span>当前</span></div></div>
      <div class="drawer-section"><h3>运行判据</h3><ul class="check-list"><li><i data-lucide="check-circle-2"></i>生产指令有效</li><li><i data-lucide="check-circle-2"></i>流量持续超过阈值</li><li><i data-lucide="check-circle-2"></i>目标罐液位上升</li></ul></div>`);
  } catch (error) { showToast(error instanceof Error ? error.message : String(error), true); }
}

function renderCandidates(): void {
  const content = document.querySelector('#content')!;
  const rows = state.candidates.map((candidate) => `
    <tr data-candidate-id="${escapeHtml(candidate.id)}" tabindex="0">
      <td>${formatTime(candidate.boundaryTime)}</td><td><strong>${escapeHtml(candidate.lineId)}</strong></td><td>${statusChip(candidate.boundaryType)}</td>
      <td>${escapeHtml(candidate.orderId || '-')}</td><td><b>${number(candidate.confidence * 100, 0)}%</b></td>
      <td>${candidate.evidence.filter((item) => item.satisfied).length} / ${candidate.evidence.length}</td><td>${candidate.missingSignals.length ? escapeHtml(candidate.missingSignals.join(', ')) : '-'}</td>
      <td>${statusChip(candidate.state)}</td><td><span class="revision">r${candidate.revision}</span></td><td><i data-lucide="chevron-right"></i></td>
    </tr>`).join('');
  content.innerHTML = `<div class="toolbar"><label class="search-field"><i data-lucide="search"></i><input id="candidate-search" placeholder="候选键、产线、指令" /></label><button class="icon-text-button" title="筛选"><i data-lucide="filter"></i><span>待处理</span></button></div>${rows ? `<div class="table-frame"><table><thead><tr><th>候选时间</th><th>产线</th><th>边界</th><th>生产指令</th><th>置信度</th><th>证据</th><th>缺失项</th><th>状态</th><th>版本</th><th></th></tr></thead><tbody>${rows}</tbody></table></div>` : `<div class="empty-state"><i data-lucide="list-checks"></i><strong>没有待审核候选</strong><span>新的 START/END 边界会按事件时间进入队列。</span></div>`}`;
  content.querySelectorAll('[data-candidate-id]').forEach((row) => row.addEventListener('click', () => void openCandidate(String((row as HTMLElement).dataset.candidateId))));
  content.querySelector<HTMLInputElement>('#candidate-search')?.addEventListener('input', (event) => {
    const keyword = (event.target as HTMLInputElement).value.trim().toLowerCase();
    content.querySelectorAll<HTMLTableRowElement>('[data-candidate-id]').forEach((row) => { row.hidden = !row.textContent!.toLowerCase().includes(keyword); });
  });
}

async function openCandidate(candidateId: string): Promise<void> {
  try {
    const response = await bpiApi.candidate(candidateId);
    const candidate = response.data;
    state.selectedCandidate = candidate;
    const evidence = candidate.evidence.map((item) => `<li><span class="evidence-state evidence-state--${item.satisfied ? 'ok' : 'bad'}"></span><div><strong>${escapeHtml(item.signal)}</strong><small>${escapeHtml(item.source)} · ${formatTime(item.eventTime)}</small></div><b>${escapeHtml(item.value)}${item.unit ? ` ${escapeHtml(item.unit)}` : ''}</b><em>${escapeHtml(item.classification)}</em></li>`).join('');
    openDrawer(`
      <header><div><span>${escapeHtml(candidate.boundaryType)} 候选</span><h2>${escapeHtml(candidate.lineId)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header>
      <div class="candidate-hero"><div><span>边界时间</span><b>${formatTime(candidate.boundaryTime)}</b></div><div><span>置信度</span><strong>${number(candidate.confidence * 100, 0)}%</strong></div></div>
      <div class="drawer-section facts-grid"><div><span>生产指令</span><b>${escapeHtml(candidate.orderId || '-')}</b></div><div><span>状态</span>${statusChip(candidate.state)}</div><div><span>规则版本</span><b>${escapeHtml(candidate.ruleVersion)}</b></div><div><span>拓扑版本</span><b>${escapeHtml(candidate.topologyVersion)}</b></div></div>
      <div class="drawer-section"><div class="section-title"><h3>规则证据</h3><span>${candidate.evidence.filter((item) => item.satisfied).length}/${candidate.evidence.length} 已满足</span></div><ul class="evidence-list">${evidence}</ul></div>
      <footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>暂不处理</button><button class="button button--danger" id="open-reject" ${candidate.state !== 'PENDING' ? 'disabled' : ''}>拒绝候选</button><button class="button button--primary" id="open-confirm" ${candidate.state !== 'PENDING' ? 'disabled' : ''}>确认候选</button></footer>`);
    document.querySelector('#open-confirm')?.addEventListener('click', () => openCandidateCommandDialog('confirm'));
    document.querySelector('#open-reject')?.addEventListener('click', () => openCandidateCommandDialog('reject'));
  } catch (error) { showToast(error instanceof Error ? error.message : String(error), true); }
}

function openCandidateCommandDialog(command: 'confirm' | 'reject'): void {
  const candidate = state.selectedCandidate;
  if (!candidate) return;
  state.candidateCommand = command;
  state.batchCommand = null;
  state.ruleCommand = null;
  state.topologyCommand = null;
  state.calibrationCommand = null;
  const isReject = command === 'reject';
  const isEnd = candidate.boundaryType === 'END';
  document.querySelector('#command-kicker')!.textContent = '候选批次';
  document.querySelector('#command-title')!.textContent = isReject
    ? '拒绝候选边界'
    : isEnd ? '确认结束边界' : '确认启动边界';
  document.querySelector('#command-reason-label')!.textContent = isReject ? '拒绝原因' : '确认原因';
  const resultLabel = isReject ? '处理结果' : isEnd ? '批次状态' : '拟定批次';
  const resultValue = isReject ? '不变更批次' : isEnd ? 'ACTIVE → CLOSED_RAW' : `BPI · ${formatTime(candidate.boundaryTime)}`;
  document.querySelector('#command-summary')!.innerHTML = `<div><span>产线</span><b>${escapeHtml(candidate.lineId)}</b></div><div><span>生产指令</span><b>${escapeHtml(candidate.orderId || '-')}</b></div><div><span>${resultLabel}</span><b>${resultValue}</b></div><div><span>版本</span><b>r${candidate.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = isReject
    ? '填写误判、上下文错误或现场处置依据'
    : isEnd ? '填写流量、泵阀路径或现场停产依据' : '填写现场确认依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = `button ${isReject ? 'button--danger' : 'button--primary'}`;
  button.textContent = isReject ? '拒绝候选' : isEnd ? '确认并关闭原始批次' : '确认并生成影子批次';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

function applyCandidateReview(candidate: Candidate): void {
  state.selectedCandidate = candidate;
  state.candidates = state.candidates.map((item) => item.id === candidate.id ? candidate : item);
  state.lines = state.lines.map((line) => line.lineId === candidate.lineId
    ? { ...line, pendingCandidates: Math.max(0, line.pendingCandidates - 1) }
    : line);
}

async function handleConfirm(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  if (state.dataQualityCommand) {
    await handleDataQualityCommand();
    return;
  }
  if (state.shadowRunCommand) {
    await handleShadowRunCommand();
    return;
  }
  if (state.calibrationCommand) {
    await handlePointCalibrationCommand();
    return;
  }
  if (state.topologyCommand) {
    await handleTopologyCommand();
    return;
  }
  if (state.ruleCommand) {
    await handleRuleCommand();
    return;
  }
  if (state.batchCommand) {
    await handleBatchCommand();
    return;
  }
  const candidate = state.selectedCandidate;
  const command = state.candidateCommand;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  if (!candidate || !command || reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = command === 'reject' ? '拒绝中...' : '确认中...';
  try {
    if (command === 'reject') {
      const response = await bpiApi.rejectCandidate(candidate, reason, commandId());
      applyCandidateReview(response.data);
      document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
      closeDrawer();
      showToast('候选已拒绝，未生成影子批次');
      state.view = 'candidates';
      history.replaceState(null, '', '#/candidates');
      await loadView();
      return;
    }
    const response = await bpiApi.confirmCandidate(candidate, reason, commandId());
    applyCandidateReview(response.data.candidate);
    try {
      const pendingResponse = await bpiApi.candidates(state.plantId);
      state.candidates = pendingResponse.data;
      state.lines = state.lines.map((line) => line.lineId === candidate.lineId
        ? { ...line, pendingCandidates: state.candidates.filter((item) => item.lineId === line.lineId).length }
        : line);
    } catch {
      state.candidates = state.candidates.filter((item) => item.id !== candidate.id);
    }
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    closeDrawer();
    showToast(candidate.boundaryType === 'END'
      ? `批次 ${response.data.batch.batchNo} 已关闭为 CLOSED_RAW`
      : `影子批次 ${response.data.batch.batchNo} 已生成`);
    state.view = 'batches';
    state.selectedBatch = response.data.batch;
    await loadView();
    await openBatch(response.data.batch.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`候选已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openCandidate(candidate.id);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.className = `button ${command === 'reject' ? 'button--danger' : 'button--primary'}`;
    button.textContent = command === 'reject'
      ? '拒绝候选'
      : candidate.boundaryType === 'END' ? '确认并关闭原始批次' : '确认并生成影子批次';
  }
}

function shadowRunBlockerLabel(code: string): string {
  const labels: Record<string, string> = {
    RULE_NOT_PUBLISHED: '固定规则版本尚未发布',
    RULE_NOT_ACTIVE: '固定规则版本不是当前激活版本',
    RULE_PUBLICATION_NOT_CONFIRMED: 'Kafka 尚未确认规则发布事件',
    RULE_APPLICATION_NOT_APPLIED: 'Flink 控制面尚未返回 APPLIED',
    RULE_RUNTIME_NOT_READY: '流式评估器尚未返回 READY',
    TOPOLOGY_NOT_PUBLISHED: '固定拓扑版本尚未发布',
    TOPOLOGY_POINT_CATALOG_MISMATCH: '拓扑校验使用了不同的点位目录快照',
    POINT_CATALOG_NOT_CURRENT: '固定点位目录已不是产线当前快照',
    POINT_CATALOG_NOT_READY: '固定点位目录仍有未准入点位',
    MINIMUM_DURATION_NOT_REACHED: '尚未达到最短影子运行周期',
    MINIMUM_BATCH_REVIEWS_NOT_REACHED: '人工复核批次数不足',
    BOUNDARY_AGREEMENT_BELOW_THRESHOLD: '边界一致率低于验收阈值',
    CUMULATIVE_QUANTITY_DEVIATION_OUT_OF_TOLERANCE: '累计数量偏差超过验收阈值',
    UNRESOLVED_CRITICAL_DATA_QUALITY: '运行窗口内仍有未解决的严重数据质量事件',
  };
  return labels[code] || code;
}

function formatObservedDays(seconds: number): string {
  const days = Math.max(0, seconds) / 86_400;
  return `${number(days, days >= 10 ? 0 : 1)} 天`;
}

function renderShadowRuns(): void {
  const content = document.querySelector<HTMLElement>('#content')!;
  const counts = {
    draft: state.shadowRuns.filter((item) => item.state === 'DRAFT').length,
    running: state.shadowRuns.filter((item) => item.state === 'RUNNING').length,
    evaluating: state.shadowRuns.filter((item) => item.state === 'EVALUATING').length,
    approved: state.shadowRuns.filter((item) => item.state === 'APPROVED').length,
    blocked: state.shadowRuns.filter((item) => item.blockers.length > 0 && !['REJECTED', 'CANCELLED'].includes(item.state)).length,
  };
  const rows = state.shadowRuns.map((run) => `
    <tr data-shadow-run-id="${escapeHtml(run.id)}" tabindex="0">
      <td><strong>${escapeHtml(run.name)}</strong><small>${escapeHtml(run.runCode)}</small></td>
      <td><strong>${escapeHtml(run.lineId)}</strong><small>${escapeHtml(run.plantId)}</small></td>
      <td>${statusChip(run.state)}</td>
      <td><strong>${formatObservedDays(run.metrics.observedDurationSeconds)}</strong><small>目标 ${run.minimumDurationDays} 天</small></td>
      <td><strong>${run.metrics.reviewedBatchCount} / ${run.minimumReviewedBatches}</strong><small>${run.metrics.acceptedBoundaryCount} / ${run.metrics.totalBoundaryCount} 边界通过</small></td>
      <td><strong>${number(run.metrics.boundaryAgreement * 100, 1)}%</strong><small>阈值 ${number(run.minimumBoundaryAgreement * 100, 1)}%</small></td>
      <td><strong>${number(run.metrics.cumulativeQuantityDeviationPercent, 2)}%</strong><small>容差 ${number(run.quantityTolerancePercent, 2)}%</small></td>
      <td>${run.readiness.ready ? statusChip('READY') : statusChip('BLOCKED')}<small>${run.blockers.length} 个阻断项</small></td>
      <td>${run.readyForApproval ? statusChip('READY') : run.state === 'APPROVED' ? statusChip('APPROVED') : '<span class="revision">待满足门禁</span>'}</td>
      <td><span class="revision">r${run.revision}</span></td><td><i data-lucide="chevron-right"></i></td>
    </tr>`).join('');
  const stateOptions: Array<[string, string]> = [
    ['', '全部状态'], ['DRAFT', '草稿'], ['RUNNING', '运行中'], ['EVALUATING', '待审批'],
    ['APPROVED', '已批准'], ['REJECTED', '已驳回'], ['CANCELLED', '已取消'],
  ];
  content.innerHTML = `
    <div class="shadow-run-summary" aria-label="影子运行验收汇总">
      <div><span>草稿</span><b>${counts.draft}</b></div><div><span>运行中</span><b>${counts.running}</b></div>
      <div><span>待审批</span><b>${counts.evaluating}</b></div><div><span>已批准</span><b>${counts.approved}</b></div>
      <div class="${counts.blocked ? 'is-blocked' : ''}"><span>存在阻断</span><b>${counts.blocked}</b></div>
    </div>
    <div class="toolbar shadow-run-toolbar">
      <div class="toolbar-note"><i data-lucide="shield-check"></i>批准只代表影子验收结论，不开启 WOM / QCS / WMS 写入</div>
      <div class="toolbar-actions">
        <label class="line-field"><span>产线</span><input id="shadow-run-line-filter" value="${escapeHtml(state.shadowRunLineId)}" placeholder="全部产线" /></label>
        <label class="line-field shadow-run-state-field"><span>状态</span><select id="shadow-run-state-filter">${stateOptions.map(([value, label]) => `<option value="${value}" ${state.shadowRunState === value ? 'selected' : ''}>${label}</option>`).join('')}</select></label>
        <button id="apply-shadow-run-filter" class="icon-button" title="查询" aria-label="查询"><i data-lucide="search"></i></button>
        <button id="new-shadow-run" class="icon-text-button"><i data-lucide="plus"></i><span>新建影子运行</span></button>
      </div>
    </div>
    ${rows ? `<div class="table-frame"><table class="shadow-run-table"><thead><tr><th>验收任务</th><th>产线</th><th>状态</th><th>观察周期</th><th>复核批次</th><th>边界一致率</th><th>累计数量偏差</th><th>运行准入</th><th>审批门禁</th><th>版本</th><th></th></tr></thead><tbody>${rows}</tbody></table></div>` : `<div class="empty-state"><i data-lucide="flask-conical"></i><strong>当前筛选范围没有影子运行</strong><span>从已发布、已应用且运行就绪的规则版本创建受控验收任务。</span></div>`}`;
  content.querySelectorAll<HTMLElement>('[data-shadow-run-id]').forEach((row) => row.addEventListener('click', () => void openShadowRun(String(row.dataset.shadowRunId))));
  const applyFilter = () => {
    state.shadowRunLineId = document.querySelector<HTMLInputElement>('#shadow-run-line-filter')!.value.trim();
    state.shadowRunState = document.querySelector<HTMLSelectElement>('#shadow-run-state-filter')!.value as ShadowRunState | '';
    localStorage.setItem('bpi.shadowRunLineId', state.shadowRunLineId);
    localStorage.setItem('bpi.shadowRunState', state.shadowRunState);
    void loadView();
  };
  content.querySelector('#apply-shadow-run-filter')?.addEventListener('click', applyFilter);
  content.querySelector('#shadow-run-state-filter')?.addEventListener('change', applyFilter);
  content.querySelector<HTMLInputElement>('#shadow-run-line-filter')?.addEventListener('keydown', (event) => { if (event.key === 'Enter') applyFilter(); });
  content.querySelector('#new-shadow-run')?.addEventListener('click', openShadowRunCreateDialog);
}

function featureFlagScopeLabel(scopeType: string): string {
  const labels: Record<string, string> = {
    GLOBAL: '平台默认',
    TENANT: '租户',
    PLANT: '工厂',
    LINE: '产线',
    DEFAULT_DENY: '默认拒绝',
  };
  return labels[scopeType] || scopeType;
}

function featureFlagEnforcementLabel(status: string): string {
  const labels: Record<string, string> = {
    ENFORCED: '后端已执行',
    PHASE_LOCKED: '阶段门禁锁定',
    CODE_INVARIANT: '代码不变量',
    PENDING_SHELL_INTEGRATION: '待旧平台接入',
  };
  return labels[status] || status;
}

function renderFeatureFlags(): void {
  const content = document.querySelector<HTMLElement>('#content')!;
  const counts = {
    total: state.featureFlags.length,
    enabled: state.featureFlags.filter((item) => item.effectiveEnabled).length,
    overridden: state.featureFlags.filter((item) => item.overrideActive).length,
    locked: state.featureFlags.filter((item) => !item.editable).length,
  };
  const rows = state.featureFlags.map((flag) => {
    const enforcementDetail = flag.flagKey === 'bpi.ui'
      ? '旧 MES 菜单由 Java 8 adapter 按当前试点范围解析'
      : '写操作由 BPI 服务强制校验';
    const selectedOverride = flag.overrideActive
      ? `<div class="flag-state ${flag.overrideEnabled ? '' : 'is-disabled'}"><strong>${flag.overrideEnabled ? '显式启用' : '显式禁用'}</strong><small>${escapeHtml(featureFlagScopeLabel(flag.selectedScopeType))} · r${flag.overrideRevision}</small></div>`
      : `<div class="flag-state"><strong>继承上级</strong><small>${flag.overrideExists ? `覆盖已移除 · r${flag.overrideRevision}` : '当前层未配置'}</small></div>`;
    const enforcement = flag.editable
      ? `<div class="flag-source"><strong>${escapeHtml(featureFlagEnforcementLabel(flag.enforcementStatus))}</strong><small>${escapeHtml(enforcementDetail)}</small></div>`
      : `<div class="flag-lock"><i data-lucide="lock-keyhole"></i><div><strong>${escapeHtml(featureFlagEnforcementLabel(flag.enforcementStatus))}</strong><small title="${escapeHtml(flag.blockedReason || '')}">${escapeHtml(flag.blockedReason || '当前阶段不可编辑')}</small></div></div>`;
    const actions = flag.editable
      ? `<div class="feature-flag-actions">
          <button class="button button--primary button--compact" data-feature-flag-key="${escapeHtml(flag.flagKey)}" data-feature-flag-action="enable" ${flag.overrideActive && flag.overrideEnabled ? 'disabled' : ''}><i data-lucide="power"></i>启用</button>
          <button class="button button--danger button--compact" data-feature-flag-key="${escapeHtml(flag.flagKey)}" data-feature-flag-action="disable" ${flag.overrideActive && flag.overrideEnabled === false ? 'disabled' : ''}><i data-lucide="power"></i>禁用</button>
          <button class="button button--secondary button--compact" data-feature-flag-key="${escapeHtml(flag.flagKey)}" data-feature-flag-action="inherit" ${flag.overrideActive ? '' : 'disabled'}><i data-lucide="rotate-ccw"></i>继承</button>
        </div>`
      : '<span class="revision">只读</span>';
    return `<tr data-feature-flag-row="${escapeHtml(flag.flagKey)}">
      <td><strong>${escapeHtml(flag.displayName)}</strong><small>${escapeHtml(flag.flagKey)} · ${escapeHtml(flag.riskLevel)}</small><small class="flag-description" title="${escapeHtml(flag.description)}">${escapeHtml(flag.description)}</small></td>
      <td><div class="flag-state ${flag.effectiveEnabled ? '' : 'is-disabled'}"><strong>${flag.effectiveEnabled ? '已启用' : '已禁用'}</strong><small>${flag.effectiveRevision === null || flag.effectiveRevision === undefined ? '无实体版本' : `r${flag.effectiveRevision}`}</small></div></td>
      <td><div class="flag-source"><strong>${escapeHtml(featureFlagScopeLabel(flag.effectiveScopeType))}</strong><code title="${escapeHtml(flag.effectiveScopeKey)}">${escapeHtml(flag.effectiveScopeKey)}</code></div></td>
      <td>${selectedOverride}</td>
      <td>${enforcement}</td>
      <td><strong>${escapeHtml(flag.updatedBy || '-')}</strong><small>${formatTime(flag.updatedAt)}${flag.lastReason ? ` · ${escapeHtml(flag.lastReason)}` : ''}</small></td>
      <td>${actions}</td>
    </tr>`;
  }).join('');
  const selectedScopeKey = state.featureFlags[0]?.selectedScopeKey || '-';
  content.innerHTML = `
    <div class="feature-flag-summary" aria-label="运行开关汇总">
      <div><span>受控开关</span><b>${counts.total}</b></div>
      <div><span>当前有效启用</span><b>${counts.enabled}</b></div>
      <div><span>当前层显式覆盖</span><b>${counts.overridden}</b></div>
      <div class="${counts.locked ? 'is-locked' : ''}"><span>阶段锁定 / 只读</span><b>${counts.locked}</b></div>
    </div>
    <div class="toolbar feature-flag-toolbar">
      <div class="toolbar-note"><i data-lucide="shield-check"></i>Phase 1 保持影子运行；开关变更不会开启 WOM / QCS / WMS 写入</div>
      <div class="toolbar-actions">
        <label class="line-field"><span>产线</span><input id="feature-flag-line" value="${escapeHtml(state.featureFlagLineId)}" required /></label>
        <div class="segmented" role="group" aria-label="覆盖作用域">
          ${(['TENANT', 'PLANT', 'LINE'] as FeatureFlagScopeType[]).map((scope) => `<button data-feature-flag-scope="${scope}" class="${state.featureFlagScopeType === scope ? 'is-selected' : ''}">${featureFlagScopeLabel(scope)}</button>`).join('')}
        </div>
        <button id="apply-feature-flag-target" class="icon-button" title="查询" aria-label="查询"><i data-lucide="search"></i></button>
      </div>
    </div>
    <div class="feature-flag-target-note">当前解析目标：${escapeHtml(state.plantId)} / ${escapeHtml(state.featureFlagLineId)}；编辑层：${escapeHtml(featureFlagScopeLabel(state.featureFlagScopeType))} (${escapeHtml(selectedScopeKey)})</div>
    ${rows ? `<div class="table-frame"><table class="feature-flag-table"><colgroup><col class="flag-col-name" /><col class="flag-col-value" /><col class="flag-col-source" /><col class="flag-col-override" /><col class="flag-col-enforcement" /><col class="flag-col-change" /><col class="flag-col-actions" /></colgroup><thead><tr><th>开关</th><th>当前有效值</th><th>生效来源</th><th>选中层覆盖</th><th>执行状态</th><th>最近变更</th><th>操作</th></tr></thead><tbody>${rows}</tbody></table></div>` : '<div class="empty-state"><i data-lucide="settings-2"></i><strong>没有可管理的运行开关</strong><span>请检查 BPI 服务迁移版本和当前租户范围。</span></div>'}`;

  const applyTarget = () => {
    const lineId = content.querySelector<HTMLInputElement>('#feature-flag-line')!.value.trim();
    if (!lineId) {
      showToast('运行开关解析必须指定产线', true);
      return;
    }
    state.featureFlagLineId = lineId;
    localStorage.setItem('bpi.featureFlagLineId', lineId);
    localStorage.setItem('bpi.featureFlagScopeType', state.featureFlagScopeType);
    void loadView();
  };
  content.querySelector('#apply-feature-flag-target')?.addEventListener('click', applyTarget);
  content.querySelector<HTMLInputElement>('#feature-flag-line')?.addEventListener('keydown', (event) => { if (event.key === 'Enter') applyTarget(); });
  content.querySelectorAll<HTMLButtonElement>('[data-feature-flag-scope]').forEach((button) => button.addEventListener('click', () => {
    state.featureFlagScopeType = button.dataset.featureFlagScope as FeatureFlagScopeType;
    applyTarget();
  }));
  content.querySelectorAll<HTMLButtonElement>('[data-feature-flag-action]').forEach((button) => button.addEventListener('click', () => {
    const flag = state.featureFlags.find((item) => item.flagKey === button.dataset.featureFlagKey);
    if (!flag) return;
    const action = button.dataset.featureFlagAction;
    if (action === 'inherit') openFeatureFlagDialog(flag, 'INHERIT');
    else openFeatureFlagDialog(flag, 'SET', action === 'enable');
  }));
}

function openFeatureFlagDialog(flag: FeatureFlag, mode: 'SET' | 'INHERIT', enabled?: boolean): void {
  if (!flag.editable) {
    showToast(flag.blockedReason || '当前开关不可编辑', true);
    return;
  }
  state.selectedFeatureFlag = flag;
  state.featureFlagCommand = { mode, enabled };
  const actionLabel = mode === 'INHERIT' ? '移除当前层覆盖并继承上级' : (enabled ? '显式启用' : '显式禁用');
  document.querySelector('#feature-flag-dialog-title')!.textContent = mode === 'INHERIT' ? '恢复上级继承' : `${enabled ? '启用' : '禁用'}运行开关`;
  document.querySelector('#feature-flag-command-summary')!.innerHTML = `
    <div><span>开关</span><b>${escapeHtml(flag.displayName)}<small>${escapeHtml(flag.flagKey)}</small></b></div>
    <div><span>目标作用域</span><b>${escapeHtml(featureFlagScopeLabel(flag.selectedScopeType))} · ${escapeHtml(flag.selectedScopeKey)}</b></div>
    <div><span>当前有效值</span><b>${flag.effectiveEnabled ? '已启用' : '已禁用'} · ${escapeHtml(featureFlagScopeLabel(flag.effectiveScopeType))}</b></div>
    <div><span>本次动作</span><b>${escapeHtml(actionLabel)} · If-Match r${flag.overrideRevision}</b></div>`;
  document.querySelector<HTMLTextAreaElement>('#feature-flag-reason')!.value = '';
  const submit = document.querySelector<HTMLButtonElement>('#feature-flag-submit')!;
  submit.className = `button ${mode === 'SET' && enabled === false ? 'button--danger' : 'button--primary'}`;
  submit.textContent = mode === 'INHERIT' ? '确认恢复继承' : (enabled ? '确认启用' : '确认禁用');
  document.querySelector<HTMLDialogElement>('#feature-flag-dialog')!.showModal();
}

async function handleFeatureFlagChange(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  const flag = state.selectedFeatureFlag;
  const requested = state.featureFlagCommand;
  if (!flag || !requested) return;
  const reason = document.querySelector<HTMLTextAreaElement>('#feature-flag-reason')!.value.trim();
  if (reason.length < 8) {
    showToast('变更依据至少填写 8 个字符', true);
    return;
  }
  const command: FeatureFlagOverrideCommand = {
    scopeType: flag.selectedScopeType,
    plantId: state.plantId,
    lineId: state.featureFlagLineId,
    mode: requested.mode,
    ...(requested.mode === 'SET' ? { enabled: requested.enabled } : {}),
    reason,
  };
  const button = document.querySelector<HTMLButtonElement>('#feature-flag-submit')!;
  const originalText = button.textContent || '确认变更';
  button.disabled = true;
  button.textContent = '提交中...';
  try {
    const response = await bpiApi.changeFeatureFlag(flag, command, commandId());
    state.featureFlags = state.featureFlags.map((item) => item.flagKey === response.data.flagKey ? response.data : item);
    document.querySelector<HTMLDialogElement>('#feature-flag-dialog')!.close();
    showToast(`${response.data.displayName} 已${requested.mode === 'INHERIT' ? '恢复继承' : (requested.enabled ? '启用' : '禁用')}`);
    await loadView(true);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      document.querySelector<HTMLDialogElement>('#feature-flag-dialog')!.close();
      await loadView(true);
      showToast(`运行开关已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
    } else {
      showToast(error instanceof Error ? error.message : String(error), true);
    }
  } finally {
    button.disabled = false;
    button.textContent = originalText;
  }
}

async function openShadowRun(runId: string): Promise<void> {
  try {
    const [runResponse, reviewResponse] = await Promise.all([
      bpiApi.shadowRun(runId),
      bpiApi.shadowRunReviews(runId),
    ]);
    const run = runResponse.data;
    state.selectedShadowRun = run;
    state.shadowRunReviews = reviewResponse.data;
    state.shadowRuns = state.shadowRuns.map((item) => item.id === run.id ? run : item);
    const readinessChecks: Array<[string, boolean]> = [
      ['规则版本已发布', run.readiness.rulePublished], ['规则版本当前激活', run.readiness.ruleActive],
      ['Kafka 发布已确认', run.readiness.publicationConfirmed], ['Flink 控制面已应用', run.readiness.applicationApplied],
      ['流式评估器已就绪', run.readiness.runtimeReady], ['拓扑版本已发布', run.readiness.topologyPublished],
      ['拓扑固定同一目录快照', run.readiness.topologySnapshotPinned], ['点位目录仍为当前版本', run.readiness.pointCatalogCurrent],
      ['全部固定点位运行就绪', run.readiness.pointCatalogReady],
    ];
    const readinessHtml = readinessChecks.map(([label, passed]) => `<li><span class="evidence-state evidence-state--${passed ? 'ok' : 'bad'}"></span><span>${label}</span><b>${passed ? '通过' : '阻断'}</b></li>`).join('');
    const metricChecks: Array<[string, string, boolean]> = [
      ['观察周期', `${formatObservedDays(run.metrics.observedDurationSeconds)} / ${run.minimumDurationDays} 天`, run.metrics.durationGatePassed],
      ['人工复核批次', `${run.metrics.reviewedBatchCount} / ${run.minimumReviewedBatches}`, run.metrics.reviewCountGatePassed],
      ['边界一致率', `${number(run.metrics.boundaryAgreement * 100, 2)}% / ${number(run.minimumBoundaryAgreement * 100, 2)}%`, run.metrics.boundaryAgreementGatePassed],
      ['累计数量偏差', `${number(run.metrics.cumulativeQuantityDeviationPercent, 3)}% / ±${number(run.quantityTolerancePercent, 3)}%`, run.metrics.quantityGatePassed],
      ['严重数据质量事件', `${run.metrics.unresolvedCriticalIncidentCount} 个未解决`, run.metrics.dataQualityGatePassed],
    ];
    const metricHtml = metricChecks.map(([label, value, passed]) => `<div class="${passed ? 'is-pass' : 'is-fail'}"><span>${label}</span><b>${value}</b><small>${passed ? 'PASS' : 'BLOCKED'}</small></div>`).join('');
    const blockers = run.blockers.map((code) => `<li><i data-lucide="circle-alert"></i><div><strong>${escapeHtml(shadowRunBlockerLabel(code))}</strong><code>${escapeHtml(code)}</code></div></li>`).join('');
    const reviews = state.shadowRunReviews.map((review) => `<tr><td><strong>${escapeHtml(review.batchNo)}</strong><small>#${review.reviewSequence}</small></td><td>${formatTime(review.reviewedAt)}</td><td>${review.startDeviationSeconds}s / ${review.endDeviationSeconds}s</td><td>${review.startBoundaryAccepted && review.endBoundaryAccepted ? statusChip('PASS') : statusChip('FAIL')}</td><td>${number(review.automaticQuantity, 3)} / ${number(review.referenceQuantity, 3)} ${escapeHtml(review.quantityUnit)}</td><td>${number(review.quantityDeviationPercent, 3)}%</td><td>${review.quantityWithinTolerance ? statusChip('PASS') : statusChip('FAIL')}</td><td>${escapeHtml(review.reviewedBy)}</td></tr>`).join('');
    let actions = '';
    if (run.state === 'DRAFT') actions = `<button class="button button--danger" id="cancel-shadow-run">取消任务</button><button class="button button--primary" id="start-shadow-run" ${run.readiness.ready ? '' : 'disabled'}>启动影子运行</button>`;
    if (run.state === 'RUNNING') actions = `<button class="button button--danger" id="cancel-shadow-run">取消任务</button><button class="button button--secondary" id="review-shadow-batch">复核批次</button><button class="button button--primary" id="complete-shadow-run" ${run.metrics.durationGatePassed && run.metrics.reviewCountGatePassed ? '' : 'disabled'}>结束观察并评估</button>`;
    if (run.state === 'EVALUATING') actions = `<button class="button button--danger" id="reject-shadow-run">管理员驳回</button><button class="button button--primary" id="approve-shadow-run" ${run.readyForApproval ? '' : 'disabled'}>独立批准验收</button>`;
    openDrawer(`
      <header><div><span>影子运行验收</span><h2>${escapeHtml(run.name)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header>
      <div class="batch-state-band"><div>${statusChip(run.state)}<span class="shadow-label">SHADOW ONLY</span></div><span>revision ${run.revision}</span></div>
      <div class="drawer-section facts-grid"><div><span>验收编码</span><b>${escapeHtml(run.runCode)}</b></div><div><span>工厂 / 产线</span><b>${escapeHtml(run.plantId)} / ${escapeHtml(run.lineId)}</b></div><div><span>固定规则</span><b>${escapeHtml(run.ruleVersion)}</b></div><div><span>固定拓扑</span><b>${escapeHtml(run.topologyVersion)}</b></div><div><span>点位目录快照</span><b class="mono-value">${escapeHtml(run.pointCatalogSnapshotId)}</b></div><div><span>创建人 / 时间</span><b>${escapeHtml(run.createdBy)} · ${formatTime(run.createdAt)}</b></div></div>
      <div class="drawer-section"><div class="section-title"><h3>固定运行版本准入</h3>${run.readiness.ready ? statusChip('READY') : statusChip('BLOCKED')}</div><ul class="shadow-readiness-list">${readinessHtml}</ul></div>
      <div class="drawer-section"><div class="section-title"><h3>验收指标</h3>${run.readyForApproval ? statusChip('READY') : '<span>全部通过后才能批准</span>'}</div><div class="shadow-metric-grid">${metricHtml}</div><div class="facts-grid shadow-metric-detail"><div><span>数量样本</span><b>${run.metrics.quantitySampleCount}</b></div><div><span>自动 / 参考累计</span><b>${number(run.metrics.automaticQuantityTotal, 3)} / ${number(run.metrics.referenceQuantityTotal, 3)} ${escapeHtml(run.metrics.quantityUnit || '')}</b></div><div><span>单批平均偏差</span><b>${number(run.metrics.meanQuantityDeviationPercent, 3)}%</b></div><div><span>单批最大偏差</span><b>${number(run.metrics.maximumQuantityDeviationPercent, 3)}%</b></div></div></div>
      ${blockers ? `<div class="drawer-section shadow-blockers"><div class="section-title"><h3>当前阻断</h3><span>${run.blockers.length} 项</span></div><ul>${blockers}</ul></div>` : ''}
      <div class="drawer-section"><div class="section-title"><h3>批次复核记录</h3><span>${state.shadowRunReviews.length} 个当前样本</span></div>${reviews ? `<div class="table-frame shadow-review-frame"><table class="shadow-review-table"><thead><tr><th>批次</th><th>复核时间</th><th>起止偏差</th><th>边界</th><th>自动 / 参考数量</th><th>偏差</th><th>数量</th><th>复核人</th></tr></thead><tbody>${reviews}</tbody></table></div>` : '<div class="simulation-empty">尚未提交人工批次复核</div>'}</div>
      <div class="drawer-section"><div class="section-title"><h3>审批责任</h3><span>双人控制</span></div><p>批准人必须是不同于创建人的 BPI 管理员；系统重新计算全部指标和数据质量门禁，不接受前端提交的“通过”结论。</p>${run.decidedBy ? `<div class="facts-grid"><div><span>决定人</span><b>${escapeHtml(run.decidedBy)}</b></div><div><span>决定时间</span><b>${formatTime(run.decidedAt)}</b></div><div><span>决定依据</span><b>${escapeHtml(run.decisionReason || '-')}</b></div></div>` : ''}</div>
      <footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button>${actions}</footer>`);
    document.querySelector('#start-shadow-run')?.addEventListener('click', () => openShadowRunCommandDialog('start'));
    document.querySelector('#complete-shadow-run')?.addEventListener('click', () => openShadowRunCommandDialog('complete'));
    document.querySelector('#approve-shadow-run')?.addEventListener('click', () => openShadowRunCommandDialog('approve'));
    document.querySelector('#reject-shadow-run')?.addEventListener('click', () => openShadowRunCommandDialog('reject'));
    document.querySelector('#cancel-shadow-run')?.addEventListener('click', () => openShadowRunCommandDialog('cancel'));
    document.querySelector('#review-shadow-batch')?.addEventListener('click', openShadowReviewDialog);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  }
}

function openShadowRunCreateDialog(): void {
  const published = state.rules.filter((rule) => rule.state === 'PUBLISHED');
  if (!published.length) {
    showToast('当前工厂没有可固定的已发布规则版本', true);
    return;
  }
  const now = new Date();
  const stamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}-${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}${String(now.getSeconds()).padStart(2, '0')}`;
  const firstLine = state.shadowRunLineId || published[0]!.lineId;
  document.querySelector<HTMLInputElement>('#shadow-run-code')!.value = `SHADOW-${stamp}`;
  document.querySelector<HTMLInputElement>('#shadow-run-name')!.value = `${firstLine} 影子运行验收`;
  document.querySelector<HTMLInputElement>('#shadow-run-line')!.value = firstLine;
  document.querySelector<HTMLTextAreaElement>('#shadow-run-reason')!.value = '';
  updateShadowRunRuleOptions();
  document.querySelector<HTMLInputElement>('#shadow-run-line')!.oninput = updateShadowRunRuleOptions;
  document.querySelector<HTMLDialogElement>('#shadow-run-dialog')!.showModal();
}

function updateShadowRunRuleOptions(): void {
  const lineId = document.querySelector<HTMLInputElement>('#shadow-run-line')?.value.trim() || '';
  const rules = state.rules.filter((rule) => rule.state === 'PUBLISHED' && (!lineId || rule.lineId === lineId));
  const select = document.querySelector<HTMLSelectElement>('#shadow-run-rule');
  if (!select) return;
  select.innerHTML = rules.map((rule) => `<option value="${escapeHtml(rule.id)}">${escapeHtml(rule.code)}@${escapeHtml(rule.version)} · ${escapeHtml(rule.runtimeReadinessStatus)}</option>`).join('');
}

async function handleShadowRunCreate(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  const command: ShadowRunCreateCommand = {
    runCode: document.querySelector<HTMLInputElement>('#shadow-run-code')!.value.trim(),
    name: document.querySelector<HTMLInputElement>('#shadow-run-name')!.value.trim(),
    plantId: state.plantId,
    lineId: document.querySelector<HTMLInputElement>('#shadow-run-line')!.value.trim(),
    ruleVersionId: document.querySelector<HTMLSelectElement>('#shadow-run-rule')!.value,
    minimumDurationDays: Number(document.querySelector<HTMLInputElement>('#shadow-run-duration')!.value),
    minimumReviewedBatches: Number(document.querySelector<HTMLInputElement>('#shadow-run-batches')!.value),
    boundaryToleranceSeconds: Number(document.querySelector<HTMLInputElement>('#shadow-run-boundary-tolerance')!.value),
    minimumBoundaryAgreement: Number(document.querySelector<HTMLInputElement>('#shadow-run-boundary-agreement')!.value),
    quantityTolerancePercent: Number(document.querySelector<HTMLInputElement>('#shadow-run-quantity-tolerance')!.value),
    reason: document.querySelector<HTMLTextAreaElement>('#shadow-run-reason')!.value.trim(),
  };
  if (!command.ruleVersionId || command.reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#shadow-run-submit')!;
  button.disabled = true;
  button.textContent = '创建中...';
  try {
    const response = await bpiApi.createShadowRun(command, commandId());
    document.querySelector<HTMLDialogElement>('#shadow-run-dialog')!.close();
    showToast(`影子运行 ${response.data.runCode} 已创建并固定运行版本`);
    await loadView(true);
    await openShadowRun(response.data.id);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = '创建验收任务';
  }
}

function toLocalDateTimeValue(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 19);
}

function eligibleShadowReviewBatches(run: ShadowRun): Batch[] {
  const reviewed = new Set(state.shadowRunReviews.map((item) => item.batchId));
  return state.batches
    .filter((batch) => batch.shadow && batch.state === 'CLOSED_RAW' && batch.lineId === run.lineId
      && batch.ruleVersion === run.ruleVersion && batch.topologyVersion === run.topologyVersion
      && Boolean(batch.endTime) && (!run.startedAt || Date.parse(batch.startTime) >= Date.parse(run.startedAt)))
    .sort((left, right) => Number(reviewed.has(left.id)) - Number(reviewed.has(right.id)) || Date.parse(right.startTime) - Date.parse(left.startTime));
}

function openShadowReviewDialog(): void {
  const run = state.selectedShadowRun;
  if (!run) return;
  const batches = eligibleShadowReviewBatches(run);
  if (!batches.length) {
    showToast('当前没有与固定规则、拓扑和运行窗口一致的 CLOSED_RAW 影子批次', true);
    return;
  }
  const reviewed = new Set(state.shadowRunReviews.map((item) => item.batchId));
  document.querySelector<HTMLSelectElement>('#shadow-review-batch')!.innerHTML = batches.map((batch) => `<option value="${escapeHtml(batch.id)}">${escapeHtml(batch.batchNo)}${reviewed.has(batch.id) ? ' · 已复核，可修订' : ''}</option>`).join('');
  document.querySelector<HTMLTextAreaElement>('#shadow-review-reason')!.value = '';
  applyShadowReviewBatch();
  document.querySelector<HTMLDialogElement>('#shadow-review-dialog')!.showModal();
}

function applyShadowReviewBatch(): void {
  const id = document.querySelector<HTMLSelectElement>('#shadow-review-batch')?.value;
  const batch = state.batches.find((item) => item.id === id);
  if (!batch || !batch.endTime) return;
  document.querySelector<HTMLInputElement>('#shadow-review-start')!.value = toLocalDateTimeValue(batch.startTime);
  document.querySelector<HTMLInputElement>('#shadow-review-end')!.value = toLocalDateTimeValue(batch.endTime);
  document.querySelector<HTMLInputElement>('#shadow-review-quantity')!.value = String(batch.quantity);
  document.querySelector<HTMLInputElement>('#shadow-review-unit')!.value = batch.quantityUnit;
}

async function handleShadowRunBatchReview(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  const run = state.selectedShadowRun;
  if (!run) return;
  const command: ShadowRunBatchReviewCommand = {
    batchId: document.querySelector<HTMLSelectElement>('#shadow-review-batch')!.value,
    manualStartTime: new Date(document.querySelector<HTMLInputElement>('#shadow-review-start')!.value).toISOString(),
    manualEndTime: new Date(document.querySelector<HTMLInputElement>('#shadow-review-end')!.value).toISOString(),
    referenceQuantity: Number(document.querySelector<HTMLInputElement>('#shadow-review-quantity')!.value),
    quantityUnit: document.querySelector<HTMLInputElement>('#shadow-review-unit')!.value.trim(),
    reason: document.querySelector<HTMLTextAreaElement>('#shadow-review-reason')!.value.trim(),
  };
  if (Date.parse(command.manualEndTime) <= Date.parse(command.manualStartTime)) {
    showToast('人工结束时间必须晚于人工开始时间', true);
    return;
  }
  const button = document.querySelector<HTMLButtonElement>('#shadow-review-submit')!;
  button.disabled = true;
  button.textContent = '复核中...';
  try {
    const response = await bpiApi.reviewShadowRunBatch(run, command, commandId());
    state.selectedShadowRun = response.data.run;
    document.querySelector<HTMLDialogElement>('#shadow-review-dialog')!.close();
    showToast(`批次 ${response.data.review.batchNo} 已复核，边界一致率 ${number(response.data.run.metrics.boundaryAgreement * 100, 2)}%`);
    await loadView(true);
    await openShadowRun(run.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`影子运行已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openShadowRun(run.id);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = '提交批次复核';
  }
}

function openShadowRunCommandDialog(command: 'start' | 'complete' | 'approve' | 'reject' | 'cancel'): void {
  const run = state.selectedShadowRun;
  if (!run) return;
  const labels = {
    start: ['启动影子运行', '启动依据', '确认启动', 'DRAFT → RUNNING'],
    complete: ['结束观察并进入评估', '评估依据', '确认进入评估', 'RUNNING → EVALUATING'],
    approve: ['独立批准影子验收', '批准依据', '批准验收', 'EVALUATING → APPROVED'],
    reject: ['驳回影子验收', '驳回原因', '确认驳回', 'EVALUATING → REJECTED'],
    cancel: ['取消影子运行', '取消原因', '确认取消', `${run.state} → CANCELLED`],
  } as const;
  const [title, reasonLabel, buttonLabel, transition] = labels[command];
  state.shadowRunCommand = command;
  state.candidateCommand = null;
  state.batchCommand = null;
  state.ruleCommand = null;
  state.topologyCommand = null;
  state.calibrationCommand = null;
  state.dataQualityCommand = null;
  setCommandAssigneeVisible(false);
  document.querySelector('#command-kicker')!.textContent = '影子运行验收';
  document.querySelector('#command-title')!.textContent = title;
  document.querySelector('#command-reason-label')!.textContent = reasonLabel;
  document.querySelector('#command-summary')!.innerHTML = `<div><span>验收任务</span><b>${escapeHtml(run.runCode)}</b></div><div><span>产线</span><b>${escapeHtml(run.lineId)}</b></div><div><span>状态变化</span><b>${transition}</b></div><div><span>版本</span><b>r${run.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = command === 'approve' ? '填写独立复核结论、指标和风险接受依据' : '填写可审计的现场或治理依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = `button ${['reject', 'cancel'].includes(command) ? 'button--danger' : 'button--primary'}`;
  button.textContent = buttonLabel;
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

async function handleShadowRunCommand(): Promise<void> {
  const run = state.selectedShadowRun;
  const command = state.shadowRunCommand;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  if (!run || !command || reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = '提交中...';
  try {
    const handlers = {
      start: bpiApi.startShadowRun,
      complete: bpiApi.completeShadowRun,
      approve: bpiApi.approveShadowRun,
      reject: bpiApi.rejectShadowRun,
      cancel: bpiApi.cancelShadowRun,
    };
    const response = await handlers[command](run, reason, commandId());
    state.selectedShadowRun = response.data;
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    const messages = { start: '影子运行已启动', complete: '观察期已结束，等待独立审批', approve: '影子验收已批准', reject: '影子验收已驳回', cancel: '影子运行已取消' };
    showToast(messages[command]);
    await loadView(true);
    await openShadowRun(run.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`影子运行已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openShadowRun(run.id);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
  }
}

function renderDataQuality(): void {
  const content = document.querySelector<HTMLElement>('#content')!;
  const summary = state.dataQualitySummary || {
    open: 0, acknowledged: 0, resolved: 0, critical: 0, affectedBatches: 0, issueCounts: {},
  };
  const categoryMetrics = [
    ['必需信号不可用', dataQualityCategoryCount(['REQUIRED', 'CATALOG_BINDING', 'RULE_BINDING', 'PROPERTY_NOT'])],
    ['设备时钟漂移', dataQualityCategoryCount(['CLOCK', 'TIME_DRIFT'])],
    ['计量单位异常', dataQualityCategoryCount(['UNIT'])],
    ['事件序列缺口', dataQualityCategoryCount(['SEQUENCE', 'GAP'])],
    ['共享仪表未分摊', dataQualityCategoryCount(['SHARED', 'ALLOCAT'])],
    ['缓冲与反压告警', dataQualityCategoryCount(['BUFFER', 'BACKPRESSURE'])],
  ];
  const rows = state.dataQualityIncidents.map((incident) => `
    <tr data-data-quality-id="${escapeHtml(incident.id)}" tabindex="0">
      <td><div class="status-stack">${statusChip(incident.severity)}${statusChip(incident.state)}</div></td>
      <td><strong>${escapeHtml(dataQualityIssueLabel(incident.issueCode))}</strong><small>${escapeHtml(incident.issueCode)}</small></td>
      <td><strong>${escapeHtml(incident.deviceId || incident.source)}</strong><small>${escapeHtml(incident.propertyId || incident.source)}</small></td>
      <td><strong>${formatDuration(incident.firstSeen, incident.lastSeen)}</strong><small>${formatTime(incident.firstSeen)} → ${formatTime(incident.lastSeen)}</small></td>
      <td class="metric"><b>${incident.eventCount}</b><small>事件</small></td>
      <td><strong>${incident.affectedBatchCount} 批次</strong><small>${incident.affectedRules.length} 规则 · ${incident.affectedLines.length} 产线</small></td>
      <td><strong>${escapeHtml(incident.assignee || '-')}</strong><small>${escapeHtml(incident.lineId)}</small></td>
      <td><strong>${formatTime(incident.lastSeen)}</strong><small>r${incident.revision}</small></td>
      <td><i data-lucide="chevron-right"></i></td>
    </tr>`).join('');
  const stateOptions: Array<[string, string]> = [['', '全部'], ['OPEN', '待确认'], ['ACKNOWLEDGED', '处理中'], ['RESOLVED', '已解决']];
  content.innerHTML = `
    <div class="data-quality-state-summary" aria-label="事件状态汇总">
      <div><span>待确认</span><b>${summary.open}</b></div><div><span>处理中</span><b>${summary.acknowledged}</b></div>
      <div><span>已解决</span><b>${summary.resolved}</b></div><div class="is-critical"><span>严重事件</span><b>${summary.critical}</b></div>
      <div><span>受影响批次</span><b>${summary.affectedBatches}</b></div>
    </div>
    <div class="data-quality-category-strip" aria-label="质量问题分类">
      ${categoryMetrics.map(([label, value]) => `<div><span>${label}</span><b>${value}</b></div>`).join('')}
    </div>
    <div class="toolbar data-quality-toolbar">
      <div class="segmented" role="group" aria-label="事件状态">${stateOptions.map(([value, label]) => `<button data-data-quality-state="${value}" class="${state.dataQualityState === value ? 'is-selected' : ''}">${label}</button>`).join('')}</div>
      <div class="toolbar-actions">
        <label class="line-field"><span>产线</span><input id="data-quality-line" value="${escapeHtml(state.dataQualityLineId)}" placeholder="全部产线" /></label>
        <label class="search-field data-quality-search"><i data-lucide="search"></i><input id="data-quality-search" value="${escapeHtml(state.dataQualitySearch)}" placeholder="问题、设备、属性、责任人" /></label>
        <button id="apply-data-quality-filter" class="icon-text-button data-quality-filter-button" title="查询" aria-label="查询"><i data-lucide="search"></i><span>查询</span></button>
      </div>
    </div>
    ${rows ? `<div class="table-frame"><table class="data-quality-table"><thead><tr><th>级别 / 状态</th><th>质量问题</th><th>来源点位</th><th>持续时间</th><th>事件数</th><th>业务影响</th><th>责任人 / 产线</th><th>最后发生</th><th></th></tr></thead><tbody>${rows}</tbody></table>${state.dataQualityNextCursor ? `<div class="data-quality-pagination"><span>${formatTime(state.dataQualitySnapshotAt)} 快照 · 已加载 ${state.dataQualityIncidents.length} 条</span><button id="load-more-data-quality" class="button button--secondary" ${state.loadingMoreDataQuality ? 'disabled' : ''}><i data-lucide="chevrons-down"></i>${state.loadingMoreDataQuality ? '加载中' : '加载更多'}</button></div>` : ''}</div>` : `<div class="empty-state"><i data-lucide="shield-check"></i><strong>当前筛选范围没有数据质量事件</strong><span>事件由 Kafka 数据质量主题聚合，原始事实不会因处置而删除。</span></div>`}`;

  content.querySelectorAll<HTMLElement>('[data-data-quality-id]').forEach((row) => row.addEventListener('click', () => void openDataQualityIncident(String(row.dataset.dataQualityId))));
  content.querySelectorAll<HTMLButtonElement>('[data-data-quality-state]').forEach((button) => button.addEventListener('click', () => {
    state.dataQualityState = (button.dataset.dataQualityState || '') as DataQualityIncidentState | '';
    localStorage.setItem('bpi.dataQualityState', state.dataQualityState);
    void loadView();
  }));
  const applyFilters = () => {
    state.dataQualityLineId = document.querySelector<HTMLInputElement>('#data-quality-line')!.value.trim();
    state.dataQualitySearch = document.querySelector<HTMLInputElement>('#data-quality-search')!.value.trim();
    localStorage.setItem('bpi.dataQualityLineId', state.dataQualityLineId);
    void loadView();
  };
  content.querySelector('#apply-data-quality-filter')?.addEventListener('click', applyFilters);
  content.querySelectorAll<HTMLInputElement>('#data-quality-line, #data-quality-search').forEach((input) => input.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') applyFilters();
  }));
  content.querySelector('#load-more-data-quality')?.addEventListener('click', () => void loadMoreDataQuality());
}

async function loadMoreDataQuality(): Promise<void> {
  if (state.loadingMoreDataQuality || !state.dataQualityNextCursor) return;
  state.loadingMoreDataQuality = true;
  renderDataQuality();
  try {
    const response = await bpiApi.dataQualityIncidents(state.plantId, {
      lineId: normalizedDataQualityLine() || undefined,
      state: state.dataQualityState || undefined,
      search: normalizedDataQualitySearch() || undefined,
      cursor: state.dataQualityNextCursor,
      limit: DATA_QUALITY_PAGE_SIZE,
    });
    const known = new Set(state.dataQualityIncidents.map((item) => item.id));
    state.dataQualityIncidents.push(...response.data.filter((item) => !known.has(item.id)));
    state.dataQualityNextCursor = response.meta.nextCursor || null;
    state.dataQualitySnapshotAt = response.meta.snapshotAt;
    state.meta = response.meta;
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    state.loadingMoreDataQuality = false;
    renderDataQuality();
    refreshIcons();
  }
}

async function openDataQualityIncident(incidentId: string): Promise<void> {
  try {
    const detail = (await bpiApi.dataQualityIncident(incidentId)).data;
    state.selectedDataQualityDetail = detail;
    state.selectedDataQualityIncident = detail.incident;
    const incident = detail.incident;
    const recommendations = detail.recommendedActions.map((action) => `<li><i data-lucide="check-circle-2"></i><span>${escapeHtml(action)}</span></li>`).join('');
    const events = detail.events.map((item) => `<li><span class="evidence-state evidence-state--${statusTone(item.severity) === 'danger' ? 'bad' : 'ok'}"></span><div><strong>${escapeHtml(item.detail)}</strong><small>${formatTime(item.detectedAt)} · 接收 ${formatTime(item.receivedAt)}</small><code>${escapeHtml(item.sourceEventId || item.eventId)}</code></div><b>${escapeHtml(item.severity)}</b></li>`).join('');
    const lifecycle = detail.lifecycle.map((item) => `<li><i></i><div><strong>${escapeHtml(item.action)}</strong><span>${escapeHtml(item.reason || '-')}</span><small>${formatTime(item.at)} · ${escapeHtml(item.actorId)}${item.assignee ? ` · 分派 ${escapeHtml(item.assignee)}` : ''}</small></div></li>`).join('');
    const affectedRules = incident.affectedRules.length ? incident.affectedRules.map((rule) => `<code>${escapeHtml(rule)}</code>`).join('') : '<span>无</span>';
    const affectedBatches = incident.affectedBatches.length ? incident.affectedBatches.map((batch) => `<code>${escapeHtml(batch)}</code>`).join('') : '<span>无已识别批次</span>';
    const actions = incident.state === 'OPEN'
      ? '<button class="button button--primary" id="open-data-quality-acknowledge">确认并分派</button>'
      : incident.state === 'ACKNOWLEDGED'
        ? '<button class="button button--secondary" id="open-data-quality-reassign">重新分派</button><button class="button button--primary" id="open-data-quality-resolve">标记已解决</button>'
        : '';
    openDrawer(`
      <header><div><span>数据质量事件</span><h2>${escapeHtml(dataQualityIssueLabel(incident.issueCode))}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header>
      <div class="batch-state-band"><div>${statusChip(incident.severity)}${statusChip(incident.state)}</div><span>revision ${incident.revision}</span></div>
      <div class="drawer-section data-quality-detail"><p class="incident-detail">${escapeHtml(incident.lastDetail)}</p><div class="facts-grid"><div><span>工厂 / 产线</span><b>${escapeHtml(incident.plantId)} / ${escapeHtml(incident.lineId)}</b></div><div><span>来源</span><b>${escapeHtml(incident.source)}</b></div><div><span>设备 / 属性</span><b>${escapeHtml(incident.deviceId || '-')} / ${escapeHtml(incident.propertyId || '-')}</b></div><div><span>持续时间 / 事件</span><b>${formatDuration(incident.firstSeen, incident.lastSeen)} / ${incident.eventCount}</b></div><div><span>责任人</span><b>${escapeHtml(incident.assignee || '-')}</b></div><div><span>最后发生</span><b>${formatTime(incident.lastSeen)}</b></div></div></div>
      <div class="drawer-section"><div class="section-title"><h3>业务影响</h3><span>${incident.affectedBatchCount} 个批次</span></div><div class="impact-groups"><div><span>规则版本</span>${affectedRules}</div><div><span>批次</span>${affectedBatches}</div></div></div>
      <div class="drawer-section"><div class="section-title"><h3>建议处置</h3><span>${detail.recommendedActions.length} 项</span></div><ul class="check-list data-quality-actions">${recommendations || '<li>暂无自动建议</li>'}</ul></div>
      <div class="drawer-section"><div class="section-title"><h3>原始事件</h3><span>${detail.events.length} 条，最多显示 100 条</span></div><ul class="evidence-list data-quality-event-list">${events || '<li class="evidence-empty">暂无事件证据</li>'}</ul></div>
      <div class="drawer-section"><div class="section-title"><h3>处置时间线</h3><span>审计记录不可变</span></div><ol class="timeline">${lifecycle || '<li><i></i><div><strong>尚无处置记录</strong></div></li>'}</ol></div>
      <footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button>${actions}</footer>`);
    document.querySelector('#open-data-quality-acknowledge')?.addEventListener('click', () => openDataQualityCommandDialog('acknowledge'));
    document.querySelector('#open-data-quality-reassign')?.addEventListener('click', () => openDataQualityCommandDialog('acknowledge'));
    document.querySelector('#open-data-quality-resolve')?.addEventListener('click', () => openDataQualityCommandDialog('resolve'));
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  }
}

function openDataQualityCommandDialog(command: 'acknowledge' | 'resolve'): void {
  const incident = state.selectedDataQualityIncident;
  if (!incident) return;
  const acknowledge = command === 'acknowledge';
  const reassign = acknowledge && incident.state === 'ACKNOWLEDGED';
  state.dataQualityCommand = command;
  state.candidateCommand = null;
  state.batchCommand = null;
  state.ruleCommand = null;
  state.topologyCommand = null;
  state.calibrationCommand = null;
  document.querySelector('#command-kicker')!.textContent = '数据质量处置';
  document.querySelector('#command-title')!.textContent = acknowledge ? (reassign ? '重新分派事件' : '确认并分派事件') : '解决数据质量事件';
  document.querySelector('#command-reason-label')!.textContent = acknowledge ? (reassign ? '重新分派依据' : '确认依据') : '解决依据';
  document.querySelector('#command-summary')!.innerHTML = `<div><span>质量问题</span><b>${escapeHtml(dataQualityIssueLabel(incident.issueCode))}</b></div><div><span>产线</span><b>${escapeHtml(incident.lineId)}</b></div><div><span>状态变化</span><b>${acknowledge ? `${incident.state} → ACKNOWLEDGED` : 'ACKNOWLEDGED → RESOLVED'}</b></div><div><span>版本</span><b>r${incident.revision}</b></div>`;
  setCommandAssigneeVisible(acknowledge, incident.assignee || '');
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = acknowledge ? '填写问题判断、业务影响和责任分派依据' : '填写根因、修复动作和复核证据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = 'button button--primary';
  button.textContent = acknowledge ? (reassign ? '确认重新分派' : '确认并分派') : '确认已解决';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  (acknowledge ? document.querySelector<HTMLInputElement>('#command-assignee') : reason)?.focus();
}

async function handleDataQualityCommand(): Promise<void> {
  const incident = state.selectedDataQualityIncident;
  const command = state.dataQualityCommand;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  const assignee = document.querySelector<HTMLInputElement>('#command-assignee')!.value.trim();
  if (!incident || !command || reason.length < 3 || (command === 'acknowledge' && !assignee)) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = command === 'acknowledge' ? '分派中...' : '解决中...';
  try {
    const response = command === 'acknowledge'
      ? await bpiApi.acknowledgeDataQuality(incident, assignee, reason, commandId())
      : await bpiApi.resolveDataQuality(incident, reason, commandId());
    state.selectedDataQualityIncident = response.data;
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    showToast(command === 'acknowledge'
      ? `事件已确认并分派给 ${response.data.assignee}`
      : '事件已解决，原始数据和处置审计已保留');
    await loadView(true);
    await openDataQualityIncident(response.data.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`事件状态已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openDataQualityIncident(incident.id);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = command === 'acknowledge' ? '确认并分派' : '确认已解决';
  }
}

function ruleConditions(rule: RuleVersion): Array<Record<string, unknown>> {
  const conditions = rule.ast.conditions;
  return Array.isArray(conditions) ? conditions.filter((item): item is Record<string, unknown> => Boolean(item) && typeof item === 'object') : [];
}

function pointIssueLabel(code: string): string {
  const normalized = code.startsWith('POINT_') ? code.slice('POINT_'.length) : code;
  const labels: Record<string, string> = {
    DEVICE_NOT_REGISTERED: '设备未注册',
    DEVICE_NOT_ACTIVE: '设备未激活',
    PROPERTY_NOT_AVAILABLE: '设备属性不可用',
    UNIT_MISSING: '单位缺失',
    UNIT_MISMATCH: '单位与规则要求不一致',
    CALIBRATION_NOT_VERIFIED: '校准证据未批准或已失效',
    SOURCE_SEQUENCE_DISABLED: '来源序列声明不完整',
    SOURCE_SEQUENCE_EVIDENCE_MISSING: '来源序列运行证据缺失',
    SOURCE_SEQUENCE_EVIDENCE_EXPIRED: '来源序列运行证据已过期',
    SOURCE_SEQUENCE_EVIDENCE_NOT_QUALIFIED: '来源序列运行证据未通过',
    CATALOG_BINDING_NOT_FOUND: '点位目录中找不到规则绑定',
    CATALOG_SNAPSHOT_MISSING: '当前产线没有点位目录快照',
  };
  return labels[normalized] || code;
}

function sourceSequenceStatus(point: PointCatalogPoint): string {
  const bindingReady = point.sourceSequenceEnabled
    && point.sourceSequenceRequired
    && ['DEVICE', 'GATEWAY'].includes(point.sourceSequenceOrigin || '')
    && /^sha256:[0-9a-f]{64}$/.test(point.sourceSequenceBindingFingerprint || '');
  if (!bindingReady) return 'DISABLED';
  return point.sourceSequenceEvidenceStatus || 'MISSING';
}

function sourceSequenceOriginLabel(origin?: PointCatalogPoint['sourceSequenceOrigin']): string {
  if (origin === 'DEVICE') return '设备原生序列';
  if (origin === 'GATEWAY') return '网关持久序列';
  return '未声明权威来源';
}

function openPointSourceSequenceEvidence(point: PointCatalogPoint): void {
  const status = sourceSequenceStatus(point);
  const sequenceRange = point.sourceSequenceFirst === null || point.sourceSequenceFirst === undefined
    ? '-'
    : `${point.sourceSequenceFirst} - ${point.sourceSequenceLast ?? '-'}`;
  const evidenceMessage = point.sourceSequenceQualified
    ? '当前证据与点位绑定指纹一致、晚于目录快照且仍在有效期内，可参与运行准入。'
    : '该点位没有可用于运行准入的新鲜证据。系统保持失败关闭，不会仅凭目录中的启用声明放行。';
  openDrawer(`<header><div><span>数据准入</span><h2>${escapeHtml(point.pointName || point.propertyId)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header>
    <div class="batch-state-band"><div>${statusChip(status)}${statusChip(point.sourceSequenceQualified ? 'READY' : 'BLOCKED')}</div><span>${escapeHtml(sourceSequenceOriginLabel(point.sourceSequenceOrigin))}</span></div>
    <div class="drawer-section"><h3>绑定声明</h3><div class="facts-grid"><div><span>产品 / 设备</span><b>${escapeHtml(point.productId)} / ${escapeHtml(point.deviceId)}</b></div><div><span>属性</span><b>${escapeHtml(point.propertyId)}</b></div><div><span>要求来源序列</span><b>${point.sourceSequenceRequired ? '是' : '否'}</b></div><div><span>来源类型</span><b>${escapeHtml(sourceSequenceOriginLabel(point.sourceSequenceOrigin))}</b></div><div><span>绑定指纹</span><b class="mono-value">${escapeHtml(point.sourceSequenceBindingFingerprint || '-')}</b></div><div><span>目录声明</span><b>${point.sourceSequenceEnabled ? '已启用' : '未启用'}</b></div></div></div>
    <div class="drawer-section"><h3>运行证据</h3><div class="facts-grid"><div><span>证据状态</span><b>${escapeHtml(status)}</b></div><div><span>证据修订</span><b>${point.sourceSequenceEvidenceRevision ? `r${point.sourceSequenceEvidenceRevision}` : '-'}</b></div><div><span>来源 epoch</span><b>${escapeHtml(point.sourceSequenceEpoch ?? '-')}</b></div><div><span>序列区间</span><b>${escapeHtml(sequenceRange)}</b></div><div><span>观测数量</span><b>${escapeHtml(point.sourceSequenceObservationCount ?? '-')}</b></div><div><span>首次观测</span><b>${formatTime(point.sourceSequenceFirstObservedAt)}</b></div><div><span>最近观测</span><b>${formatTime(point.sourceSequenceLastObservedAt)}</b></div><div><span>有效截止</span><b>${formatTime(point.sourceSequenceValidUntil)}</b></div><div><span>证据事件</span><b class="mono-value">${escapeHtml(point.sourceSequenceEvidenceEventId || '-')}</b></div></div><p>${escapeHtml(evidenceMessage)}</p></div>
    <div class="drawer-section"><h3>当前准入结论</h3><div class="status-stack">${statusChip(point.ready ? 'READY' : 'BLOCKED')}</div><p>${escapeHtml(point.readinessIssues.map(pointIssueLabel).join('、') || '点位全部准入条件已通过。')}</p></div>
    <footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button></footer>`);
}

function rulePublicationProblemMessage(error: ApiProblem): string {
  const prefix = 'Rule publication requires current READY point catalog bindings:';
  const detail = error.problem.detail.trim();
  if (error.problem.status !== 422 || !detail.startsWith(prefix)) return error.message;

  const codes = detail
    .slice(prefix.length)
    .replace(/\.$/, '')
    .split(',')
    .map((code) => code.trim())
    .filter(Boolean);
  const labels = codes.map(pointIssueLabel);
  const unknownCount = labels.filter((label, index) => label === codes[index]).length;
  const knownLabels = labels.filter((label, index) => label !== codes[index]);
  if (unknownCount > 0) knownLabels.push(`其他点位准入问题 ${unknownCount} 项`);
  const reasons = knownLabels.length > 0 ? `（${knownLabels.join('、')}）` : '';
  return `规则未发布：当前点位未通过运行准入${reasons}。请先在点位目录修复后重新校验。`;
}

function calibrationEffectivenessLabel(status: PointCalibration['effectivenessStatus']): string {
  const labels: Record<PointCalibration['effectivenessStatus'], string> = {
    PENDING: '待独立复核',
    REJECTED: '已驳回',
    REVOKED: '已撤销',
    NOT_YET_EFFECTIVE: '尚未生效',
    EXPIRED: '已过期',
    EFFECTIVE: '当前有效',
  };
  return labels[status];
}

function pointCalibrationSection(): string {
  const rows = state.calibrations.map((calibration) => {
    const lifecycleStatus = calibration.state === calibration.effectivenessStatus
      ? statusChip(calibration.state)
      : `${statusChip(calibration.state)}${statusChip(calibration.effectivenessStatus)}`;
    const actions = calibration.state === 'PENDING'
      ? `<button type="button" class="button button--danger button--compact" data-calibration-action="reject" data-calibration-id="${escapeHtml(calibration.id)}">驳回</button><button type="button" class="button button--primary button--compact" data-calibration-action="approve" data-calibration-id="${escapeHtml(calibration.id)}">批准</button>`
      : calibration.state === 'APPROVED'
        ? `<button type="button" class="button button--danger button--compact" data-calibration-action="revoke" data-calibration-id="${escapeHtml(calibration.id)}">撤销</button>`
        : '-';
    const latestActor = calibration.state === 'REVOKED'
      ? calibration.revokedBy
      : calibration.decidedBy;
    const latestActionAt = calibration.state === 'REVOKED'
      ? calibration.revokedAt
      : calibration.decidedAt;
    return `<tr data-calibration-row="${escapeHtml(calibration.id)}">
      <td><strong>${escapeHtml(calibration.productId)} / ${escapeHtml(calibration.deviceId)}</strong><small>${escapeHtml(calibration.propertyId)}</small></td>
      <td><strong>${escapeHtml(calibration.calibrationVersion)}</strong><small>r${calibration.revision}</small></td>
      <td><strong>${escapeHtml(calibration.certificateReference)}</strong><small class="mono-value">${escapeHtml(calibration.certificateChecksum)}</small></td>
      <td><strong>${formatTime(calibration.validFrom)}</strong><small>至 ${formatTime(calibration.validUntil)}</small></td>
      <td><div class="status-stack">${lifecycleStatus}</div><small>${escapeHtml(calibrationEffectivenessLabel(calibration.effectivenessStatus))}</small></td>
      <td><strong>${escapeHtml(calibration.submittedBy)}</strong><small>${formatTime(calibration.submittedAt)}</small></td>
      <td><strong>${escapeHtml(latestActor || '-')}</strong><small>${formatTime(latestActionAt)}</small></td>
      <td><div class="table-actions">${actions}</div></td>
    </tr>`;
  }).join('');
  return `<section class="calibration-workbench">
    <div class="section-bar"><div><i data-lucide="shield-check"></i><strong>MES 校准证据</strong></div><span>已加载 ${state.calibrations.length} 条</span></div>
    ${rows
      ? `<div class="table-frame table-frame--flush"><table class="calibration-table"><thead><tr><th>点位</th><th>校准版本</th><th>证书 / 校验和</th><th>有效期</th><th>状态</th><th>提交人</th><th>最近处置人</th><th>操作</th></tr></thead><tbody>${rows}</tbody></table></div>`
      : '<div class="calibration-empty">当前产线没有 MES 批准的校准证据。来源系统的 VERIFIED 声明不会自动放行点位。</div>'}
    ${state.calibrationNextCursor
      ? `<div class="calibration-pagination"><span>${formatTime(state.calibrationSnapshotAt)} 快照</span><button id="load-more-calibrations" type="button" class="button button--secondary" ${state.loadingMoreCalibrations ? 'disabled' : ''}><i data-lucide="chevrons-down"></i>${state.loadingMoreCalibrations ? '加载中' : '加载更多'}</button></div>`
      : ''}
  </section>`;
}

function renderPoints(): void {
  const content = document.querySelector<HTMLElement>('#content')!;
  const catalog = state.pointCatalog;
  const toolbar = `<div class="toolbar"><label class="search-field"><i data-lucide="search"></i><input id="point-search" maxlength="128" value="${escapeHtml(state.pointSearch)}" placeholder="产品、设备、属性、证书" /></label><div class="toolbar-actions"><label class="line-field"><span>产线</span><input id="point-line" value="${escapeHtml(state.pointLineId)}" /></label><button id="load-point-line" class="icon-button" title="加载产线" aria-label="加载产线"><i data-lucide="refresh-cw"></i></button><button id="new-point-calibration" class="icon-text-button"><i data-lucide="plus"></i><span>提交校准证据</span></button><button id="import-point-catalog" class="icon-text-button"><i data-lucide="upload"></i><span>导入快照</span></button></div></div>`;
  if (!catalog) {
    content.innerHTML = `${toolbar}<div class="empty-state"><i data-lucide="database"></i><strong>该产线没有点位目录快照</strong><span>${escapeHtml(state.plantId)} / ${escapeHtml(state.pointLineId)}</span></div>${pointCalibrationSection()}`;
    bindPointPageEvents(content);
    return;
  }
  const { snapshot, points } = catalog;
  const rows = points.map((point) => `
    <tr data-point-id="${escapeHtml(point.id)}">
      <td><strong>${escapeHtml(point.pointName || point.propertyId)}</strong><small>${escapeHtml(point.sourcePropertyId ? `${point.sourcePropertyId} → ${point.propertyId}` : point.propertyId)}</small></td>
      <td>${escapeHtml(point.productId)}</td><td>${escapeHtml(point.deviceId)}</td>
      <td>${statusChip(point.deviceState)}<small>${point.registered ? '已注册' : '未注册'}</small></td>
      <td>${point.propertyPresent ? '已发现' : '缺失'}</td><td>${escapeHtml(point.unit || '-')}</td>
      <td>${statusChip(point.sourceCalibrationStatus)}<small>来源系统声明</small></td>
      <td><strong>${escapeHtml(point.calibrationVersion || '-')}</strong><div class="status-stack">${statusChip(point.calibrationStatus)}</div><small>${point.calibrationValidUntil ? `证据有效至 ${formatTime(point.calibrationValidUntil)}` : '无有效证据'}</small></td>
      <td><div class="point-sequence-cell"><div>${statusChip(sourceSequenceStatus(point))}<small>${escapeHtml(sourceSequenceOriginLabel(point.sourceSequenceOrigin))}</small></div><button type="button" class="icon-button" data-point-sequence="${escapeHtml(point.id)}" title="查看来源序列证据" aria-label="查看来源序列证据"><i data-lucide="list-checks"></i></button></div></td>
      <td>${statusChip(point.ready ? 'READY' : 'BLOCKED')}<small>${escapeHtml(point.readinessIssues.map(pointIssueLabel).join('、') || '准入通过')}</small></td>
      <td><button type="button" class="button button--secondary button--compact" data-point-calibrate="${escapeHtml(point.id)}">提交证据</button></td>
    </tr>`).join('');
  content.innerHTML = `${toolbar}
    <div class="point-summary">
      <div><span>来源实例</span><b>${escapeHtml(snapshot.source)} / ${escapeHtml(snapshot.sourceInstance)}</b></div>
      <div><span>来源修订</span><b>${escapeHtml(snapshot.sourceRevision)}</b></div>
      <div><span>目录总点位</span><b>${snapshot.pointCount}</b></div>
      <div><span>就绪点位</span><b>${snapshot.readyPointCount}</b></div>
      <div><span>观测时间</span><b>${formatTime(snapshot.observedAt)}</b></div>
      <div><span>快照校验和</span><b class="mono-value">${escapeHtml(snapshot.checksum)}</b></div>
    </div>
    ${rows ? `<div class="table-frame"><table class="point-table"><thead><tr><th>点位</th><th>产品</th><th>设备</th><th>设备状态</th><th>属性</th><th>单位</th><th>来源声明</th><th>MES 校准证据</th><th>源序列</th><th>准入状态</th><th>操作</th></tr></thead><tbody>${rows}</tbody></table></div>` : `<div class="empty-state"><i data-lucide="database"></i><strong>${normalizedPointSearch() ? '没有匹配的点位' : '快照中没有可用点位'}</strong><span>${escapeHtml(snapshot.sourceRevision)}</span></div>`}
    <div class="point-pagination"><span>${formatTime(state.pointCatalogSnapshotAt)} 快照 · 已加载 ${points.length}${normalizedPointSearch() ? ' 条匹配点位' : ` / ${snapshot.pointCount} 条`}</span>${state.pointCatalogNextCursor ? `<button id="load-more-points" type="button" class="button button--secondary" ${state.loadingMorePointCatalog ? 'disabled' : ''}><i data-lucide="chevrons-down"></i>${state.loadingMorePointCatalog ? '加载中' : '加载更多'}</button>` : ''}</div>
    ${pointCalibrationSection()}`;
  bindPointPageEvents(content);
}

function bindPointPageEvents(content: HTMLElement): void {
  content.querySelector('#import-point-catalog')?.addEventListener('click', openPointCatalogImport);
  content.querySelector('#new-point-calibration')?.addEventListener('click', () => openPointCalibrationSubmit());
  content.querySelectorAll<HTMLButtonElement>('[data-point-calibrate]').forEach((button) => button.addEventListener('click', () => {
    const point = state.pointCatalog?.points.find((item) => item.id === button.dataset.pointCalibrate);
    openPointCalibrationSubmit(point);
  }));
  content.querySelectorAll<HTMLButtonElement>('[data-point-sequence]').forEach((button) => button.addEventListener('click', () => {
    const point = state.pointCatalog?.points.find((item) => item.id === button.dataset.pointSequence);
    if (point) openPointSourceSequenceEvidence(point);
  }));
  content.querySelectorAll<HTMLButtonElement>('[data-calibration-action]').forEach((button) => button.addEventListener('click', () => {
    const calibration = state.calibrations.find((item) => item.id === button.dataset.calibrationId);
    const command = button.dataset.calibrationAction as 'approve' | 'reject' | 'revoke' | undefined;
    if (calibration && command) openPointCalibrationCommand(calibration, command);
  }));
  content.querySelector('#load-more-calibrations')?.addEventListener('click', () => void loadMorePointCalibrations());
  content.querySelector('#load-more-points')?.addEventListener('click', () => void loadMorePointCatalog());
  const load = (): void => {
    const value = content.querySelector<HTMLInputElement>('#point-line')?.value.trim();
    if (!value) return;
    state.pointLineId = value;
    localStorage.setItem('bpi.lineId', value);
    void loadView();
  };
  content.querySelector('#load-point-line')?.addEventListener('click', load);
  content.querySelector<HTMLInputElement>('#point-line')?.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') load();
  });
  content.querySelector<HTMLInputElement>('#point-search')?.addEventListener('input', (event) => {
    state.pointSearch = (event.target as HTMLInputElement).value;
    applyPointSearch(content);
    schedulePointCatalogSearch();
  });
  applyPointSearch(content);
}

function normalizedPointSearch(): string {
  return state.pointSearch.trim();
}

function applyPointCatalogPage(response: { data: PointCatalogView | null; meta: ResponseMeta }): void {
  state.pointCatalog = response.data;
  state.pointCatalogNextCursor = response.meta.nextCursor || null;
  state.pointCatalogSnapshotAt = response.meta.snapshotAt;
  state.meta = response.meta;
}

function schedulePointCatalogSearch(): void {
  if (pointSearchTimer !== null) window.clearTimeout(pointSearchTimer);
  const requestGeneration = ++pointCatalogRequestGeneration;
  pointSearchTimer = window.setTimeout(() => {
    pointSearchTimer = null;
    void reloadPointCatalogForSearch(requestGeneration);
  }, POINT_SEARCH_DEBOUNCE_MS);
}

async function reloadPointCatalogForSearch(requestGeneration: number): Promise<void> {
  const plantId = state.plantId;
  const lineId = state.pointLineId;
  const search = normalizedPointSearch();
  try {
    const response = await bpiApi.currentPointCatalog(plantId, lineId, {
      limit: POINT_CATALOG_PAGE_SIZE,
      search: search || undefined,
    });
    if (requestGeneration !== pointCatalogRequestGeneration || state.view !== 'points'
        || plantId !== state.plantId || lineId !== state.pointLineId
        || search !== normalizedPointSearch()) return;
    applyPointCatalogPage(response);
    renderPoints();
    refreshIcons();
    const input = document.querySelector<HTMLInputElement>('#point-search');
    if (input) {
      input.focus();
      input.setSelectionRange(input.value.length, input.value.length);
    }
  } catch (error) {
    if (requestGeneration === pointCatalogRequestGeneration) {
      showToast(error instanceof Error ? error.message : String(error), true);
    }
  }
}

async function loadMorePointCatalog(): Promise<void> {
  const cursor = state.pointCatalogNextCursor;
  if (!cursor || state.loadingMorePointCatalog || !state.pointCatalog) return;
  const plantId = state.plantId;
  const lineId = state.pointLineId;
  const search = normalizedPointSearch();
  const snapshotAt = state.pointCatalogSnapshotAt;
  const snapshotId = state.pointCatalog.snapshot.id;
  const requestGeneration = pointCatalogRequestGeneration;
  state.loadingMorePointCatalog = true;
  renderPoints();
  refreshIcons();
  try {
    const response = await bpiApi.currentPointCatalog(plantId, lineId, {
      cursor,
      limit: POINT_CATALOG_PAGE_SIZE,
      search: search || undefined,
    });
    if (requestGeneration !== pointCatalogRequestGeneration || state.view !== 'points'
        || plantId !== state.plantId || lineId !== state.pointLineId
        || search !== normalizedPointSearch() || cursor !== state.pointCatalogNextCursor) return;
    if (!response.data || response.data.snapshot.id !== snapshotId
        || (snapshotAt && response.meta.snapshotAt !== snapshotAt)) {
      throw new Error('点位目录分页快照已变化，请刷新后重试。');
    }
    const knownIds = new Set(state.pointCatalog.points.map((point) => point.id));
    state.pointCatalog = {
      snapshot: state.pointCatalog.snapshot,
      points: [
        ...state.pointCatalog.points,
        ...response.data.points.filter((point) => !knownIds.has(point.id)),
      ],
    };
    state.pointCatalogNextCursor = response.meta.nextCursor || null;
    state.pointCatalogSnapshotAt = response.meta.snapshotAt;
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    state.loadingMorePointCatalog = false;
    if (state.view === 'points') {
      renderPoints();
      refreshIcons();
    }
  }
}

function applyPointSearch(content: HTMLElement): void {
  const keyword = state.pointSearch.trim().toLowerCase();
  content.querySelectorAll<HTMLTableRowElement>('[data-point-id], [data-calibration-row]').forEach((row) => {
    row.hidden = !row.textContent!.toLowerCase().includes(keyword);
  });
}

async function loadMorePointCalibrations(): Promise<void> {
  const cursor = state.calibrationNextCursor;
  if (!cursor || state.loadingMoreCalibrations) return;
  const plantId = state.plantId;
  const lineId = state.pointLineId;
  const snapshotAt = state.calibrationSnapshotAt;
  state.loadingMoreCalibrations = true;
  renderPoints();
  refreshIcons();
  try {
    const response = await bpiApi.listPointCalibrations(
      plantId,
      lineId,
      cursor,
      CALIBRATION_PAGE_SIZE,
    );
    if (plantId !== state.plantId || lineId !== state.pointLineId
        || cursor !== state.calibrationNextCursor) return;
    if (snapshotAt && response.meta.snapshotAt !== snapshotAt) {
      throw new Error('校准证据分页快照已变化，请刷新后重试。');
    }
    const knownIds = new Set(state.calibrations.map((calibration) => calibration.id));
    state.calibrations = [
      ...state.calibrations,
      ...response.data.filter((calibration) => !knownIds.has(calibration.id)),
    ];
    state.calibrationNextCursor = response.meta.nextCursor || null;
    state.calibrationSnapshotAt = response.meta.snapshotAt;
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    state.loadingMoreCalibrations = false;
    if (state.view === 'points') {
      renderPoints();
      refreshIcons();
    }
  }
}

function localDateTimeValue(date: Date): string {
  const value = new Date(date);
  value.setMinutes(value.getMinutes() - value.getTimezoneOffset());
  return value.toISOString().slice(0, 19);
}

function openPointCalibrationSubmit(point?: PointCatalogView['points'][number]): void {
  state.selectedCalibration = null;
  state.calibrationCommand = null;
  document.querySelector<HTMLInputElement>('#calibration-product')!.value = point?.productId || '';
  document.querySelector<HTMLInputElement>('#calibration-device')!.value = point?.deviceId || '';
  document.querySelector<HTMLInputElement>('#calibration-property')!.value = point?.propertyId || '';
  document.querySelector<HTMLInputElement>('#calibration-version')!.value = point?.calibrationVersion || '';
  document.querySelector<HTMLInputElement>('#calibration-certificate')!.value = '';
  document.querySelector<HTMLInputElement>('#calibration-checksum')!.value = '';
  const now = new Date();
  const nextYear = new Date(now);
  nextYear.setFullYear(nextYear.getFullYear() + 1);
  document.querySelector<HTMLInputElement>('#calibration-valid-from')!.value = localDateTimeValue(now);
  document.querySelector<HTMLInputElement>('#calibration-valid-until')!.value = localDateTimeValue(nextYear);
  document.querySelector<HTMLTextAreaElement>('#calibration-reason')!.value = '';
  document.querySelector<HTMLDialogElement>('#point-calibration-dialog')!.showModal();
}

async function handlePointCalibrationSubmit(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  const validFrom = new Date(document.querySelector<HTMLInputElement>('#calibration-valid-from')!.value);
  const validUntil = new Date(document.querySelector<HTMLInputElement>('#calibration-valid-until')!.value);
  if (Number.isNaN(validFrom.getTime()) || Number.isNaN(validUntil.getTime()) || validUntil <= validFrom) {
    showToast('校准有效截止时间必须晚于开始时间', true);
    return;
  }
  const command: PointCalibrationSubmitCommand = {
    plantId: state.plantId,
    lineId: state.pointLineId,
    productId: document.querySelector<HTMLInputElement>('#calibration-product')!.value.trim(),
    deviceId: document.querySelector<HTMLInputElement>('#calibration-device')!.value.trim(),
    propertyId: document.querySelector<HTMLInputElement>('#calibration-property')!.value.trim(),
    calibrationVersion: document.querySelector<HTMLInputElement>('#calibration-version')!.value.trim(),
    certificateReference: document.querySelector<HTMLInputElement>('#calibration-certificate')!.value.trim(),
    certificateChecksum: document.querySelector<HTMLInputElement>('#calibration-checksum')!.value.trim().toLowerCase(),
    validFrom: validFrom.toISOString(),
    validUntil: validUntil.toISOString(),
    reason: document.querySelector<HTMLTextAreaElement>('#calibration-reason')!.value.trim(),
  };
  const button = document.querySelector<HTMLButtonElement>('#point-calibration-submit')!;
  button.disabled = true;
  button.textContent = '提交中...';
  try {
    const response = await bpiApi.submitPointCalibration(command, commandId());
    state.calibrations = [response.data, ...state.calibrations];
    document.querySelector<HTMLDialogElement>('#point-calibration-dialog')!.close();
    showToast(`校准证据 ${response.data.calibrationVersion} 已提交，等待独立管理员复核`);
    await loadView(true);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = '提交复核';
  }
}

function openPointCalibrationCommand(calibration: PointCalibration, command: 'approve' | 'reject' | 'revoke'): void {
  state.selectedCalibration = calibration;
  state.calibrationCommand = command;
  state.candidateCommand = null;
  state.batchCommand = null;
  state.ruleCommand = null;
  state.topologyCommand = null;
  const labels = command === 'approve'
    ? { kicker: '校准证据复核', title: '批准校准证据', field: '批准依据', button: '批准证据', danger: false }
    : command === 'reject'
      ? { kicker: '校准证据复核', title: '驳回校准证据', field: '驳回原因', button: '驳回证据', danger: true }
      : { kicker: '校准证据治理', title: '撤销已批准证据', field: '撤销原因', button: '撤销证据', danger: true };
  document.querySelector('#command-kicker')!.textContent = labels.kicker;
  document.querySelector('#command-title')!.textContent = labels.title;
  document.querySelector('#command-reason-label')!.textContent = labels.field;
  document.querySelector('#command-summary')!.innerHTML = `<div><span>点位</span><b>${escapeHtml(calibration.productId)} / ${escapeHtml(calibration.deviceId)} / ${escapeHtml(calibration.propertyId)}</b></div><div><span>校准版本</span><b>${escapeHtml(calibration.calibrationVersion)}</b></div><div><span>证书</span><b>${escapeHtml(calibration.certificateReference)}</b></div><div><span>有效截止</span><b>${formatTime(calibration.validUntil)}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = command === 'approve' ? '填写证书、校验和和有效期复核依据' : '填写可审计的处置原因';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = `button ${labels.danger ? 'button--danger' : 'button--primary'}`;
  button.textContent = labels.button;
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

async function handlePointCalibrationCommand(): Promise<void> {
  const calibration = state.selectedCalibration;
  const command = state.calibrationCommand;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  if (!calibration || !command || reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  const buttonLabel = command === 'approve' ? '批准证据' : command === 'reject' ? '驳回证据' : '撤销证据';
  button.disabled = true;
  button.textContent = command === 'approve' ? '批准中...' : command === 'reject' ? '驳回中...' : '撤销中...';
  try {
    const response = command === 'approve'
      ? await bpiApi.approvePointCalibration(calibration, reason, commandId())
      : command === 'reject'
        ? await bpiApi.rejectPointCalibration(calibration, reason, commandId())
        : await bpiApi.revokePointCalibration(calibration, reason, commandId());
    state.selectedCalibration = response.data;
    state.calibrations = state.calibrations.map((item) => item.id === response.data.id ? response.data : item);
    state.calibrationCommand = null;
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    showToast(command === 'approve' ? '校准证据已批准，系统将按版本、有效期和来源序列重新计算准入' : command === 'reject' ? '校准证据已驳回' : '校准证据已撤销，相关点位已重新阻断');
    await loadView(true);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) await loadView(true);
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = buttonLabel;
  }
}

function openPointCatalogImport(): void {
  document.querySelector<HTMLInputElement>('#point-source')!.value = state.pointCatalog?.snapshot.source || 'JETLINKS';
  document.querySelector<HTMLInputElement>('#point-source-instance')!.value = state.pointCatalog?.snapshot.sourceInstance || '';
  document.querySelector<HTMLInputElement>('#point-source-revision')!.value = `JETLINKS_${Date.now()}`;
  document.querySelector<HTMLInputElement>('#point-import-line')!.value = state.pointLineId;
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  document.querySelector<HTMLInputElement>('#point-observed-at')!.value = now.toISOString().slice(0, 19);
  document.querySelector<HTMLTextAreaElement>('#point-import-json')!.value = '[]';
  document.querySelector<HTMLTextAreaElement>('#point-import-reason')!.value = '';
  document.querySelector<HTMLDialogElement>('#point-catalog-dialog')!.showModal();
}

async function handlePointCatalogImport(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  let points: PointCatalogPointCommand[];
  try {
    const parsed = JSON.parse(document.querySelector<HTMLTextAreaElement>('#point-import-json')!.value) as unknown;
    if (!Array.isArray(parsed)) throw new Error('点位 JSON 必须是数组');
    points = parsed as PointCatalogPointCommand[];
  } catch (error) {
    showToast(error instanceof Error ? error.message : '点位 JSON 无效', true);
    return;
  }
  const observedValue = document.querySelector<HTMLInputElement>('#point-observed-at')!.value;
  const observedAt = new Date(observedValue);
  if (Number.isNaN(observedAt.getTime())) {
    showToast('观测时间无效', true);
    return;
  }
  const command: PointCatalogSnapshotCommand = {
    source: document.querySelector<HTMLInputElement>('#point-source')!.value.trim(),
    sourceInstance: document.querySelector<HTMLInputElement>('#point-source-instance')!.value.trim(),
    sourceRevision: document.querySelector<HTMLInputElement>('#point-source-revision')!.value.trim(),
    plantId: state.plantId,
    lineId: document.querySelector<HTMLInputElement>('#point-import-line')!.value.trim(),
    observedAt: observedAt.toISOString(),
    points,
    reason: document.querySelector<HTMLTextAreaElement>('#point-import-reason')!.value.trim(),
  };
  const button = document.querySelector<HTMLButtonElement>('#point-catalog-submit')!;
  button.disabled = true;
  button.textContent = '导入中...';
  try {
    const response = await bpiApi.importPointCatalog(command, commandId());
    state.pointLineId = command.lineId;
    localStorage.setItem('bpi.lineId', command.lineId);
    document.querySelector<HTMLDialogElement>('#point-catalog-dialog')!.close();
    showToast(`点位快照已导入：${response.data.snapshot.readyPointCount}/${response.data.snapshot.pointCount} 就绪`);
    await loadView(true);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = '导入快照';
  }
}

function renderRules(): void {
  const content = document.querySelector('#content')!;
  const topology = state.topologies.find((item) => item.state === 'PUBLISHED') || state.topologies[0];
  const bindings = topology?.definition.bindings || [];
  const rows = state.rules.map((rule) => `
    <tr data-rule-id="${escapeHtml(rule.id)}" tabindex="0">
      <td><strong>${escapeHtml(rule.code)}</strong><small>${escapeHtml(rule.id)}</small></td>
      <td>${escapeHtml(rule.lineId)}</td><td>${escapeHtml(rule.version)}</td><td><div class="status-stack">${statusChip(rule.state)}${publicationChip(rule.publicationStatus)}${applicationChip(rule.applicationStatus)}${runtimeReadinessChip(rule.runtimeReadinessStatus)}</div></td>
      <td>${escapeHtml(rule.topologyVersion)}</td><td>${ruleConditions(rule).length}</td>
      <td>${rule.latestSimulationId ? '<span class="evidence-state evidence-state--ok"></span> 已回放' : '未回放'}</td>
      <td><span class="revision">r${rule.revision}</span></td><td><i data-lucide="chevron-right"></i></td>
    </tr>`).join('');
  const topologyRows = state.topologies.map((item) => `
    <tr data-topology-id="${escapeHtml(item.id)}" tabindex="0">
      <td><strong>${escapeHtml(item.code)}</strong><small>${escapeHtml(item.id)}</small></td>
      <td>${escapeHtml(item.lineId)}</td><td>${escapeHtml(item.version)}</td>
      <td><div class="status-stack">${statusChip(item.state)}${statusChip(item.validationStatus || 'NOT_VALIDATED')}</div></td>
      <td>${item.definition.nodes?.length || 0}</td><td>${item.definition.bindings?.length || 0}</td>
      <td><span class="revision">r${item.revision}</span></td><td><i data-lucide="chevron-right"></i></td>
    </tr>`).join('');
  const bindingRows = bindings.map((binding) => `<tr><td><strong>${escapeHtml(binding.signal)}</strong></td><td>${escapeHtml(binding.productId || '-')} / ${escapeHtml(binding.deviceId || '-')} / ${escapeHtml(binding.propertyId)}</td><td>${escapeHtml(binding.expectedUnit || binding.unit || '-')}</td><td>${escapeHtml(binding.calibrationVersion)}</td></tr>`).join('');
  content.innerHTML = `
    <div class="toolbar"><label class="search-field"><i data-lucide="search"></i><input id="rule-search" placeholder="规则编码、产线、拓扑版本" /></label><div class="toolbar-actions"><button id="new-topology" class="icon-text-button"><i data-lucide="plus"></i><span>新建拓扑</span></button><button id="new-rule" class="icon-text-button"><i data-lucide="plus"></i><span>新建规则</span></button></div></div>
    <div class="rule-workbench">
      <section><div class="section-bar"><div><i data-lucide="flask-conical"></i><strong>边界规则版本</strong></div><span>${state.rules.length} 个版本</span></div>
        ${rows ? `<div class="table-frame"><table class="rule-table"><thead><tr><th>规则</th><th>产线</th><th>版本</th><th>状态 / Kafka / 控制面 / 运行时</th><th>拓扑</th><th>条件</th><th>模拟</th><th>版本号</th><th></th></tr></thead><tbody>${rows}</tbody></table></div>` : `<div class="empty-state"><i data-lucide="network"></i><strong>暂无作用域规则</strong><span>仅展示当前工厂和令牌范围内的版本。</span></div>`}
      </section>
      <section><div class="section-bar"><div><i data-lucide="network"></i><strong>工艺拓扑版本</strong></div><span>${state.topologies.length} 个版本</span></div>
        ${topologyRows ? `<div class="table-frame"><table class="topology-table"><thead><tr><th>拓扑</th><th>产线</th><th>版本</th><th>状态 / 校验</th><th>节点</th><th>绑定</th><th>版本号</th><th></th></tr></thead><tbody>${topologyRows}</tbody></table></div>` : `<div class="empty-state"><i data-lucide="network"></i><strong>暂无拓扑版本</strong><span>先创建并校验产线拓扑。</span></div>`}
        ${topology ? `<div class="topology-summary"><div><span>当前版本</span><b>${escapeHtml(topology.code)}@${escapeHtml(topology.version)}</b></div><div><span>作用域</span><b>${escapeHtml(topology.plantId)} / ${escapeHtml(topology.lineId)}</b></div><div><span>节点</span><b>${topology.definition.nodes?.length || 0}</b></div><div><span>测点绑定</span><b>${bindings.length}</b></div></div><div class="table-frame"><table class="binding-table"><thead><tr><th>语义信号</th><th>JetLinks 产品 / 设备 / 属性</th><th>单位</th><th>校准版本</th></tr></thead><tbody>${bindingRows}</tbody></table></div>` : ''}
      </section>
    </div>`;
  content.querySelectorAll('[data-rule-id]').forEach((row) => row.addEventListener('click', () => void openRule(String((row as HTMLElement).dataset.ruleId))));
  content.querySelectorAll('[data-topology-id]').forEach((row) => row.addEventListener('click', () => void openTopology(String((row as HTMLElement).dataset.topologyId))));
  content.querySelector('#new-topology')?.addEventListener('click', openTopologyEditor);
  content.querySelector('#new-rule')?.addEventListener('click', () => openRuleEditor());
  content.querySelector<HTMLInputElement>('#rule-search')?.addEventListener('input', (event) => {
    const keyword = (event.target as HTMLInputElement).value.trim().toLowerCase();
    content.querySelectorAll<HTMLTableRowElement>('[data-rule-id]').forEach((row) => { row.hidden = !row.textContent!.toLowerCase().includes(keyword); });
  });
}

async function openTopology(topologyId: string): Promise<void> {
  try {
    const topology = (await bpiApi.topology(topologyId)).data;
    state.selectedTopology = topology;
    const comparisonPeer = state.topologies.find((item) => item.id !== topology.id
      && item.code === topology.code
      && item.plantId === topology.plantId
      && item.lineId === topology.lineId);
    const comparison = comparisonPeer
      ? (await bpiApi.compareTopologies(topology.id, comparisonPeer.id)).data
      : null;
    const issues = [...(topology.validationErrors || []), ...(topology.validationWarnings || [])];
    const issueRows = issues.map((issue) => `<li><span class="evidence-state evidence-state--${issue.severity === 'ERROR' ? 'bad' : 'ok'}"></span><div><strong>${escapeHtml(issue.code)}</strong><small>${escapeHtml(issue.path)}</small></div><b>${escapeHtml(issue.message)}</b></li>`).join('');
    const bindingRows = (topology.definition.bindings || []).map((binding) => `<li><span class="evidence-state evidence-state--ok"></span><div><strong>${escapeHtml(binding.signal)}</strong><small>${escapeHtml(binding.productId || '-')} / ${escapeHtml(binding.deviceId || '-')}</small></div><b>${escapeHtml(binding.propertyId)}</b></li>`).join('');
    const canValidate = topology.state === 'DRAFT';
    const canPublish = topology.state === 'DRAFT' && topology.validationStatus === 'PASSED';
    openDrawer(`<header><div><span>版本化工艺拓扑</span><h2>${escapeHtml(topology.code)}@${escapeHtml(topology.version)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="batch-state-band"><div>${statusChip(topology.state)}${statusChip(topology.validationStatus || 'NOT_VALIDATED')}</div><span>revision ${topology.revision}</span></div><div class="drawer-section facts-grid"><div><span>作用域</span><b>${escapeHtml(topology.plantId)} / ${escapeHtml(topology.lineId)}</b></div><div><span>本地性组</span><b>${escapeHtml(topology.definition.localityGroup || '-')}</b></div><div><span>校验人 / 时间</span><b>${escapeHtml(topology.validatedBy || '-')} · ${formatTime(topology.validatedAt)}</b></div><div><span>发布人 / 时间</span><b>${escapeHtml(topology.publishedBy || '-')} · ${formatTime(topology.publishedAt)}</b></div><div><span>拓扑 checksum</span><b class="mono-value">${escapeHtml(topology.checksum)}</b></div><div><span>点位目录快照</span><b class="mono-value">${escapeHtml(topology.validatedPointCatalogSnapshotId || '-')}</b></div><div><span>目录 checksum</span><b class="mono-value">${escapeHtml(topology.validatedPointCatalogChecksum || '-')}</b></div><div><span>节点 / 路径</span><b>${topology.definition.nodes?.length || 0} / ${topology.definition.edges?.length || 0}</b></div></div><div class="drawer-section">${versionComparisonHtml(comparison, '拓扑')}</div><div class="drawer-section"><div class="section-title"><h3>JetLinks 测点绑定</h3><span>${topology.definition.bindings?.length || 0} 条</span></div>${bindingRows ? `<ul class="evidence-list evidence-list--compact">${bindingRows}</ul>` : '<div class="simulation-empty">暂无测点绑定</div>'}</div><div class="drawer-section"><div class="section-title"><h3>校验结果</h3><span>${issues.length} 项</span></div>${issueRows ? `<ul class="evidence-list topology-issue-list">${issueRows}</ul>` : '<div class="simulation-empty">暂无校验问题</div>'}</div><footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button>${canValidate ? '<button class="button button--secondary" id="open-topology-validate">校验拓扑</button>' : ''}${canPublish ? '<button class="button button--primary" id="open-topology-publish">发布拓扑</button>' : ''}</footer>`);
    document.querySelector('#open-topology-validate')?.addEventListener('click', () => openTopologyCommandDialog('validate'));
    document.querySelector('#open-topology-publish')?.addEventListener('click', () => openTopologyCommandDialog('publish'));
  } catch (error) { showToast(error instanceof Error ? error.message : String(error), true); }
}

function openTopologyCommandDialog(command: 'validate' | 'publish'): void {
  const topology = state.selectedTopology;
  if (!topology) return;
  state.topologyCommand = command;
  state.candidateCommand = null;
  state.batchCommand = null;
  state.ruleCommand = null;
  state.calibrationCommand = null;
  const publish = command === 'publish';
  document.querySelector('#command-kicker')!.textContent = publish ? '拓扑版本控制' : '拓扑结构校验';
  document.querySelector('#command-title')!.textContent = publish ? '发布不可变拓扑版本' : '校验拓扑与测点绑定';
  document.querySelector('#command-reason-label')!.textContent = publish ? '发布原因' : '校验原因';
  document.querySelector('#command-summary')!.innerHTML = `<div><span>拓扑</span><b>${escapeHtml(topology.code)}@${escapeHtml(topology.version)}</b></div><div><span>作用域</span><b>${escapeHtml(topology.lineId)}</b></div><div><span>当前校验</span><b>${escapeHtml(topology.validationStatus)}</b></div><div><span>版本</span><b>r${topology.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = publish ? '填写独立审批和上线依据' : '填写结构、测点和单位校验依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = 'button button--primary';
  button.textContent = publish ? '确认发布' : '开始校验';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

async function handleTopologyCommand(): Promise<void> {
  const topology = state.selectedTopology;
  const command = state.topologyCommand;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  if (!topology || !command || reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = command === 'publish' ? '发布中...' : '校验中...';
  try {
    const response = command === 'publish'
      ? await bpiApi.publishTopology(topology, reason, commandId())
      : await bpiApi.validateTopology(topology, reason, commandId());
    state.selectedTopology = response.data;
    state.topologies = state.topologies.map((item) => item.id === response.data.id ? response.data : item);
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    state.topologyCommand = null;
    showToast(command === 'publish'
      ? `拓扑 ${response.data.code}@${response.data.version} 已发布`
      : response.data.validationStatus === 'PASSED' ? '拓扑校验通过，可提交独立管理员发布' : `拓扑校验失败：${response.data.validationErrors.length} 项错误`);
    renderRules();
    await openTopology(response.data.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`拓扑已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openTopology(topology.id);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = command === 'publish' ? '确认发布' : '开始校验';
  }
}

function openTopologyEditor(): void {
  const published = state.topologies.filter((item) => item.state === 'PUBLISHED');
  const base = document.querySelector<HTMLSelectElement>('#topology-base')!;
  base.innerHTML = `<option value="">全新拓扑</option>${published.map((item) => `<option value="${escapeHtml(item.id)}">${escapeHtml(item.code)}@${escapeHtml(item.version)}</option>`).join('')}`;
  base.value = '';
  document.querySelector<HTMLInputElement>('#topology-code')!.value = '';
  document.querySelector<HTMLInputElement>('#topology-version')!.value = '';
  document.querySelector<HTMLInputElement>('#topology-line')!.value = topologyLineFallback();
  document.querySelector<HTMLTextAreaElement>('#topology-definition')!.value = JSON.stringify(defaultTopologyDefinition(), null, 2);
  document.querySelector<HTMLTextAreaElement>('#topology-reason')!.value = '';
  document.querySelector<HTMLDialogElement>('#topology-editor-dialog')!.showModal();
}

function applyTopologyBase(): void {
  const id = document.querySelector<HTMLSelectElement>('#topology-base')!.value;
  const base = state.topologies.find((item) => item.id === id);
  if (!base) return;
  document.querySelector<HTMLInputElement>('#topology-code')!.value = base.code;
  document.querySelector<HTMLInputElement>('#topology-version')!.value = '';
  document.querySelector<HTMLInputElement>('#topology-line')!.value = base.lineId;
  document.querySelector<HTMLTextAreaElement>('#topology-definition')!.value = JSON.stringify(base.definition, null, 2);
}

async function handleTopologyDraft(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  const baseId = document.querySelector<HTMLSelectElement>('#topology-base')!.value;
  const base = state.topologies.find((item) => item.id === baseId);
  let definition: TopologyDraftCommand['definition'];
  try {
    definition = JSON.parse(document.querySelector<HTMLTextAreaElement>('#topology-definition')!.value) as TopologyDraftCommand['definition'];
  } catch {
    showToast('拓扑定义不是有效 JSON', true);
    return;
  }
  const command: TopologyDraftCommand = {
    code: document.querySelector<HTMLInputElement>('#topology-code')!.value.trim(),
    version: document.querySelector<HTMLInputElement>('#topology-version')!.value.trim(),
    plantId: state.plantId,
    lineId: document.querySelector<HTMLInputElement>('#topology-line')!.value.trim(),
    baseVersionId: base?.id || null,
    definition,
    reason: document.querySelector<HTMLTextAreaElement>('#topology-reason')!.value.trim(),
  };
  const button = document.querySelector<HTMLButtonElement>('#topology-editor-submit')!;
  button.disabled = true;
  button.textContent = '创建中...';
  try {
    const response = await bpiApi.createTopologyDraft(command, commandId(), base?.revision || 0);
    state.topologies = [response.data, ...state.topologies];
    document.querySelector<HTMLDialogElement>('#topology-editor-dialog')!.close();
    showToast(`拓扑草稿 ${response.data.code}@${response.data.version} 已创建`);
    renderRules();
    await openTopology(response.data.id);
  } catch (error) { showToast(error instanceof Error ? error.message : String(error), true); }
  finally { button.disabled = false; button.textContent = '创建草稿'; }
}

function openRuleEditor(preferredBaseId?: string): void {
  const topologies = state.topologies.filter((item) => item.state === 'PUBLISHED');
  if (!topologies.length) {
    showToast('请先校验并发布一个拓扑版本', true);
    return;
  }
  const controlledRules = state.rules.filter((item) => ['PUBLISHED', 'RETIRED'].includes(item.state));
  const base = document.querySelector<HTMLSelectElement>('#rule-base')!;
  base.innerHTML = `<option value="">全新规则</option>${controlledRules.map((item) => `<option value="${escapeHtml(item.id)}">${escapeHtml(item.code)}@${escapeHtml(item.version)} · ${item.state === 'RETIRED' ? '回滚来源' : '已发布'}</option>`).join('')}`;
  const topologySelect = document.querySelector<HTMLSelectElement>('#rule-topology')!;
  topologySelect.innerHTML = topologies.map((item) => `<option value="${escapeHtml(item.code)}@${escapeHtml(item.version)}">${escapeHtml(item.code)}@${escapeHtml(item.version)} · ${escapeHtml(item.lineId)}</option>`).join('');
  topologySelect.onchange = applyRuleTopology;
  document.querySelector<HTMLInputElement>('#rule-code')!.value = '';
  document.querySelector<HTMLInputElement>('#rule-version')!.value = '';
  applyRuleTopology();
  document.querySelector<HTMLTextAreaElement>('#rule-reason')!.value = '';
  document.querySelector('#rule-editor-title')!.textContent = preferredBaseId ? '创建回滚规则草稿' : '新建规则版本';
  if (preferredBaseId && controlledRules.some((item) => item.id === preferredBaseId)) {
    base.value = preferredBaseId;
    applyRuleBase();
  }
  document.querySelector<HTMLDialogElement>('#rule-editor-dialog')!.showModal();
}

function applyRuleBase(): void {
  const id = document.querySelector<HTMLSelectElement>('#rule-base')!.value;
  const base = state.rules.find((item) => item.id === id);
  if (!base) return;
  document.querySelector<HTMLInputElement>('#rule-code')!.value = base.code;
  document.querySelector<HTMLInputElement>('#rule-version')!.value = '';
  document.querySelector<HTMLInputElement>('#rule-line')!.value = base.lineId;
  document.querySelector<HTMLSelectElement>('#rule-topology')!.value = base.topologyVersion;
  document.querySelector<HTMLTextAreaElement>('#rule-ast')!.value = JSON.stringify(base.ast, null, 2);
}

function applyRuleTopology(): void {
  const topologyRef = document.querySelector<HTMLSelectElement>('#rule-topology')!.value;
  const topology = state.topologies.find((item) => `${item.code}@${item.version}` === topologyRef);
  if (!topology) return;
  document.querySelector<HTMLInputElement>('#rule-line')!.value = topology.lineId;
  const signal = topology.definition.bindings?.[0]?.signal || 'feed.flow';
  document.querySelector<HTMLTextAreaElement>('#rule-ast')!.value = JSON.stringify(defaultRuleAst(signal), null, 2);
}

async function handleRuleDraft(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  const baseId = document.querySelector<HTMLSelectElement>('#rule-base')!.value;
  const base = state.rules.find((item) => item.id === baseId);
  let ast: Record<string, unknown>;
  try {
    ast = JSON.parse(document.querySelector<HTMLTextAreaElement>('#rule-ast')!.value) as Record<string, unknown>;
  } catch {
    showToast('规则 AST 不是有效 JSON', true);
    return;
  }
  const command: RuleDraftCommand = {
    code: document.querySelector<HTMLInputElement>('#rule-code')!.value.trim(),
    version: document.querySelector<HTMLInputElement>('#rule-version')!.value.trim(),
    lineId: document.querySelector<HTMLInputElement>('#rule-line')!.value.trim(),
    topologyVersion: document.querySelector<HTMLSelectElement>('#rule-topology')!.value,
    baseVersionId: base?.id || null,
    ast,
    reason: document.querySelector<HTMLTextAreaElement>('#rule-reason')!.value.trim(),
  };
  const button = document.querySelector<HTMLButtonElement>('#rule-editor-submit')!;
  button.disabled = true;
  button.textContent = '创建中...';
  try {
    const response = await bpiApi.createRuleDraft(command, commandId(), base?.revision || 0);
    state.rules = [response.data, ...state.rules];
    document.querySelector<HTMLDialogElement>('#rule-editor-dialog')!.close();
    showToast(`规则草稿 ${response.data.code}@${response.data.version} 已创建`);
    renderRules();
    await openRule(response.data.id);
  } catch (error) { showToast(error instanceof Error ? error.message : String(error), true); }
  finally { button.disabled = false; button.textContent = '创建草稿'; }
}

function topologyLineFallback(): string {
  return state.topologies[0]?.lineId || state.rules[0]?.lineId || 'LINE-S07-01';
}

function defaultTopologyDefinition(): TopologyDraftCommand['definition'] {
  const point = state.pointCatalog?.points.find((item) => item.ready);
  return {
    localityGroup: point?.localityGroup || topologyLineFallback(),
    nodes: [
      { code: 'FEED-TANK', type: 'TANK', name: '进料罐' },
      { code: 'FLOW-METER', type: 'METER', name: '进料流量计' },
      { code: 'RECEIVE-TANK', type: 'TANK', name: '接收罐' },
    ],
    edges: [
      { from: 'FEED-TANK', to: 'FLOW-METER' },
      { from: 'FLOW-METER', to: 'RECEIVE-TANK' },
    ],
    bindings: point ? [{
      signal: 'feed.flow',
      productId: point.productId,
      deviceId: point.deviceId,
      propertyId: point.propertyId,
      expectedUnit: point.unit || '',
      calibrationVersion: point.calibrationVersion || '',
    }] : [],
    requiredSignals: point ? ['feed.flow'] : [],
  };
}

function defaultRuleAst(signal: string): Record<string, unknown> {
  return {
    boundaryType: 'START',
    quorumMinimum: 1,
    minimumConfidence: 0.8,
    maxCompositePenalty: 0.8,
    timing: { allowedLatenessSeconds: 0, watermarkDelaySeconds: 0, evaluationTimeoutSeconds: 300 },
    conditions: [{ signal, operator: 'GREATER_THAN', threshold: 10, holdSeconds: 15, maxSilenceSeconds: 60, classification: 'QUORUM', weight: 100 }],
  };
}

async function openRule(ruleId: string): Promise<void> {
  try {
    const ruleResponse = await bpiApi.rule(ruleId);
    const rule = ruleResponse.data;
    state.selectedRule = rule;
    state.selectedSimulation = rule.latestSimulationId
      ? (await bpiApi.simulation(rule.latestSimulationId)).data
      : null;
    const topology = state.topologies.find((item) => `${item.code}@${item.version}` === rule.topologyVersion);
    const comparisonPeer = state.rules.find((item) => item.id !== rule.id
      && item.code === rule.code
      && item.plantId === rule.plantId
      && item.lineId === rule.lineId);
    let comparison: VersionComparison | null = null;
    if (comparisonPeer) {
      comparison = (await bpiApi.compareRules(rule.id, comparisonPeer.id)).data;
    }
    const conditions = ruleConditions(rule).map((condition) => `<li><div><strong>${escapeHtml(condition.signal)}</strong><small>${escapeHtml(condition.classification)} · ${escapeHtml(condition.operator)}</small></div><b>${escapeHtml(condition.threshold)}</b><em>${escapeHtml(condition.holdSeconds)}s</em></li>`).join('');
    const simulation = state.selectedSimulation;
    const simulationHtml = simulation ? `<div class="simulation-result simulation-result--${simulation.state === 'PASSED' ? 'pass' : 'fail'}"><div class="section-title"><h3>最近回放</h3>${statusChip(simulation.state)}</div><div class="metric-grid"><div><span>命中</span><b>${simulation.metrics.matched}</b></div><div><span>漏检</span><b>${simulation.metrics.missed}</b></div><div><span>误报</span><b>${simulation.metrics.falsePositive}</b></div><div><span>平均偏差</span><b>${number(simulation.metrics.meanBoundaryErrorSeconds)}s</b></div></div><dl class="manifest"><div><dt>观测值</dt><dd>${simulation.inputManifest.observationCount ?? '-'}</dd></div><div><dt>金标准边界</dt><dd>${simulation.inputManifest.goldenBoundaryCount ?? '-'}</dd></div><div><dt>发射边界</dt><dd>${simulation.emittedBoundaries.map(formatTime).join('、') || '-'}</dd></div></dl><div class="checksum"><span>simulation checksum</span><code>${escapeHtml(simulation.checksum)}</code></div>${simulation.failureReason ? `<p>${escapeHtml(simulation.failureReason)}</p>` : ''}</div>` : `<div class="simulation-empty"><i data-lucide="flask-conical"></i><span>尚未使用 PostgreSQL 历史测点和人工金标准执行回放。</span></div>`;
    const applicationError = rule.applicationErrorCode || rule.applicationErrorDetail
      ? `<div class="error-callout"><strong>${escapeHtml(rule.applicationErrorCode || 'FLINK_APPLICATION_REJECTED')}</strong>${rule.applicationErrorDetail ? `<span>${escapeHtml(rule.applicationErrorDetail)}</span>` : ''}</div>`
      : '';
    const runtimeReadinessError = rule.runtimeReadinessReasonCode || rule.runtimeReadinessDetail
      ? `<div class="error-callout"><strong>${escapeHtml(rule.runtimeReadinessReasonCode || 'RUNTIME_NOT_READY')}</strong>${rule.runtimeReadinessDetail ? `<span>${escapeHtml(rule.runtimeReadinessDetail)}</span>` : ''}</div>`
      : '';
    const approvalHtml = `<div class="section-title"><h3>版本审批</h3>${statusChip(rule.approvalStatus)}</div><div class="facts-grid"><div><span>提交人</span><b>${escapeHtml(rule.approvalSubmittedBy || '-')}</b></div><div><span>提交时间</span><b>${formatTime(rule.approvalSubmittedAt)}</b></div><div><span>决定人</span><b>${escapeHtml(rule.approvalDecidedBy || '-')}</b></div><div><span>决定时间</span><b>${formatTime(rule.approvalDecidedAt)}</b></div></div><p>${rule.approvalStatus === 'PENDING' ? '规则已冻结在待审批状态，必须由不同于创建人和提交人的管理员批准。' : rule.approvalStatus === 'APPROVED' ? '审批与发布审计已落库，后续运行状态仍以 Kafka 和 Flink 回执为准。' : rule.approvalStatus === 'REJECTED' ? '该审批已驳回，规则退回草稿并需要重新模拟。' : '最近一次模拟通过后可提交审批。'}</p>`;
    const lifecycleHtml = `<div class="lifecycle-summary"><div class="section-title"><h3>生命周期命令</h3>${statusChip(rule.lifecycleAction)}</div><div class="facts-grid"><div><span>动作</span><b>${rule.lifecycleAction === 'RETIRE' ? '退役停用' : rule.lifecycleAction === 'ACTIVATE' ? '发布激活' : '尚未发布'}</b></div><div><span>序号</span><b>#${rule.lifecycleSequence}</b></div><div><span>期望在线状态</span><b>${rule.lifecycleActive ? 'ACTIVE' : 'INACTIVE'}</b></div><div><span>规则状态</span><b>${escapeHtml(rule.state)}</b></div></div></div>`;
    const comparisonHtml = versionComparisonHtml(comparison, '规则');
    const publicationHtml = `<div class="section-title"><h3>规则发布链路</h3>${publicationChip(rule.publicationStatus)}</div><div class="facts-grid"><div><span>本轮尝试</span><b>${rule.publicationAttemptCount}</b></div><div><span>累计尝试</span><b>${rule.publicationTotalAttemptCount}</b></div><div><span>人工重试</span><b>${rule.publicationManualRetryCount}</b></div><div><span>发布修订</span><b>r${rule.publicationRevision}</b></div><div><span>最近重新入队</span><b>${formatTime(rule.publicationLastRequeuedAt)}</b></div><div><span>Kafka 确认时间</span><b>${formatTime(rule.publicationPublishedAt)}</b></div></div><p>${escapeHtml(publicationExplanation(rule))}</p>${rule.publicationLastError ? `<div class="error-callout">${escapeHtml(rule.publicationLastError)}</div>` : ''}<div class="application-trace"><div class="section-title"><h3>控制面应用回执</h3>${applicationChip(rule.applicationStatus)}</div><div class="facts-grid"><div><span>控制面部署</span><b>${escapeHtml(rule.applicationDeploymentId || '-')}</b></div><div><span>Flink 观察时间</span><b>${formatTime(rule.applicationObservedAt)}</b></div><div><span>BPI 接收时间</span><b>${formatTime(rule.applicationReceivedAt)}</b></div><div><span>回执后修订</span><b>r${rule.publicationRevision}</b></div></div><p>${escapeHtml(applicationExplanation(rule))}</p>${applicationError}</div><div class="runtime-readiness-trace runtime-readiness-trace--${statusTone(rule.runtimeReadinessStatus)}"><div class="section-title"><h3>流式评估器运行就绪</h3>${runtimeReadinessChip(rule.runtimeReadinessStatus)}</div><div class="facts-grid"><div><span>规则版本</span><b>${escapeHtml(rule.code)}@${escapeHtml(rule.version)}</b></div><div><span>评估器部署</span><b>${escapeHtml(rule.runtimeReadinessDeploymentId || '-')}</b></div><div><span>运行观察时间</span><b>${formatTime(rule.runtimeReadinessObservedAt)}</b></div><div><span>BPI 接收时间</span><b>${formatTime(rule.runtimeReadinessReceivedAt)}</b></div><div><span>点位目录事件</span><b class="mono-value">${escapeHtml(rule.runtimePointCatalogEventId || '-')}</b></div><div><span>目录来源版本</span><b class="mono-value">${escapeHtml(rule.runtimePointCatalogSourceRevision || '-')}</b></div></div><p>${escapeHtml(runtimeReadinessExplanation(rule))}</p>${runtimeReadinessError}</div>`;
    const canSubmit = rule.state === 'SIMULATION_PASSED' && simulation?.state === 'PASSED';
    const canApprove = rule.state === 'PENDING_APPROVAL' && simulation?.state === 'PASSED';
    const canSimulate = ['DRAFT', 'SIMULATION_PASSED'].includes(rule.state);
    const canRetire = rule.state === 'PUBLISHED'
      && rule.lifecycleAction === 'ACTIVATE'
      && rule.lifecycleActive
      && rule.publicationStatus === 'PUBLISHED'
      && rule.applicationStatus === 'APPLIED'
      && ['READY', 'DEGRADED'].includes(rule.runtimeReadinessStatus);
    openDrawer(`<header><div><span>受控边界规则</span><h2>${escapeHtml(rule.code)}@${escapeHtml(rule.version)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="batch-state-band"><div>${statusChip(rule.state)}</div><span>revision ${rule.revision}</span></div><div class="drawer-section facts-grid"><div><span>作用域</span><b>${escapeHtml(rule.plantId)} / ${escapeHtml(rule.lineId)}</b></div><div><span>拓扑版本</span><b>${escapeHtml(rule.topologyVersion)}</b></div><div><span>规则 checksum</span><b class="mono-value">${escapeHtml(rule.checksum)}</b></div><div><span>拓扑绑定</span><b>${topology?.definition.bindings?.length || 0} 个测点</b></div></div><div class="drawer-section"><div class="section-title"><h3>受控 AST 条件</h3><span>${ruleConditions(rule).length} 条</span></div><ul class="evidence-list rule-condition-list">${conditions}</ul></div><div class="drawer-section">${comparisonHtml}</div><div class="drawer-section">${simulationHtml}</div><div class="drawer-section">${approvalHtml}</div><div class="drawer-section">${publicationHtml}</div><footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button><button class="button button--secondary" id="open-simulation"><i data-lucide="play"></i>运行历史回放</button>${rule.publicationStatus === 'FAILED' ? '<button class="button button--danger" id="open-publication-retry">管理员重新入队</button>' : ''}${canApprove ? '<button class="button button--danger" id="open-rule-reject">管理员驳回</button><button class="button button--primary" id="open-rule-approve">管理员批准并发布</button>' : ''}${canSubmit ? '<button class="button button--primary" id="open-rule-submit">提交审批</button>' : ''}</footer>`);
    const publicationSection = Array.from(document.querySelectorAll<HTMLElement>('.drawer-section'))
      .find((section) => section.querySelector('h3')?.textContent === '规则发布链路');
    publicationSection?.insertAdjacentHTML('afterbegin', lifecycleHtml);
    if (!canSimulate) document.querySelector('#open-simulation')?.remove();
    const actions = document.querySelector<HTMLElement>('.drawer-actions');
    if (canRetire) actions?.insertAdjacentHTML('beforeend', '<button class="button button--danger" id="open-rule-retire">管理员退役</button>');
    if (rule.state === 'RETIRED') actions?.insertAdjacentHTML('beforeend', '<button class="button button--primary" id="open-rule-rollback">创建回滚草稿</button>');
    document.querySelector('#open-simulation')?.addEventListener('click', openRuleSimulationDialog);
    document.querySelector('#open-rule-submit')?.addEventListener('click', () => openRulePublishDialog('submit'));
    document.querySelector('#open-rule-approve')?.addEventListener('click', () => openRulePublishDialog('approve'));
    document.querySelector('#open-rule-reject')?.addEventListener('click', openRuleRejectDialog);
    document.querySelector('#open-publication-retry')?.addEventListener('click', openRuleRetryDialog);
    document.querySelector('#open-rule-retire')?.addEventListener('click', openRuleRetireDialog);
    document.querySelector('#open-rule-rollback')?.addEventListener('click', () => openRuleEditor(rule.id));
  } catch (error) { showToast(error instanceof Error ? error.message : String(error), true); }
}

function openRuleSimulationDialog(): void {
  const rule = state.selectedRule;
  if (!rule) return;
  document.querySelector<HTMLInputElement>('#simulation-from')!.value = '2026-07-01T00:00:00';
  document.querySelector<HTMLInputElement>('#simulation-to')!.value = '2026-07-12T00:00:00';
  document.querySelector<HTMLDialogElement>('#simulation-dialog')!.showModal();
}

async function handleRuleSimulation(event: SubmitEvent): Promise<void> {
  const submitter = event.submitter as HTMLButtonElement | null;
  if (submitter?.value === 'cancel') return;
  event.preventDefault();
  const rule = state.selectedRule;
  if (!rule) return;
  const command: RuleSimulationCommand = {
    lineId: rule.lineId,
    from: new Date(document.querySelector<HTMLInputElement>('#simulation-from')!.value).toISOString(),
    to: new Date(document.querySelector<HTMLInputElement>('#simulation-to')!.value).toISOString(),
    topologyVersion: rule.topologyVersion,
    calibrationVersion: document.querySelector<HTMLInputElement>('#simulation-calibration')!.value.trim(),
    goldenSetId: document.querySelector<HTMLInputElement>('#simulation-golden')!.value.trim(),
  };
  const button = document.querySelector<HTMLButtonElement>('#simulation-submit')!;
  button.disabled = true;
  button.textContent = '回放中...';
  try {
    const response = await bpiApi.simulateRule(rule, command, commandId());
    state.selectedSimulation = response.data;
    state.selectedRule = (await bpiApi.rule(rule.id)).data;
    state.rules = state.rules.map((item) => item.id === rule.id ? state.selectedRule! : item);
    document.querySelector<HTMLDialogElement>('#simulation-dialog')!.close();
    showToast(response.data.state === 'PASSED' ? '历史回放通过，可提交发布' : '历史回放未通过，规则保持草稿');
    renderRules();
    await openRule(rule.id);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = '开始回放';
  }
}

function openRulePublishDialog(command: 'submit' | 'approve'): void {
  const rule = state.selectedRule;
  const simulation = state.selectedSimulation;
  if (!rule || !simulation || simulation.state !== 'PASSED') return;
  state.candidateCommand = null;
  state.batchCommand = null;
  state.topologyCommand = null;
  state.ruleCommand = command;
  state.calibrationCommand = null;
  document.querySelector('#command-kicker')!.textContent = '规则版本控制';
  document.querySelector('#command-title')!.textContent = command === 'submit' ? '提交规则审批' : '批准并发布边界规则';
  document.querySelector('#command-reason-label')!.textContent = command === 'submit' ? '提交依据' : '审批依据';
  document.querySelector('#command-summary')!.innerHTML = `<div><span>规则</span><b>${escapeHtml(rule.code)}@${escapeHtml(rule.version)}</b></div><div><span>作用域</span><b>${escapeHtml(rule.lineId)}</b></div><div><span>回放结果</span><b>${simulation.metrics.matched} 命中 / ${simulation.metrics.missed} 漏检 / ${simulation.metrics.falsePositive} 误报</b></div><div><span>版本</span><b>r${rule.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = command === 'submit' ? '填写回放批次范围、现场复核和提交依据' : '填写独立复核、职责分离和发布依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = 'button button--primary';
  button.textContent = command === 'submit' ? '确认提交审批' : '确认批准并发布';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

async function handleRulePublish(): Promise<void> {
  const rule = state.selectedRule;
  const simulation = state.selectedSimulation;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  const command = state.ruleCommand;
  if (!rule || !simulation || !['submit', 'approve'].includes(command || '') || reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = command === 'submit' ? '提交中...' : '发布中...';
  try {
    const response = command === 'submit'
      ? await bpiApi.submitRuleApproval(rule, simulation, reason, commandId())
      : await bpiApi.publishRule(rule, simulation, reason, commandId());
    state.selectedRule = response.data;
    state.rules = state.rules.map((item) => item.id === response.data.id ? response.data : item);
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    state.ruleCommand = null;
    showToast(command === 'submit'
      ? `规则 ${response.data.code}@${response.data.version} 已进入待审批`
      : `规则 ${response.data.code}@${response.data.version} 已批准发布，当前${response.data.publicationStatus === 'PENDING' ? '待分发' : response.data.publicationStatus}`);
    renderRules();
    await openRule(response.data.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`规则已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openRule(rule.id);
    } else if (error instanceof ApiProblem) {
      showToast(rulePublicationProblemMessage(error), true);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = command === 'submit' ? '确认提交审批' : '确认批准并发布';
  }
}

function openRuleRejectDialog(): void {
  const rule = state.selectedRule;
  if (!rule || rule.state !== 'PENDING_APPROVAL') return;
  state.candidateCommand = null;
  state.batchCommand = null;
  state.topologyCommand = null;
  state.ruleCommand = 'reject';
  state.calibrationCommand = null;
  document.querySelector('#command-kicker')!.textContent = '规则版本审批';
  document.querySelector('#command-title')!.textContent = '驳回规则审批';
  document.querySelector('#command-reason-label')!.textContent = '驳回原因';
  document.querySelector('#command-summary')!.innerHTML = `<div><span>规则</span><b>${escapeHtml(rule.code)}@${escapeHtml(rule.version)}</b></div><div><span>提交人</span><b>${escapeHtml(rule.approvalSubmittedBy || '-')}</b></div><div><span>处理结果</span><b>PENDING_APPROVAL → DRAFT</b></div><div><span>版本</span><b>r${rule.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = '填写规则、数据、回放或现场复核中需要修正的问题';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = 'button button--danger';
  button.textContent = '确认驳回';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

function openRuleRetryDialog(): void {
  const rule = state.selectedRule;
  if (!rule || rule.publicationStatus !== 'FAILED') return;
  state.candidateCommand = null;
  state.batchCommand = null;
  state.topologyCommand = null;
  state.ruleCommand = 'retry';
  state.calibrationCommand = null;
  document.querySelector('#command-kicker')!.textContent = '规则发布运维';
  document.querySelector('#command-title')!.textContent = '重新入队失败事件';
  document.querySelector('#command-reason-label')!.textContent = '重试依据';
  document.querySelector('#command-summary')!.innerHTML = `<div><span>规则</span><b>${escapeHtml(rule.code)}@${escapeHtml(rule.version)}</b></div><div><span>作用域</span><b>${escapeHtml(rule.lineId)}</b></div><div><span>累计尝试</span><b>${rule.publicationTotalAttemptCount}</b></div><div><span>发布修订</span><b>r${rule.publicationRevision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = '填写 Kafka 故障处置、恢复验证和重新入队依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = 'button button--danger';
  button.textContent = '确认重新入队';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

function openRuleRetireDialog(): void {
  const rule = state.selectedRule;
  if (!rule || rule.state !== 'PUBLISHED' || rule.lifecycleAction !== 'ACTIVATE') return;
  state.candidateCommand = null;
  state.batchCommand = null;
  state.topologyCommand = null;
  state.ruleCommand = 'retire';
  state.calibrationCommand = null;
  document.querySelector('#command-kicker')!.textContent = '规则生命周期';
  document.querySelector('#command-title')!.textContent = '退役边界规则';
  document.querySelector('#command-reason-label')!.textContent = '退役依据';
  document.querySelector('#command-summary')!.innerHTML = `<div><span>规则</span><b>${escapeHtml(rule.code)}@${escapeHtml(rule.version)}</b></div><div><span>当前证据</span><b>Kafka ${rule.publicationStatus} / Flink ${rule.applicationStatus}</b></div><div><span>处理结果</span><b>PUBLISHED → RETIRED</b></div><div><span>生命周期</span><b>#${rule.lifecycleSequence} → #${rule.lifecycleSequence + 1}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = '填写替代版本、变更单、停用窗口和现场复核依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = 'button button--danger';
  button.textContent = '确认退役并停用';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

async function handleRuleCommand(): Promise<void> {
  if (state.ruleCommand === 'submit' || state.ruleCommand === 'approve') {
    await handleRulePublish();
    return;
  }
  const rule = state.selectedRule;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  if (rule && state.ruleCommand === 'reject' && reason.length >= 3) {
    const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
    button.disabled = true;
    button.textContent = '驳回中...';
    try {
      const response = await bpiApi.rejectRuleApproval(rule, reason, commandId());
      state.selectedRule = response.data;
      state.rules = state.rules.map((item) => item.id === response.data.id ? response.data : item);
      document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
      state.ruleCommand = null;
      showToast(`规则 ${response.data.code}@${response.data.version} 已退回草稿`);
      renderRules();
      await openRule(response.data.id);
    } catch (error) {
      showToast(error instanceof Error ? error.message : String(error), true);
    } finally {
      button.disabled = false;
      button.textContent = '确认驳回';
    }
    return;
  }
  if (rule && state.ruleCommand === 'retire' && reason.length >= 3) {
    const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
    button.disabled = true;
    button.textContent = '退役中...';
    try {
      const response = await bpiApi.retireRule(rule, reason, commandId());
      state.selectedRule = response.data;
      state.rules = state.rules.map((item) => item.id === response.data.id ? response.data : item);
      document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
      state.ruleCommand = null;
      showToast(`规则 ${response.data.code}@${response.data.version} 已退役，等待 Kafka 与 Flink 确认 INACTIVE`);
      renderRules();
      await openRule(response.data.id);
    } catch (error) {
      if (error instanceof ApiProblem && error.problem.status === 409) {
        showToast(`退役前置状态已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
        await openRule(rule.id);
      } else showToast(error instanceof Error ? error.message : String(error), true);
    } finally {
      button.disabled = false;
      button.textContent = '确认退役并停用';
    }
    return;
  }
  if (!rule || state.ruleCommand !== 'retry' || reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = '重新入队中...';
  try {
    const response = await bpiApi.retryRulePublication(rule, reason, commandId());
    state.selectedRule = response.data;
    state.rules = state.rules.map((item) => item.id === response.data.id ? response.data : item);
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    state.ruleCommand = null;
    showToast(`规则 ${response.data.code}@${response.data.version} 的发布事件已重新入队`);
    renderRules();
    await openRule(response.data.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`发布事件已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openRule(rule.id);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = '确认重新入队';
  }
}

function renderBatches(): void {
  const content = document.querySelector('#content')!;
  const rows = state.batches.map((batch) => `<tr data-batch-id="${escapeHtml(batch.id)}" tabindex="0"><td><strong>${escapeHtml(batch.batchNo)}</strong>${batch.shadow ? '<small>SHADOW</small>' : ''}</td><td>${escapeHtml(batch.materialCode || '-')}</td><td>${escapeHtml(batch.lineId)}</td><td>${escapeHtml(batch.stageCode)}</td><td>${formatTime(batch.startTime)}</td><td class="metric"><b>${number(batch.quantity)}</b><small>${escapeHtml(batch.quantityUnit)}</small></td><td>${statusChip(batch.qualityGate)}</td><td>${statusChip(batch.wmsStatus)}</td><td>${statusChip(batch.state)}</td><td><span class="revision">r${batch.revision}</span></td><td><i data-lucide="chevron-right"></i></td></tr>`).join('');
  content.innerHTML = `<div class="toolbar"><label class="search-field"><i data-lucide="search"></i><input id="batch-search" placeholder="批次号、物料、生产指令" /></label><span class="toolbar-note">查询快照 ${state.meta ? formatTime(state.meta.snapshotAt) : '-'}</span></div>${rows ? `<div class="table-frame"><table><thead><tr><th>批次号</th><th>物料</th><th>产线</th><th>工段</th><th>开始时间</th><th>累计量</th><th>质量门</th><th>库存</th><th>状态</th><th>版本</th><th></th></tr></thead><tbody>${rows}</tbody></table></div>` : `<div class="empty-state"><i data-lucide="archive"></i><strong>暂无影子批次</strong><span>确认 START 候选后会生成第一条批次事实。</span></div>`}`;
  content.querySelectorAll('[data-batch-id]').forEach((row) => row.addEventListener('click', () => void openBatch(String((row as HTMLElement).dataset.batchId))));
  content.querySelector<HTMLInputElement>('#batch-search')?.addEventListener('input', (event) => {
    const keyword = (event.target as HTMLInputElement).value.trim().toLowerCase();
    content.querySelectorAll<HTMLTableRowElement>('[data-batch-id]').forEach((row) => { row.hidden = !row.textContent!.toLowerCase().includes(keyword); });
  });
}

function releaseStageHtml(
  key: string,
  icon: string,
  label: string,
  detail: string,
  stateName: 'idle' | 'active' | 'complete' | 'danger',
): string {
  return `<div class="release-stage release-stage--${stateName}" data-release-stage="${key}">
    <i data-lucide="${icon}"></i><div><strong>${escapeHtml(label)}</strong><span>${escapeHtml(detail)}</span></div>
  </div>`;
}

function batchReleaseProgressHtml(release: BatchRelease): string {
  const gate = release.qualityGate;
  const inbound = release.wmsInbound;
  const inbounded = inbound?.status === 'ACCEPTED' && Boolean(inbound.documentId);
  const qualityState = gate?.state === 'ACCEPTED' ? 'complete'
    : gate?.state === 'REJECTED' ? 'danger'
      : gate?.state === 'WAITING' ? 'active' : 'idle';
  const qualityDetail = gate?.state === 'ACCEPTED' ? '必检项目已合格'
    : gate?.state === 'REJECTED' ? '存在不合格项'
      : gate?.state === 'WAITING' ? '等待检验完成' : '尚未进入质量门';
  const commandState = inbound?.status === 'REJECTED' ? 'danger'
    : inbound?.status === 'PENDING' ? 'active'
      : inbound?.status === 'ACCEPTED' ? 'complete' : 'idle';
  const commandDetail = inbound?.status === 'REJECTED' ? '回执已拒绝'
    : inbound?.status === 'PENDING' ? '等待仓储回执'
      : inbound?.status === 'ACCEPTED' ? '仓储已接受' : '尚未生成命令';
  const completionState = inbounded ? 'complete' : inbound?.status === 'REJECTED' ? 'danger' : 'idle';
  const completionDetail = inbounded ? inbound?.documentId || '单据已生成'
    : inbound?.status === 'REJECTED' ? '需要处理后重试' : '尚未完成';

  return `<div class="release-progress" aria-label="质量与库存进度">
    ${releaseStageHtml('batch', 'database', '原始批次', release.batch.endTime ? '生产事实已闭合' : '生产事实形成中', release.batch.endTime ? 'complete' : 'active')}
    ${releaseStageHtml('quality', 'shield-check', '质量放行', qualityDetail, qualityState)}
    ${releaseStageHtml('command', 'clock-3', '入库指令', commandDetail, commandState)}
    ${releaseStageHtml('inbounded', 'boxes', '完工入库', completionDetail, completionState)}
  </div>`;
}

function qualityReleaseHtml(release: BatchRelease): string {
  const gate = release.qualityGate;
  if (!gate) {
    return `<div class="release-status-block" data-release-quality="NONE">
      <div class="release-summary"><i data-lucide="shield-check"></i><div><span>质量放行</span><strong>尚未进入质量放行</strong><p>当前没有 QCS/LIMS 质量门投影，系统不会据此生成完工入库命令。</p></div>${statusChip('NOT_APPLICABLE')}</div>
    </div>`;
  }

  const required = gate.inspections.filter((inspection) => inspection.required);
  const pendingRequired = required.filter((inspection) => !inspection.finalResult || inspection.disposition === 'PENDING');
  const title = gate.state === 'ACCEPTED' ? '全部必检项目已合格'
    : gate.state === 'REJECTED' ? '存在不合格必检项目'
      : `等待 ${pendingRequired.length} 项必检项目完成`;
  const detail = gate.state === 'ACCEPTED'
    ? '质量门已接受，可依据放行数量生成幂等入库命令。'
    : gate.state === 'REJECTED'
      ? '质量门已拒绝，后续入库动作必须保持阻断。'
      : '仅展示 QCS/LIMS 返回的最终结果，不根据接口成功状态推断放行。';
  const inspections = gate.inspections.map((inspection) => {
    const label = inspection.disposition === 'ACCEPTED' ? '合格'
      : inspection.disposition === 'REJECTED' ? '不合格' : '待判定';
    return `<li data-inspection-code="${escapeHtml(inspection.inspectionCode)}">
      <span class="evidence-state evidence-state--${inspection.disposition === 'ACCEPTED' ? 'ok' : inspection.disposition === 'REJECTED' ? 'bad' : 'pending'}"></span>
      <div><strong>${escapeHtml(inspection.inspectionCode)}</strong><small>${escapeHtml(inspection.inspectionRecordId)} · ${formatTime(inspection.observedAt)}</small></div>
      <em>${inspection.required ? '必检' : '参考'}</em><b>${escapeHtml(label)} · ${inspection.finalResult ? '最终结果' : '待最终确认'}</b>
    </li>`;
  }).join('');

  return `<div class="release-status-block" data-release-quality="${escapeHtml(gate.state)}">
    <div class="release-summary release-summary--${statusTone(gate.state)}"><i data-lucide="shield-check"></i><div><span>质量放行</span><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div>${statusChip(gate.state)}</div>
    <div class="release-facts"><div><span>质量门 / 外部修订</span><b>${escapeHtml(gate.externalGateId)} · r${gate.externalRevision}</b></div><div><span>观测时间</span><b>${formatTime(gate.observedAt)}</b></div><div><span>放行物料</span><b>${escapeHtml(gate.materialCode || '-')}</b></div><div><span>放行数量</span><b>${gate.releaseQuantity == null ? '-' : `${number(gate.releaseQuantity, 3)} ${escapeHtml(gate.quantityUnit || '')}`}</b></div></div>
    <div class="section-title release-subtitle"><h4>检验明细</h4><span>${required.length} 项必检 / ${pendingRequired.length} 项待完成</span></div>
    <ul class="inspection-list">${inspections || '<li class="release-empty">暂无检验明细</li>'}</ul>
    <dl class="release-technical"><div><dt>质量事件</dt><dd>${escapeHtml(gate.sourceEventId)}</dd></div><div><dt>投影 ID</dt><dd>${escapeHtml(gate.id)}</dd></div></dl>
  </div>`;
}

function wmsReconciliationBlockedLabel(reason: WmsInbound['reconciliationBlockedReason']): string {
  const labels: Partial<Record<NonNullable<WmsInbound['reconciliationBlockedReason']>, string>> = {
    BATCH_NOT_RELEASED: '批次不在待入库状态',
    WMS_RECEIPT_TERMINAL: 'WMS 已返回终态回执',
    ADMIN_ROLE_REQUIRED: '仅 BPI 管理员可发起核对',
    PHASE2_DISABLED: '质量与仓储联动当前已关闭',
    COMMANDS_DISABLED: '人工命令开关当前已关闭',
    WMS_LINK_DISABLED: 'WMS 联动开关当前已关闭',
    OUTBOX_BUSY: '原命令正在队列中处理',
    SAFETY_DELAY_ACTIVE: '仍在回执安全等待期',
  };
  return reason ? labels[reason] || '当前不可重新核对' : '';
}

function wmsReleaseHtml(release: BatchRelease): string {
  const inbound = release.wmsInbound;
  if (!inbound) {
    return `<div class="release-status-block" data-release-wms="NONE">
      <div class="release-summary"><i data-lucide="boxes"></i><div><span>完工入库</span><strong>尚未生成入库命令</strong><p>只有质量门明确接受后，BPI 才能形成带幂等键的 WMS 完工入库命令。</p></div>${statusChip('NOT_REQUESTED')}</div>
    </div>`;
  }

  const inbounded = inbound.status === 'ACCEPTED' && Boolean(inbound.documentId);
  const title = inbounded ? '已入库'
    : inbound.status === 'ACCEPTED' ? '回执不完整'
      : inbound.status === 'REJECTED' ? '入库失败' : '入库处理中';
  const detail = inbounded ? `仓储单据 ${inbound.documentId} 已作为持久化回执返回。`
    : inbound.status === 'ACCEPTED' ? 'WMS 返回 ACCEPTED 但缺少持久化单据号，系统按异常保持阻断。'
      : inbound.status === 'REJECTED' ? (inbound.detail || '仓储系统拒绝了本次入库命令。')
        : inbound.outboxStatus === 'FAILED' ? '原命令派发失败，尚未产生终态回执；重新核对仍会沿用同一命令和幂等键。'
          : inbound.outboxStatus === 'PENDING' || inbound.outboxStatus === 'DISPATCHING'
            ? '原命令正在安全队列中处理，系统不会创建第二张入库单。'
            : '命令已持久化并等待 WMS 返回最终回执，不能按 HTTP 200 推断已入库。';
  const businessStatus = inbounded ? 'INBOUNDED' : inbound.status === 'ACCEPTED' ? 'BLOCKED' : inbound.status;
  const reconcileAction = inbound.reconciliationAllowed
    ? `<div class="release-reconciliation"><div><strong>回执长时间未闭合</strong><span>重新核对会查询并重放原命令，不会生成新的入库指令。</span></div><button class="button button--secondary button--compact" id="open-wms-reconcile"><i data-lucide="refresh-cw"></i>重新核对原单</button></div>`
    : inbound.status === 'PENDING'
      ? `<div class="release-reconciliation release-reconciliation--blocked"><i data-lucide="clock-3"></i><span>${escapeHtml(wmsReconciliationBlockedLabel(inbound.reconciliationBlockedReason))}${inbound.reconciliationBlockedReason === 'SAFETY_DELAY_ACTIVE' ? `，${formatTime(inbound.reconcileAfter)} 后可操作` : ''}</span></div>`
      : '';

  return `<div class="release-status-block" data-release-wms="${escapeHtml(inbound.status)}">
    <div class="release-summary release-summary--${statusTone(businessStatus)}"><i data-lucide="boxes"></i><div><span>完工入库</span><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div>${statusChip(businessStatus)}</div>
    <div class="release-facts"><div><span>仓储单据</span><b>${escapeHtml(inbound.documentId || '-')}</b></div><div><span>回执时间</span><b>${formatTime(inbound.observedAt)}</b></div><div><span>消息状态</span><b>${escapeHtml(inbound.outboxStatus)}</b></div><div><span>投递 / 人工核对</span><b>${inbound.deliveryAttemptCount} / ${inbound.reconciliationCount}</b></div><div><span>错误编码</span><b>${escapeHtml(inbound.errorCode || '-')}</b></div><div><span>回执修订</span><b>r${inbound.revision}</b></div></div>
    <dl class="release-technical"><div><dt>命令事件</dt><dd>${escapeHtml(inbound.commandEventId)}</dd></div><div><dt>幂等键</dt><dd>${escapeHtml(inbound.idempotencyKey)}</dd></div><div><dt>回执事件</dt><dd>${escapeHtml(inbound.receiptEventId || '-')}</dd></div></dl>
    ${reconcileAction}
  </div>`;
}

function wmsReversalHtml(release: BatchRelease): string {
  const task = release.wmsInboundReversal;
  if (!task) {
    if (release.batch.state !== 'INBOUNDED') return '';
    return `<div class="release-status-block release-reversal" data-release-reversal="AVAILABLE">
      <div class="release-summary"><i data-lucide="rotate-ccw"></i><div><span>完工入库冲销</span><strong>原入库单可申请冲销</strong><p>冲销采用申请、独立审批和 WMS 红单回执三段闭环；原蓝单及其命令事实只读保留。</p></div>${statusChip('AVAILABLE')}</div>
      <div class="release-reversal-action"><div><strong>原蓝单不可覆盖</strong><span>当前持久化单据 ${escapeHtml(release.wmsInbound?.documentId || '-')}，批准后将追加独立红单命令。</span></div><button class="button button--danger button--compact" id="open-wms-reversal-request"><i data-lucide="rotate-ccw"></i>申请入库冲销</button></div>
    </div>`;
  }

  const pendingApproval = task.state === 'PENDING_APPROVAL';
  const pendingWms = task.state === 'PENDING_WMS';
  const completed = task.state === 'COMPLETED';
  const title = pendingApproval ? '等待独立管理员审批'
    : pendingWms ? '红单命令等待 WMS 回执'
      : completed ? '完工入库已冲销' : 'WMS 拒绝冲销';
  const detail = pendingApproval
    ? '原入库事实保持不变，申请人不能批准自己的冲销申请。'
    : pendingWms
      ? '独立红单命令已持久化；只有 WMS 返回持久化红单号后，系统才会标记冲销完成。'
      : completed
        ? `WMS 红单 ${task.reversalDocumentId || '-'} 已落回，原蓝单仍保留供审计和追溯。`
        : (task.detail || 'WMS 未接受本次红单命令，批次已恢复为可重新申请状态。');
  const businessStatus = completed ? 'INBOUND_REVERSED'
    : task.state === 'FAILED' ? 'REVERSAL_FAILED' : task.state;
  const action = pendingApproval
    ? `<div class="release-reversal-action"><div><strong>四眼审批待办</strong><span>请由不同于 ${escapeHtml(task.requestedBy)} 的 BPI 管理员核对原单和申请依据。</span></div><button class="button button--danger button--compact" id="open-wms-reversal-approve"><i data-lucide="shield-check"></i>独立审批冲销</button></div>`
    : task.state === 'FAILED' && release.batch.state === 'INBOUNDED'
      ? `<div class="release-reversal-action"><div><strong>冲销失败可重新申请</strong><span>先处理 WMS 错误 ${escapeHtml(task.errorCode || '-')}，新申请会生成新的审批任务。</span></div><button class="button button--danger button--compact" id="open-wms-reversal-request"><i data-lucide="rotate-ccw"></i>重新申请冲销</button></div>`
      : `<div class="release-reversal-assurance"><i data-lucide="lock-keyhole"></i><span>原蓝单 ${escapeHtml(task.originalDocumentId)} 及原命令 ${escapeHtml(task.originalCommandEventId)} 始终只读保留。</span></div>`;

  return `<div class="release-status-block release-reversal" data-release-reversal="${escapeHtml(task.state)}">
    <div class="release-summary release-summary--${statusTone(businessStatus)}"><i data-lucide="rotate-ccw"></i><div><span>完工入库冲销</span><strong>${escapeHtml(title)}</strong><p>${escapeHtml(detail)}</p></div>${statusChip(businessStatus)}</div>
    <div class="release-facts"><div><span>原蓝单</span><b data-original-document>${escapeHtml(task.originalDocumentId)}</b></div><div><span>冲销红单</span><b>${escapeHtml(task.reversalDocumentId || '-')}</b></div><div><span>申请人 / 时间</span><b>${escapeHtml(task.requestedBy)} · ${formatTime(task.requestedAt)}</b></div><div><span>审批人 / 时间</span><b>${escapeHtml(task.decidedBy || '-')} · ${formatTime(task.decidedAt)}</b></div><div><span>申请依据</span><b>${escapeHtml(task.requestReason)}</b></div><div><span>红单消息状态</span><b>${escapeHtml(task.outboxStatus || 'NOT_CREATED')} · ${task.deliveryAttemptCount} 次</b></div></div>
    <dl class="release-technical"><div><dt>冲销任务</dt><dd>${escapeHtml(task.taskId)}</dd></div><div><dt>红单命令</dt><dd>${escapeHtml(task.reversalCommandEventId || '-')}</dd></div><div><dt>红单幂等键</dt><dd>${escapeHtml(task.reversalIdempotencyKey || '-')}</dd></div><div><dt>红单回执</dt><dd>${escapeHtml(task.reversalReceiptEventId || '-')}</dd></div></dl>
    ${action}
  </div>`;
}

function batchReleaseSectionHtml(): string {
  if (state.batchReleaseLoading) {
    return `<div class="drawer-section batch-release-section"><div class="section-title"><h3>质量与库存</h3><span>正在核对权威投影</span></div><div class="batch-detail-loading" role="status"><i data-lucide="refresh-cw"></i><div><strong>正在读取质量门和入库回执</strong><span>批次核心事实已保留，等待独立投影返回。</span></div></div></div>`;
  }
  if (state.batchReleaseError) {
    const traceId = state.batchReleaseError instanceof ApiProblem ? state.batchReleaseError.problem.traceId : null;
    return `<div class="drawer-section batch-release-section"><div class="section-title"><h3>质量与库存</h3><span>局部查询失败</span></div><div class="release-error" data-batch-release-error><i data-lucide="circle-alert"></i><div><strong>质量与库存暂不可用</strong><p>${escapeHtml(state.batchReleaseError.message)}</p>${traceId ? `<small>traceId ${escapeHtml(traceId)}</small>` : ''}</div><button class="button button--secondary button--compact" id="retry-batch-release"><i data-lucide="refresh-cw"></i>重试</button></div></div>`;
  }
  const release = state.selectedBatchRelease;
  if (!release) return '';
  return `<div class="drawer-section batch-release-section" data-batch-release>
    <div class="section-title"><h3>质量与库存</h3><span>QCS/LIMS → BPI → WMS</span></div>
    ${batchReleaseProgressHtml(release)}
    <div class="release-status-flow">${qualityReleaseHtml(release)}${wmsReleaseHtml(release)}${wmsReversalHtml(release)}</div>
  </div>`;
}

function forceCloseTaskHtml(): string {
  const task = state.selectedForceCloseTask;
  if (!task) return '';
  const pending = task.state === 'PENDING_APPROVAL';
  const title = pending ? '强制结束待独立审批' : '强制结束已完成';
  const detail = pending
    ? '批次运行命令已冻结。管理员必须核对同一边界时间，且申请人不能批准自己的申请。'
    : `批次已按批准边界 ${formatTime(task.boundaryTime)} 关闭。`;
  return `<div class="drawer-section force-close-task force-close-task--${pending ? 'pending' : 'completed'}" data-force-close-state="${escapeHtml(task.state)}">
    <div class="force-close-task__header"><i data-lucide="${pending ? 'clock-3' : 'check-circle-2'}"></i><div><span>高风险批次命令</span><strong>${title}</strong><p>${escapeHtml(detail)}</p></div>${statusChip(task.state)}</div>
    <div class="force-close-task__facts"><div><span>任务 ID</span><b>${escapeHtml(task.taskId)}</b></div><div><span>拟定边界</span><b>${formatTime(task.boundaryTime)}</b></div><div><span>申请人 / 时间</span><b>${escapeHtml(task.requestedBy)} · ${formatTime(task.requestedAt)}</b></div><div><span>申请依据</span><b>${escapeHtml(task.requestReason)}</b></div></div>
  </div>`;
}

function renderBatchDrawer(): void {
  const batch = state.selectedBatch;
  if (!batch) return;
  const evidence = [...state.batchEvidence.start, ...state.batchEvidence.end].map((item) => `<li><span class="evidence-state evidence-state--${item.satisfied ? 'ok' : 'bad'}"></span><div><strong>${escapeHtml(item.signal)}</strong><small>${escapeHtml(item.source)} · ${formatTime(item.eventTime)}</small></div><b>${escapeHtml(item.value)}${item.unit ? ` ${escapeHtml(item.unit)}` : ''}</b></li>`).join('');
  const timeline = state.timeline.map((item) => `<li><i></i><div><strong>${escapeHtml(item.action)}</strong><span>${escapeHtml(item.reason || '-')}</span><small>${formatTime(item.at || item.eventTime)} · ${escapeHtml(item.actor || item.actorId || '-')}</small></div></li>`).join('');
  const pendingForceClose = state.selectedForceCloseTask?.state === 'PENDING_APPROVAL';
  const runtimeCommand = pendingForceClose ? '' : batch.state === 'ACTIVE'
    ? '<button class="button button--secondary" id="open-suspend">暂停自动处理</button>'
    : batch.state === 'SUSPENDED'
      ? '<button class="button button--secondary" id="open-resume">恢复自动处理</button>'
      : '';
  const forceCloseCommand = pendingForceClose
    ? '<button class="button button--danger" id="open-force-close-approve">批准并强制结束</button>'
    : (batch.state === 'ACTIVE' || batch.state === 'SUSPENDED')
      ? '<button class="button button--danger" id="open-force-close-request">申请强制结束</button>'
      : '';
  openDrawer(`<header><div><span>批次档案</span><h2>${escapeHtml(batch.batchNo)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="batch-state-band"><div>${statusChip(batch.state)}${batch.shadow ? '<span class="shadow-label">SHADOW</span>' : ''}</div><span>revision ${batch.revision}</span></div><div class="drawer-section facts-grid"><div><span>产线 / 工段</span><b>${escapeHtml(batch.lineId)} / ${escapeHtml(batch.stageCode)}</b></div><div><span>生产指令</span><b>${escapeHtml(batch.orderId || '-')}</b></div><div><span>开始时间</span><b>${formatTime(batch.startTime)}</b></div><div><span>结束时间</span><b>${formatTime(batch.endTime)}</b></div><div><span>累计量</span><b>${number(batch.quantity)} ${escapeHtml(batch.quantityUnit)}</b></div><div><span>干物量</span><b>${number(batch.dryMatter)} ${escapeHtml(batch.quantityUnit)}</b></div><div><span>质量门</span>${statusChip(batch.qualityGate)}</div><div><span>库存状态</span>${statusChip(batch.wmsStatus)}</div></div>${forceCloseTaskHtml()}${batchReleaseSectionHtml()}<div class="drawer-section"><div class="section-title"><h3>边界证据</h3><span>${state.batchEvidence.start.length} START / ${state.batchEvidence.end.length} END</span></div><ul class="evidence-list evidence-list--compact">${evidence || '<li class="evidence-empty">暂无证据</li>'}</ul></div><div class="drawer-section"><h3>状态时间线</h3><ol class="timeline">${timeline || '<li><i></i><div><strong>暂无状态事件</strong></div></li>'}</ol></div><footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button>${runtimeCommand}${forceCloseCommand}</footer>`, `batch:${batch.id}`);
  document.querySelector('#open-suspend')?.addEventListener('click', () => openBatchCommandDialog('suspend'));
  document.querySelector('#open-resume')?.addEventListener('click', () => openBatchCommandDialog('resume'));
  document.querySelector('#open-force-close-request')?.addEventListener('click', () => openBatchCommandDialog('forceCloseRequest'));
  document.querySelector('#open-force-close-approve')?.addEventListener('click', () => openBatchCommandDialog('forceCloseApprove'));
  document.querySelector('#open-wms-reconcile')?.addEventListener('click', () => openBatchCommandDialog('reconcileWms'));
  document.querySelector('#open-wms-reversal-request')?.addEventListener('click', () => openBatchCommandDialog('requestWmsReversal'));
  document.querySelector('#open-wms-reversal-approve')?.addEventListener('click', () => openBatchCommandDialog('approveWmsReversal'));
  document.querySelector('#retry-batch-release')?.addEventListener('click', () => void reloadBatchRelease(batch.id));
}

async function reloadBatchRelease(batchId: string): Promise<void> {
  const generation = batchRequestGeneration;
  state.batchReleaseLoading = true;
  state.batchReleaseError = null;
  renderBatchDrawer();
  try {
    const response = await bpiApi.batchRelease(batchId);
    if (!batchDrawerIsCurrent(batchId, generation)) return;
    state.selectedBatchRelease = response.data;
    state.selectedBatch = response.data.batch;
  } catch (error) {
    if (!batchDrawerIsCurrent(batchId, generation)) return;
    state.batchReleaseError = error instanceof Error ? error : new Error(String(error));
  } finally {
    if (batchDrawerIsCurrent(batchId, generation)) {
      state.batchReleaseLoading = false;
      renderBatchDrawer();
    }
  }
}

function batchDrawerIsCurrent(batchId: string, generation: number): boolean {
  return generation === batchRequestGeneration
    && state.selectedBatch?.id === batchId
    && activeDrawerKey === `batch:${batchId}`;
}

async function openBatch(batchId: string): Promise<void> {
  const generation = ++batchRequestGeneration;
  const knownBatch = state.batches.find((batch) => batch.id === batchId) || null;
  state.selectedBatch = knownBatch;
  state.selectedBatchRelease = null;
  state.selectedForceCloseTask = null;
  state.batchReleaseLoading = true;
  state.batchReleaseError = null;
  state.batchEvidence = { start: [], end: [] };
  state.timeline = [];
  openDrawer(`<header><div><span>批次档案</span><h2>${escapeHtml(knownBatch?.batchNo || '正在加载')}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="batch-detail-loading batch-detail-loading--page" role="status"><i data-lucide="refresh-cw"></i><div><strong>正在核对批次档案</strong><span>同步读取批次事实、边界证据、质量门、入库回执和状态时间线。</span></div></div>`, `batch:${batchId}`);
  try {
    const releaseRequest = bpiApi.batchRelease(batchId)
      .then((response) => ({ response, error: null as Error | null }))
      .catch((error) => ({ response: null, error: error instanceof Error ? error : new Error(String(error)) }));
    const [batchResponse, evidenceResponse, timelineResponse, forceCloseResponse] = await Promise.all([
      bpiApi.batch(batchId),
      bpiApi.evidence(batchId),
      bpiApi.timeline(batchId),
      bpiApi.forceCloseTask(batchId),
    ]);
    if (generation !== batchRequestGeneration || activeDrawerKey !== `batch:${batchId}`) return;
    state.selectedBatch = batchResponse.data;
    state.batchEvidence = evidenceResponse.data;
    state.timeline = timelineResponse.data;
    state.selectedForceCloseTask = forceCloseResponse.data;
    renderBatchDrawer();

    const releaseResult = await releaseRequest;
    if (!batchDrawerIsCurrent(batchId, generation)) return;
    state.selectedBatchRelease = releaseResult.response?.data || null;
    state.selectedBatch = releaseResult.response?.data.batch || batchResponse.data;
    state.batchReleaseError = releaseResult.error;
    state.batchReleaseLoading = false;
    renderBatchDrawer();
  } catch (error) {
    if (generation !== batchRequestGeneration || activeDrawerKey !== `batch:${batchId}`) return;
    const message = error instanceof Error ? error.message : String(error);
    state.batchReleaseLoading = false;
    openDrawer(`<header><div><span>批次档案</span><h2>${escapeHtml(knownBatch?.batchNo || batchId)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="drawer-section"><div class="release-error release-error--page"><i data-lucide="circle-alert"></i><div><strong>批次档案暂不可用</strong><p>${escapeHtml(message)}</p></div><button class="button button--secondary button--compact" id="retry-batch-core"><i data-lucide="refresh-cw"></i>重试</button></div></div>`, `batch:${batchId}`);
    document.querySelector('#retry-batch-core')?.addEventListener('click', () => void openBatch(batchId));
    showToast(message, true);
  }
}

function openBatchCommandDialog(command: 'suspend' | 'resume' | 'reconcileWms' | 'forceCloseRequest' | 'forceCloseApprove' | 'requestWmsReversal' | 'approveWmsReversal'): void {
  const batch = state.selectedBatch;
  if (!batch) return;
  const isSuspend = command === 'suspend';
  const isReconciliation = command === 'reconcileWms';
  const isForceCloseRequest = command === 'forceCloseRequest';
  const isForceCloseApprove = command === 'forceCloseApprove';
  const isForceClose = isForceCloseRequest || isForceCloseApprove;
  const isReversalRequest = command === 'requestWmsReversal';
  const isReversalApprove = command === 'approveWmsReversal';
  const isReversal = isReversalRequest || isReversalApprove;
  const release = state.selectedBatchRelease;
  const inbound = release?.wmsInbound;
  const reversal = release?.wmsInboundReversal;
  const forceCloseTask = state.selectedForceCloseTask;
  if (isReconciliation && (!inbound || !inbound.reconciliationAllowed)) return;
  if (isForceCloseApprove && forceCloseTask?.state !== 'PENDING_APPROVAL') return;
  if (isReversalRequest && (batch.state !== 'INBOUNDED'
      || !inbound?.documentId || (reversal && reversal.state !== 'FAILED'))) return;
  if (isReversalApprove && reversal?.state !== 'PENDING_APPROVAL') return;
  state.candidateCommand = null;
  state.batchCommand = command;
  state.ruleCommand = null;
  state.topologyCommand = null;
  state.calibrationCommand = null;
  document.querySelector('#command-kicker')!.textContent = isReconciliation
    ? '完工入库核对' : isReversal ? '完工入库冲销' : isForceClose ? '高风险批次命令' : '批次运行控制';
  document.querySelector('#command-title')!.textContent = isReconciliation
    ? '重新核对原 WMS 单据'
    : isReversalRequest ? '申请完工入库冲销'
      : isReversalApprove ? '批准完工入库冲销'
        : isForceCloseRequest ? '申请强制结束批次'
          : isForceCloseApprove ? '批准强制结束批次'
            : isSuspend ? '暂停批次自动处理' : '恢复批次自动处理';
  document.querySelector('#command-reason-label')!.textContent = isReconciliation
    ? '核对原因'
    : isReversalRequest ? '冲销申请依据'
      : isReversalApprove ? '独立审批依据'
        : isForceCloseRequest ? '申请依据'
          : isForceCloseApprove ? '独立审批依据'
            : isSuspend ? '暂停原因' : '恢复原因';
  document.querySelector('#command-summary')!.innerHTML = isReconciliation && inbound
    ? `<div><span>批次</span><b>${escapeHtml(batch.batchNo)}</b></div><div><span>命令事件</span><b>${escapeHtml(inbound.commandEventId)}</b></div><div><span>处理策略</span><b>先查原单 · 同一幂等键</b></div><div><span>入库投影版本</span><b>r${inbound.revision}</b></div>`
    : isReversal
      ? `<div><span>批次</span><b>${escapeHtml(batch.batchNo)}</b></div><div><span>原蓝单</span><b>${escapeHtml(reversal?.originalDocumentId || inbound?.documentId || '-')}</b></div><div><span>${isReversalRequest ? '处理阶段' : '申请人'}</span><b>${isReversalRequest ? 'INBOUNDED → 待独立审批' : escapeHtml(reversal?.requestedBy || '-')}</b></div><div><span>${isReversalRequest ? '批次版本' : '申请依据'}</span><b>${isReversalRequest ? `r${batch.revision}` : escapeHtml(reversal?.requestReason || '-')}</b></div>`
      : isForceClose
        ? `<div><span>批次</span><b>${escapeHtml(batch.batchNo)}</b></div><div><span>产线</span><b>${escapeHtml(batch.lineId)}</b></div><div><span>处理阶段</span><b>${isForceCloseRequest ? `${escapeHtml(batch.state)} → 待独立审批` : `${escapeHtml(batch.state)} → CLOSED_RAW`}</b></div><div><span>批次版本</span><b>r${batch.revision}</b></div>`
        : `<div><span>批次</span><b>${escapeHtml(batch.batchNo)}</b></div><div><span>产线</span><b>${escapeHtml(batch.lineId)}</b></div><div><span>状态变化</span><b>${isSuspend ? 'ACTIVE → SUSPENDED' : 'SUSPENDED → ACTIVE'}</b></div><div><span>版本</span><b>r${batch.revision}</b></div>`;
  setCommandBoundaryVisible(
    isForceClose,
    isForceCloseApprove && forceCloseTask
      ? toLocalDateTimeValue(forceCloseTask.boundaryTime)
      : toLocalDateTimeValue(new Date().toISOString()),
  );
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = isReconciliation
    ? '填写回执超时、消息失败或人工查单依据'
    : isReversalRequest ? '填写退库、错单或业务纠正依据，至少 3 个字符'
      : isReversalApprove ? '填写独立核对原蓝单、物料和数量后的审批依据'
        : isForceCloseRequest ? '填写停产、切罐、设备故障或现场异常依据'
          : isForceCloseApprove ? '填写独立核对边界时间和现场事实的依据'
            : isSuspend ? '填写上下文过期、数据冲突或现场处置依据' : '填写上下文恢复或人工复核依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = `button ${isSuspend || isForceClose || isReversal ? 'button--danger' : 'button--primary'}`;
  button.textContent = isReconciliation ? '确认核对原单'
    : isReversalRequest ? '提交独立审批'
      : isReversalApprove ? '批准并生成红单'
        : isForceCloseRequest ? '提交独立审批'
          : isForceCloseApprove ? '批准并关闭批次'
            : isSuspend ? '确认暂停' : '确认恢复';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

async function handleBatchCommand(): Promise<void> {
  const batch = state.selectedBatch;
  const command = state.batchCommand;
  const release = state.selectedBatchRelease;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  const forceCloseTask = state.selectedForceCloseTask;
  const isForceClose = command === 'forceCloseRequest' || command === 'forceCloseApprove';
  const isReversal = command === 'requestWmsReversal' || command === 'approveWmsReversal';
  const reversalTask = release?.wmsInboundReversal;
  const boundaryValue = document.querySelector<HTMLInputElement>('#command-boundary-time')!.value;
  const boundaryTime = boundaryValue ? new Date(boundaryValue).toISOString() : '';
  if (!batch || !command || reason.length < 3 || (command === 'reconcileWms' && !release)
      || (command === 'forceCloseApprove' && forceCloseTask?.state !== 'PENDING_APPROVAL')
      || (command === 'requestWmsReversal' && (batch.state !== 'INBOUNDED'
        || !release?.wmsInbound?.documentId || (reversalTask && reversalTask.state !== 'FAILED')))
      || (command === 'approveWmsReversal' && reversalTask?.state !== 'PENDING_APPROVAL')
      || (isForceClose && !boundaryTime)) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = command === 'reconcileWms' ? '核对排队中...'
    : command === 'requestWmsReversal' ? '提交冲销审批中...'
      : command === 'approveWmsReversal' ? '生成红单中...'
        : command === 'forceCloseRequest' ? '提交审批中...'
          : command === 'forceCloseApprove' ? '审批关闭中...'
            : command === 'suspend' ? '暂停中...' : '恢复中...';
  try {
    if (command === 'reconcileWms') {
      const response = await bpiApi.reconcileWmsInbound(release!, reason, commandId());
      state.selectedBatchRelease = response.data;
      state.selectedBatch = response.data.batch;
      state.batches = state.batches.map((item) => item.id === response.data.batch.id ? response.data.batch : item);
    } else if (isReversal) {
      await bpiApi.commandWmsInboundReversal(batch, {
        reason,
        approvalMode: command === 'requestWmsReversal' ? 'REQUEST' : 'APPROVE',
      }, commandId());
    } else if (isForceClose) {
      const response = await bpiApi.forceCloseBatch(batch, {
        reason,
        boundaryTime,
        approvalMode: command === 'forceCloseRequest' ? 'REQUEST' : 'APPROVE',
      }, commandId());
      state.selectedForceCloseTask = response.data;
    } else {
      const response = command === 'suspend'
        ? await bpiApi.suspendBatch(batch, reason, commandId())
        : await bpiApi.resumeBatch(batch, reason, commandId());
      state.selectedBatch = response.data;
      state.batches = state.batches.map((item) => item.id === response.data.id ? response.data : item);
      state.lines = state.lines.map((line) => line.lineId === response.data.lineId
        ? { ...line, status: command === 'suspend' ? 'BLOCKED' : 'RUNNING' }
        : line);
    }
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    state.batchCommand = null;
    showToast(command === 'reconcileWms'
      ? '原入库命令已进入重新核对队列'
      : command === 'requestWmsReversal' ? '入库冲销申请已提交，等待独立管理员审批'
        : command === 'approveWmsReversal' ? '冲销已批准，红单命令已进入 WMS 队列'
          : command === 'forceCloseRequest' ? '强制结束申请已提交，等待独立管理员审批'
            : command === 'forceCloseApprove' ? '强制结束已批准，批次已关闭为 CLOSED_RAW'
              : command === 'suspend' ? '批次自动处理已暂停' : '批次自动处理已恢复');
    await loadView(true);
    await openBatch(batch.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`${command === 'reconcileWms' ? '入库投影' : isReversal ? '冲销批次' : '批次'}已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openBatch(batch.id);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.className = `button ${command === 'suspend' || isForceClose || isReversal ? 'button--danger' : 'button--primary'}`;
    button.textContent = command === 'reconcileWms' ? '确认核对原单'
      : command === 'requestWmsReversal' ? '提交独立审批'
        : command === 'approveWmsReversal' ? '批准并生成红单'
          : command === 'forceCloseRequest' ? '提交独立审批'
            : command === 'forceCloseApprove' ? '批准并关闭批次'
              : command === 'suspend' ? '确认暂停' : '确认恢复';
  }
}

function datasetListValues(value: string, required = true): string[] {
  const values = value.split(/[,，]/).map((item) => item.trim()).filter(Boolean).sort();
  if (required && values.length === 0) throw new Error('至少填写一个值');
  if (new Set(values).size !== values.length) throw new Error('列表中不能包含重复值');
  return values;
}

function checkedDatasetRefs(selector: string): string[] {
  return Array.from(document.querySelectorAll<HTMLInputElement>(selector))
    .filter((input) => input.checked)
    .map((input) => input.value)
    .sort();
}

function renderDatasets(): void {
  const content = document.querySelector('#content')!;
  const readyCount = state.datasets.filter((dataset) => dataset.latestSnapshot?.state === 'MANIFEST_READY').length;
  const parquetReadyCount = state.datasets.filter((dataset) => dataset.latestSnapshot?.materializationState === 'READY').length;
  const activeCount = state.datasets.filter((dataset) => ['QUEUED', 'BUILDING'].includes(dataset.latestSnapshot?.state || '')
    || ['QUEUED', 'WRITING'].includes(dataset.latestSnapshot?.materializationState || '')).length;
  const failedCount = state.datasets.filter((dataset) => dataset.latestSnapshot?.state === 'FAILED'
    || dataset.latestSnapshot?.materializationState === 'FAILED').length;
  const toolbar = `<div class="toolbar dataset-toolbar"><div class="dataset-phase-badge"><i data-lucide="database"></i><span>PARQUET</span><b>VERSION-PINNED</b></div><div class="toolbar-actions"><button id="open-dataset-definition" class="icon-text-button"><i data-lucide="plus"></i>新建定义</button></div></div>`;
  const summary = `<div class="dataset-summary"><div><span>数据集定义</span><b>${state.datasets.length}</b></div><div><span>清单已就绪</span><b>${readyCount}</b></div><div><span>Parquet 已就绪</span><b>${parquetReadyCount}</b></div><div><span>后台处理中</span><b>${activeCount}</b></div><div><span>失败任务</span><b>${failedCount}</b></div></div>`;
  const rows = state.datasets.map((dataset) => {
    const latest = dataset.latestSnapshot;
    return `<tr data-dataset-id="${escapeHtml(dataset.id)}" tabindex="0"><td><strong>${escapeHtml(dataset.name)}</strong><small>${escapeHtml(dataset.datasetCode)}@${escapeHtml(dataset.version)}</small></td><td>${escapeHtml(dataset.lineIds.join(', '))}</td><td>${escapeHtml(dataset.predictionTimePolicy)}</td><td>${dataset.featureRefs.length} / ${dataset.labelRefs.length}</td><td>${number(dataset.minimumConfidence * 100, 0)}%</td><td>${latest ? statusChip(latest.state) : statusChip('NOT_CREATED')}</td><td>${latest ? statusChip(latest.materializationState) : statusChip('NOT_STARTED')}</td><td>${latest ? `v${latest.snapshotVersion} · ${formatTime(latest.freezeAt)}` : '-'}</td><td>${latest?.manifestChecksum ? `<code class="dataset-checksum">${escapeHtml(latest.manifestChecksum.slice(0, 12))}</code>` : '-'}</td><td><button class="icon-text-button dataset-row-action" data-create-dataset-snapshot="${escapeHtml(dataset.id)}"><i data-lucide="play"></i>生成清单</button></td></tr>`;
  }).join('');
  content.innerHTML = state.datasets.length
    ? `${toolbar}${summary}<div class="table-frame"><table class="dataset-table"><thead><tr><th>数据集</th><th>产线</th><th>预测时点</th><th>特征 / 标签</th><th>最低置信度</th><th>清单状态</th><th>Parquet</th><th>最近快照</th><th>清单校验和</th><th>操作</th></tr></thead><tbody>${rows}</tbody></table></div>`
    : `${toolbar}${summary}<div class="empty-state"><i data-lucide="boxes"></i><strong>尚未建立数据集定义</strong><span>当前工厂没有不可变数据集定义。</span></div>`;
  content.querySelector('#open-dataset-definition')?.addEventListener('click', openDatasetDefinitionDialog);
  content.querySelectorAll<HTMLElement>('[data-dataset-id]').forEach((row) => row.addEventListener('click', () => {
    const dataset = state.datasets.find((item) => item.id === row.dataset.datasetId);
    if (dataset) openDatasetDefinition(dataset);
  }));
  content.querySelectorAll<HTMLButtonElement>('[data-create-dataset-snapshot]').forEach((button) => button.addEventListener('click', (event) => {
    event.stopPropagation();
    const dataset = state.datasets.find((item) => item.id === button.dataset.createDatasetSnapshot);
    if (dataset) openDatasetSnapshotDialog(dataset);
  }));
}

function openDatasetDefinitionDialog(): void {
  const form = document.querySelector<HTMLFormElement>('#dataset-definition-form')!;
  form.reset();
  document.querySelector<HTMLInputElement>('#dataset-version')!.value = '1.0.0';
  document.querySelector<HTMLInputElement>('#dataset-lines')!.value = state.pointLineId;
  document.querySelector<HTMLInputElement>('#dataset-prediction-policy')!.value = 'AUTOMATIC_BATCH_START';
  document.querySelector<HTMLInputElement>('#dataset-cutoff-policy')!.value = 'AT_OR_BEFORE_PREDICTION_TIME';
  document.querySelector<HTMLInputElement>('#dataset-label-delay')!.value = '48';
  document.querySelector<HTMLInputElement>('#dataset-confidence')!.value = '0.8';
  document.querySelectorAll<HTMLInputElement>('[data-dataset-feature], [data-dataset-label]')
    .forEach((input) => { input.checked = true; });
  document.querySelector<HTMLDialogElement>('#dataset-definition-dialog')!.showModal();
}

async function handleDatasetDefinitionCreate(event: SubmitEvent): Promise<void> {
  event.preventDefault();
  const featureRefs = checkedDatasetRefs('[data-dataset-feature]');
  const labelRefs = checkedDatasetRefs('[data-dataset-label]');
  if (!featureRefs.length || !labelRefs.length) {
    showToast('特征字段和标签字段都至少选择一项', true);
    return;
  }
  let command: DatasetDefinitionCreateCommand;
  try {
    command = {
      datasetCode: document.querySelector<HTMLInputElement>('#dataset-code')!.value.trim(),
      version: document.querySelector<HTMLInputElement>('#dataset-version')!.value.trim(),
      name: document.querySelector<HTMLInputElement>('#dataset-name')!.value.trim(),
      plantId: state.plantId,
      lineIds: datasetListValues(document.querySelector<HTMLInputElement>('#dataset-lines')!.value),
      predictionTimePolicy: 'AUTOMATIC_BATCH_START',
      featureCutoffPolicy: 'AT_OR_BEFORE_PREDICTION_TIME',
      featureRefs,
      labelRefs,
      maxLabelDelayHours: Number(document.querySelector<HTMLInputElement>('#dataset-label-delay')!.value),
      minimumConfidence: Number(document.querySelector<HTMLInputElement>('#dataset-confidence')!.value),
      splitPolicy: 'PRODUCTION_TIME',
      reason: document.querySelector<HTMLTextAreaElement>('#dataset-reason')!.value.trim(),
    };
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
    return;
  }
  const button = document.querySelector<HTMLButtonElement>('#dataset-definition-submit')!;
  button.disabled = true;
  button.textContent = '创建中...';
  try {
    const response = await bpiApi.createDatasetDefinition(command, commandId());
    document.querySelector<HTMLDialogElement>('#dataset-definition-dialog')!.close();
    showToast(`${response.data.datasetCode}@${response.data.version} 已创建`);
    await loadView(true);
    openDatasetDefinition(response.data);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = '创建定义';
  }
}

function openDatasetDefinition(dataset: DatasetDefinition): void {
  state.selectedDataset = dataset;
  const latest = dataset.latestSnapshot;
  const featureItems = dataset.featureRefs.map((reference) => `<li><code>${escapeHtml(reference)}</code></li>`).join('');
  const labelItems = dataset.labelRefs.map((reference) => `<li><code>${escapeHtml(reference)}</code></li>`).join('');
  const latestHtml = latest
    ? `<div class="dataset-latest"><div><span>v${latest.snapshotVersion}</span>${statusChip(latest.state)}${statusChip(latest.materializationState)}</div><b>${formatTime(latest.freezeAt)}</b><code>${escapeHtml(latest.manifestChecksum || '-')}</code><button id="open-latest-dataset-snapshot" class="button button--secondary">查看最近快照</button></div>`
    : '<div class="simulation-empty">尚未生成快照</div>';
  openDrawer(`<header><div><span>不可变数据集定义</span><h2>${escapeHtml(dataset.name)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="batch-state-band"><div>${statusChip(dataset.state)}<span class="shadow-label">POINT-IN-TIME</span></div><span>revision ${dataset.revision}</span></div><div class="drawer-section facts-grid"><div><span>编码 / 版本</span><b>${escapeHtml(dataset.datasetCode)}@${escapeHtml(dataset.version)}</b></div><div><span>工厂 / 产线</span><b>${escapeHtml(dataset.plantId)} / ${escapeHtml(dataset.lineIds.join(', '))}</b></div><div><span>预测时点</span><b>${escapeHtml(dataset.predictionTimePolicy)}</b></div><div><span>特征截止</span><b>${escapeHtml(dataset.featureCutoffPolicy)}</b></div><div><span>标签最大延迟</span><b>${dataset.maxLabelDelayHours} 小时</b></div><div><span>最低置信度</span><b>${number(dataset.minimumConfidence * 100, 0)}%</b></div><div><span>拆分策略</span><b>${escapeHtml(dataset.splitPolicy)}</b></div><div><span>定义 checksum</span><b class="mono-value">${escapeHtml(dataset.checksum)}</b></div></div><div class="drawer-section dataset-ref-columns"><div><div class="section-title"><h3>特征字段</h3><span>${dataset.featureRefs.length} 项</span></div><ul class="dataset-ref-list">${featureItems}</ul></div><div><div class="section-title"><h3>标签字段</h3><span>${dataset.labelRefs.length} 项</span></div><ul class="dataset-ref-list">${labelItems}</ul></div></div><div class="drawer-section"><div class="section-title"><h3>最近快照</h3><span>${latest ? `v${latest.snapshotVersion}` : '无'}</span></div>${latestHtml}</div><footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button><button class="button button--primary" id="open-dataset-snapshot"><i data-lucide="play"></i>生成清单</button></footer>`, `dataset:${dataset.id}`);
  document.querySelector('#open-dataset-snapshot')?.addEventListener('click', () => openDatasetSnapshotDialog(dataset));
  document.querySelector('#open-latest-dataset-snapshot')?.addEventListener('click', () => {
    if (latest) void openDatasetSnapshotById(latest.id);
  });
}

function openDatasetSnapshotDialog(dataset: DatasetDefinition): void {
  state.selectedDataset = dataset;
  const now = new Date(Date.now() - 60_000).toISOString();
  document.querySelector('#dataset-snapshot-summary')!.innerHTML = `<div><span>数据集</span><b>${escapeHtml(dataset.datasetCode)}@${escapeHtml(dataset.version)}</b></div><div><span>定义 revision</span><b>r${dataset.revision}</b></div>`;
  document.querySelector<HTMLInputElement>('#dataset-freeze-at')!.value = toLocalDateTimeValue(now);
  document.querySelector<HTMLInputElement>('#dataset-snapshot-lines')!.value = dataset.lineIds.join(', ');
  document.querySelector<HTMLInputElement>('#dataset-rule-versions')!.value = '';
  document.querySelector<HTMLInputElement>('#dataset-exclude-low-confidence')!.checked = true;
  document.querySelector<HTMLTextAreaElement>('#dataset-snapshot-reason')!.value = '';
  document.querySelector<HTMLDialogElement>('#dataset-snapshot-dialog')!.showModal();
}

async function handleDatasetSnapshotCreate(event: SubmitEvent): Promise<void> {
  event.preventDefault();
  const dataset = state.selectedDataset;
  if (!dataset) return;
  let command: DatasetSnapshotCommand;
  try {
    const freezeAtValue = document.querySelector<HTMLInputElement>('#dataset-freeze-at')!.value;
    const freezeAt = new Date(freezeAtValue);
    if (Number.isNaN(freezeAt.getTime())) throw new Error('冻结时间无效');
    command = {
      freezeAt: freezeAt.toISOString(),
      lineIds: datasetListValues(document.querySelector<HTMLInputElement>('#dataset-snapshot-lines')!.value),
      predictionTimePolicy: dataset.predictionTimePolicy,
      ruleVersionIds: datasetListValues(document.querySelector<HTMLInputElement>('#dataset-rule-versions')!.value, false),
      excludeLowConfidence: document.querySelector<HTMLInputElement>('#dataset-exclude-low-confidence')!.checked,
      reason: document.querySelector<HTMLTextAreaElement>('#dataset-snapshot-reason')!.value.trim(),
    };
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
    return;
  }
  const button = document.querySelector<HTMLButtonElement>('#dataset-snapshot-submit')!;
  button.disabled = true;
  button.textContent = '排队中...';
  try {
    const response = await bpiApi.createDatasetSnapshot(dataset, command, commandId());
    document.querySelector<HTMLDialogElement>('#dataset-snapshot-dialog')!.close();
    const generation = ++datasetSnapshotRequestGeneration;
    state.selectedDatasetSnapshot = response.data;
    state.selectedDatasetCatalogPublication = null;
    showToast(`快照 v${response.data.snapshotVersion} 已进入后台队列`);
    renderDatasetSnapshotDrawer(response.data);
    await pollDatasetSnapshot(response.data.id, generation);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = '生成清单';
  }
}

function datasetSnapshotDrawerIsCurrent(snapshotId: string, generation: number): boolean {
  return generation === datasetSnapshotRequestGeneration
    && state.selectedDatasetSnapshot?.id === snapshotId
    && activeDrawerKey === `dataset-snapshot:${snapshotId}`;
}

async function pollDatasetSnapshot(snapshotId: string, generation: number): Promise<void> {
  for (let attempt = 0; attempt < 12; attempt += 1) {
    if (!datasetSnapshotDrawerIsCurrent(snapshotId, generation)) return;
    const response = await bpiApi.datasetSnapshot(snapshotId);
    if (!datasetSnapshotDrawerIsCurrent(snapshotId, generation)) return;
    state.selectedDatasetSnapshot = response.data;
    renderDatasetSnapshotDrawer(response.data);
    if (['MANIFEST_READY', 'FAILED'].includes(response.data.state)) {
      await loadView(true);
      return;
    }
    await new Promise((resolve) => window.setTimeout(resolve, 600));
  }
  showToast('清单仍在后台构建，可稍后刷新快照', true);
  await loadView(true);
}

function withDatasetMaterialization(
  snapshot: DatasetSnapshot,
  materialization: DatasetMaterialization,
): DatasetSnapshot {
  return {
    ...snapshot,
    materializationState: materialization.state,
    artifactUri: materialization.artifactUri || null,
    latestMaterialization: materialization,
  };
}

function openDatasetMaterializationDialog(
  snapshot: DatasetSnapshot,
  command: 'request' | 'retry',
): void {
  const materialization = snapshot.latestMaterialization || null;
  if (command === 'request' && snapshot.state !== 'MANIFEST_READY') {
    showToast('Parquet 物化要求快照清单已就绪', true);
    return;
  }
  if (command === 'retry' && materialization?.state !== 'FAILED') {
    showToast('只有失败的 Parquet 任务可以重新排队', true);
    return;
  }
  state.selectedDatasetSnapshot = snapshot;
  state.selectedDatasetMaterialization = materialization;
  state.datasetMaterializationCommand = command;
  document.querySelector('#dataset-materialization-title')!.textContent = command === 'retry'
    ? '重新排队 Parquet' : '生成版本锁定对象';
  document.querySelector('#dataset-materialization-summary')!.innerHTML = `<div><span>数据集快照</span><b>${escapeHtml(snapshot.datasetCode)}@${escapeHtml(snapshot.datasetVersion)} / v${snapshot.snapshotVersion}</b></div><div><span>manifest checksum</span><b>${escapeHtml(snapshot.manifestChecksum || '-')}</b></div><div><span>当前状态</span><b>${escapeHtml(materialization?.state || 'NOT_STARTED')}</b></div><div><span>revision</span><b>${command === 'retry' ? materialization?.revision : snapshot.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#dataset-materialization-reason')!;
  reason.value = '';
  const button = document.querySelector<HTMLButtonElement>('#dataset-materialization-submit')!;
  button.textContent = command === 'retry' ? '重新排队' : '生成 Parquet';
  document.querySelector<HTMLDialogElement>('#dataset-materialization-dialog')!.showModal();
  reason.focus();
}

async function handleDatasetMaterializationCommand(event: SubmitEvent): Promise<void> {
  event.preventDefault();
  const snapshot = state.selectedDatasetSnapshot;
  const command = state.datasetMaterializationCommand;
  const materialization = state.selectedDatasetMaterialization;
  if (!snapshot || !command || (command === 'retry' && !materialization)) return;
  const reason = document.querySelector<HTMLTextAreaElement>('#dataset-materialization-reason')!.value.trim();
  if (reason.length < 3) {
    showToast('操作依据至少填写 3 个字符', true);
    return;
  }
  const button = document.querySelector<HTMLButtonElement>('#dataset-materialization-submit')!;
  const idleLabel = command === 'retry' ? '重新排队' : '生成 Parquet';
  button.disabled = true;
  button.textContent = command === 'retry' ? '排队中...' : '提交中...';
  const generation = datasetSnapshotRequestGeneration;
  try {
    const response = command === 'retry'
      ? await bpiApi.retryDatasetMaterialization(materialization!, reason, commandId())
      : await bpiApi.requestDatasetMaterialization(snapshot, reason, commandId());
    document.querySelector<HTMLDialogElement>('#dataset-materialization-dialog')!.close();
    state.selectedDatasetMaterialization = response.data;
    state.datasetMaterializationCommand = null;
    const updatedSnapshot = withDatasetMaterialization(snapshot, response.data);
    if (!datasetSnapshotDrawerIsCurrent(snapshot.id, generation)) {
      showToast(command === 'retry' ? 'Parquet 任务已重新排队' : 'Parquet 任务已进入后台队列');
      return;
    }
    state.selectedDatasetSnapshot = updatedSnapshot;
    renderDatasetSnapshotDrawer(updatedSnapshot);
    showToast(command === 'retry' ? 'Parquet 任务已重新排队' : 'Parquet 任务已进入后台队列');
    await pollDatasetMaterialization(snapshot.id, response.data.id, generation);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = idleLabel;
  }
}

async function pollDatasetMaterialization(
  snapshotId: string,
  materializationId: string,
  generation: number,
): Promise<void> {
  for (let attempt = 0; attempt < 12; attempt += 1) {
    if (!datasetSnapshotDrawerIsCurrent(snapshotId, generation)) return;
    const response = await bpiApi.datasetMaterialization(materializationId);
    if (!datasetSnapshotDrawerIsCurrent(snapshotId, generation)) return;
    state.selectedDatasetMaterialization = response.data;
    state.selectedDatasetSnapshot = withDatasetMaterialization(state.selectedDatasetSnapshot!, response.data);
    renderDatasetSnapshotDrawer(state.selectedDatasetSnapshot);
    if (['READY', 'FAILED'].includes(response.data.state)) {
      const snapshotResponse = await bpiApi.datasetSnapshot(snapshotId);
      if (!datasetSnapshotDrawerIsCurrent(snapshotId, generation)) return;
      state.selectedDatasetSnapshot = snapshotResponse.data;
      if (response.data.state === 'READY') {
        await loadDatasetCatalogPublication(snapshotResponse.data, generation);
      } else {
        state.selectedDatasetCatalogPublication = null;
        renderDatasetSnapshotDrawer(snapshotResponse.data);
      }
      showToast(response.data.state === 'READY'
        ? `Parquet 已就绪，共 ${response.data.rowCount ?? 0} 行`
        : `Parquet 生成失败：${response.data.failureCode || 'UNKNOWN'}`, response.data.state === 'FAILED');
      await loadView(true);
      return;
    }
    await new Promise((resolve) => window.setTimeout(resolve, 800));
  }
  showToast('Parquet 仍在后台生成，可稍后刷新状态', true);
  await loadView(true);
}

function currentDatasetCatalogPublication(
  materialization: DatasetMaterialization | null,
): DatasetCatalogPublication | null {
  const publication = state.selectedDatasetCatalogPublication;
  return materialization && publication?.materializationId === materialization.id
    ? publication : null;
}

async function loadDatasetCatalogPublication(
  snapshot: DatasetSnapshot,
  generation: number = datasetSnapshotRequestGeneration,
): Promise<DatasetCatalogPublication | null> {
  const materialization = snapshot.latestMaterialization || null;
  if (materialization?.state !== 'READY') {
    if (datasetSnapshotDrawerIsCurrent(snapshot.id, generation)) {
      state.selectedDatasetCatalogPublication = null;
      renderDatasetSnapshotDrawer(snapshot);
    }
    return null;
  }
  const response = await bpiApi.datasetCatalogPublicationForMaterialization(materialization.id);
  if (!datasetSnapshotDrawerIsCurrent(snapshot.id, generation)) return null;
  state.selectedDatasetCatalogPublication = response.data;
  renderDatasetSnapshotDrawer(snapshot);
  return response.data;
}

function openDatasetCatalogPublicationDialog(
  snapshot: DatasetSnapshot,
  command: 'request' | 'retry',
): void {
  const materialization = snapshot.latestMaterialization || null;
  const publication = currentDatasetCatalogPublication(materialization);
  if (materialization?.state !== 'READY') {
    showToast('Iceberg 发布要求 Parquet 对象已就绪并完成版本复验', true);
    return;
  }
  if (command === 'request' && publication) {
    showToast('当前 Parquet 版本已经有目录发布任务', true);
    return;
  }
  if (command === 'retry' && publication?.state !== 'FAILED') {
    showToast('只有失败的 Iceberg 发布任务可以重新排队', true);
    return;
  }
  state.selectedDatasetSnapshot = snapshot;
  state.selectedDatasetMaterialization = materialization;
  state.datasetCatalogPublicationCommand = command;
  document.querySelector('#dataset-catalog-publication-title')!.textContent = command === 'retry'
    ? '重新排队 Iceberg 发布' : '发布版本锁定对象';
  document.querySelector('#dataset-catalog-publication-summary')!.innerHTML = `<div><span>Parquet 对象</span><b>${escapeHtml(materialization.artifactUri || '-')}</b></div><div><span>内容 SHA-256</span><b>${escapeHtml(materialization.contentSha256 || '-')}</b></div><div><span>当前状态</span><b>${escapeHtml(publication?.state || 'NOT_STARTED')}</b></div><div><span>revision</span><b>${command === 'retry' ? publication?.revision : materialization.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#dataset-catalog-publication-reason')!;
  reason.value = '';
  const button = document.querySelector<HTMLButtonElement>('#dataset-catalog-publication-submit')!;
  button.textContent = command === 'retry' ? '重新排队' : '发布 Iceberg';
  document.querySelector<HTMLDialogElement>('#dataset-catalog-publication-dialog')!.showModal();
  reason.focus();
}

async function handleDatasetCatalogPublicationCommand(event: SubmitEvent): Promise<void> {
  event.preventDefault();
  const snapshot = state.selectedDatasetSnapshot;
  const materialization = state.selectedDatasetMaterialization;
  const publication = currentDatasetCatalogPublication(materialization);
  const command = state.datasetCatalogPublicationCommand;
  if (!snapshot || !materialization || !command || (command === 'retry' && !publication)) return;
  const reason = document.querySelector<HTMLTextAreaElement>('#dataset-catalog-publication-reason')!.value.trim();
  if (reason.length < 3) {
    showToast('操作依据至少填写 3 个字符', true);
    return;
  }
  const button = document.querySelector<HTMLButtonElement>('#dataset-catalog-publication-submit')!;
  const idleLabel = command === 'retry' ? '重新排队' : '发布 Iceberg';
  button.disabled = true;
  button.textContent = '提交中...';
  const generation = datasetSnapshotRequestGeneration;
  try {
    const response = command === 'retry'
      ? await bpiApi.retryDatasetCatalogPublication(publication!, reason, commandId())
      : await bpiApi.requestDatasetCatalogPublication(materialization, reason, commandId());
    document.querySelector<HTMLDialogElement>('#dataset-catalog-publication-dialog')!.close();
    if (!datasetSnapshotDrawerIsCurrent(snapshot.id, generation)) {
      showToast(command === 'retry' ? 'Iceberg 发布任务已重新排队' : 'Iceberg 发布任务已进入后台队列');
      return;
    }
    state.selectedDatasetCatalogPublication = response.data;
    state.datasetCatalogPublicationCommand = null;
    renderDatasetSnapshotDrawer(snapshot);
    showToast(command === 'retry' ? 'Iceberg 发布任务已重新排队' : 'Iceberg 发布任务已进入后台队列');
    await pollDatasetCatalogPublication(snapshot.id, response.data.id, generation);
  } catch (error) {
    showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.textContent = idleLabel;
  }
}

async function pollDatasetCatalogPublication(
  snapshotId: string,
  publicationId: string,
  generation: number,
): Promise<void> {
  for (let attempt = 0; attempt < 15; attempt += 1) {
    if (!datasetSnapshotDrawerIsCurrent(snapshotId, generation)) return;
    const response = await bpiApi.datasetCatalogPublication(publicationId);
    if (!datasetSnapshotDrawerIsCurrent(snapshotId, generation)) return;
    state.selectedDatasetCatalogPublication = response.data;
    renderDatasetSnapshotDrawer(state.selectedDatasetSnapshot!);
    if (['READY', 'FAILED'].includes(response.data.state)) {
      showToast(response.data.state === 'READY'
        ? `Iceberg 快照 ${response.data.icebergSnapshotId || '-'} 已复验`
        : `Iceberg 发布失败：${response.data.failureCode || 'UNKNOWN'}`,
      response.data.state === 'FAILED');
      return;
    }
    await new Promise((resolve) => window.setTimeout(resolve, 800));
  }
  showToast('Iceberg 发布仍在后台执行，可稍后刷新状态', true);
}

async function openDatasetSnapshotById(snapshotId: string): Promise<void> {
  const generation = ++datasetSnapshotRequestGeneration;
  const originDrawerKey = activeDrawerKey;
  try {
    const response = await bpiApi.datasetSnapshot(snapshotId);
    if (generation !== datasetSnapshotRequestGeneration
      || state.view !== 'datasets'
      || activeDrawerKey !== originDrawerKey) return;
    state.selectedDatasetSnapshot = response.data;
    state.selectedDatasetCatalogPublication = null;
    renderDatasetSnapshotDrawer(response.data);
    if (['QUEUED', 'BUILDING'].includes(response.data.state)) {
      await pollDatasetSnapshot(snapshotId, generation);
    }
    else if (response.data.latestMaterialization
      && ['QUEUED', 'WRITING'].includes(response.data.latestMaterialization.state)) {
      await pollDatasetMaterialization(
        snapshotId,
        response.data.latestMaterialization.id,
        generation,
      );
    } else if (response.data.latestMaterialization?.state === 'READY') {
      const publication = await loadDatasetCatalogPublication(response.data, generation);
      if (publication && ['QUEUED', 'COMMITTING', 'VERIFYING'].includes(publication.state)) {
        await pollDatasetCatalogPublication(snapshotId, publication.id, generation);
      }
    }
  } catch (error) {
    if (generation === datasetSnapshotRequestGeneration
      && state.view === 'datasets'
      && (activeDrawerKey === originDrawerKey
        || activeDrawerKey === `dataset-snapshot:${snapshotId}`)) {
      showToast(error instanceof Error ? error.message : String(error), true);
    }
  }
}

function datasetMaterializationHtml(snapshot: DatasetSnapshot): string {
  const materialization = snapshot.latestMaterialization || null;
  if (!materialization) {
    return `<div class="drawer-section dataset-materialization-panel" data-materialization-state="NOT_STARTED"><div class="section-title"><h3>Parquet 物化</h3>${statusChip('NOT_STARTED')}</div><div class="simulation-empty">尚未生成 Parquet 对象</div></div>`;
  }
  const metadata = materialization.artifactMetadata || {};
  const objectVersionId = typeof metadata.objectVersionId === 'string' ? metadata.objectVersionId : '-';
  const schemaFields = materialization.schema?.fields;
  const schemaFieldCount = Array.isArray(schemaFields) ? schemaFields.length : 0;
  const evidenceState = metadata.simulationOnly === true
    ? 'SIMULATED' : metadata.objectContentVerified === true ? 'VERIFIED' : 'UNVERIFIED';
  let body = '';
  if (['QUEUED', 'WRITING'].includes(materialization.state)) {
    body = `<div class="batch-detail-loading"><i data-lucide="refresh-cw"></i><div><strong>${materialization.state === 'QUEUED' ? '等待 Worker 领取' : '正在写入并复验对象'}</strong><span>attempt ${materialization.attemptCount} · revision ${materialization.revision}</span></div></div>`;
  } else if (materialization.state === 'FAILED') {
    body = `<div class="dataset-materialization-error"><i data-lucide="circle-alert"></i><div><strong>${escapeHtml(materialization.failureCode || 'MATERIALIZATION_FAILED')}</strong><span>${escapeHtml(materialization.failureDetail || '物化任务失败')}</span></div></div>`;
  } else {
    body = `<div class="dataset-artifact-grid"><div><span>对象复验</span>${statusChip(evidenceState)}</div><div><span>行数</span><b>${materialization.rowCount ?? 0}</b></div><div><span>文件大小</span><b>${formatBytes(materialization.byteSize)}</b></div><div><span>Schema 字段</span><b>${schemaFieldCount}</b></div><div><span>对象版本</span><code>${escapeHtml(objectVersionId)}</code></div><div><span>完成时间</span><b>${formatTime(materialization.completedAt)}</b></div></div><div class="dataset-artifact-reference"><span>精确对象 URI</span><code class="dataset-artifact-uri">${escapeHtml(materialization.artifactUri || '-')}</code><span>内容 SHA-256</span><code class="dataset-artifact-sha">${escapeHtml(materialization.contentSha256 || '-')}</code></div>`;
  }
  return `<div class="drawer-section dataset-materialization-panel" data-materialization-state="${escapeHtml(materialization.state)}"><div class="section-title"><h3>Parquet 物化</h3><span>${statusChip(materialization.state)} · r${materialization.revision}</span></div>${body}</div>`;
}

function datasetDeliveryChainHtml(snapshot: DatasetSnapshot): string {
  const materialization = snapshot.latestMaterialization || null;
  const publication = currentDatasetCatalogPublication(materialization);
  return `<div class="drawer-section dataset-delivery-panel"><div class="section-title"><h3>数据交付链</h3><span>分层验收</span></div><div class="dataset-delivery-grid"><div><span>1 · Manifest</span>${statusChip(snapshot.state)}</div><div><span>2 · Parquet</span>${statusChip(materialization?.state || 'NOT_STARTED')}</div><div><span>3 · Iceberg</span>${statusChip(publication?.state || 'NOT_STARTED')}</div><div><span>4 · ML / 模型</span>${statusChip('NOT_STARTED')}</div></div></div>`;
}

function safeCatalogFailureDetail(value?: string | null): string {
  const normalized = (value || '目录发布任务失败').replace(/\s+/g, ' ').trim();
  return normalized.length > 300 ? `${normalized.slice(0, 300)}...` : normalized;
}

function datasetCatalogPublicationHtml(snapshot: DatasetSnapshot): string {
  const materialization = snapshot.latestMaterialization || null;
  const publication = currentDatasetCatalogPublication(materialization);
  if (materialization?.state !== 'READY') {
    return `<div class="drawer-section dataset-catalog-panel" data-catalog-state="NOT_STARTED"><div class="section-title"><h3>Iceberg 目录</h3>${statusChip('NOT_STARTED')}</div><div class="simulation-empty">等待 Parquet 精确版本完成复验</div></div>`;
  }
  if (!publication) {
    return `<div class="drawer-section dataset-catalog-panel" data-catalog-state="NOT_STARTED"><div class="section-title"><h3>Iceberg 目录</h3>${statusChip('NOT_STARTED')}</div><div class="simulation-empty">Parquet 已就绪，尚未申请发布到 Iceberg REST Catalog</div></div>`;
  }
  let body = '';
  if (['QUEUED', 'COMMITTING', 'VERIFYING'].includes(publication.state)) {
    const label = publication.state === 'QUEUED' ? '等待 Publisher 领取'
      : publication.state === 'COMMITTING' ? '正在提交 Iceberg 快照'
        : '正在复验目录快照与源对象版本';
    body = `<div class="batch-detail-loading"><i data-lucide="refresh-cw"></i><div><strong>${label}</strong><span>attempt ${publication.attemptCount} · revision ${publication.revision}</span></div></div>`;
  } else if (publication.state === 'FAILED') {
    body = `<div class="dataset-materialization-error"><i data-lucide="circle-alert"></i><div><strong>${escapeHtml(publication.failureCode || 'CATALOG_PUBLICATION_FAILED')}</strong><span>${escapeHtml(safeCatalogFailureDetail(publication.failureDetail))}</span></div></div>`;
  } else {
    const verified = publication.catalogMetadata?.catalogSnapshotVerified === true
      ? 'VERIFIED' : 'UNVERIFIED';
    body = `<div class="dataset-catalog-grid"><div><span>目录复验</span>${statusChip(verified)}</div><div><span>Iceberg 快照</span><code class="dataset-iceberg-snapshot">${escapeHtml(publication.icebergSnapshotId || '-')}</code></div><div><span>行数一致</span><b>${publication.verifiedRowCount ?? 0} / ${publication.sourceRowCount}</b></div><div><span>Schema / 分区规范</span><b>${publication.icebergSchemaId ?? '-'} / ${publication.icebergPartitionSpecId ?? '-'}</b></div><div><span>完成时间</span><b>${formatTime(publication.completedAt)}</b></div><div><span>发布器</span><code>${escapeHtml(publication.publisherVersion)}</code></div></div><div class="dataset-artifact-reference"><span>表标识</span><code class="dataset-table-identifier">${escapeHtml(publication.tableIdentifier)}</code><span>Metadata location</span><code>${escapeHtml(publication.icebergMetadataLocation || '-')}</code><span>语义 SHA-256</span><code class="dataset-semantic-sha">${escapeHtml(publication.semanticChecksum || '-')}</code><span>源对象版本</span><code>${escapeHtml(publication.sourceObjectVersionId)}</code></div>`;
  }
  return `<div class="drawer-section dataset-catalog-panel" data-catalog-state="${escapeHtml(publication.state)}"><div class="section-title"><h3>Iceberg 目录</h3><span>${statusChip(publication.state)} · r${publication.revision}</span></div>${body}</div>`;
}

function datasetDeliveryActionHtml(snapshot: DatasetSnapshot): string {
  if (['QUEUED', 'BUILDING'].includes(snapshot.state)) {
    return '<button class="button button--primary" id="refresh-dataset-snapshot"><i data-lucide="refresh-cw"></i>刷新清单</button>';
  }
  if (snapshot.state !== 'MANIFEST_READY') return '';
  const materialization = snapshot.latestMaterialization || null;
  if (!materialization) {
    return '<button class="button button--primary" id="open-dataset-materialization"><i data-lucide="database"></i>生成 Parquet</button>';
  }
  if (materialization.state === 'FAILED') {
    return '<button class="button button--primary" id="retry-dataset-materialization"><i data-lucide="rotate-ccw"></i>重新排队</button>';
  }
  if (['QUEUED', 'WRITING'].includes(materialization.state)) {
    return '<button class="button button--primary" id="refresh-dataset-materialization"><i data-lucide="refresh-cw"></i>刷新 Parquet</button>';
  }
  const publication = currentDatasetCatalogPublication(materialization);
  if (!publication) {
    return '<button class="button button--primary" id="open-dataset-catalog-publication"><i data-lucide="upload"></i>发布 Iceberg</button>';
  }
  if (publication.state === 'FAILED') {
    return '<button class="button button--primary" id="retry-dataset-catalog-publication"><i data-lucide="rotate-ccw"></i>重试 Iceberg</button>';
  }
  if (['QUEUED', 'COMMITTING', 'VERIFYING'].includes(publication.state)) {
    return '<button class="button button--primary" id="refresh-dataset-catalog-publication"><i data-lucide="refresh-cw"></i>刷新 Iceberg</button>';
  }
  return '';
}

function renderDatasetSnapshotDrawer(snapshot: DatasetSnapshot): void {
  const manifest = snapshot.manifest;
  const exclusions = Object.entries(snapshot.exclusionSummary || {})
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([reason, count]) => `<li><code>${escapeHtml(reason)}</code><b>${count}</b></li>`).join('');
  const samples = (manifest?.samples || []).slice(0, 50).map((sample) => `<tr><td><strong>${escapeHtml(sample.batchNo)}</strong><small>${escapeHtml(sample.lineId)}</small></td><td>${formatTime(sample.predictionTime)}</td><td>${escapeHtml(sample.splitKey)}</td><td>${number(sample.confidence * 100, 0)}%</td><td>${sample.included ? statusChip('INCLUDED') : statusChip('EXCLUDED')}</td><td>${escapeHtml(sample.exclusionReasons.join(', ') || '-')}</td></tr>`).join('');
  const phase = manifest?.phaseBoundary;
  const buildHtml = manifest
    ? `<div class="drawer-section"><div class="section-title"><h3>Manifest 边界</h3><span>不可变</span></div><div class="dataset-phase-grid"><div><span>交付状态</span><b>${escapeHtml(phase?.deliveryState || 'MANIFEST_ONLY')}</b></div><div><span>Manifest 物化声明</span><b>${escapeHtml(phase?.materializationState || 'NOT_STARTED')}</b></div><div><span>Manifest Artifact URI</span><b>${escapeHtml(phase?.artifactUri || '-')}</b></div></div></div>${datasetDeliveryChainHtml(snapshot)}${datasetMaterializationHtml(snapshot)}${datasetCatalogPublicationHtml(snapshot)}<div class="drawer-section"><div class="section-title"><h3>样本统计</h3><span>${manifest.counts.total} 条</span></div><div class="metric-grid dataset-metrics"><div><span>总样本</span><b>${manifest.counts.total}</b></div><div><span>纳入</span><b>${manifest.counts.included}</b></div><div><span>排除</span><b>${manifest.counts.excluded}</b></div><div><span>排除原因</span><b>${Object.keys(manifest.counts.exclusionSummary).length}</b></div></div>${exclusions ? `<ul class="dataset-exclusion-list">${exclusions}</ul>` : ''}</div><div class="drawer-section"><div class="section-title"><h3>Point-in-time 样本</h3><span>${samples ? `显示 ${Math.min(manifest.samples.length, 50)} / ${manifest.samples.length}` : '无'}</span></div>${samples ? `<div class="table-frame dataset-sample-frame"><table><thead><tr><th>批次</th><th>预测时点</th><th>拆分</th><th>置信度</th><th>结果</th><th>排除原因</th></tr></thead><tbody>${samples}</tbody></table></div>` : '<div class="simulation-empty">没有满足冻结条件的影子复核样本</div>'}</div>`
    : `<div class="drawer-section"><div class="batch-detail-loading"><i data-lucide="refresh-cw"></i><div><strong>${snapshot.state === 'FAILED' ? '清单构建失败' : '后台正在构建清单'}</strong><span>${escapeHtml(snapshot.failureDetail || `attempt ${snapshot.attemptCount}`)}</span></div></div></div>`;
  openDrawer(`<header><div><span>数据集快照 v${snapshot.snapshotVersion}</span><h2>${escapeHtml(snapshot.datasetName)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="batch-state-band"><div>${statusChip(snapshot.state)}${statusChip(snapshot.materializationState)}<span class="shadow-label">POINT-IN-TIME</span></div><span>snapshot revision ${snapshot.revision}</span></div><div class="drawer-section facts-grid"><div><span>冻结时间</span><b>${formatTime(snapshot.freezeAt)}</b></div><div><span>产线</span><b>${escapeHtml(snapshot.lineIds.join(', '))}</b></div><div><span>规则版本筛选</span><b>${escapeHtml(snapshot.ruleVersionIds.join(', ') || '全部合格版本')}</b></div><div><span>低置信度排除</span><b>${snapshot.excludeLowConfidence ? '是' : '否'}</b></div><div><span>定义 checksum</span><b class="mono-value">${escapeHtml(snapshot.definitionChecksum)}</b></div><div><span>manifest checksum</span><b class="mono-value">${escapeHtml(snapshot.manifestChecksum || '-')}</b></div></div>${buildHtml}<footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button>${datasetDeliveryActionHtml(snapshot)}</footer>`, `dataset-snapshot:${snapshot.id}`);
  document.querySelector('#refresh-dataset-snapshot')?.addEventListener('click', () => void openDatasetSnapshotById(snapshot.id));
  document.querySelector('#open-dataset-materialization')?.addEventListener('click', () => openDatasetMaterializationDialog(snapshot, 'request'));
  document.querySelector('#retry-dataset-materialization')?.addEventListener('click', () => openDatasetMaterializationDialog(snapshot, 'retry'));
  document.querySelector('#refresh-dataset-materialization')?.addEventListener('click', () => {
    const materialization = snapshot.latestMaterialization;
    if (!materialization) return;
    void pollDatasetMaterialization(
      snapshot.id,
      materialization.id,
      datasetSnapshotRequestGeneration,
    )
      .catch((error) => showToast(error instanceof Error ? error.message : String(error), true));
  });
  document.querySelector('#open-dataset-catalog-publication')?.addEventListener('click', () => openDatasetCatalogPublicationDialog(snapshot, 'request'));
  document.querySelector('#retry-dataset-catalog-publication')?.addEventListener('click', () => openDatasetCatalogPublicationDialog(snapshot, 'retry'));
  document.querySelector('#refresh-dataset-catalog-publication')?.addEventListener('click', () => {
    const publication = currentDatasetCatalogPublication(snapshot.latestMaterialization || null);
    if (!publication) return;
    void pollDatasetCatalogPublication(
      snapshot.id,
      publication.id,
      datasetSnapshotRequestGeneration,
    )
      .catch((error) => showToast(error instanceof Error ? error.message : String(error), true));
  });
}

function openDrawer(html: string, drawerKey: string | null = null): void {
  const drawer = document.querySelector<HTMLElement>('#detail-drawer')!;
  const previousKey = activeDrawerKey;
  const previousScrollTop = drawer.scrollTop;
  activeDrawerKey = drawerKey;
  drawer.innerHTML = html;
  drawer.scrollTop = previousKey === drawerKey ? previousScrollTop : 0;
  drawer.classList.add('is-open');
  drawer.setAttribute('aria-hidden', 'false');
  drawer.querySelectorAll('[data-close-drawer]').forEach((button) => button.addEventListener('click', closeDrawer));
  refreshIcons();
}

function closeDrawer(): void {
  const drawer = document.querySelector<HTMLElement>('#detail-drawer');
  activeDrawerKey = null;
  drawer?.classList.remove('is-open');
  drawer?.setAttribute('aria-hidden', 'true');
}

function showToast(message: string, error = false): void {
  const toast = document.querySelector<HTMLDivElement>('#toast')!;
  toast.textContent = message;
  toast.className = `toast is-visible${error ? ' is-error' : ''}`;
  window.setTimeout(() => { toast.className = 'toast'; }, 3600);
}

shell();
const initialView = location.hash.replace('#/', '') as View;
if (['overview', 'candidates', 'batches', 'shadowRuns', 'dataQuality', 'points', 'rules', 'datasets', 'featureFlags'].includes(initialView)) state.view = initialView;
void loadView();
window.setInterval(() => { if (!document.hidden && state.view === 'overview') void loadView(true); }, 5_000);

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
  Network,
  Play,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Upload,
  X,
  createIcons,
} from 'lucide';
import { ApiProblem, bpiApi } from './api';
import type {
  Batch,
  Candidate,
  Evidence,
  LineState,
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
  StateEvent,
  TopologyVersion,
  TopologyDraftCommand,
  VersionComparison,
} from './types';
import './styles.css';

type View = 'overview' | 'candidates' | 'batches' | 'points' | 'rules';
const CALIBRATION_PAGE_SIZE = 50;
const POINT_CATALOG_PAGE_SIZE = 100;
const POINT_SEARCH_DEBOUNCE_MS = 250;

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
  rules: [] as RuleVersion[],
  topologies: [] as TopologyVersion[],
  pointCatalog: null as PointCatalogView | null,
  calibrations: [] as PointCalibration[],
  pointSearch: '',
  pointCatalogNextCursor: null as string | null,
  pointCatalogSnapshotAt: null as string | null,
  loadingMorePointCatalog: false,
  calibrationNextCursor: null as string | null,
  calibrationSnapshotAt: null as string | null,
  loadingMoreCalibrations: false,
  selectedCandidate: null as Candidate | null,
  selectedBatch: null as Batch | null,
  selectedRule: null as RuleVersion | null,
  selectedTopology: null as TopologyVersion | null,
  selectedSimulation: null as RuleSimulation | null,
  selectedCalibration: null as PointCalibration | null,
  candidateCommand: null as 'confirm' | 'reject' | null,
  batchCommand: null as 'suspend' | 'resume' | null,
  ruleCommand: null as 'submit' | 'approve' | 'reject' | 'retry' | 'retire' | null,
  topologyCommand: null as 'validate' | 'publish' | null,
  calibrationCommand: null as 'approve' | 'reject' | 'revoke' | null,
  batchEvidence: { start: [], end: [] } as { start: Evidence[]; end: Evidence[] },
  timeline: [] as StateEvent[],
  error: null as Error | null,
};

let pointCatalogRequestGeneration = 0;
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

function statusTone(status: string): string {
  if (['RUNNING', 'ACTIVE', 'READY', 'CONFIRMED', 'GOOD', 'RELEASED', 'PUBLISHED', 'APPLIED', 'EFFECTIVE', 'APPROVED'].includes(status)) return 'ok';
  if (['PENDING', 'DISPATCHING', 'WAITING', 'PARTIAL', 'WAIT_QA', 'DEGRADED', 'NOT_YET_EFFECTIVE'].includes(status)) return 'warn';
  if (['FAILED', 'BAD', 'REJECTED', 'BLOCKED', 'SUSPENDED', 'REVOKED', 'EXPIRED'].includes(status)) return 'danger';
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
          <button class="nav-item" data-view="overview">${icon('activity', '实时生产态势')}</button>
          <button class="nav-item" data-view="candidates">${icon('list-checks', '候选批次')}<b id="candidate-count">0</b></button>
          <button class="nav-item" data-view="batches">${icon('archive', '批次档案')}</button>
          <button class="nav-item" data-view="points">${icon('database', '点位准入')}</button>
          <button class="nav-item" data-view="rules">${icon('network', '规则与拓扑')}</button>
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
      <div id="toast" class="toast" role="status" aria-live="polite"></div>
    </div>`;
  document.querySelector<HTMLSelectElement>('#plant-id')!.value = state.plantId;
  bindShellEvents();
  refreshIcons();
}

function refreshIcons(): void {
  createIcons({ icons: { Activity, Archive, Boxes, CheckCircle2, ChevronRight, ChevronsDown, CircleAlert, Clock3, Database, Factory, Filter, FlaskConical, Gauge, ListChecks, Network, Play, Plus, RefreshCw, Search, ShieldCheck, Upload, X } });
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
  document.querySelector<HTMLSelectElement>('#topology-base')?.addEventListener('change', applyTopologyBase);
  document.querySelector<HTMLSelectElement>('#rule-base')?.addEventListener('change', applyRuleBase);
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
    points: ['数据准入', '点位目录'],
    rules: ['边界治理', '规则与拓扑'],
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
  else if (state.view === 'points') renderPoints();
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
    SOURCE_SEQUENCE_DISABLED: '来源序列未启用',
    CATALOG_BINDING_NOT_FOUND: '点位目录中找不到规则绑定',
    CATALOG_SNAPSHOT_MISSING: '当前产线没有点位目录快照',
  };
  return labels[normalized] || code;
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
      <td>${point.sourceSequenceEnabled ? '已启用' : '未启用'}</td>
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

async function openBatch(batchId: string): Promise<void> {
  try {
    const [batchResponse, evidenceResponse, timelineResponse] = await Promise.all([bpiApi.batch(batchId), bpiApi.evidence(batchId), bpiApi.timeline(batchId)]);
    state.selectedBatch = batchResponse.data;
    state.batchEvidence = evidenceResponse.data;
    state.timeline = timelineResponse.data;
    const batch = state.selectedBatch;
    const evidence = [...state.batchEvidence.start, ...state.batchEvidence.end].map((item) => `<li><span class="evidence-state evidence-state--${item.satisfied ? 'ok' : 'bad'}"></span><div><strong>${escapeHtml(item.signal)}</strong><small>${escapeHtml(item.source)} · ${formatTime(item.eventTime)}</small></div><b>${escapeHtml(item.value)}${item.unit ? ` ${escapeHtml(item.unit)}` : ''}</b></li>`).join('');
    const timeline = state.timeline.map((item) => `<li><i></i><div><strong>${escapeHtml(item.action)}</strong><span>${escapeHtml(item.reason || '-')}</span><small>${formatTime(item.at || item.eventTime)} · ${escapeHtml(item.actor || item.actorId || '-')}</small></div></li>`).join('');
    const command = batch.state === 'ACTIVE'
      ? '<button class="button button--danger" id="open-suspend">暂停自动处理</button>'
      : batch.state === 'SUSPENDED'
        ? '<button class="button button--primary" id="open-resume">恢复自动处理</button>'
        : '';
    openDrawer(`<header><div><span>批次档案</span><h2>${escapeHtml(batch.batchNo)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="batch-state-band"><div>${statusChip(batch.state)}${batch.shadow ? '<span class="shadow-label">SHADOW</span>' : ''}</div><span>revision ${batch.revision}</span></div><div class="drawer-section facts-grid"><div><span>产线 / 工段</span><b>${escapeHtml(batch.lineId)} / ${escapeHtml(batch.stageCode)}</b></div><div><span>生产指令</span><b>${escapeHtml(batch.orderId || '-')}</b></div><div><span>开始时间</span><b>${formatTime(batch.startTime)}</b></div><div><span>结束时间</span><b>${formatTime(batch.endTime)}</b></div><div><span>累计量</span><b>${number(batch.quantity)} ${escapeHtml(batch.quantityUnit)}</b></div><div><span>干物量</span><b>${number(batch.dryMatter)} ${escapeHtml(batch.quantityUnit)}</b></div><div><span>质量门</span>${statusChip(batch.qualityGate)}</div><div><span>库存状态</span>${statusChip(batch.wmsStatus)}</div></div><div class="drawer-section"><div class="section-title"><h3>边界证据</h3><span>${state.batchEvidence.start.length} START / ${state.batchEvidence.end.length} END</span></div><ul class="evidence-list evidence-list--compact">${evidence || '<li>暂无证据</li>'}</ul></div><div class="drawer-section"><h3>状态时间线</h3><ol class="timeline">${timeline}</ol></div><footer class="drawer-actions"><button class="button button--secondary" data-close-drawer>关闭</button>${command}</footer>`);
    document.querySelector('#open-suspend')?.addEventListener('click', () => openBatchCommandDialog('suspend'));
    document.querySelector('#open-resume')?.addEventListener('click', () => openBatchCommandDialog('resume'));
  } catch (error) { showToast(error instanceof Error ? error.message : String(error), true); }
}

function openBatchCommandDialog(command: 'suspend' | 'resume'): void {
  const batch = state.selectedBatch;
  if (!batch) return;
  const isSuspend = command === 'suspend';
  state.candidateCommand = null;
  state.batchCommand = command;
  state.ruleCommand = null;
  state.topologyCommand = null;
  state.calibrationCommand = null;
  document.querySelector('#command-kicker')!.textContent = '批次运行控制';
  document.querySelector('#command-title')!.textContent = isSuspend ? '暂停批次自动处理' : '恢复批次自动处理';
  document.querySelector('#command-reason-label')!.textContent = isSuspend ? '暂停原因' : '恢复原因';
  document.querySelector('#command-summary')!.innerHTML = `<div><span>批次</span><b>${escapeHtml(batch.batchNo)}</b></div><div><span>产线</span><b>${escapeHtml(batch.lineId)}</b></div><div><span>状态变化</span><b>${isSuspend ? 'ACTIVE → SUSPENDED' : 'SUSPENDED → ACTIVE'}</b></div><div><span>版本</span><b>r${batch.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = isSuspend ? '填写上下文过期、数据冲突或现场处置依据' : '填写上下文恢复或人工复核依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = `button ${isSuspend ? 'button--danger' : 'button--primary'}`;
  button.textContent = isSuspend ? '确认暂停' : '确认恢复';
  document.querySelector<HTMLDialogElement>('#confirm-dialog')!.showModal();
  reason.focus();
}

async function handleBatchCommand(): Promise<void> {
  const batch = state.selectedBatch;
  const command = state.batchCommand;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  if (!batch || !command || reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = command === 'suspend' ? '暂停中...' : '恢复中...';
  try {
    const response = command === 'suspend'
      ? await bpiApi.suspendBatch(batch, reason, commandId())
      : await bpiApi.resumeBatch(batch, reason, commandId());
    state.selectedBatch = response.data;
    state.batches = state.batches.map((item) => item.id === response.data.id ? response.data : item);
    state.lines = state.lines.map((line) => line.lineId === response.data.lineId
      ? { ...line, status: command === 'suspend' ? 'BLOCKED' : 'RUNNING' }
      : line);
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    showToast(command === 'suspend' ? '批次自动处理已暂停' : '批次自动处理已恢复');
    await loadView(true);
    await openBatch(response.data.id);
  } catch (error) {
    if (error instanceof ApiProblem && error.problem.status === 409) {
      showToast(`批次已变化，服务器版本 r${error.problem.currentRevision ?? '-'}`, true);
      await openBatch(batch.id);
    } else showToast(error instanceof Error ? error.message : String(error), true);
  } finally {
    button.disabled = false;
    button.className = `button ${command === 'suspend' ? 'button--danger' : 'button--primary'}`;
    button.textContent = command === 'suspend' ? '确认暂停' : '确认恢复';
  }
}

function openDrawer(html: string): void {
  const drawer = document.querySelector<HTMLElement>('#detail-drawer')!;
  drawer.innerHTML = html;
  drawer.classList.add('is-open');
  drawer.setAttribute('aria-hidden', 'false');
  drawer.querySelectorAll('[data-close-drawer]').forEach((button) => button.addEventListener('click', closeDrawer));
  refreshIcons();
}

function closeDrawer(): void {
  const drawer = document.querySelector<HTMLElement>('#detail-drawer');
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
if (['overview', 'candidates', 'batches', 'points', 'rules'].includes(initialView)) state.view = initialView;
void loadView();
window.setInterval(() => { if (!document.hidden && state.view === 'overview') void loadView(true); }, 5_000);

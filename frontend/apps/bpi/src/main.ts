import {
  Activity,
  Archive,
  Boxes,
  CheckCircle2,
  ChevronRight,
  CircleAlert,
  Clock3,
  Factory,
  Filter,
  Gauge,
  ListChecks,
  RefreshCw,
  Search,
  ShieldCheck,
  X,
  createIcons,
} from 'lucide';
import { ApiProblem, bpiApi } from './api';
import type { Batch, Candidate, Evidence, LineState, ResponseMeta, StateEvent } from './types';
import './styles.css';

type View = 'overview' | 'candidates' | 'batches';

const appRoot = document.querySelector<HTMLDivElement>('#app');
if (!appRoot) throw new Error('BPI app root is missing');
const app: HTMLDivElement = appRoot;

const state = {
  view: 'overview' as View,
  plantId: localStorage.getItem('bpi.plantId') || 'PLANT-01',
  onlyAbnormal: localStorage.getItem('bpi.onlyAbnormal') === 'true',
  loading: false,
  meta: null as ResponseMeta | null,
  lines: [] as LineState[],
  candidates: [] as Candidate[],
  batches: [] as Batch[],
  selectedCandidate: null as Candidate | null,
  selectedBatch: null as Batch | null,
  candidateCommand: null as 'confirm' | 'reject' | null,
  batchEvidence: { start: [], end: [] } as { start: Evidence[]; end: Evidence[] },
  timeline: [] as StateEvent[],
  error: null as Error | null,
};

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

function number(value: number | null | undefined, digits = 1): string {
  return value === null || value === undefined ? '-' : value.toFixed(digits);
}

function statusTone(status: string): string {
  if (['RUNNING', 'ACTIVE', 'CONFIRMED', 'GOOD', 'RELEASED'].includes(status)) return 'ok';
  if (['PENDING', 'PARTIAL', 'WAIT_QA', 'DEGRADED'].includes(status)) return 'warn';
  if (['FAILED', 'BAD', 'REJECTED', 'BLOCKED'].includes(status)) return 'danger';
  return 'neutral';
}

function statusChip(status: string): string {
  return `<span class="status status--${statusTone(status)}">${escapeHtml(status)}</span>`;
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
          <header><div><span>候选批次</span><h2 id="command-title">审核候选边界</h2></div><button value="cancel" class="icon-button" aria-label="关闭"><i data-lucide="x"></i></button></header>
          <div class="command-summary" id="command-summary"></div>
          <label><span id="command-reason-label">审核原因</span><textarea id="confirm-reason" minlength="3" maxlength="500" required placeholder="填写现场审核依据"></textarea></label>
          <footer><button value="cancel" class="button button--secondary">取消</button><button id="confirm-submit" value="default" class="button button--primary">提交</button></footer>
        </form>
      </dialog>
      <div id="toast" class="toast" role="status" aria-live="polite"></div>
    </div>`;
  document.querySelector<HTMLSelectElement>('#plant-id')!.value = state.plantId;
  bindShellEvents();
  refreshIcons();
}

function refreshIcons(): void {
  createIcons({ icons: { Activity, Archive, Boxes, CheckCircle2, ChevronRight, CircleAlert, Clock3, Factory, Filter, Gauge, ListChecks, RefreshCw, Search, ShieldCheck, X } });
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
}

function navigate(view: View): void {
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
    } else {
      const response = await bpiApi.batches(state.plantId);
      state.batches = response.data;
      state.meta = response.meta;
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
  else renderBatches();
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
  const isReject = command === 'reject';
  document.querySelector('#command-title')!.textContent = isReject ? '拒绝候选边界' : '确认启动边界';
  document.querySelector('#command-reason-label')!.textContent = isReject ? '拒绝原因' : '确认原因';
  document.querySelector('#command-summary')!.innerHTML = `<div><span>产线</span><b>${escapeHtml(candidate.lineId)}</b></div><div><span>生产指令</span><b>${escapeHtml(candidate.orderId || '-')}</b></div><div><span>${isReject ? '处理结果' : '拟定批次'}</span><b>${isReject ? '不生成批次' : `BPI · ${formatTime(candidate.boundaryTime)}`}</b></div><div><span>版本</span><b>r${candidate.revision}</b></div>`;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!;
  reason.value = '';
  reason.placeholder = isReject ? '填写误判、上下文错误或现场处置依据' : '填写现场确认依据';
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.className = `button ${isReject ? 'button--danger' : 'button--primary'}`;
  button.textContent = isReject ? '拒绝候选' : '确认并生成影子批次';
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
  const candidate = state.selectedCandidate;
  const command = state.candidateCommand;
  const reason = document.querySelector<HTMLTextAreaElement>('#confirm-reason')!.value.trim();
  if (!candidate || !command || reason.length < 3) return;
  const button = document.querySelector<HTMLButtonElement>('#confirm-submit')!;
  button.disabled = true;
  button.textContent = command === 'reject' ? '拒绝中...' : '确认中...';
  try {
    if (command === 'reject') {
      const response = await bpiApi.rejectCandidate(candidate, reason, crypto.randomUUID());
      applyCandidateReview(response.data);
      document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
      closeDrawer();
      showToast('候选已拒绝，未生成影子批次');
      state.view = 'candidates';
      history.replaceState(null, '', '#/candidates');
      await loadView();
      return;
    }
    const response = await bpiApi.confirmCandidate(candidate, reason, crypto.randomUUID());
    applyCandidateReview(response.data.candidate);
    document.querySelector<HTMLDialogElement>('#confirm-dialog')!.close();
    closeDrawer();
    showToast(`影子批次 ${response.data.batch.batchNo} 已生成`);
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
    button.textContent = command === 'reject' ? '拒绝候选' : '确认并生成影子批次';
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
    openDrawer(`<header><div><span>批次档案</span><h2>${escapeHtml(batch.batchNo)}</h2></div><button class="icon-button" data-close-drawer aria-label="关闭"><i data-lucide="x"></i></button></header><div class="batch-state-band"><div>${statusChip(batch.state)}${batch.shadow ? '<span class="shadow-label">SHADOW</span>' : ''}</div><span>revision ${batch.revision}</span></div><div class="drawer-section facts-grid"><div><span>产线 / 工段</span><b>${escapeHtml(batch.lineId)} / ${escapeHtml(batch.stageCode)}</b></div><div><span>生产指令</span><b>${escapeHtml(batch.orderId || '-')}</b></div><div><span>累计量</span><b>${number(batch.quantity)} ${escapeHtml(batch.quantityUnit)}</b></div><div><span>干物量</span><b>${number(batch.dryMatter)} ${escapeHtml(batch.quantityUnit)}</b></div><div><span>质量门</span>${statusChip(batch.qualityGate)}</div><div><span>库存状态</span>${statusChip(batch.wmsStatus)}</div></div><div class="drawer-section"><div class="section-title"><h3>边界证据</h3><span>${state.batchEvidence.start.length} START / ${state.batchEvidence.end.length} END</span></div><ul class="evidence-list evidence-list--compact">${evidence || '<li>暂无证据</li>'}</ul></div><div class="drawer-section"><h3>状态时间线</h3><ol class="timeline">${timeline}</ol></div>`);
  } catch (error) { showToast(error instanceof Error ? error.message : String(error), true); }
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
if (['overview', 'candidates', 'batches'].includes(initialView)) state.view = initialView;
void loadView();
window.setInterval(() => { if (!document.hidden && state.view === 'overview') void loadView(true); }, 5_000);

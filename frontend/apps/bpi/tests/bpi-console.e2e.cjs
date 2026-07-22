const assert = require('node:assert/strict');
const { after, before, test } = require('node:test');
const { spawn } = require('node:child_process');
const { once } = require('node:events');
const path = require('node:path');
const { chromium } = require('playwright');
const { createBpiSimulator, listen } = require('../../../../simulation/bpi/server');

const APP_ROOT = path.resolve(__dirname, '..');
const APP_URL = 'http://127.0.0.1:4173';
const RULE_ID = '78d57d90-fdc8-4a57-a660-a1ae73c2bc96';
const SOURCE_SEQUENCE_FINGERPRINT = `sha256:${'2'.repeat(64)}`;
let simulator;
let vite;
let browser;
let viteLog = '';
let simulatorUrl;

async function waitForHttp(url, timeoutMs = 20_000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url);
      if (response.ok) return;
    } catch (_) {
      // Process is still starting.
    }
    await new Promise((resolve) => setTimeout(resolve, 150));
  }
  throw new Error(`Timed out waiting for ${url}\n${viteLog}`);
}

function observe(page) {
  const errors = [];
  page.on('console', (message) => {
    if (message.type() === 'error') errors.push(`console: ${message.text()}`);
  });
  page.on('pageerror', (error) => errors.push(`pageerror: ${error.message}`));
  page.on('requestfailed', (request) => errors.push(`requestfailed: ${request.method()} ${request.url()} ${request.failure()?.errorText || ''}`));
  return errors;
}

async function assertDrawerSettled(page) {
  const drawer = page.locator('#detail-drawer');
  await drawer.waitFor({ state: 'visible' });
  await page.waitForTimeout(250);
  const box = await drawer.boundingBox();
  const viewport = page.viewportSize();
  assert.ok(box && viewport, 'detail drawer and viewport geometry must be available');
  assert.ok(box.x >= 0, `detail drawer starts outside viewport: ${JSON.stringify(box)}`);
  assert.ok(box.x + box.width <= viewport.width + 1, `detail drawer ends outside viewport: ${JSON.stringify({ box, viewport })}`);
}

async function prepareBatchReleaseScenario() {
  let response = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(response.status, 200);
  response = await fetch(`${simulatorUrl}/__simulation/prepare-batch-release`, { method: 'POST' });
  assert.equal(response.status, 200);
  const prepared = await response.json();
  assert.equal(prepared.preparedBatchCount, 6);
  return prepared.batchIds;
}

async function stopChildProcess(child) {
  if (!child || child.exitCode !== null) return;
  const exited = once(child, 'exit');
  child.kill('SIGTERM');
  const forceStop = new Promise((resolve) => {
    const timer = setTimeout(() => {
      if (child.exitCode === null) child.kill('SIGKILL');
      resolve();
    }, 3_000);
    timer.unref();
  });
  await Promise.race([exited, forceStop]);
}

before(async () => {
  ({ server: simulator } = createBpiSimulator());
  const address = await listen(simulator);
  simulatorUrl = `http://127.0.0.1:${address.port}`;
  vite = spawn(process.execPath, [path.join(APP_ROOT, 'node_modules/vite/bin/vite.js'), '--host', '127.0.0.1', '--port', '4173'], {
    cwd: APP_ROOT,
    env: { ...process.env, BPI_API_TARGET: simulatorUrl },
    stdio: ['ignore', 'pipe', 'pipe'],
  });
  vite.stdout.on('data', (chunk) => { viteLog += chunk.toString(); });
  vite.stderr.on('data', (chunk) => { viteLog += chunk.toString(); });
  await waitForHttp(APP_URL);
  browser = await chromium.launch({ headless: true });
});

after(async () => {
  if (browser) await browser.close();
  await stopChildProcess(vite);
  if (simulator) await new Promise((resolve, reject) => simulator.close((error) => error ? reject(error) : resolve()));
});

test('desktop operator confirms a candidate and opens the shadow batch', async () => {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await context.addInitScript(() => {
    Object.defineProperty(window.crypto, 'randomUUID', { value: undefined, configurable: true });
  });
  const page = await context.newPage();
  const errors = observe(page);
  await page.goto(APP_URL, { waitUntil: 'networkidle' });

  await assert.doesNotReject(() => page.getByRole('heading', { name: '实时生产态势' }).waitFor());
  await assert.doesNotReject(() => page.getByText('S07 制糖线').waitFor());
  assert.equal(await page.locator('[data-line-id]').count(), 1);

  await page.locator('[data-view="candidates"]').click();
  await page.getByRole('heading', { name: '候选批次' }).waitFor();
  await page.locator('[data-candidate-id]').click();
  await page.getByRole('heading', { name: 'LINE-S07-01' }).waitFor();
  await page.getByRole('button', { name: '确认候选' }).click();
  await page.locator('#confirm-reason').fill('班长现场确认泵阀路径与流量稳定');
  await page.getByRole('button', { name: '确认并生成影子批次' }).click();

  await page.getByRole('heading', { name: '批次档案' }).waitFor();
  await page.getByRole('heading', { name: 'S07-20260712-001' }).waitFor();
  await page.getByText('SHADOW', { exact: true }).last().waitFor();
  assert.equal(await page.locator('#candidate-count').textContent(), '1');
  await page.screenshot({ path: '/tmp/bpi-console-desktop.png', fullPage: true });
  assert.deepEqual(errors, []);
  await context.close();
});

test('data engineer materializes a point-in-time dataset with failed retry and mobile evidence', async () => {
  let response = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(response.status, 200);
  response = await fetch(`${simulatorUrl}/__simulation/prepare-dataset-manifest`, { method: 'POST' });
  assert.equal(response.status, 200);

  const desktop = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await desktop.newPage();
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/datasets`, { waitUntil: 'networkidle' });
  await page.getByRole('heading', { name: '数据集清单' }).waitFor();
  await page.locator('#open-dataset-definition').click();
  await page.locator('#dataset-code').fill('ADP-E2E-START-BOUNDARY');
  await page.locator('#dataset-name').fill('ADP E2E 启动边界清单');
  await page.locator('#dataset-reason').fill('建立浏览器到 manifest 的受控验收定义');
  await page.locator('#dataset-definition-submit').click();

  await page.getByRole('heading', { name: 'ADP E2E 启动边界清单' }).waitFor();
  await page.locator('#open-dataset-snapshot').click();
  await page.locator('#dataset-snapshot-reason').fill('冻结已批准影子复核记录并生成 point-in-time 清单');
  await page.locator('#dataset-snapshot-submit').click();

  await page.locator('.batch-state-band').getByText('MANIFEST_READY', { exact: true }).waitFor();
  await page.locator('.batch-state-band').getByText('POINT-IN-TIME', { exact: true }).waitFor();
  await page.getByText('MANIFEST_ONLY', { exact: true }).waitFor();
  await page.locator('.dataset-exclusion-list').getByText('CONFIDENCE_BELOW_THRESHOLD', { exact: true }).waitFor();
  await page.locator('.dataset-exclusion-list').getByText('LABEL_DELAY_EXCEEDED', { exact: true }).waitFor();
  assert.equal(await page.locator('.dataset-sample-frame tbody tr').count(), 3);
  assert.match(await page.locator('.facts-grid .mono-value').last().textContent(), /^[a-f0-9]{64}$/);

  await page.getByRole('button', { name: '生成 Parquet' }).click();
  await page.getByRole('heading', { name: '生成版本锁定对象' }).waitFor();
  await page.locator('#dataset-materialization-reason').fill('生成浏览器验收使用的版本锁定 Parquet 对象');
  await page.locator('#dataset-materialization-submit').click();
  await page.locator('[data-materialization-state="WRITING"]').waitFor();

  let snapshotResponse = await fetch(`${simulatorUrl}/bpi/v1/datasets?plantId=PLANT-01&limit=100`).then((item) => item.json());
  const snapshotId = snapshotResponse.data[0].latestSnapshot.id;
  snapshotResponse = await fetch(`${simulatorUrl}/bpi/v1/dataset-snapshots/${snapshotId}`).then((item) => item.json());
  const materializationId = snapshotResponse.data.latestMaterialization.id;
  response = await fetch(`${simulatorUrl}/__simulation/fail-dataset-materialization`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      materializationId,
      failureCode: 'SIMULATED_MINIO_TIMEOUT',
      failureDetail: '浏览器验收注入的对象存储超时。',
    }),
  });
  assert.equal(response.status, 200);
  await page.locator('[data-materialization-state="FAILED"]').waitFor();
  await page.getByText('SIMULATED_MINIO_TIMEOUT', { exact: true }).waitFor();
  await page.getByRole('button', { name: '重新排队' }).click();
  await page.getByRole('heading', { name: '重新排队 Parquet' }).waitFor();
  await page.locator('#dataset-materialization-reason').fill('对象存储恢复后从页面重新排队并复验');
  await page.locator('#dataset-materialization-submit').click();

  const materializationPanel = page.locator('[data-materialization-state="READY"]');
  await materializationPanel.waitFor();
  await materializationPanel.getByText('SIMULATED', { exact: true }).waitFor();
  assert.match(await materializationPanel.locator('.dataset-artifact-uri').textContent(), /^s3:\/\/bpi-datasets\/datasets\/.*\.parquet\?versionId=.+$/);
  assert.match(await materializationPanel.locator('.dataset-artifact-sha').textContent(), /^[a-f0-9]{64}$/);
  assert.deepEqual(await page.locator('.dataset-delivery-grid .status').allTextContents(), [
    'MANIFEST_READY', 'READY', 'NOT_STARTED', 'NOT_STARTED', 'NOT_STARTED',
  ]);

  await page.getByRole('button', { name: '发布 Iceberg' }).click();
  await page.getByRole('heading', { name: '发布版本锁定对象' }).waitFor();
  await page.locator('#dataset-catalog-publication-reason').fill('发布浏览器验收使用的精确 Parquet 版本');
  await page.locator('#dataset-catalog-publication-submit').click();
  await page.locator('[data-catalog-state="COMMITTING"]').waitFor();

  let publicationResponse = await fetch(`${simulatorUrl}/bpi/v1/dataset-materializations/${materializationId}/catalog-publications`).then((item) => item.json());
  const publicationId = publicationResponse.data.id;
  response = await fetch(`${simulatorUrl}/__simulation/fail-dataset-catalog-publication`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      publicationId,
      failureCode: 'SIMULATED_POLARIS_TIMEOUT',
      failureDetail: '浏览器验收注入的 Polaris 目录提交超时。',
    }),
  });
  assert.equal(response.status, 200);
  await page.locator('[data-catalog-state="FAILED"]').waitFor();
  await page.getByText('SIMULATED_POLARIS_TIMEOUT', { exact: true }).waitFor();
  await page.getByRole('button', { name: '重试 Iceberg' }).click();
  await page.getByRole('heading', { name: '重新排队 Iceberg 发布' }).waitFor();
  await page.locator('#dataset-catalog-publication-reason').fill('目录服务恢复后重新排队并复验快照');
  await page.locator('#dataset-catalog-publication-submit').click();

  const catalogPanel = page.locator('[data-catalog-state="READY"]');
  await catalogPanel.waitFor();
  assert.equal(await catalogPanel.locator('.dataset-iceberg-snapshot').textContent(), '9223372036854775001');
  assert.match(await catalogPanel.locator('.dataset-table-identifier').textContent(), /^ft_mes_bpi\.bpi_training\.tenant_[a-f0-9]{16}\.dataset_[a-f0-9]+$/);
  assert.match(await catalogPanel.locator('.dataset-semantic-sha').textContent(), /^[a-f0-9]{64}$/);
  assert.deepEqual(await page.locator('.dataset-delivery-grid .status').allTextContents(), [
    'MANIFEST_READY', 'READY', 'READY', 'NOT_STARTED', 'NOT_STARTED',
  ]);

  await page.getByRole('button', { name: '创建恢复包' }).click();
  await page.getByRole('heading', { name: '创建不可变恢复包' }).waitFor();
  await page.locator('#dataset-retention-archive-reason').fill('为精确 Iceberg 快照创建浏览器验收恢复包');
  await page.locator('#dataset-retention-archive-submit').click();
  await page.locator('[data-retention-state="ARCHIVING"]').waitFor();

  let archiveResponse = await fetch(`${simulatorUrl}/bpi/v1/dataset-catalog-publications/${publicationId}/retention-archives`).then((item) => item.json());
  const archiveId = archiveResponse.data.id;
  response = await fetch(`${simulatorUrl}/__simulation/fail-dataset-retention-archive`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      archiveId,
      failureCode: 'SIMULATED_OBJECT_LOCK_TIMEOUT',
      failureDetail: '浏览器验收注入的 Object Lock 写入超时。',
    }),
  });
  assert.equal(response.status, 200);
  await page.locator('[data-retention-state="FAILED"]').waitFor();
  await page.getByText('SIMULATED_OBJECT_LOCK_TIMEOUT', { exact: true }).waitFor();
  await page.getByRole('button', { name: '重试恢复包' }).click();
  await page.getByRole('heading', { name: '重新排队恢复包' }).waitFor();
  await page.locator('#dataset-retention-archive-reason').fill('对象存储恢复后复用同一归档任务并重新校验');
  await page.locator('#dataset-retention-archive-submit').click();

  const archivePanel = page.locator('[data-retention-state="LOCKED"]');
  await archivePanel.waitFor();
  assert.match(await archivePanel.locator('.dataset-retention-prefix').textContent(), /^s3:\/\/bpi-dataset-recovery\/archives\/tenant_[a-f0-9]{16}\/.+$/);
  assert.match(await archivePanel.locator('.dataset-retained-source-version').textContent(), /^[a-f0-9-]{36}$/);
  assert.match(await archivePanel.locator('.dataset-retained-manifest-version').textContent(), /^[a-f0-9-]{36}$/);
  assert.match(await archivePanel.locator('.dataset-retention-manifest-sha').textContent(), /^[a-f0-9]{64}$/);
  assert.equal(await archivePanel.locator('.dataset-retention-semantic-sha').textContent(),
    await catalogPanel.locator('.dataset-semantic-sha').textContent());
  assert.deepEqual(await page.locator('.dataset-delivery-grid .status').allTextContents(), [
    'MANIFEST_READY', 'READY', 'READY', 'LOCKED', 'NOT_STARTED',
  ]);

  publicationResponse = await fetch(`${simulatorUrl}/bpi/v1/dataset-catalog-publications/${publicationId}`).then((item) => item.json());
  const materializedResponse = await fetch(`${simulatorUrl}/bpi/v1/dataset-materializations/${materializationId}`).then((item) => item.json());
  assert.equal(publicationResponse.data.state, 'READY');
  assert.equal(publicationResponse.data.icebergSnapshotId, '9223372036854775001');
  assert.equal(publicationResponse.data.sourceContentSha256, materializedResponse.data.contentSha256);
  assert.equal(publicationResponse.data.sourceObjectVersionId, materializedResponse.data.artifactMetadata.objectVersionId);
  assert.equal(publicationResponse.data.catalogMetadata.catalogSnapshotVerified, true);
  archiveResponse = await fetch(`${simulatorUrl}/bpi/v1/dataset-retention-archives/${archiveId}`).then((item) => item.json());
  assert.equal(archiveResponse.data.state, 'LOCKED');
  assert.equal(archiveResponse.data.catalogSemanticChecksum, publicationResponse.data.semanticChecksum);
  assert.equal(archiveResponse.data.verifiedSemanticChecksum, publicationResponse.data.semanticChecksum);
  assert.equal(archiveResponse.data.archiveMetadata.objectLockVerified, true);
  assert.equal(archiveResponse.data.archiveMetadata.recoveryVerified, true);
  assert.equal(archiveResponse.data.archiveMetadata.mlflowRegistered, false);
  assert.equal(archiveResponse.data.archiveMetadata.modelTrained, false);

  const definitions = await fetch(`${simulatorUrl}/bpi/v1/datasets?plantId=PLANT-01&limit=100`).then((item) => item.json());
  assert.equal(definitions.data.length, 1);
  assert.equal(definitions.data[0].latestSnapshot.state, 'MANIFEST_READY');
  assert.equal(definitions.data[0].latestSnapshot.materializationState, 'READY');
  const snapshot = await fetch(`${simulatorUrl}/bpi/v1/dataset-snapshots/${definitions.data[0].latestSnapshot.id}`).then((item) => item.json());
  assert.equal(snapshot.data.manifest.phaseBoundary.deliveryState, 'MANIFEST_ONLY');
  assert.equal(snapshot.data.manifest.phaseBoundary.materializationState, 'NOT_STARTED');
  assert.equal(snapshot.data.manifest.phaseBoundary.artifactUri, null);
  assert.equal(snapshot.data.manifest.phaseBoundary.icebergReady, false);
  assert.equal(snapshot.data.manifest.phaseBoundary.mlflowRegistered, false);
  assert.equal(snapshot.data.manifest.phaseBoundary.modelTrained, false);
  assert.equal(snapshot.data.includedCount, 1);
  assert.equal(snapshot.data.excludedCount, 2);
  assert.equal(snapshot.data.materializationState, 'READY');
  assert.equal(snapshot.data.latestMaterialization.id, materializationId);
  assert.equal(snapshot.data.latestMaterialization.artifactMetadata.objectContentVerified, true);
  assert.equal(snapshot.data.latestMaterialization.artifactMetadata.simulationOnly, true);
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('[data-dataset-id]').click();
  await page.getByRole('button', { name: '查看最近快照' }).click();
  await page.locator('[data-catalog-state="READY"]').waitFor();
  await page.locator('[data-retention-state="LOCKED"]').waitFor();
  assert.equal(await page.locator('.dataset-iceberg-snapshot').textContent(), '9223372036854775001',
    'page reload must rediscover the publication from the materialization');
  assert.match(await page.locator('.dataset-retention-manifest-sha').textContent(), /^[a-f0-9]{64}$/,
    'page reload must rediscover the immutable recovery package from the publication');
  await page.screenshot({ path: '/tmp/bpi-dataset-object-lock-desktop.png', fullPage: true });
  assert.deepEqual(errors, []);
  await desktop.close();

  const mobile = await browser.newContext({ viewport: { width: 390, height: 844 } });
  const mobilePage = await mobile.newPage();
  const mobileErrors = observe(mobilePage);
  await mobilePage.goto(`${APP_URL}/#/datasets`, { waitUntil: 'networkidle' });
  await mobilePage.getByRole('heading', { name: '数据集清单' }).waitFor();
  const geometry = await mobilePage.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
  assert.ok(geometry.body <= geometry.viewport + 1, `dataset page overflows viewport: ${JSON.stringify(geometry)}`);
  await mobilePage.locator('[data-dataset-id]').click();
  await assertDrawerSettled(mobilePage);
  await mobilePage.getByRole('heading', { name: 'ADP E2E 启动边界清单' }).waitFor();
  await mobilePage.getByRole('button', { name: '查看最近快照' }).click();
  await assertDrawerSettled(mobilePage);
  assert.equal(await mobilePage.locator('#detail-drawer').evaluate((element) => element.scrollTop), 0,
    'opening another dataset object must reset the drawer to its header');
  await mobilePage.locator('[data-materialization-state="READY"]').waitFor();
  await mobilePage.locator('[data-catalog-state="READY"]').waitFor();
  await mobilePage.locator('[data-retention-state="LOCKED"]').waitFor();
  assert.equal(await mobilePage.locator('.dataset-iceberg-snapshot').textContent(), '9223372036854775001');
  const drawerGeometry = await mobilePage.evaluate(() => ({ body: document.body.scrollWidth, viewport: window.innerWidth }));
  assert.ok(drawerGeometry.body <= drawerGeometry.viewport + 1, `dataset drawer overflows viewport: ${JSON.stringify(drawerGeometry)}`);
  await mobilePage.screenshot({ path: '/tmp/bpi-dataset-object-lock-mobile.png', fullPage: true });
  assert.deepEqual(mobileErrors, []);
  await mobile.close();
});

test('shift lead confirms the END boundary and closes the raw batch', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/candidates`, { waitUntil: 'networkidle' });

  await page.locator('[data-candidate-id]').click();
  await page.getByRole('button', { name: '确认候选' }).click();
  await page.locator('#confirm-reason').fill('班长确认启动边界并建立待结束批次');
  await page.getByRole('button', { name: '确认并生成影子批次' }).click();
  await page.getByRole('heading', { name: 'S07-20260712-001' }).waitFor();
  assert.equal(await page.locator('#candidate-count').textContent(), '1');

  await page.locator('[data-view="candidates"]').click();
  await page.getByRole('heading', { name: '候选批次' }).waitFor();
  assert.equal(await page.locator('[data-candidate-id]').count(), 1);
  await page.locator('[data-candidate-id]').click();
  await page.getByText('END 候选').waitFor();
  await page.getByRole('button', { name: '确认候选' }).click();
  await page.getByRole('heading', { name: '确认结束边界' }).waitFor();
  await page.locator('#confirm-reason').fill('流量归零且泵阀路径停止，确认结束边界');
  await page.getByRole('button', { name: '确认并关闭原始批次' }).click();

  await page.getByText('已关闭为 CLOSED_RAW').waitFor();
  await page.locator('.batch-state-band').getByText('CLOSED_RAW', { exact: true }).waitFor();
  assert.match(await page.locator('.batch-state-band').textContent(), /revision 2/);
  await page.getByText('4 START / 3 END', { exact: true }).waitFor();
  await page.getByText('END_BOUNDARY_CONFIRMED', { exact: true }).waitFor();
  assert.equal(await page.getByRole('button', { name: '暂停自动处理' }).count(), 0);
  assert.equal(await page.getByRole('button', { name: '恢复自动处理' }).count(), 0);
  assert.equal(await page.locator('#candidate-count').textContent(), '0');

  const batch = await fetch(`${simulatorUrl}/bpi/v1/batches/BATCH-S07-20260712-001`).then((response) => response.json());
  const line = await fetch(`${simulatorUrl}/bpi/v1/lines/LINE-S07-01/current-state`).then((response) => response.json());
  assert.equal(batch.data.state, 'CLOSED_RAW');
  assert.equal(batch.data.revision, 2);
  assert.equal(batch.data.endTime, '2026-07-12T08:29:40.000Z');
  assert.equal(line.data.status, 'IDLE');
  assert.equal(line.data.currentBatchId, null);
  await page.screenshot({ path: '/tmp/bpi-console-end-boundary.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('shift lead suspends and resumes a batch from the detail drawer', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/candidates`, { waitUntil: 'networkidle' });

  await page.locator('[data-candidate-id]').click();
  await page.getByRole('button', { name: '确认候选' }).click();
  await page.locator('#confirm-reason').fill('班长确认边界并创建生命周期验收批次');
  await page.getByRole('button', { name: '确认并生成影子批次' }).click();
  await page.getByRole('heading', { name: 'S07-20260712-001' }).waitFor();

  await page.getByRole('button', { name: '暂停自动处理' }).click();
  await page.getByRole('heading', { name: '暂停批次自动处理' }).waitFor();
  await page.locator('#confirm-reason').fill('上游制造指令上下文已过期');
  await page.getByRole('button', { name: '确认暂停' }).click();
  await page.getByText('批次自动处理已暂停').waitFor();
  await page.locator('.batch-state-band').getByText('SUSPENDED', { exact: true }).waitFor();
  assert.match(await page.locator('.batch-state-band').textContent(), /revision 2/);
  await page.getByText('BATCH_SUSPENDED', { exact: true }).waitFor();

  await page.getByRole('button', { name: '恢复自动处理' }).click();
  await page.getByRole('heading', { name: '恢复批次自动处理' }).waitFor();
  await page.locator('#confirm-reason').fill('上游制造指令上下文已恢复并完成复核');
  await page.getByRole('button', { name: '确认恢复' }).click();
  await page.getByText('批次自动处理已恢复').waitFor();
  await page.locator('.batch-state-band').getByText('ACTIVE', { exact: true }).waitFor();
  assert.match(await page.locator('.batch-state-band').textContent(), /revision 3/);
  await page.getByText('BATCH_RESUMED', { exact: true }).waitFor();

  const batch = await fetch(`${simulatorUrl}/bpi/v1/batches/BATCH-S07-20260712-001`).then((response) => response.json());
  const timeline = await fetch(`${simulatorUrl}/bpi/v1/batches/BATCH-S07-20260712-001/timeline`).then((response) => response.json());
  assert.equal(batch.data.state, 'ACTIVE');
  assert.equal(batch.data.revision, 3);
  assert.deepEqual(timeline.data.map((item) => item.action), [
    'SHADOW_BATCH_CREATED',
    'BATCH_SUSPENDED',
    'BATCH_RESUMED',
  ]);
  await page.screenshot({ path: '/tmp/bpi-console-batch-lifecycle.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('batch detail completes recoverable two-step force-close without console or network errors', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/candidates`, { waitUntil: 'networkidle' });

  await page.locator('[data-candidate-id]').click();
  await page.getByRole('button', { name: '确认候选' }).click();
  await page.locator('#confirm-reason').fill('班长确认边界并创建强制结束验收批次');
  await page.getByRole('button', { name: '确认并生成影子批次' }).click();
  await page.getByRole('heading', { name: 'S07-20260712-001' }).waitFor();

  await page.getByRole('button', { name: '申请强制结束' }).click();
  await page.getByRole('heading', { name: '申请强制结束批次' }).waitFor();
  await page.locator('#command-boundary-time').fill('2026-07-12T16:20');
  await page.locator('#confirm-reason').fill('蒸发循环泵故障停机，按现场流量归零时间申请结束');
  await page.getByRole('button', { name: '提交独立审批' }).click();

  await page.getByText('强制结束申请已提交，等待独立管理员审批').waitFor();
  await page.locator('[data-force-close-state="PENDING_APPROVAL"]').waitFor();
  await page.getByText('BATCH_FORCE_CLOSE_REQUESTED', { exact: true }).waitFor();
  assert.match(await page.locator('.batch-state-band').textContent(), /revision 2/);
  assert.equal(await page.getByRole('button', { name: '暂停自动处理' }).count(), 0);
  assert.equal(await page.getByRole('button', { name: '恢复自动处理' }).count(), 0);

  await page.getByRole('button', { name: '批准并强制结束' }).click();
  await page.getByRole('heading', { name: '批准强制结束批次' }).waitFor();
  assert.match(await page.locator('#command-boundary-time').inputValue(), /^2026-07-12T16:20(?::00)?$/);
  await page.locator('#confirm-reason').fill('独立复核设备停机记录、阀路状态和流量归零时间');
  await page.getByRole('button', { name: '批准并关闭批次' }).click();

  await page.getByText('强制结束已批准，批次已关闭为 CLOSED_RAW').waitFor();
  await page.locator('.batch-state-band').getByText('CLOSED_RAW', { exact: true }).waitFor();
  await page.getByText('BATCH_FORCE_CLOSED', { exact: true }).waitFor();
  assert.match(await page.locator('.batch-state-band').textContent(), /revision 3/);
  assert.equal(await page.getByRole('button', { name: '申请强制结束' }).count(), 0);
  assert.equal(await page.getByRole('button', { name: '批准并强制结束' }).count(), 0);

  const batch = await fetch(`${simulatorUrl}/bpi/v1/batches/BATCH-S07-20260712-001`).then((response) => response.json());
  const task = await fetch(`${simulatorUrl}/bpi/v1/batches/BATCH-S07-20260712-001/force-close`).then((response) => response.json());
  assert.equal(batch.data.state, 'CLOSED_RAW');
  assert.equal(batch.data.revision, 3);
  assert.equal(batch.data.endTime, '2026-07-12T08:20:00.000Z');
  assert.equal(task.data.state, 'COMPLETED');
  assert.notEqual(task.data.requestedBy, task.data.decidedBy);
  await page.screenshot({ path: '/tmp/bpi-console-force-close.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('batch detail presents quality release and WMS truth without inferring success from HTTP status', async () => {
  const batchIds = await prepareBatchReleaseScenario();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();
  const errors = observe(page);
  const releaseOperations = [];
  page.on('response', (response) => {
    if (response.url().endsWith('/release')) {
      releaseOperations.push(response.headers()['x-bpi-operation-id'] || null);
    }
  });
  await page.goto(`${APP_URL}/#/batches`, { waitUntil: 'networkidle' });
  await page.getByRole('heading', { name: '批次档案' }).waitFor();
  assert.equal(await page.locator('[data-batch-id]').count(), 6);
  const drawer = page.locator('#detail-drawer');

  await page.route(`**/bpi-api/batches/${batchIds.inbounded}/evidence`, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ data: { start: [], end: [] }, meta: {} }),
    });
  });

  await page.locator(`[data-batch-id="${batchIds.closedRaw}"]`).click();
  await drawer.getByText('尚未进入质量放行', { exact: true }).waitFor();
  await drawer.getByText('尚未生成入库命令', { exact: true }).waitFor();
  assert.equal(await drawer.getByText('已入库', { exact: true }).count(), 0);
  await drawer.locator('[data-close-drawer]').first().click();

  const waitingReleaseUrl = `**/bpi-api/batches/${batchIds.waiting}/release`;
  await page.route(waitingReleaseUrl, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 600));
    await route.continue();
  });
  await page.locator(`[data-batch-id="${batchIds.waiting}"]`).click();
  await drawer.getByText('正在读取质量门和入库回执', { exact: true }).waitFor();
  await drawer.getByText('等待 1 项必检项目完成', { exact: true }).waitFor();
  await drawer.getByText('FG-MICRO', { exact: true }).waitFor();
  await drawer.getByText('待判定 · 待最终确认', { exact: true }).waitFor();
  assert.match(await drawer.textContent(), /QCS-GATE-ADP-E2E-002 · r1/);
  await page.unroute(waitingReleaseUrl);
  await drawer.locator('[data-close-drawer]').first().click();

  await page.locator(`[data-batch-id="${batchIds.rejected}"]`).click();
  await drawer.getByText('存在不合格必检项目', { exact: true }).waitFor();
  await drawer.getByText('不合格 · 最终结果', { exact: true }).waitFor();
  assert.equal(await drawer.getByText('已入库', { exact: true }).count(), 0);
  await drawer.locator('[data-close-drawer]').first().click();

  await page.locator(`[data-batch-id="${batchIds.wmsPending}"]`).click();
  await drawer.getByText('入库处理中', { exact: true }).waitFor();
  await drawer.getByText(`WMS-INBOUND-${batchIds.wmsPending}`, { exact: true }).waitFor();
  assert.equal(await drawer.getByText('已入库', { exact: true }).count(), 0);
  const originalCommandEventId = await drawer.locator('.release-technical dd').first().textContent();
  await drawer.getByRole('button', { name: '重新核对原单' }).click();
  await page.getByRole('heading', { name: '重新核对原 WMS 单据' }).waitFor();
  await page.getByText('先查原单 · 同一幂等键', { exact: true }).waitFor();
  await page.locator('#confirm-reason').fill('WMS 回执超时，管理员确认按原命令查单');
  await page.getByRole('button', { name: '确认核对原单' }).click();
  await page.getByText('原入库命令已进入重新核对队列', { exact: true }).waitFor();
  await drawer.getByText('原命令正在队列中处理', { exact: true }).waitFor();
  assert.equal(await drawer.locator('.release-technical dd').first().textContent(), originalCommandEventId);
  const reconciledRelease = await fetch(`${simulatorUrl}/bpi/v1/batches/${batchIds.wmsPending}/release`)
    .then((response) => response.json());
  assert.equal(reconciledRelease.data.wmsInbound.outboxStatus, 'PENDING');
  assert.equal(reconciledRelease.data.wmsInbound.reconciliationCount, 1);
  assert.equal(reconciledRelease.data.wmsInbound.revision, 2);
  await drawer.locator('[data-close-drawer]').first().click();

  await page.locator(`[data-batch-id="${batchIds.wmsFailed}"]`).click();
  await drawer.getByText('入库失败', { exact: true }).waitFor();
  await drawer.getByText('WMS_LOCATION_LOCKED', { exact: true }).waitFor();
  await drawer.locator('[data-release-wms="REJECTED"] .release-summary p')
    .getByText('目标成品库位正在盘点锁定，WMS 拒绝本次入库命令。', { exact: true }).waitFor();
  assert.equal(await drawer.getByText('已入库', { exact: true }).count(), 0);
  await drawer.locator('[data-close-drawer]').first().click();

  await page.locator(`[data-batch-id="${batchIds.inbounded}"]`).click();
  await drawer.getByText('已入库', { exact: true }).waitFor();
  await drawer.locator('[data-release-wms="ACCEPTED"] .release-facts')
    .getByText('WMS-IN-ADP-E2E-0001', { exact: true }).waitFor();
  await drawer.locator('[data-release-stage="inbounded"]').getByText('WMS-IN-ADP-E2E-0001', { exact: true }).waitFor();
  const emptyEvidence = drawer.locator('.evidence-empty');
  await emptyEvidence.getByText('暂无证据', { exact: true }).waitFor();
  const emptyEvidenceBox = await emptyEvidence.boundingBox();
  assert.ok(emptyEvidenceBox && emptyEvidenceBox.width > 80 && emptyEvidenceBox.height < 80,
    `empty evidence must render horizontally: ${JSON.stringify(emptyEvidenceBox)}`);
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-batch-quality-inventory.png', fullPage: true });

  await drawer.locator('[data-close-drawer]').first().click();
  const closeRaceUrl = `**/bpi-api/batches/${batchIds.closedRaw}/release`;
  await page.route(closeRaceUrl, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 500));
    await route.continue();
  }, { times: 1 });
  await page.locator(`[data-batch-id="${batchIds.closedRaw}"]`).click();
  await drawer.getByText('正在核对批次档案', { exact: true }).waitFor();
  await drawer.locator('[data-close-drawer]').first().click();
  await page.waitForTimeout(700);
  assert.equal(await drawer.getAttribute('aria-hidden'), 'true');

  assert.ok(releaseOperations.length >= 6);
  assert.ok(releaseOperations.every((operation) => operation === 'getBatchRelease'));
  assert.deepEqual(errors, []);
  await context.close();
});

test('batch detail completes four-eye WMS reversal and keeps the original blue document visible', async () => {
  const batchIds = await prepareBatchReleaseScenario();
  const batchId = batchIds.inbounded;
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();
  const errors = observe(page);
  const reversalRequests = [];
  const reversalOperations = [];
  page.on('request', (request) => {
    if (request.method() === 'POST' && request.url().endsWith(`/batches/${batchId}/wms/reversal`)) {
      reversalRequests.push({
        method: request.method(),
        headers: request.headers(),
        body: request.postDataJSON(),
      });
    }
  });
  page.on('response', (response) => {
    if (response.request().method() === 'POST' && response.url().endsWith(`/batches/${batchId}/wms/reversal`)) {
      reversalOperations.push(response.headers()['x-bpi-operation-id'] || null);
    }
  });

  await page.goto(`${APP_URL}/#/batches`, { waitUntil: 'networkidle' });
  await page.getByRole('heading', { name: '批次档案' }).waitFor();
  const drawer = page.locator('#detail-drawer');
  await page.locator(`[data-batch-id="${batchId}"]`).click();
  await drawer.locator('[data-release-reversal="AVAILABLE"]').waitFor();
  await drawer.getByText('原入库单可申请冲销', { exact: true }).waitFor();
  await drawer.getByRole('button', { name: '申请入库冲销' }).click();
  await page.getByRole('heading', { name: '申请完工入库冲销' }).waitFor();
  await page.getByText('INBOUNDED → 待独立审批', { exact: true }).waitFor();
  await page.locator('#confirm-reason').fill('原完工入库业务单据录入错误，申请红单冲销');
  await page.getByRole('button', { name: '提交独立审批' }).click();
  await page.locator('#toast').getByText('入库冲销申请已提交，等待独立管理员审批', { exact: true }).waitFor();
  await drawer.locator('[data-release-reversal="PENDING_APPROVAL"]').waitFor();
  await drawer.getByText('等待独立管理员审批', { exact: true }).waitFor();
  assert.equal(await drawer.locator('[data-original-document]').textContent(), 'WMS-IN-ADP-E2E-0001');

  await drawer.getByRole('button', { name: '独立审批冲销' }).click();
  await page.getByRole('heading', { name: '批准完工入库冲销' }).waitFor();
  await page.getByText('simulated.shift.lead', { exact: true }).waitFor();
  await page.locator('#confirm-reason').fill('独立复核原蓝单、物料、数量和申请依据一致');
  await page.getByRole('button', { name: '批准并生成红单' }).click();
  await page.locator('#toast').getByText('冲销已批准，红单命令已进入 WMS 队列', { exact: true }).waitFor();
  await drawer.locator('[data-release-reversal="PENDING_WMS"]').waitFor();
  await drawer.getByText('红单命令等待 WMS 回执', { exact: true }).waitFor();
  assert.equal(await drawer.locator('[data-original-document]').textContent(), 'WMS-IN-ADP-E2E-0001');

  let release = await fetch(`${simulatorUrl}/bpi/v1/batches/${batchId}/release`).then((response) => response.json());
  assert.equal(release.data.batch.state, 'INBOUND_REVERSING');
  assert.equal(release.data.wmsInbound.documentId, 'WMS-IN-ADP-E2E-0001');
  assert.equal(release.data.wmsInboundReversal.state, 'PENDING_WMS');
  assert.notEqual(release.data.wmsInboundReversal.reversalCommandEventId, release.data.wmsInbound.commandEventId);

  let response = await fetch(`${simulatorUrl}/__simulation/complete-wms-inbound-reversal`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ batchId, status: 'ACCEPTED', reversalDocumentId: 'WMS-RED-ADP-E2E-0001' }),
  });
  assert.equal(response.status, 200);
  await drawer.locator('[data-close-drawer]').first().click();
  await page.locator(`[data-batch-id="${batchId}"]`).click();
  await drawer.locator('[data-release-reversal="COMPLETED"]').waitFor();
  await drawer.getByText('完工入库已冲销', { exact: true }).waitFor();
  await drawer.getByText('WMS-RED-ADP-E2E-0001', { exact: true }).waitFor();
  assert.equal(await drawer.locator('[data-original-document]').textContent(), 'WMS-IN-ADP-E2E-0001');
  await drawer.getByText(/原蓝单 WMS-IN-ADP-E2E-0001.*始终只读保留/).waitFor();
  await assertDrawerSettled(page);
  const completedReversal = drawer.locator('[data-release-reversal="COMPLETED"]');
  await completedReversal.screenshot({ path: '/tmp/bpi-console-wms-inbound-reversal.png' });

  await page.setViewportSize({ width: 390, height: 844 });
  await completedReversal.scrollIntoViewIfNeeded();
  const mobileBox = await completedReversal.boundingBox();
  assert.ok(mobileBox && mobileBox.x >= 0 && mobileBox.x + mobileBox.width <= 391,
    `reversal block must fit the mobile viewport: ${JSON.stringify(mobileBox)}`);
  assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth), true);
  await completedReversal.screenshot({ path: '/tmp/bpi-console-wms-inbound-reversal-mobile.png' });

  release = await fetch(`${simulatorUrl}/bpi/v1/batches/${batchId}/release`).then((item) => item.json());
  assert.equal(release.data.batch.state, 'INBOUND_REVERSED');
  assert.equal(release.data.batch.wmsStatus, 'REVERSED');
  assert.equal(release.data.wmsInbound.documentId, 'WMS-IN-ADP-E2E-0001');
  assert.equal(release.data.wmsInboundReversal.reversalDocumentId, 'WMS-RED-ADP-E2E-0001');
  assert.equal(reversalRequests.length, 2);
  assert.deepEqual(reversalRequests.map((item) => item.method), ['POST', 'POST']);
  assert.deepEqual(reversalRequests.map((item) => item.body.approvalMode), ['REQUEST', 'APPROVE']);
  assert.ok(reversalRequests.every((item) => item.headers['idempotency-key']));
  assert.deepEqual(reversalRequests.map((item) => item.headers['if-match']), ['7', '8']);
  assert.deepEqual(reversalOperations, ['commandWmsInboundReversal', 'commandWmsInboundReversal']);
  assert.deepEqual(errors, []);
  await context.close();
});

test('mobile batch detail keeps core facts visible when the release projection fails and recovers locally', async () => {
  const batchIds = await prepareBatchReleaseScenario();
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
    isMobile: true,
    hasTouch: true,
  });
  const page = await context.newPage();
  const errors = observe(page);
  const releaseUrl = `**/bpi-api/batches/${batchIds.wmsFailed}/release`;
  await page.route(releaseUrl, async (route) => {
    await route.fulfill({
      status: 503,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        title: 'Release Projection Unavailable',
        status: 503,
        detail: 'QCS/WMS 投影暂时不可用',
        traceId: 'ADP-E2E-RELEASE-TRACE-503',
      }),
    });
  });
  await page.goto(`${APP_URL}/#/batches`, { waitUntil: 'networkidle' });
  await page.locator(`[data-batch-id="${batchIds.wmsFailed}"]`).click();
  const drawer = page.locator('#detail-drawer');
  await drawer.getByText('质量与库存暂不可用', { exact: true }).waitFor();
  await drawer.getByText('QCS/WMS 投影暂时不可用', { exact: true }).waitFor();
  await drawer.getByText('traceId ADP-E2E-RELEASE-TRACE-503', { exact: true }).waitFor();
  await drawer.getByText('2 START / 2 END', { exact: true }).waitFor();
  await drawer.getByText('WMS_INBOUND_REJECTED', { exact: true }).waitFor();
  assert.equal(await drawer.getByText('批次档案暂不可用', { exact: true }).count(), 0);

  await page.unroute(releaseUrl);
  await page.route(releaseUrl, async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 500));
    await route.continue();
  }, { times: 1 });
  await drawer.getByRole('button', { name: '重试' }).click();
  await drawer.getByText('正在读取质量门和入库回执', { exact: true }).waitFor();
  await drawer.getByText('入库失败', { exact: true }).waitFor();
  await drawer.getByText('WMS_LOCATION_LOCKED', { exact: true }).waitFor();
  await assertDrawerSettled(page);
  const geometry = await drawer.evaluate((element) => ({
    clientWidth: element.clientWidth,
    scrollWidth: element.scrollWidth,
    viewportWidth: window.innerWidth,
  }));
  assert.ok(geometry.scrollWidth <= geometry.clientWidth + 1, `drawer overflows horizontally: ${JSON.stringify(geometry)}`);
  assert.ok(geometry.clientWidth <= geometry.viewportWidth, `drawer exceeds viewport: ${JSON.stringify(geometry)}`);
  await drawer.locator('[data-release-wms="REJECTED"]').scrollIntoViewIfNeeded();
  await page.screenshot({ path: '/tmp/bpi-console-batch-quality-inventory-mobile.png', fullPage: true });
  const expectedFailures = errors.filter((error) => error.includes('503 (Service Unavailable)'));
  assert.equal(expectedFailures.length, 1);
  assert.deepEqual(errors.filter((error) => !error.includes('503 (Service Unavailable)')), []);
  await context.close();
});

test('administrator governs scoped feature flags while phase locks remain read only', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/featureFlags`, { waitUntil: 'networkidle' });

  await page.getByRole('heading', { name: '运行开关' }).waitFor();
  assert.equal(await page.locator('.feature-flag-table tbody tr').count(), 6);
  assert.match(await page.locator('.feature-flag-summary').textContent(), /受控开关6/);
  assert.match(await page.locator('.feature-flag-target-note').textContent(), /PLANT-01 \/ LINE-S07-01/);

  const commandsRow = page.locator('[data-feature-flag-row="bpi.commands"]');
  await commandsRow.getByText('已启用', { exact: true }).waitFor();
  await commandsRow.getByText('显式启用', { exact: true }).waitFor();
  assert.match(await commandsRow.textContent(), /产线.*r1/);

  const uiRow = page.locator('[data-feature-flag-row="bpi.ui"]');
  await uiRow.getByText('后端已执行', { exact: true }).waitFor();
  assert.match(await uiRow.textContent(), /旧 MES 菜单由 Java 8 adapter/);
  assert.equal(await uiRow.getByRole('button').count(), 3);

  const wmsRow = page.locator('[data-feature-flag-row="bpi.wms-link"]');
  await wmsRow.getByText('阶段门禁锁定', { exact: true }).waitFor();
  assert.match(await wmsRow.textContent(), /QCS\/WMS Phase 2/);
  assert.equal(await wmsRow.getByRole('button').count(), 0);

  await commandsRow.getByRole('button', { name: '禁用' }).click();
  await page.getByRole('heading', { name: '禁用运行开关' }).waitFor();
  assert.match(await page.locator('#feature-flag-command-summary').textContent(), /If-Match r1/);
  await page.locator('#feature-flag-reason').fill('ADP_E2E_FEATURE_FLAG 禁用产线人工命令');
  await page.getByRole('button', { name: '确认禁用' }).click();
  await page.getByText('批次人工命令 已禁用').waitFor();
  await commandsRow.getByText('显式禁用', { exact: true }).waitFor();
  assert.match(await commandsRow.textContent(), /r2/);

  let flags = await fetch(`${simulatorUrl}/bpi/v1/feature-flags?plantId=PLANT-01&lineId=LINE-S07-01&scopeType=LINE`)
    .then((response) => response.json()).then((body) => body.data);
  let commands = flags.find((item) => item.flagKey === 'bpi.commands');
  assert.equal(commands.effectiveEnabled, false);
  assert.equal(commands.overrideActive, true);
  assert.equal(commands.overrideRevision, 2);

  await commandsRow.getByRole('button', { name: '继承' }).click();
  await page.getByRole('heading', { name: '恢复上级继承' }).waitFor();
  await page.locator('#feature-flag-reason').fill('ADP_E2E_FEATURE_FLAG 移除产线覆盖恢复继承');
  await page.getByRole('button', { name: '确认恢复继承' }).click();
  await page.getByText('批次人工命令 已恢复继承').waitFor();
  await commandsRow.getByText('继承上级', { exact: true }).waitFor();
  await commandsRow.getByText('平台默认', { exact: true }).waitFor();
  assert.match(await commandsRow.textContent(), /覆盖已移除 · r3/);

  flags = await fetch(`${simulatorUrl}/bpi/v1/feature-flags?plantId=PLANT-01&lineId=LINE-S07-01&scopeType=LINE`)
    .then((response) => response.json()).then((body) => body.data);
  commands = flags.find((item) => item.flagKey === 'bpi.commands');
  assert.equal(commands.effectiveEnabled, false);
  assert.equal(commands.effectiveScopeType, 'GLOBAL');
  assert.equal(commands.overrideActive, false);
  assert.equal(commands.overrideRevision, 3);

  await page.locator('#toast').evaluate((element) => { element.className = 'toast'; });
  await page.screenshot({ path: '/tmp/bpi-console-feature-flags.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('team completes a shadow run acceptance and critical data quality blocks approval until resolved', async () => {
  let response = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(response.status, 200);
  response = await fetch(`${simulatorUrl}/__simulation/prepare-shadow-run`, { method: 'POST' });
  assert.equal(response.status, 200);

  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/shadowRuns`, { waitUntil: 'networkidle' });
  await page.getByRole('heading', { name: '影子运行验收' }).waitFor();
  await page.getByText('当前筛选范围没有影子运行').waitFor();

  await page.getByRole('button', { name: '新建影子运行' }).click();
  await page.getByRole('heading', { name: '新建影子运行' }).waitFor();
  await page.locator('#shadow-run-code').fill('SHADOW-E2E-UI');
  await page.locator('#shadow-run-name').fill('影子运行端到端验收');
  assert.match(await page.locator('#shadow-run-rule').inputValue(), /^[0-9a-f-]{36}$/);
  await page.locator('#shadow-run-reason').fill('固定规则、拓扑和点位目录版本进行七天影子运行验收');
  await page.getByRole('button', { name: '创建验收任务' }).click();

  await page.getByRole('heading', { name: '影子运行端到端验收' }).waitFor();
  await page.getByText('RULE-S07-START@1.2.0', { exact: true }).waitFor();
  await page.getByText('TOPO-S07@3', { exact: true }).waitFor();
  const runId = await page.locator('[data-shadow-run-id]').getAttribute('data-shadow-run-id');
  assert.ok(runId);
  await page.getByRole('button', { name: '启动影子运行' }).click();
  await page.getByRole('heading', { name: '启动影子运行' }).waitFor();
  await page.locator('#confirm-reason').fill('确认固定版本的 Kafka、Flink 和点位准入均已就绪');
  await page.getByRole('button', { name: '确认启动' }).click();
  await page.getByText('影子运行已启动').waitFor();
  await page.locator('.batch-state-band').getByText('RUNNING', { exact: true }).waitFor();

  await page.getByRole('button', { name: '复核批次' }).click();
  await page.getByRole('heading', { name: '复核影子批次' }).waitFor();
  await page.locator('#shadow-review-end').evaluate((element) => {
    const parsed = new Date(element.value);
    parsed.setSeconds(parsed.getSeconds() + 61);
    const local = new Date(parsed.getTime() - parsed.getTimezoneOffset() * 60_000).toISOString().slice(0, 19);
    element.value = local;
    element.dispatchEvent(new Event('input', { bubbles: true }));
  });
  await page.locator('#shadow-review-reason').fill('人工复核首批，结束边界偏差六十一秒用于容差验收');
  await page.getByRole('button', { name: '提交批次复核' }).click();
  await page.getByText(/边界一致率 50\.00%/).waitFor();

  let run = await fetch(`${simulatorUrl}/bpi/v1/shadow-runs/${runId}`).then((item) => item.json()).then((body) => body.data);
  const reviews = await fetch(`${simulatorUrl}/bpi/v1/shadow-runs/${runId}/batch-reviews`).then((item) => item.json()).then((body) => body.data);
  const reviewedIds = new Set(reviews.map((item) => item.batchId));
  const batches = await fetch(`${simulatorUrl}/bpi/v1/batches?plantId=PLANT-01`).then((item) => item.json()).then((body) => body.data);
  let reviewIndex = 2;
  for (const batch of batches.filter((item) => !reviewedIds.has(item.id))) {
    response = await fetch(`${simulatorUrl}/bpi/v1/shadow-runs/${runId}/batch-reviews`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Idempotency-Key': `ui-shadow-review-${reviewIndex}`, 'If-Match': String(run.revision) },
      body: JSON.stringify({
        batchId: batch.id,
        manualStartTime: batch.startTime,
        manualEndTime: batch.endTime,
        referenceQuantity: batch.quantity,
        quantityUnit: batch.quantityUnit,
        reason: `人工复核第 ${reviewIndex} 个影子批次`,
      }),
    });
    assert.equal(response.status, 200);
    run = (await response.json()).data.run;
    reviewIndex += 1;
  }
  assert.equal(run.metrics.reviewedBatchCount, 10);
  assert.equal(run.metrics.boundaryAgreement, 0.95);

  await page.locator('#detail-drawer [data-close-drawer]').first().click();
  await page.getByRole('button', { name: '刷新' }).click();
  await page.locator(`[data-shadow-run-id="${runId}"]`).click();
  await page.getByRole('heading', { name: '影子运行端到端验收' }).waitFor();
  await page.locator('.shadow-metric-grid').getByText('10 / 10', { exact: true }).waitFor();
  await page.locator('.shadow-metric-grid').getByText('95.00% / 95.00%', { exact: true }).waitFor();
  await page.getByRole('button', { name: '结束观察并评估' }).click();
  await page.locator('#confirm-reason').fill('十个批次和七天观察周期均已完成，提交独立评估');
  await page.getByRole('button', { name: '确认进入评估' }).click();
  await page.getByText('观察期已结束，等待独立审批').waitFor();
  await page.locator('.batch-state-band').getByText('EVALUATING', { exact: true }).waitFor();
  await page.getByText('UNRESOLVED_CRITICAL_DATA_QUALITY', { exact: true }).waitFor();
  assert.equal(await page.getByRole('button', { name: '独立批准验收' }).isDisabled(), true);

  run = await fetch(`${simulatorUrl}/bpi/v1/shadow-runs/${runId}`).then((item) => item.json()).then((body) => body.data);
  response = await fetch(`${simulatorUrl}/bpi/v1/shadow-runs/${runId}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': 'ui-shadow-approve-blocked', 'If-Match': String(run.revision) },
    body: JSON.stringify({ reason: '验证严重数据质量事件未关闭时审批必须失败' }),
  });
  assert.equal(response.status, 422);

  await page.locator('[data-view="dataQuality"]').click();
  await page.getByRole('heading', { name: '数据质量事件' }).waitFor();
  await page.locator('[data-data-quality-id]').first().click();
  await page.getByRole('heading', { name: '设备时钟漂移' }).waitFor();
  await page.getByRole('button', { name: '确认并分派' }).click();
  await page.locator('#command-assignee').fill('platform.engineer');
  await page.locator('#confirm-reason').fill('确认设备时钟漂移并分派平台工程师完成校时');
  await page.getByRole('button', { name: '确认并分派', exact: true }).last().click();
  await page.getByText('事件已确认并分派给 platform.engineer').waitFor();
  await page.getByRole('button', { name: '标记已解决' }).click();
  await page.locator('#confirm-reason').fill('完成 NTP 校时并连续三个采集周期复核正常');
  await page.getByRole('button', { name: '确认已解决' }).click();
  await page.getByText('事件已解决，原始数据和处置审计已保留').waitFor();

  await page.locator('[data-view="shadowRuns"]').click();
  await page.locator(`[data-shadow-run-id="${runId}"]`).click();
  await page.getByRole('button', { name: '独立批准验收' }).waitFor();
  assert.equal(await page.getByRole('button', { name: '独立批准验收' }).isEnabled(), true);
  await page.getByRole('button', { name: '独立批准验收' }).click();
  await page.locator('#confirm-reason').fill('独立管理员复核版本、边界、数量偏差和数据质量后批准');
  await page.getByRole('button', { name: '批准验收', exact: true }).click();
  await page.getByText('影子验收已批准').waitFor();
  await page.locator('.batch-state-band').getByText('APPROVED', { exact: true }).waitFor();

  run = await fetch(`${simulatorUrl}/bpi/v1/shadow-runs/${runId}`).then((item) => item.json()).then((body) => body.data);
  assert.equal(run.state, 'APPROVED');
  assert.equal(run.revision, 14);
  assert.equal(run.metrics.reviewedBatchCount, 10);
  assert.equal(run.metrics.boundaryAgreement, 0.95);
  assert.equal(run.metrics.unresolvedCriticalIncidentCount, 0);
  assert.equal(run.decidedBy, 'simulated.bpi.admin');
  const health = await fetch(`${simulatorUrl}/health`).then((item) => item.json());
  assert.equal(health.externalWrites, false);
  await page.screenshot({ path: '/tmp/bpi-console-shadow-run-approved.png', fullPage: true });
  assert.deepEqual(errors, []);
  await context.close();
});

test('mobile layout keeps navigation usable without page-level horizontal overflow', async () => {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1 });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/batches`, { waitUntil: 'networkidle' });
  await page.getByRole('heading', { name: '批次档案' }).waitFor();
  assert.equal(await page.locator('.side-nav').evaluate((element) => getComputedStyle(element).position), 'fixed');
  const dimensions = await page.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
  assert.ok(dimensions.scroll <= dimensions.client, `page overflow: ${JSON.stringify(dimensions)}`);
  await page.locator('[data-view="overview"]').click();
  await page.getByRole('heading', { name: '实时生产态势' }).waitFor();
  await page.locator('[data-view="rules"]').click();
  await page.getByRole('heading', { name: '规则与拓扑' }).waitFor();
  const ruleDimensions = await page.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
  assert.ok(ruleDimensions.scroll <= ruleDimensions.client, `rules page overflow: ${JSON.stringify(ruleDimensions)}`);
  await page.locator('[data-view="points"]').click();
  await page.getByRole('heading', { name: '点位目录' }).waitFor();
  const pointDimensions = await page.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
  assert.ok(pointDimensions.scroll <= pointDimensions.client, `points page overflow: ${JSON.stringify(pointDimensions)}`);
  await page.locator('[data-view="dataQuality"]').click();
  await page.getByRole('heading', { name: '数据质量事件' }).waitFor();
  const dataQualityDimensions = await page.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
  assert.ok(dataQualityDimensions.scroll <= dataQualityDimensions.client, `data quality page overflow: ${JSON.stringify(dataQualityDimensions)}`);
  await page.locator('[data-view="shadowRuns"]').click();
  await page.getByRole('heading', { name: '影子运行验收' }).waitFor();
  const shadowRunDimensions = await page.evaluate(() => ({ client: document.documentElement.clientWidth, scroll: document.documentElement.scrollWidth }));
  assert.ok(shadowRunDimensions.scroll <= shadowRunDimensions.client, `shadow run page overflow: ${JSON.stringify(shadowRunDimensions)}`);
  await page.screenshot({ path: '/tmp/bpi-console-data-quality-mobile.png', fullPage: true });
  await page.screenshot({ path: '/tmp/bpi-console-mobile.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('shift lead rejects a false candidate without creating a batch', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/candidates`, { waitUntil: 'networkidle' });

  await page.getByRole('heading', { name: '候选批次' }).waitFor();
  await page.locator('[data-candidate-id]').click();
  await page.getByRole('button', { name: '拒绝候选' }).click();
  await page.getByRole('heading', { name: '拒绝候选边界' }).waitFor();
  await page.locator('#confirm-reason').fill('现场确认该边界为流量波动误判');
  await page.getByRole('button', { name: '拒绝候选', exact: true }).last().click();

  await page.getByText('候选已拒绝，未生成影子批次').waitFor();
  await page.getByText('没有待审核候选').waitFor();
  assert.equal(await page.locator('#candidate-count').textContent(), '0');
  const candidate = await fetch(`${simulatorUrl}/bpi/v1/candidates/CAND-START-S07-001`).then((response) => response.json());
  const batches = await fetch(`${simulatorUrl}/bpi/v1/batches?plantId=PLANT-01`).then((response) => response.json());
  assert.equal(candidate.data.state, 'REJECTED');
  assert.equal(candidate.data.revision, 4);
  assert.deepEqual(batches.data, []);
  await page.screenshot({ path: '/tmp/bpi-console-candidate-rejected.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('administrator imports a point catalog snapshot and sees readiness blockers', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/points`, { waitUntil: 'networkidle' });

  await page.getByRole('heading', { name: '点位目录' }).waitFor();
  await page.getByText('2', { exact: true }).last().waitFor();
  assert.equal(await page.locator('[data-point-id]').count(), 2);
  await page.locator('[data-point-id]').first().getByRole('button', { name: '查看来源序列证据' }).click();
  await assertDrawerSettled(page);
  const evidenceDrawer = page.locator('#detail-drawer');
  await evidenceDrawer.getByText('QUALIFIED', { exact: true }).first().waitFor();
  await evidenceDrawer.getByText('设备原生序列', { exact: true }).first().waitFor();
  await evidenceDrawer.getByText('1001 - 1016', { exact: true }).waitFor();
  await evidenceDrawer.getByText('source-sequence-evidence-simulator-device-s07-01', { exact: true }).waitFor();
  await evidenceDrawer.getByText('当前证据与点位绑定指纹一致、晚于目录快照且仍在有效期内，可参与运行准入。', { exact: true }).waitFor();
  await evidenceDrawer.getByRole('button', { name: '关闭' }).first().click();
  await page.getByRole('button', { name: '导入快照' }).click();
  await page.getByRole('heading', { name: '导入点位目录快照' }).waitFor();
  await page.locator('#point-source-instance').fill('jetlinks-e2e');
  await page.locator('#point-source-revision').fill('ADP_E2E_POINT_CATALOG_0001');
  await page.locator('#point-import-json').fill(JSON.stringify([{
    localityGroup: 'LOCALITY-S07-EVAP',
    productId: 'PRODUCT-SUGAR',
    deviceId: 'DEVICE-S07-INACTIVE',
    propertyId: 'tank.level',
    sourcePropertyId: 'tankLevel',
    pointName: '未就绪液位点',
    unit: null,
    dataType: null,
    deviceState: 'INACTIVE',
    registered: false,
    propertyPresent: false,
    calibrationVersion: null,
    calibrationStatus: 'MISSING',
    sourceSequenceEnabled: false,
    sourceSequenceRequired: false,
  }], null, 2));
  await page.locator('#point-import-reason').fill('验收未激活设备和缺失属性的准入阻断');
  await page.getByRole('button', { name: '导入快照', exact: true }).last().click();

  await page.getByText('点位快照已导入：0/1 就绪').waitFor();
  await page.getByText('未就绪液位点', { exact: true }).waitFor();
  await page.getByText('BLOCKED', { exact: true }).waitFor();
  await page.getByText('设备未注册、设备未激活、设备属性不可用、单位缺失、校准证据未批准或已失效、来源序列声明不完整', { exact: true }).waitFor();
  const current = await fetch(`${simulatorUrl}/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01`).then((response) => response.json());
  assert.equal(current.data.snapshot.sourceRevision, 'ADP_E2E_POINT_CATALOG_0001');
  assert.equal(current.data.snapshot.readyPointCount, 0);
  await page.screenshot({ path: '/tmp/bpi-console-point-catalog.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('point catalog incrementally loads a pinned snapshot and searches the full catalog', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const points = Array.from({ length: 205 }, (_, index) => ({
    localityGroup: 'LOCALITY-PAGE-E2E',
    productId: 'PRODUCT-PAGE-E2E',
    deviceId: 'DEVICE-PAGE-E2E',
    propertyId: `property.${String(index).padStart(4, '0')}`,
    sourcePropertyId: `sourceProperty${String(index).padStart(4, '0')}`,
    pointName: `分页验收点位 ${String(index).padStart(4, '0')}`,
    unit: 't/h',
    dataType: 'double',
    deviceState: 'ACTIVE',
    registered: true,
    propertyPresent: true,
    calibrationVersion: null,
    calibrationStatus: 'MISSING',
    sourceSequenceEnabled: true,
    sourceSequenceRequired: true,
    sourceSequenceOrigin: 'DEVICE',
    sourceSequenceBindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
  }));
  const importCatalog = (sourceRevision, importedPoints, key) => fetch(
    `${simulatorUrl}/bpi/v1/point-catalog/snapshots`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': key,
        'If-Match': '0',
      },
      body: JSON.stringify({
        source: 'JETLINKS',
        sourceInstance: 'jetlinks-pagination-e2e',
        sourceRevision,
        plantId: 'PLANT-01',
        lineId: 'LINE-S07-01',
        observedAt: new Date().toISOString(),
        points: importedPoints,
        reason: '验收高基数点位目录的快照固定分页',
      }),
    },
  );
  const imported = await importCatalog('ADP_E2E_POINT_PAGE_0205', points, 'point-page-e2e-0205');
  assert.equal(imported.status, 200);
  const importedBody = await imported.json();
  const pinnedSnapshotId = importedBody.data.snapshot.id;

  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  const pointRequests = [];
  page.on('request', (request) => {
    if (request.url().includes('/point-catalog/current')) pointRequests.push(request.url());
  });
  await page.goto(`${APP_URL}/#/points`, { waitUntil: 'networkidle' });

  await page.getByText('已加载 100 / 205 条').waitFor();
  assert.equal(await page.locator('[data-point-id]').count(), 100);
  await page.locator('#load-more-points').click();
  await page.getByText('已加载 200 / 205 条').waitFor();
  assert.equal(await page.locator('[data-point-id]').count(), 200);
  await page.locator('#load-more-points').click();
  await page.getByText('已加载 205 / 205 条').waitFor();
  assert.equal(await page.locator('[data-point-id]').count(), 205);
  assert.equal(await page.locator('#load-more-points').count(), 0);

  const searchResponse = page.waitForResponse((response) => {
    const requestUrl = new URL(response.url());
    return requestUrl.pathname.endsWith('/point-catalog/current')
      && requestUrl.searchParams.get('search') === 'property.0204';
  });
  await page.locator('#point-search').fill('property.0204');
  await searchResponse;
  await page.getByText('已加载 1 条匹配点位').waitFor();
  assert.equal(await page.locator('[data-point-id]').count(), 1);
  await page.locator('[data-point-id]').filter({ hasText: 'property.0204' }).waitFor();
  assert.ok(pointRequests.some((value) => new URL(value).searchParams.get('limit') === '100'));
  assert.ok(pointRequests.some((value) => new URL(value).searchParams.get('search') === 'property.0204'));

  const firstPageResponse = await fetch(`${simulatorUrl}/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01&search=property&limit=2`);
  assert.equal(firstPageResponse.status, 200);
  const firstPage = await firstPageResponse.json();
  assert.equal(firstPage.data.snapshot.id, pinnedSnapshotId);
  assert.equal(firstPage.data.points.length, 2);
  assert.ok(firstPage.meta.nextCursor);

  const replacement = await importCatalog('ADP_E2E_POINT_PAGE_REPLACEMENT', [{
    ...points[0],
    propertyId: 'property.replacement',
    sourcePropertyId: 'sourcePropertyReplacement',
    pointName: '替换快照点位',
  }], 'point-page-e2e-replacement');
  assert.equal(replacement.status, 200);
  const replacementBody = await replacement.json();

  const continuationResponse = await fetch(`${simulatorUrl}/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01&search=property&limit=2&cursor=${encodeURIComponent(firstPage.meta.nextCursor)}`);
  assert.equal(continuationResponse.status, 200);
  const continuation = await continuationResponse.json();
  assert.equal(continuation.data.snapshot.id, pinnedSnapshotId);
  assert.deepEqual(continuation.data.points.map((point) => point.propertyId), ['property.0002', 'property.0003']);

  const fresh = await fetch(`${simulatorUrl}/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01&search=property&limit=2`).then((response) => response.json());
  assert.equal(fresh.data.snapshot.id, replacementBody.data.snapshot.id);
  assert.equal(fresh.data.points.length, 1);
  const tampered = await fetch(`${simulatorUrl}/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01&search=property&limit=2&cursor=${encodeURIComponent(`${firstPage.meta.nextCursor}a`)}`);
  assert.equal(tampered.status, 422);
  const wrongSearch = await fetch(`${simulatorUrl}/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01&search=different&limit=2&cursor=${encodeURIComponent(firstPage.meta.nextCursor)}`);
  assert.equal(wrongSearch.status, 422);

  await page.screenshot({ path: '/tmp/bpi-console-point-catalog-pagination.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('independent calibration approval and revocation dynamically control point readiness', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/points`, { waitUntil: 'networkidle' });

  const calibrationVersion = 'ADP_E2E_CAL_GOVERNANCE_0001';
  await page.getByRole('button', { name: '导入快照' }).click();
  await page.locator('#point-source-instance').fill('jetlinks-calibration-e2e');
  await page.locator('#point-source-revision').fill('ADP_E2E_CAL_CATALOG_0001');
  await page.locator('#point-import-json').fill(JSON.stringify([{
    localityGroup: 'LOCALITY-S07-EVAP',
    productId: 'PRODUCT-SUGAR',
    deviceId: 'DEVICE-S07-CAL-E2E',
    propertyId: 'flow.calibration.e2e',
    sourcePropertyId: 'flowCalibrationE2e',
    pointName: '校准治理验收流量点',
    unit: 'm3/h',
    dataType: 'double',
    deviceState: 'ACTIVE',
    registered: true,
    propertyPresent: true,
    calibrationVersion,
    calibrationStatus: 'VERIFIED',
    sourceSequenceEnabled: true,
    sourceSequenceRequired: true,
    sourceSequenceOrigin: 'DEVICE',
    sourceSequenceBindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
  }], null, 2));
  await page.locator('#point-import-reason').fill('验证来源 VERIFIED 不能绕过 MES 校准审批');
  await page.getByRole('button', { name: '导入快照', exact: true }).last().click();

  await page.getByText('点位快照已导入：0/1 就绪').waitFor();
  const sourceEvidence = await fetch(`${simulatorUrl}/__simulation/source-sequence-evidence`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      productId: 'PRODUCT-SUGAR', deviceId: 'DEVICE-S07-CAL-E2E',
      bindingFingerprint: SOURCE_SEQUENCE_FINGERPRINT,
      status: 'QUALIFIED', sequenceOrigin: 'DEVICE',
    }),
  });
  assert.equal(sourceEvidence.status, 200);
  const pointRow = page.locator('[data-point-id]').filter({ hasText: '校准治理验收流量点' });
  await pointRow.getByText('VERIFIED', { exact: true }).waitFor();
  await pointRow.getByText('UNVERIFIED', { exact: true }).waitFor();
  await pointRow.getByText('BLOCKED', { exact: true }).waitFor();

  await pointRow.getByRole('button', { name: '提交证据' }).click();
  await page.getByRole('heading', { name: '提交点位校准证据' }).waitFor();
  assert.equal(await page.locator('#calibration-version').inputValue(), calibrationVersion);
  const localInput = (value) => {
    const date = new Date(value);
    date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
    return date.toISOString().slice(0, 19);
  };
  await page.locator('#calibration-certificate').fill('urn:adp:e2e:calibration:governance:0001');
  await page.locator('#calibration-checksum').fill('c'.repeat(64));
  await page.locator('#calibration-valid-from').fill(localInput(Date.now() - 86_400_000));
  await page.locator('#calibration-valid-until').fill(localInput(Date.now() + 365 * 86_400_000));
  await page.locator('#calibration-reason').fill('提交受控证书并申请独立复核');
  await page.getByRole('button', { name: '提交复核' }).click();
  await page.getByText(new RegExp(`校准证据 ${calibrationVersion} 已提交`)).waitFor();

  let calibrationRow = page.locator('[data-calibration-row]').filter({ hasText: calibrationVersion });
  await calibrationRow.getByText('PENDING', { exact: true }).first().waitFor();
  await calibrationRow.getByRole('button', { name: '批准', exact: true }).click();
  await page.getByRole('heading', { name: '批准校准证据' }).waitFor();
  await page.locator('#confirm-reason').fill('独立管理员复核证书、校验和和有效期');
  await page.getByRole('button', { name: '批准证据' }).click();
  await page.getByText('校准证据已批准，系统将按版本、有效期和来源序列重新计算准入').waitFor();

  await pointRow.getByText('READY', { exact: true }).waitFor();
  calibrationRow = page.locator('[data-calibration-row]').filter({ hasText: calibrationVersion });
  await calibrationRow.getByText('EFFECTIVE', { exact: true }).waitFor();
  const approved = await fetch(`${simulatorUrl}/bpi/v1/point-calibrations?plantId=PLANT-01&lineId=LINE-S07-01`).then((response) => response.json());
  const approvedEvidence = approved.data.find((item) => item.calibrationVersion === calibrationVersion);
  assert.equal(approvedEvidence.state, 'APPROVED');
  assert.equal(approvedEvidence.effective, true);

  await calibrationRow.getByRole('button', { name: '撤销', exact: true }).click();
  await page.getByRole('heading', { name: '撤销已批准证据' }).waitFor();
  await page.locator('#confirm-reason').fill('验收撤销后立即重新阻断点位');
  await page.getByRole('button', { name: '撤销证据' }).click();
  await page.getByText('校准证据已撤销，相关点位已重新阻断').waitFor();
  await pointRow.getByText('BLOCKED', { exact: true }).waitFor();
  calibrationRow = page.locator('[data-calibration-row]').filter({ hasText: calibrationVersion });
  await calibrationRow.getByText('simulated.bpi.revoker', { exact: true }).waitFor();

  const current = await fetch(`${simulatorUrl}/bpi/v1/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01`).then((response) => response.json());
  const revoked = await fetch(`${simulatorUrl}/bpi/v1/point-calibrations?plantId=PLANT-01&lineId=LINE-S07-01`).then((response) => response.json());
  const revokedEvidence = revoked.data.find((item) => item.calibrationVersion === calibrationVersion);
  assert.equal(current.data.snapshot.readyPointCount, 0);
  assert.equal(current.data.points[0].sourceCalibrationStatus, 'VERIFIED');
  assert.equal(current.data.points[0].calibrationStatus, 'UNVERIFIED');
  assert.equal(current.data.points[0].calibrationEvidenceId, null);
  assert.equal(revokedEvidence.state, 'REVOKED');
  assert.equal(revokedEvidence.effective, false);
  assert.equal(revokedEvidence.revokedBy, 'simulated.bpi.revoker');
  await page.screenshot({ path: '/tmp/bpi-console-point-calibration-governance.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('calibration workbench incrementally loads a stable cursor page without duplicates', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const now = Date.now();
  const submissions = await Promise.all(Array.from({ length: 49 }, async (_, index) => fetch(
    `${simulatorUrl}/bpi/v1/point-calibrations`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': `calibration-page-e2e-${String(index).padStart(3, '0')}`,
        'If-Match': '0',
      },
      body: JSON.stringify({
        plantId: 'PLANT-01',
        lineId: 'LINE-S07-01',
        productId: 'PRODUCT-SUGAR',
        deviceId: `DEVICE-CAL-PAGE-${String(index).padStart(3, '0')}`,
        propertyId: 'flow.instant',
        calibrationVersion: `CAL-PAGE-E2E-${String(index).padStart(3, '0')}`,
        certificateReference: `urn:ft-mes:e2e:calibration:page:${index}`,
        certificateChecksum: index.toString(16).padStart(64, '0'),
        validFrom: new Date(now - 86_400_000).toISOString(),
        validUntil: new Date(now + 365 * 86_400_000).toISOString(),
        reason: '构造高基数校准证据分页浏览器验收数据',
      }),
    },
  )));
  assert.ok(submissions.every((response) => response.status === 200));

  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  const listRequests = [];
  page.on('response', (response) => {
    const url = new URL(response.url());
    if (url.pathname.endsWith('/bpi-api/point-calibrations')) listRequests.push(url);
  });
  await page.goto(`${APP_URL}/#/points`, { waitUntil: 'networkidle' });

  await page.getByRole('heading', { name: '点位目录' }).waitFor();
  assert.equal(await page.locator('[data-calibration-row]').count(), 50);
  await page.getByText('已加载 50 条', { exact: true }).waitFor();
  await page.locator('#point-search').fill('CAL-PAGE-E2E');
  await page.getByRole('button', { name: '加载更多' }).click();
  await page.getByText('已加载 51 条', { exact: true }).waitFor();
  assert.equal(await page.locator('#point-search').inputValue(), 'CAL-PAGE-E2E');
  assert.equal(await page.locator('[data-calibration-row]').count(), 51);
  assert.equal(await page.getByRole('button', { name: '加载更多' }).count(), 0);

  const calibrationIds = await page.locator('[data-calibration-row]').evaluateAll(
    (rows) => rows.map((row) => row.getAttribute('data-calibration-row')),
  );
  assert.equal(new Set(calibrationIds).size, 51);
  assert.ok(listRequests.length >= 2);
  assert.equal(listRequests[0].searchParams.get('limit'), '50');
  assert.equal(listRequests[0].searchParams.has('cursor'), false);
  assert.ok(listRequests.at(-1).searchParams.get('cursor'));
  await page.screenshot({ path: '/tmp/bpi-console-point-calibration-pagination.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('process engineer creates validates and publishes topology before creating a rule draft', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/rules`, { waitUntil: 'networkidle' });

  await page.getByRole('button', { name: '新建拓扑' }).click();
  await page.getByRole('heading', { name: '新建拓扑版本' }).waitFor();
  await page.locator('#topology-code').fill('ADP_E2E_TOPOLOGY');
  await page.locator('#topology-version').fill('1.0.0');
  await page.locator('#topology-reason').fill('建立 S07 批次边界测点拓扑');
  await page.getByRole('button', { name: '创建草稿' }).click();
  await page.getByText('拓扑草稿 ADP_E2E_TOPOLOGY@1.0.0 已创建').waitFor();
  await page.getByRole('heading', { name: 'ADP_E2E_TOPOLOGY@1.0.0' }).waitFor();

  await page.getByRole('button', { name: '校验拓扑' }).click();
  await page.getByRole('heading', { name: '校验拓扑与测点绑定' }).waitFor();
  await page.locator('#confirm-reason').fill('核对节点路径、单位和 JetLinks 点位绑定');
  await page.getByRole('button', { name: '开始校验' }).click();
  await page.getByText('拓扑校验通过，可提交独立管理员发布').waitFor();
  await page.getByRole('button', { name: '发布拓扑' }).click();
  await page.getByRole('heading', { name: '发布不可变拓扑版本' }).waitFor();
  await page.locator('#confirm-reason').fill('独立管理员复核通过并批准上线');
  await page.getByRole('button', { name: '确认发布' }).click();
  await page.getByText('拓扑 ADP_E2E_TOPOLOGY@1.0.0 已发布').waitFor();
  await page.locator('#detail-drawer [data-close-drawer]').first().click();

  await page.getByRole('button', { name: '新建拓扑' }).click();
  await page.locator('#topology-base').selectOption({ label: 'ADP_E2E_TOPOLOGY@1.0.0' });
  await page.locator('#topology-version').fill('1.1.0');
  const nextTopology = JSON.parse(await page.locator('#topology-definition').inputValue());
  nextTopology.localityGroup = 'LINE-S07-01-V2';
  await page.locator('#topology-definition').fill(JSON.stringify(nextTopology, null, 2));
  await page.locator('#topology-reason').fill('调整版本化本地性组并核对结构差异');
  await page.getByRole('button', { name: '创建草稿' }).click();
  await page.getByText('拓扑草稿 ADP_E2E_TOPOLOGY@1.1.0 已创建').waitFor();
  await page.getByRole('heading', { name: '版本差异' }).waitFor();
  await page.getByText('对比 ADP_E2E_TOPOLOGY@1.0.0 → ADP_E2E_TOPOLOGY@1.1.0', { exact: true }).waitFor();
  await page.locator('.version-diff code').getByText('/localityGroup', { exact: true }).waitFor();
  await page.locator('#detail-drawer [data-close-drawer]').first().click();

  await page.getByRole('button', { name: '新建规则' }).click();
  await page.getByRole('heading', { name: '新建规则版本' }).waitFor();
  await page.locator('#rule-code').fill('ADP_E2E_BATCH_START');
  await page.locator('#rule-version').fill('1.0.0');
  await page.locator('#rule-reason').fill('建立进料流量启动边界规则');
  await page.getByRole('button', { name: '创建草稿' }).click();
  await page.getByText('规则草稿 ADP_E2E_BATCH_START@1.0.0 已创建').waitFor();
  await page.getByRole('heading', { name: 'ADP_E2E_BATCH_START@1.0.0' }).waitFor();

  const topologies = await fetch(`${simulatorUrl}/bpi/v1/topologies?plantId=PLANT-01`).then((response) => response.json());
  const rules = await fetch(`${simulatorUrl}/bpi/v1/rules?plantId=PLANT-01`).then((response) => response.json());
  const createdTopology = topologies.data.find((item) => item.code === 'ADP_E2E_TOPOLOGY' && item.version === '1.0.0');
  const createdRule = rules.data.find((item) => item.code === 'ADP_E2E_BATCH_START');
  assert.equal(createdTopology.state, 'PUBLISHED');
  assert.equal(createdTopology.validationStatus, 'PASSED');
  assert.equal(createdTopology.revision, 3);
  assert.equal(createdRule.state, 'DRAFT');
  assert.equal(createdRule.topologyVersion, 'ADP_E2E_TOPOLOGY@1.0.0');
  assert.equal(createdRule.revision, 1);
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-topology-rule-productization.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('process engineer submits replay proof and an independent administrator publishes the rule', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/rules`, { waitUntil: 'networkidle' });

  await page.getByRole('heading', { name: '规则与拓扑' }).waitFor();
  await page.locator('.topology-summary').getByText('TOPO-S07@3', { exact: true }).waitFor();
  await page.locator('.binding-table').getByText('flow.instant', { exact: true }).first().waitFor();
  await page.locator('[data-rule-id]').click();
  await page.getByRole('heading', { name: 'RULE-S07-START@1.2.0' }).waitFor();
  await page.getByText('尚未使用 PostgreSQL 历史测点和人工金标准执行回放。').waitFor();

  await page.getByRole('button', { name: '运行历史回放' }).click();
  await page.getByRole('heading', { name: '运行历史回放' }).waitFor();
  await page.getByRole('button', { name: '开始回放' }).click();
  await page.getByText('历史回放通过，可提交发布').waitFor();
  await page.locator('.simulation-result').getByText('PASSED', { exact: true }).waitFor();
  assert.match(await page.locator('.simulation-result').textContent(), /命中42/);
  assert.match(await page.locator('.simulation-result').textContent(), /漏检0/);
  assert.match(await page.locator('.simulation-result').textContent(), /误报0/);
  assert.match(await page.locator('.simulation-result').textContent(), /观测值18640/);

  await page.getByRole('button', { name: '提交审批' }).click();
  await page.getByRole('heading', { name: '提交规则审批' }).waitFor();
  await page.locator('#confirm-reason').fill('S07 历史批次回放通过并完成工艺工程师复核');
  await page.getByRole('button', { name: '确认提交审批' }).click();
  await page.getByText('规则 RULE-S07-START@1.2.0 已进入待审批').waitFor();
  await page.locator('.batch-state-band').getByText('PENDING_APPROVAL', { exact: true }).waitFor();
  await page.getByRole('heading', { name: '版本审批' }).waitFor();
  await page.getByText('PENDING', { exact: true }).last().waitFor();

  await page.getByRole('button', { name: '管理员批准并发布' }).click();
  await page.getByRole('heading', { name: '批准并发布边界规则' }).waitFor();
  await page.locator('#confirm-reason').fill('独立管理员复核规则、模拟证明和作用域后批准发布');
  await page.getByRole('button', { name: '确认批准并发布' }).click();
  await page.getByText('规则 RULE-S07-START@1.2.0 已批准发布，当前待分发').waitFor();
  await page.locator('.batch-state-band').getByText('PUBLISHED', { exact: true }).waitFor();
  await page.getByText('APPROVED', { exact: true }).last().waitFor();
  await page.getByRole('heading', { name: '规则发布链路' }).waitFor();
  await page.getByText('待分发', { exact: true }).last().waitFor();
  await page.getByText('发布事件已与规则版本同事务落库，等待 Kafka 分发。').waitFor();
  await page.getByRole('heading', { name: '控制面应用回执' }).waitFor();
  await page.getByText('控制面等待', { exact: true }).last().waitFor();
  await page.getByText('尚未收到 Flink 应用回执；即使 Kafka 已确认，也不能将该规则标记为在线生效。').waitFor();
  await page.getByRole('heading', { name: '流式评估器运行就绪' }).waitFor();
  await page.getByText('运行时 WAITING', { exact: true }).last().waitFor();
  assert.match(await page.locator('.batch-state-band').textContent(), /revision 10/);

  const rule = await fetch(`${simulatorUrl}/bpi/v1/rules/${RULE_ID}`).then((response) => response.json());
  assert.equal(rule.data.state, 'PUBLISHED');
  assert.equal(rule.data.publicationStatus, 'PENDING');
  assert.equal(rule.data.publicationAttemptCount, 0);
  assert.equal(rule.data.approvalStatus, 'APPROVED');
  assert.equal(rule.data.revision, 10);
  assert.ok(rule.data.latestSimulationId);
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-rule-published.png', fullPage: true });

  const failPublication = await fetch(`${simulatorUrl}/__simulation/fail-rule-publication`, { method: 'POST' });
  assert.equal(failPublication.status, 200);
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('[data-rule-id]').click();
  await page.getByText('分发失败', { exact: true }).last().waitFor();
  await page.getByText('Simulated Kafka broker outage', { exact: true }).waitFor();
  await page.getByRole('button', { name: '管理员重新入队' }).click();
  await page.getByRole('heading', { name: '重新入队失败事件' }).waitFor();
  await page.locator('#confirm-reason').fill('Kafka 集群恢复并完成连通性检查');
  await page.getByRole('button', { name: '确认重新入队' }).click();
  await page.getByText('规则 RULE-S07-START@1.2.0 的发布事件已重新入队').waitFor();
  await page.getByText('待分发', { exact: true }).last().waitFor();
  assert.match(await page.locator('#detail-drawer').textContent(), /累计尝试5/);
  assert.match(await page.locator('#detail-drawer').textContent(), /人工重试1/);
  assert.match(await page.locator('#detail-drawer').textContent(), /发布修订r12/);
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-rule-publication-retried.png', fullPage: true });

  const completePublication = await fetch(`${simulatorUrl}/__simulation/complete-rule-publication`, { method: 'POST' });
  assert.equal(completePublication.status, 200);
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('[data-rule-id]').click();
  await page.getByText('Kafka 已确认', { exact: true }).last().waitFor();
  await page.getByText('控制面等待', { exact: true }).last().waitFor();
  await page.getByText('发布事件已获 Kafka broker 确认；是否进入运行态仍以 Flink 应用回执为准。').waitFor();

  const rejectApplication = await fetch(`${simulatorUrl}/__simulation/rule-application`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      status: 'REJECTED',
      deploymentId: 'flink-simulator-a',
      errorCode: 'RULE_WINDOW_EXCEEDS_STATE_TTL',
      errorDetail: 'rule window exceeds state TTL',
    }),
  });
  assert.equal(rejectApplication.status, 200);
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('[data-rule-id]').click();
  await page.getByText('控制面 REJECTED', { exact: true }).last().waitFor();
  await page.getByText('RULE_WINDOW_EXCEEDS_STATE_TTL', { exact: true }).waitFor();
  await page.getByText('rule window exceeds state TTL', { exact: true }).waitFor();
  await page.getByRole('heading', { name: '控制面应用回执' }).scrollIntoViewIfNeeded();
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-rule-application-rejected.png', fullPage: true });

  const applyApplication = await fetch(`${simulatorUrl}/__simulation/rule-application`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: 'APPLIED', deploymentId: 'flink-simulator-b' }),
  });
  assert.equal(applyApplication.status, 200);
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('[data-rule-id]').click();
  await page.getByText('控制面 APPLIED', { exact: true }).last().waitFor();
  await page.getByText('flink-simulator-b', { exact: true }).waitFor();
  await page.getByText('运行时 WAITING', { exact: true }).last().waitFor();
  await page.getByText('控制面已接受该规则版本，APPLIED 回执经 checkpoint 提交后完成作用域与 checksum 校验并写入 PostgreSQL；这不代表流式评估器已 READY。').waitFor();
  assert.equal(await page.getByText('RULE_WINDOW_EXCEEDS_STATE_TTL', { exact: true }).count(), 0);
  const appliedRule = await fetch(`${simulatorUrl}/bpi/v1/rules/${RULE_ID}`).then((response) => response.json());
  assert.equal(appliedRule.data.publicationStatus, 'PUBLISHED');
  assert.equal(appliedRule.data.applicationStatus, 'APPLIED');
  assert.equal(appliedRule.data.applicationDeploymentId, 'flink-simulator-b');
  assert.equal(appliedRule.data.applicationErrorCode, null);
  assert.equal(appliedRule.data.publicationRevision, 15);
  const degradedReadiness = await fetch(`${simulatorUrl}/__simulation/rule-runtime-readiness`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      eventId: 'e2e-readiness-degraded-0001',
      status: 'DEGRADED',
      deploymentId: 'flink-simulator-b',
      observedAt: '2026-07-12T08:00:04.000Z',
      reasonCode: 'POINT_SOURCE_SEQUENCE_DISABLED',
      detail: 'device source sequence evidence is missing',
      pointCatalogEventId: 'catalog-event-41',
      pointCatalogSourceRevision: 'sha256:catalog-41',
    }),
  });
  assert.equal(degradedReadiness.status, 200);
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('[data-rule-id]').click();
  await page.getByText('控制面 APPLIED', { exact: true }).last().waitFor();
  await page.getByText('运行时 DEGRADED', { exact: true }).last().waitFor();
  await page.getByText('POINT_SOURCE_SEQUENCE_DISABLED', { exact: true }).waitFor();
  await page.getByText('device source sequence evidence is missing', { exact: true }).waitFor();
  await page.getByText('catalog-event-41', { exact: true }).waitFor();

  const readyReadiness = await fetch(`${simulatorUrl}/__simulation/rule-runtime-readiness`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      eventId: 'e2e-readiness-ready-0002',
      status: 'READY',
      deploymentId: 'flink-simulator-b',
      observedAt: '2026-07-12T08:00:20.000Z',
      receivedAt: '2026-07-12T08:05:00.000Z',
      pointCatalogEventId: 'catalog-event-42',
      pointCatalogSourceRevision: 'sha256:catalog-42',
    }),
  });
  assert.equal(readyReadiness.status, 200);
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('[data-rule-id]').click();
  await page.getByText('控制面 APPLIED', { exact: true }).last().waitFor();
  await page.getByText('运行时 READY', { exact: true }).last().waitFor();
  await page.getByText('流式评估器已激活该精确规则版本，并已记录运行部署和点位目录证据。').waitFor();
  await page.getByText('catalog-event-42', { exact: true }).waitFor();
  const readyRule = await fetch(`${simulatorUrl}/bpi/v1/rules/${RULE_ID}`).then((response) => response.json());
  assert.equal(readyRule.data.applicationStatus, 'APPLIED');
  assert.equal(readyRule.data.runtimeReadinessStatus, 'READY');
  assert.equal(readyRule.data.runtimeReadinessReceivedAt, '2026-07-12T08:05:00.000Z');
  await page.getByRole('heading', { name: '流式评估器运行就绪' }).scrollIntoViewIfNeeded();
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-rule-application-applied.png', fullPage: true });

  await page.getByRole('button', { name: '管理员退役' }).click();
  await page.getByRole('heading', { name: '退役边界规则' }).waitFor();
  await page.locator('#confirm-reason').fill('替代版本发布前按变更窗口停用当前在线规则');
  await page.getByRole('button', { name: '确认退役并停用' }).click();
  await page.getByText('规则 RULE-S07-START@1.2.0 已退役，等待 Kafka 与 Flink 确认 INACTIVE').waitFor();
  await page.locator('.batch-state-band').getByText('RETIRED', { exact: true }).waitFor();
  await page.getByRole('heading', { name: '生命周期命令' }).waitFor();
  await page.locator('.lifecycle-summary').getByText('RETIRE', { exact: true }).waitFor();
  await page.locator('.lifecycle-summary').getByText('#2', { exact: true }).waitFor();
  await page.locator('.lifecycle-summary').getByText('INACTIVE', { exact: true }).waitFor();
  await page.getByText('退役状态与 active=false 停用事件已同事务落库，等待 Kafka 分发。').waitFor();
  const retiredRule = await fetch(`${simulatorUrl}/bpi/v1/rules/${RULE_ID}`).then((response) => response.json());
  assert.equal(retiredRule.data.state, 'RETIRED');
  assert.equal(retiredRule.data.lifecycleAction, 'RETIRE');
  assert.equal(retiredRule.data.lifecycleSequence, 2);
  assert.equal(retiredRule.data.lifecycleActive, false);
  assert.equal(retiredRule.data.publicationStatus, 'PENDING');
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-rule-retirement-pending.png', fullPage: true });

  const completeRetirement = await fetch(`${simulatorUrl}/__simulation/complete-rule-publication`, { method: 'POST' });
  assert.equal(completeRetirement.status, 200);
  const applyRetirement = await fetch(`${simulatorUrl}/__simulation/rule-application`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status: 'APPLIED', deploymentId: 'flink-simulator-retirement' }),
  });
  assert.equal(applyRetirement.status, 200);
  const inactiveReadiness = await fetch(`${simulatorUrl}/__simulation/rule-runtime-readiness`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      eventId: 'e2e-readiness-inactive-0003',
      status: 'INACTIVE',
      deploymentId: 'flink-simulator-retirement',
      observedAt: '2026-07-12T08:01:00.000Z',
      receivedAt: '2026-07-12T08:01:01.000Z',
      reasonCode: 'RULE_INACTIVE',
      detail: 'retired rule version removed from the evaluator',
      pointCatalogEventId: 'catalog-event-43',
      pointCatalogSourceRevision: 'sha256:catalog-43',
    }),
  });
  assert.equal(inactiveReadiness.status, 200);
  await page.reload({ waitUntil: 'networkidle' });
  await page.locator('[data-rule-id]').click();
  await page.getByText('Kafka 已确认', { exact: true }).last().waitFor();
  await page.getByText('控制面 APPLIED', { exact: true }).last().waitFor();
  await page.getByText('运行时 INACTIVE', { exact: true }).last().waitFor();
  await page.getByText('该精确规则版本已从评估器停用，不会参与新的边界计算。').waitFor();
  await page.getByRole('button', { name: '创建回滚草稿' }).click();
  await page.getByRole('heading', { name: '创建回滚规则草稿' }).waitFor();
  assert.equal(await page.locator('#rule-base').inputValue(), RULE_ID);
  assert.equal(await page.locator('#rule-code').inputValue(), 'RULE-S07-START');
  await page.locator('#rule-version').fill('1.2.1');
  await page.locator('#rule-reason').fill('从已确认 INACTIVE 的稳定版本创建受控回滚草稿');
  await page.getByRole('button', { name: '创建草稿' }).click();
  await page.getByText('规则草稿 RULE-S07-START@1.2.1 已创建').waitFor();
  await page.getByRole('heading', { name: 'RULE-S07-START@1.2.1' }).waitFor();
  await page.locator('.batch-state-band').getByText('DRAFT', { exact: true }).waitFor();
  const rollbackRules = await fetch(`${simulatorUrl}/bpi/v1/rules?plantId=PLANT-01`).then((response) => response.json());
  const rollbackRule = rollbackRules.data.find((item) => item.version === '1.2.1');
  assert.ok(rollbackRule);
  assert.equal(rollbackRule.state, 'DRAFT');
  assert.notEqual(rollbackRule.id, RULE_ID);
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-rule-rollback-draft.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

test('process engineer sees business wording when point readiness blocks publication', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/rules`, { waitUntil: 'networkidle' });

  await page.locator('[data-rule-id]').click();
  await page.getByRole('button', { name: '运行历史回放' }).click();
  await page.getByRole('button', { name: '开始回放' }).click();
  await page.getByText('历史回放通过，可提交发布').waitFor();
  await page.getByRole('button', { name: '提交审批' }).click();
  await page.locator('#confirm-reason').fill('提交点位准入拦截验证');
  await page.getByRole('button', { name: '确认提交审批' }).click();
  await page.getByText('已进入待审批').waitFor();
  await page.route('**/bpi-api/rules/*/publish', async (route) => {
    await route.fulfill({
      status: 422,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        title: 'Validation failed',
        status: 422,
        detail: 'Rule publication requires current READY point catalog bindings: POINT_CALIBRATION_NOT_VERIFIED, POINT_CATALOG_BINDING_NOT_FOUND, POINT_DEVICE_NOT_ACTIVE, POINT_DEVICE_NOT_REGISTERED, POINT_PROPERTY_NOT_AVAILABLE, POINT_SOURCE_SEQUENCE_DISABLED, POINT_UNIT_MISSING.',
        traceId: 'ADP_E2E_POINT_READINESS_BLOCKED',
      }),
    });
  });

  await page.getByRole('button', { name: '管理员批准并发布' }).click();
  await page.locator('#confirm-reason').fill('验证点位准入失败时的业务提示');
  await page.getByRole('button', { name: '确认批准并发布' }).click();

  const toast = page.locator('#toast');
  await toast.getByText(/规则未发布：当前点位未通过运行准入/).waitFor();
  const message = await toast.textContent();
  assert.match(message, /设备未注册/);
  assert.match(message, /点位目录中找不到规则绑定/);
  assert.doesNotMatch(message, /Rule publication|POINT_[A-Z_]+/);
  assert.equal(await page.locator('#confirm-dialog').getAttribute('open'), '');
  assert.deepEqual(errors.filter((item) => !item.includes('422')), []);
  await page.screenshot({ path: '/tmp/bpi-console-rule-publication-blocked.png', fullPage: true });
  await page.close();
});

test('shift lead closes a data quality incident with preserved evidence and audit history', async () => {
  const reset = await fetch(`${simulatorUrl}/__simulation/reset`, { method: 'POST' });
  assert.equal(reset.status, 200);
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  const errors = observe(page);
  await page.goto(`${APP_URL}/#/dataQuality`, { waitUntil: 'networkidle' });

  await page.getByRole('heading', { name: '数据质量事件' }).waitFor();
  assert.equal(await page.locator('.data-quality-table tbody tr').count(), 3, 'OPEN is the default work queue');
  assert.match(await page.locator('.data-quality-state-summary').textContent(), /待确认3/);
  assert.match(await page.locator('.data-quality-category-strip').textContent(), /设备时钟漂移1/);

  const clockRow = page.locator('[data-data-quality-id]').filter({ hasText: 'CLOCK_DRIFT' });
  const incidentId = await clockRow.getAttribute('data-data-quality-id');
  assert.ok(incidentId);
  await clockRow.click();
  await page.getByRole('heading', { name: '设备时钟漂移' }).waitFor();
  await page.getByText('4 条，最多显示 100 条').waitFor();
  await page.getByRole('button', { name: '确认并分派' }).click();
  await page.getByRole('heading', { name: '确认并分派事件' }).waitFor();
  assert.equal(await page.locator('#command-assignee-field').isVisible(), true);
  await page.locator('#command-assignee').fill('shift.lead');
  await page.locator('#confirm-reason').fill('确认网关时钟漂移影响边界计算，交由班长跟进');
  await page.getByRole('button', { name: '确认并分派', exact: true }).last().click();

  await page.getByText('事件已确认并分派给 shift.lead').waitFor();
  await page.locator('.batch-state-band').getByText('ACKNOWLEDGED', { exact: true }).waitFor();
  await page.getByRole('button', { name: '重新分派' }).click();
  await page.locator('#command-assignee').fill('platform.engineer');
  await page.locator('#confirm-reason').fill('转交平台工程师完成 NTP 校时和网关复核');
  await page.getByRole('button', { name: '确认重新分派' }).click();
  await page.getByText('事件已确认并分派给 platform.engineer').waitFor();
  await page.getByText('REASSIGNED', { exact: true }).waitFor();

  await page.getByRole('button', { name: '标记已解决' }).click();
  await page.getByRole('heading', { name: '解决数据质量事件' }).waitFor();
  assert.equal(await page.locator('#command-assignee-field').isVisible(), false);
  await page.locator('#confirm-reason').fill('NTP 校时完成，连续三个采集周期时间偏差低于阈值');
  await page.getByRole('button', { name: '确认已解决' }).click();
  await page.getByText('事件已解决，原始数据和处置审计已保留').waitFor();
  await page.locator('.batch-state-band').getByText('RESOLVED', { exact: true }).waitFor();
  await page.getByText('RESOLVED', { exact: true }).last().waitFor();

  const detail = await fetch(`${simulatorUrl}/bpi/v1/data-quality/incidents/${incidentId}`).then((response) => response.json());
  assert.equal(detail.data.incident.state, 'RESOLVED');
  assert.equal(detail.data.incident.revision, 4);
  assert.equal(detail.data.incident.assignee, 'platform.engineer');
  assert.equal(detail.data.events.length, 4);
  assert.deepEqual(detail.data.lifecycle.map((item) => item.action), [
    'CREATED', 'ACKNOWLEDGED', 'REASSIGNED', 'RESOLVED',
  ]);
  await assertDrawerSettled(page);
  await page.screenshot({ path: '/tmp/bpi-console-data-quality-workbench.png', fullPage: true });
  assert.deepEqual(errors, []);
  await page.close();
});

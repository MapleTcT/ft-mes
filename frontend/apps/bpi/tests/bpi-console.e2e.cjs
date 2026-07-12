const assert = require('node:assert/strict');
const { after, before, test } = require('node:test');
const { spawn } = require('node:child_process');
const path = require('node:path');
const { chromium } = require('playwright');
const { createBpiSimulator, listen } = require('../../../../simulation/bpi/server');

const APP_ROOT = path.resolve(__dirname, '..');
const APP_URL = 'http://127.0.0.1:4173';
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

before(async () => {
  ({ server: simulator } = createBpiSimulator());
  const address = await listen(simulator);
  simulatorUrl = `http://127.0.0.1:${address.port}`;
  vite = spawn('npm', ['run', 'dev', '--', '--host', '127.0.0.1', '--port', '4173'], {
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
  if (vite && !vite.killed) vite.kill('SIGTERM');
  if (simulator) await new Promise((resolve, reject) => simulator.close((error) => error ? reject(error) : resolve()));
});

test('desktop operator confirms a candidate and opens the shadow batch', async () => {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
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
  assert.equal(await page.locator('#candidate-count').textContent(), '0');
  await page.screenshot({ path: '/tmp/bpi-console-desktop.png', fullPage: true });
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

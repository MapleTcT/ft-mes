const assert = require('node:assert/strict');
const path = require('node:path');
const test = require('node:test');
const {
  buildTelemetryEnvelopes,
  evaluateCase,
  loadScenario,
} = require('./fructose-line-scenario');

const scenario = loadScenario(path.join(
  __dirname,
  'scenarios/fructose-jet-saccharification-v1.json',
));

test('fructose pilot samples twelve signals fifteen times per minute without writeback', () => {
  assert.equal(scenario.signals.length, 12);
  assert.equal(60 / scenario.sampling.intervalSeconds, 15);
  assert.equal(scenario.safety.shadowOnly, true);
  assert.deepEqual(
    scenario.safety.writeback,
    { wom: false, qcs: false, wms: false, plcDcs: false },
  );
});

test('normal jet to saccharification route produces a closed two-process chain', () => {
  const result = evaluateCase(scenario, 'NORMAL_HANDOVER');
  assert.deepEqual(
    result.boundaries.map((item) => `${item.processCode}:${item.type}:${item.reason}`),
    [
      'JET:START:SIGNAL_HOLD',
      'JET:END:ROUTE_SWITCH',
      'SACCHARIFICATION:START:SIGNAL_HOLD',
      'SACCHARIFICATION:END:SIGNAL_HOLD',
    ],
  );
  assert.equal(result.qualityExcursions.length, 0);
});

test('eight-second flow dropout does not split the jet process', () => {
  const result = evaluateCase(scenario, 'BRIEF_FLOW_DROPOUT');
  assert.deepEqual(
    result.boundaries.map((item) => `${item.type}:${item.reason}`),
    ['START:SIGNAL_HOLD', 'END:SIGNAL_HOLD'],
  );
  const dropoutEnd = result.frames.find(
    (item) => item.phase === 'BRIEF_DROPOUT' && item.phaseSample === 2,
  );
  assert.ok(result.boundaries.every(
    (item) => item.type !== 'END' || item.globalSample > dropoutEnd.globalSample,
  ));
});

test('Baume excursion creates quality evidence without creating a batch boundary', () => {
  const result = evaluateCase(scenario, 'BAUME_EXCURSION');
  assert.equal(result.qualityExcursions.length, 1);
  assert.equal(result.qualityExcursions[0].propertyId, 'jet.feed.baume');
  assert.deepEqual(
    result.boundaries.map((item) => item.type),
    ['START', 'END'],
  );
});

test('production order and route switch are immediate hard boundaries', () => {
  const result = evaluateCase(scenario, 'ORDER_ROUTE_SWITCH');
  assert.equal(result.boundaries[1].processCode, 'JET');
  assert.equal(result.boundaries[1].type, 'END');
  assert.equal(result.boundaries[1].reason, 'ORDER_SWITCH');
  assert.equal(result.boundaries[2].processCode, 'SACCHARIFICATION');
  assert.equal(result.boundaries[2].type, 'START');
});

test('telemetry envelopes preserve scope, sequence, context and all signal values', () => {
  const envelopes = buildTelemetryEnvelopes(
    scenario,
    'NORMAL_HANDOVER',
    'ADP_E2E_FRUCTOSE_TEST',
  );
  assert.ok(envelopes.length > 20);
  assert.ok(envelopes.every((envelope) => envelope.sequenceOrigin === 'GATEWAY'));
  assert.equal(envelopes[0].points.length, 12);
  assert.equal(envelopes[0].sequence, 1);
  assert.equal(envelopes.at(-1).sequence, envelopes.length);
  assert.equal(envelopes[0].headers.orderId, 'MO-FRU-NORMAL-001');
  assert.ok(envelopes[0].points.some(
    (item) => item.propertyId === 'jet.feed.baume' && item.unit === 'Be',
  ));
});

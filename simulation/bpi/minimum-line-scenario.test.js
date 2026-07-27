const assert = require('node:assert/strict');
const path = require('node:path');
const test = require('node:test');
const {
  buildPilotMapping,
  expandSamplingFrames,
  loadScenario,
  predictBoundary,
} = require('./minimum-line-scenario');

const scenarioPath = path.join(
  __dirname,
  'scenarios/minimum-transfer-cell-v1.json',
);

test('minimum transfer cell has five coherent JetLinks bindings', () => {
  const scenario = loadScenario(scenarioPath);
  const mapping = buildPilotMapping(scenario);
  const points = mapping.devices[0].points;

  assert.equal(points.length, 5);
  assert.deepEqual(
    points.filter((point) => point.dataType === 'boolean')
      .map((point) => point.targetPropertyId),
    ['pump.running', 'valve.path.ready'],
  );
  assert.equal(mapping.devices[0].requireSourceSequence, true);
  assert.equal(scenario.safety.shadowOnly, true);
  assert.deepEqual(
    scenario.safety.writeback,
    { wom: false, qcs: false, wms: false, plcDcs: false },
  );
});

test('continuous samples cross START and END only after their hold windows', () => {
  const scenario = loadScenario(scenarioPath);
  const frames = expandSamplingFrames(scenario);
  const startRule = scenario.rules.find((rule) => rule.ast.boundaryType === 'START');
  const endRule = scenario.rules.find((rule) => rule.ast.boundaryType === 'END');

  assert.equal(frames.length, 25);
  assert.deepEqual(
    predictBoundary(scenario, startRule),
    {
      phase: 'START_CONFIRM',
      phaseSample: 6,
      globalSample: 9,
      confidence: 1,
    },
  );
  assert.deepEqual(
    predictBoundary(scenario, endRule),
    {
      phase: 'STOP_CONFIRM',
      phaseSample: 5,
      globalSample: 23,
      confidence: 1,
    },
  );
});

test('one MQTT envelope preserves all values at a device sequence', () => {
  const scenario = loadScenario(scenarioPath);
  const frames = expandSamplingFrames(scenario);
  const running = frames.find(
    (frame) => frame.phase === 'RUNNING' && frame.phaseSample === 3,
  );

  assert.deepEqual(running.values, {
    'flow.instant': 18.2,
    'flow.totalizer': 103.1,
    'pump.running': true,
    'valve.path.ready': true,
    'tank.level': 40.8,
  });
});

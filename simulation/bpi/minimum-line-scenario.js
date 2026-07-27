const fs = require('node:fs');
const path = require('node:path');

const BOOLEAN_OPERATORS = new Set(['EQUALS_TRUE', 'EQUALS_FALSE']);
const NUMERIC_OPERATORS = new Set(['GREATER_THAN', 'LESS_THAN', 'RISING']);
const CLASSIFICATIONS = new Set(['REQUIRED', 'QUORUM', 'OPTIONAL']);

function requireObject(value, location) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`${location} must be an object`);
  }
  return value;
}

function requireText(value, location) {
  if (typeof value !== 'string' || !value.trim()) {
    throw new Error(`${location} must be a non-empty string`);
  }
  return value.trim();
}

function requireFinite(value, location) {
  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`${location} must be a finite number`);
  }
  return value;
}

function expandSamplingFrames(scenario) {
  const frames = [];
  for (const phase of scenario.sampling.phases) {
    for (let sampleIndex = 0; sampleIndex < phase.samples; sampleIndex += 1) {
      const values = {};
      for (const signal of scenario.signals) {
        const propertyId = signal.targetPropertyId;
        const base = phase.values[propertyId];
        const step = phase.stepPerSample?.[propertyId] ?? 0;
        values[propertyId] = typeof base === 'boolean'
          ? base
          : Number((base + step * sampleIndex).toFixed(6));
      }
      frames.push({
        globalSample: frames.length + 1,
        phase: phase.name,
        phaseSample: sampleIndex + 1,
        values,
      });
    }
  }
  return frames;
}

function predicate(condition, value, previousValue) {
  switch (condition.operator) {
    case 'GREATER_THAN':
      return value > condition.threshold;
    case 'LESS_THAN':
      return value < condition.threshold;
    case 'EQUALS_TRUE':
      return value === true;
    case 'EQUALS_FALSE':
      return value === false;
    case 'RISING':
      return typeof previousValue === 'number'
        && value - previousValue >= condition.threshold;
    default:
      throw new Error(`unsupported operator ${condition.operator}`);
  }
}

function predictBoundary(scenario, rule) {
  const interval = scenario.sampling.intervalSeconds;
  const states = new Map();
  const previousValues = new Map();
  for (const frame of expandSamplingFrames(scenario)) {
    let earnedWeight = 0;
    let totalWeight = 0;
    let requiredSatisfied = true;
    let quorumSatisfied = 0;
    for (const condition of rule.ast.conditions) {
      const value = frame.values[condition.signal];
      const matches = predicate(
        condition,
        value,
        previousValues.get(condition.signal),
      );
      const prior = states.get(condition.signal);
      const trueSince = matches
        ? prior?.matches ? prior.trueSince : frame.globalSample
        : null;
      const heldSeconds = trueSince === null
        ? 0
        : (frame.globalSample - trueSince) * interval;
      const satisfied = matches && heldSeconds >= condition.holdSeconds;
      states.set(condition.signal, { matches, trueSince, satisfied });
      previousValues.set(condition.signal, value);
      totalWeight += condition.weight;
      if (satisfied) {
        earnedWeight += condition.weight;
        if (condition.classification === 'QUORUM') quorumSatisfied += 1;
      } else if (condition.classification === 'REQUIRED') {
        requiredSatisfied = false;
      }
    }
    const confidence = totalWeight === 0 ? 0 : earnedWeight / totalWeight;
    if (
      requiredSatisfied
      && quorumSatisfied >= rule.ast.quorumMinimum
      && confidence >= rule.ast.minimumConfidence
    ) {
      return {
        phase: frame.phase,
        phaseSample: frame.phaseSample,
        globalSample: frame.globalSample,
        confidence: Number(confidence.toFixed(4)),
      };
    }
  }
  return null;
}

function buildPilotMapping(scenario) {
  return {
    schemaVersion: 1,
    scope: structuredClone(scenario.scope),
    devices: [{
      ...structuredClone(scenario.device),
      points: scenario.signals.map((signal) => structuredClone(signal)),
    }],
  };
}

function validateScenario(scenario) {
  requireObject(scenario, 'scenario');
  if (scenario.schemaVersion !== 1) throw new Error('schemaVersion must be 1');
  requireText(scenario.scenarioCode, 'scenarioCode');
  const safety = requireObject(scenario.safety, 'safety');
  if (safety.shadowOnly !== true || safety.requiresActiveProductionContext !== true) {
    throw new Error('scenario must be shadow-only and require active production context');
  }
  const writeback = requireObject(safety.writeback, 'safety.writeback');
  for (const target of ['wom', 'qcs', 'wms', 'plcDcs']) {
    if (writeback[target] !== false) {
      throw new Error(`safety.writeback.${target} must remain false`);
    }
  }
  for (const field of ['tenantId', 'plantId', 'lineId', 'localityGroup']) {
    requireText(scenario.scope?.[field], `scope.${field}`);
  }
  for (const field of [
    'gatewayId',
    'productId',
    'deviceId',
    'sequenceHeader',
    'sourceEpochHeader',
    'sourceSequenceOrigin',
  ]) {
    requireText(scenario.device?.[field], `device.${field}`);
  }
  if (scenario.device.requireSourceSequence !== true) {
    throw new Error('device.requireSourceSequence must be true');
  }
  if (!Array.isArray(scenario.signals) || scenario.signals.length < 2) {
    throw new Error('signals must contain at least two points');
  }
  const targetIds = new Set();
  const sourceIds = new Set();
  for (const [index, signal] of scenario.signals.entries()) {
    const targetId = requireText(signal.targetPropertyId, `signals[${index}].targetPropertyId`);
    const sourceId = requireText(signal.sourcePropertyId, `signals[${index}].sourcePropertyId`);
    if (targetIds.has(targetId) || sourceIds.has(sourceId)) {
      throw new Error('signal source and target property ids must be unique');
    }
    targetIds.add(targetId);
    sourceIds.add(sourceId);
    if (!['double', 'boolean'].includes(signal.dataType)) {
      throw new Error(`signals[${index}].dataType is unsupported`);
    }
    requireText(signal.unit, `signals[${index}].unit`);
    requireText(signal.calibrationVersion, `signals[${index}].calibrationVersion`);
  }

  const topology = requireObject(scenario.topology, 'topology');
  requireText(topology.code, 'topology.code');
  requireText(topology.version, 'topology.version');
  const definition = requireObject(topology.definition, 'topology.definition');
  const bindingSignals = new Set(definition.bindings?.map((item) => item.signal));
  if (
    bindingSignals.size !== targetIds.size
    || [...targetIds].some((signal) => !bindingSignals.has(signal))
  ) {
    throw new Error('topology bindings must cover every scenario signal exactly once');
  }

  if (!Array.isArray(scenario.rules) || scenario.rules.length !== 2) {
    throw new Error('rules must contain one START and one END rule');
  }
  const rulesByType = new Map();
  for (const [ruleIndex, rule] of scenario.rules.entries()) {
    requireText(rule.code, `rules[${ruleIndex}].code`);
    requireText(rule.version, `rules[${ruleIndex}].version`);
    const ast = requireObject(rule.ast, `rules[${ruleIndex}].ast`);
    if (!['START', 'END'].includes(ast.boundaryType) || rulesByType.has(ast.boundaryType)) {
      throw new Error('rules must contain unique START and END boundary types');
    }
    rulesByType.set(ast.boundaryType, rule);
    if (!Number.isInteger(ast.quorumMinimum) || ast.quorumMinimum < 1) {
      throw new Error(`rules[${ruleIndex}].ast.quorumMinimum must be positive`);
    }
    requireFinite(ast.minimumConfidence, `rules[${ruleIndex}].ast.minimumConfidence`);
    let quorumCount = 0;
    for (const [conditionIndex, condition] of ast.conditions.entries()) {
      if (!targetIds.has(condition.signal)) {
        throw new Error(`rules[${ruleIndex}] condition uses an unbound signal`);
      }
      if (!BOOLEAN_OPERATORS.has(condition.operator) && !NUMERIC_OPERATORS.has(condition.operator)) {
        throw new Error(`rules[${ruleIndex}] condition operator is unsupported`);
      }
      if (NUMERIC_OPERATORS.has(condition.operator)) {
        requireFinite(condition.threshold, `rules[${ruleIndex}].conditions[${conditionIndex}].threshold`);
      }
      if (!CLASSIFICATIONS.has(condition.classification)) {
        throw new Error(`rules[${ruleIndex}] condition classification is unsupported`);
      }
      if (condition.classification === 'QUORUM') quorumCount += 1;
      if (!Number.isInteger(condition.holdSeconds) || condition.holdSeconds < 0) {
        throw new Error(`rules[${ruleIndex}] condition holdSeconds is invalid`);
      }
      if (!Number.isInteger(condition.weight) || condition.weight < 0) {
        throw new Error(`rules[${ruleIndex}] condition weight is invalid`);
      }
    }
    if (quorumCount < ast.quorumMinimum) {
      throw new Error(`rules[${ruleIndex}] quorumMinimum exceeds quorum conditions`);
    }
  }

  const sampling = requireObject(scenario.sampling, 'sampling');
  const interval = requireFinite(sampling.intervalSeconds, 'sampling.intervalSeconds');
  if (interval <= 0) throw new Error('sampling.intervalSeconds must be positive');
  if (!Array.isArray(sampling.phases) || sampling.phases.length < 3) {
    throw new Error('sampling.phases must include idle, running, and stopping evidence');
  }
  const phaseNames = new Set();
  for (const [phaseIndex, phase] of sampling.phases.entries()) {
    const name = requireText(phase.name, `sampling.phases[${phaseIndex}].name`);
    if (phaseNames.has(name)) throw new Error(`duplicate phase ${name}`);
    phaseNames.add(name);
    if (!Number.isInteger(phase.samples) || phase.samples < 1) {
      throw new Error(`sampling.phases[${phaseIndex}].samples must be positive`);
    }
    if (
      !phase.values
      || Object.keys(phase.values).length !== targetIds.size
      || [...targetIds].some((signal) => !(signal in phase.values))
    ) {
      throw new Error(`sampling.phases[${phaseIndex}] must define every signal`);
    }
  }

  const start = predictBoundary(scenario, rulesByType.get('START'));
  const end = predictBoundary(scenario, rulesByType.get('END'));
  for (const [boundary, prediction] of [['start', start], ['end', end]]) {
    const expected = scenario.expectations?.[boundary];
    if (
      !prediction
      || prediction.phase !== expected?.phase
      || prediction.phaseSample !== expected?.phaseSample
    ) {
      throw new Error(`${boundary} boundary prediction does not match expectations`);
    }
  }
  return {
    scenarioCode: scenario.scenarioCode,
    signalCount: scenario.signals.length,
    frameCount: expandSamplingFrames(scenario).length,
    start,
    end,
  };
}

function loadScenario(filePath) {
  const scenario = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  validateScenario(scenario);
  return scenario;
}

function runCli(argv) {
  const [command, input] = argv;
  if (!command || !input) {
    throw new Error('usage: minimum-line-scenario.js <validate|mapping|frames> <scenario.json>');
  }
  const scenario = loadScenario(path.resolve(input));
  if (command === 'validate') return validateScenario(scenario);
  if (command === 'mapping') return buildPilotMapping(scenario);
  if (command === 'frames') return expandSamplingFrames(scenario);
  throw new Error(`unsupported command ${command}`);
}

if (require.main === module) {
  try {
    process.stdout.write(`${JSON.stringify(runCli(process.argv.slice(2)), null, 2)}\n`);
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exitCode = 1;
  }
}

module.exports = {
  buildPilotMapping,
  expandSamplingFrames,
  loadScenario,
  predictBoundary,
  validateScenario,
};

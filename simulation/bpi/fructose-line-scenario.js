const fs = require('node:fs');
const path = require('node:path');

function requireText(value, location) {
  if (typeof value !== 'string' || !value.trim()) {
    throw new Error(`${location} must be a non-empty string`);
  }
  return value.trim();
}

function loadScenario(filePath) {
  const scenario = JSON.parse(fs.readFileSync(filePath, 'utf8'));
  validateScenario(scenario);
  return scenario;
}

function validateScenario(scenario) {
  if (!scenario || scenario.schemaVersion !== 1) {
    throw new Error('schemaVersion must be 1');
  }
  requireText(scenario.scenarioCode, 'scenarioCode');
  if (
    scenario.safety?.shadowOnly !== true
    || scenario.safety?.requiresActiveProductionContext !== true
  ) {
    throw new Error('fructose scenario must remain shadow-only with active production context');
  }
  for (const target of ['wom', 'qcs', 'wms', 'plcDcs']) {
    if (scenario.safety?.writeback?.[target] !== false) {
      throw new Error(`safety.writeback.${target} must remain false`);
    }
  }
  if (scenario.sampling?.intervalSeconds !== 4) {
    throw new Error('sampling.intervalSeconds must be 4 (15 samples per minute)');
  }
  if (!Array.isArray(scenario.signals) || scenario.signals.length !== 12) {
    throw new Error('fructose scenario must define exactly 12 telemetry signals');
  }
  validateProductionModel(scenario.productionModel);
  const propertyIds = new Set();
  for (const [index, signal] of scenario.signals.entries()) {
    const propertyId = requireText(signal.propertyId, `signals[${index}].propertyId`);
    if (propertyIds.has(propertyId)) throw new Error(`duplicate propertyId ${propertyId}`);
    propertyIds.add(propertyId);
    if (!['double', 'boolean'].includes(signal.dataType)) {
      throw new Error(`signals[${index}].dataType is unsupported`);
    }
    requireText(signal.unit, `signals[${index}].unit`);
  }
  if (!Array.isArray(scenario.processes) || scenario.processes.length !== 2) {
    throw new Error('fructose scenario must define jet and saccharification processes');
  }
  for (const process of scenario.processes) {
    requireText(process.code, 'process.code');
    requireText(process.routeCode, 'process.routeCode');
    for (const field of ['flowProperty', 'pumpProperty', 'valveProperty', 'baumeProperty']) {
      if (!propertyIds.has(process[field])) {
        throw new Error(`${process.code}.${field} must reference a telemetry signal`);
      }
    }
  }
  if (!Array.isArray(scenario.cases) || scenario.cases.length < 4) {
    throw new Error('fructose scenario must include at least four boundary cases');
  }
  for (const item of scenario.cases) {
    requireText(item.code, 'case.code');
    if (!Array.isArray(item.phases) || item.phases.length < 3) {
      throw new Error(`${item.code} must contain at least three phases`);
    }
    for (const phase of item.phases) {
      requireText(phase.name, `${item.code}.phase.name`);
      requireText(phase.orderId, `${item.code}.${phase.name}.orderId`);
      requireText(phase.routeCode, `${item.code}.${phase.name}.routeCode`);
      if (!Number.isInteger(phase.samples) || phase.samples < 1) {
        throw new Error(`${item.code}.${phase.name}.samples must be positive`);
      }
      for (const propertyId of Object.keys(phase.values || {})) {
        if (!propertyIds.has(propertyId)) {
          throw new Error(`${item.code}.${phase.name} contains unknown signal ${propertyId}`);
        }
      }
    }
    const result = evaluateCase(scenario, item.code);
    const actual = result.boundaries.map(({ processCode, type, reason }) => ({
      processCode,
      type,
      reason,
    }));
    if (JSON.stringify(actual) !== JSON.stringify(item.expectations.boundaries)) {
      throw new Error(`${item.code} boundary result does not match expectations`);
    }
    if (result.qualityExcursions.length !== item.expectations.qualityExcursions) {
      throw new Error(`${item.code} quality result does not match expectations`);
    }
  }
  return scenario;
}

function validateProductionModel(model) {
  if (!model || model.status !== 'TEST_ONLY_DRAFT') {
    throw new Error('productionModel.status must be TEST_ONLY_DRAFT');
  }
  requireText(model.routeCode, 'productionModel.routeCode');
  requireText(model.routeName, 'productionModel.routeName');
  requireText(model.quantityUnit, 'productionModel.quantityUnit');
  if (!Array.isArray(model.processes) || model.processes.length !== 2) {
    throw new Error('productionModel must define jet and saccharification');
  }
  if (!Array.isArray(model.materials) || model.materials.length < 3) {
    throw new Error('productionModel must define at least three material states');
  }
  if (!Array.isArray(model.qualityStandards) || model.qualityStandards.length < 3) {
    throw new Error('productionModel must define material quality standards');
  }

  const materials = new Map();
  for (const [index, material] of model.materials.entries()) {
    const code = requireText(material.code, `productionModel.materials[${index}].code`);
    if (materials.has(code)) throw new Error(`duplicate material code ${code}`);
    if (material.batchManaged !== true) {
      throw new Error(`${code} must be batch managed`);
    }
    materials.set(code, material);
  }

  const processCodes = new Set();
  for (const [index, process] of model.processes.entries()) {
    const code = requireText(process.code, `productionModel.processes[${index}].code`);
    if (processCodes.has(code)) throw new Error(`duplicate production process ${code}`);
    processCodes.add(code);
    if (process.batchRequired !== true) {
      throw new Error(`${code} must create a process batch`);
    }
    if (!['NONE', 'VIRTUAL_LOT', 'MATERIAL_LOT'].includes(process.inventoryMaterialization)) {
      throw new Error(`${code}.inventoryMaterialization is unsupported`);
    }
    if (!materials.has(process.inputMaterial) || !materials.has(process.outputMaterial)) {
      throw new Error(`${code} references an unknown material`);
    }
    if (!(process.inputQuantity > 0) || !(process.outputQuantity > 0)) {
      throw new Error(`${code} quantities must be positive`);
    }
  }

  const standards = new Map();
  for (const [index, standard] of model.qualityStandards.entries()) {
    const code = requireText(
      standard.code,
      `productionModel.qualityStandards[${index}].code`,
    );
    if (standards.has(code)) throw new Error(`duplicate quality standard ${code}`);
    if (!materials.has(standard.materialCode)) {
      throw new Error(`${code} references an unknown material`);
    }
    if (!Array.isArray(standard.items) || standard.items.length < 1) {
      throw new Error(`${code} must define inspection items`);
    }
    for (const [itemIndex, item] of standard.items.entries()) {
      requireText(item.code, `${code}.items[${itemIndex}].code`);
      requireText(item.name, `${code}.items[${itemIndex}].name`);
      requireText(item.unit, `${code}.items[${itemIndex}].unit`);
      requireText(item.source, `${code}.items[${itemIndex}].source`);
      if (
        !Number.isFinite(item.minimum)
        || !Number.isFinite(item.maximum)
        || !Number.isFinite(item.result)
        || item.minimum > item.maximum
        || item.result < item.minimum
        || item.result > item.maximum
      ) {
        throw new Error(`${code}.items[${itemIndex}] has invalid test limits/result`);
      }
    }
    standards.set(code, standard);
  }
  for (const material of materials.values()) {
    if (!standards.has(material.qualityStandard)) {
      throw new Error(`${material.code} references an unknown quality standard`);
    }
  }
}

function expandCaseFrames(scenario, caseCode) {
  const item = scenario.cases.find((candidate) => candidate.code === caseCode);
  if (!item) throw new Error(`unknown case ${caseCode}`);
  const frames = [];
  let values = structuredClone(scenario.baseValues);
  for (const phase of item.phases) {
    values = { ...values, ...(phase.values || {}) };
    for (let phaseSample = 1; phaseSample <= phase.samples; phaseSample += 1) {
      const frameValues = { ...values };
      for (const [propertyId, step] of Object.entries(phase.stepPerSample || {})) {
        frameValues[propertyId] = Number(
          (values[propertyId] + step * (phaseSample - 1)).toFixed(6),
        );
      }
      frames.push({
        caseCode,
        phase: phase.name,
        phaseSample,
        globalSample: frames.length + 1,
        orderId: phase.orderId,
        orderActive: phase.orderActive !== false,
        routeCode: phase.routeCode,
        values: frameValues,
      });
    }
    for (const [propertyId, step] of Object.entries(phase.stepPerSample || {})) {
      values[propertyId] = Number(
        (values[propertyId] + step * phase.samples).toFixed(6),
      );
    }
  }
  const baseTime = Date.parse(item.baseTime || scenario.sampling.baseTime);
  return frames.map((frame, index) => ({
    ...frame,
    sampleTime: new Date(baseTime + index * scenario.sampling.intervalSeconds * 1000)
      .toISOString(),
  }));
}

function evaluateCase(scenario, caseCode) {
  const frames = expandCaseFrames(scenario, caseCode);
  const interval = scenario.sampling.intervalSeconds;
  const state = new Map(scenario.processes.map((process) => [
    process.code,
    {
      active: false,
      activeOrderId: null,
      startSince: null,
      endSince: null,
      qualityExcursion: false,
    },
  ]));
  const boundaries = [];
  const qualityExcursions = [];

  for (const frame of frames) {
    for (const process of scenario.processes) {
      const current = state.get(process.code);
      if (current.active) {
        let hardReason = null;
        if (!frame.orderActive) hardReason = 'ORDER_CLEARED';
        else if (frame.orderId !== current.activeOrderId) hardReason = 'ORDER_SWITCH';
        else if (frame.routeCode !== process.routeCode) hardReason = 'ROUTE_SWITCH';
        if (hardReason) {
          boundaries.push(boundary(frame, process, 'END', hardReason));
          current.active = false;
          current.activeOrderId = null;
          current.endSince = null;
        } else {
          const endMatches = frame.values[process.flowProperty] < process.endFlowThreshold
            && frame.values[process.pumpProperty] === false
            && frame.values[process.valveProperty] === false;
          current.endSince = endMatches
            ? current.endSince ?? frame.globalSample
            : null;
          if (
            endMatches
            && heldSeconds(current.endSince, frame.globalSample, interval)
              >= process.endHoldSeconds
          ) {
            boundaries.push(boundary(frame, process, 'END', 'SIGNAL_HOLD'));
            current.active = false;
            current.activeOrderId = null;
            current.endSince = null;
          }
        }
      }

      if (!current.active) {
        const startMatches = frame.orderActive
          && frame.routeCode === process.routeCode
          && frame.values[process.flowProperty] > process.startFlowThreshold
          && frame.values[process.pumpProperty] === true
          && frame.values[process.valveProperty] === true;
        current.startSince = startMatches
          ? current.startSince ?? frame.globalSample
          : null;
        if (
          startMatches
          && heldSeconds(current.startSince, frame.globalSample, interval)
            >= process.startHoldSeconds
        ) {
          boundaries.push(boundary(frame, process, 'START', 'SIGNAL_HOLD'));
          current.active = true;
          current.activeOrderId = frame.orderId;
          current.startSince = null;
        }
      }

      const baume = frame.values[process.baumeProperty];
      const outsideRange = frame.routeCode === process.routeCode
        && (baume < process.baumeRange.minimum || baume > process.baumeRange.maximum);
      if (outsideRange && !current.qualityExcursion) {
        qualityExcursions.push({
          processCode: process.code,
          propertyId: process.baumeProperty,
          value: baume,
          minimum: process.baumeRange.minimum,
          maximum: process.baumeRange.maximum,
          sampleTime: frame.sampleTime,
          globalSample: frame.globalSample,
        });
      }
      current.qualityExcursion = outsideRange;
    }
  }
  return { caseCode, frames, boundaries, qualityExcursions };
}

function heldSeconds(since, current, interval) {
  return since === null ? 0 : (current - since) * interval;
}

function boundary(frame, process, type, reason) {
  return {
    processCode: process.code,
    processName: process.name,
    type,
    reason,
    orderId: frame.orderId,
    routeCode: frame.routeCode,
    sampleTime: frame.sampleTime,
    globalSample: frame.globalSample,
  };
}

function buildTelemetryEnvelopes(scenario, caseCode, marker = 'ADP_E2E_FRUCTOSE') {
  const signalById = new Map(scenario.signals.map((signal) => [signal.propertyId, signal]));
  return expandCaseFrames(scenario, caseCode).map((frame) => ({
    eventId: `${marker}-${caseCode}-${String(frame.globalSample).padStart(4, '0')}`,
    messageId: `${marker}-MSG-${caseCode}-${String(frame.globalSample).padStart(4, '0')}`,
    tenantId: scenario.scope.tenantId,
    plantId: scenario.scope.plantId,
    lineId: scenario.scope.lineId,
    gatewayId: scenario.device.gatewayId,
    productId: scenario.device.productId,
    deviceId: scenario.device.deviceId,
    eventTimeMs: Date.parse(frame.sampleTime),
    ingestTimeMs: Date.parse(frame.sampleTime) + 1000,
    sourceEpoch: 1,
    sequence: frame.globalSample,
    sequenceOrigin: 'GATEWAY',
    headers: {
      scenarioCode: scenario.scenarioCode,
      caseCode,
      phase: frame.phase,
      orderId: frame.orderId,
      routeCode: frame.routeCode,
    },
    points: Object.entries(frame.values).map(([propertyId, value]) => {
      const signal = signalById.get(propertyId);
      return {
        propertyId,
        ...(signal.dataType === 'boolean' ? { boolValue: value } : { doubleValue: value }),
        unit: signal.unit,
        qualityCode: 'GOOD',
        sampleTimeMs: Date.parse(frame.sampleTime),
        calibrationVersion: signal.calibrationVersion,
      };
    }),
  }));
}

function runCli(argv) {
  const [command, input, caseCode, marker] = argv;
  if (!command || !input) {
    throw new Error(
      'usage: fructose-line-scenario.js <validate|evaluate|frames|envelopes> <scenario.json> [case] [marker]',
    );
  }
  const scenario = loadScenario(path.resolve(input));
  if (command === 'validate') {
    return {
      scenarioCode: scenario.scenarioCode,
      signalCount: scenario.signals.length,
      sampleIntervalSeconds: scenario.sampling.intervalSeconds,
      samplesPerMinute: 60 / scenario.sampling.intervalSeconds,
      cases: scenario.cases.map((item) => evaluateCase(scenario, item.code)),
    };
  }
  if (!caseCode) throw new Error(`${command} requires a case code`);
  if (command === 'evaluate') return evaluateCase(scenario, caseCode);
  if (command === 'frames') return expandCaseFrames(scenario, caseCode);
  if (command === 'envelopes') return buildTelemetryEnvelopes(scenario, caseCode, marker);
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
  buildTelemetryEnvelopes,
  evaluateCase,
  expandCaseFrames,
  loadScenario,
  validateScenario,
  validateProductionModel,
};

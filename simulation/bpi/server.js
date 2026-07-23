const http = require('node:http');
const { createHmac, timingSafeEqual } = require('node:crypto');
const { FIXED_TIME, clone, createScenario, sha256 } = require('./scenario');

const JSON_TYPE = 'application/json; charset=utf-8';
const PROBLEM_TYPE = 'application/problem+json; charset=utf-8';
const POINT_CATALOG_CURSOR_SECRET = 'bpi-simulator-point-catalog-cursor-v1';
const SOURCE_SEQUENCE_FINGERPRINT = /^sha256:[0-9a-f]{64}$/;
const FEATURE_FLAG_TENANT = 'TENANT-01';
const DATASET_PREDICTION_TIME_POLICY = 'AUTOMATIC_BATCH_START';
const DATASET_FEATURE_CUTOFF_POLICY = 'AT_OR_BEFORE_PREDICTION_TIME';
const DATASET_SPLIT_POLICY = 'PRODUCTION_TIME';
const DATASET_ARTIFACT_SCHEMA_VERSION = 'bpi.dataset-parquet.v1';
const DATASET_MATERIALIZER_VERSION = 'bpi-dataset-materializer/0.1.0';
const DATASET_OBJECT_BUCKET = 'bpi-datasets';
const DATASET_CATALOG_NAME = 'ft_mes_bpi';
const DATASET_CATALOG_PUBLISHER_VERSION = 'bpi-dataset-catalog-publisher/0.1.0';
const DATASET_RETENTION_ARCHIVER_VERSION = 'bpi-dataset-retention-archiver/0.1.0';
const DATASET_RECOVERY_PROFILE = 'bpi-dataset-recovery-v1';
const DATASET_RECOVERY_BUCKET = 'bpi-dataset-recovery';
const DATASET_MLFLOW_REGISTRAR_VERSION = 'bpi-dataset-mlflow-registrar/0.1.0';
const DATASET_MLFLOW_TRACKING_PROFILE = 'bpi-mlflow-dataset-v1';
const DATASET_TRAINING_OBJECTIVE = 'BATCH_START_BOUNDARY_REVIEW_RISK';
const DATASET_TRAINING_POLICY = 'bpi-training-readiness/batch-start-boundary-v2';
const TRAINING_DATA_COVERAGE_POLICY = 'bpi-training-data-coverage/batch-start-boundary-v1';
const REQUIRED_TRAINING_REVIEWED_BATCHES = 200;
const REQUIRED_TRAINING_PRODUCTION_DAYS = 7;
const REQUIRED_TRAINING_ACCEPTED_START_LABELS = 100;
const REQUIRED_TRAINING_REJECTED_START_LABELS = 10;
const DATASET_FEATURE_REFS = new Set([
  'batch.order_id', 'batch.material_code', 'batch.stage_code', 'batch.quantity_unit',
  'rule.version_id', 'topology.version_id', 'point_catalog.snapshot_id',
]);
const DATASET_PROCESS_FEATURE_REF = /^process\.window\.[a-z0-9][a-z0-9._-]*$/;
const DATASET_PROCESS_SIGNAL = /^[A-Za-z0-9][A-Za-z0-9._:-]*$/;
const DATASET_NUMERIC_WINDOW_METRICS = new Set(['MEAN', 'MIN', 'MAX', 'LAST', 'DELTA', 'SLOPE']);
const DATASET_LABEL_REFS = new Set([
  'review.manual_start_time', 'review.manual_end_time', 'review.reference_quantity',
  'review.boundary_acceptance', 'review.quantity_acceptance',
  'batch.automatic_end_time', 'batch.automatic_quantity',
]);
const FEATURE_FLAG_DEFINITIONS = [
  { flagKey: 'bpi.ui', displayName: 'BPI 导航入口', description: '控制旧平台是否展示 BPI 导航入口。', riskLevel: 'MEDIUM', enforcementStatus: 'ENFORCED', editable: true, blockedReason: null },
  { flagKey: 'bpi.commands', displayName: '批次人工命令', description: '控制候选确认、驳回和批次状态命令。', riskLevel: 'HIGH', enforcementStatus: 'ENFORCED', editable: true, blockedReason: null },
  { flagKey: 'bpi.rule-management', displayName: '规则与拓扑管理', description: '控制拓扑、规则、回放和发布类写操作。', riskLevel: 'HIGH', enforcementStatus: 'ENFORCED', editable: true, blockedReason: null },
  { flagKey: 'bpi.shadow-only', displayName: '影子模式', description: '保证 Phase 1 只生成影子事实，不写生产业务状态。', riskLevel: 'CRITICAL', enforcementStatus: 'CODE_INVARIANT', editable: false, blockedReason: 'Phase 1 必须保持 shadow-only，不能通过运行页面关闭。' },
  { flagKey: 'bpi.auto-confirm', displayName: '候选自动确认', description: '允许高置信候选绕过人工确认。', riskLevel: 'CRITICAL', enforcementStatus: 'PHASE_LOCKED', editable: false, blockedReason: '未完成真实 7-14 天影子验收，Phase 2 自动确认门禁未开放。' },
  { flagKey: 'bpi.wms-link', displayName: 'WMS 完工入库联动', description: '允许 BPI 发起幂等 WMS 完工入库命令。', riskLevel: 'CRITICAL', enforcementStatus: 'PHASE_LOCKED', editable: false, blockedReason: 'QCS/WMS Phase 2 契约和真实写回验收尚未完成。' },
];

function meta(operationId, pagination = {}) {
  return {
    traceId: `sim-${sha256(operationId).slice(0, 16)}`,
    generatedAt: FIXED_TIME,
    snapshotAt: pagination.snapshotAt || FIXED_TIME,
    nextCursor: pagination.nextCursor || null,
  };
}

function envelope(operationId, data, pagination = {}) {
  return { data: clone(data), meta: meta(operationId, pagination) };
}

function calibrationScopeFingerprint(url) {
  return sha256(['plantId', 'lineId', 'productId', 'deviceId', 'propertyId']
    .map((name) => `${name}=${url.searchParams.get(name) || ''}`)
    .join('|'));
}

function encodeCalibrationCursor(cursor) {
  return Buffer.from(JSON.stringify(cursor), 'utf8').toString('base64url');
}

function decodeCalibrationCursor(value, expectedScopeFingerprint) {
  if (!value || value.length > 2048) throw new Error('invalid cursor');
  const decoded = JSON.parse(Buffer.from(value, 'base64url').toString('utf8'));
  if (decoded.version !== 1 || decoded.scopeFingerprint !== expectedScopeFingerprint
      || !Number.isFinite(Date.parse(decoded.snapshotAt))
      || !Number.isFinite(Date.parse(decoded.submittedAt))
      || Date.parse(decoded.submittedAt) > Date.parse(decoded.snapshotAt)
      || typeof decoded.id !== 'string' || !decoded.id) {
    throw new Error('invalid cursor');
  }
  return decoded;
}

function pointCatalogScopeFingerprint(url, search) {
  return sha256(['plantId', 'lineId']
    .map((name) => `${name}=${url.searchParams.get(name) || ''}`)
    .concat(`search=${search}`)
    .join('|'));
}

function pointCatalogCursorSignature(payload) {
  return createHmac('sha256', POINT_CATALOG_CURSOR_SECRET)
    .update(`bpi.point-catalog.cursor.v1|${payload}`)
    .digest('base64url');
}

function encodePointCatalogCursor(cursor) {
  const payload = Buffer.from(JSON.stringify(cursor), 'utf8').toString('base64url');
  return `${payload}.${pointCatalogCursorSignature(payload)}`;
}

function decodePointCatalogCursor(value, expectedScopeFingerprint) {
  if (!value || value.length > 4096) throw new Error('invalid cursor');
  const parts = value.split('.');
  if (parts.length !== 2) throw new Error('invalid cursor');
  const expectedSignature = Buffer.from(pointCatalogCursorSignature(parts[0]), 'utf8');
  const actualSignature = Buffer.from(parts[1], 'utf8');
  if (actualSignature.length !== expectedSignature.length
      || !timingSafeEqual(actualSignature, expectedSignature)) throw new Error('invalid cursor');
  const decoded = JSON.parse(Buffer.from(parts[0], 'base64url').toString('utf8'));
  if (decoded.version !== 1 || decoded.scopeFingerprint !== expectedScopeFingerprint
      || typeof decoded.snapshotId !== 'string' || !decoded.snapshotId
      || typeof decoded.productId !== 'string' || !decoded.productId
      || typeof decoded.deviceId !== 'string' || !decoded.deviceId
      || typeof decoded.propertyId !== 'string' || !decoded.propertyId) {
    throw new Error('invalid cursor');
  }
  return decoded;
}

function comparePointIdentity(left, right) {
  for (const field of ['productId', 'deviceId', 'propertyId']) {
    if (left[field] < right[field]) return -1;
    if (left[field] > right[field]) return 1;
  }
  return 0;
}

function problem(status, title, detail, operationId, currentRevision = null) {
  return {
    type: `https://ft-mes.local/problems/${status}`,
    title,
    status,
    detail,
    traceId: meta(operationId).traceId,
    currentRevision,
  };
}

function send(res, status, body, operationId, headers = {}) {
  const serialized = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': status >= 400 ? PROBLEM_TYPE : JSON_TYPE,
    'Content-Length': Buffer.byteLength(serialized),
    'X-BPI-Operation-Id': operationId,
    ...headers,
  });
  res.end(serialized);
}

async function readJson(req) {
  const chunks = [];
  for await (const chunk of req) {
    chunks.push(chunk);
  }
  if (chunks.length === 0) return {};
  return JSON.parse(Buffer.concat(chunks).toString('utf8'));
}

function revisionFromHeader(value) {
  const match = String(value || '').match(/(\d+)/);
  return match ? Number(match[1]) : null;
}

function commandContext(req, res, operationId, entityRevision, state, pathname) {
  const key = req.headers['idempotency-key'];
  const revision = revisionFromHeader(req.headers['if-match']);
  if (!key || revision === null) {
    send(
      res,
      428,
      problem(428, 'Precondition Required', 'Idempotency-Key and If-Match are required.', operationId),
      operationId,
    );
    return null;
  }
  const cacheKey = String(key);
  if (state.idempotency.has(cacheKey)) {
    const cached = state.idempotency.get(cacheKey);
    if (cached.method !== req.method || cached.pathname !== pathname) {
      send(res, 409, problem(409, 'Idempotency Conflict', 'Idempotency-Key was reused for another command.', operationId), operationId);
      return null;
    }
    send(res, cached.status, cached.body, operationId, { 'Idempotent-Replay': 'true' });
    return null;
  }
  if (revision !== entityRevision) {
    const body = problem(409, 'Revision Conflict', `Expected revision ${entityRevision}, received ${revision}.`, operationId, entityRevision);
    send(res, 409, body, operationId);
    return null;
  }
  return { key, cacheKey, method: req.method, pathname };
}

function rememberAndSend(state, context, res, status, body, operationId) {
  state.idempotency.set(context.cacheKey, {
    method: context.method,
    pathname: context.pathname,
    status,
    body: clone(body),
  });
  send(res, status, body, operationId);
}

function batchFromCandidate(candidate) {
  return {
    id: 'BATCH-S07-20260712-001', batchNo: 'S07-20260712-001', lineId: candidate.lineId,
    stageCode: 'EVAPORATION', orderId: candidate.orderId, materialCode: 'MAT-SYRUP-001',
    state: 'ACTIVE', revision: 1, shadow: true, startTime: candidate.boundaryTime, endTime: null,
    quantity: 12.4, quantityUnit: 't', dryMatter: 7.86, qualityGate: 'PENDING',
    wmsStatus: 'NOT_REQUESTED', ruleVersion: candidate.ruleVersion, topologyVersion: candidate.topologyVersion,
  };
}

function shadowRunSourceCoverage(state, run) {
  const catalog = state.pointCatalogHistory
    .find((item) => item.snapshot.id === run.pointCatalogSnapshotId);
  const points = catalog?.points || [];
  const activeRegistered = (point) => point.registered === true
    && point.deviceState === 'ACTIVE'
    && point.propertyPresent === true
    && typeof point.unit === 'string'
    && point.unit.trim().length > 0;
  const physicalIdentity = (point) => point.sourceSequenceEnabled === true
    && point.sourceSequenceRequired === true
    && ['DEVICE', 'GATEWAY'].includes(point.sourceSequenceOrigin)
    && typeof point.sourceSequenceBindingFingerprint === 'string'
    && point.sourceSequenceBindingFingerprint.length > 0;
  const approvedCalibration = (point) => typeof point.calibrationVersion === 'string'
    && point.calibrationVersion.trim().length > 0
    && point.calibrationStatus === 'VERIFIED';
  const count = (predicate) => points.filter(predicate).length;
  const pinnedPointCount = points.length;
  const activeRegisteredPointCount = count(activeRegistered);
  const physicalIdentityPointCount = count((point) =>
    activeRegistered(point) && physicalIdentity(point));
  const freshSequenceQualifiedPointCount = count((point) =>
    activeRegistered(point) && physicalIdentity(point) && point.sourceSequenceQualified === true);
  const approvedCalibrationPointCount = count((point) =>
    activeRegistered(point) && approvedCalibration(point));
  const readyPointCount = count((point) => activeRegistered(point)
    && physicalIdentity(point)
    && point.sourceSequenceQualified === true
    && approvedCalibration(point));
  return {
    pinnedPointCount,
    activeRegisteredPointCount,
    physicalIdentityPointCount,
    freshSequenceQualifiedPointCount,
    approvedCalibrationPointCount,
    readyPointCount,
    fullyReady: pinnedPointCount > 0 && readyPointCount === pinnedPointCount,
  };
}

function shadowRunReadiness(state, run, sourceCoverage) {
  const rule = state.rules.find((item) => item.id === run.ruleVersionId);
  const topology = state.topologies.find((item) => item.id === run.topologyVersionId);
  const catalog = state.pointCatalog;
  const readiness = {
    rulePublished: rule?.state === 'PUBLISHED',
    ruleActive: rule?.lifecycleAction === 'ACTIVATE' && rule?.lifecycleActive === true,
    publicationConfirmed: rule?.publicationStatus === 'PUBLISHED',
    applicationApplied: rule?.applicationStatus === 'APPLIED',
    runtimeReady: rule?.runtimeReadinessStatus === 'READY',
    topologyPublished: topology?.state === 'PUBLISHED',
    topologySnapshotPinned: topology?.validatedPointCatalogSnapshotId === run.pointCatalogSnapshotId
      && topology?.validatedPointCatalogChecksum === run.pointCatalogChecksum,
    pointCatalogCurrent: catalog?.snapshot.id === run.pointCatalogSnapshotId
      && catalog?.snapshot.checksum === run.pointCatalogChecksum,
    pointCatalogReady: sourceCoverage.fullyReady,
  };
  readiness.ready = Object.values(readiness).every(Boolean);
  return readiness;
}

function shadowRunTelemetryCoverage(state, run, sourceCoverage) {
  const windowStart = run.startedAt || null;
  const windowEnd = windowStart
    ? (run.completedAt || run.decidedAt || run.cancelledAt || FIXED_TIME) : null;
  const pinnedPoints = state.pointCatalog.points.filter((point) =>
    point.snapshotId === run.pointCatalogSnapshotId
      && point.plantId === run.plantId
      && point.lineId === run.lineId);
  const observedPoints = new Set();
  const authoritativeSequencePoints = new Set();
  const calibratedPoints = new Set();
  const goodQualityPoints = new Set();
  const acceptedEvents = new Set();
  const acceptedObservations = new Set();
  const rejectedObservations = new Set();
  const gapEvents = new Set();
  const outOfOrderEvents = new Set();
  const observedTimes = [];

  if (windowStart) {
    const startMs = Date.parse(windowStart);
    const endMs = Date.parse(windowEnd);
    pinnedPoints.forEach((entry) => {
      const pointKey = `${entry.productId}|${entry.deviceId}|${entry.propertyId}`;
      state.telemetryEvents.filter((event) =>
        event.plantId === run.plantId
          && event.lineId === run.lineId
          && event.productId === entry.productId
          && event.deviceId === entry.deviceId
          && Date.parse(event.eventTime) >= startMs
          && Date.parse(event.eventTime) <= endMs
          && Date.parse(event.createdAt) >= startMs
          && Date.parse(event.createdAt) <= endMs)
        .forEach((event) => {
          event.points.filter((point) =>
            point.propertyId === entry.propertyId
              && Date.parse(point.sampleTime) >= startMs
              && Date.parse(point.sampleTime) <= endMs
              && Date.parse(point.createdAt) >= startMs
              && Date.parse(point.createdAt) <= endMs)
            .forEach((point) => {
              observedPoints.add(pointKey);
              acceptedEvents.add(event.id);
              acceptedObservations.add(point.id);
              observedTimes.push(point.sampleTime);
              if (event.sequenceOrigin === entry.sourceSequenceOrigin
                  && event.sourceEpoch > 0 && event.sequence > 0) {
                authoritativeSequencePoints.add(pointKey);
              }
              if (entry.calibrationVersion
                  && point.calibrationVersion === entry.calibrationVersion) {
                calibratedPoints.add(pointKey);
              }
              if (point.qualityCode === 'GOOD') goodQualityPoints.add(pointKey);
              if (event.sequenceDisposition === 'GAP') gapEvents.add(event.id);
              if (event.sequenceDisposition === 'OUT_OF_ORDER') outOfOrderEvents.add(event.id);
            });
          event.rejects.filter((reject) => reject.propertyId === entry.propertyId)
            .forEach((reject) => rejectedObservations.add(reject.id));
        });
    });
  }

  const blockers = [];
  if (!windowStart) {
    blockers.push('TELEMETRY_WINDOW_NOT_STARTED');
  } else {
    if (sourceCoverage.pinnedPointCount <= 0
        || observedPoints.size < sourceCoverage.pinnedPointCount) {
      blockers.push('TELEMETRY_POINTS_NOT_OBSERVED');
    }
    if (sourceCoverage.pinnedPointCount <= 0
        || authoritativeSequencePoints.size < sourceCoverage.pinnedPointCount) {
      blockers.push('TELEMETRY_AUTHORITATIVE_SEQUENCE_INCOMPLETE');
    }
    if (sourceCoverage.pinnedPointCount <= 0
        || calibratedPoints.size < sourceCoverage.pinnedPointCount) {
      blockers.push('TELEMETRY_CALIBRATION_INCOMPLETE');
    }
    if (sourceCoverage.pinnedPointCount <= 0
        || goodQualityPoints.size < sourceCoverage.pinnedPointCount) {
      blockers.push('TELEMETRY_GOOD_QUALITY_INCOMPLETE');
    }
    if (gapEvents.size > 0) blockers.push('TELEMETRY_SEQUENCE_GAP_DETECTED');
    if (outOfOrderEvents.size > 0) blockers.push('TELEMETRY_OUT_OF_ORDER_DETECTED');
  }
  observedTimes.sort();
  return {
    windowStarted: Boolean(windowStart),
    windowStart,
    windowEnd,
    pinnedPointCount: sourceCoverage.pinnedPointCount,
    observedPointCount: observedPoints.size,
    authoritativeSequencePointCount: authoritativeSequencePoints.size,
    calibratedPointCount: calibratedPoints.size,
    goodQualityPointCount: goodQualityPoints.size,
    acceptedEventCount: acceptedEvents.size,
    acceptedObservationCount: acceptedObservations.size,
    rejectedObservationCount: rejectedObservations.size,
    gapEventCount: gapEvents.size,
    outOfOrderEventCount: outOfOrderEvents.size,
    firstObservedAt: observedTimes[0] || null,
    lastObservedAt: observedTimes.at(-1) || null,
    fullyCovered: Boolean(windowStart)
      && sourceCoverage.pinnedPointCount > 0 && blockers.length === 0,
    blockers,
  };
}

function shadowRunMetrics(state, run) {
  const activeReviews = state.shadowRunReviews.filter((item) => item.shadowRunId === run.id && item.state === 'ACTIVE');
  const endTime = run.completedAt || run.decidedAt || run.cancelledAt || FIXED_TIME;
  const observedDurationSeconds = run.startedAt
    ? Math.max(0, Math.floor((Date.parse(endTime) - Date.parse(run.startedAt)) / 1000))
    : 0;
  const acceptedBoundaryCount = activeReviews.reduce((sum, review) => sum
    + Number(review.startBoundaryAccepted) + Number(review.endBoundaryAccepted), 0);
  const totalBoundaryCount = activeReviews.length * 2;
  const automaticQuantityTotal = activeReviews.reduce((sum, review) => sum + review.automaticQuantity, 0);
  const referenceQuantityTotal = activeReviews.reduce((sum, review) => sum + review.referenceQuantity, 0);
  const deviations = activeReviews.map((review) => review.quantityDeviationPercent);
  const boundaryAgreement = totalBoundaryCount ? acceptedBoundaryCount / totalBoundaryCount : 0;
  const cumulativeQuantityDeviationPercent = referenceQuantityTotal
    ? Math.abs(automaticQuantityTotal - referenceQuantityTotal) / referenceQuantityTotal * 100
    : 0;
  const unresolvedCriticalIncidentCount = run.startedAt
    ? state.dataQualityIncidents.filter((incident) => incident.severity === 'CRITICAL'
      && incident.state !== 'RESOLVED' && incident.lineId === run.lineId
      && incident.affectedRules.includes(run.ruleVersion)
      && Date.parse(incident.lastSeen) >= Date.parse(run.startedAt)).length
    : 0;
  return {
    observedDurationSeconds,
    reviewedBatchCount: activeReviews.length,
    acceptedBoundaryCount,
    totalBoundaryCount,
    boundaryAgreement,
    quantitySampleCount: activeReviews.length,
    automaticQuantityTotal,
    referenceQuantityTotal,
    quantityUnit: activeReviews[0]?.quantityUnit || null,
    cumulativeQuantityDeviationPercent,
    meanQuantityDeviationPercent: deviations.length
      ? deviations.reduce((sum, value) => sum + value, 0) / deviations.length : 0,
    maximumQuantityDeviationPercent: deviations.length ? Math.max(...deviations) : 0,
    unresolvedCriticalIncidentCount,
    durationGatePassed: observedDurationSeconds >= run.minimumDurationDays * 86_400,
    reviewCountGatePassed: activeReviews.length >= run.minimumReviewedBatches,
    boundaryAgreementGatePassed: boundaryAgreement >= run.minimumBoundaryAgreement,
    quantityGatePassed: activeReviews.length > 0
      && cumulativeQuantityDeviationPercent <= run.quantityTolerancePercent,
    dataQualityGatePassed: unresolvedCriticalIncidentCount === 0,
  };
}

function shadowRunTrainingDataCoverage(state, run) {
  const activeReviews = state.shadowRunReviews
    .filter((item) => item.shadowRunId === run.id && item.state === 'ACTIVE');
  const reviewedBatchCount = new Set(activeReviews.map((review) => review.batchId)).size;
  const distinctProductionDayCount = new Set(activeReviews
    .map((review) => new Date(review.automaticStartTime).toISOString().slice(0, 10))).size;
  const acceptedStartLabelCount = activeReviews
    .filter((review) => review.startBoundaryAccepted).length;
  const rejectedStartLabelCount = activeReviews.length - acceptedStartLabelCount;
  const blockers = [];
  if (reviewedBatchCount < REQUIRED_TRAINING_REVIEWED_BATCHES) {
    blockers.push('TRAINING_REVIEWED_BATCHES_BELOW_MINIMUM');
  }
  if (distinctProductionDayCount < REQUIRED_TRAINING_PRODUCTION_DAYS) {
    blockers.push('TRAINING_PRODUCTION_DAYS_BELOW_MINIMUM');
  }
  if (acceptedStartLabelCount < REQUIRED_TRAINING_ACCEPTED_START_LABELS) {
    blockers.push('TRAINING_ACCEPTED_START_LABELS_BELOW_MINIMUM');
  }
  if (rejectedStartLabelCount < REQUIRED_TRAINING_REJECTED_START_LABELS) {
    blockers.push('TRAINING_REJECTED_START_LABELS_BELOW_MINIMUM');
  }
  return {
    policyVersion: TRAINING_DATA_COVERAGE_POLICY,
    requiredReviewedBatchCount: REQUIRED_TRAINING_REVIEWED_BATCHES,
    reviewedBatchCount,
    requiredProductionDayCount: REQUIRED_TRAINING_PRODUCTION_DAYS,
    distinctProductionDayCount,
    requiredAcceptedStartLabelCount: REQUIRED_TRAINING_ACCEPTED_START_LABELS,
    acceptedStartLabelCount,
    requiredRejectedStartLabelCount: REQUIRED_TRAINING_REJECTED_START_LABELS,
    rejectedStartLabelCount,
    thresholdsMet: blockers.length === 0,
    blockers,
  };
}

function hydrateShadowRun(state, run) {
  const sourceCoverage = shadowRunSourceCoverage(state, run);
  const readiness = shadowRunReadiness(state, run, sourceCoverage);
  const telemetryCoverage = shadowRunTelemetryCoverage(state, run, sourceCoverage);
  const metrics = shadowRunMetrics(state, run);
  const trainingDataCoverage = shadowRunTrainingDataCoverage(state, run);
  const blockers = [];
  const readinessBlockers = [
    ['rulePublished', 'RULE_NOT_PUBLISHED'], ['ruleActive', 'RULE_NOT_ACTIVE'],
    ['publicationConfirmed', 'RULE_PUBLICATION_NOT_CONFIRMED'], ['applicationApplied', 'RULE_APPLICATION_NOT_APPLIED'],
    ['runtimeReady', 'RULE_RUNTIME_NOT_READY'], ['topologyPublished', 'TOPOLOGY_NOT_PUBLISHED'],
    ['topologySnapshotPinned', 'TOPOLOGY_POINT_CATALOG_MISMATCH'], ['pointCatalogCurrent', 'POINT_CATALOG_NOT_CURRENT'],
    ['pointCatalogReady', 'POINT_CATALOG_NOT_READY'],
  ];
  readinessBlockers.forEach(([field, code]) => { if (!readiness[field]) blockers.push(code); });
  blockers.push(...telemetryCoverage.blockers);
  if (!metrics.durationGatePassed) blockers.push('MINIMUM_DURATION_NOT_REACHED');
  if (!metrics.reviewCountGatePassed) blockers.push('MINIMUM_BATCH_REVIEWS_NOT_REACHED');
  if (!metrics.boundaryAgreementGatePassed) blockers.push('BOUNDARY_AGREEMENT_BELOW_THRESHOLD');
  if (!metrics.quantityGatePassed) blockers.push('CUMULATIVE_QUANTITY_DEVIATION_OUT_OF_TOLERANCE');
  if (!metrics.dataQualityGatePassed) blockers.push('UNRESOLVED_CRITICAL_DATA_QUALITY');
  return {
    ...run,
    readiness,
    sourceCoverage,
    telemetryCoverage,
    metrics,
    trainingDataCoverage,
    blockers,
    readyForApproval: run.state === 'EVALUATING' && blockers.length === 0,
  };
}

function prepareShadowRunAcceptance(state) {
  Object.assign(state.rule, {
    state: 'PUBLISHED', revision: 12,
    approvalStatus: 'APPROVED', approvalRevision: 2,
    approvalSubmittedBy: 'simulated.process.engineer', approvalSubmittedAt: '2026-07-11T06:00:00.000Z',
    approvalDecidedBy: 'simulated.bpi.admin', approvalDecidedAt: '2026-07-11T06:10:00.000Z',
    lifecycleAction: 'ACTIVATE', lifecycleSequence: 1, lifecycleActive: true,
    publicationStatus: 'PUBLISHED', publicationRevision: 14,
    publicationAttemptCount: 1, publicationTotalAttemptCount: 1,
    publicationPublishedAt: '2026-07-11T06:11:00.000Z', publicationLastError: null,
    applicationStatus: 'APPLIED', applicationDeploymentId: 'flink-shadow-control-1',
    applicationObservedAt: '2026-07-11T06:12:00.000Z', applicationReceivedAt: '2026-07-11T06:12:01.000Z',
    applicationErrorCode: null, applicationErrorDetail: null,
    runtimeReadinessStatus: 'READY', runtimeReadinessDeploymentId: 'flink-shadow-evaluator-1',
    runtimeReadinessObservedAt: '2026-07-11T06:13:00.000Z', runtimeReadinessReceivedAt: '2026-07-11T06:13:01.000Z',
    runtimeReadinessReasonCode: null, runtimeReadinessDetail: null,
    runtimePointCatalogEventId: 'catalog-shadow-ready-1',
    runtimePointCatalogSourceRevision: state.pointCatalog.snapshot.sourceRevision,
  });
  state.batches = Array.from({ length: 10 }, (_, index) => {
    const start = new Date(Date.parse('2026-07-05T10:00:00.000Z') + index * 14 * 60 * 60 * 1000);
    const end = new Date(start.getTime() + 2 * 60 * 60 * 1000);
    return {
      id: stableUuid(`shadow-acceptance-batch-${index + 1}`),
      batchNo: `SHADOW-ACCEPT-${String(index + 1).padStart(3, '0')}`,
      lineId: state.rule.lineId, stageCode: 'EVAPORATION', orderId: `MO-SHADOW-${String(index + 1).padStart(3, '0')}`,
      materialCode: 'MAT-SYRUP-001', state: 'CLOSED_RAW', revision: 2, shadow: true,
      startTime: start.toISOString(), endTime: end.toISOString(), quantity: 100 + index,
      quantityUnit: 't', dryMatter: 63 + index * 0.5, qualityGate: 'PENDING', wmsStatus: 'NOT_REQUESTED',
      ruleVersion: `${state.rule.code}@${state.rule.version}`, topologyVersion: state.rule.topologyVersion,
    };
  });
  state.shadowRuns = [];
  state.shadowRunReviews = [];
  state.telemetryEvents = [{
    id: stableUuid('shadow-telemetry-event-1'),
    eventId: 'SHADOW-TELEMETRY-001',
    tenantId: FEATURE_FLAG_TENANT,
    plantId: 'PLANT-01',
    lineId: 'LINE-S07-01',
    productId: 'PRODUCT-SUGAR',
    deviceId: 'DEVICE-S07-01',
    eventTime: '2026-07-10T08:00:00.000Z',
    createdAt: '2026-07-10T08:00:01.000Z',
    sourceEpoch: 7,
    sequence: 1017,
    sequenceOrigin: 'DEVICE',
    sequenceDisposition: 'IN_ORDER',
    points: state.pointCatalog.points.map((point, index) => ({
      id: stableUuid(`shadow-telemetry-point-${index + 1}`),
      propertyId: point.propertyId,
      qualityCode: 'GOOD',
      calibrationVersion: point.calibrationVersion,
      sampleTime: '2026-07-10T08:00:00.000Z',
      createdAt: '2026-07-10T08:00:01.000Z',
    })),
    rejects: [],
  }];
  state.batchEvents = [];
  state.batchEventsById = new Map();
  state.batchEvidenceById = new Map();
  state.batchReleases = new Map();
  state.wmsInboundReversalTasks = new Map();
  state.forceCloseTasks = new Map();
  state.idempotency = new Map();
  return { ruleId: state.rule.id, preparedBatchCount: state.batches.length };
}

function prepareBatchReleaseAcceptance(state) {
  const specs = [
    { key: 'closedRaw', suffix: 'RAW', state: 'CLOSED_RAW', qualityGate: 'NOT_APPLICABLE', wmsStatus: 'NOT_REQUESTED', gate: null, inbound: null },
    { key: 'waiting', suffix: 'WAIT-QA', state: 'WAIT_QA', qualityGate: 'WAITING', wmsStatus: 'NOT_REQUESTED', gate: 'WAITING', inbound: null },
    { key: 'rejected', suffix: 'QA-REJECTED', state: 'REJECTED', qualityGate: 'REJECTED', wmsStatus: 'NOT_REQUESTED', gate: 'REJECTED', inbound: null },
    { key: 'wmsPending', suffix: 'WMS-PENDING', state: 'RELEASED', qualityGate: 'ACCEPTED', wmsStatus: 'PENDING', gate: 'ACCEPTED', inbound: 'PENDING' },
    { key: 'wmsFailed', suffix: 'WMS-FAILED', state: 'RELEASED', qualityGate: 'ACCEPTED', wmsStatus: 'FAILED', gate: 'ACCEPTED', inbound: 'REJECTED' },
    { key: 'inbounded', suffix: 'INBOUNDED', state: 'INBOUNDED', qualityGate: 'ACCEPTED', wmsStatus: 'INBOUNDED', gate: 'ACCEPTED', inbound: 'ACCEPTED' },
  ];
  const releases = new Map();
  const timelines = new Map();
  const evidenceById = new Map();
  const ids = {};
  const batches = specs.map((spec, index) => {
    const id = stableUuid(`batch-release-ui-${spec.key}`);
    ids[spec.key] = id;
    const start = new Date(Date.parse('2026-07-12T01:00:00.000Z') + index * 75 * 60 * 1000);
    const end = new Date(start.getTime() + 60 * 60 * 1000);
    const observedAt = new Date(end.getTime() + 5 * 60 * 1000).toISOString();
    const batch = {
      id,
      batchNo: `ADP-E2E-${spec.suffix}`,
      tenantId: 'TENANT-01',
      plantId: 'PLANT-01',
      lineId: 'LINE-S07-01',
      stageCode: 'FINISHED_PRODUCT',
      orderId: `MO-ADP-E2E-${String(index + 1).padStart(3, '0')}`,
      materialCode: 'MAT-SUGAR-FG-001',
      state: spec.state,
      revision: 2 + index,
      shadow: false,
      startTime: start.toISOString(),
      endTime: end.toISOString(),
      quantity: 42.125 + index,
      quantityUnit: 't',
      dryMatter: 41.7 + index,
      qualityGate: spec.qualityGate,
      wmsStatus: spec.wmsStatus,
      ruleVersion: 'RULE-FG-BATCH@2.0.0',
      topologyVersion: 'TOPO-FG@4',
    };
    let qualityGate = null;
    if (spec.gate) {
      const accepted = spec.gate === 'ACCEPTED';
      const rejected = spec.gate === 'REJECTED';
      qualityGate = {
        id: stableUuid(`quality-gate-${spec.key}`),
        externalGateId: `QCS-GATE-ADP-E2E-${String(index + 1).padStart(3, '0')}`,
        externalRevision: rejected ? 2 : 1,
        sourceEventId: `qcs-quality-gate-${spec.key}-event`,
        state: spec.gate,
        releaseQuantity: accepted ? batch.quantity : null,
        quantityUnit: accepted ? batch.quantityUnit : null,
        materialCode: accepted ? batch.materialCode : null,
        observedAt,
        inspections: spec.gate === 'WAITING' ? [
          { inspectionCode: 'FG-BRIX', inspectionRecordId: `QCS-INSPECTION-${spec.key}-BRIX`, required: true, disposition: 'ACCEPTED', finalResult: true, observedAt },
          { inspectionCode: 'FG-MICRO', inspectionRecordId: `QCS-INSPECTION-${spec.key}-MICRO`, required: true, disposition: 'PENDING', finalResult: false, observedAt },
        ] : rejected ? [
          { inspectionCode: 'FG-BRIX', inspectionRecordId: `QCS-INSPECTION-${spec.key}-BRIX`, required: true, disposition: 'ACCEPTED', finalResult: true, observedAt },
          { inspectionCode: 'FG-MICRO', inspectionRecordId: `QCS-INSPECTION-${spec.key}-MICRO`, required: true, disposition: 'REJECTED', finalResult: true, observedAt },
        ] : [
          { inspectionCode: 'FG-BRIX', inspectionRecordId: `QCS-INSPECTION-${spec.key}-BRIX`, required: true, disposition: 'ACCEPTED', finalResult: true, observedAt },
          { inspectionCode: 'FG-MICRO', inspectionRecordId: `QCS-INSPECTION-${spec.key}-MICRO`, required: true, disposition: 'ACCEPTED', finalResult: true, observedAt },
        ],
      };
    }
    let inbound = null;
    if (spec.inbound) {
      const commandEventId = stableUuid(`wms-command-${spec.key}`);
      inbound = {
        id: stableUuid(`wms-inbound-${spec.key}`),
        commandEventId,
        idempotencyKey: `WMS-INBOUND-${id}`,
        status: spec.inbound,
        receiptEventId: spec.inbound === 'PENDING' ? null : `wms-inbound-receipt-${spec.key}`,
        documentId: spec.inbound === 'ACCEPTED' ? 'WMS-IN-ADP-E2E-0001' : null,
        errorCode: spec.inbound === 'REJECTED' ? 'WMS_LOCATION_LOCKED' : null,
        detail: spec.inbound === 'REJECTED' ? '目标成品库位正在盘点锁定，WMS 拒绝本次入库命令。' : null,
        observedAt: spec.inbound === 'PENDING' ? null : new Date(end.getTime() + 8 * 60 * 1000).toISOString(),
        revision: 1,
        outboxStatus: 'PUBLISHED',
        deliveryAttemptCount: 1,
        reconciliationCount: 0,
        commandPublishedAt: new Date(end.getTime() + 6 * 60 * 1000).toISOString(),
        lastReconciledAt: null,
        lastReconciledBy: null,
        reconcileAfter: new Date(end.getTime() + 11 * 60 * 1000).toISOString(),
        reconciliationAllowed: spec.inbound === 'PENDING',
        reconciliationBlockedReason: spec.inbound === 'PENDING' ? null : 'WMS_RECEIPT_TERMINAL',
      };
    }
    releases.set(id, { batch, qualityGate, wmsInbound: inbound, wmsInboundReversal: null });
    const timeline = [
      { revision: 1, action: 'START_BOUNDARY_CONFIRMED', at: start.toISOString(), actor: 'simulated.batch.engine', reason: '多信号边界证据达到启动阈值', fromState: null, toState: 'ACTIVE' },
      { revision: 2, action: 'END_BOUNDARY_CONFIRMED', at: end.toISOString(), actor: 'simulated.batch.engine', reason: '流量归零且目标累计量完成', fromState: 'ACTIVE', toState: 'CLOSED_RAW' },
    ];
    if (spec.gate) timeline.push({
      revision: timeline.length + 1,
      action: `QUALITY_GATE_${spec.gate}`,
      at: observedAt,
      actor: 'qcs.integration',
      reason: spec.gate === 'ACCEPTED' ? '全部必检项目最终合格' : spec.gate === 'REJECTED' ? '必检项目最终不合格' : '仍有必检项目等待最终结果',
      fromState: 'CLOSED_RAW',
      toState: spec.state,
    });
    if (spec.inbound) timeline.push({
      revision: timeline.length + 1,
      action: spec.inbound === 'PENDING' ? 'WMS_INBOUND_COMMAND_CREATED' : `WMS_INBOUND_${spec.inbound}`,
      at: inbound.observedAt || new Date(end.getTime() + 6 * 60 * 1000).toISOString(),
      actor: spec.inbound === 'PENDING' ? 'bpi.wms.outbox' : 'wms.integration',
      reason: spec.inbound === 'ACCEPTED' ? 'WMS 返回持久化完工入库单据' : spec.inbound === 'REJECTED' ? inbound.detail : '幂等命令已持久化并等待回执',
      fromState: 'RELEASED',
      toState: spec.state,
    });
    timelines.set(id, timeline);
    evidenceById.set(id, {
      start: [
        { eventId: `EVT-${spec.key}-START-FLOW`, signal: 'instantFlowAboveThreshold', classification: 'REQUIRED', satisfied: true, value: 18.4, unit: 't/h', quality: 'GOOD', eventTime: start.toISOString(), source: 'JetLinks' },
        { eventId: `EVT-${spec.key}-START-ORDER`, signal: 'productionOrderReleased', classification: 'REQUIRED', satisfied: true, value: batch.orderId, unit: null, quality: 'GOOD', eventTime: start.toISOString(), source: 'WOM' },
      ],
      end: [
        { eventId: `EVT-${spec.key}-END-FLOW`, signal: 'instantFlowBelowThreshold', classification: 'REQUIRED', satisfied: true, value: 0.08, unit: 't/h', quality: 'GOOD', eventTime: end.toISOString(), source: 'JetLinks' },
        { eventId: `EVT-${spec.key}-END-TOTAL`, signal: 'targetQuantityCompleted', classification: 'QUORUM', satisfied: true, value: batch.quantity, unit: batch.quantityUnit, quality: 'GOOD', eventTime: end.toISOString(), source: 'BPI' },
      ],
    });
    return batch;
  });
  state.batches = batches;
  state.batchEvents = [];
  state.batchEventsById = timelines;
  state.batchEvidenceById = evidenceById;
  state.batchReleases = releases;
  state.wmsInboundReversalTasks = new Map();
  state.forceCloseTasks = new Map();
  state.shadowRuns = [];
  state.shadowRunReviews = [];
  state.idempotency = new Map();
  return { preparedBatchCount: batches.length, batchIds: ids };
}

function endCandidateFromStart(candidate) {
  const boundaryTime = '2026-07-12T08:29:40.000Z';
  return {
    id: 'CAND-END-S07-001',
    candidateKey: '93cdf206-4664-58bd-b7c9-cdbca49e74d4',
    boundaryType: 'END',
    lineId: candidate.lineId,
    orderId: candidate.orderId,
    batchId: null,
    boundaryTime,
    state: 'PENDING',
    revision: 1,
    confidence: 0.96,
    ruleVersion: 'RULE-S07-END@1.2.0',
    topologyVersion: candidate.topologyVersion,
    missingSignals: [],
    evidence: [
      {
        eventId: 'EVT-END-FLOW-001', signal: 'instantFlowBelowStopThreshold', classification: 'QUORUM',
        satisfied: true, value: 0.2, unit: 't/h', quality: 'GOOD', eventTime: boundaryTime, source: 'JetLinks',
      },
      {
        eventId: 'EVT-END-PUMP-001', signal: 'feedPumpStopped', classification: 'QUORUM',
        satisfied: true, value: true, unit: null, quality: 'GOOD', eventTime: boundaryTime, source: 'JetLinks',
      },
      {
        eventId: 'EVT-END-VALVE-001', signal: 'transferPathClosed', classification: 'QUORUM',
        satisfied: true, value: true, unit: null, quality: 'GOOD', eventTime: boundaryTime, source: 'JetLinks',
      },
    ],
    review: null,
  };
}

function candidates(state) {
  return [state.candidate, state.endCandidate].filter(Boolean);
}

function scopedDataQualityIncidents(state, url) {
  const plantId = url.searchParams.get('plantId');
  const lineId = url.searchParams.get('lineId');
  return state.dataQualityIncidents.filter((item) => (!plantId || item.plantId === plantId)
    && (!lineId || item.lineId === lineId));
}

function dataQualitySeverityRank(severity) {
  return { CRITICAL: 4, ERROR: 3, WARNING: 2, INFO: 1 }[severity] || 0;
}

function dataQualitySummary(state, url) {
  const incidents = scopedDataQualityIncidents(state, url);
  const unresolved = incidents.filter((item) => item.state !== 'RESOLVED');
  const issueCounts = {};
  unresolved.forEach((item) => { issueCounts[item.issueCode] = (issueCounts[item.issueCode] || 0) + 1; });
  const affectedBatches = new Set(unresolved.flatMap((item) => item.affectedBatches));
  return {
    open: incidents.filter((item) => item.state === 'OPEN').length,
    acknowledged: incidents.filter((item) => item.state === 'ACKNOWLEDGED').length,
    resolved: incidents.filter((item) => item.state === 'RESOLVED').length,
    critical: unresolved.filter((item) => item.severity === 'CRITICAL').length,
    affectedBatches: affectedBatches.size,
    issueCounts,
  };
}

function featureFlagScopeKey(scopeType, plantId, lineId) {
  if (scopeType === 'TENANT') return FEATURE_FLAG_TENANT;
  if (scopeType === 'PLANT') return plantId;
  if (scopeType === 'LINE') return lineId;
  return null;
}

function featureFlagPriority(item) {
  if (item.tenantId !== FEATURE_FLAG_TENANT) return 10;
  return { GLOBAL: 20, TENANT: 30, PLANT: 40, LINE: 50 }[item.scopeType] || 0;
}

function featureFlagApplies(item, plantId, lineId) {
  if (item.tenantId === '*' && item.scopeType === 'GLOBAL' && item.scopeKey === '*') return true;
  if (item.tenantId !== FEATURE_FLAG_TENANT) return false;
  return (item.scopeType === 'GLOBAL' && item.scopeKey === '*')
    || (item.scopeType === 'TENANT' && item.scopeKey === FEATURE_FLAG_TENANT)
    || (item.scopeType === 'PLANT' && item.scopeKey === plantId)
    || (item.scopeType === 'LINE' && item.scopeKey === lineId);
}

function featureFlagViews(state, plantId, lineId, selectedScopeType) {
  const selectedScopeKey = featureFlagScopeKey(selectedScopeType, plantId, lineId);
  return FEATURE_FLAG_DEFINITIONS.map((definition) => {
    const effective = state.featureFlags
      .filter((item) => item.active && item.flagKey === definition.flagKey
        && featureFlagApplies(item, plantId, lineId))
      .sort((left, right) => featureFlagPriority(right) - featureFlagPriority(left))[0] || null;
    const selected = state.featureFlags.find((item) => item.tenantId === FEATURE_FLAG_TENANT
      && item.scopeType === selectedScopeType && item.scopeKey === selectedScopeKey
      && item.flagKey === definition.flagKey) || null;
    return {
      ...definition,
      effectiveEnabled: Boolean(effective?.enabled),
      effectiveScopeType: effective?.scopeType || 'DEFAULT_DENY',
      effectiveScopeKey: effective?.scopeKey || '-',
      effectiveRevision: effective?.revision ?? null,
      selectedScopeType,
      selectedScopeKey,
      overrideExists: Boolean(selected),
      overrideActive: Boolean(selected?.active),
      overrideEnabled: selected ? selected.enabled : null,
      overrideRevision: selected?.revision || 0,
      updatedBy: selected?.updatedBy || null,
      updatedAt: selected?.updatedAt || null,
      lastReason: selected?.lastReason || null,
    };
  });
}

function match(pathname, pattern) {
  const result = pathname.match(pattern);
  return result ? result.slice(1).map(decodeURIComponent) : null;
}

function stableUuid(value) {
  const digest = sha256(value);
  return `${digest.slice(0, 8)}-${digest.slice(8, 12)}-5${digest.slice(13, 16)}-a${digest.slice(17, 20)}-${digest.slice(20, 32)}`;
}

function latestDatasetMaterialization(state, snapshotId) {
  return state.datasetMaterializations
    .filter((item) => item.snapshotId === snapshotId)
    .sort((left, right) => right.createdAt.localeCompare(left.createdAt)
      || right.id.localeCompare(left.id))[0] || null;
}

function datasetSnapshotView(state, snapshot) {
  const materialization = latestDatasetMaterialization(state, snapshot.id);
  return {
    ...clone(snapshot),
    materializationState: materialization?.state || 'NOT_STARTED',
    artifactUri: materialization?.artifactUri || null,
    latestMaterialization: materialization ? clone(materialization) : null,
  };
}

function datasetSnapshotSummary(state, snapshot) {
  const materialization = latestDatasetMaterialization(state, snapshot.id);
  return {
    id: snapshot.id,
    snapshotVersion: snapshot.snapshotVersion,
    state: snapshot.state,
    revision: snapshot.revision,
    freezeAt: snapshot.freezeAt,
    manifestChecksum: snapshot.manifestChecksum,
    includedCount: snapshot.includedCount,
    excludedCount: snapshot.excludedCount,
    materializationState: materialization?.state || 'NOT_STARTED',
    createdAt: snapshot.createdAt,
    completedAt: snapshot.completedAt,
    failureCode: snapshot.failureCode,
    failureDetail: snapshot.failureDetail,
  };
}

function datasetDefinitionView(state, definition) {
  const latestSnapshot = state.datasetSnapshots
    .filter((snapshot) => snapshot.datasetId === definition.id)
    .sort((left, right) => right.snapshotVersion - left.snapshotVersion)[0] || null;
  return {
    ...clone(definition),
    latestSnapshot: latestSnapshot ? datasetSnapshotSummary(state, latestSnapshot) : null,
  };
}

function normalizeDatasetStrings(values) {
  if (!Array.isArray(values) || !values.length) return null;
  const normalized = values.map((value) => String(value || '').trim()).sort();
  if (normalized.some((value) => !value) || new Set(normalized).size !== normalized.length) return null;
  return normalized;
}

function normalizeProcessSignalWindows(values) {
  if (values === undefined || values === null) return [];
  if (!Array.isArray(values) || values.length > 20) return null;
  const normalized = values.map((value) => {
    if (!value || typeof value !== 'object') return null;
    const acceptedQualityCodes = normalizeDatasetStrings(value.acceptedQualityCodes);
    const featureRef = String(value.featureRef || '').trim();
    const signal = String(value.signal || '').trim();
    const valueType = String(value.valueType || '').trim();
    const metric = String(value.metric || '').trim();
    const startOffsetSeconds = Number(value.startOffsetSeconds);
    const endOffsetSeconds = Number(value.endOffsetSeconds);
    const minimumSamples = Number(value.minimumSamples);
    const maximumGapSeconds = Number(value.maximumGapSeconds);
    const expectedUnit = String(value.expectedUnit || '').trim();
    const validMetric = valueType === 'BOOLEAN'
      ? metric === 'TRUE_RATIO'
      : valueType === 'NUMERIC' && DATASET_NUMERIC_WINDOW_METRICS.has(metric);
    if (!DATASET_PROCESS_FEATURE_REF.test(featureRef)
        || !DATASET_PROCESS_SIGNAL.test(signal)
        || !validMetric
        || !Number.isInteger(startOffsetSeconds)
        || startOffsetSeconds < -3600 || startOffsetSeconds > -1
        || !Number.isInteger(endOffsetSeconds)
        || endOffsetSeconds < -3599 || endOffsetSeconds > 0
        || endOffsetSeconds <= startOffsetSeconds
        || !Number.isInteger(minimumSamples)
        || minimumSamples < 2 || minimumSamples > 900
        || !Number.isInteger(maximumGapSeconds)
        || maximumGapSeconds < 1 || maximumGapSeconds > 600
        || !expectedUnit || expectedUnit.length > 32
        || typeof value.requireCalibration !== 'boolean'
        || !acceptedQualityCodes
        || acceptedQualityCodes.length > 2
        || acceptedQualityCodes.some((code) => !['GOOD', 'SUBSTITUTED'].includes(code))) {
      return null;
    }
    const controlled = {
      featureRef,
      signal,
      valueType,
      metric,
      startOffsetSeconds,
      endOffsetSeconds,
      minimumSamples,
      maximumGapSeconds,
      expectedUnit,
      requireCalibration: value.requireCalibration,
      acceptedQualityCodes,
    };
    return { ...controlled, checksum: sha256(controlled) };
  });
  if (normalized.some((value) => !value)) return null;
  normalized.sort((left, right) => left.featureRef.localeCompare(right.featureRef));
  if (new Set(normalized.map((value) => value.featureRef)).size !== normalized.length) return null;
  return normalized;
}

function validateDatasetDefinition(body) {
  const lineIds = normalizeDatasetStrings(body.lineIds);
  const featureRefs = normalizeDatasetStrings(body.featureRefs);
  const labelRefs = normalizeDatasetStrings(body.labelRefs);
  const processSignalWindows = normalizeProcessSignalWindows(body.processSignalWindows);
  const validCode = /^[A-Za-z0-9][A-Za-z0-9._:-]*$/.test(body.datasetCode || '');
  const validVersion = /^[A-Za-z0-9][A-Za-z0-9._-]*$/.test(body.version || '');
  const processFeatureRefs = processSignalWindows?.map((window) => window.featureRef) || [];
  const selectedProcessFeatureRefs = featureRefs?.filter((reference) =>
    reference.startsWith('process.window.')) || [];
  const validFeatures = featureRefs?.every((reference) =>
    DATASET_FEATURE_REFS.has(reference) || DATASET_PROCESS_FEATURE_REF.test(reference));
  const processFeatureRefsMatch = processSignalWindows
    && JSON.stringify(processFeatureRefs) === JSON.stringify(selectedProcessFeatureRefs);
  const validLabels = labelRefs?.every((reference) => DATASET_LABEL_REFS.has(reference));
  const validDelay = Number.isInteger(body.maxLabelDelayHours)
    && body.maxLabelDelayHours >= 1 && body.maxLabelDelayHours <= 2160;
  const confidence = Number(body.minimumConfidence);
  if (!validCode || !validVersion || !String(body.name || '').trim()
      || !String(body.plantId || '').trim() || !lineIds || !featureRefs || !labelRefs
      || !processSignalWindows || !validFeatures || !processFeatureRefsMatch || !validLabels || !validDelay
      || !Number.isFinite(confidence) || confidence < 0 || confidence > 1
      || body.predictionTimePolicy !== DATASET_PREDICTION_TIME_POLICY
      || body.featureCutoffPolicy !== DATASET_FEATURE_CUTOFF_POLICY
      || body.splitPolicy !== DATASET_SPLIT_POLICY
      || !String(body.reason || '').trim()) {
    return null;
  }
  return { lineIds, featureRefs, processSignalWindows, labelRefs, confidence };
}

function prepareDatasetManifestAcceptance(state) {
  prepareShadowRunAcceptance(state);
  const runId = stableUuid('dataset-manifest-approved-shadow-run');
  const run = {
    id: runId,
    runCode: 'DATASET-MANIFEST-ACCEPTANCE',
    name: 'Dataset manifest acceptance source',
    tenantId: FEATURE_FLAG_TENANT,
    plantId: 'PLANT-01',
    lineId: 'LINE-S07-01',
    state: 'APPROVED',
    revision: 15,
    ruleVersionId: state.rule.id,
    ruleVersion: `${state.rule.code}@${state.rule.version}`,
    topologyVersionId: state.topology.id,
    topologyVersion: `${state.topology.code}@${state.topology.version}`,
    pointCatalogSnapshotId: state.pointCatalog.snapshot.id,
    pointCatalogChecksum: state.pointCatalog.snapshot.checksum,
    minimumDurationDays: 7,
    minimumReviewedBatches: 3,
    boundaryToleranceSeconds: 60,
    minimumBoundaryAgreement: 0.95,
    quantityTolerancePercent: 2,
    createdBy: 'simulated.process.engineer',
    createdAt: '2026-07-05T07:00:00.000Z',
    startedBy: 'simulated.process.engineer',
    startedAt: '2026-07-05T08:00:00.000Z',
    completedBy: 'simulated.process.engineer',
    completedAt: '2026-07-12T07:50:00.000Z',
    decidedBy: 'simulated.bpi.admin',
    decidedAt: '2026-07-12T07:55:00.000Z',
    decisionReason: '受控影子样本满足数据集清单验收条件',
    cancelledBy: null,
    cancelledAt: null,
    cancellationReason: null,
  };
  state.shadowRuns = [run];
  state.shadowRunReviews = state.batches.slice(0, 3).map((batch, index) => {
    const automaticStart = Date.parse(batch.startTime);
    const reviewDelayHours = index === 2 ? 80 : index + 2;
    const accepted = index !== 1;
    return {
      id: stableUuid(`dataset-manifest-review-${index + 1}`),
      shadowRunId: run.id,
      batchId: batch.id,
      batchNo: batch.batchNo,
      reviewSequence: 1,
      state: 'ACTIVE',
      automaticStartTime: batch.startTime,
      automaticEndTime: batch.endTime,
      manualStartTime: index === 1
        ? new Date(automaticStart + 120_000).toISOString() : batch.startTime,
      manualEndTime: index === 1
        ? new Date(Date.parse(batch.endTime) + 120_000).toISOString() : batch.endTime,
      startDeviationSeconds: index === 1 ? 120 : 0,
      endDeviationSeconds: index === 1 ? 120 : 0,
      startBoundaryAccepted: accepted,
      endBoundaryAccepted: accepted,
      automaticQuantity: batch.quantity,
      referenceQuantity: index === 1 ? batch.quantity * 0.9 : batch.quantity,
      quantityUnit: batch.quantityUnit,
      quantityDeviationPercent: index === 1 ? 11.111111 : 0,
      quantityWithinTolerance: accepted,
      reviewedBy: 'simulated.shift.lead',
      reviewReason: `数据集清单受控复核样本 ${index + 1}`,
      reviewedAt: new Date(automaticStart + reviewDelayHours * 3_600_000).toISOString(),
      supersededAt: null,
    };
  });
  state.datasetDefinitions = [];
  state.datasetSnapshots = [];
  state.pendingDatasetSnapshotIds = new Set();
  state.datasetMaterializations = [];
  state.pendingDatasetMaterializationIds = new Set();
  state.datasetCatalogPublications = [];
  state.pendingDatasetCatalogPublicationIds = new Set();
  state.datasetRetentionArchives = [];
  state.pendingDatasetRetentionArchiveIds = new Set();
  state.datasetMlflowRegistrations = [];
  state.pendingDatasetMlflowRegistrationIds = new Set();
  state.datasetTrainingReadinessAssessments = [];
  state.idempotency = new Map();
  return { runId, ruleVersionId: state.rule.id, preparedReviewCount: state.shadowRunReviews.length };
}

function selectedDatasetPayload(references, values) {
  return Object.fromEntries(references.map((reference) => [reference, values[reference]]));
}

function simulatedProcessSignalValue(definition, sampleIndex) {
  if (definition.metric === 'TRUE_RATIO') return sampleIndex % 2 === 0 ? 1 : 0.5;
  const base = 18.6 + sampleIndex * 0.4;
  if (definition.metric === 'MIN') return Number((base - 0.8).toFixed(6));
  if (definition.metric === 'MAX') return Number((base + 0.8).toFixed(6));
  if (definition.metric === 'DELTA') return Number((0.8 + sampleIndex * 0.1).toFixed(6));
  if (definition.metric === 'SLOPE') return Number((0.02 + sampleIndex * 0.001).toFixed(6));
  return Number(base.toFixed(6));
}

function buildSimulatedProcessSignalWindowEvidence(state, definition, batch, sampleIndex) {
  const predictionMillis = Date.parse(batch.startTime);
  const binding = state.topology.definition.bindings.find((item) =>
    item.signal === definition.signal);
  const point = binding ? state.pointCatalog.points.find((item) =>
    item.productId === binding.productId
      && item.deviceId === binding.deviceId
      && item.propertyId === binding.propertyId) : null;
  const blockers = [];
  if (!binding) blockers.push('WINDOW_BINDING_MISSING');
  if (binding && binding.unit !== definition.expectedUnit) {
    blockers.push('WINDOW_BINDING_UNIT_MISMATCH');
  }
  if (binding && !point) blockers.push('WINDOW_POINT_CATALOG_MISSING');
  if (point && !point.ready) blockers.push('WINDOW_POINT_NOT_READY');
  if (point && point.unit !== definition.expectedUnit) {
    blockers.push('WINDOW_POINT_CATALOG_UNIT_MISMATCH');
  }
  if (definition.requireCalibration && binding && point
      && (binding.calibrationVersion !== point.calibrationVersion
        || point.calibrationStatus !== 'VERIFIED')) {
    blockers.push('WINDOW_CALIBRATION_MISMATCH');
  }
  const stableBlockers = [...new Set(blockers)].sort();
  const sampleIntervalSeconds = Math.min(15, definition.maximumGapSeconds);
  const syntheticSourcePointCount = Math.max(
    definition.minimumSamples,
    Math.floor(
      (definition.endOffsetSeconds - definition.startOffsetSeconds)
      / sampleIntervalSeconds,
    ) + 1,
  );
  const sourcePointCount = stableBlockers.length ? 0 : syntheticSourcePointCount;
  const sourceFingerprint = sha256({
    simulationOnly: true,
    batchId: batch.id,
    definitionChecksum: definition.checksum,
    predictionTime: batch.startTime,
    sourcePointCount,
  });
  const evidence = {
    featureRef: definition.featureRef,
    signal: definition.signal,
    metric: definition.metric,
    valueType: definition.valueType,
    windowStart: new Date(
      predictionMillis + definition.startOffsetSeconds * 1000,
    ).toISOString(),
    windowEnd: new Date(
      predictionMillis + definition.endOffsetSeconds * 1000,
    ).toISOString(),
    predictionTime: batch.startTime,
    physicalPoint: {
      productId: binding?.productId || null,
      deviceId: binding?.deviceId || null,
      propertyId: binding?.propertyId || null,
    },
    expectedUnit: definition.expectedUnit,
    minimumSamples: definition.minimumSamples,
    maximumGapSeconds: definition.maximumGapSeconds,
    sourcePointCount,
    acceptedSampleCount: sourcePointCount,
    rejectedQualityCount: 0,
    lateAvailabilityCount: 0,
    unitMismatchCount: stableBlockers.some((code) => code.includes('UNIT_MISMATCH')) ? 1 : 0,
    valueTypeMismatchCount: 0,
    calibrationMismatchCount: stableBlockers.includes('WINDOW_CALIBRATION_MISMATCH') ? 1 : 0,
    maximumObservedGapSeconds: sourcePointCount ? sampleIntervalSeconds : null,
    numericValue: stableBlockers.length
      ? null : simulatedProcessSignalValue(definition, sampleIndex),
    state: stableBlockers.length ? 'BLOCKED' : 'READY',
    blockerCodes: stableBlockers,
    sourceFingerprint,
  };
  return {
    ...evidence,
    factChecksum: sha256(evidence),
  };
}

function buildDatasetSnapshot(state, snapshot) {
  const definition = state.datasetDefinitions.find((item) => item.id === snapshot.datasetId);
  if (!definition) return;
  snapshot.state = 'BUILDING';
  snapshot.revision = 2;
  snapshot.startedAt = FIXED_TIME;
  snapshot.attemptCount = 1;
  const selectedRuleIds = new Set(snapshot.ruleVersionIds);
  const rows = state.shadowRunReviews
    .filter((review) => review.state === 'ACTIVE' && Date.parse(review.reviewedAt) <= Date.parse(snapshot.freezeAt))
    .map((review) => ({
      review,
      run: state.shadowRuns.find((run) => run.id === review.shadowRunId),
      batch: state.batches.find((batch) => batch.id === review.batchId),
    }))
    .filter(({ run, batch }) => run && batch && run.state === 'APPROVED'
      && Date.parse(run.decidedAt) <= Date.parse(snapshot.freezeAt)
      && snapshot.lineIds.includes(run.lineId)
      && (!selectedRuleIds.size || selectedRuleIds.has(run.ruleVersionId)))
    .sort((left, right) => left.batch.lineId.localeCompare(right.batch.lineId)
      || Date.parse(left.batch.startTime) - Date.parse(right.batch.startTime)
      || left.batch.id.localeCompare(right.batch.id)
      || left.review.id.localeCompare(right.review.id));
  const exclusionSummary = {};
  const samples = rows.map(({ review, run, batch }, sampleIndex) => {
    const acceptedChecks = Number(review.startBoundaryAccepted)
      + Number(review.endBoundaryAccepted) + Number(review.quantityWithinTolerance);
    const confidence = Number((acceptedChecks / 3).toFixed(6));
    const reasons = [];
    if (Date.parse(review.reviewedAt) < Date.parse(batch.startTime)) {
      reasons.push('LABEL_AVAILABLE_BEFORE_PREDICTION_TIME');
    }
    if (Date.parse(review.reviewedAt) > Date.parse(snapshot.freezeAt)) {
      reasons.push('LABEL_AVAILABLE_AFTER_FREEZE_AT');
    }
    if (Date.parse(review.reviewedAt) - Date.parse(batch.startTime)
        > definition.maxLabelDelayHours * 3_600_000) {
      reasons.push('LABEL_DELAY_EXCEEDED');
    }
    if (snapshot.excludeLowConfidence && confidence < definition.minimumConfidence) {
      reasons.push('CONFIDENCE_BELOW_THRESHOLD');
      if (!review.startBoundaryAccepted) reasons.push('START_BOUNDARY_OUTSIDE_TOLERANCE');
      if (!review.endBoundaryAccepted) reasons.push('END_BOUNDARY_OUTSIDE_TOLERANCE');
      if (!review.quantityWithinTolerance) reasons.push('QUANTITY_OUTSIDE_TOLERANCE');
    }
    const processSignalWindows = definition.processSignalWindows.map((window) =>
      buildSimulatedProcessSignalWindowEvidence(state, window, batch, sampleIndex));
    const blockedProcessSignalWindows = processSignalWindows
      .filter((evidence) => evidence.state !== 'READY');
    if (blockedProcessSignalWindows.length) {
      reasons.push('PROCESS_SIGNAL_WINDOW_NOT_READY');
      blockedProcessSignalWindows.forEach((evidence) =>
        reasons.push(...evidence.blockerCodes));
    }
    const stableReasons = [...new Set(reasons)].sort();
    stableReasons.forEach((reason) => { exclusionSummary[reason] = (exclusionSummary[reason] || 0) + 1; });
    const featureValues = {
      'batch.order_id': batch.orderId,
      'batch.material_code': batch.materialCode,
      'batch.stage_code': batch.stageCode,
      'batch.quantity_unit': batch.quantityUnit,
      'rule.version_id': run.ruleVersionId,
      'topology.version_id': run.topologyVersionId,
      'point_catalog.snapshot_id': run.pointCatalogSnapshotId,
    };
    processSignalWindows.forEach((evidence) => {
      featureValues[evidence.featureRef] = evidence.numericValue;
    });
    const labelValues = {
      'review.manual_start_time': review.manualStartTime,
      'review.manual_end_time': review.manualEndTime,
      'review.reference_quantity': review.referenceQuantity,
      'review.boundary_acceptance': {
        start: review.startBoundaryAccepted,
        end: review.endBoundaryAccepted,
      },
      'review.quantity_acceptance': review.quantityWithinTolerance,
      'batch.automatic_end_time': batch.endTime,
      'batch.automatic_quantity': batch.quantity,
    };
    return {
      reviewId: review.id,
      shadowRunId: run.id,
      batchId: batch.id,
      batchNo: batch.batchNo,
      lineId: batch.lineId,
      included: stableReasons.length === 0,
      exclusionReasons: stableReasons,
      predictionTime: batch.startTime,
      featureCutoff: batch.startTime,
      labelAvailableAt: review.reviewedAt,
      confidence,
      splitKey: batch.startTime.slice(0, 7),
      featurePayload: selectedDatasetPayload(definition.featureRefs, featureValues),
      labelPayload: selectedDatasetPayload(definition.labelRefs, labelValues),
      sourcePayload: {
        reviewId: review.id,
        shadowRunId: run.id,
        batchId: batch.id,
        batchNo: batch.batchNo,
        automaticStartTime: batch.startTime,
        automaticEndTime: batch.endTime,
        reviewedAt: review.reviewedAt,
        startBoundaryAccepted: review.startBoundaryAccepted,
        endBoundaryAccepted: review.endBoundaryAccepted,
        quantityWithinTolerance: review.quantityWithinTolerance,
        processSignalWindows,
      },
    };
  });
  const includedCount = samples.filter((sample) => sample.included).length;
  const manifest = {
    schemaVersion: 'bpi.dataset-manifest.v1',
    definition: {
      datasetId: definition.id,
      datasetCode: definition.datasetCode,
      datasetVersion: definition.version,
      definitionChecksum: definition.checksum,
      predictionTimePolicy: definition.predictionTimePolicy,
      featureCutoffPolicy: definition.featureCutoffPolicy,
      featureRefs: definition.featureRefs,
      processSignalWindows: definition.processSignalWindows,
      labelRefs: definition.labelRefs,
      maxLabelDelayHours: definition.maxLabelDelayHours,
      minimumConfidence: definition.minimumConfidence,
      splitPolicy: definition.splitPolicy,
    },
    selection: {
      freezeAt: snapshot.freezeAt,
      plantId: snapshot.plantId,
      lineIds: snapshot.lineIds,
      ruleVersionIds: snapshot.ruleVersionIds,
      approvedShadowRunsOnly: true,
      activeReviewAtFreezeOnly: true,
      excludeLowConfidence: snapshot.excludeLowConfidence,
    },
    phaseBoundary: {
      deliveryState: 'MANIFEST_ONLY',
      materializationState: 'NOT_STARTED',
      artifactUri: null,
      icebergReady: false,
      mlflowRegistered: false,
      modelTrained: false,
    },
    counts: {
      total: samples.length,
      included: includedCount,
      excluded: samples.length - includedCount,
      exclusionSummary,
    },
    samples,
  };
  snapshot.state = 'MANIFEST_READY';
  snapshot.revision = 3;
  snapshot.manifestSchemaVersion = manifest.schemaVersion;
  snapshot.manifestChecksum = sha256(manifest);
  snapshot.manifest = manifest;
  snapshot.includedCount = includedCount;
  snapshot.excludedCount = samples.length - includedCount;
  snapshot.exclusionSummary = exclusionSummary;
  snapshot.completedAt = FIXED_TIME;
  state.pendingDatasetSnapshotIds.delete(snapshot.id);
}

function materializationSchema(snapshot) {
  return {
    schemaVersion: DATASET_ARTIFACT_SCHEMA_VERSION,
    fields: [
      { name: 'snapshot_id', type: 'string', nullable: false },
      { name: 'review_id', type: 'string', nullable: false },
      { name: 'shadow_run_id', type: 'string', nullable: false },
      { name: 'batch_id', type: 'string', nullable: false },
      { name: 'batch_no', type: 'string', nullable: false },
      { name: 'line_id', type: 'string', nullable: false },
      { name: 'prediction_time', type: 'timestamp[us, tz=UTC]', nullable: false },
      { name: 'feature_cutoff', type: 'timestamp[us, tz=UTC]', nullable: false },
      { name: 'label_available_at', type: 'timestamp[us, tz=UTC]', nullable: false },
      { name: 'confidence', type: 'decimal128(7, 6)', nullable: false },
      { name: 'split_key', type: 'string', nullable: false },
    ],
    selectedFeatureRefs: clone(snapshot.manifest.definition.featureRefs),
    selectedLabelRefs: clone(snapshot.manifest.definition.labelRefs),
  };
}

function completeDatasetMaterialization(state, materialization) {
  const snapshot = state.datasetSnapshots.find((item) => item.id === materialization.snapshotId);
  if (!snapshot?.manifest) return;
  const includedSamples = snapshot.manifest.samples
    .filter((sample) => sample.included)
    .sort((left, right) => left.lineId.localeCompare(right.lineId)
      || left.predictionTime.localeCompare(right.predictionTime)
      || left.batchId.localeCompare(right.batchId)
      || left.reviewId.localeCompare(right.reviewId));
  const contentSha256 = sha256({
    schemaVersion: DATASET_ARTIFACT_SCHEMA_VERSION,
    snapshotId: snapshot.id,
    manifestChecksum: snapshot.manifestChecksum,
    samples: includedSamples,
  });
  const versionSegment = DATASET_MATERIALIZER_VERSION.replace('/', '-');
  const objectKey = `datasets/${snapshot.id}/${snapshot.manifestChecksum}/${versionSegment}/${contentSha256}.parquet`;
  const objectVersionId = stableUuid(`simulated-object-version:${objectKey}`);
  materialization.state = 'READY';
  materialization.revision += 1;
  materialization.completedAt = FIXED_TIME;
  materialization.artifactUri = `s3://${DATASET_OBJECT_BUCKET}/${objectKey}?versionId=${objectVersionId}`;
  materialization.objectBucket = DATASET_OBJECT_BUCKET;
  materialization.objectKey = objectKey;
  materialization.contentSha256 = contentSha256;
  materialization.byteSize = 4096 + includedSamples.length * 1024;
  materialization.rowCount = includedSamples.length;
  materialization.schema = materializationSchema(snapshot);
  materialization.artifactMetadata = {
    artifactSchemaVersion: DATASET_ARTIFACT_SCHEMA_VERSION,
    materializerVersion: DATASET_MATERIALIZER_VERSION,
    manifestSchemaVersion: snapshot.manifestSchemaVersion,
    manifestChecksum: snapshot.manifestChecksum,
    definitionChecksum: snapshot.definitionChecksum,
    rowOrder: 'line_id,prediction_time,batch_id,review_id',
    compression: 'zstd:3',
    sourcePayloadIncluded: false,
    excludedSamplesIncluded: false,
    icebergReady: false,
    mlflowRegistered: false,
    modelTrained: false,
    objectVersionId,
    objectContentVerified: true,
    simulationOnly: true,
  };
  materialization.failureCode = null;
  materialization.failureDetail = null;
  state.pendingDatasetMaterializationIds.delete(materialization.id);
}

function progressDatasetMaterialization(state, materialization) {
  if (!state.pendingDatasetMaterializationIds.has(materialization.id)) return;
  if (materialization.state === 'QUEUED') {
    materialization.state = 'WRITING';
    materialization.revision += 1;
    materialization.startedAt = FIXED_TIME;
    materialization.attemptCount += 1;
    return;
  }
  if (materialization.state === 'WRITING') completeDatasetMaterialization(state, materialization);
}

function latestDatasetCatalogPublication(state, materializationId) {
  return state.datasetCatalogPublications
    .filter((item) => item.materializationId === materializationId
      && item.catalogName === DATASET_CATALOG_NAME
      && item.publisherVersion === DATASET_CATALOG_PUBLISHER_VERSION)
    .sort((left, right) => right.createdAt.localeCompare(left.createdAt)
      || right.id.localeCompare(left.id))[0] || null;
}

function completeDatasetCatalogPublication(state, publication) {
  publication.state = 'READY';
  publication.revision += 1;
  publication.completedAt = FIXED_TIME;
  publication.verifiedRowCount = publication.sourceRowCount;
  publication.semanticChecksum = sha256({
    tableIdentifier: publication.tableIdentifier,
    icebergSnapshotId: publication.icebergSnapshotId,
    sourceContentSha256: publication.sourceContentSha256,
    sourceObjectVersionId: publication.sourceObjectVersionId,
    rowCount: publication.sourceRowCount,
  });
  publication.catalogMetadata = {
    catalogSnapshotVerified: true,
    sourceVersionVerified: true,
    simulationOnly: true,
  };
  publication.failureCode = null;
  publication.failureDetail = null;
  state.pendingDatasetCatalogPublicationIds.delete(publication.id);
}

function progressDatasetCatalogPublication(state, publication) {
  if (!state.pendingDatasetCatalogPublicationIds.has(publication.id)) return;
  if (publication.state === 'QUEUED') {
    publication.state = 'COMMITTING';
    publication.revision += 1;
    publication.startedAt = FIXED_TIME;
    publication.attemptCount += 1;
    return;
  }
  if (publication.state === 'COMMITTING') {
    publication.state = 'VERIFYING';
    publication.revision += 1;
    publication.icebergSnapshotId = '9223372036854775001';
    publication.icebergMetadataLocation = `s3://bpi-iceberg/warehouse/${publication.catalogNamespace}/${publication.tableName}/metadata/v1.metadata.json`;
    publication.icebergSchemaId = 0;
    publication.icebergPartitionSpecId = 0;
    return;
  }
  if (publication.state === 'VERIFYING') completeDatasetCatalogPublication(state, publication);
}

function latestDatasetRetentionArchive(state, publicationId) {
  return state.datasetRetentionArchives
    .filter((item) => item.catalogPublicationId === publicationId
      && item.archiverVersion === DATASET_RETENTION_ARCHIVER_VERSION)
    .sort((left, right) => right.createdAt.localeCompare(left.createdAt)
      || right.id.localeCompare(left.id))[0] || null;
}

function completeDatasetRetentionArchive(state, archive) {
  archive.state = 'LOCKED';
  archive.revision += 1;
  archive.completedAt = FIXED_TIME;
  archive.verifiedRowCount = archive.catalogVerifiedRowCount;
  archive.verifiedSemanticChecksum = archive.catalogSemanticChecksum;
  archive.archiveMetadata = {
    objectLockVerified: true,
    recoveryVerified: true,
    sourceVersionVerified: true,
    manifestVersionVerified: true,
    simulationOnly: true,
    mlflowRegistered: false,
    modelTrained: false,
  };
  archive.failureCode = null;
  archive.failureDetail = null;
  state.pendingDatasetRetentionArchiveIds.delete(archive.id);
}

function progressDatasetRetentionArchive(state, archive) {
  if (!state.pendingDatasetRetentionArchiveIds.has(archive.id)) return;
  if (archive.state === 'QUEUED') {
    archive.state = 'ARCHIVING';
    archive.revision += 1;
    archive.startedAt = FIXED_TIME;
    archive.attemptCount += 1;
    archive.retentionMode = 'GOVERNANCE';
    archive.retainUntil = '2026-07-13T08:00:00.000Z';
    archive.legalHoldEnabled = false;
    return;
  }
  if (archive.state === 'ARCHIVING') {
    const tenantHash = sha256(archive.tenantId).slice(0, 16);
    archive.state = 'VERIFYING';
    archive.revision += 1;
    archive.archiveBucket = DATASET_RECOVERY_BUCKET;
    archive.archivePrefix = `archives/tenant_${tenantHash}/${archive.catalogPublicationId}/${archive.id}`;
    archive.sourceArchiveObjectKey = `${archive.archivePrefix}/source.parquet`;
    archive.sourceArchiveVersionId = stableUuid(`retained-source:${archive.id}`);
    archive.archiveManifestObjectKey = `${archive.archivePrefix}/recovery-manifest.json`;
    archive.archiveManifestVersionId = stableUuid(`retained-manifest:${archive.id}`);
    archive.archiveManifestSha256 = sha256({
      archiveId: archive.id,
      publicationId: archive.catalogPublicationId,
      sourceContentSha256: archive.sourceContentSha256,
      sourceArchiveVersionId: archive.sourceArchiveVersionId,
      catalogSemanticChecksum: archive.catalogSemanticChecksum,
      retentionMode: archive.retentionMode,
      retainUntil: archive.retainUntil,
    });
    archive.archiveObjectCount = 2;
    archive.archiveTotalBytes = archive.sourceByteSize + 2048;
    return;
  }
  if (archive.state === 'VERIFYING') completeDatasetRetentionArchive(state, archive);
}

function latestDatasetMlflowRegistration(state, archiveId) {
  return state.datasetMlflowRegistrations
    .filter((item) => item.retentionArchiveId === archiveId
      && item.registrarVersion === DATASET_MLFLOW_REGISTRAR_VERSION
      && item.trackingProfile === DATASET_MLFLOW_TRACKING_PROFILE)
    .sort((left, right) => right.createdAt.localeCompare(left.createdAt)
    || right.id.localeCompare(left.id))[0] || null;
}

function latestDatasetTrainingReadiness(state, registrationId) {
  return state.datasetTrainingReadinessAssessments
    .filter((item) => item.mlflowRegistrationId === registrationId
      && item.objectiveCode === DATASET_TRAINING_OBJECTIVE
      && item.policyVersion === DATASET_TRAINING_POLICY)
    .sort((left, right) => right.assessmentSequence - left.assessmentSequence
      || right.id.localeCompare(left.id))[0] || null;
}

function buildDatasetTrainingReadiness(state, registration, sequence, reason) {
  const snapshot = state.datasetSnapshots.find((item) => item.id === registration.snapshotId);
  const definition = state.datasetDefinitions.find((item) => item.id === registration.datasetId);
  const samples = snapshot?.manifest?.samples || [];
  const included = samples.filter((sample) => sample.included);
  const excluded = samples.filter((sample) => !sample.included);
  const featureRefs = definition?.featureRefs || [];
  const labelRefs = definition?.labelRefs || [];
  const requiredContext = [
    'batch.material_code', 'batch.stage_code', 'rule.version_id',
    'topology.version_id', 'point_catalog.snapshot_id',
  ];
  const missingContext = requiredContext.filter((item) => !featureRefs.includes(item));
  const signalFeatures = featureRefs.filter((item) => /^(signal\.|telemetry\.|process\.window\.|parameter\.window\.)/.test(item));
  const accepted = included.filter((sample) =>
    (sample.labelPayload || sample.labels)?.['review.boundary_acceptance']?.start === true).length;
  const rejected = included.filter((sample) =>
    (sample.labelPayload || sample.labels)?.['review.boundary_acceptance']?.start === false).length;
  const missingLabels = included.length - accepted - rejected;
  const distinctBatches = new Set(included.map((sample) => sample.batchId)).size;
  const productionDays = new Set(included.map((sample) => sample.predictionTime.slice(0, 10))).size;
  const splitGroups = new Set(included.map((sample) => sample.splitKey)).size;
  const leakageRows = included.filter((sample) =>
    Date.parse(sample.featureCutoff || sample.featureCutoffTime) > Date.parse(sample.predictionTime)
    || Date.parse(sample.labelAvailableAt) < Date.parse(sample.predictionTime)).length;
  const expectedProcessSignalWindowFactCount =
    included.length * (definition?.processSignalWindows?.length || 0);
  const processSignalWindowFacts = included.flatMap((sample) => {
    const facts = (sample.sourcePayload || sample.source)?.processSignalWindows;
    return Array.isArray(facts) ? facts : [];
  });
  const readyProcessSignalWindowFactCount = processSignalWindowFacts
    .filter((fact) => fact.state === 'READY').length;
  const blockedProcessSignalWindowFactCount = processSignalWindowFacts
    .filter((fact) => fact.state !== 'READY').length;
  const missingProcessSignalWindowFactCount = Math.max(
    0,
    expectedProcessSignalWindowFactCount - processSignalWindowFacts.length,
  );
  const excludedRatio = samples.length ? excluded.length / samples.length : 1;
  const inputVerified = registration.state === 'REGISTERED'
    && registration.registrationMetadata?.datasetInputVerified === true
    && registration.registrationMetadata?.lineageVerified === true
    && registration.registrationMetadata?.sourceFactsVerified === true;
  const unresolvedCritical = state.dataQualityIncidents.filter((incident) =>
    incident.severity === 'CRITICAL' && incident.state !== 'RESOLVED').length;
  const observedMetrics = {
    registrationState: registration.state,
    registrationRevision: registration.revision,
    datasetInputVerified: registration.registrationMetadata?.datasetInputVerified === true,
    lineageVerified: registration.registrationMetadata?.lineageVerified === true,
    sourceFactsVerified: registration.registrationMetadata?.sourceFactsVerified === true,
    sourceRowCount: registration.sourceRowCount,
    snapshotIncludedCount: snapshot?.includedCount || 0,
    snapshotExcludedCount: snapshot?.excludedCount || 0,
    persistedSampleCount: samples.length,
    includedSampleCount: included.length,
    excludedSampleCount: excluded.length,
    excludedRatio,
    distinctBatchCount: distinctBatches,
    distinctProductionDayCount: productionDays,
    productionSplitGroupCount: splitGroups,
    leakageRowCount: leakageRows,
    featureRefs: clone(featureRefs),
    labelRefs: clone(labelRefs),
    missingContextFeatureRefs: missingContext,
    signalWindowFeatureRefs: signalFeatures,
    processWindowExpectedFactCount: expectedProcessSignalWindowFactCount,
    processWindowReadyFactCount: readyProcessSignalWindowFactCount,
    processWindowBlockedFactCount: blockedProcessSignalWindowFactCount,
    processWindowMissingFactCount: missingProcessSignalWindowFactCount,
    startAcceptedLabelCount: accepted,
    startRejectedLabelCount: rejected,
    startLabelMissingCount: missingLabels,
    distinctShadowRunCount: included.length ? 1 : 0,
    approvedShadowRunCount: included.length ? 1 : 0,
    shadowRunDurationGateFailureCount: 0,
    maximumContinuousShadowRunDays: included.length ? 8 : 0,
    pointCatalogSnapshotCount: included.length ? 1 : 0,
    readyPointCatalogSnapshotCount: included.length ? 1 : 0,
    unresolvedCriticalIncidentCount: unresolvedCritical,
  };
  const requiredThresholds = {
    requiredRegistrationState: 'REGISTERED',
    requiredDatasetInputEvidence: true,
    requiredPredictionTimePolicy: DATASET_PREDICTION_TIME_POLICY,
    requiredFeatureCutoffPolicy: DATASET_FEATURE_CUTOFF_POLICY,
    requiredSplitPolicy: DATASET_SPLIT_POLICY,
    requiredContextFeatureRefs: requiredContext,
    requiredLabelRef: 'review.boundary_acceptance',
    minimumSignalWindowFeatureRefs: 2,
    minimumIncludedSamples: 200,
    minimumDistinctBatches: 200,
    minimumProductionDays: 7,
    minimumProductionSplitGroups: 2,
    maximumExcludedRatio: 0.2,
    minimumStartAcceptedLabels: 100,
    minimumStartRejectedLabels: 10,
    minimumContinuousShadowRunDays: 7,
    maximumLeakageRows: 0,
    maximumUnresolvedCriticalIncidents: 0,
  };
  const gates = [];
  const gate = (code, expected, observed, detail, passed) => gates.push({
    code, expected, observed, detail, passed,
  });
  gate('MLFLOW_DATASET_INPUT_NOT_VERIFIED', true, inputVerified, 'MLflow 数据输入、血缘和来源事实必须全部复验。', inputVerified);
  gate('DATASET_POLICY_MISMATCH', `${DATASET_PREDICTION_TIME_POLICY}|${DATASET_FEATURE_CUTOFF_POLICY}|${DATASET_SPLIT_POLICY}`, `${definition?.predictionTimePolicy}|${definition?.featureCutoffPolicy}|${definition?.splitPolicy}`, '数据集必须使用预测时点截止和生产时间切分。', definition?.predictionTimePolicy === DATASET_PREDICTION_TIME_POLICY && definition?.featureCutoffPolicy === DATASET_FEATURE_CUTOFF_POLICY && definition?.splitPolicy === DATASET_SPLIT_POLICY);
  gate('SOURCE_ROW_RECONCILIATION_FAILED', included.length, registration.sourceRowCount, 'MLflow 来源行数必须等于清单纳入行数。', registration.sourceRowCount === included.length && snapshot?.includedCount === included.length && snapshot?.excludedCount === excluded.length);
  gate('POINT_IN_TIME_LEAKAGE_DETECTED', 0, leakageRows, '特征截止和标签可用时间不能泄漏未来事实。', leakageRows === 0);
  gate('REQUIRED_CONTEXT_FEATURES_MISSING', [], missingContext, '缺少物料、工段或版本血缘上下文。', missingContext.length === 0);
  gate('PROCESS_SIGNAL_WINDOWS_MISSING', 2, signalFeatures.length, '至少需要两组边界前过程信号窗口，只有标识符不能训练工艺模型。', signalFeatures.length >= 2);
  gate(
    'PROCESS_SIGNAL_WINDOW_FACTS_INCOMPLETE',
    expectedProcessSignalWindowFactCount,
    {
      ready: readyProcessSignalWindowFactCount,
      blocked: blockedProcessSignalWindowFactCount,
      missing: missingProcessSignalWindowFactCount,
    },
    '每个纳入样本的每组工艺窗口都必须形成 READY 的不可变事实。',
    expectedProcessSignalWindowFactCount > 0
      && readyProcessSignalWindowFactCount === expectedProcessSignalWindowFactCount
      && blockedProcessSignalWindowFactCount === 0
      && missingProcessSignalWindowFactCount === 0,
  );
  gate('BOUNDARY_REVIEW_LABEL_MISSING', 'review.boundary_acceptance', labelRefs, '需要人工复核的起始边界标签。', labelRefs.includes('review.boundary_acceptance'));
  gate('INCLUDED_SAMPLE_COUNT_BELOW_MINIMUM', 200, included.length, '纳入样本不足。', included.length >= 200);
  gate('DISTINCT_BATCH_COUNT_BELOW_MINIMUM', 200, distinctBatches, '独立批次数不足。', distinctBatches >= 200);
  gate('PRODUCTION_DAY_COVERAGE_BELOW_MINIMUM', 7, productionDays, '生产日期覆盖不足。', productionDays >= 7);
  gate('PRODUCTION_SPLIT_GROUPS_BELOW_MINIMUM', 2, splitGroups, '至少需要两个生产时间切分组。', splitGroups >= 2);
  gate('EXCLUDED_RATIO_ABOVE_MAXIMUM', 0.2, excludedRatio, '样本排除比例过高。', excludedRatio <= 0.2);
  gate('START_ACCEPTED_LABEL_COUNT_BELOW_MINIMUM', 100, accepted, '起始边界接受标签不足。', accepted >= 100);
  gate('START_REJECTED_LABEL_COUNT_BELOW_MINIMUM', 10, rejected, '起始边界拒绝标签不足。', rejected >= 10);
  gate('START_LABEL_VALUES_MISSING', 0, missingLabels, '每个纳入样本都必须有起始边界复核标签。', missingLabels === 0);
  gate('SHADOW_RUN_SOURCE_NOT_APPROVED', observedMetrics.distinctShadowRunCount, observedMetrics.approvedShadowRunCount, '所有来源必须是已批准影子运行。', observedMetrics.distinctShadowRunCount > 0 && observedMetrics.approvedShadowRunCount === observedMetrics.distinctShadowRunCount);
  gate('SHADOW_RUN_DURATION_GATE_FAILED', 7, observedMetrics.maximumContinuousShadowRunDays, '影子运行必须满足 7-14 天现场窗口。', observedMetrics.maximumContinuousShadowRunDays >= 7);
  gate('POINT_CATALOG_READINESS_UNPROVEN', observedMetrics.pointCatalogSnapshotCount, observedMetrics.readyPointCatalogSnapshotCount, '固定点位目录必须保留就绪证据。', observedMetrics.pointCatalogSnapshotCount > 0 && observedMetrics.readyPointCatalogSnapshotCount === observedMetrics.pointCatalogSnapshotCount);
  gate('UNRESOLVED_CRITICAL_DATA_QUALITY', 0, unresolvedCritical, '来源窗口内不能存在未解决关键数据质量事件。', unresolvedCritical === 0);
  const blockerCodes = gates.filter((item) => !item.passed).map((item) => item.code);
  const phaseBoundary = {
    assessmentOnly: true,
    trainingStarted: false,
    modelCreated: false,
    modelRegistered: false,
    onlineInferenceEnabled: false,
    productionActivationAllowed: false,
  };
  const assessmentChecksum = sha256({
    registrationId: registration.id,
    snapshotId: registration.snapshotId,
    sourceRegistrationRevision: registration.revision,
    objectiveCode: DATASET_TRAINING_OBJECTIVE,
    policyVersion: DATASET_TRAINING_POLICY,
    requiredThresholds,
    observedMetrics,
    gateResults: gates,
    phaseBoundary,
  });
  return {
    id: stableUuid(`dataset-training-readiness:${registration.id}:${sequence}`),
    mlflowRegistrationId: registration.id,
    sourceSnapshotId: registration.snapshotId,
    datasetId: registration.datasetId,
    datasetCode: registration.datasetCode,
    datasetVersion: registration.datasetVersion,
    tenantId: registration.tenantId,
    plantId: registration.plantId,
    lineIds: clone(registration.lineIds),
    objectiveCode: DATASET_TRAINING_OBJECTIVE,
    policyVersion: DATASET_TRAINING_POLICY,
    assessmentSequence: sequence,
    state: blockerCodes.length ? 'BLOCKED' : 'ELIGIBLE',
    revision: 1,
    sourceRegistrationRevision: registration.revision,
    manifestChecksum: registration.manifestChecksum,
    datasetDigest: registration.datasetDigest,
    requiredThresholds,
    observedMetrics,
    gateResults: gates,
    blockerCodes,
    phaseBoundary,
    assessmentChecksum,
    assessedBy: 'simulated.data.engineer',
    assessmentReason: reason,
    assessedAt: FIXED_TIME,
  };
}

function completeDatasetMlflowRegistration(state, registration) {
  const experimentId = String(parseInt(sha256(registration.experimentName).slice(0, 12), 16));
  const runId = sha256(`mlflow-dataset-run:${registration.id}`).slice(0, 32);
  const datasetSource = `s3://${registration.archiveBucket}/${registration.sourceArchiveObjectKey}`
    + `?versionId=${registration.sourceArchiveVersionId}`;
  registration.state = 'REGISTERED';
  registration.revision += 1;
  registration.completedAt = FIXED_TIME;
  registration.mlflowExperimentId = experimentId;
  registration.mlflowRunId = runId;
  registration.mlflowArtifactUri = `mlflow-artifacts:/${experimentId}/${runId}/artifacts`;
  registration.mlflowDatasetSource = datasetSource;
  registration.registrationMetadata = {
    datasetInputVerified: true,
    lineageVerified: true,
    sourceFactsVerified: true,
    modelTrained: false,
    modelRegistered: false,
    onlineInferenceEnabled: false,
    productionActivationAllowed: false,
    simulationOnly: true,
  };
  registration.failureCode = null;
  registration.failureDetail = null;
  state.pendingDatasetMlflowRegistrationIds.delete(registration.id);
}

function progressDatasetMlflowRegistration(state, registration) {
  if (!state.pendingDatasetMlflowRegistrationIds.has(registration.id)) return;
  if (registration.state === 'QUEUED') {
    registration.state = 'REGISTERING';
    registration.revision += 1;
    registration.startedAt = FIXED_TIME;
    registration.attemptCount += 1;
    return;
  }
  if (registration.state === 'REGISTERING') {
    completeDatasetMlflowRegistration(state, registration);
  }
}

function calibrationEffectiveness(calibration, now = Date.now()) {
  if (calibration.state !== 'APPROVED') return calibration.state;
  if (now < Date.parse(calibration.validFrom)) return 'NOT_YET_EFFECTIVE';
  if (now >= Date.parse(calibration.validUntil)) return 'EXPIRED';
  return 'EFFECTIVE';
}

function sourceSequenceEvidenceKey(snapshot, point) {
  return [snapshot.source, snapshot.sourceInstance, snapshot.plantId, snapshot.lineId,
    point.productId, point.deviceId, point.sourceSequenceBindingFingerprint].join('|');
}

function sourceSequenceBindingReady(point) {
  return point.sourceSequenceEnabled === true
    && point.sourceSequenceRequired === true
    && ['DEVICE', 'GATEWAY'].includes(point.sourceSequenceOrigin)
    && SOURCE_SEQUENCE_FINGERPRINT.test(point.sourceSequenceBindingFingerprint || '');
}

function refreshPointCatalogReadiness(state) {
  const now = Date.now();
  state.pointCalibrations.forEach((calibration) => {
    calibration.effectivenessStatus = calibrationEffectiveness(calibration, now);
    calibration.effective = calibration.effectivenessStatus === 'EFFECTIVE';
  });
  if (!state.pointCatalog) return null;
  const observedAt = Date.parse(state.pointCatalog.snapshot.observedAt);
  state.pointCatalog.points.forEach((point) => {
    if (!point.sourceCalibrationStatus) {
      point.sourceCalibrationStatus = point.calibrationStatus || (point.calibrationVersion ? 'UNVERIFIED' : 'MISSING');
    }
    const evidence = state.pointCalibrations
      .filter((calibration) => calibration.plantId === point.plantId
        && calibration.lineId === point.lineId
        && calibration.productId === point.productId
        && calibration.deviceId === point.deviceId
        && calibration.propertyId === point.propertyId
        && calibration.calibrationVersion === point.calibrationVersion
        && calibration.effectivenessStatus === 'EFFECTIVE'
        && Date.parse(calibration.validFrom) <= observedAt)
      .sort((left, right) => Date.parse(right.decidedAt || '') - Date.parse(left.decidedAt || ''))[0];
    point.calibrationStatus = evidence ? 'VERIFIED' : point.calibrationVersion ? 'UNVERIFIED' : 'MISSING';
    point.calibrationEvidenceId = evidence?.id || null;
    point.calibrationValidUntil = evidence?.validUntil || null;
    const sequenceBindingReady = sourceSequenceBindingReady(point);
    const sequenceEvidence = sequenceBindingReady
      ? state.sourceSequenceEvidence.get(sourceSequenceEvidenceKey(state.pointCatalog.snapshot, point))
      : null;
    const evidenceExpired = sequenceEvidence
      && ['PENDING', 'QUALIFIED'].includes(sequenceEvidence.status)
      && Date.parse(sequenceEvidence.validUntil || '') <= now;
    point.sourceSequenceEvidenceStatus = sequenceBindingReady
      ? evidenceExpired ? 'EXPIRED' : sequenceEvidence?.status || 'MISSING'
      : 'DISABLED';
    point.sourceSequenceQualified = Boolean(sequenceBindingReady
      && sequenceEvidence?.status === 'QUALIFIED'
      && sequenceEvidence.sequenceOrigin === point.sourceSequenceOrigin
      && Date.parse(sequenceEvidence.observedAt) >= observedAt
      && Date.parse(sequenceEvidence.validUntil) > observedAt
      && Date.parse(sequenceEvidence.validUntil) > now);
    point.sourceSequenceEpoch = sequenceEvidence?.sourceEpoch ?? null;
    point.sourceSequenceFirst = sequenceEvidence?.firstSequence ?? null;
    point.sourceSequenceLast = sequenceEvidence?.lastSequence ?? null;
    point.sourceSequenceObservationCount = sequenceEvidence?.observationCount ?? null;
    point.sourceSequenceFirstObservedAt = sequenceEvidence?.firstObservedAt || null;
    point.sourceSequenceLastObservedAt = sequenceEvidence?.lastObservedAt || null;
    point.sourceSequenceValidUntil = sequenceEvidence?.validUntil || null;
    point.sourceSequenceEvidenceEventId = sequenceEvidence?.eventId || null;
    point.sourceSequenceEvidenceRevision = sequenceEvidence?.revision ?? null;
    const readinessIssues = [];
    if (!point.registered) readinessIssues.push('DEVICE_NOT_REGISTERED');
    if (point.deviceState !== 'ACTIVE') readinessIssues.push('DEVICE_NOT_ACTIVE');
    if (!point.propertyPresent) readinessIssues.push('PROPERTY_NOT_AVAILABLE');
    if (!point.unit) readinessIssues.push('UNIT_MISSING');
    if (!point.calibrationVersion || point.calibrationStatus !== 'VERIFIED') readinessIssues.push('CALIBRATION_NOT_VERIFIED');
    if (!sequenceBindingReady) readinessIssues.push('SOURCE_SEQUENCE_DISABLED');
    else if (!point.sourceSequenceQualified) {
      if (point.sourceSequenceEvidenceStatus === 'MISSING') readinessIssues.push('SOURCE_SEQUENCE_EVIDENCE_MISSING');
      else if (point.sourceSequenceEvidenceStatus === 'EXPIRED') readinessIssues.push('SOURCE_SEQUENCE_EVIDENCE_EXPIRED');
      else readinessIssues.push('SOURCE_SEQUENCE_EVIDENCE_NOT_QUALIFIED');
    }
    point.ready = readinessIssues.length === 0;
    point.readinessIssues = readinessIssues;
  });
  state.pointCatalog.snapshot.readyPointCount = state.pointCatalog.points.filter((point) => point.ready).length;
  return state.pointCatalog;
}

function compareVersions(objectType, base, target, baseContent, targetContent) {
  const changes = [];
  let changeCount = 0;
  const missing = Symbol('missing');
  const value = (item) => item === missing ? null : clone(item);
  const walk = (path, before, after) => {
    if (before !== missing && after !== missing
        && JSON.stringify(before) === JSON.stringify(after)) return;
    const beforeObject = before !== null && before !== missing && !Array.isArray(before) && typeof before === 'object';
    const afterObject = after !== null && after !== missing && !Array.isArray(after) && typeof after === 'object';
    if (beforeObject && afterObject) {
      const names = [...new Set([...Object.keys(before), ...Object.keys(after)])].sort();
      names.forEach((name) => walk(
        `${path}/${name.replaceAll('~', '~0').replaceAll('/', '~1')}`,
        Object.prototype.hasOwnProperty.call(before, name) ? before[name] : missing,
        Object.prototype.hasOwnProperty.call(after, name) ? after[name] : missing,
      ));
      return;
    }
    if (Array.isArray(before) && Array.isArray(after)) {
      const size = Math.max(before.length, after.length);
      for (let index = 0; index < size; index += 1) {
        walk(`${path}/${index}`, index < before.length ? before[index] : missing,
          index < after.length ? after[index] : missing);
      }
      return;
    }
    changeCount += 1;
    if (changes.length >= 500) return;
    changes.push({
      path: path || '/',
      changeType: before === missing ? 'ADDED' : after === missing ? 'REMOVED' : 'CHANGED',
      beforeValue: value(before),
      afterValue: value(after),
    });
  };
  walk('', baseContent, targetContent);
  const reference = (item) => ({
    id: item.id, code: item.code, version: item.version, state: item.state, checksum: item.checksum,
  });
  return {
    objectType,
    base: reference(base),
    target: reference(target),
    identical: changeCount === 0,
    changeCount,
    truncated: changeCount > changes.length,
    changes,
  };
}

function topologyValidation(definition, pointCatalog) {
  const errors = [];
  const warnings = [];
  const bindings = Array.isArray(definition?.bindings) ? definition.bindings : [];
  if (!bindings.length) {
    errors.push({ code: 'BINDINGS_REQUIRED', path: '/bindings', severity: 'ERROR', message: 'At least one JetLinks point binding is required.' });
  }
  bindings.forEach((binding, index) => {
    ['signal', 'productId', 'deviceId', 'propertyId', 'calibrationVersion'].forEach((field) => {
      if (!binding[field]) errors.push({ code: 'FIELD_REQUIRED', path: `/bindings/${index}/${field}`, severity: 'ERROR', message: `${field} is required.` });
    });
    if (!binding.expectedUnit && !binding.unit) {
      errors.push({ code: 'BINDING_UNIT_REQUIRED', path: `/bindings/${index}/expectedUnit`, severity: 'ERROR', message: 'A binding must declare expectedUnit or unit.' });
    }
    if (!pointCatalog) return;
    const point = pointCatalog?.points.find((item) => item.productId === binding.productId
      && item.deviceId === binding.deviceId && item.propertyId === binding.propertyId);
    if (!point) {
      errors.push({ code: 'POINT_CATALOG_BINDING_NOT_FOUND', path: `/bindings/${index}`, severity: 'ERROR', message: 'The binding does not exist in the current point catalog snapshot.' });
      return;
    }
    if (!point.registered) errors.push({ code: 'POINT_DEVICE_NOT_REGISTERED', path: `/bindings/${index}/deviceId`, severity: 'ERROR', message: 'The bound device is not registered.' });
    if (point.deviceState !== 'ACTIVE') errors.push({ code: 'POINT_DEVICE_NOT_ACTIVE', path: `/bindings/${index}/deviceId`, severity: 'ERROR', message: `The bound device state is ${point.deviceState}.` });
    if (!point.propertyPresent) errors.push({ code: 'POINT_PROPERTY_NOT_AVAILABLE', path: `/bindings/${index}/propertyId`, severity: 'ERROR', message: 'The bound property is absent from product metadata.' });
    const expectedUnit = binding.expectedUnit || binding.unit;
    if (!point.unit) errors.push({ code: 'POINT_UNIT_MISSING', path: `/bindings/${index}/expectedUnit`, severity: 'ERROR', message: 'The catalog point has no source unit.' });
    else if (expectedUnit && point.unit.toLowerCase() !== String(expectedUnit).toLowerCase()) errors.push({ code: 'POINT_UNIT_MISMATCH', path: `/bindings/${index}/expectedUnit`, severity: 'ERROR', message: 'Expected and source units do not match.' });
    if (point.calibrationStatus !== 'VERIFIED' || point.calibrationVersion !== binding.calibrationVersion) errors.push({ code: 'POINT_CALIBRATION_NOT_VERIFIED', path: `/bindings/${index}/calibrationVersion`, severity: 'ERROR', message: 'The requested calibration version is not verified.' });
    if (!point.sourceSequenceQualified) errors.push({ code: 'POINT_SOURCE_SEQUENCE_DISABLED', path: `/bindings/${index}`, severity: 'ERROR', message: 'Qualified device or gateway source sequence evidence is required for replay-safe topology binding.' });
  });
  if (!pointCatalog) errors.push({ code: 'POINT_CATALOG_SNAPSHOT_MISSING', path: '/bindings', severity: 'ERROR', message: 'No point catalog snapshot exists for this scope.' });
  return { errors, warnings };
}

function createHandler(state) {
  return async (req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1');
    const path = url.pathname;
    let ids;

    try {
      if (req.method === 'GET' && path === '/health') {
        return send(res, 200, { status: 'UP', mode: 'phase-1-shadow', externalWrites: false }, 'simulationHealth');
      }
      if (req.method === 'POST' && path === '/__simulation/reset') {
        Object.assign(state, createScenario());
        return send(res, 200, { status: 'RESET' }, 'simulationReset');
      }
      if (req.method === 'POST' && path === '/__simulation/source-sequence-evidence') {
        const operationId = 'simulationSourceSequenceEvidence';
        if (!state.pointCatalog) {
          return send(res, 409, { status: 'POINT_CATALOG_MISSING' }, operationId);
        }
        const body = await readJson(req);
        const status = String(body.status || '').toUpperCase();
        if (!body.productId || !body.deviceId
            || !SOURCE_SEQUENCE_FINGERPRINT.test(body.bindingFingerprint || '')
            || !['DISABLED', 'MISSING', 'PENDING', 'QUALIFIED', 'EXPIRED'].includes(status)) {
          return send(res, 422, { status: 'INVALID_SOURCE_SEQUENCE_EVIDENCE' }, operationId);
        }
        const points = state.pointCatalog.points.filter((point) => point.productId === body.productId
          && point.deviceId === body.deviceId
          && point.sourceSequenceBindingFingerprint === body.bindingFingerprint);
        if (!points.length) {
          return send(res, 404, { status: 'SOURCE_SEQUENCE_BINDING_NOT_FOUND' }, operationId);
        }
        const hasSequence = !['DISABLED', 'MISSING'].includes(status);
        const now = Date.now();
        const observedAt = body.observedAt || new Date(now).toISOString();
        const firstObservedAt = hasSequence
          ? body.firstObservedAt || new Date(now - 60_000).toISOString() : null;
        const lastObservedAt = hasSequence
          ? body.lastObservedAt || new Date(now).toISOString() : null;
        const validUntil = hasSequence
          ? body.validUntil || new Date(now + 86_400_000).toISOString() : null;
        const sequenceOrigin = hasSequence ? body.sequenceOrigin || points[0].sourceSequenceOrigin : null;
        const sourceEpoch = hasSequence ? body.sourceEpoch ?? 1 : null;
        const firstSequence = hasSequence ? body.firstSequence ?? 1 : null;
        const lastSequence = hasSequence ? body.lastSequence ?? 2 : null;
        const observationCount = hasSequence ? body.observationCount ?? 2 : null;
        const timeValues = [observedAt, firstObservedAt, lastObservedAt, validUntil]
          .filter(Boolean).map(Date.parse);
        const validSequence = !hasSequence || (
          ['DEVICE', 'GATEWAY'].includes(sequenceOrigin)
          && sourceEpoch > 0 && firstSequence > 0 && lastSequence >= firstSequence
          && observationCount > 0 && Date.parse(lastObservedAt) >= Date.parse(firstObservedAt)
          && Date.parse(validUntil) > Date.parse(lastObservedAt)
          && (status !== 'QUALIFIED' || (lastSequence > firstSequence && observationCount >= 2))
        );
        if (timeValues.some((value) => !Number.isFinite(value)) || !validSequence) {
          return send(res, 422, { status: 'INVALID_SOURCE_SEQUENCE_EVIDENCE_SHAPE' }, operationId);
        }
        const key = sourceSequenceEvidenceKey(state.pointCatalog.snapshot, points[0]);
        const current = state.sourceSequenceEvidence.get(key);
        const evidence = {
          eventId: body.eventId || `source-sequence-evidence-${sha256({
            key, status, observedAt, sourceEpoch, firstSequence, lastSequence,
          })}`,
          status, sequenceOrigin, sourceEpoch, firstSequence, lastSequence, observationCount,
          firstObservedAt, lastObservedAt, validUntil, observedAt,
          revision: (current?.revision || 0) + 1,
        };
        state.sourceSequenceEvidence.set(key, evidence);
        refreshPointCatalogReadiness(state);
        return send(res, 200, envelope(operationId, {
          status: 'SOURCE_SEQUENCE_EVIDENCE_APPLIED',
          affectedPointCount: points.length,
          evidence,
          catalog: state.pointCatalog,
        }), operationId);
      }
      if (req.method === 'POST' && path === '/__simulation/prepare-shadow-run') {
        const prepared = prepareShadowRunAcceptance(state);
        return send(res, 200, { status: 'SHADOW_RUN_READY', ...prepared }, 'simulationPrepareShadowRun');
      }
      if (req.method === 'POST' && path === '/__simulation/prepare-dataset-manifest') {
        const prepared = prepareDatasetManifestAcceptance(state);
        return send(res, 200, { status: 'DATASET_MANIFEST_SOURCE_READY', ...prepared }, 'simulationPrepareDatasetManifest');
      }
      if (req.method === 'POST' && path === '/__simulation/fail-dataset-materialization') {
        const operationId = 'simulationFailDatasetMaterialization';
        const body = await readJson(req);
        const materialization = state.datasetMaterializations.find((item) => item.id === body.materializationId);
        if (!materialization) {
          return send(res, 404, { status: 'DATASET_MATERIALIZATION_NOT_FOUND' }, operationId);
        }
        if (materialization.state !== 'WRITING') {
          return send(res, 409, { status: 'DATASET_MATERIALIZATION_NOT_WRITING' }, operationId);
        }
        materialization.state = 'FAILED';
        materialization.revision += 1;
        materialization.completedAt = FIXED_TIME;
        materialization.failureCode = String(body.failureCode || 'SIMULATED_OBJECT_STORE_FAILURE');
        materialization.failureDetail = String(body.failureDetail || 'Deterministic simulator failure injection.');
        state.pendingDatasetMaterializationIds.delete(materialization.id);
        return send(res, 200, envelope(operationId, materialization), operationId);
      }
      if (req.method === 'POST' && path === '/__simulation/fail-dataset-catalog-publication') {
        const operationId = 'simulationFailDatasetCatalogPublication';
        const body = await readJson(req);
        const publication = state.datasetCatalogPublications.find((item) => item.id === body.publicationId);
        if (!publication) {
          return send(res, 404, { status: 'DATASET_CATALOG_PUBLICATION_NOT_FOUND' }, operationId);
        }
        if (!['COMMITTING', 'VERIFYING'].includes(publication.state)) {
          return send(res, 409, { status: 'DATASET_CATALOG_PUBLICATION_NOT_ACTIVE' }, operationId);
        }
        publication.state = 'FAILED';
        publication.revision += 1;
        publication.completedAt = FIXED_TIME;
        publication.failureCode = String(body.failureCode || 'SIMULATED_CATALOG_FAILURE');
        publication.failureDetail = String(body.failureDetail || 'Deterministic catalog failure injection.');
        state.pendingDatasetCatalogPublicationIds.delete(publication.id);
        return send(res, 200, envelope(operationId, publication), operationId);
      }
      if (req.method === 'POST' && path === '/__simulation/fail-dataset-retention-archive') {
        const operationId = 'simulationFailDatasetRetentionArchive';
        const body = await readJson(req);
        const archive = state.datasetRetentionArchives.find((item) => item.id === body.archiveId);
        if (!archive) {
          return send(res, 404, { status: 'DATASET_RETENTION_ARCHIVE_NOT_FOUND' }, operationId);
        }
        if (!['ARCHIVING', 'VERIFYING'].includes(archive.state)) {
          return send(res, 409, { status: 'DATASET_RETENTION_ARCHIVE_NOT_ACTIVE' }, operationId);
        }
        archive.state = 'FAILED';
        archive.revision += 1;
        archive.completedAt = FIXED_TIME;
        archive.archiveBucket = null;
        archive.archivePrefix = null;
        archive.sourceArchiveObjectKey = null;
        archive.sourceArchiveVersionId = null;
        archive.archiveManifestObjectKey = null;
        archive.archiveManifestVersionId = null;
        archive.archiveManifestSha256 = null;
        archive.archiveObjectCount = null;
        archive.archiveTotalBytes = null;
        archive.verifiedRowCount = null;
        archive.verifiedSemanticChecksum = null;
        archive.archiveMetadata = null;
        archive.failureCode = String(body.failureCode || 'SIMULATED_OBJECT_LOCK_FAILURE');
        archive.failureDetail = String(body.failureDetail || 'Deterministic Object Lock failure injection.');
        state.pendingDatasetRetentionArchiveIds.delete(archive.id);
        return send(res, 200, envelope(operationId, archive), operationId);
      }
      if (req.method === 'POST' && path === '/__simulation/fail-dataset-mlflow-registration') {
        const operationId = 'simulationFailDatasetMlflowRegistration';
        const body = await readJson(req);
        const registration = state.datasetMlflowRegistrations
          .find((item) => item.id === body.registrationId);
        if (!registration) {
          return send(res, 404, { status: 'DATASET_MLFLOW_REGISTRATION_NOT_FOUND' }, operationId);
        }
        if (registration.state !== 'REGISTERING') {
          return send(res, 409, { status: 'DATASET_MLFLOW_REGISTRATION_NOT_ACTIVE' }, operationId);
        }
        registration.state = 'FAILED';
        registration.revision += 1;
        registration.completedAt = FIXED_TIME;
        registration.mlflowExperimentId = null;
        registration.mlflowRunId = null;
        registration.mlflowArtifactUri = null;
        registration.mlflowDatasetSource = null;
        registration.registrationMetadata = null;
        registration.failureCode = String(body.failureCode || 'SIMULATED_MLFLOW_TRANSPORT_ERROR');
        registration.failureDetail = String(body.failureDetail || 'Deterministic MLflow failure injection.');
        state.pendingDatasetMlflowRegistrationIds.delete(registration.id);
        return send(res, 200, envelope(operationId, registration), operationId);
      }
      if (req.method === 'POST' && path === '/__simulation/prepare-batch-release') {
        const prepared = prepareBatchReleaseAcceptance(state);
        return send(res, 200, { status: 'BATCH_RELEASE_READY', ...prepared }, 'simulationPrepareBatchRelease');
      }
      if (req.method === 'POST' && path === '/__simulation/complete-wms-inbound-reversal') {
        const operationId = 'simulationCompleteWmsInboundReversal';
        const body = await readJson(req);
        const batch = state.batches.find((item) => item.id === body.batchId);
        const task = batch ? state.wmsInboundReversalTasks.get(batch.id) : null;
        if (!batch || !task) {
          return send(res, 404, { status: 'REVERSAL_TASK_NOT_FOUND' }, operationId);
        }
        if (task.state !== 'PENDING_WMS' || batch.state !== 'INBOUND_REVERSING') {
          return send(res, 409, { status: 'REVERSAL_NOT_PENDING_WMS' }, operationId);
        }
        const accepted = String(body.status || 'ACCEPTED').toUpperCase() === 'ACCEPTED';
        task.state = accepted ? 'COMPLETED' : 'FAILED';
        task.revision += 1;
        task.reversalReceiptEventId = stableUuid(`wms-reversal-receipt-${batch.id}-${task.revision}`);
        task.reversalDocumentId = accepted ? body.reversalDocumentId || 'WMS-RED-ADP-E2E-0001' : null;
        task.errorCode = accepted ? null : body.errorCode || 'WMS_REVERSAL_REJECTED';
        task.detail = accepted ? 'WMS durable reversal document persisted.' : body.detail || 'WMS rejected the simulated reversal command.';
        task.observedAt = FIXED_TIME;
        task.outboxStatus = 'PUBLISHED';
        task.deliveryAttemptCount = 1;
        batch.revision += 1;
        batch.state = accepted ? 'INBOUND_REVERSED' : 'INBOUNDED';
        batch.wmsStatus = accepted ? 'REVERSED' : 'REVERSAL_FAILED';
        task.batchRevision = batch.revision;
        const release = state.batchReleases.get(batch.id);
        release.wmsInboundReversal = task;
        const timeline = state.batchEventsById.get(batch.id) || [];
        timeline.push({
          revision: batch.revision,
          action: accepted ? 'WMS_INBOUND_REVERSAL_ACCEPTED' : 'WMS_INBOUND_REVERSAL_REJECTED',
          at: FIXED_TIME,
          actor: 'wms.integration',
          reason: task.detail,
          fromState: 'INBOUND_REVERSING',
          toState: batch.state,
        });
        state.batchEventsById.set(batch.id, timeline);
        return send(res, 200, envelope(operationId, release), operationId);
      }
      if (req.method === 'POST' && path === '/__simulation/fail-rule-publication') {
        if (!['PUBLISHED', 'RETIRED'].includes(state.rule.state)
            || !['PENDING', 'DISPATCHING'].includes(state.rule.publicationStatus)) {
          return send(res, 409, { status: 'NOT_DISPATCHING' }, 'simulationFailRulePublication');
        }
        state.rule.publicationStatus = 'FAILED';
        state.rule.publicationRevision = 11;
        state.rule.publicationAttemptCount = 5;
        state.rule.publicationTotalAttemptCount = 5;
        state.rule.publicationLastError = 'Simulated Kafka broker outage';
        return send(res, 200, { status: 'FAILED' }, 'simulationFailRulePublication');
      }
      if (req.method === 'POST' && path === '/__simulation/complete-rule-publication') {
        if (state.rule.publicationStatus === 'PUBLISHED') {
          return send(res, 200, envelope('simulationCompleteRulePublication', state.rule), 'simulationCompleteRulePublication');
        }
        if (!['PUBLISHED', 'RETIRED'].includes(state.rule.state) || state.rule.publicationStatus === 'FAILED') {
          return send(res, 409, { status: 'NOT_DISPATCHABLE' }, 'simulationCompleteRulePublication');
        }
        state.rule.publicationStatus = 'PUBLISHED';
        state.rule.publicationRevision += 1;
        state.rule.publicationAttemptCount = 1;
        state.rule.publicationTotalAttemptCount += 1;
        state.rule.publicationPublishedAt = '2026-07-12T08:00:01.000Z';
        state.rule.publicationLastError = null;
        return send(res, 200, envelope('simulationCompleteRulePublication', state.rule), 'simulationCompleteRulePublication');
      }
      if (req.method === 'POST' && path === '/__simulation/rule-application') {
        if (state.rule.publicationStatus !== 'PUBLISHED') {
          return send(res, 409, { status: 'PUBLICATION_NOT_CONFIRMED' }, 'simulationRuleApplication');
        }
        const body = await readJson(req);
        if (!['APPLIED', 'REJECTED'].includes(body.status)) {
          return send(res, 422, { status: 'INVALID_APPLICATION_STATUS' }, 'simulationRuleApplication');
        }
        if (state.rule.applicationStatus === 'APPLIED'
            || (state.rule.applicationStatus === 'REJECTED' && body.status === 'REJECTED')) {
          return send(res, 200, envelope('simulationRuleApplication', state.rule), 'simulationRuleApplication');
        }
        state.rule.applicationStatus = body.status;
        state.rule.applicationDeploymentId = body.deploymentId || 'flink-simulator-a';
        state.rule.applicationObservedAt = body.observedAt || '2026-07-12T08:00:02.000Z';
        state.rule.applicationReceivedAt = '2026-07-12T08:00:03.000Z';
        state.rule.applicationErrorCode = body.status === 'REJECTED'
          ? body.errorCode || 'RULE_APPLICATION_REJECTED'
          : null;
        state.rule.applicationErrorDetail = body.status === 'REJECTED'
          ? body.errorDetail || 'Flink rejected the simulated rule update.'
          : null;
        state.rule.publicationRevision += 1;
        return send(res, 200, envelope('simulationRuleApplication', state.rule), 'simulationRuleApplication');
      }
      if (req.method === 'POST' && path === '/__simulation/rule-runtime-readiness') {
        const operationId = 'simulationRuleRuntimeReadiness';
        if (state.rule.publicationStatus !== 'PUBLISHED') {
          return send(res, 409, { status: 'PUBLICATION_NOT_CONFIRMED' }, operationId);
        }
        const body = await readJson(req);
        if (!['READY', 'DEGRADED', 'INACTIVE'].includes(body.status)) {
          return send(res, 422, { status: 'INVALID_RUNTIME_READINESS_STATUS' }, operationId);
        }
        if (body.status !== 'READY' && (!body.reasonCode || !body.detail)) {
          return send(res, 422, { status: 'RUNTIME_READINESS_REASON_REQUIRED' }, operationId);
        }
        const observedAt = body.observedAt || '2026-07-12T08:00:04.000Z';
        const observedAtMs = Date.parse(observedAt);
        if (!Number.isFinite(observedAtMs)) {
          return send(res, 422, { status: 'INVALID_RUNTIME_READINESS_TIME' }, operationId);
        }
        const receipt = {
          eventId: body.eventId || `sha256:${sha256({
            ruleId: state.rule.id,
            status: body.status,
            reasonCode: body.reasonCode || '',
            observedAt,
          })}`,
          status: body.status,
          deploymentId: body.deploymentId || 'flink-simulator-b',
          observedAt,
          receivedAt: body.receivedAt || '2026-07-12T08:00:05.000Z',
          reasonCode: body.status === 'READY' ? null : body.reasonCode,
          detail: body.status === 'READY' ? null : body.detail,
          pointCatalogEventId: body.pointCatalogEventId || null,
          pointCatalogSourceRevision: body.pointCatalogSourceRevision || null,
        };
        const payloadChecksum = sha256(receipt);
        const existing = state.runtimeReadinessReceipts.get(receipt.eventId);
        if (existing) {
          if (existing.payloadChecksum !== payloadChecksum) {
            return send(res, 409, { status: 'RUNTIME_READINESS_REPLAY_CONFLICT' }, operationId);
          }
          return send(res, 200, envelope(operationId, state.rule), operationId);
        }
        state.runtimeReadinessReceipts.set(receipt.eventId, { payloadChecksum, receipt: clone(receipt) });
        const currentObservedAtMs = Date.parse(state.rule.runtimeReadinessObservedAt || '');
        if (Number.isFinite(currentObservedAtMs) && observedAtMs < currentObservedAtMs) {
          return send(res, 200, envelope(operationId, state.rule), operationId);
        }
        if (Number.isFinite(currentObservedAtMs) && observedAtMs === currentObservedAtMs) {
          return send(res, 409, { status: 'RUNTIME_READINESS_TIME_CONFLICT' }, operationId);
        }
        state.rule.runtimeReadinessStatus = receipt.status;
        state.rule.runtimeReadinessDeploymentId = receipt.deploymentId;
        state.rule.runtimeReadinessObservedAt = receipt.observedAt;
        state.rule.runtimeReadinessReceivedAt = receipt.receivedAt;
        state.rule.runtimeReadinessReasonCode = receipt.reasonCode;
        state.rule.runtimeReadinessDetail = receipt.detail;
        state.rule.runtimePointCatalogEventId = receipt.pointCatalogEventId;
        state.rule.runtimePointCatalogSourceRevision = receipt.pointCatalogSourceRevision;
        state.rule.publicationRevision += 1;
        return send(res, 200, envelope(operationId, state.rule), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/overview') {
        return send(res, 200, envelope('getBpiOverview', [state.line]), 'getBpiOverview');
      }
      if (req.method === 'GET' && path === '/bpi/v1/feature-flags') {
        const operationId = 'listFeatureFlags';
        const plantId = url.searchParams.get('plantId');
        const lineId = url.searchParams.get('lineId');
        const scopeType = String(url.searchParams.get('scopeType') || 'LINE').toUpperCase();
        if (plantId !== 'PLANT-01' || lineId !== 'LINE-S07-01'
            || !['TENANT', 'PLANT', 'LINE'].includes(scopeType)) {
          return send(res, 422, problem(422, 'Validation Failed', 'A valid plant, line and scope type are required.', operationId), operationId);
        }
        return send(res, 200, envelope(operationId,
          featureFlagViews(state, plantId, lineId, scopeType)), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/feature-flags\/([^/]+)$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'changeFeatureFlagOverride';
        const definition = FEATURE_FLAG_DEFINITIONS.find((item) => item.flagKey === ids[0]);
        if (!definition) return send(res, 422, problem(422, 'Validation Failed', 'Unsupported BPI feature flag.', operationId), operationId);
        if (!definition.editable) return send(res, 422, problem(422, 'Validation Failed', definition.blockedReason, operationId), operationId);
        const body = await readJson(req);
        const scopeType = String(body.scopeType || '').toUpperCase();
        const mode = String(body.mode || '').toUpperCase();
        const scopeKey = featureFlagScopeKey(scopeType, body.plantId, body.lineId);
        if (body.plantId !== 'PLANT-01' || body.lineId !== 'LINE-S07-01'
            || !scopeKey || !['SET', 'INHERIT'].includes(mode)
            || !body.reason || String(body.reason).trim().length < 8
            || (mode === 'SET' && typeof body.enabled !== 'boolean')) {
          return send(res, 422, problem(422, 'Validation Failed', 'Feature flag command is invalid.', operationId), operationId);
        }
        const current = state.featureFlags.find((item) => item.tenantId === FEATURE_FLAG_TENANT
          && item.scopeType === scopeType && item.scopeKey === scopeKey && item.flagKey === ids[0]) || null;
        const context = commandContext(req, res, operationId, current?.revision || 0, state, path);
        if (!context) return;
        if (mode === 'INHERIT' && (!current || !current.active)) {
          return send(res, 409, problem(409, 'Conflict', 'Feature flag already inherits from its parent scope.', operationId, current?.revision || 0), operationId);
        }
        if (mode === 'SET' && current?.active && current.enabled === body.enabled) {
          return send(res, 422, problem(422, 'Validation Failed', 'Feature flag override already has the requested value.', operationId), operationId);
        }
        const before = current ? clone(current) : null;
        let after = current;
        if (!after) {
          after = {
            id: stableUuid(`${FEATURE_FLAG_TENANT}|${scopeType}|${scopeKey}|${ids[0]}`),
            tenantId: FEATURE_FLAG_TENANT, scopeType, scopeKey, flagKey: ids[0], enabled: body.enabled,
            active: true, revision: 1, updatedBy: 'bpi.admin', updatedAt: FIXED_TIME, lastReason: body.reason,
          };
          state.featureFlags.push(after);
        } else {
          after.enabled = mode === 'SET' ? body.enabled : after.enabled;
          after.active = mode === 'SET';
          after.revision += 1;
          after.updatedBy = 'bpi.admin';
          after.updatedAt = FIXED_TIME;
          after.lastReason = body.reason;
        }
        state.featureFlagAudits.push({ flagKey: ids[0], scopeType, scopeKey,
          beforeRevision: before?.revision || 0, afterRevision: after.revision,
          action: mode === 'INHERIT' ? 'FEATURE_FLAG_OVERRIDE_REMOVED'
            : body.enabled ? 'FEATURE_FLAG_ENABLED' : 'FEATURE_FLAG_DISABLED', reason: body.reason });
        const result = featureFlagViews(state, body.plantId, body.lineId, scopeType)
          .find((item) => item.flagKey === ids[0]);
        const response = envelope(operationId, result);
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/lines\/([^/]+)\/current-state$/);
      if (req.method === 'GET' && ids) {
        if (ids[0] !== state.line.lineId) return send(res, 404, problem(404, 'Not Found', 'Line not found.', 'getCurrentLineState'), 'getCurrentLineState');
        return send(res, 200, envelope('getCurrentLineState', state.line), 'getCurrentLineState');
      }
      if (req.method === 'GET' && path === '/bpi/v1/candidates') {
        const requestedState = url.searchParams.get('state');
        const items = candidates(state).filter((candidate) => !requestedState || candidate.state === requestedState);
        return send(res, 200, envelope('listBatchCandidates', items), 'listBatchCandidates');
      }
      ids = match(path, /^\/bpi\/v1\/candidates\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const candidate = candidates(state).find((item) => item.id === ids[0]);
        if (!candidate) return send(res, 404, problem(404, 'Not Found', 'Candidate not found.', 'getBatchCandidate'), 'getBatchCandidate');
        return send(res, 200, envelope('getBatchCandidate', candidate), 'getBatchCandidate');
      }
      ids = match(path, /^\/bpi\/v1\/candidates\/([^/]+)\/confirm$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'confirmBatchCandidate';
        const candidate = candidates(state).find((item) => item.id === ids[0]);
        if (!candidate) return send(res, 404, problem(404, 'Not Found', 'Candidate not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, candidate.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (candidate.state !== 'PENDING') {
          const response = problem(409, 'Candidate Already Processed', 'The candidate is no longer pending.', operationId, candidate.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        let batch;
        if (candidate.boundaryType === 'START') {
          if (state.batches.some((item) => item.lineId === candidate.lineId && ['ACTIVE', 'SUSPENDED'].includes(item.state))) {
            const response = problem(409, 'Open Batch Exists', 'The line already has an open batch.', operationId, candidate.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          batch = batchFromCandidate(candidate);
          state.batches.push(batch);
          state.endCandidate = endCandidateFromStart(candidate);
          state.line.currentBatchId = batch.id;
          state.line.pendingCandidates = 1;
          state.batchEvents = [
            { revision: 1, action: 'SHADOW_BATCH_CREATED', at: FIXED_TIME, actor: 'simulated.shift.lead', reason: body.reason, fromState: null, toState: 'ACTIVE' },
          ];
        } else {
          batch = state.batches.find((item) => item.lineId === candidate.lineId
            && item.orderId === candidate.orderId && item.state === 'ACTIVE');
          if (!batch) {
            const response = problem(409, 'Active Batch Not Found', 'No matching ACTIVE batch can be closed.', operationId, candidate.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          if (state.forceCloseTasks.get(batch.id)?.state === 'PENDING_APPROVAL') {
            const response = problem(409, 'Force Close Pending', 'The batch has a pending force-close request.', operationId, batch.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          if (new Date(candidate.boundaryTime) <= new Date(batch.startTime)) {
            const response = problem(409, 'Invalid End Boundary', 'END boundary must be after START boundary.', operationId, candidate.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          batch.state = 'CLOSED_RAW';
          batch.revision += 1;
          batch.endTime = candidate.boundaryTime;
          state.line.currentBatchId = null;
          state.line.status = 'IDLE';
          state.line.pendingCandidates = 0;
          state.batchEvents.push({
            revision: batch.revision,
            action: 'END_BOUNDARY_CONFIRMED',
            at: FIXED_TIME,
            actor: 'simulated.shift.lead',
            reason: body.reason,
            fromState: 'ACTIVE',
            toState: 'CLOSED_RAW',
          });
        }
        candidate.state = 'CONFIRMED';
        candidate.revision += 1;
        candidate.batchId = batch.id;
        candidate.review = { actor: 'simulated.shift.lead', reason: body.reason, at: FIXED_TIME };
        const response = envelope(operationId, { candidate, batch });
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/candidates\/([^/]+)\/reject$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'rejectBatchCandidate';
        const candidate = candidates(state).find((item) => item.id === ids[0]);
        if (!candidate) return send(res, 404, problem(404, 'Not Found', 'Candidate not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, candidate.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (candidate.state !== 'PENDING') {
          const response = problem(409, 'Candidate Already Processed', 'The candidate is no longer pending.', operationId, candidate.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        candidate.state = 'REJECTED';
        candidate.revision += 1;
        candidate.batchId = null;
        candidate.review = { actor: 'simulated.shift.lead', reason: body.reason, at: FIXED_TIME };
        state.line.pendingCandidates = 0;
        const response = envelope(operationId, candidate);
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/batches') {
        return send(res, 200, envelope('listBatches', state.batches), 'listBatches');
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', 'getBatch'), 'getBatch');
        return send(res, 200, envelope('getBatch', batch), 'getBatch');
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)\/release$/);
      if (req.method === 'GET' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', 'getBatchRelease'), 'getBatchRelease');
        const release = state.batchReleases.get(batch.id) || {
          batch, qualityGate: null, wmsInbound: null, wmsInboundReversal: null,
        };
        return send(res, 200, envelope('getBatchRelease', release), 'getBatchRelease');
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)\/wms\/reversal$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getWmsInboundReversalTask';
        const batch = state.batches.find((item) => item.id === ids[0]);
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        return send(res, 200, envelope(operationId, state.wmsInboundReversalTasks.get(batch.id) || null), operationId);
      }
      if (req.method === 'POST' && ids) {
        const operationId = 'commandWmsInboundReversal';
        const batch = state.batches.find((item) => item.id === ids[0]);
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, batch.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3
            || !['REQUEST', 'APPROVE'].includes(body.approvalMode)) {
          const response = problem(422, 'Validation Failed', 'reason and approvalMode are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const release = state.batchReleases.get(batch.id);
        const inbound = release?.wmsInbound;
        if (batch.shadow || batch.state !== 'INBOUNDED' || inbound?.status !== 'ACCEPTED'
            || !inbound.documentId || inbound.outboxStatus !== 'PUBLISHED') {
          const response = problem(409, 'Invalid Batch State', 'Only a durable INBOUNDED batch can enter reversal approval.', operationId, batch.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const current = state.wmsInboundReversalTasks.get(batch.id);
        const timeline = state.batchEventsById.get(batch.id) || [];
        if (body.approvalMode === 'REQUEST') {
          if (current && ['PENDING_APPROVAL', 'PENDING_WMS'].includes(current.state)) {
            const response = problem(409, 'Reversal Pending', 'Batch already has an active WMS reversal task.', operationId, batch.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          batch.revision += 1;
          const task = {
            taskId: stableUuid(`${batch.id}|wms-reversal|${batch.revision}`),
            batchId: batch.id,
            state: 'PENDING_APPROVAL',
            revision: 1,
            batchRevision: batch.revision,
            originalInboundLinkId: inbound.id,
            originalCommandEventId: inbound.commandEventId,
            originalIdempotencyKey: inbound.idempotencyKey,
            originalDocumentId: inbound.documentId,
            requestedBy: 'simulated.shift.lead',
            requestedAt: FIXED_TIME,
            requestReason: body.reason,
            requestComment: body.comment || null,
            decidedBy: null,
            decidedAt: null,
            decisionReason: null,
            decisionComment: null,
            reversalCommandEventId: null,
            reversalIdempotencyKey: null,
            reversalReceiptEventId: null,
            reversalDocumentId: null,
            errorCode: null,
            detail: null,
            observedAt: null,
            outboxStatus: null,
            deliveryAttemptCount: 0,
          };
          state.wmsInboundReversalTasks.set(batch.id, task);
          release.wmsInboundReversal = task;
          timeline.push({
            revision: batch.revision,
            action: 'WMS_INBOUND_REVERSAL_REQUESTED',
            at: FIXED_TIME,
            actor: task.requestedBy,
            reason: body.reason,
            fromState: 'INBOUNDED',
            toState: 'INBOUNDED',
          });
          state.batchEventsById.set(batch.id, timeline);
          return rememberAndSend(state, context, res, 202, envelope(operationId, task), operationId);
        }
        if (!current || current.state !== 'PENDING_APPROVAL') {
          const response = problem(409, 'Reversal Missing', 'Batch has no pending WMS reversal approval.', operationId, batch.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        if (current.originalDocumentId !== inbound.documentId
            || current.originalCommandEventId !== inbound.commandEventId
            || current.originalIdempotencyKey !== inbound.idempotencyKey) {
          const response = problem(409, 'Original Inbound Changed', 'Accepted WMS inbound facts changed after reversal submission.', operationId, batch.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        batch.revision += 1;
        batch.state = 'INBOUND_REVERSING';
        batch.wmsStatus = 'REVERSAL_PENDING';
        current.state = 'PENDING_WMS';
        current.revision += 1;
        current.batchRevision = batch.revision;
        current.decidedBy = 'simulated.bpi.admin';
        current.decidedAt = FIXED_TIME;
        current.decisionReason = body.reason;
        current.decisionComment = body.comment || null;
        current.reversalCommandEventId = stableUuid(`${current.taskId}|red-command`);
        current.reversalIdempotencyKey = `WMS_COMPLETION_INBOUND_REVERSAL|TENANT-01|${batch.id}|${current.taskId}|1`;
        current.outboxStatus = 'PENDING';
        release.wmsInboundReversal = current;
        timeline.push({
          revision: batch.revision,
          action: 'WMS_INBOUND_REVERSAL_APPROVED',
          at: FIXED_TIME,
          actor: current.decidedBy,
          reason: body.reason,
          fromState: 'INBOUNDED',
          toState: 'INBOUND_REVERSING',
        });
        state.batchEventsById.set(batch.id, timeline);
        return rememberAndSend(state, context, res, 202, envelope(operationId, current), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)\/wms\/reconcile$/);
      if (req.method === 'POST' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        const operationId = 'reconcileWmsInbound';
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        const release = state.batchReleases.get(batch.id);
        const inbound = release?.wmsInbound;
        if (!inbound) return send(res, 404, problem(404, 'Not Found', 'WMS command not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, inbound.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (batch.state !== 'RELEASED' || inbound.status !== 'PENDING'
            || !['PUBLISHED', 'FAILED'].includes(inbound.outboxStatus)
            || !inbound.reconciliationAllowed) {
          const response = problem(
            409,
            'WMS Reconciliation Conflict',
            'Only an eligible PENDING WMS command can be reconciled.',
            operationId,
            inbound.revision,
          );
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        inbound.outboxStatus = 'PENDING';
        inbound.reconciliationCount += 1;
        inbound.lastReconciledAt = FIXED_TIME;
        inbound.lastReconciledBy = 'simulated.bpi.admin';
        inbound.reconcileAfter = new Date(Date.parse(FIXED_TIME) + 5 * 60 * 1000).toISOString();
        inbound.reconciliationAllowed = false;
        inbound.reconciliationBlockedReason = 'OUTBOX_BUSY';
        inbound.revision += 1;
        const timeline = state.batchEventsById.get(batch.id) || [];
        timeline.push({
          revision: inbound.revision,
          action: 'WMS_INBOUND_RECONCILIATION_QUEUED',
          at: FIXED_TIME,
          actor: 'simulated.bpi.admin',
          reason: body.reason,
          fromState: 'RELEASED',
          toState: 'RELEASED',
        });
        state.batchEventsById.set(batch.id, timeline);
        return rememberAndSend(state, context, res, 200, envelope(operationId, release), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)\/force-close$/);
      if (req.method === 'GET' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        const operationId = 'getBatchForceCloseTask';
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        return send(res, 200, envelope(operationId, state.forceCloseTasks.get(batch.id) || null), operationId);
      }
      if (req.method === 'POST' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        const operationId = 'forceCloseBatch';
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, batch.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const boundaryTime = Date.parse(body.boundaryTime || '');
        if (!body.reason || String(body.reason).trim().length < 3
            || !['REQUEST', 'APPROVE'].includes(body.approvalMode)
            || !Number.isFinite(boundaryTime)) {
          const response = problem(422, 'Validation Failed', 'reason, boundaryTime and approvalMode are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (!['ACTIVE', 'SUSPENDED'].includes(batch.state)) {
          const response = problem(409, 'Invalid Batch State', 'Only ACTIVE or SUSPENDED batches can enter force-close approval.', operationId, batch.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const pending = state.forceCloseTasks.get(batch.id);
        const timeline = state.batchEventsById.get(batch.id) || state.batchEvents;
        if (body.approvalMode === 'REQUEST') {
          if (pending?.state === 'PENDING_APPROVAL') {
            const response = problem(409, 'Force Close Pending', 'Batch already has a pending force-close request.', operationId, batch.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          if (boundaryTime < Date.parse(batch.startTime)) {
            const response = problem(422, 'Invalid Boundary Time', 'boundaryTime cannot be before batch startTime.', operationId, batch.revision);
            return rememberAndSend(state, context, res, 422, response, operationId);
          }
          batch.revision += 1;
          const task = {
            taskId: stableUuid(`${batch.id}|force-close|${batch.revision}`),
            batchId: batch.id,
            state: 'PENDING_APPROVAL',
            revision: 1,
            batchRevision: batch.revision,
            sourceState: batch.state,
            boundaryTime: new Date(boundaryTime).toISOString(),
            requestedBy: 'simulated.shift.lead',
            requestedAt: FIXED_TIME,
            requestReason: body.reason,
            requestComment: body.comment || null,
            decidedBy: null,
            decidedAt: null,
            decisionReason: null,
            decisionComment: null,
          };
          state.forceCloseTasks.set(batch.id, task);
          timeline.push({
            revision: batch.revision,
            action: 'BATCH_FORCE_CLOSE_REQUESTED',
            at: FIXED_TIME,
            actor: task.requestedBy,
            reason: body.reason,
            fromState: batch.state,
            toState: batch.state,
          });
          state.batchEventsById.set(batch.id, timeline);
          return rememberAndSend(state, context, res, 202, envelope(operationId, task), operationId);
        }
        if (!pending || pending.state !== 'PENDING_APPROVAL') {
          const response = problem(409, 'Force Close Missing', 'Batch has no pending force-close request.', operationId, batch.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        if (new Date(boundaryTime).toISOString() !== pending.boundaryTime) {
          const response = problem(409, 'Boundary Time Changed', 'Approval boundaryTime must match the pending request.', operationId, batch.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const previousState = batch.state;
        batch.state = 'CLOSED_RAW';
        batch.revision += 1;
        batch.endTime = pending.boundaryTime;
        Object.assign(pending, {
          state: 'COMPLETED',
          revision: 2,
          batchRevision: batch.revision,
          decidedBy: 'simulated.bpi.admin',
          decidedAt: FIXED_TIME,
          decisionReason: body.reason,
          decisionComment: body.comment || null,
        });
        timeline.push({
          revision: batch.revision,
          action: 'BATCH_FORCE_CLOSED',
          at: FIXED_TIME,
          actor: pending.decidedBy,
          reason: body.reason,
          fromState: previousState,
          toState: 'CLOSED_RAW',
        });
        state.batchEventsById.set(batch.id, timeline);
        state.line.currentBatchId = null;
        state.line.status = 'IDLE';
        return rememberAndSend(state, context, res, 202, envelope(operationId, pending), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)\/(suspend|resume)$/);
      if (req.method === 'POST' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        const isSuspend = ids[1] === 'suspend';
        const operationId = isSuspend ? 'suspendBatch' : 'resumeBatch';
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, batch.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (state.forceCloseTasks.get(batch.id)?.state === 'PENDING_APPROVAL') {
          const response = problem(409, 'Force Close Pending', 'Batch has a pending force-close request.', operationId, batch.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const expectedState = isSuspend ? 'ACTIVE' : 'SUSPENDED';
        const nextState = isSuspend ? 'SUSPENDED' : 'ACTIVE';
        if (batch.state !== expectedState) {
          const response = problem(
            409,
            'Invalid Batch State',
            `Batch must be ${expectedState} before it can ${ids[1]}.`,
            operationId,
            batch.revision,
          );
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const previousState = batch.state;
        batch.state = nextState;
        batch.revision += 1;
        state.line.status = isSuspend ? 'BLOCKED' : 'RUNNING';
        state.batchEvents.push({
          revision: batch.revision,
          action: isSuspend ? 'BATCH_SUSPENDED' : 'BATCH_RESUMED',
          at: FIXED_TIME,
          actor: 'simulated.shift.lead',
          reason: body.reason,
          fromState: previousState,
          toState: nextState,
        });
        return rememberAndSend(state, context, res, 200, envelope(operationId, batch), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/batches\/([^/]+)\/(evidence|balance|genealogy|timeline)$/);
      if (req.method === 'GET' && ids) {
        const batch = state.batches.find((item) => item.id === ids[0]);
        const operationIds = { evidence: 'getBatchEvidence', balance: 'getBatchBalance', genealogy: 'getBatchGenealogy', timeline: 'getBatchTimeline' };
        const operationId = operationIds[ids[1]];
        if (!batch) return send(res, 404, problem(404, 'Not Found', 'Batch not found.', operationId), operationId);
        const data = {
          evidence: state.batchEvidenceById.get(batch.id) || {
            start: state.candidate.batchId === batch.id ? state.candidate.evidence : [],
            end: state.endCandidate?.batchId === batch.id ? state.endCandidate.evidence : [],
          },
          balance: { input: 12.4, output: 12.1, difference: 0.3, differencePercent: 2.42, status: 'WITHIN_TOLERANCE', allocations: [] },
          genealogy: { nodes: [{ id: batch.id, type: 'BATCH', label: batch.batchNo }], edges: [] },
          timeline: state.batchEventsById.get(batch.id) || state.batchEvents,
        }[ids[1]];
        return send(res, 200, envelope(operationId, data), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/shadow-runs') {
        const plantId = url.searchParams.get('plantId');
        const lineId = url.searchParams.get('lineId');
        const requestedState = url.searchParams.get('state');
        const runs = state.shadowRuns
          .filter((run) => (!plantId || run.plantId === plantId)
            && (!lineId || run.lineId === lineId)
            && (!requestedState || run.state === requestedState))
          .map((run) => hydrateShadowRun(state, run));
        return send(res, 200, envelope('listShadowRuns', runs), 'listShadowRuns');
      }
      if (req.method === 'POST' && path === '/bpi/v1/shadow-runs') {
        const operationId = 'createShadowRun';
        const context = commandContext(req, res, operationId, 0, state, path);
        if (!context) return;
        const body = await readJson(req);
        const rule = state.rules.find((item) => item.id === body.ruleVersionId);
        const topology = rule
          ? state.topologies.find((item) => `${item.code}@${item.version}` === rule.topologyVersion)
          : null;
        const validThresholds = Number.isInteger(body.minimumDurationDays)
          && body.minimumDurationDays >= 7 && body.minimumDurationDays <= 14
          && Number.isInteger(body.minimumReviewedBatches) && body.minimumReviewedBatches >= 10
          && Number.isInteger(body.boundaryToleranceSeconds) && body.boundaryToleranceSeconds >= 0
          && Number(body.minimumBoundaryAgreement) >= 0.95 && Number(body.minimumBoundaryAgreement) <= 1
          && Number(body.quantityTolerancePercent) > 0;
        if (!body.runCode || !body.name || !body.plantId || !body.lineId || !body.reason
            || String(body.reason).trim().length < 3 || !validThresholds) {
          const response = problem(422, 'Validation Failed', 'Shadow run fields and acceptance thresholds are invalid.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (!rule || rule.state !== 'PUBLISHED' || rule.plantId !== body.plantId || rule.lineId !== body.lineId) {
          const response = problem(422, 'Rule Not Eligible', 'A shadow run must pin a PUBLISHED rule in the same scope.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (!topology || topology.state !== 'PUBLISHED' || !topology.validatedPointCatalogSnapshotId) {
          const response = problem(422, 'Topology Not Eligible', 'The rule must pin a published topology and point catalog snapshot.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (state.shadowRuns.some((item) => item.runCode === body.runCode)) {
          const response = problem(409, 'Shadow Run Exists', 'runCode already exists in the tenant.', operationId, 0);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const run = {
          id: stableUuid(`shadow-run|${body.runCode}`), runCode: body.runCode, name: body.name,
          tenantId: 'TENANT-01', plantId: body.plantId, lineId: body.lineId, state: 'DRAFT', revision: 1,
          ruleVersionId: rule.id, ruleVersion: `${rule.code}@${rule.version}`,
          topologyVersionId: topology.id, topologyVersion: `${topology.code}@${topology.version}`,
          pointCatalogSnapshotId: topology.validatedPointCatalogSnapshotId,
          pointCatalogChecksum: topology.validatedPointCatalogChecksum,
          minimumDurationDays: body.minimumDurationDays,
          minimumReviewedBatches: body.minimumReviewedBatches,
          boundaryToleranceSeconds: body.boundaryToleranceSeconds,
          minimumBoundaryAgreement: Number(body.minimumBoundaryAgreement),
          quantityTolerancePercent: Number(body.quantityTolerancePercent),
          createdBy: 'simulated.process.engineer', createdAt: FIXED_TIME,
          startedBy: null, startedAt: null, completedBy: null, completedAt: null,
          decidedBy: null, decidedAt: null, decisionReason: null,
          cancelledBy: null, cancelledAt: null, cancellationReason: null,
        };
        state.shadowRuns.push(run);
        const response = envelope(operationId, hydrateShadowRun(state, run));
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/shadow-runs\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const run = state.shadowRuns.find((item) => item.id === ids[0]);
        if (!run) return send(res, 404, problem(404, 'Not Found', 'Shadow run not found.', 'getShadowRun'), 'getShadowRun');
        return send(res, 200, envelope('getShadowRun', hydrateShadowRun(state, run)), 'getShadowRun');
      }
      ids = match(path, /^\/bpi\/v1\/shadow-runs\/([^/]+)\/batch-reviews$/);
      if (req.method === 'GET' && ids) {
        const run = state.shadowRuns.find((item) => item.id === ids[0]);
        if (!run) return send(res, 404, problem(404, 'Not Found', 'Shadow run not found.', 'listShadowRunBatchReviews'), 'listShadowRunBatchReviews');
        const includeSuperseded = url.searchParams.get('includeSuperseded') === 'true';
        const reviews = state.shadowRunReviews.filter((item) => item.shadowRunId === run.id
          && (includeSuperseded || item.state === 'ACTIVE'));
        return send(res, 200, envelope('listShadowRunBatchReviews', reviews), 'listShadowRunBatchReviews');
      }
      if (req.method === 'POST' && ids) {
        const operationId = 'reviewShadowRunBatch';
        const run = state.shadowRuns.find((item) => item.id === ids[0]);
        if (!run) return send(res, 404, problem(404, 'Not Found', 'Shadow run not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, run.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const batch = state.batches.find((item) => item.id === body.batchId);
        const manualStart = Date.parse(body.manualStartTime);
        const manualEnd = Date.parse(body.manualEndTime);
        const referenceQuantity = Number(body.referenceQuantity);
        if (run.state !== 'RUNNING') {
          const response = problem(409, 'Invalid Shadow Run State', 'Only a RUNNING shadow run accepts batch reviews.', operationId, run.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        if (!batch || !batch.shadow || batch.state !== 'CLOSED_RAW' || batch.lineId !== run.lineId
            || batch.ruleVersion !== run.ruleVersion || batch.topologyVersion !== run.topologyVersion
            || !batch.endTime || Date.parse(batch.startTime) < Date.parse(run.startedAt)) {
          const response = problem(422, 'Batch Not Eligible', 'The batch must be CLOSED_RAW and match the pinned run scope and versions.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (!Number.isFinite(manualStart) || !Number.isFinite(manualEnd) || manualEnd <= manualStart
            || !Number.isFinite(referenceQuantity) || referenceQuantity <= 0
            || body.quantityUnit !== batch.quantityUnit || !body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'Manual boundaries, quantity, unit and reason are invalid.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const existing = state.shadowRunReviews.find((item) => item.shadowRunId === run.id
          && item.batchId === batch.id && item.state === 'ACTIVE');
        if (existing) {
          existing.state = 'SUPERSEDED';
          existing.supersededAt = FIXED_TIME;
        }
        const previousSequence = state.shadowRunReviews
          .filter((item) => item.shadowRunId === run.id && item.batchId === batch.id)
          .reduce((maximum, item) => Math.max(maximum, item.reviewSequence), 0);
        const startDeviationSeconds = Math.ceil(Math.abs(Date.parse(batch.startTime) - manualStart) / 1000);
        const endDeviationSeconds = Math.ceil(Math.abs(Date.parse(batch.endTime) - manualEnd) / 1000);
        const quantityDeviationPercent = Math.abs(batch.quantity - referenceQuantity) / referenceQuantity * 100;
        const review = {
          id: stableUuid(`${run.id}|${batch.id}|${previousSequence + 1}`), shadowRunId: run.id,
          batchId: batch.id, batchNo: batch.batchNo, reviewSequence: previousSequence + 1, state: 'ACTIVE',
          automaticStartTime: batch.startTime, automaticEndTime: batch.endTime,
          manualStartTime: new Date(manualStart).toISOString(), manualEndTime: new Date(manualEnd).toISOString(),
          startDeviationSeconds, endDeviationSeconds,
          startBoundaryAccepted: startDeviationSeconds <= run.boundaryToleranceSeconds,
          endBoundaryAccepted: endDeviationSeconds <= run.boundaryToleranceSeconds,
          automaticQuantity: batch.quantity, referenceQuantity, quantityUnit: body.quantityUnit,
          quantityDeviationPercent, quantityWithinTolerance: quantityDeviationPercent <= run.quantityTolerancePercent,
          reviewedBy: 'simulated.shift.lead', reviewReason: body.reason, reviewedAt: FIXED_TIME, supersededAt: null,
        };
        state.shadowRunReviews.push(review);
        run.revision += 1;
        const response = envelope(operationId, { run: hydrateShadowRun(state, run), review });
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/shadow-runs\/([^/]+)\/(start|complete|approve|reject|cancel)$/);
      if (req.method === 'POST' && ids) {
        const run = state.shadowRuns.find((item) => item.id === ids[0]);
        const action = ids[1];
        const operationIds = {
          start: 'startShadowRun', complete: 'completeShadowRun', approve: 'approveShadowRun',
          reject: 'rejectShadowRun', cancel: 'cancelShadowRun',
        };
        const operationId = operationIds[action];
        if (!run) return send(res, 404, problem(404, 'Not Found', 'Shadow run not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, run.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const current = hydrateShadowRun(state, run);
        if (action === 'start') {
          if (run.state !== 'DRAFT') {
            const response = problem(409, 'Invalid Shadow Run State', 'Only a DRAFT shadow run can start.', operationId, run.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          if (!current.readiness.ready) {
            const response = problem(422, 'Runtime Not Ready', 'Pinned rule, topology and point catalog are not operationally ready.', operationId);
            return rememberAndSend(state, context, res, 422, response, operationId);
          }
          run.state = 'RUNNING'; run.revision += 1;
          run.startedBy = 'simulated.process.engineer'; run.startedAt = '2026-07-05T08:00:00.000Z';
        } else if (action === 'complete') {
          if (run.state !== 'RUNNING') {
            const response = problem(409, 'Invalid Shadow Run State', 'Only a RUNNING shadow run can enter evaluation.', operationId, run.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          if (!current.metrics.durationGatePassed || !current.metrics.reviewCountGatePassed) {
            const response = problem(422, 'Acceptance Window Incomplete', 'Minimum duration and batch review count must pass first.', operationId);
            return rememberAndSend(state, context, res, 422, response, operationId);
          }
          run.state = 'EVALUATING'; run.revision += 1;
          run.completedBy = 'simulated.process.engineer'; run.completedAt = FIXED_TIME;
        } else if (action === 'approve' || action === 'reject') {
          if (run.state !== 'EVALUATING') {
            const response = problem(409, 'Invalid Shadow Run State', 'Only an EVALUATING shadow run can be decided.', operationId, run.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          if (action === 'approve' && !current.readyForApproval) {
            const response = problem(422, 'Acceptance Gates Blocked', `Approval blocked by: ${current.blockers.join(', ')}`, operationId);
            return rememberAndSend(state, context, res, 422, response, operationId);
          }
          run.state = action === 'approve' ? 'APPROVED' : 'REJECTED'; run.revision += 1;
          run.decidedBy = 'simulated.bpi.admin'; run.decidedAt = FIXED_TIME; run.decisionReason = body.reason;
        } else {
          if (!['DRAFT', 'RUNNING'].includes(run.state)) {
            const response = problem(409, 'Invalid Shadow Run State', 'Only a DRAFT or RUNNING shadow run can be cancelled.', operationId, run.revision);
            return rememberAndSend(state, context, res, 409, response, operationId);
          }
          run.state = 'CANCELLED'; run.revision += 1;
          run.cancelledBy = 'simulated.process.engineer'; run.cancelledAt = FIXED_TIME; run.cancellationReason = body.reason;
        }
        const response = envelope(operationId, hydrateShadowRun(state, run));
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/topologies') {
        return send(res, 200, envelope('listTopologies', state.topologies), 'listTopologies');
      }
      if (req.method === 'GET' && path === '/bpi/v1/point-catalog/current') {
        refreshPointCatalogReadiness(state);
        const operationId = 'getCurrentPointCatalog';
        const explicitPagination = ['search', 'cursor', 'limit']
          .some((name) => url.searchParams.has(name));
        const matchesScope = (catalog) => catalog
          && catalog.snapshot.plantId === url.searchParams.get('plantId')
          && catalog.snapshot.lineId === url.searchParams.get('lineId');
        if (!explicitPagination) {
          return send(res, 200, envelope(operationId, matchesScope(state.pointCatalog) ? state.pointCatalog : null), operationId);
        }
        const rawLimit = url.searchParams.get('limit');
        const limit = rawLimit === null ? 100 : Number(rawLimit);
        const search = String(url.searchParams.get('search') || '').trim().toLowerCase();
        if (!Number.isInteger(limit) || limit < 1 || limit > 200 || search.length > 128) {
          return send(res, 422, problem(422, 'Validation Failed',
            search.length > 128 ? 'search must not exceed 128 characters.' : 'limit must be between 1 and 200.', operationId), operationId);
        }
        const scopeFingerprint = pointCatalogScopeFingerprint(url, search);
        let cursor = null;
        try {
          const encodedCursor = url.searchParams.get('cursor');
          cursor = encodedCursor ? decodePointCatalogCursor(encodedCursor, scopeFingerprint) : null;
        } catch (error) {
          return send(res, 422, problem(422, 'Validation Failed',
            'Point catalog cursor is invalid or does not match the requested scope.', operationId), operationId);
        }
        const catalog = cursor
          ? state.pointCatalogHistory.find((item) => item.snapshot.id === cursor.snapshotId)
          : state.pointCatalog;
        if (!matchesScope(catalog)) {
          if (cursor) return send(res, 422, problem(422, 'Validation Failed', 'Point catalog cursor snapshot is unavailable.', operationId), operationId);
          return send(res, 200, envelope(operationId, null), operationId);
        }
        const values = catalog.points
          .filter((point) => !search || [point.productId, point.deviceId, point.propertyId,
            point.sourcePropertyId, point.pointName, point.localityGroup]
            .filter(Boolean).join(' ').toLowerCase().includes(search))
          .filter((point) => !cursor || comparePointIdentity(point, cursor) > 0)
          .sort(comparePointIdentity);
        const page = values.slice(0, limit);
        const last = page.at(-1);
        const nextCursor = values.length > limit && last
          ? encodePointCatalogCursor({
            version: 1,
            snapshotId: catalog.snapshot.id,
            productId: last.productId,
            deviceId: last.deviceId,
            propertyId: last.propertyId,
            scopeFingerprint,
          })
          : null;
        return send(res, 200, envelope(operationId, {
          snapshot: catalog.snapshot,
          points: page,
        }, { snapshotAt: catalog.snapshot.importedAt, nextCursor }), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/point-catalog/snapshots') {
        refreshPointCatalogReadiness(state);
        return send(res, 200, envelope('listPointCatalogSnapshots', state.pointCatalog ? [state.pointCatalog.snapshot] : []), 'listPointCatalogSnapshots');
      }
      if (req.method === 'POST' && path === '/bpi/v1/point-catalog/snapshots') {
        const operationId = 'importPointCatalogSnapshot';
        const context = commandContext(req, res, operationId, 0, state, path);
        if (!context) return;
        const body = await readJson(req);
        const missing = ['source', 'sourceInstance', 'sourceRevision', 'plantId', 'lineId', 'observedAt', 'points', 'reason']
          .filter((key) => body[key] === undefined || body[key] === '');
        if (missing.length || !Array.isArray(body.points)) {
          return send(res, 422, problem(422, 'Validation Failed', `Missing or invalid fields: ${missing.join(', ')}.`, operationId), operationId);
        }
        const snapshotId = stableUuid({ type: 'point-catalog', source: body.source, sourceRevision: body.sourceRevision });
        const points = body.points.map((point) => ({
            id: stableUuid({ type: 'point', snapshotId, productId: point.productId, deviceId: point.deviceId, propertyId: point.propertyId }),
            snapshotId, plantId: body.plantId, lineId: body.lineId, ...clone(point),
            sourceCalibrationStatus: point.calibrationStatus || (point.calibrationVersion ? 'UNVERIFIED' : 'MISSING'),
            calibrationStatus: point.calibrationVersion ? 'UNVERIFIED' : 'MISSING',
            calibrationEvidenceId: null, calibrationValidUntil: null,
            sourceSequenceRequired: point.sourceSequenceRequired === true,
            sourceSequenceOrigin: point.sourceSequenceOrigin || null,
            sourceSequenceBindingFingerprint: point.sourceSequenceBindingFingerprint || null,
            sourceSequenceQualified: false,
            sourceSequenceEvidenceStatus: sourceSequenceBindingReady(point) ? 'MISSING' : 'DISABLED',
            sourceSequenceEpoch: null, sourceSequenceFirst: null, sourceSequenceLast: null,
            sourceSequenceObservationCount: null, sourceSequenceFirstObservedAt: null,
            sourceSequenceLastObservedAt: null, sourceSequenceValidUntil: null,
            sourceSequenceEvidenceEventId: null, sourceSequenceEvidenceRevision: null,
            ready: false, readinessIssues: [],
          }));
        const { reason: ignoredReason, ...catalogPayload } = body;
        const snapshot = {
          id: snapshotId, source: body.source, sourceInstance: body.sourceInstance,
          sourceRevision: body.sourceRevision, plantId: body.plantId, lineId: body.lineId,
          checksum: sha256(catalogPayload), observedAt: body.observedAt,
          pointCount: points.length, readyPointCount: points.filter((point) => point.ready).length,
          importedBy: 'simulated.bpi.admin', importedAt: FIXED_TIME,
        };
        state.pointCatalog = { snapshot, points };
        state.pointCatalogHistory.push(state.pointCatalog);
        refreshPointCatalogReadiness(state);
        return rememberAndSend(state, context, res, 200, envelope(operationId, state.pointCatalog), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/point-calibrations') {
        refreshPointCatalogReadiness(state);
        const operationId = 'listPointCalibrations';
        const rawLimit = url.searchParams.get('limit');
        const limit = rawLimit === null ? 50 : Number(rawLimit);
        if (!Number.isInteger(limit) || limit < 1 || limit > 200) {
          return send(res, 422, problem(422, 'Validation Failed', 'limit must be between 1 and 200.', operationId), operationId);
        }
        const scopeFingerprint = calibrationScopeFingerprint(url);
        let cursor = null;
        try {
          const encodedCursor = url.searchParams.get('cursor');
          cursor = encodedCursor ? decodeCalibrationCursor(encodedCursor, scopeFingerprint) : null;
        } catch (error) {
          return send(res, 422, problem(
            422,
            'Validation Failed',
            'Point calibration cursor is invalid or does not match the requested scope.',
            operationId,
          ), operationId);
        }
        const snapshotAt = cursor?.snapshotAt || new Date().toISOString();
        const snapshotTime = Date.parse(snapshotAt);
        const filters = ['plantId', 'lineId', 'productId', 'deviceId', 'propertyId'];
        const calibrations = state.pointCalibrations
          .filter((calibration) => filters.every((name) => {
            const expected = url.searchParams.get(name);
            return !expected || calibration[name] === expected;
          }))
          .filter((calibration) => Date.parse(calibration.submittedAt) <= snapshotTime)
          .filter((calibration) => !cursor
            || Date.parse(calibration.submittedAt) < Date.parse(cursor.submittedAt)
            || (Date.parse(calibration.submittedAt) === Date.parse(cursor.submittedAt)
              && calibration.id.localeCompare(cursor.id) < 0))
          .sort((left, right) => Date.parse(right.submittedAt) - Date.parse(left.submittedAt)
            || right.id.localeCompare(left.id));
        const page = calibrations.slice(0, limit);
        const last = page.at(-1);
        const nextCursor = calibrations.length > limit && last
          ? encodeCalibrationCursor({
            version: 1,
            snapshotAt,
            submittedAt: last.submittedAt,
            id: last.id,
            scopeFingerprint,
          })
          : null;
        return send(res, 200, envelope(operationId, page, { snapshotAt, nextCursor }), operationId);
      }
      if (req.method === 'POST' && path === '/bpi/v1/point-calibrations') {
        const operationId = 'submitPointCalibration';
        const context = commandContext(req, res, operationId, 0, state, path);
        if (!context) return;
        const body = await readJson(req);
        const required = ['plantId', 'lineId', 'productId', 'deviceId', 'propertyId', 'calibrationVersion',
          'certificateReference', 'certificateChecksum', 'validFrom', 'validUntil', 'reason'];
        const missing = required.filter((key) => body[key] === undefined || String(body[key]).trim() === '');
        const validFrom = Date.parse(body.validFrom);
        const validUntil = Date.parse(body.validUntil);
        if (missing.length || String(body.reason || '').trim().length < 3
            || !/^[a-f0-9]{64}$/.test(String(body.certificateChecksum || ''))
            || !Number.isFinite(validFrom) || !Number.isFinite(validUntil) || validUntil <= validFrom) {
          return send(res, 422, problem(422, 'Validation Failed', 'Point calibration payload is invalid.', operationId), operationId);
        }
        if (validUntil <= Date.now()) {
          return send(res, 422, problem(422, 'Validation Failed', 'Expired calibration evidence cannot be submitted.', operationId), operationId);
        }
        const duplicate = state.pointCalibrations.some((calibration) => calibration.plantId === body.plantId
          && calibration.lineId === body.lineId && calibration.productId === body.productId
          && calibration.deviceId === body.deviceId && calibration.propertyId === body.propertyId
          && calibration.calibrationVersion === body.calibrationVersion);
        if (duplicate) {
          return send(res, 409, problem(409, 'Point Calibration Conflict', 'The point calibration version already exists.', operationId), operationId);
        }
        const calibration = {
          id: stableUuid({ type: 'point-calibration', body }), plantId: body.plantId, lineId: body.lineId,
          productId: body.productId, deviceId: body.deviceId, propertyId: body.propertyId,
          calibrationVersion: body.calibrationVersion, certificateReference: body.certificateReference,
          certificateChecksum: body.certificateChecksum, validFrom: new Date(validFrom).toISOString(),
          validUntil: new Date(validUntil).toISOString(), state: 'PENDING', revision: 1,
          submittedBy: 'simulated.metrology.engineer', submittedAt: new Date().toISOString(),
          submitReason: body.reason, decidedBy: null, decidedAt: null, decisionReason: null,
          revokedBy: null, revokedAt: null, revokeReason: null, effective: false, effectivenessStatus: 'PENDING',
        };
        state.pointCalibrations.unshift(calibration);
        return rememberAndSend(state, context, res, 200, envelope(operationId, calibration), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/point-calibrations\/([^/]+)\/(approve|reject|revoke)$/);
      if (req.method === 'POST' && ids) {
        const action = ids[1];
        const operationId = `${action}PointCalibration`;
        const calibration = state.pointCalibrations.find((item) => item.id === ids[0]);
        if (!calibration) return send(res, 404, problem(404, 'Not Found', 'Point calibration not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, calibration.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          return send(res, 422, problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId), operationId);
        }
        const expectedState = action === 'revoke' ? 'APPROVED' : 'PENDING';
        if (calibration.state !== expectedState) {
          return send(res, 409, problem(409, 'Invalid Point Calibration State', `Point calibration must be ${expectedState}.`, operationId, calibration.revision), operationId);
        }
        if (action === 'approve' && Date.parse(calibration.validUntil) <= Date.now()) {
          return send(res, 422, problem(422, 'Validation Failed', 'Expired calibration evidence cannot be approved.', operationId), operationId);
        }
        calibration.revision += 1;
        if (action === 'revoke') {
          calibration.state = 'REVOKED';
          calibration.revokedBy = 'simulated.bpi.revoker';
          calibration.revokedAt = new Date().toISOString();
          calibration.revokeReason = body.reason;
        } else {
          calibration.state = action === 'approve' ? 'APPROVED' : 'REJECTED';
          calibration.decidedBy = 'simulated.bpi.admin';
          calibration.decidedAt = new Date().toISOString();
          calibration.decisionReason = body.reason;
        }
        refreshPointCatalogReadiness(state);
        return rememberAndSend(state, context, res, 200, envelope(operationId, calibration), operationId);
      }
      if (req.method === 'POST' && path === '/bpi/v1/topologies/drafts') {
        const operationId = 'createTopologyDraft';
        const body = await readJson(req);
        const base = body.baseVersionId ? state.topologies.find((item) => item.id === body.baseVersionId) : null;
        const context = commandContext(req, res, operationId, base?.revision || 0, state, path);
        if (!context) return;
        const required = ['code', 'version', 'plantId', 'lineId', 'definition', 'reason'];
        const missing = required.filter((key) => body[key] === undefined || body[key] === '');
        if (missing.length) return send(res, 422, problem(422, 'Validation Failed', `Missing fields: ${missing.join(', ')}.`, operationId), operationId);
        if (state.topologies.some((item) => item.code === body.code && item.version === body.version)) {
          return send(res, 409, problem(409, 'Version Conflict', 'Topology code and version already exist.', operationId), operationId);
        }
        const topology = {
          id: stableUuid({ type: 'topology', code: body.code, version: body.version }),
          code: body.code, version: body.version, state: 'DRAFT', revision: 1,
          plantId: body.plantId, lineId: body.lineId, checksum: sha256(body.definition),
          definition: clone(body.definition), validationStatus: 'NOT_VALIDATED',
          validationErrors: [], validationWarnings: [], validatedBy: null, validatedAt: null,
          validatedPointCatalogSnapshotId: null, validatedPointCatalogChecksum: null,
          publishedBy: null, publishedAt: null,
        };
        state.topologies.unshift(topology);
        const response = envelope(operationId, topology);
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const topology = state.topologies.find((item) => item.id === ids[0]);
        if (!topology) return send(res, 404, problem(404, 'Not Found', 'Topology not found.', 'getTopologyVersion'), 'getTopologyVersion');
        return send(res, 200, envelope('getTopologyVersion', topology), 'getTopologyVersion');
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)\/compare$/);
      if (req.method === 'GET' && ids) {
        const target = state.topologies.find((item) => item.id === ids[0]);
        const base = state.topologies.find((item) => item.id === url.searchParams.get('against'));
        if (!target || !base) return send(res, 404, problem(404, 'Not Found', 'Topology version not found.', 'compareTopologyVersions'), 'compareTopologyVersions');
        if (target.code !== base.code || target.plantId !== base.plantId || target.lineId !== base.lineId) {
          return send(res, 422, problem(422, 'Validation Failed', 'Topology versions must share code and scope before comparison.', 'compareTopologyVersions'), 'compareTopologyVersions');
        }
        return send(res, 200, envelope('compareTopologyVersions', compareVersions(
          'TOPOLOGY_VERSION', base, target, base.definition, target.definition,
        )), 'compareTopologyVersions');
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)\/validate$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'validateTopologyDraft';
        const topology = state.topologies.find((item) => item.id === ids[0]);
        if (!topology) return send(res, 404, problem(404, 'Not Found', 'Topology not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, topology.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || topology.state !== 'DRAFT') return send(res, 409, problem(409, 'Invalid Topology State', 'Only a draft topology can be validated.', operationId, topology.revision), operationId);
        const result = topologyValidation(topology.definition, state.pointCatalog);
        topology.validationErrors = result.errors;
        topology.validationWarnings = result.warnings;
        topology.validationStatus = result.errors.length ? 'FAILED' : 'PASSED';
        topology.validatedBy = 'simulated.process.engineer';
        topology.validatedAt = FIXED_TIME;
        topology.validatedPointCatalogSnapshotId = state.pointCatalog?.snapshot.id || null;
        topology.validatedPointCatalogChecksum = state.pointCatalog?.snapshot.checksum || null;
        topology.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, topology), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/topologies\/([^/]+)\/publish$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'publishTopologyVersion';
        const topology = state.topologies.find((item) => item.id === ids[0]);
        if (!topology) return send(res, 404, problem(404, 'Not Found', 'Topology not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, topology.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        refreshPointCatalogReadiness(state);
        const currentValidation = topologyValidation(topology.definition, state.pointCatalog);
        if (currentValidation.errors.length) {
          const codes = [...new Set(currentValidation.errors.map((issue) => issue.code))].sort().join(', ');
          return send(res, 422, problem(422, 'Validation Failed', `Topology publication requires current READY point catalog bindings: ${codes}.`, operationId, topology.revision), operationId);
        }
        if (!body.reason || topology.state !== 'DRAFT' || topology.validationStatus !== 'PASSED'
            || !topology.validatedPointCatalogSnapshotId || !topology.validatedPointCatalogChecksum
            || topology.validatedPointCatalogSnapshotId !== state.pointCatalog?.snapshot.id
            || topology.validatedPointCatalogChecksum !== state.pointCatalog?.snapshot.checksum) {
          return send(res, 409, problem(409, 'Invalid Topology State', 'Topology must pass validation against the current point catalog snapshot.', operationId, topology.revision), operationId);
        }
        topology.state = 'PUBLISHED';
        topology.publishedBy = 'simulated.bpi.admin';
        topology.publishedAt = FIXED_TIME;
        topology.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, topology), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/rules') {
        return send(res, 200, envelope('listRules', state.rules), 'listRules');
      }
      if (req.method === 'POST' && path === '/bpi/v1/rules/drafts') {
        const operationId = 'createRuleDraft';
        const body = await readJson(req);
        const base = body.baseVersionId ? state.rules.find((item) => item.id === body.baseVersionId) : null;
        const context = commandContext(req, res, operationId, base?.revision || 0, state, path);
        if (!context) return;
        const topology = state.topologies.find((item) => `${item.code}@${item.version}` === body.topologyVersion && item.state === 'PUBLISHED');
        const missing = ['code', 'version', 'lineId', 'topologyVersion', 'ast', 'reason'].filter((key) => body[key] === undefined || body[key] === '');
        if (missing.length || !topology) return send(res, 422, problem(422, 'Validation Failed', missing.length ? `Missing fields: ${missing.join(', ')}.` : 'Published topology not found.', operationId), operationId);
        if (base && !['PUBLISHED', 'RETIRED'].includes(base.state)) {
          return send(res, 409, problem(409, 'Invalid Base Version', 'Only a published or retired rule can be copied.', operationId, base.revision), operationId);
        }
        const boundSignals = new Set((topology.definition.bindings || []).map((binding) => binding.signal));
        const unbound = (body.ast.conditions || []).map((condition) => condition.signal).filter((signal) => !boundSignals.has(signal));
        if (unbound.length) return send(res, 422, problem(422, 'Validation Failed', `Rule conditions reference signals not bound by the topology: ${unbound.join(', ')}.`, operationId), operationId);
        if (state.rules.some((item) => item.code === body.code && item.version === body.version)) {
          return send(res, 409, problem(409, 'Version Conflict', 'Rule code and version already exist.', operationId), operationId);
        }
        const rule = {
          id: stableUuid({ type: 'rule', code: body.code, version: body.version }), code: body.code,
          version: body.version, plantId: topology.plantId, lineId: body.lineId,
          topologyVersion: body.topologyVersion, ast: clone(body.ast), state: 'DRAFT', revision: 1,
          checksum: sha256(body.ast), latestSimulationId: null,
          approvalId: null, approvalStatus: 'NOT_REQUESTED', approvalRevision: 0,
          approvalSubmittedBy: null, approvalSubmittedAt: null,
          approvalDecidedBy: null, approvalDecidedAt: null,
          lifecycleAction: 'NOT_PUBLISHED', lifecycleSequence: 0, lifecycleActive: false,
          publicationStatus: 'NOT_PUBLISHED',
          publicationRevision: 0, publicationAttemptCount: 0, publicationTotalAttemptCount: 0,
          publicationManualRetryCount: 0, publicationPublishedAt: null,
          publicationLastRequeuedAt: null, publicationLastError: null,
          applicationStatus: 'NOT_PUBLISHED', applicationDeploymentId: null,
          applicationObservedAt: null, applicationReceivedAt: null,
          applicationErrorCode: null, applicationErrorDetail: null,
          runtimeReadinessStatus: 'NOT_PUBLISHED', runtimeReadinessDeploymentId: null,
          runtimeReadinessObservedAt: null, runtimeReadinessReceivedAt: null,
          runtimeReadinessReasonCode: null, runtimeReadinessDetail: null,
          runtimePointCatalogEventId: null, runtimePointCatalogSourceRevision: null,
        };
        state.rules.unshift(rule);
        return rememberAndSend(state, context, res, 200, envelope(operationId, rule), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', 'getRuleVersion'), 'getRuleVersion');
        return send(res, 200, envelope('getRuleVersion', rule), 'getRuleVersion');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/compare$/);
      if (req.method === 'GET' && ids) {
        const target = state.rules.find((item) => item.id === ids[0]);
        const base = state.rules.find((item) => item.id === url.searchParams.get('against'));
        if (!target || !base) return send(res, 404, problem(404, 'Not Found', 'Rule version not found.', 'compareRuleVersions'), 'compareRuleVersions');
        if (target.code !== base.code || target.plantId !== base.plantId || target.lineId !== base.lineId) {
          return send(res, 422, problem(422, 'Validation Failed', 'Rule versions must share code and scope before comparison.', 'compareRuleVersions'), 'compareRuleVersions');
        }
        return send(res, 200, envelope('compareRuleVersions', compareVersions(
          'RULE_VERSION', base, target,
          { topologyVersion: base.topologyVersion, ast: base.ast },
          { topologyVersion: target.topologyVersion, ast: target.ast },
        )), 'compareRuleVersions');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/simulate$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'simulateRule';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const required = ['lineId', 'from', 'to', 'topologyVersion', 'calibrationVersion', 'goldenSetId'];
        const missing = required.filter((key) => body[key] === undefined);
        if (missing.length) {
          const response = problem(422, 'Validation Failed', `Missing fields: ${missing.join(', ')}.`, operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (!['DRAFT', 'SIMULATION_PASSED'].includes(rule.state)) {
          const response = problem(409, 'Invalid Rule State', `Rule cannot be simulated from state ${rule.state}.`, operationId, rule.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const simulation = {
          id: stableUuid({ type: 'simulation', ruleId: rule.id }), ruleId: rule.id, state: 'PASSED',
          checksum: sha256({ ruleChecksum: rule.checksum, input: body }),
          metrics: { matched: 42, missed: 0, falsePositive: 0, meanBoundaryErrorSeconds: 2.4 },
          inputManifest: { ...clone(body), observationCount: 18640, goldenBoundaryCount: 42 },
          emittedBoundaries: ['2026-07-12T07:59:40.000Z'],
          failureReason: null,
        };
        state.simulations.set(simulation.id, simulation);
        rule.latestSimulationId = simulation.id;
        rule.state = 'SIMULATION_PASSED';
        rule.revision += 1;
        const response = envelope(operationId, simulation);
        return rememberAndSend(state, context, res, 202, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rule-simulations\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const simulation = state.simulations.get(ids[0]);
        if (!simulation) return send(res, 404, problem(404, 'Not Found', 'Simulation not found.', 'getRuleSimulation'), 'getRuleSimulation');
        return send(res, 200, envelope('getRuleSimulation', simulation), 'getRuleSimulation');
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/submit-approval$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'submitRuleApproval';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const simulation = state.simulations.get(body.simulationId);
        if (rule.state !== 'SIMULATION_PASSED' || !body.reason || !simulation
            || simulation.state !== 'PASSED' || body.simulationChecksum !== simulation.checksum) {
          const response = problem(422, 'Simulation Proof Required', 'A passed simulation and matching checksum are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        rule.state = 'PENDING_APPROVAL';
        rule.approvalId = stableUuid({ type: 'approval', ruleId: rule.id, revision: rule.revision });
        rule.approvalStatus = 'PENDING';
        rule.approvalRevision = 1;
        rule.approvalSubmittedBy = 'simulated.bpi.engineer';
        rule.approvalSubmittedAt = FIXED_TIME;
        rule.approvalDecidedBy = null;
        rule.approvalDecidedAt = null;
        rule.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, rule), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/reject-approval$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'rejectRuleApproval';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (rule.state !== 'PENDING_APPROVAL' || !body.reason || String(body.reason).trim().length < 3) {
          const response = problem(409, 'Invalid Approval State', 'Rule must be pending approval.', operationId, rule.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        rule.state = 'DRAFT';
        rule.approvalStatus = 'REJECTED';
        rule.approvalRevision += 1;
        rule.approvalDecidedBy = 'simulated.bpi.admin';
        rule.approvalDecidedAt = FIXED_TIME;
        rule.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, rule), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/publish$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'publishRuleVersion';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const simulation = state.simulations.get(body.simulationId);
        if (rule.state !== 'PENDING_APPROVAL' || rule.approvalStatus !== 'PENDING'
            || !body.reason || !simulation || simulation.state !== 'PASSED'
            || body.simulationChecksum !== simulation.checksum) {
          const response = problem(422, 'Simulation Proof Required', 'A passed simulation and matching checksum are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        rule.state = 'PUBLISHED';
        rule.approvalStatus = 'APPROVED';
        rule.approvalRevision += 1;
        rule.approvalDecidedBy = 'simulated.bpi.admin';
        rule.approvalDecidedAt = FIXED_TIME;
        rule.lifecycleAction = 'ACTIVATE';
        rule.lifecycleSequence += 1;
        rule.lifecycleActive = true;
        rule.publicationStatus = 'PENDING';
        rule.publicationRevision = 1;
        rule.publicationAttemptCount = 0;
        rule.publicationTotalAttemptCount = 0;
        rule.publicationManualRetryCount = 0;
        rule.publicationPublishedAt = null;
        rule.publicationLastRequeuedAt = null;
        rule.publicationLastError = null;
        rule.applicationStatus = 'WAITING';
        rule.applicationDeploymentId = null;
        rule.applicationObservedAt = null;
        rule.applicationReceivedAt = null;
        rule.applicationErrorCode = null;
        rule.applicationErrorDetail = null;
        rule.runtimeReadinessStatus = 'WAITING';
        rule.runtimeReadinessDeploymentId = null;
        rule.runtimeReadinessObservedAt = null;
        rule.runtimeReadinessReceivedAt = null;
        rule.runtimeReadinessReasonCode = null;
        rule.runtimeReadinessDetail = null;
        rule.runtimePointCatalogEventId = null;
        rule.runtimePointCatalogSourceRevision = null;
        rule.revision += 1;
        const response = envelope(operationId, rule);
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/retire$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'retireRuleVersion';
        const rule = state.rules.find((item) => item.id === ids[0]);
        if (!rule) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, rule.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const readyToRetire = rule.state === 'PUBLISHED'
          && rule.lifecycleAction === 'ACTIVATE'
          && rule.lifecycleActive
          && rule.publicationStatus === 'PUBLISHED'
          && rule.applicationStatus === 'APPLIED'
          && ['READY', 'DEGRADED'].includes(rule.runtimeReadinessStatus);
        if (!body.reason || String(body.reason).trim().length < 3 || !readyToRetire) {
          const response = problem(409, 'Invalid Retirement State', 'Rule retirement requires Kafka PUBLISHED, Flink APPLIED and a known active runtime state.', operationId, rule.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        rule.state = 'RETIRED';
        rule.lifecycleAction = 'RETIRE';
        rule.lifecycleSequence += 1;
        rule.lifecycleActive = false;
        rule.publicationStatus = 'PENDING';
        rule.publicationRevision = 1;
        rule.publicationAttemptCount = 0;
        rule.publicationTotalAttemptCount = 0;
        rule.publicationManualRetryCount = 0;
        rule.publicationPublishedAt = null;
        rule.publicationLastRequeuedAt = null;
        rule.publicationLastError = null;
        rule.applicationStatus = 'WAITING';
        rule.applicationDeploymentId = null;
        rule.applicationObservedAt = null;
        rule.applicationReceivedAt = null;
        rule.applicationErrorCode = null;
        rule.applicationErrorDetail = null;
        rule.runtimeReadinessStatus = 'WAITING';
        rule.runtimeReadinessDeploymentId = null;
        rule.runtimeReadinessObservedAt = null;
        rule.runtimeReadinessReceivedAt = null;
        rule.runtimeReadinessReasonCode = null;
        rule.runtimeReadinessDetail = null;
        rule.runtimePointCatalogEventId = null;
        rule.runtimePointCatalogSourceRevision = null;
        rule.revision += 1;
        return rememberAndSend(state, context, res, 200, envelope(operationId, rule), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/rules\/([^/]+)\/publication\/retry$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'retryRulePublication';
        if (ids[0] !== state.rule.id) return send(res, 404, problem(404, 'Not Found', 'Rule not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, state.rule.publicationRevision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (!body.reason || String(body.reason).trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (state.rule.publicationStatus !== 'FAILED') {
          const response = problem(409, 'Invalid Publication State', 'Only a FAILED publication can be retried.', operationId, state.rule.publicationRevision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        state.rule.publicationStatus = 'PENDING';
        state.rule.publicationRevision += 1;
        state.rule.publicationAttemptCount = 0;
        state.rule.publicationManualRetryCount += 1;
        state.rule.publicationLastRequeuedAt = FIXED_TIME;
        state.rule.publicationLastError = null;
        return rememberAndSend(state, context, res, 200, envelope(operationId, state.rule), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/data-quality/incidents') {
        const operationId = 'listDataQualityIncidents';
        const plantId = url.searchParams.get('plantId');
        const incidentState = url.searchParams.get('state');
        const search = (url.searchParams.get('search') || '').trim().toLowerCase();
        const limit = Number(url.searchParams.get('limit') || 50);
        if (!plantId) return send(res, 422, problem(422, 'Validation Failed', 'plantId is required.', operationId), operationId);
        if (incidentState && !['OPEN', 'ACKNOWLEDGED', 'RESOLVED'].includes(incidentState)) {
          return send(res, 422, problem(422, 'Validation Failed', 'state must be OPEN, ACKNOWLEDGED or RESOLVED.', operationId), operationId);
        }
        if (!Number.isInteger(limit) || limit < 1 || limit > 200) {
          return send(res, 422, problem(422, 'Validation Failed', 'limit must be between 1 and 200.', operationId), operationId);
        }
        const filtered = scopedDataQualityIncidents(state, url)
          .filter((item) => !incidentState || item.state === incidentState)
          .filter((item) => !search || [item.issueCode, item.source, item.deviceId, item.propertyId, item.assignee, item.lastDetail]
            .filter(Boolean).some((value) => String(value).toLowerCase().includes(search)))
          .sort((left, right) => right.affectedBatchCount - left.affectedBatchCount
            || dataQualitySeverityRank(right.severity) - dataQualitySeverityRank(left.severity)
            || Date.parse(right.lastSeen) - Date.parse(left.lastSeen)
            || right.id.localeCompare(left.id));
        return send(res, 200, envelope(operationId, filtered.slice(0, limit)), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/data-quality/summary') {
        const operationId = 'getDataQualitySummary';
        if (!url.searchParams.get('plantId')) {
          return send(res, 422, problem(422, 'Validation Failed', 'plantId is required.', operationId), operationId);
        }
        return send(res, 200, envelope(operationId, dataQualitySummary(state, url)), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/data-quality\/incidents\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDataQualityIncident';
        const incident = state.dataQualityIncidents.find((item) => item.id === ids[0]);
        if (!incident) return send(res, 404, problem(404, 'Not Found', 'Incident not found.', operationId), operationId);
        return send(res, 200, envelope(operationId, {
          incident,
          events: state.dataQualityEvents.get(incident.id) || [],
          lifecycle: state.dataQualityLifecycle.get(incident.id) || [],
          recommendedActions: state.dataQualityRecommendations.get(incident.id) || [],
        }), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/data-quality\/incidents\/([^/]+)\/acknowledge$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'acknowledgeDataQualityIncident';
        const incident = state.dataQualityIncidents.find((item) => item.id === ids[0]);
        if (!incident) return send(res, 404, problem(404, 'Not Found', 'Incident not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, incident.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const assignee = String(body.assignee || '').trim();
        const reason = String(body.reason || '').trim();
        if (!assignee || assignee.length > 128 || reason.length < 3 || reason.length > 500) {
          const response = problem(422, 'Validation Failed', 'assignee and a reason of at least 3 characters are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (incident.state === 'RESOLVED') {
          const response = problem(409, 'Invalid Incident State', 'A resolved incident cannot be acknowledged.', operationId, incident.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const fromState = incident.state;
        incident.state = 'ACKNOWLEDGED';
        incident.revision += 1;
        incident.assignee = assignee;
        incident.acknowledgedBy = 'simulated.shift.lead';
        incident.acknowledgedAt = FIXED_TIME;
        incident.acknowledgmentReason = reason;
        state.dataQualityLifecycle.get(incident.id).push({
          revision: incident.revision,
          action: fromState === 'ACKNOWLEDGED' ? 'REASSIGNED' : 'ACKNOWLEDGED',
          fromState, toState: 'ACKNOWLEDGED', actorId: incident.acknowledgedBy,
          assignee, reason, at: FIXED_TIME,
        });
        return rememberAndSend(state, context, res, 200, envelope(operationId, incident), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/data-quality\/incidents\/([^/]+)\/resolve$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'resolveDataQualityIncident';
        const incident = state.dataQualityIncidents.find((item) => item.id === ids[0]);
        if (!incident) return send(res, 404, problem(404, 'Not Found', 'Incident not found.', operationId), operationId);
        const context = commandContext(req, res, operationId, incident.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const reason = String(body.reason || '').trim();
        if (reason.length < 3 || reason.length > 500) {
          const response = problem(422, 'Validation Failed', 'A reason of at least 3 characters is required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (incident.state !== 'ACKNOWLEDGED') {
          const response = problem(409, 'Invalid Incident State', 'Only an acknowledged incident can be resolved.', operationId, incident.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        incident.state = 'RESOLVED';
        incident.revision += 1;
        incident.resolvedBy = 'simulated.shift.lead';
        incident.resolvedAt = FIXED_TIME;
        incident.resolutionReason = reason;
        state.dataQualityLifecycle.get(incident.id).push({
          revision: incident.revision, action: 'RESOLVED', fromState: 'ACKNOWLEDGED', toState: 'RESOLVED',
          actorId: incident.resolvedBy, assignee: incident.assignee, reason, at: FIXED_TIME,
        });
        return rememberAndSend(state, context, res, 200, envelope(operationId, incident), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/datasets') {
        const operationId = 'listDatasets';
        const plantId = url.searchParams.get('plantId');
        const limit = Number(url.searchParams.get('limit') || 100);
        if (!plantId || !Number.isInteger(limit) || limit < 1 || limit > 200) {
          return send(res, 422, problem(422, 'Validation Failed', 'plantId and a limit between 1 and 200 are required.', operationId), operationId);
        }
        const definitions = state.datasetDefinitions
          .filter((definition) => definition.plantId === plantId)
          .sort((left, right) => right.createdAt.localeCompare(left.createdAt)
            || left.datasetCode.localeCompare(right.datasetCode))
          .slice(0, limit)
          .map((definition) => datasetDefinitionView(state, definition));
        return send(res, 200, envelope(operationId, definitions), operationId);
      }
      if (req.method === 'POST' && path === '/bpi/v1/datasets') {
        const operationId = 'createDatasetDefinition';
        const context = commandContext(req, res, operationId, 0, state, path);
        if (!context) return;
        const body = await readJson(req);
        const validated = validateDatasetDefinition(body);
        if (!validated) {
          const response = problem(422, 'Validation Failed', 'Dataset definition violates the immutable Phase 3A contract.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (state.datasetDefinitions.some((definition) => definition.datasetCode === body.datasetCode
            && definition.version === body.version)) {
          const response = problem(409, 'Dataset Definition Exists', 'datasetCode and version already exist.', operationId, 0);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const controlled = {
          datasetCode: body.datasetCode,
          version: body.version,
          name: body.name,
          plantId: body.plantId,
          lineIds: validated.lineIds,
          predictionTimePolicy: body.predictionTimePolicy,
          featureCutoffPolicy: body.featureCutoffPolicy,
          featureRefs: validated.featureRefs,
          processSignalWindows: validated.processSignalWindows,
          labelRefs: validated.labelRefs,
          maxLabelDelayHours: body.maxLabelDelayHours,
          minimumConfidence: validated.confidence,
          splitPolicy: body.splitPolicy,
        };
        const definition = {
          id: stableUuid({ type: 'dataset-definition', code: body.datasetCode, version: body.version }),
          ...controlled,
          tenantId: FEATURE_FLAG_TENANT,
          state: 'ACTIVE',
          revision: 1,
          checksum: sha256(controlled),
          createdBy: 'simulated.data.engineer',
          createReason: body.reason,
          createdAt: FIXED_TIME,
        };
        state.datasetDefinitions.push(definition);
        const response = envelope(operationId, datasetDefinitionView(state, definition));
        return rememberAndSend(state, context, res, 200, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/datasets\/([^/]+)\/snapshots$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'createDatasetSnapshot';
        const definition = state.datasetDefinitions.find((item) => item.id === ids[0]);
        if (!definition) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset definition not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, definition.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        const freezeAtMs = Date.parse(body.freezeAt);
        const lineIds = normalizeDatasetStrings(body.lineIds);
        const requestedRuleIds = body.ruleVersionIds === undefined || body.ruleVersionIds === null
          ? [] : body.ruleVersionIds;
        const ruleVersionIds = Array.isArray(requestedRuleIds) && requestedRuleIds.length === 0
          ? [] : normalizeDatasetStrings(requestedRuleIds);
        const validRules = ruleVersionIds !== null
          && ruleVersionIds.every((id) => /^[0-9a-f-]{36}$/i.test(id));
        if (!Number.isFinite(freezeAtMs) || freezeAtMs > Date.now() || !lineIds || !validRules
            || !lineIds.every((lineId) => definition.lineIds.includes(lineId))
            || body.predictionTimePolicy !== definition.predictionTimePolicy
            || !String(body.reason || '').trim()) {
          const response = problem(422, 'Validation Failed', 'Snapshot selection violates the immutable dataset definition.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const selectedRules = new Set(ruleVersionIds);
        const eligibleRuns = state.shadowRuns.filter((run) => run.state === 'APPROVED'
          && lineIds.includes(run.lineId) && Date.parse(run.decidedAt) <= freezeAtMs
          && (!selectedRules.size || selectedRules.has(run.ruleVersionId))
          && state.shadowRunReviews.some((review) => review.shadowRunId === run.id
            && Date.parse(review.reviewedAt) <= freezeAtMs
            && (review.state === 'ACTIVE' || Date.parse(review.supersededAt) > freezeAtMs)));
        const eligibleLines = new Set(eligibleRuns.map((run) => run.lineId));
        const eligibleRules = new Set(eligibleRuns.map((run) => run.ruleVersionId));
        if (!lineIds.every((lineId) => eligibleLines.has(lineId))
            || !ruleVersionIds.every((ruleVersionId) => eligibleRules.has(ruleVersionId))) {
          const response = problem(422, 'Approved Shadow Evidence Required', 'Every selected line and rule requires approved active reviews at freezeAt.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const snapshotVersion = state.datasetSnapshots
          .filter((snapshot) => snapshot.datasetId === definition.id)
          .reduce((maximum, snapshot) => Math.max(maximum, snapshot.snapshotVersion), 0) + 1;
        const snapshot = {
          id: stableUuid({ type: 'dataset-snapshot', datasetId: definition.id, snapshotVersion }),
          datasetId: definition.id,
          datasetCode: definition.datasetCode,
          datasetVersion: definition.version,
          datasetName: definition.name,
          tenantId: definition.tenantId,
          plantId: definition.plantId,
          snapshotVersion,
          state: 'QUEUED',
          revision: 1,
          freezeAt: new Date(freezeAtMs).toISOString(),
          lineIds,
          predictionTimePolicy: body.predictionTimePolicy,
          ruleVersionIds,
          excludeLowConfidence: body.excludeLowConfidence !== false,
          definitionChecksum: definition.checksum,
          manifestSchemaVersion: null,
          manifestChecksum: null,
          manifest: null,
          includedCount: null,
          excludedCount: null,
          exclusionSummary: null,
          materializationState: 'NOT_STARTED',
          artifactUri: null,
          requestedBy: 'simulated.data.engineer',
          requestReason: body.reason,
          createdAt: FIXED_TIME,
          startedAt: null,
          completedAt: null,
          attemptCount: 0,
          failureCode: null,
          failureDetail: null,
        };
        state.datasetSnapshots.push(snapshot);
        state.pendingDatasetSnapshotIds.add(snapshot.id);
        const response = envelope(operationId, datasetSnapshotView(state, snapshot));
        return rememberAndSend(state, context, res, 202, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-snapshots\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetSnapshot';
        const snapshot = state.datasetSnapshots.find((item) => item.id === ids[0]);
        if (!snapshot) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset snapshot not found.', operationId), operationId);
        }
        if (state.pendingDatasetSnapshotIds.has(snapshot.id)) buildDatasetSnapshot(state, snapshot);
        return send(res, 200, envelope(operationId, datasetSnapshotView(state, snapshot)), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-snapshots\/([^/]+)\/materializations$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'requestDatasetMaterialization';
        const snapshot = state.datasetSnapshots.find((item) => item.id === ids[0]);
        if (!snapshot) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset snapshot not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, snapshot.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (snapshot.state !== 'MANIFEST_READY' || !snapshot.manifestChecksum) {
          const response = problem(409, 'Manifest Required', 'Dataset materialization requires a MANIFEST_READY snapshot.', operationId, snapshot.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        if (body.artifactFormat !== 'PARQUET' || String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'Phase 3B-A supports PARQUET and requires a reason.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const existing = state.datasetMaterializations.find((item) => item.snapshotId === snapshot.id
          && item.artifactFormat === 'PARQUET'
          && item.artifactSchemaVersion === DATASET_ARTIFACT_SCHEMA_VERSION
          && item.materializerVersion === DATASET_MATERIALIZER_VERSION);
        if (existing) {
          const response = problem(409, 'Materialization Exists', 'The versioned Parquet materialization contract already exists.', operationId, existing.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const materialization = {
          id: stableUuid({ type: 'dataset-materialization', snapshotId: snapshot.id, format: 'PARQUET' }),
          snapshotId: snapshot.id,
          datasetId: snapshot.datasetId,
          datasetCode: snapshot.datasetCode,
          datasetVersion: snapshot.datasetVersion,
          tenantId: snapshot.tenantId,
          plantId: snapshot.plantId,
          lineIds: clone(snapshot.lineIds),
          artifactFormat: 'PARQUET',
          artifactSchemaVersion: DATASET_ARTIFACT_SCHEMA_VERSION,
          materializerVersion: DATASET_MATERIALIZER_VERSION,
          state: 'QUEUED',
          revision: 1,
          manifestChecksum: snapshot.manifestChecksum,
          requestedBy: 'simulated.data.engineer',
          requestReason: String(body.reason).trim(),
          createdAt: FIXED_TIME,
          startedAt: null,
          completedAt: null,
          attemptCount: 0,
          artifactUri: null,
          objectBucket: null,
          objectKey: null,
          contentSha256: null,
          byteSize: null,
          rowCount: null,
          schema: null,
          artifactMetadata: null,
          failureCode: null,
          failureDetail: null,
        };
        state.datasetMaterializations.push(materialization);
        state.pendingDatasetMaterializationIds.add(materialization.id);
        const response = envelope(operationId, materialization);
        return rememberAndSend(state, context, res, 202, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-materializations\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetMaterialization';
        const materialization = state.datasetMaterializations.find((item) => item.id === ids[0]);
        if (!materialization) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset materialization not found.', operationId), operationId);
        }
        progressDatasetMaterialization(state, materialization);
        return send(res, 200, envelope(operationId, materialization), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-materializations\/([^/]+)\/catalog-publications$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetCatalogPublicationForMaterialization';
        const publication = latestDatasetCatalogPublication(state, ids[0]);
        if (!publication) {
          return send(res, 200, envelope(operationId, null), operationId);
        }
        return send(res, 200, envelope(operationId, publication), operationId);
      }
      if (req.method === 'POST' && ids) {
        const operationId = 'requestDatasetCatalogPublication';
        const materialization = state.datasetMaterializations.find((item) => item.id === ids[0]);
        if (!materialization) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset materialization not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, materialization.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'Catalog publication reason must contain at least three characters.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const objectVersionId = materialization.artifactMetadata?.objectVersionId;
        if (materialization.state !== 'READY' || !materialization.contentSha256
            || !objectVersionId || !materialization.schema || materialization.rowCount <= 0) {
          const response = problem(409, 'Verified Parquet Required', 'Catalog publication requires a verified non-empty Parquet object version.', operationId, materialization.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const existing = latestDatasetCatalogPublication(state, materialization.id);
        if (existing) {
          const response = problem(409, 'Catalog Publication Exists', 'The publisher contract already exists for this materialization.', operationId, existing.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const catalogNamespace = `bpi_training.tenant_${sha256(materialization.tenantId).slice(0, 16)}`;
        const tableName = `dataset_${materialization.datasetId.replaceAll('-', '')}`;
        const publication = {
          id: stableUuid({ type: 'dataset-catalog-publication', materializationId: materialization.id }),
          materializationId: materialization.id,
          snapshotId: materialization.snapshotId,
          datasetId: materialization.datasetId,
          datasetCode: materialization.datasetCode,
          datasetVersion: materialization.datasetVersion,
          tenantId: materialization.tenantId,
          plantId: materialization.plantId,
          lineIds: clone(materialization.lineIds),
          catalogName: DATASET_CATALOG_NAME,
          catalogNamespace,
          tableName,
          tableIdentifier: `${DATASET_CATALOG_NAME}.${catalogNamespace}.${tableName}`,
          publisherVersion: DATASET_CATALOG_PUBLISHER_VERSION,
          state: 'QUEUED',
          revision: 1,
          manifestChecksum: materialization.manifestChecksum,
          sourceContentSha256: materialization.contentSha256,
          sourceObjectVersionId: objectVersionId,
          sourceByteSize: materialization.byteSize,
          sourceRowCount: materialization.rowCount,
          sourceSchema: clone(materialization.schema),
          requestedBy: 'simulated.data.engineer',
          requestReason: String(body.reason).trim(),
          createdAt: FIXED_TIME,
          startedAt: null,
          completedAt: null,
          attemptCount: 0,
          icebergSnapshotId: null,
          icebergMetadataLocation: null,
          icebergSchemaId: null,
          icebergPartitionSpecId: null,
          verifiedRowCount: null,
          semanticChecksum: null,
          catalogMetadata: null,
          failureCode: null,
          failureDetail: null,
        };
        state.datasetCatalogPublications.push(publication);
        state.pendingDatasetCatalogPublicationIds.add(publication.id);
        return rememberAndSend(state, context, res, 202, envelope(operationId, publication), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-materializations\/([^/]+)\/retry$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'retryDatasetMaterialization';
        const materialization = state.datasetMaterializations.find((item) => item.id === ids[0]);
        if (!materialization) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset materialization not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, materialization.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'Retry reason must contain at least three characters.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (materialization.state !== 'FAILED') {
          const response = problem(409, 'Invalid Materialization State', 'Only a FAILED dataset materialization can be retried.', operationId, materialization.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        materialization.state = 'QUEUED';
        materialization.revision += 1;
        materialization.startedAt = null;
        materialization.completedAt = null;
        materialization.failureCode = null;
        materialization.failureDetail = null;
        state.pendingDatasetMaterializationIds.add(materialization.id);
        const response = envelope(operationId, materialization);
        return rememberAndSend(state, context, res, 202, response, operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-catalog-publications\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetCatalogPublication';
        const publication = state.datasetCatalogPublications.find((item) => item.id === ids[0]);
        if (!publication) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset catalog publication not found.', operationId), operationId);
        }
        progressDatasetCatalogPublication(state, publication);
        return send(res, 200, envelope(operationId, publication), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-catalog-publications\/([^/]+)\/retry$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'retryDatasetCatalogPublication';
        const publication = state.datasetCatalogPublications.find((item) => item.id === ids[0]);
        if (!publication) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset catalog publication not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, publication.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'Retry reason must contain at least three characters.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (publication.state !== 'FAILED') {
          const response = problem(409, 'Invalid Catalog Publication State', 'Only a FAILED dataset catalog publication can be retried.', operationId, publication.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        publication.state = 'QUEUED';
        publication.revision += 1;
        publication.startedAt = null;
        publication.completedAt = null;
        publication.failureCode = null;
        publication.failureDetail = null;
        state.pendingDatasetCatalogPublicationIds.add(publication.id);
        return rememberAndSend(state, context, res, 202, envelope(operationId, publication), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-catalog-publications\/([^/]+)\/retention-archives$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetRetentionArchiveForPublication';
        const publication = state.datasetCatalogPublications.find((item) => item.id === ids[0]);
        if (!publication) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset catalog publication not found.', operationId), operationId);
        }
        return send(res, 200, envelope(operationId, latestDatasetRetentionArchive(state, publication.id)), operationId);
      }
      if (req.method === 'POST' && ids) {
        const operationId = 'requestDatasetRetentionArchive';
        const publication = state.datasetCatalogPublications.find((item) => item.id === ids[0]);
        if (!publication) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset catalog publication not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, publication.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'Retention archive reason must contain at least three characters.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const catalogVerified = publication.catalogMetadata?.catalogSnapshotVerified === true;
        if (publication.state !== 'READY' || !catalogVerified
            || !publication.icebergSnapshotId || !publication.icebergMetadataLocation
            || publication.verifiedRowCount !== publication.sourceRowCount
            || !publication.semanticChecksum) {
          const response = problem(409, 'Verified Catalog Publication Required', 'Retention archive requires a reconciled READY Iceberg publication.', operationId, publication.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const existing = latestDatasetRetentionArchive(state, publication.id);
        if (existing) {
          const response = problem(409, 'Retention Archive Exists', 'The retention archive contract already exists for this publication.', operationId, existing.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const archive = {
          id: stableUuid({ type: 'dataset-retention-archive', publicationId: publication.id }),
          catalogPublicationId: publication.id,
          materializationId: publication.materializationId,
          snapshotId: publication.snapshotId,
          datasetId: publication.datasetId,
          datasetCode: publication.datasetCode,
          datasetVersion: publication.datasetVersion,
          tenantId: publication.tenantId,
          plantId: publication.plantId,
          lineIds: clone(publication.lineIds),
          archiverVersion: DATASET_RETENTION_ARCHIVER_VERSION,
          archiveProfile: DATASET_RECOVERY_PROFILE,
          state: 'QUEUED',
          revision: 1,
          manifestChecksum: publication.manifestChecksum,
          sourceContentSha256: publication.sourceContentSha256,
          sourceObjectVersionId: publication.sourceObjectVersionId,
          sourceByteSize: publication.sourceByteSize,
          sourceRowCount: publication.sourceRowCount,
          sourceSchema: clone(publication.sourceSchema),
          tableIdentifier: publication.tableIdentifier,
          icebergSnapshotId: publication.icebergSnapshotId,
          icebergMetadataLocation: publication.icebergMetadataLocation,
          icebergSchemaId: publication.icebergSchemaId,
          icebergPartitionSpecId: publication.icebergPartitionSpecId,
          catalogVerifiedRowCount: publication.verifiedRowCount,
          catalogSemanticChecksum: publication.semanticChecksum,
          requestedBy: 'simulated.data.engineer',
          requestReason: String(body.reason).trim(),
          createdAt: FIXED_TIME,
          startedAt: null,
          completedAt: null,
          attemptCount: 0,
          retentionMode: null,
          retainUntil: null,
          legalHoldEnabled: null,
          archiveBucket: null,
          archivePrefix: null,
          sourceArchiveObjectKey: null,
          sourceArchiveVersionId: null,
          archiveManifestObjectKey: null,
          archiveManifestVersionId: null,
          archiveManifestSha256: null,
          archiveObjectCount: null,
          archiveTotalBytes: null,
          verifiedRowCount: null,
          verifiedSemanticChecksum: null,
          archiveMetadata: null,
          failureCode: null,
          failureDetail: null,
        };
        state.datasetRetentionArchives.push(archive);
        state.pendingDatasetRetentionArchiveIds.add(archive.id);
        return rememberAndSend(state, context, res, 202, envelope(operationId, archive), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-retention-archives\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetRetentionArchive';
        const archive = state.datasetRetentionArchives.find((item) => item.id === ids[0]);
        if (!archive) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset retention archive not found.', operationId), operationId);
        }
        progressDatasetRetentionArchive(state, archive);
        return send(res, 200, envelope(operationId, archive), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-retention-archives\/([^/]+)\/retry$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'retryDatasetRetentionArchive';
        const archive = state.datasetRetentionArchives.find((item) => item.id === ids[0]);
        if (!archive) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset retention archive not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, archive.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'Retry reason must contain at least three characters.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (archive.state !== 'FAILED') {
          const response = problem(409, 'Invalid Retention Archive State', 'Only a FAILED dataset retention archive can be retried.', operationId, archive.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        archive.state = 'QUEUED';
        archive.revision += 1;
        archive.startedAt = null;
        archive.completedAt = null;
        archive.failureCode = null;
        archive.failureDetail = null;
        state.pendingDatasetRetentionArchiveIds.add(archive.id);
        return rememberAndSend(state, context, res, 202, envelope(operationId, archive), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-retention-archives\/([^/]+)\/mlflow-registrations$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetMlflowRegistrationForArchive';
        const archive = state.datasetRetentionArchives.find((item) => item.id === ids[0]);
        if (!archive) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset retention archive not found.', operationId), operationId);
        }
        return send(res, 200, envelope(
          operationId, latestDatasetMlflowRegistration(state, archive.id),
        ), operationId);
      }
      if (req.method === 'POST' && ids) {
        const operationId = 'requestDatasetMlflowRegistration';
        const archive = state.datasetRetentionArchives.find((item) => item.id === ids[0]);
        if (!archive) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset retention archive not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, archive.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'MLflow registration reason must contain at least three characters.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        const recoveryVerified = archive.archiveMetadata?.objectLockVerified === true
          && archive.archiveMetadata?.recoveryVerified === true;
        const sourceFactsComplete = archive.archiveBucket && archive.sourceArchiveObjectKey
          && archive.sourceArchiveVersionId && archive.archiveManifestObjectKey
          && archive.archiveManifestVersionId && archive.archiveManifestSha256
          && archive.catalogSemanticChecksum && archive.sourceSchema
          && archive.verifiedRowCount === archive.sourceRowCount
          && archive.verifiedSemanticChecksum === archive.catalogSemanticChecksum;
        if (archive.state !== 'LOCKED' || !recoveryVerified || !sourceFactsComplete) {
          const response = problem(409, 'Verified Recovery Archive Required', 'MLflow Dataset Input registration requires a LOCKED, recovery-verified exact archive version.', operationId, archive.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const existing = latestDatasetMlflowRegistration(state, archive.id);
        if (existing) {
          const response = problem(409, 'MLflow Registration Exists', 'The registrar contract already exists for this recovery archive.', operationId, existing.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const registration = {
          id: stableUuid({ type: 'dataset-mlflow-registration', archiveId: archive.id }),
          retentionArchiveId: archive.id,
          catalogPublicationId: archive.catalogPublicationId,
          materializationId: archive.materializationId,
          snapshotId: archive.snapshotId,
          datasetId: archive.datasetId,
          datasetCode: archive.datasetCode,
          datasetVersion: archive.datasetVersion,
          tenantId: archive.tenantId,
          plantId: archive.plantId,
          lineIds: clone(archive.lineIds),
          registrarVersion: DATASET_MLFLOW_REGISTRAR_VERSION,
          trackingProfile: DATASET_MLFLOW_TRACKING_PROFILE,
          state: 'QUEUED',
          revision: 1,
          manifestChecksum: archive.manifestChecksum,
          sourceContentSha256: archive.sourceContentSha256,
          sourceObjectVersionId: archive.sourceObjectVersionId,
          sourceByteSize: archive.sourceByteSize,
          sourceRowCount: archive.sourceRowCount,
          sourceSchema: clone(archive.sourceSchema),
          tableIdentifier: archive.tableIdentifier,
          icebergSnapshotId: archive.icebergSnapshotId,
          catalogSemanticChecksum: archive.catalogSemanticChecksum,
          archiveBucket: archive.archiveBucket,
          sourceArchiveObjectKey: archive.sourceArchiveObjectKey,
          sourceArchiveVersionId: archive.sourceArchiveVersionId,
          archiveManifestObjectKey: archive.archiveManifestObjectKey,
          archiveManifestVersionId: archive.archiveManifestVersionId,
          archiveManifestSha256: archive.archiveManifestSha256,
          experimentName: `ft-mes-bpi-training-candidates-${archive.tenantId.replace(/[^A-Za-z0-9_.-]/g, '_')}`,
          datasetName: archive.datasetCode,
          datasetDigest: archive.catalogSemanticChecksum.slice(0, 16),
          requestedBy: 'simulated.data.engineer',
          requestReason: String(body.reason).trim(),
          createdAt: FIXED_TIME,
          startedAt: null,
          completedAt: null,
          attemptCount: 0,
          mlflowExperimentId: null,
          mlflowRunId: null,
          mlflowArtifactUri: null,
          mlflowDatasetSource: null,
          registrationMetadata: null,
          failureCode: null,
          failureDetail: null,
        };
        state.datasetMlflowRegistrations.push(registration);
        state.pendingDatasetMlflowRegistrationIds.add(registration.id);
        return rememberAndSend(state, context, res, 202, envelope(operationId, registration), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-mlflow-registrations\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetMlflowRegistration';
        const registration = state.datasetMlflowRegistrations.find((item) => item.id === ids[0]);
        if (!registration) {
          return send(res, 404, problem(404, 'Not Found', 'MLflow dataset registration not found.', operationId), operationId);
        }
        progressDatasetMlflowRegistration(state, registration);
        return send(res, 200, envelope(operationId, registration), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-mlflow-registrations\/([^/]+)\/retry$/);
      if (req.method === 'POST' && ids) {
        const operationId = 'retryDatasetMlflowRegistration';
        const registration = state.datasetMlflowRegistrations.find((item) => item.id === ids[0]);
        if (!registration) {
          return send(res, 404, problem(404, 'Not Found', 'MLflow dataset registration not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, registration.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'Retry reason must contain at least three characters.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (registration.state !== 'FAILED') {
          const response = problem(409, 'Invalid MLflow Registration State', 'Only a FAILED MLflow dataset registration can be retried.', operationId, registration.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        registration.state = 'QUEUED';
        registration.revision += 1;
        registration.startedAt = null;
        registration.completedAt = null;
        registration.mlflowExperimentId = null;
        registration.mlflowRunId = null;
        registration.mlflowArtifactUri = null;
        registration.mlflowDatasetSource = null;
        registration.registrationMetadata = null;
        registration.failureCode = null;
        registration.failureDetail = null;
        state.pendingDatasetMlflowRegistrationIds.add(registration.id);
        return rememberAndSend(state, context, res, 202, envelope(operationId, registration), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-mlflow-registrations\/([^/]+)\/training-readiness-assessments$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getLatestDatasetTrainingReadinessAssessment';
        const registration = state.datasetMlflowRegistrations.find((item) => item.id === ids[0]);
        if (!registration) {
          return send(res, 404, problem(404, 'Not Found', 'MLflow dataset registration not found.', operationId), operationId);
        }
        return send(res, 200, envelope(
          operationId, latestDatasetTrainingReadiness(state, registration.id),
        ), operationId);
      }
      if (req.method === 'POST' && ids) {
        const operationId = 'assessDatasetTrainingReadiness';
        const registration = state.datasetMlflowRegistrations.find((item) => item.id === ids[0]);
        if (!registration) {
          return send(res, 404, problem(404, 'Not Found', 'MLflow dataset registration not found.', operationId), operationId);
        }
        const context = commandContext(req, res, operationId, registration.revision, state, path);
        if (!context) return;
        const body = await readJson(req);
        if (body.objectiveCode !== DATASET_TRAINING_OBJECTIVE
          || String(body.reason || '').trim().length < 3) {
          const response = problem(422, 'Validation Failed', 'A supported objective and assessment reason are required.', operationId);
          return rememberAndSend(state, context, res, 422, response, operationId);
        }
        if (registration.state !== 'REGISTERED') {
          const response = problem(409, 'Verified Dataset Input Required', 'Training readiness requires a REGISTERED MLflow Dataset Input.', operationId, registration.revision);
          return rememberAndSend(state, context, res, 409, response, operationId);
        }
        const latest = latestDatasetTrainingReadiness(state, registration.id);
        const assessment = buildDatasetTrainingReadiness(
          state, registration, (latest?.assessmentSequence || 0) + 1,
          String(body.reason).trim(),
        );
        state.datasetTrainingReadinessAssessments.push(assessment);
        return rememberAndSend(state, context, res, 200, envelope(operationId, assessment), operationId);
      }
      ids = match(path, /^\/bpi\/v1\/dataset-training-readiness-assessments\/([^/]+)$/);
      if (req.method === 'GET' && ids) {
        const operationId = 'getDatasetTrainingReadinessAssessment';
        const assessment = state.datasetTrainingReadinessAssessments.find((item) => item.id === ids[0]);
        if (!assessment) {
          return send(res, 404, problem(404, 'Not Found', 'Dataset training readiness assessment not found.', operationId), operationId);
        }
        return send(res, 200, envelope(operationId, assessment), operationId);
      }
      if (req.method === 'GET' && path === '/bpi/v1/integrations/health') {
        return send(res, 200, envelope('getIntegrationHealth', state.integrations), 'getIntegrationHealth');
      }
      return send(res, 404, problem(404, 'Not Found', 'Route is not implemented by the Phase 1 simulator.', 'unknownOperation'), 'unknownOperation');
    } catch (error) {
      return send(res, 400, problem(400, 'Bad Request', error.message, 'requestParsing'), 'requestParsing');
    }
  };
}

function createBpiSimulator() {
  const state = createScenario();
  const server = http.createServer(createHandler(state));
  return { server, state };
}

function listen(server, port = 0, host = '127.0.0.1') {
  return new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(port, host, () => resolve(server.address()));
  });
}

if (require.main === module) {
  const { server } = createBpiSimulator();
  const port = Number(process.env.BPI_SIM_PORT || 19090);
  listen(server, port).then((address) => {
    console.log(`BPI Phase 1 simulator listening on http://${address.address}:${address.port}`);
  }).catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
}

module.exports = { createBpiSimulator, listen };

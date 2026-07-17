#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");

const sshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "8";
const eamContainer = process.env.ADP_EAM_CONTAINER || "adp-mes-newbase-EamMs-1";
const kafkaContainer = process.env.ADP_KAFKA_CONTAINER || "adp-mes-newbase-kafka-1";
const postgresContainer =
  process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const topic = process.env.ADP_PATROL_GATHER_TOPIC || "topic.kafka.PATROL.gatherData";
const zookeeperConnect = process.env.ADP_ZOOKEEPER_CONNECT || "zookeeper:2181";
const taskId = process.env.ADP_PATROL_TASK_ID || "6676867595027280";
const workItemId = process.env.ADP_PATROL_WORK_ITEM_ID || "6675549930308432";
const username = process.env.ADP_USERNAME || "admin";
const companyId = Number(process.env.ADP_COMPANY_ID || "1000");
const waitMs = Number(process.env.ADP_PATROL_GATHER_WAIT_MS || "15000");
const expectedEamSha = process.env.ADP_EXPECTED_EAM_SHA256 || "";
const outputPath =
  process.env.ADP_PATROL_GATHER_OUTPUT ||
  path.join("metadata", "patrol-gather-data-runtime-acceptance.json");
const producerPath =
  "/opt/adp/bap-server/assembly/kafka_2/bin/kafka-console-producer.sh";

function requirePattern(label, value, pattern) {
  if (!pattern.test(value)) {
    throw new Error(`${label} contains unsupported characters: ${value}`);
  }
}

requirePattern("SSH target", sshTarget, /^[A-Za-z0-9_.@-]+$/);
requirePattern("EamMs container", eamContainer, /^[A-Za-z0-9_.-]+$/);
requirePattern("Kafka container", kafkaContainer, /^[A-Za-z0-9_.-]+$/);
requirePattern("PostgreSQL container", postgresContainer, /^[A-Za-z0-9_.-]+$/);
requirePattern("database", dbName, /^[A-Za-z0-9_-]+$/);
requirePattern("database user", dbUser, /^[A-Za-z0-9_-]+$/);
requirePattern("Kafka topic", topic, /^[A-Za-z0-9_.-]+$/);
requirePattern("ZooKeeper address", zookeeperConnect, /^[A-Za-z0-9_.:-]+$/);
requirePattern("task id", taskId, /^\d+$/);
requirePattern("work-item id", workItemId, /^\d+$/);
if (!Number.isInteger(companyId) || companyId <= 0) {
  throw new Error(`invalid company id: ${companyId}`);
}
if (!Number.isFinite(waitMs) || waitMs < 0 || waitMs > 120000) {
  throw new Error(`invalid gather wait: ${waitMs}`);
}
if (expectedEamSha) {
  requirePattern("expected EamMs SHA-256", expectedEamSha, /^[a-f0-9]{64}$/i);
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, `'"'"'`)}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function run(file, args, options = {}) {
  return execFileSync(file, args, {
    encoding: "utf8",
    maxBuffer: 32 * 1024 * 1024,
    stdio: [options.input == null ? "ignore" : "pipe", "pipe", "pipe"],
    input: options.input,
  }).trim();
}

function ssh(command, input) {
  return run(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      `ConnectTimeout=${sshConnectTimeout}`,
      sshTarget,
      command,
    ],
    { input }
  );
}

function query(sql) {
  const command = [
    "docker exec",
    postgresContainer,
    "psql",
    "-v ON_ERROR_STOP=1",
    `-U ${dbUser}`,
    `-d ${dbName}`,
    "-At",
    `-F ${shellQuote("|")}`,
    `-c ${shellQuote(sql)}`,
  ].join(" ");
  return ssh(command);
}

function sleep(milliseconds) {
  return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

function parseDetail(line) {
  const fields = line.split("|");
  if (fields.length < 7) {
    throw new Error(`unexpected PATROL detail row: ${line}`);
  }
  return {
    id: fields[0],
    patrolTaskId: fields[1],
    workItemId: fields[2],
    companyId: Number(fields[3]),
    itemNumber: fields[4],
    completeDate: fields[5],
    gatherData: fields[6] || null,
  };
}

function parseTagCounts(line) {
  const [total, valid, enabled, valued, matching] = line.split("|").map(Number);
  if ([total, valid, enabled, valued, matching].some((value) => !Number.isFinite(value))) {
    throw new Error(`unexpected TagManagement counts: ${line}`);
  }
  return { total, valid, enabled, valued, matching };
}

function relevantLogLines(logs) {
  const pattern =
    /(PATROL gather-data|TagManagement history|调用测点接口参数|调用测点接口返回结果|调用测点接口失败|调用测点模块接口失败|authenticated user|fallback public key|because parse access token failed|模拟登陆失败)/;
  return logs
    .split(/\r?\n/)
    .filter((line) => pattern.test(line))
    .map((line) => line.slice(0, 1200));
}

function readTopicEndOffset() {
  const output = ssh(
    `docker exec ${kafkaContainer} ${producerPath.replace(
      "kafka-console-producer.sh",
      "kafka-run-class.sh"
    )} kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic ${topic} --time -1`
  );
  const line = output.split(/\r?\n/).find((candidate) => candidate.startsWith(`${topic}:`));
  const match = line && line.match(/:(\d+)$/);
  if (!match) {
    throw new Error(`cannot parse Kafka topic end offset: ${output}`);
  }
  return Number(match[1]);
}

function findConsumerGroup(assignmentEvidence) {
  const pattern = new RegExp(`${topic.replace(/\./g, "\\.")}\\.(anonymous\\.[^.]+)\\.errors`, "g");
  const matches = [...assignmentEvidence.matchAll(pattern)];
  return matches.length ? matches[matches.length - 1][1] : null;
}

function readConsumerPosition(group) {
  if (!group) {
    return null;
  }
  requirePattern("Kafka consumer group", group, /^[A-Za-z0-9_.-]+$/);
  const output = ssh(
    `docker exec ${kafkaContainer} ${producerPath.replace(
      "kafka-console-producer.sh",
      "kafka-consumer-groups.sh"
    )} --bootstrap-server localhost:9092 --describe --group ${group} 2>&1`
  );
  const row = output
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line.startsWith(`${topic} `));
  if (!row) {
    return { group, raw: output.split(/\r?\n/).filter(Boolean) };
  }
  const fields = row.split(/\s+/);
  return {
    group,
    partition: Number(fields[1]),
    currentOffset: Number(fields[2]),
    logEndOffset: Number(fields[3]),
    lag: Number(fields[4]),
  };
}

async function main() {
  const repoCommit = run("git", ["rev-parse", "HEAD"]);
  const generatedAt = new Date().toISOString();
  const initialState = JSON.parse(ssh(`docker inspect ${eamContainer}`))[0];
  const deployedSha = ssh(
    `sha256sum /home/v6/adp-mes-docker-newbase-20260611-181921/runtime/bap-server/module-Server/EamMs/manual/EamMs-1.0.0.jar | cut -d' ' -f1`
  );
  const topicDescription = ssh(
    `docker exec ${kafkaContainer} /opt/adp/bap-server/assembly/kafka_2/bin/kafka-topics.sh --describe --zookeeper ${zookeeperConnect} --topic ${topic}`
  );
  const assignmentEvidence = ssh(
    `docker logs --since ${shellQuote(initialState.State.StartedAt)} ${eamContainer} 2>&1 | grep -E ${shellQuote(
      "PATROL.gatherData|topic.kafka.PATROL.gatherData"
    )} | tail -n 30`
  );
  const consumerGroup = findConsumerGroup(assignmentEvidence);
  const topicEndOffsetBefore = readTopicEndOffset();
  const consumerPositionBefore = readConsumerPosition(consumerGroup);

  const detailSql = `SELECT id,patrol_task,work_item_id,cid,item_number,complete_date,gather_data
FROM mp_task_details
WHERE patrol_task=${taskId} AND work_item_id=${workItemId}
ORDER BY id DESC
LIMIT 1;`;
  const before = parseDetail(query(detailSql));
  const tagSql = `SELECT
  count(*),
  count(*) FILTER (WHERE valid),
  count(*) FILTER (WHERE is_enabled),
  count(*) FILTER (WHERE real_value IS NOT NULL AND btrim(real_value) <> ''),
  count(*) FILTER (WHERE valid AND is_enabled AND tag_name=${sqlLiteral(before.itemNumber)})
FROM tmm_tags;`;
  const tagCounts = parseTagCounts(query(tagSql));

  const marker = `ADP_GATHER_${Date.now()}`;
  const payloads = [
    {
      case: "empty-data",
      expectedLog: "Ignore empty PATROL gather-data message",
      body: { userName: username, companyId, data: [] },
    },
    {
      case: "invalid-task-id",
      expectedLog: "invalid task id",
      body: {
        userName: username,
        companyId,
        data: [{ taskId: marker, workItemIds: [workItemId] }],
      },
    },
    {
      case: "invalid-work-item-list",
      expectedLog: "invalid work-item list",
      body: {
        userName: username,
        companyId,
        data: [{ taskId, workItemIds: marker }],
      },
    },
    {
      case: "invalid-work-item-id",
      expectedLog: "invalid work-item id",
      body: {
        userName: username,
        companyId,
        data: [{ taskId, workItemIds: [marker] }],
      },
    },
    {
      case: "valid-completed-detail",
      expectedLog: "调用测点接口参数",
      body: {
        userName: username,
        companyId,
        data: [{ taskId, workItemIds: [workItemId] }],
      },
    },
  ];

  const logSince = new Date(Date.now() - 1000).toISOString();
  const producerCommand =
    `docker exec -i ${kafkaContainer} ${producerPath}` +
    ` --broker-list localhost:9092 --topic ${topic}`;
  for (const testCase of payloads) {
    ssh(producerCommand, `${JSON.stringify(testCase.body)}\n`);
  }
  await sleep(waitMs);

  const logs = ssh(
    `docker logs --since ${shellQuote(logSince)} ${eamContainer} 2>&1`
  );
  const after = parseDetail(query(detailSql));
  const finalState = JSON.parse(ssh(`docker inspect ${eamContainer}`))[0];
  const topicEndOffsetAfter = readTopicEndOffset();
  const consumerPositionAfter = readConsumerPosition(consumerGroup);
  const lines = relevantLogLines(logs);
  const caseResults = payloads.map((testCase) => ({
    case: testCase.case,
    expectedLog: testCase.expectedLog,
    observed: logs.includes(testCase.expectedLog),
  }));
  const validLookupLine = lines.find(
    (line) => line.includes("调用测点接口参数") && line.includes(before.itemNumber)
  );
  const usesTwentyFourHourWindow = Boolean(
    validLookupLine &&
      /"(?:startTime|endTime)":"\d{4}-\d{2}-\d{2} (?:[01]\d|2[0-3]):\d{2}:\d{2}"/.test(
        validLookupLine
      )
  );
  const outerFailure = logs.includes("调用测点模块接口失败");
  const simulatedLoginFailure =
    logs.includes("because parse access token failed") ||
    logs.includes("模拟登陆失败");
  const simulatedLoginSuccess = logs.includes("authenticated user ");
  const authenticatedDownstreamObserved = Boolean(validLookupLine);
  const simulatedLoginOrContextUsable =
    !simulatedLoginFailure &&
    (simulatedLoginSuccess || authenticatedDownstreamObserved);
  const technicalChecks = {
    containerRunning: finalState.State.Status === "running",
    restartCountZero: finalState.RestartCount === 0,
    deployedShaMatches: !expectedEamSha || deployedSha === expectedEamSha,
    topicExists: /PartitionCount:\s*1/.test(topicDescription),
    consumerBindingVisible: assignmentEvidence.includes(topic),
    topicOffsetAdvanced:
      topicEndOffsetAfter >= topicEndOffsetBefore + payloads.length,
    consumerLagZero:
      consumerPositionAfter != null &&
      consumerPositionAfter.currentOffset === topicEndOffsetAfter &&
      consumerPositionAfter.lag === 0,
    malformedMessagesIsolated: caseResults.slice(0, 4).every((item) => item.observed),
    simulatedLoginOrContextUsable,
    validLookupAttempted: Boolean(validLookupLine),
    twentyFourHourWindowObserved: usesTwentyFourHourWindow,
    noOuterConsumerFailure: !outerFailure,
    detailIdentityStable:
      before.id === after.id &&
      before.patrolTaskId === after.patrolTaskId &&
      before.workItemId === after.workItemId,
  };

  const technicalPass = Object.values(technicalChecks).every(Boolean);
  const persisted = after.gatherData != null;
  const externalDataAvailable = tagCounts.matching > 0 || persisted;
  let status = "FAIL";
  const issues = [];
  if (technicalPass && persisted) {
    status = "PASS";
  } else if (technicalPass && !externalDataAvailable) {
    status = "PASS_WITH_EXTERNAL_DATA_BLOCKER";
    issues.push({
      code: "NO_TAG_METADATA_OR_HISTORY",
      detail: `No enabled tmm_tags row matches ${before.itemNumber}; median persistence cannot be truthfully accepted.`,
    });
  } else {
    if (!technicalPass) {
      issues.push({
        code: "GATHER_CONSUMER_RUNTIME_CHECK_FAILED",
        detail: Object.entries(technicalChecks)
          .filter(([, passed]) => !passed)
          .map(([name]) => name)
          .join(", "),
      });
    }
    if (externalDataAvailable && !persisted) {
      issues.push({
        code: "GATHER_DATA_NOT_PERSISTED",
        detail: "Tag metadata/history appears available but mp_task_details.gather_data stayed null.",
      });
    }
  }

  const report = {
    generatedAt,
    repoCommit,
    database: "PostgreSQL",
    runtime: {
      sshTarget,
      eamContainer,
      kafkaContainer,
      postgresContainer,
      deployedSha256: deployedSha,
      expectedEamSha256: expectedEamSha || null,
      startedAt: finalState.State.StartedAt,
      restartCount: finalState.RestartCount,
    },
    kafka: {
      topic,
      zookeeperConnect,
      description: topicDescription.split(/\r?\n/),
      consumerBindingEvidence: assignmentEvidence.split(/\r?\n/).filter(Boolean),
      consumerGroup,
      topicEndOffsetBefore,
      topicEndOffsetAfter,
      consumerPositionBefore,
      consumerPositionAfter,
      marker,
      cases: caseResults,
    },
    tagManagement: {
      table: "tmm_tags",
      counts: tagCounts,
      requestedTag: before.itemNumber,
      requestWindowUses24HourFormat: usesTwentyFourHourWindow,
      lookupEvidence: validLookupLine || null,
    },
    simulatedLogin: {
      successObserved: simulatedLoginSuccess,
      failureObserved: simulatedLoginFailure,
      authenticatedDownstreamObserved,
      usable: simulatedLoginOrContextUsable,
      tokenValuesCaptured: false,
    },
    persistence: {
      table: "mp_task_details",
      verificationSql: detailSql,
      before,
      after,
      gatherDataPersisted: persisted,
    },
    technicalChecks,
    logEvidence: lines,
    status,
    issues,
  };

  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`, "utf8");
  process.stdout.write(`${JSON.stringify(report, null, 2)}\n`);
  if (status === "FAIL") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error.stack || error.message || String(error));
  process.exitCode = 1;
});

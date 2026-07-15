package com.mapletct.ftmes.bpi.stream;

import org.apache.flink.core.execution.CheckpointingMode;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.streaming.api.graph.StreamNode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BpiKafkaJobTopologyTest {

    @Test
    void productionGraphHasStableStatefulOperatorsAndAllTransactionalSinks() {
        StreamExecutionEnvironment environment = StreamExecutionEnvironment.getExecutionEnvironment();
        BpiKafkaJobConfig config = BpiKafkaJobConfig.from(Map.of(
                "bootstrap-servers", "kafka:9092",
                "deployment-id", "graph-test",
                "parallelism", "3"));

        BpiKafkaJob.build(environment, config);
        StreamGraph graph = environment.getStreamGraph();
        Set<String> uids = graph.getStreamNodes().stream()
                .map(StreamNode::getTransformationUID)
                .filter(value -> value != null)
                .collect(Collectors.toSet());
        Set<String> names = graph.getStreamNodes().stream()
                .map(StreamNode::getOperatorName)
                .collect(Collectors.toSet());

        assertEquals(CheckpointingMode.EXACTLY_ONCE, graph.getCheckpointingMode());
        assertEquals(30_000, graph.getCheckpointConfig().getCheckpointInterval());
        assertTrue(uids.contains("bpi-production-context-join-v1"));
        assertTrue(uids.contains("bpi-kafka-point-catalog-source-v1"));
        assertTrue(uids.contains("bpi-point-catalog-decode-v1"));
        assertTrue(uids.contains("bpi-rule-watermarks-v1"));
        assertTrue(uids.contains("bpi-rule-lifecycle-v1"));
        assertTrue(uids.contains("bpi-boundary-indexed-routing-v1"));
        assertTrue(uids.contains("bpi-boundary-evaluator-v1"));
        assertTrue(uids.contains("bpi-kafka-candidate-sink-v1"));
        assertTrue(uids.contains("bpi-kafka-data-quality-sink-v1"));
        assertTrue(uids.contains("bpi-kafka-rule-application-sink-v1"));
        assertTrue(names.stream().anyMatch(name -> name.contains("batch-candidate sink")));
        assertTrue(names.stream().anyMatch(name -> name.contains("data-quality sink")));
        assertTrue(names.stream().anyMatch(name -> name.contains("rule-application sink")));
        assertTrue(names.stream().anyMatch(name -> name.contains("point-catalog source")));
        assertEquals(3, environment.getParallelism());
    }
}

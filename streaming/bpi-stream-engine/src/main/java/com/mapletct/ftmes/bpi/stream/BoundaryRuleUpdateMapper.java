package com.mapletct.ftmes.bpi.stream;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mapletct.ftmes.bpi.contract.v1.BoundaryRulePublicationV1;
import org.apache.flink.api.common.functions.MapFunction;

public final class BoundaryRuleUpdateMapper implements MapFunction<byte[], byte[]> {

    @Override
    public byte[] map(byte[] value) {
        try {
            return BoundaryRuleUpdateCodec.encode(BoundaryRulePublicationMapper.map(
                    BoundaryRulePublicationV1.parseFrom(value)).ruleUpdate());
        } catch (InvalidProtocolBufferException | IllegalArgumentException | IllegalStateException error) {
            throw new IllegalArgumentException("validated rule publication cannot be mapped", error);
        }
    }
}

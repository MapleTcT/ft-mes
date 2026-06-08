/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.alibaba.fastjson.JSON
 *  com.alibaba.fastjson.parser.Feature
 *  com.alibaba.fastjson.parser.ParserConfig
 *  com.alibaba.fastjson.serializer.SerializerFeature
 *  io.netty.buffer.ByteBuf
 *  io.netty.buffer.ByteBufAllocator
 *  org.redisson.client.codec.BaseCodec
 *  org.redisson.client.handler.State
 *  org.redisson.client.protocol.Decoder
 *  org.redisson.client.protocol.Encoder
 */
package com.supcon.supos.suposgateway.cache.redisson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

public class GenericFastJsonCodec
extends BaseCodec {
    public static final GenericFastJsonCodec INSTANCE = new GenericFastJsonCodec();
    private static final ParserConfig parserConfig = new ParserConfig();
    private final Encoder encoder = new Encoder(){

        public ByteBuf encode(Object in) throws IOException {
            ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
            try {
                if (in == null) {
                    out.writeBytes(new byte[0]);
                } else {
                    out.writeBytes(JSON.toJSONBytes((Object)in, (SerializerFeature[])new SerializerFeature[]{SerializerFeature.WriteClassName}));
                }
                return out;
            }
            catch (Exception e) {
                out.release();
                throw new IOException(e);
            }
        }
    };
    private final Decoder<Object> decoder = new Decoder<Object>(){

        public Object decode(ByteBuf in, State state) throws IOException {
            try {
                if (in.readableBytes() > 0) {
                    return JSON.parseObject((String)in.toString(StandardCharsets.UTF_8), Object.class, (ParserConfig)parserConfig, (Feature[])new Feature[0]);
                }
                return null;
            }
            catch (Exception e) {
                throw new IOException(e);
            }
        }
    };

    private GenericFastJsonCodec() {
    }

    public Decoder<Object> getValueDecoder() {
        return this.decoder;
    }

    public Encoder getValueEncoder() {
        return this.encoder;
    }

    static {
        parserConfig.setAutoTypeSupport(true);
    }
}


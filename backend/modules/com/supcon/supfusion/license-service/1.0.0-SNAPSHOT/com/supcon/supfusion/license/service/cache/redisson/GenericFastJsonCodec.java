package com.supcon.supfusion.license.service.cache.redisson;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.handler.State;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GenericFastJsonCodec extends BaseCodec {
    public static GenericFastJsonCodec INSTANCE = new GenericFastJsonCodec();
    private static final ParserConfig parserConfig = new ParserConfig();
    private final Encoder encoder;
    private final Decoder<Object> decoder;

    static {
        parserConfig.setAutoTypeSupport(true);
    }

    private GenericFastJsonCodec() {
        encoder = new Encoder() {
            @Override
            public ByteBuf encode(Object in) throws IOException {
                ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
                try {
                    if (in == null) {
                        out.writeBytes(new byte[0]);
                    } else {
                        out.writeBytes(JSON.toJSONBytes(in, SerializerFeature.WriteClassName));
                    }
                    return out;
                } catch (Exception e) {
                    out.release();
                    throw new IOException(e);
                }
            }
        };
        decoder = new Decoder<Object>() {
            @Override
            public Object decode(ByteBuf in, State state) throws IOException {
                try {
                    if (in.readableBytes() > 0) {
                        return JSON.parseObject(in.toString(StandardCharsets.UTF_8), Object.class, parserConfig);
                    } else {
                        return null;
                    }
                } catch (Exception e) {
                    throw new IOException(e);
                }
            }
        };
    }

    @Override
    public Decoder<Object> getValueDecoder() {
        return decoder;
    }

    @Override
    public Encoder getValueEncoder() {
        return encoder;
    }

}

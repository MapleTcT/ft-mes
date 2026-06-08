/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.pojo;

import com.supcon.supfusion.framework.cloud.common.pojo.POJO;

public class VO
implements POJO {
    private static final long serialVersionUID = 972249376862163258L;

    protected VO(VOBuilder<?, ?> b) {
    }

    public static VOBuilder<?, ?> builder() {
        return new VOBuilderImpl();
    }

    public VO() {
    }

    private static final class VOBuilderImpl
    extends VOBuilder<VO, VOBuilderImpl> {
        private VOBuilderImpl() {
        }

        @Override
        protected VOBuilderImpl self() {
            return this;
        }

        @Override
        public VO build() {
            return new VO(this);
        }
    }

    public static abstract class VOBuilder<C extends VO, B extends VOBuilder<C, B>> {
        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "VO.VOBuilder()";
        }
    }
}


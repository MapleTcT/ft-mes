/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.pojo;

import com.supcon.supfusion.framework.cloud.common.pojo.POJO;

public class PO
implements POJO {
    private static final long serialVersionUID = 8614268472293823159L;

    protected PO(POBuilder<?, ?> b) {
    }

    public static POBuilder<?, ?> builder() {
        return new POBuilderImpl();
    }

    public PO() {
    }

    private static final class POBuilderImpl
    extends POBuilder<PO, POBuilderImpl> {
        private POBuilderImpl() {
        }

        @Override
        protected POBuilderImpl self() {
            return this;
        }

        @Override
        public PO build() {
            return new PO(this);
        }
    }

    public static abstract class POBuilder<C extends PO, B extends POBuilder<C, B>> {
        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "PO.POBuilder()";
        }
    }
}


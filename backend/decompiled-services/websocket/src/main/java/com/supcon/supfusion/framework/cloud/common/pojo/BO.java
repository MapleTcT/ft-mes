/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.pojo;

import com.supcon.supfusion.framework.cloud.common.pojo.POJO;

public class BO
implements POJO {
    private static final long serialVersionUID = -5460168097411311451L;

    protected BO(BOBuilder<?, ?> b) {
    }

    public static BOBuilder<?, ?> builder() {
        return new BOBuilderImpl();
    }

    public BO() {
    }

    private static final class BOBuilderImpl
    extends BOBuilder<BO, BOBuilderImpl> {
        private BOBuilderImpl() {
        }

        @Override
        protected BOBuilderImpl self() {
            return this;
        }

        @Override
        public BO build() {
            return new BO(this);
        }
    }

    public static abstract class BOBuilder<C extends BO, B extends BOBuilder<C, B>> {
        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "BO.BOBuilder()";
        }
    }
}


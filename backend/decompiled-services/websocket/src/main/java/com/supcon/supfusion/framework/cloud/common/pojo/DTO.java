/*
 * Decompiled with CFR 0.152.
 */
package com.supcon.supfusion.framework.cloud.common.pojo;

import com.supcon.supfusion.framework.cloud.common.pojo.POJO;

public class DTO
implements POJO {
    private static final long serialVersionUID = -3990959205035972670L;

    protected DTO(DTOBuilder<?, ?> b) {
    }

    public static DTOBuilder<?, ?> builder() {
        return new DTOBuilderImpl();
    }

    public DTO() {
    }

    private static final class DTOBuilderImpl
    extends DTOBuilder<DTO, DTOBuilderImpl> {
        private DTOBuilderImpl() {
        }

        @Override
        protected DTOBuilderImpl self() {
            return this;
        }

        @Override
        public DTO build() {
            return new DTO(this);
        }
    }

    public static abstract class DTOBuilder<C extends DTO, B extends DTOBuilder<C, B>> {
        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "DTO.DTOBuilder()";
        }
    }
}


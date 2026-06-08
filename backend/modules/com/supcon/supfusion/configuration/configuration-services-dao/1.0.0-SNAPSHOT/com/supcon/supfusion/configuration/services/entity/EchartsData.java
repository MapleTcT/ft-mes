package com.supcon.supfusion.configuration.services.entity;

import lombok.Data;

import java.util.List;

@Data
public class EchartsData {

    private String echartsCode;
    private String title;
    // @Fields legendOrient : 图例数据,格式['data1', 'data2'] 与系列名列表保持一致
    private List<String> legendData;
    // @Fields seriesList : 系列
    private List<EchartsSeries> seriesList;
    // @Fields xAxisData : X轴数据
    private List<String> xAxisData;

    public static class Builder {
        private String echartsCode;
        private String title;
        private List<String> xAxisData;
        private List<String> legendData;
        private List<EchartsSeries> seriesList;

        public Builder(String echartsCode) {
            super();
            this.echartsCode = echartsCode;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder xAxisData(List<String> xAxisData) {
            this.xAxisData = xAxisData;
            return this;
        }

        public Builder legendData(List<String> legendData) {
            this.legendData = legendData;
            return this;
        }

        public Builder seriesList(List<EchartsSeries> seriesList) {
            this.seriesList = seriesList;
            return this;
        }

        public EchartsData builder() {
            return new EchartsData(this);
        }
    }

    private EchartsData(Builder builder) {
        this.echartsCode = builder.echartsCode;
        this.title = builder.title;
        this.xAxisData = builder.xAxisData;
        this.legendData = builder.legendData;
        this.seriesList = builder.seriesList;
    }

}

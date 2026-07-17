package com.mapletct.ftmes.rmformula.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FormulaSaveRequest {
    private String requestId;
    private Integer expectedVersion;
    private String formulaCode;
    private String formulaName;
    private String formulaEdition;
    private Long productId;
    private String batchFormulaId;
    private String batchFormulaCode;
    private String batchFormulaEdition;
    private Long batchServerId;
    private String normalSize;
    private String description;
    private String reportType;
    private String setProcess;
    private List<ProcessInput> processes = new ArrayList<ProcessInput>();
    private List<ActivityInput> activities = new ArrayList<ActivityInput>();

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Integer getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(Integer expectedVersion) { this.expectedVersion = expectedVersion; }
    public String getFormulaCode() { return formulaCode; }
    public void setFormulaCode(String formulaCode) { this.formulaCode = formulaCode; }
    public String getFormulaName() { return formulaName; }
    public void setFormulaName(String formulaName) { this.formulaName = formulaName; }
    public String getFormulaEdition() { return formulaEdition; }
    public void setFormulaEdition(String formulaEdition) { this.formulaEdition = formulaEdition; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getBatchFormulaId() { return batchFormulaId; }
    public void setBatchFormulaId(String batchFormulaId) { this.batchFormulaId = batchFormulaId; }
    public String getBatchFormulaCode() { return batchFormulaCode; }
    public void setBatchFormulaCode(String batchFormulaCode) { this.batchFormulaCode = batchFormulaCode; }
    public String getBatchFormulaEdition() { return batchFormulaEdition; }
    public void setBatchFormulaEdition(String batchFormulaEdition) { this.batchFormulaEdition = batchFormulaEdition; }
    public Long getBatchServerId() { return batchServerId; }
    public void setBatchServerId(Long batchServerId) { this.batchServerId = batchServerId; }
    public String getNormalSize() { return normalSize; }
    public void setNormalSize(String normalSize) { this.normalSize = normalSize; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getSetProcess() { return setProcess; }
    public void setSetProcess(String setProcess) { this.setProcess = setProcess; }
    public List<ProcessInput> getProcesses() { return processes; }
    public void setProcesses(List<ProcessInput> processes) { this.processes = processes; }
    public List<ActivityInput> getActivities() { return activities; }
    public void setActivities(List<ActivityInput> activities) { this.activities = activities; }

    public static class ProcessInput {
        private Long id;
        private String clientKey;
        private String name;
        private String processSort;
        private String batchUnitId;
        private Boolean autoStart;
        private Integer executionOrder;
        private BigDecimal duration;
        private Boolean firstProcess;
        private Boolean lastProcess;
        private String remark;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getClientKey() { return clientKey; }
        public void setClientKey(String clientKey) { this.clientKey = clientKey; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProcessSort() { return processSort; }
        public void setProcessSort(String processSort) { this.processSort = processSort; }
        public String getBatchUnitId() { return batchUnitId; }
        public void setBatchUnitId(String batchUnitId) { this.batchUnitId = batchUnitId; }
        public Boolean getAutoStart() { return autoStart; }
        public void setAutoStart(Boolean autoStart) { this.autoStart = autoStart; }
        public Integer getExecutionOrder() { return executionOrder; }
        public void setExecutionOrder(Integer executionOrder) { this.executionOrder = executionOrder; }
        public BigDecimal getDuration() { return duration; }
        public void setDuration(BigDecimal duration) { this.duration = duration; }
        public Boolean getFirstProcess() { return firstProcess; }
        public void setFirstProcess(Boolean firstProcess) { this.firstProcess = firstProcess; }
        public Boolean getLastProcess() { return lastProcess; }
        public void setLastProcess(Boolean lastProcess) { this.lastProcess = lastProcess; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class ActivityInput {
        private Long id;
        private String clientKey;
        private String processKey;
        private String name;
        private String activeType;
        private String batchPhaseId;
        private String batchPhaseName;
        private String batchSite;
        private String dispatchSystem;
        private String executionSystem;
        private Boolean automatic;
        private Boolean fixedQuantity;
        private BigDecimal quantity;
        private BigDecimal minimumQuantity;
        private BigDecimal maximumQuantity;
        private String releaseConditions;
        private String responseItem;
        private String setItem;
        private String useItem;
        private String remark;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getClientKey() { return clientKey; }
        public void setClientKey(String clientKey) { this.clientKey = clientKey; }
        public String getProcessKey() { return processKey; }
        public void setProcessKey(String processKey) { this.processKey = processKey; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getActiveType() { return activeType; }
        public void setActiveType(String activeType) { this.activeType = activeType; }
        public String getBatchPhaseId() { return batchPhaseId; }
        public void setBatchPhaseId(String batchPhaseId) { this.batchPhaseId = batchPhaseId; }
        public String getBatchPhaseName() { return batchPhaseName; }
        public void setBatchPhaseName(String batchPhaseName) { this.batchPhaseName = batchPhaseName; }
        public String getBatchSite() { return batchSite; }
        public void setBatchSite(String batchSite) { this.batchSite = batchSite; }
        public String getDispatchSystem() { return dispatchSystem; }
        public void setDispatchSystem(String dispatchSystem) { this.dispatchSystem = dispatchSystem; }
        public String getExecutionSystem() { return executionSystem; }
        public void setExecutionSystem(String executionSystem) { this.executionSystem = executionSystem; }
        public Boolean getAutomatic() { return automatic; }
        public void setAutomatic(Boolean automatic) { this.automatic = automatic; }
        public Boolean getFixedQuantity() { return fixedQuantity; }
        public void setFixedQuantity(Boolean fixedQuantity) { this.fixedQuantity = fixedQuantity; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getMinimumQuantity() { return minimumQuantity; }
        public void setMinimumQuantity(BigDecimal minimumQuantity) { this.minimumQuantity = minimumQuantity; }
        public BigDecimal getMaximumQuantity() { return maximumQuantity; }
        public void setMaximumQuantity(BigDecimal maximumQuantity) { this.maximumQuantity = maximumQuantity; }
        public String getReleaseConditions() { return releaseConditions; }
        public void setReleaseConditions(String releaseConditions) { this.releaseConditions = releaseConditions; }
        public String getResponseItem() { return responseItem; }
        public void setResponseItem(String responseItem) { this.responseItem = responseItem; }
        public String getSetItem() { return setItem; }
        public void setSetItem(String setItem) { this.setItem = setItem; }
        public String getUseItem() { return useItem; }
        public void setUseItem(String useItem) { this.useItem = useItem; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}

package com.mapletct.ftmes.womprint.domain;

import java.time.LocalDate;

public final class QrCodeRecord {

    private final String tenantId;
    private final String requestId;
    private final String requestHash;
    private final int sequenceNo;
    private final TaskContext task;
    private final PrinterConfig printer;
    private final LocalDate manufactureDate;
    private final LocalDate expiryDate;
    private final String qrCode;
    private final String qrContent;
    private final String detail;

    public QrCodeRecord(
            String tenantId,
            String requestId,
            String requestHash,
            int sequenceNo,
            TaskContext task,
            PrinterConfig printer,
            LocalDate manufactureDate,
            LocalDate expiryDate,
            String qrCode,
            String qrContent,
            String detail) {
        this.tenantId = tenantId;
        this.requestId = requestId;
        this.requestHash = requestHash;
        this.sequenceNo = sequenceNo;
        this.task = task;
        this.printer = printer;
        this.manufactureDate = manufactureDate;
        this.expiryDate = expiryDate;
        this.qrCode = qrCode;
        this.qrContent = qrContent;
        this.detail = detail;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public TaskContext getTask() {
        return task;
    }

    public PrinterConfig getPrinter() {
        return printer;
    }

    public LocalDate getManufactureDate() {
        return manufactureDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public String getQrCode() {
        return qrCode;
    }

    public String getQrContent() {
        return qrContent;
    }

    public String getDetail() {
        return detail;
    }
}

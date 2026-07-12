package com.mapletct.ftmes.bpi.contract.validation;

import java.util.Objects;

public final class ContractViolation {

    private final String path;
    private final String code;
    private final String message;

    public ContractViolation(String path, String code, String message) {
        this.path = path;
        this.code = code;
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContractViolation)) {
            return false;
        }
        ContractViolation that = (ContractViolation) other;
        return Objects.equals(path, that.path)
            && Objects.equals(code, that.code)
            && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, code, message);
    }

    @Override
    public String toString() {
        return path + ":" + code + ":" + message;
    }
}

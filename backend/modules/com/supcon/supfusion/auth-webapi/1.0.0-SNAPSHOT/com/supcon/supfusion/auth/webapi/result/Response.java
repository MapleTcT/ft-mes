package com.supcon.supfusion.auth.webapi.result;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author lifangyuan
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Response implements Serializable {
    private static final long serialVersionUID = -5837383265338330426L;
    private boolean succeeded;
    private int code;
    private String message;
    private String requestUrl;
    private Throwable cause;
    private String ticket;
    private Object[] params;

    public Response() {

    }

    @JsonCreator
    public Response(@JsonProperty("succeeded") boolean succeeded, @JsonProperty("code") int code,
                    @JsonProperty("msg") String msg, @JsonProperty("ticket") String ticket) {
        this.setMessage(msg);
        this.setSucceeded(succeeded);
        this.setCode(code);
        this.setTicket(ticket);
    }

    @JsonCreator
    public Response(@JsonProperty("succeeded") boolean succeeded, @JsonProperty("code") int code,
                    @JsonProperty("msg") String msg, @JsonProperty("ticket") String ticket, @JsonProperty("throwable") Throwable cause) {
        this.setMessage(msg);
        this.setSucceeded(succeeded);
        this.setCause(cause);
        this.setTicket(ticket);
    }

    @JsonCreator
    public Response(@JsonProperty("succeeded") boolean succeeded, @JsonProperty("code") int code,
                    @JsonProperty("msg") String msg) {
        this.setMessage(msg);
        this.setSucceeded(succeeded);
        this.setCode(code);
    }

    @JsonCreator
    public Response(@JsonProperty("succeeded") boolean succeeded, @JsonProperty("code") int code,
                    @JsonProperty("msg") String msg, @JsonProperty("throwable") Throwable cause) {
        this.setMessage(msg);
        this.setSucceeded(succeeded);
        this.setCause(cause);
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

}

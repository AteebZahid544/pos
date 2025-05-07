package com.example.pos.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Bilal Hassan on 09-Sep-2020
 * @project zingpay-ms
 */


public class Status {

    private int code;
    private String message;
    private Long returnId;

    private Object additionalDetail;

    public Status(final StatusMessage statusMessage, final Long returnId) {
        this.setCode(statusMessage.getId());
        this.setMessage(statusMessage.getDescription());
        this.setReturnId(returnId);
    }

    public Status(final StatusMessage statusMessage) {
        this.setCode(statusMessage.getId());
        this.setMessage(statusMessage.getDescription());
        this.setReturnId(0L);
    }

    public Status(final String message) {
        this.setCode(0);
        this.setMessage(message);
        this.setReturnId(0L);
    }

    public Status(final String message, final Long id) {
        this.setCode(0);
        this.setMessage(message);
        this.setReturnId(id);
    }

    public Status(final int code, final String message, final Long returnId) {
        this.code = code;
        this.message = message;
        this.returnId = returnId;
    }

    public Status(final StatusMessage statusMessage, final Object additionalDetail) {
        this.setCode(statusMessage.getId());
        this.setMessage(statusMessage.getDescription());
        this.additionalDetail = additionalDetail;
    }

    @JsonCreator
    public Status(@JsonProperty("code") final int code,
                  @JsonProperty("message") final String message,
                  @JsonProperty("returnId") final Long returnId,
                  @JsonProperty("additionalDetail") final Object additionalDetail) {
        this.code = code;
        this.message = message;
        this.returnId = returnId;
        this.additionalDetail = additionalDetail;
    }



    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public Long getReturnId() {
        return returnId;
    }

    public void setReturnId(final Long returnId) {
        this.returnId = returnId;
    }

    public Object getAdditionalDetail() {
        return additionalDetail;
    }

    public void setAdditionalDetail(final Object additionalDetail) {
        this.additionalDetail = additionalDetail;
    }
}

package com.example.pos.util;

import org.springframework.http.HttpStatus;

/**
 * @author Bilal Hassan on 09-Sep-2020
 * @project zingpay-ms
 */

public enum StatusMessage {
    FAILURE(0, "FAILURE", HttpStatus.NOT_FOUND),
    SUCCESS(1, "SUCCESS", HttpStatus.OK),
    ARCHIVED(6002, "Archived", HttpStatus.OK),
    ACTIVE(6003, "Active", HttpStatus.OK),
    QR_DELETED_SUCCESSFULLY(6004, "QR deleted successfully", HttpStatus.OK),
    QR_NOT_FOUND(6005, "QR not found", HttpStatus.OK),
    MULTIPLE_RECORDS_FOUND(-101, "Unbale to process the request.", HttpStatus.UNPROCESSABLE_ENTITY),
    SOMETHING_WENT_WRONG(-102, "Something went wrong while saving transaction",
            HttpStatus.INTERNAL_SERVER_ERROR),
    QR_NOT_GENERATED(-1, "Failed to generate QR code", HttpStatus.NOT_FOUND),
    NO_QR_FOUND(-103,
            "No QR code information found for the provided user account ID and company name.",
            HttpStatus.UNPROCESSABLE_ENTITY);

    private int id;
    private String description;
    private HttpStatus statusCode;

    StatusMessage(final int id, final String description, final HttpStatus statusCode) {
        this.id = id;
        this.description = description;
        this.statusCode = statusCode;
    }


    public int getId() {
        return id;
    }
    public void setId(final int id) {
        this.id = id;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(final String description) {
        this.description = description;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(final HttpStatus statusCode) {
        this.statusCode = statusCode;
    }
}

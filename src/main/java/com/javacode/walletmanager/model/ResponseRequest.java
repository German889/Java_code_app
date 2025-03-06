package com.javacode.walletmanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class ResponseRequest {

    @JsonProperty("requestId")
    private UUID requestId;

    @JsonProperty("responseBody")
    private String responseBody;

    @JsonProperty("errorMessage")
    private String errorMessage;

    public ResponseRequest() {
    }

    public ResponseRequest(UUID requestId, String responseBody, String errorMessage) {
        this.requestId = requestId;
        this.responseBody = responseBody;
        this.errorMessage = errorMessage;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
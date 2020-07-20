package com.gocompliant.mockserver;

import lombok.Builder;
import lombok.Value;

public interface ResponseReader {

    @Value
    @Builder
    public class Response {
        int statusCode;
        String body;
    }

    static ResponseReader getInstance() {
        return new AzureGitAccess();
    }

    Response read(String path) throws Throwable;
}

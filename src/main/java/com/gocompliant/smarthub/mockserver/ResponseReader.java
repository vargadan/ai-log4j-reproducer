package com.gocompliant.smarthub.mockserver;

import lombok.Builder;
import lombok.Value;

public interface ResponseReader {

    @Value
    @Builder
    public class Response {
        int statusCode;
        String body;
    }

    static ResponseReader newInstance() {
        return new AzureGitAccess();
    }

    Response read(String path) throws Throwable;
}

package io.jd.framework.tests;

import io.jd.framework.webapp.HttpMethod;
import io.jd.framework.webapp.Request;

import java.util.List;

public record SimpleRequest(String body) implements Request {
    private static final List<HttpMethod> HTTP_METHOD_WITH_BODY =
            List.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    static SimpleRequest of(HttpMethod method, String body) {
        return HTTP_METHOD_WITH_BODY.contains(method) ? new SimpleRequest(body) : new SimpleRequest(null);
    }
}

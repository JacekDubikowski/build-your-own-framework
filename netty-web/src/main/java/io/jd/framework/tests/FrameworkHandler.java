package io.jd.framework.tests;

import io.jd.framework.webapp.HttpMethod;
import io.jd.framework.webapp.RequestHandler;
import io.jd.framework.webapp.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;

class FrameworkHandler extends AbstractHandler {

    private final RequestHandler handler;

    FrameworkHandler(RequestHandler handler) {
        this.handler = handler;
    }

    private static void handleWorkResult(HttpServletResponse response, Object result) throws IOException {
        if (result instanceof Response r) {
            processResponse(response, r);
        } else {
            process(response, result);
        }
    }

    private static void processResponse(HttpServletResponse response, Response r) throws IOException {
        response.setStatus(r.statusCode());
        if (r.body() != null) {
            response.getWriter().print(r.body());
        }
    }

    private static void process(HttpServletResponse response, Object result) throws IOException {
        response.setStatus(200);
        response.getWriter().print(result.toString());
    }

    private static String readWholeBody(HttpServletRequest request) throws IOException {
        return request.getReader().lines().collect(Collectors.joining("\n"));
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (methodAndPathMatches(baseRequest)) {
            response.setCharacterEncoding("utf-8");
            response.setContentType(handler.produce());
            handle(response, request);
            baseRequest.setHandled(true);
        }
    }

    private void handle(HttpServletResponse response, HttpServletRequest request) throws IOException {
        var frameworkRequest = SimpleRequest.of(HttpMethod.valueOf(request.getMethod()), readWholeBody(request));
        try {
            var result = handler.process(frameworkRequest);
            handleWorkResult(response, result);
        } catch (Exception e) {
            response.getWriter().print("{\"errorMessage\": \"%s\"}".formatted(e.getMessage()));
            response.setStatus(500);
        }
    }

    private boolean methodAndPathMatches(Request baseRequest) {
        return Objects.equals(baseRequest.getHttpURI().getPath(), handler.path())
                && Objects.equals(baseRequest.getMethod(), handler.method().toString());
    }
}

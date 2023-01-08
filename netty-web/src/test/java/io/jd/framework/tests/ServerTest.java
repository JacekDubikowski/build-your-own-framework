package io.jd.framework.tests;

import io.jd.framework.BeanProvider;
import io.jd.framework.BeanProviderFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;

import static io.jd.framework.webapp.MediaType.APPLICATION_JSON;
import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.eclipse.jetty.http.HttpHeader.ACCEPT;
import static org.eclipse.jetty.http.HttpHeader.CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerTest {
    private static ServerContainer serverContainer = null;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeAll
    static void setupTest() throws Exception {
        BeanProvider beanProvider = BeanProviderFactory.getInstance();
        serverContainer = beanProvider.provide(ServerContainer.class);
        serverContainer.start();
    }

    @AfterAll
    static void cleanupTest() throws Exception {
        serverContainer.stop();
    }

    private static URI path(String endpoint) {
        return URI.create("http://localhost:%s".formatted(serverContainer.port()) + endpoint);
    }

    private static HttpRequest getRequest(String endpointPath) {
        return buildWithAcceptHeader(endpointPath)
                .GET()
                .build();
    }

    private static HttpRequest postRequest(BodyPublisher publisher, String endpoint) {
        return buildWithAcceptHeader(endpoint)
                .POST(publisher)
                .build();
    }

    private static HttpRequest putRequest(BodyPublisher publisher, String endpoint) {
        return buildWithAcceptHeader(endpoint)
                .PUT(publisher)
                .build();
    }

    private static HttpRequest.Builder buildWithAcceptHeader(String endpoint) {
        return HttpRequest.newBuilder(path(endpoint))
                .header(ACCEPT.toString(), APPLICATION_JSON);
    }

    private static String getContentTypeValue(HttpResponse<String> response) {
        return response.headers().firstValue(CONTENT_TYPE.toString()).get();
    }

    @Test
    void testSlashInt() throws IOException, InterruptedException {
        var request = getRequest("/int");

        var response = client.send(request, ofString());

        assertEquals(200, response.statusCode());
        assertDoesNotThrow(() -> Integer.parseInt(response.body()));

        assertEquals(
                "application/json+framework;charset=utf-8",
                getContentTypeValue(response)
        );
    }

    @Test
    void testSlashInt2() throws IOException, InterruptedException {
        var request = getRequest("/int2");

        var response = client.send(request, ofString());

        assertEquals(200, response.statusCode());
        assertDoesNotThrow(() -> Integer.parseInt(response.body()));
        assertEquals("text/plain;charset=utf-8", getContentTypeValue(response));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 11, 100, 400})
    void testSlashInt3(int value) throws IOException, InterruptedException {
        var request = postRequest(BodyPublishers.ofString(String.valueOf(value)), "/int3");

        var response = client.send(request, ofString());

        assertEquals(response.statusCode(), 200);
        assertEquals(Integer.parseInt(response.body()), value);
        assertEquals(getContentTypeValue(response), APPLICATION_JSON);
    }

    @Test
    void testSlashVoid() throws IOException, InterruptedException {
        var request = postRequest(noBody(), "/void");

        var response = client.send(request, ofString());

        assertEquals(response.statusCode(), 204);
        assertEquals(response.body(), "");
    }

    @Test
    void testSlashReturnErrorResponse() throws IOException, InterruptedException {
        var request = putRequest(noBody(), "/return-error-response");

        var response = client.send(request, ofString());

        assertEquals(500, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void testSlashResource() throws IOException, InterruptedException {
        var request = getRequest("/resource");

        var response = client.send(request, ofString());

        assertEquals(200, response.statusCode());
        assertEquals("{\"key\":\"value\"}", response.body());
    }

    @Test
    void testSlashError() throws IOException, InterruptedException {
        var request = getRequest("/error");

        var response = client.send(request, ofString());

        assertEquals(500, response.statusCode());
        assertEquals("{\"errorMessage\": \"Expected error message\"}", response.body());
    }

}

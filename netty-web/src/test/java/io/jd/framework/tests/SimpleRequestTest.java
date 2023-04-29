package io.jd.framework.tests;

import io.jd.framework.webapp.HttpMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;

class SimpleRequestTest {

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, names = {"POST", "PUT", "PATCH"}, mode = EXCLUDE)
    void shouldNotPassBodyForSomeHttpMethods(HttpMethod httpMethod) {
        var request = SimpleRequest.of(httpMethod, "body");

        assertNull(request.body());
    }

    @ParameterizedTest
    @EnumSource(value = HttpMethod.class, names = {"POST", "PUT", "PATCH"}, mode = INCLUDE)
    void shouldPassBodyForSomeHttpMethods(HttpMethod httpMethod) {
        String exampleBody = "body";

        var request = SimpleRequest.of(httpMethod, exampleBody);

        assertEquals(exampleBody, request.body());
    }

}
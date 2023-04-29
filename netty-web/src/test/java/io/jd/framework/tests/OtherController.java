package io.jd.framework.tests;

import io.jd.framework.webapp.HttpMethod;
import io.jd.framework.webapp.RequestHandle;
import jakarta.inject.Singleton;

@Singleton
public class OtherController {

    @RequestHandle(value = "/resource", method = HttpMethod.GET)
    public String getResource() {
        return """
                {"key":"value"}""";
    }

}

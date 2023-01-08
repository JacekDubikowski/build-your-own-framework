package io.jd.framework.tests;

import io.jd.framework.webapp.HttpMethod;
import io.jd.framework.webapp.MediaType;
import io.jd.framework.webapp.Request;
import io.jd.framework.webapp.RequestHandle;
import jakarta.inject.Singleton;

import java.util.Random;

@Singleton
public class ExampleController {
    private final static Random RANDOM = new Random();

    @RequestHandle(value = "/int", method = HttpMethod.GET, produce = "application/json+framework")
    public int getInt() {
        return RANDOM.nextInt();
    }

    @RequestHandle(value = "/int2", method = HttpMethod.GET, produce = MediaType.TEXT_PLAIN)
    public int getInt(Request request) {
        return RANDOM.nextInt();
    }

    @RequestHandle(value = "/int3", method = HttpMethod.POST)
    public int getIntFromString(Request request) {
        return Integer.parseInt(request.body());
    }

    @RequestHandle(value = "/void", method = HttpMethod.POST)
    public void doSomething() {
    }

}

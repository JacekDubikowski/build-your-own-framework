package io.jd.framework.web;

import io.jd.framework.webapp.HttpMethod;
import io.jd.framework.webapp.Request;
import io.jd.framework.webapp.RequestHandle;
import jakarta.inject.Singleton;

import java.util.Random;

@Singleton
public class ExampleController {
    private final static Random RANDOM = new Random();

    @RequestHandle(value = "/int", method = HttpMethod.GET)
    int getInt() {
        return RANDOM.nextInt();
    }

    @RequestHandle(value = "/int2", method = HttpMethod.GET)
    int getInt(Request request) {
        return 2;
    }

    @RequestHandle(value = "/int3", method = HttpMethod.GET)
    int getIntFromString(Request request) {
        return Integer.parseInt("1");
    }

    @RequestHandle(value = "/void", method = HttpMethod.POST)
    void doSomething() {
    }

}

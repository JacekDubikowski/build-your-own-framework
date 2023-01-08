package io.jd.framework.tests;

import jakarta.inject.Singleton;

@Singleton
public class ServiceB implements Service {
    private final ServiceA serviceA;

    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}

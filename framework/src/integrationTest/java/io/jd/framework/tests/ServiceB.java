package io.jd.framework.tests;

import jakarta.inject.Singleton;

@Singleton
public class ServiceB {
    private final ServiceA serviceA;

    public ServiceB(ServiceA serviceA) {
        this.serviceA = serviceA;
    }
}

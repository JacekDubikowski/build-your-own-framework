package io.jd.framework.tests;

import jakarta.inject.Singleton;

@Singleton
public class ServiceC implements Service {
    private final ServiceA serviceA;
    private final ServiceB serviceB;

    public ServiceC(ServiceA serviceA, ServiceB serviceB) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
    }
}

package io.jd.framework.tests;

import jakarta.inject.Singleton;

import java.util.Collection;

@Singleton
public class Services {

    private final Collection<Service> services;

    public Services(Collection<Service> services) {
        this.services = services;
    }
}

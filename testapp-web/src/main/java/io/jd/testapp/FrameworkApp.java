package io.jd.testapp;

import io.jd.framework.BeanProvider;
import io.jd.framework.BeanProviderFactory;
import io.jd.framework.tests.ServerContainer;

public class FrameworkApp {
    public static void main(String[] args) throws Exception {
        BeanProvider provider = BeanProviderFactory.getInstance();
        ServerContainer container = provider.provide(ServerContainer.class);
        container.start();
        System.out.printf("Port: %s%n", container.port());

        // wait for input to close
        System.in.read();
        container.stop();
    }
}

package io.jd.framework.tests;

import io.jd.framework.webapp.RequestHandler;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;

import java.util.Collection;

@Singleton
public class ServerContainer {
    private final HandlerCollection handlers = new HandlerCollection();
    private volatile Server server = null;

    ServerContainer(Collection<RequestHandler> handlers) {
        handlers.stream()
                .map(FrameworkHandler::new)
                .forEach(this.handlers::addHandler);
    }

    public synchronized void start() throws Exception {
        if (server == null) {
            createServerInstance();
        }
        server.start();
    }

    private void createServerInstance() {
        server = new Server();
        var connector = new ServerConnector(server);
        server.addConnector(connector);
        server.setHandler(this.handlers);
    }

    public int port() {
        return server.getURI().getPort();
    }

    public void stop() throws Exception {
        server.stop();
    }
}

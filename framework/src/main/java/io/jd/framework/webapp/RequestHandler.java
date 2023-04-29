package io.jd.framework.webapp;

public interface RequestHandler {

    HttpMethod method();

    String produce();

    String path();

    Object process(Request request) throws Exception;

}

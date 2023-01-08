package io.jd.framework.webapp;

public interface Response {
    static Response noContent() {
        return new Response() {
            @Override
            public int statusCode() {
                return 204;
            }

            @Override
            public String body() {
                return null;
            }
        };
    }

    int statusCode();

    String body();
}

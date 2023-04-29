package io.jd.framework.webapp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RequestHandle {

    HttpMethod method();

    String value() default "/";

    String produce() default MediaType.APPLICATION_JSON;
}

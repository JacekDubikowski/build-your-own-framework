package io.jd.framework;

class FailedToInstantiateBeanDefinitionException extends RuntimeException {

    public FailedToInstantiateBeanDefinitionException(Class<?> definitionClass, Throwable cause) {
        super("Failed to instantiate '%s'".formatted(definitionClass.getCanonicalName()), cause);
    }
}

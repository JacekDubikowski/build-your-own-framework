package io.jd.framework.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;

import javax.lang.model.element.ExecutableElement;
import java.util.List;

class TransactionalInterceptedWriter {
    private final ClassName element;
    private final List<ExecutableElement> executableElements;

    TransactionalInterceptedWriter(ClassName element, List<ExecutableElement> executableElements) {
        this.element = element;
        this.executableElements = executableElements;
    }

    public JavaFile createDefinition() {
        return null;
    }
}

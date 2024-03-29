package io.jd.framework.transactional;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import java.util.Set;
import java.util.function.Function;

import static javax.tools.Diagnostic.Kind.ERROR;

class TransactionalMessenger {
    private final Messager messager;

    public TransactionalMessenger(Messager messager) {
        this.messager = messager;
    }

    void raiseFor(Set<ExecutableElement> executableElements, Modifier modifier, String errorMessage) {
        raiseFor(executableElements, modifier, errorMessage, Function.identity());
    }

    void raiseFor(Set<ExecutableElement> executableElements, Modifier modifier, String errorMessage, Function<ExecutableElement, ? extends Element> mapper) {
        executableElements.stream().map(mapper)
                .filter(method -> method.getModifiers().contains(modifier))
                .findFirst()
                .ifPresent(method -> messager.printMessage(ERROR, errorMessage, method));
    }
}

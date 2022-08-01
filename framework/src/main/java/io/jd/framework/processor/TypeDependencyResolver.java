package io.jd.framework.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.stream.Collectors;

public class TypeDependencyResolver {

    public Dependency resolve(TypeElement element, Messager messager) {
        var constructors = ElementFilter.constructorsIn(element.getEnclosedElements());
        if (constructors.size() == 1) {
            ExecutableElement constructor = constructors.get(0);
            return new Dependency(element, constructor.getParameters().stream().map(VariableElement::asType).toList());
        } else {
            failOnTooManyConstructors(element, messager, constructors);
            throw new IllegalStateException("Not good");
        }
    }

    private void failOnTooManyConstructors(TypeElement element, Messager messager, List<ExecutableElement> constructors) {
        messager.printMessage(Diagnostic.Kind.ERROR, "Too many constructors of the class %s".formatted(constructors.stream().map(ExecutableElement::toString).collect(Collectors.joining(", "))), element);
    }
}

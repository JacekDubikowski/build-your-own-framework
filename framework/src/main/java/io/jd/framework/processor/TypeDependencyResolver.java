package io.jd.framework.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.stream.Collectors;

import static javax.tools.Diagnostic.Kind.ERROR;

public class TypeDependencyResolver {

    public Dependency resolve(TypeElement element, Messager messager) {
        return isConcreteClass(element)
                ? resolveConcreteClass(element, messager)
                : failOnInvalidElement(element, messager);
    }

    private Dependency resolveConcreteClass(TypeElement element, Messager messager) {
        var constructors = ElementFilter.constructorsIn(element.getEnclosedElements());
        return constructors.size() == 1
                ? resolveDependency(element, constructors)
                : failOnTooManyConstructors(element, messager, constructors);
    }

    private boolean isConcreteClass(TypeElement element) {
        return element.getKind().isClass() && !element.getModifiers().contains(Modifier.ABSTRACT);
    }

    private Dependency resolveDependency(TypeElement element, List<ExecutableElement> constructors) {
        ExecutableElement constructor = constructors.get(0);
        return new Dependency(element, constructor.getParameters().stream().map(VariableElement::asType).toList());
    }

    private Dependency failOnTooManyConstructors(TypeElement element, Messager messager, List<ExecutableElement> constructors) {
        String constructorsRep = constructors.stream().map(ExecutableElement::toString).collect(Collectors.joining(", "));
        messager.printMessage(ERROR, "Too many constructors of the class %s (%s)".formatted(constructorsRep, element), element);
        throw new IllegalStateException("Compilation faced error.");
    }

    private Dependency failOnInvalidElement(TypeElement element, Messager messager) {
        messager.printMessage(ERROR, "Invalid type element: %s".formatted(element), element);
        throw new IllegalStateException("Compilation faced error.");
    }
}

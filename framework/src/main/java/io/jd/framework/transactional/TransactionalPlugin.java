package io.jd.framework.transactional;

import com.squareup.javapoet.JavaFile;
import io.jd.framework.processor.ProcessorPlugin;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.transaction.Transactional;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

public class TransactionalPlugin implements ProcessorPlugin {
    private TransactionalMessenger transactionalMessenger;
    private ProcessingEnvironment processingEnv;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        transactionalMessenger = new TransactionalMessenger(processingEnv.getMessager());
    }

    @Override
    public Class<? extends Annotation> reactsOn() {
        return Transactional.class;
    }

    @Override
    public Collection<JavaFile> process(Set<? extends Element> annotated) {
        Set<ExecutableElement> transactionalMethods = ElementFilter.methodsIn(annotated);
        validateMethods(transactionalMethods);
        Map<TypeElement, List<ExecutableElement>> typeToTransactionalMethods = transactionalMethods.stream().collect(groupingBy(element -> (TypeElement) element.getEnclosingElement()));
        return typeToTransactionalMethods.entrySet()
                .stream()
                .map(this::writeTransactional)
                .toList();
    }

    private void validateMethods(Set<ExecutableElement> transactionalMethods) {
        raiseForPrivate(transactionalMethods);
        raiseForStatic(transactionalMethods);
        raiseForFinalMethods(transactionalMethods);
        raiseForFinalClass(transactionalMethods);
    }

    private void raiseForFinalClass(Set<ExecutableElement> transactionalMethods) {
        transactionalMessenger.raiseFor(transactionalMethods, Modifier.FINAL, "final class annotated as transactional", javax.lang.model.element.Element::getEnclosingElement);
    }

    private void raiseForFinalMethods(Set<ExecutableElement> transactionalMethods) {
        transactionalMessenger.raiseFor(transactionalMethods, Modifier.FINAL, "final method annotated as transactional");
    }

    private void raiseForStatic(Set<ExecutableElement> transactionalMethods) {
        transactionalMessenger.raiseFor(transactionalMethods, Modifier.STATIC, "static method annotated as transactional");
    }

    private void raiseForPrivate(Set<ExecutableElement> transactionalMethods) {
        transactionalMessenger.raiseFor(transactionalMethods, Modifier.PRIVATE, "private method annotated as transactional");
    }

    private JavaFile writeTransactional(Map.Entry<TypeElement, List<ExecutableElement>> typeElementListEntry) {
        var transactionalType = typeElementListEntry.getKey();
        var transactionalMethods = typeElementListEntry.getValue();
        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(transactionalType);
        return new TransactionalInterceptedWriter(transactionalType, transactionalMethods, packageElement)
                .createDefinition(processingEnv.getMessager());
    }
}

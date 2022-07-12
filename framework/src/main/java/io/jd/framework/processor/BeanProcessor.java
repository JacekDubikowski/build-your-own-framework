package io.jd.framework.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import jakarta.inject.Singleton;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;
import static javax.tools.Diagnostic.Kind.ERROR;

@SupportedAnnotationTypes({"jakarta.inject.Singleton"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class BeanProcessor extends AbstractProcessor {

    private TransactionalMessager transactionalMessager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        transactionalMessager = new TransactionalMessager(this.processingEnv.getMessager());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            processBeans(roundEnv);
            processTransactional(roundEnv);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(ERROR, "Exception occurred %s".formatted(e));
        }
        return false;
    }

    private void processTransactional(RoundEnvironment roundEnv) {
        var annotated = roundEnv.getElementsAnnotatedWith(Transactional.class);
        Set<ExecutableElement> transactionalMethods = ElementFilter.methodsIn(annotated);
        raiseForPrivate(transactionalMethods);
        raiseForStatic(transactionalMethods);
        raiseForFinalMethods(transactionalMethods);
        raiseForFinalClass(transactionalMethods);
        Map<TypeElement, List<ExecutableElement>> typeToTransactionalMethods = transactionalMethods.stream().collect(groupingBy(element -> (TypeElement) element.getEnclosingElement()));
        typeToTransactionalMethods.forEach(this::writeTransactional);
    }

    private void writeTransactional(TypeElement transactionalType, List<ExecutableElement> transactionalMethods) {
        try {
            PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(transactionalType);
            JavaFile javaFile = new TransactionalInterceptedWriter(transactionalType, transactionalMethods, packageElement).createDefinition();
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(ERROR, "Failed to write transactional definition of ", transactionalType);
        }
    }

    private void raiseForFinalClass(Set<ExecutableElement> transactionalMethods) {
        transactionalMessager.raiseFor(transactionalMethods, Modifier.FINAL, "final class annotated as transactional", Element::getEnclosingElement);
    }

    private void raiseForFinalMethods(Set<ExecutableElement> transactionalMethods) {
        transactionalMessager.raiseFor(transactionalMethods, Modifier.FINAL, "final method annotated as transactional");
    }

    private void raiseForStatic(Set<ExecutableElement> transactionalMethods) {
        transactionalMessager.raiseFor(transactionalMethods, Modifier.STATIC, "static method annotated as transactional");
    }

    private void raiseForPrivate(Set<ExecutableElement> transactionalMethods) {
        transactionalMessager.raiseFor(transactionalMethods, Modifier.PRIVATE, "private method annotated as transactional");
    }

    private void processBeans(RoundEnvironment roundEnv) {
        var annotated = roundEnv.getElementsAnnotatedWith(Singleton.class);
        var types = ElementFilter.typesIn(annotated);
        var typeDependencyResolver = new TypeDependencyResolver();
        types.stream().map(t -> typeDependencyResolver.resolve(t, processingEnv))
                .forEach(this::writeDefinition);
    }

    private void writeDefinition(Dependency dependency) {
        try {
            ClassName definedClass = ClassName.get(dependency.type());
            List<TypeName> constructorParameterTypes = dependency.dependencies().stream().map(TypeName::get).toList();
            JavaFile javaFile = new DefinitionWriter(definedClass, constructorParameterTypes).createDefinition();
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(ERROR, "Failed to write definition", dependency.type());
        }
    }
}

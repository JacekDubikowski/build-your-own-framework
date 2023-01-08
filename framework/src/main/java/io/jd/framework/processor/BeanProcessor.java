package io.jd.framework.processor;

import com.squareup.javapoet.JavaFile;
import io.jd.framework.transactional.TransactionalPlugin;
import io.jd.framework.webapp.WebPlugin;
import jakarta.inject.Singleton;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static javax.tools.Diagnostic.Kind.ERROR;

@SupportedAnnotationTypes({"jakarta.inject.Singleton"})
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class BeanProcessor extends AbstractProcessor {
    private List<ProcessorPlugin> plugins = List.of();
    private TypeElement collectionElement;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        plugins = List.of(new TransactionalPlugin(), new WebPlugin());
        plugins.forEach(processorPlugin -> processorPlugin.init(processingEnv));
        this.collectionElement = processingEnv.getElementUtils().getTypeElement("java.util.Collection");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            runPluginsProcessing(roundEnv);
            processBeans(roundEnv);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(ERROR, "Exception occurred %s".formatted(e));
        }
        return false;
    }

    private void runPluginsProcessing(RoundEnvironment roundEnv) {
        plugins.stream().map(processorPlugin -> processorPlugin.process(roundEnv.getElementsAnnotatedWith(processorPlugin.reactsTo())))
                .flatMap(Collection::stream)
                .forEach(this::writeFile);
    }

    private void processBeans(RoundEnvironment roundEnv) {
        var annotated = roundEnv.getElementsAnnotatedWith(Singleton.class);
        var types = ElementFilter.typesIn(annotated);
        var typeDependencyResolver = new TypeDependencyResolver();
        types.stream().map(t -> typeDependencyResolver.resolve(t, processingEnv.getMessager()))
                .forEach(this::writeDefinition);
    }

    private void writeDefinition(Dependency dependency) {
        JavaFile javaFile = new DefinitionWriter(
                dependency.type(),
                dependency.dependencies(),
                processingEnv.getTypeUtils(),
                this.collectionElement
        ).createDefinition();
        writeFile(javaFile);
    }

    private void writeFile(JavaFile javaFile) {
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(ERROR, "Failed to write definition %s".formatted(javaFile));
        }
    }
}

package io.jd.framework.webapp;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.jd.framework.ProcessingEnvUtils;
import io.jd.framework.processor.ProcessorPlugin;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

public class WebPlugin implements ProcessorPlugin {
    private HandlerWriter handlerWriter;
    private ProcessingEnvironment processingEnv;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.handlerWriter = new HandlerWriter(processingEnv);
    }

    @Override
    public Class<? extends Annotation> reactsTo() {
        return RequestHandle.class;
    }

    @Override
    public Collection<JavaFile> process(Set<? extends Element> annotated) {
        return ElementFilter.methodsIn(annotated).stream()
                .collect(groupingBy(NameData::new))
                .entrySet()
                .stream()
                .flatMap(entry -> handle(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Stream<JavaFile> handle(NameData nameData, List<ExecutableElement> handlers) {
        return handlers.stream()
                .map(IndexedValue.indexed())
                .map(element -> handle(nameData, element));
    }

    private JavaFile handle(NameData nameData, IndexedValue<ExecutableElement> indexedValue) {
        try {
            return getJavaFile(nameData, indexedValue);
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), indexedValue.value());
            throw e;
        }
    }

    private JavaFile getJavaFile(NameData nameData, IndexedValue<ExecutableElement> indexedValue) {
        var handlerMethod = indexedValue.value();
        var typeSpec = createType(nameData, indexedValue, handlerMethod);
        String packageName = ProcessingEnvUtils.getPackageName(processingEnv, handlerMethod);
        return JavaFile.builder(packageName, typeSpec).build();
    }

    private TypeSpec createType(NameData nameData, IndexedValue<ExecutableElement> indexedValue, ExecutableElement handlerMethod) {
        return handlerWriter.buildHandler(
                nameData.toHandlerMethodName(indexedValue.index()),
                handlerMethod,
                TypeName.get(handlerMethod.getEnclosingElement().asType()),
                handlerMethod.getAnnotation(RequestHandle.class)
        );
    }

    private record NameData(Name controllerName, Name handleName) {
        NameData(ExecutableElement element) {
            this(element.getEnclosingElement().getSimpleName(), element.getSimpleName());
        }

        String toHandlerMethodName(int index) {
            return "%s$%s$%s$handler".formatted(controllerName.toString(), handleName.toString(), index);
        }
    }
}

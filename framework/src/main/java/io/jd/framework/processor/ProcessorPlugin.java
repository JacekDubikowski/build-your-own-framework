package io.jd.framework.processor;

import com.squareup.javapoet.JavaFile;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;

public interface ProcessorPlugin {
    void init(ProcessingEnvironment processingEnv);

    Collection<JavaFile> process(Set<? extends Element> annotated);

    Class<? extends Annotation> reactsOn();
}

package io.jd.framework;

import com.google.testing.compile.Compiler;
import io.jd.framework.processor.BeanProcessor;

public class AbstractAnnotationProcessorTest {
    protected final Compiler javac = Compiler.javac()
            .withProcessors(new BeanProcessor());
}

package io.jd.framework;

import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static io.jd.framework.TestUtil.getJavaFileObject;

public class RequestHandlerCreationTest extends AbstractAnnotationProcessorTest {
    private static final JavaFileObject CONTROLLER = getJavaFileObject("definitions/web/ExampleController.java");
    private static final String PACKAGE_NAME = "io.jd.framework.web";

    @Test
    void shouldCreateTwoRequestHandlers() {
        Compilation compilation = javac.compile(CONTROLLER);

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("%s.$ExampleController$Definition".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.ExampleController$getInt$1$handler".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.ExampleController$getInt$2$handler".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.ExampleController$getIntFromString$1$handler".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.ExampleController$doSomething$1$handler".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.$ExampleController$getInt$1$handler$Definition".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.$ExampleController$getInt$2$handler$Definition".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.$ExampleController$getIntFromString$1$handler$Definition".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.$ExampleController$doSomething$1$handler$Definition".formatted(PACKAGE_NAME));
    }
}

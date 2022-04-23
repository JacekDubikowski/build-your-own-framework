package io.jd.framework;

import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static io.jd.framework.TestUtil.getJavaFileObject;

public class TransactionalCreationTest extends AbstractAnnotationProcessorTest {
    private static final JavaFileObject VALID_TRANSACTIONAL_CASE = getJavaFileObject("definitions/transactional/A.java");
    private static final JavaFileObject PRIVATE_TRANSACTIONAL_METHOD_CASE = getJavaFileObject("definitions/transactional/PrivateA.java");
    private static final JavaFileObject FINAL_CLASS_TRANSACTIONAL_TEST = getJavaFileObject("definitions/transactional/FinalA.java");
    private static final JavaFileObject FINAL_METHOD_TRANSACTIONAL_TEST = getJavaFileObject("definitions/transactional/FinalMethodA.java");
    private static final JavaFileObject STATIC_TRANSACTIONAL_TEST = getJavaFileObject("definitions/transactional/StaticA.java");
    private static final String PACKAGE_NAME = "io.jd.framework.definitions";

    @Test
    void shouldGenerateExpectedFilesForTransactional() {
        Compilation compilation = javac.compile(VALID_TRANSACTIONAL_CASE);

        assertThat(compilation).succeededWithoutWarnings();
        assertThat(compilation).generatedSourceFile("%s.$A$Definition".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.A$Intercepted".formatted(PACKAGE_NAME));
        assertThat(compilation).generatedSourceFile("%s.$A$Intercepted$Definition".formatted(PACKAGE_NAME));
    }

    @Test
    void shouldFailOnPrivateMethodAnnotatedAsTransactional() {
        Compilation compilation = javac.compile(PRIVATE_TRANSACTIONAL_METHOD_CASE);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("private method annotated as transactional");
    }

    @Test
    void shouldFailOnFinalClassWithMethodAnnotatedAsTransactional() {
        Compilation compilation = javac.compile(FINAL_CLASS_TRANSACTIONAL_TEST);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("final class annotated as transactional");
    }

    @Test
    void shouldFailOnFinalMethodAnnotatedAsTransactional() {
        Compilation compilation = javac.compile(FINAL_METHOD_TRANSACTIONAL_TEST);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("final method annotated as transactional");
    }

    @Test
    void shouldFailOnStaticMethodAnnotatedAsTransactional() {
        Compilation compilation = javac.compile(STATIC_TRANSACTIONAL_TEST);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("static method annotated as transactional");
    }
}

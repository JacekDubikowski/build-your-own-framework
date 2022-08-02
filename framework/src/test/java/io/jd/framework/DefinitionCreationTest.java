package io.jd.framework;

import com.google.testing.compile.Compilation;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.List;
import java.util.stream.Stream;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static io.jd.framework.TestUtil.getJavaFileObject;
import static io.jd.framework.TestUtil.getJavaFileObjects;

public class DefinitionCreationTest extends AbstractAnnotationProcessorTest {
    private static final List<String> CLASS_NAMES = Stream.of("A", "B", "C").toList();
    private static final List<JavaFileObject> FILES_TO_CREATE_DEFS = getJavaFileObjects(CLASS_NAMES.stream(), "definitions/simpleDefs/%s.java");
    private static final JavaFileObject TWO_CONSTRUCTORS = getJavaFileObject("definitions/invalidDefs/TwoConstructors.java");
    private static final JavaFileObject ABSTRACT_B = getJavaFileObject("definitions/invalidDefs/AbstractB.java");
    private static final JavaFileObject INTERFACE_C = getJavaFileObject("definitions/invalidDefs/InterfaceC.java");

    private static final String PACKAGE_NAME = "io.jd.framework.definitions";

    @Test
    void shouldCreateExpectedDefinitions() {
        Compilation compilation = javac.compile(FILES_TO_CREATE_DEFS);

        assertThat(compilation).succeededWithoutWarnings();
        CLASS_NAMES.forEach(className ->
                assertThat(compilation).generatedSourceFile("%s.$%s$Definition".formatted(PACKAGE_NAME, className))
        );
    }

    @Test
    void shouldFailForInterfaces() {
        Compilation compilation = javac.compile(INTERFACE_C);

        assertThat(compilation).failed();
        assertThat(compilation).hadErrorContaining("Invalid type element: ");
    }

    @Test
    void shouldFailForAbstractClasses() {
        Compilation compilation = javac.compile(ABSTRACT_B);

        assertThat(compilation).hadErrorContaining("Invalid type element: ");
    }

    @Test
    void shouldFailForClassesWithTwoConstructors() {
        Compilation compilation = javac.compile(TWO_CONSTRUCTORS);

        assertThat(compilation).hadErrorContaining("Too many constructors of the class");
    }
}

package io.jd.framework;

import com.google.testing.compile.JavaFileObjects;

import javax.tools.JavaFileObject;
import java.util.List;
import java.util.stream.Stream;

public class TestUtil {
    private TestUtil() {
    }

    public static JavaFileObject getJavaFileObject(String fileName) {
        return JavaFileObjects.forResource(fileName);
    }

    public static List<JavaFileObject> getJavaFileObjects(Stream<String> fileNames, String pathTemplate) {
        return fileNames
                .map(pathTemplate::formatted)
                .map(JavaFileObjects::forResource)
                .toList();
    }
}

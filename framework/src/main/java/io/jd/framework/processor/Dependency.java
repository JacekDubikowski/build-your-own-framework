package io.jd.framework.processor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

public record Dependency(TypeElement type, List<TypeMirror> dependencies) {
}

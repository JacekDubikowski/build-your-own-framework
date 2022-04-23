package io.jd.framework.processor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Objects;

public final class Dependency {
    private final TypeElement type;
    private final List<TypeMirror> dependencies;

    public Dependency(TypeElement type, List<TypeMirror> dependencies) {
        this.type = type;
        this.dependencies = dependencies;
    }

    public TypeElement type() {
        return type;
    }

    public List<TypeMirror> dependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Dependency) obj;
        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, dependencies);
    }

    @Override
    public String toString() {
        return "Dependency[" +
                "type=" + type + ", " +
                "dependencies=" + dependencies + ']';
    }
}

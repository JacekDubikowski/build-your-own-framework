package io.jd.framework.processor;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.jd.framework.BeanDefinition;
import io.jd.framework.BeanProvider;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.PUBLIC;

class DefinitionWriter {
    private final TypeElement definedClass;
    private final List<TypeMirror> constructorParameterTypes;
    private final ClassName definedClassName;

    DefinitionWriter(TypeElement definedClass, List<TypeMirror> constructorParameterTypes) {
        this.definedClass = definedClass;
        this.constructorParameterTypes = constructorParameterTypes;
        this.definedClassName = ClassName.get(definedClass);
    }

    public JavaFile createDefinition() {
        ParameterizedTypeName parameterizedBeanDefinition = ParameterizedTypeName.get(ClassName.get(BeanDefinition.class), definedClassName);
        var definitionSpec = TypeSpec.classBuilder("$%s$Definition".formatted(definedClassName.simpleName()))
                .addModifiers(PUBLIC)
                .addSuperinterface(parameterizedBeanDefinition)
                .addMethod(createMethodSpec())
                .addMethod(typeMethodSpec())
                .build();
        return JavaFile.builder(definedClassName.packageName(), definitionSpec).build();
    }

    private MethodSpec typeMethodSpec() {
        var classTypeForDefinedTyped = ParameterizedTypeName.get(ClassName.get(Class.class), definedClassName);

        return MethodSpec.methodBuilder("type")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addCode(CodeBlock.builder()
                        .addStatement("return $T.class", definedClass)
                        .build())
                .returns(classTypeForDefinedTyped)
                .build();
    }

    private MethodSpec createMethodSpec() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(BeanProvider.class, "beanProvider").build())
                .addCode(constructorInvocation(definedClassName))
                .returns(definedClassName)
                .build();
    }

    private CodeBlock constructorInvocation(TypeName typeName) {
        var typeNames = constructorParameterTypes.stream().map(TypeName::get).toList();
        var constructorParameters = typeNames.stream().map(this::providerCall)
                .collect(Collectors.joining(", "));
        return CodeBlock.builder()
                .add("return new ")
                .add("$T", typeName)
                .add("(" + constructorParameters + ");", typeNames.toArray())
                .build();
    }

    private String providerCall(TypeName e) {
        return isBaseOfInterceptedType(e) ? "beanProvider.provideExact($T.class)" : "beanProvider.provide($T.class)";
    }

    private boolean isBaseOfInterceptedType(TypeName e) {
        String possibleInterceptedName = e.withoutAnnotations().toString() + "$Intercepted";
        String definedObjectCanonicalName = definedClassName.withoutAnnotations().canonicalName();
        return possibleInterceptedName.equals(definedObjectCanonicalName);
    }
}

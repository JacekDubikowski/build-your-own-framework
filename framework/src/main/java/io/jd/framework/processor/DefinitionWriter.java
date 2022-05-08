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

import java.util.List;
import java.util.stream.Collectors;

import static javax.lang.model.element.Modifier.PUBLIC;

class DefinitionWriter {
    private final ClassName definedClass;
    private final List<TypeName> constructorParameterTypes;

    DefinitionWriter(ClassName definedClass, List<TypeName> constructorParameterTypes) {
        this.definedClass = definedClass;
        this.constructorParameterTypes = constructorParameterTypes;
    }

    public JavaFile createDefinition() {
        ParameterizedTypeName parameterizedBeanDefinition = ParameterizedTypeName.get(ClassName.get(BeanDefinition.class), definedClass);
        var definitionSpec = TypeSpec.classBuilder("$%s$Definition".formatted(definedClass.simpleName()))
                .addModifiers(PUBLIC)
                .addSuperinterface(parameterizedBeanDefinition)
                .addMethod(createMethodSpec())
                .addMethod(typeMethodSpec())
                .build();
        return JavaFile.builder(definedClass.packageName(), definitionSpec).build();
    }

    private MethodSpec typeMethodSpec() {
        var classTypeForDefinedTyped = ParameterizedTypeName.get(ClassName.get(Class.class), definedClass);

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
                .addCode(constructorInvocation(definedClass, constructorParameterTypes))
                .returns(definedClass)
                .build();
    }

    private CodeBlock constructorInvocation(TypeName typeName, List<TypeName> dependencies) {
        var constructorParameters = dependencies.stream().map(e -> "beanProvider.provide($T.class)").collect(Collectors.joining(", "));
        return CodeBlock.builder()
                .add("return new ")
                .add("$T", typeName)
                .add("(" + constructorParameters + ");", dependencies.toArray())
                .build();
    }
}

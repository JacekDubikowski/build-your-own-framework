package io.jd.framework.processor;

import com.squareup.javapoet.*;
import io.jd.framework.BeanDefinition;
import io.jd.framework.BeanProvider;
import io.jd.framework.ScopeProvider;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static javax.lang.model.element.Modifier.PUBLIC;

class DefinitionWriter {
    private final TypeElement definedClass;
    private final List<TypeMirror> constructorParameterTypes;
    private final ClassName definedClassName;
    private final Types types;
    private final TypeElement collectionElement;

    DefinitionWriter(TypeElement definedClass, List<TypeMirror> constructorParameterTypes, Types types, TypeElement collectionElement) {
        this.definedClass = definedClass;
        this.constructorParameterTypes = constructorParameterTypes;
        this.definedClassName = ClassName.get(definedClass);
        this.types = types;
        this.collectionElement = collectionElement;
    }

    public JavaFile createDefinition() {
        ParameterizedTypeName parameterizedBeanDefinition = ParameterizedTypeName.get(ClassName.get(BeanDefinition.class), definedClassName);
        var definitionSpec = TypeSpec.classBuilder("$%s$Definition".formatted(definedClassName.simpleName()))
                .addModifiers(PUBLIC)
                .addSuperinterface(parameterizedBeanDefinition)
                .addMethod(createMethodSpec())
                .addMethod(typeMethodSpec())
                .addField(scopeProvider())
                .build();
        return JavaFile.builder(definedClassName.packageName(), definitionSpec).build();
    }

    private MethodSpec typeMethodSpec() {
        var classTypeForDefinedTyped = ParameterizedTypeName.get(ClassName.get(Class.class), definedClassName);
        return MethodSpec.methodBuilder("type")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addStatement("return $T.class", definedClass)
                .returns(classTypeForDefinedTyped)
                .build();
    }

    private MethodSpec createMethodSpec() {
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(BeanProvider.class, "beanProvider").build())
                .addStatement("return provider.apply(beanProvider)")
                .returns(definedClassName)
                .build();
    }

    private FieldSpec scopeProvider() {
        ParameterizedTypeName scopeProviderType = ParameterizedTypeName.get(ClassName.get(ScopeProvider.class), definedClassName);
        return FieldSpec.builder(scopeProviderType, "provider", Modifier.FINAL, Modifier.PRIVATE)
                .initializer(singletonScopeInitializer())
                .build();
    }

    private CodeBlock singletonScopeInitializer() {
        var providerCallAndItsTypes = constructorParameterTypes.stream()
                .map(this::processConstructorType)
                .toList();
        var constructorParameters = providerCallAndItsTypes.stream()
                .map(ProviderCallAndItsType::callTemplate)
                .collect(joining(","));
        var types = providerCallAndItsTypes.stream()
                .map(ProviderCallAndItsType::typeName)
                .toArray();

        return CodeBlock.builder()
                .add("ScopeProvider.singletonScope(")
                .add("beanProvider -> ")
                .add("new ")
                .add("$T", definedClassName)
                .add("(" + constructorParameters + ")", types)
                .add(")")
                .build();
    }

    private ProviderCallAndItsType processConstructorType(TypeMirror type) {
        DeclaredType declaredType = (DeclaredType) type;

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.size() == 0) {
            return new ProviderCallAndItsType("beanProvider.provide($T.class)", TypeName.get(type));
        } else if (typeArguments.size() == 1 && types.isAssignable(types.erasure(type), collectionElement.asType())) {
            return new ProviderCallAndItsType("beanProvider.provideAll($T.class)", TypeName.get(typeArguments.get(0)));
        } else {
            throw new RuntimeException("Cannot provide %s".formatted(type));
        }
    }

    private record ProviderCallAndItsType(
            String callTemplate,
            TypeName typeName
    ) {
    }
}

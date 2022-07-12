package io.jd.framework.processor;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import io.jd.framework.Intercepted;
import jakarta.inject.Singleton;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.transaction.TransactionManager;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

class TransactionalInterceptedWriter {
    private static final Modifier[] PRIVATE_FINAL_MODIFIERS = {Modifier.PRIVATE, Modifier.FINAL};

    private final TypeElement transactionalElement;
    private final List<ExecutableElement> transactionalMethods;
    private final PackageElement packageElement;

    TransactionalInterceptedWriter(TypeElement transactionalElement, List<ExecutableElement> transactionalMethods, PackageElement packageElement) {
        this.transactionalElement = transactionalElement;
        this.transactionalMethods = transactionalMethods;
        this.packageElement = packageElement;
    }

    public JavaFile createDefinition() {
        var methods = transactionalMethods.stream().map(this::generateTransactionalMethod).toList();
        TypeSpec typeSpec = TypeSpec.classBuilder("%s$Intercepted".formatted(transactionalElement.getSimpleName().toString()))
                .addAnnotation(Singleton.class)
                .superclass(transactionalElement.asType())
                .addSuperinterface(TypeName.get(Intercepted.class))
                .addField(TransactionManager.class, "transactionManager", PRIVATE_FINAL_MODIFIERS)
                .addField(TypeName.get(transactionalElement.asType()), "delegate", PRIVATE_FINAL_MODIFIERS)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameters(List.of(
                                ParameterSpec.builder(TransactionManager.class, "transactionManager").build(),
                                ParameterSpec.builder(TypeName.get(transactionalElement.asType()), "delegate").build()
                        ))
                        .addCode(CodeBlock.builder()
                                .addStatement("this.transactionManager = transactionManager")
                                .addStatement("this.delegate = delegate")
                                .build())
                        .build())
                .addMethods(methods)
                .build();
        return JavaFile.builder(packageElement.getQualifiedName().toString(), typeSpec).build();
    }

    private MethodSpec generateTransactionalMethod(ExecutableElement executableElement) {
        BiFunction<MethodSpec.Builder, ExecutableElement, MethodSpec.Builder> methodInvoker = voidOrRetuningMethodFactory(executableElement);
        var methodName = executableElement.getSimpleName().toString();

        return methodInvoker.apply(MethodSpec.methodBuilder(methodName).beginControlFlow("try"), executableElement)
                .endControlFlow()
                .beginControlFlow("catch ($T e)", Exception.class)
                .beginControlFlow("try")
                .addStatement("transactionManager.rollback()")
                .endControlFlow()
                .beginControlFlow("catch ($T innerException)", Exception.class)
                .addStatement("throw new $T(innerException)", RuntimeException.class)
                .endControlFlow()
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow()
                .addAnnotation(Override.class)
                .returns(TypeName.get(executableElement.getReturnType()))
                .addTypeVariables(Optional.of(executableElement.getReturnType().getKind() == TypeKind.TYPEVAR).filter(it -> it).map(__ -> (TypeVariableName) TypeVariableName.get(executableElement.getReturnType())).stream().toList())
                .build();
    }

    private BiFunction<MethodSpec.Builder, ExecutableElement, MethodSpec.Builder> voidOrRetuningMethodFactory(ExecutableElement executableElement) {
        return executableElement.getReturnType().getKind() == TypeKind.VOID ? this::voidMethod : this::returningMethod;
    }

    private MethodSpec.Builder voidMethod(MethodSpec.Builder methodBuilder, ExecutableElement method) {
        var methodName = method.getSimpleName();
        return methodBuilder
                .addStatement("transactionManager.begin()")
                .addStatement("delegate.$L()", methodName)
                .addStatement("transactionManager.commit()");
    }

    private MethodSpec.Builder returningMethod(MethodSpec.Builder methodBuilder, ExecutableElement method) {
        var methodName = method.getSimpleName();
        return methodBuilder
                .addStatement("transactionManager.begin()")
                .addStatement("var $LReturnValue = ($L) delegate.$L()", methodName, TypeName.get(method.getReturnType()), methodName)
                .addStatement("transactionManager.commit()")
                .addStatement("return $LReturnValue", methodName);
    }
}

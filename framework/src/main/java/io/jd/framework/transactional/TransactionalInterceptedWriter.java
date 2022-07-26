package io.jd.framework.transactional;

import com.squareup.javapoet.ClassName;
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

import static javax.lang.model.element.Modifier.PUBLIC;

class TransactionalInterceptedWriter {
    private static final String TRANSACTION_MANAGER = "transactionManager";
    private static final String DELEGATE = "delegate";
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
        TypeSpec typeSpec = TypeSpec.classBuilder("%s$Intercepted".formatted(transactionalElement.getSimpleName().toString()))
                .addAnnotation(Singleton.class)
                .superclass(transactionalElement.asType())
                .addSuperinterface(TypeName.get(Intercepted.class))
                .addField(TransactionManager.class, TRANSACTION_MANAGER, PRIVATE_FINAL_MODIFIERS)
                .addField(TypeName.get(transactionalElement.asType()), DELEGATE, PRIVATE_FINAL_MODIFIERS)
                .addMethod(constructor())
                .addMethod(interceptedTypeMethod())
                .addMethods(transactionalMethodDefinitions())
                .build();
        return JavaFile.builder(packageElement.getQualifiedName().toString(), typeSpec).build();
    }

    private MethodSpec constructor() {
        return MethodSpec.constructorBuilder()
                .addParameters(List.of(
                        ParameterSpec.builder(TransactionManager.class, TRANSACTION_MANAGER).build(),
                        ParameterSpec.builder(TypeName.get(transactionalElement.asType()), DELEGATE).build()
                ))
                .addCode(CodeBlock.builder()
                        .addStatement("this.%s = %s".formatted(TRANSACTION_MANAGER, TRANSACTION_MANAGER))
                        .addStatement("this.%s = %s".formatted(DELEGATE, DELEGATE))
                        .build())
                .build();
    }

    private MethodSpec interceptedTypeMethod() {
        return MethodSpec.methodBuilder("interceptedType")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addStatement("return $T.class", TypeName.get(transactionalElement.asType()))
                .returns(ClassName.get(Class.class))
                .build();
    }

    private List<MethodSpec> transactionalMethodDefinitions() {
        return transactionalMethods.stream().map(this::generateTransactionalMethod).toList();
    }

    private MethodSpec generateTransactionalMethod(ExecutableElement executableElement) {
        var methodName = executableElement.getSimpleName().toString();
        var transactionalMethodCall = transactionalMethodCall(executableElement);
        var catchClause = catchClause();
        return MethodSpec.methodBuilder(methodName).beginControlFlow("try")
                .addCode(transactionalMethodCall)
                .endControlFlow()
                .addCode(catchClause)
                .addAnnotation(Override.class)
                .returns(TypeName.get(executableElement.getReturnType()))
                .addTypeVariables(getTypeVariableIfNeeded(executableElement).stream().toList())
                .build();
    }

    private CodeBlock transactionalMethodCall(ExecutableElement executableElement) {
        return executableElement.getReturnType().getKind() == TypeKind.VOID
                ? transactionalVoidCall(executableElement)
                : returningTransactionalMethodCall(executableElement);
    }

    private CodeBlock transactionalVoidCall(ExecutableElement method) {
        return CodeBlock.builder()
                .addStatement(TRANSACTION_MANAGER + ".begin()")
                .addStatement("$L.$L()", DELEGATE, method.getSimpleName())
                .addStatement(TRANSACTION_MANAGER + ".commit()")
                .build();
    }

    private CodeBlock returningTransactionalMethodCall(ExecutableElement method) {
        var methodName = method.getSimpleName();
        return CodeBlock.builder()
                .addStatement(TRANSACTION_MANAGER + ".begin()")
                .addStatement("var $LReturnValue = ($L) $L.$L()", methodName, method.getReturnType(), DELEGATE, methodName)
                .addStatement(TRANSACTION_MANAGER + ".commit()")
                .addStatement("return $LReturnValue", methodName)
                .build();
    }

    private CodeBlock catchClause() {
        return CodeBlock.builder()
                .beginControlFlow("catch ($T e)", Exception.class)
                .beginControlFlow("try")
                .addStatement(TRANSACTION_MANAGER + ".rollback()")
                .endControlFlow()
                .beginControlFlow("catch ($T innerException)", Exception.class)
                .addStatement("throw new $T(innerException)", RuntimeException.class)
                .endControlFlow()
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow()
                .build();
    }

    private Optional<TypeVariableName> getTypeVariableIfNeeded(ExecutableElement executableElement) {
        if (executableElement.getReturnType().getKind() == TypeKind.TYPEVAR) {
            return Optional.of(TypeVariableName.get(executableElement.getReturnType().toString()));
        }
        return Optional.empty();
    }
}

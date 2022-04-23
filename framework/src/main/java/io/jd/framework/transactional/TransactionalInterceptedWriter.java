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
import io.jd.framework.processor.Dependency;
import io.jd.framework.processor.TypeDependencyResolver;
import jakarta.inject.Singleton;

import javax.annotation.processing.Messager;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.transaction.TransactionManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javax.lang.model.element.Modifier.PUBLIC;

class TransactionalInterceptedWriter {
    private static final String TRANSACTION_MANAGER = "transactionManager";
    private static final Modifier[] PRIVATE_FINAL_MODIFIERS = {Modifier.PRIVATE, Modifier.FINAL};

    private final TypeElement transactionalElement;
    private final List<ExecutableElement> transactionalMethods;
    private final PackageElement packageElement;

    TransactionalInterceptedWriter(TypeElement transactionalElement, List<ExecutableElement> transactionalMethods, PackageElement packageElement) {
        this.transactionalElement = transactionalElement;
        this.transactionalMethods = transactionalMethods;
        this.packageElement = packageElement;
    }

    public JavaFile createDefinition(Messager messager) {
        TypeSpec typeSpec = TypeSpec.classBuilder("%s$Intercepted".formatted(transactionalElement.getSimpleName().toString()))
                .addAnnotation(Singleton.class)
                .superclass(transactionalElement.asType())
                .addSuperinterface(TypeName.get(Intercepted.class))
                .addField(TransactionManager.class, TRANSACTION_MANAGER, PRIVATE_FINAL_MODIFIERS)
                .addMethod(constructor(messager))
                .addMethod(interceptedTypeMethod())
                .addMethods(transactionalMethodDefinitions())
                .build();
        return JavaFile.builder(packageElement.getQualifiedName().toString(), typeSpec).build();
    }

    private MethodSpec constructor(Messager messager) {
        Dependency dependency = new TypeDependencyResolver().resolve(transactionalElement, messager);
        var typeNames = dependency.dependencies().stream().map(TypeName::get).toList();
        var constructorParameters = typeNames.stream()
                .map(typeName -> ParameterSpec.builder(typeName, "$" + typeNames.indexOf(typeName)).build())
                .toList();
        var superCallParams = IntStream.range(0, typeNames.size())
                .mapToObj(integer -> "$" + integer)
                .collect(Collectors.joining(", "));

        return MethodSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder(TransactionManager.class, TRANSACTION_MANAGER).build())
                .addParameters(constructorParameters)
                .addCode(CodeBlock.builder()
                        .addStatement("super($L)", superCallParams)
                        .addStatement("this.$L = $L", TRANSACTION_MANAGER, TRANSACTION_MANAGER)
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
        var methodCode = CodeBlock.builder()
                .beginControlFlow("try")
                .add(transactionalMethodCall)
                .endControlFlow()
                .add(catchClause)
                .build();
        return MethodSpec.methodBuilder(methodName)
                .addModifiers(executableElement.getModifiers())
                .addParameters(executableElement.getParameters().stream().map(ParameterSpec::get).toList())
                .addAnnotation(Override.class)
                .addCode(methodCode)
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
        var params = translateMethodToSuperCallParams(method);
        return CodeBlock.builder()
                .addStatement(TRANSACTION_MANAGER + ".begin()")
                .addStatement("super.$L(%s)".formatted(params), method.getSimpleName())
                .addStatement(TRANSACTION_MANAGER + ".commit()")
                .build();
    }

    private CodeBlock returningTransactionalMethodCall(ExecutableElement method) {
        var methodName = method.getSimpleName();
        var params = translateMethodToSuperCallParams(method);
        return CodeBlock.builder()
                .addStatement(TRANSACTION_MANAGER + ".begin()")
                .addStatement("var $LReturnValue = ($L) super.$L(%s)".formatted(params), methodName, method.getReturnType(), methodName)
                .addStatement(TRANSACTION_MANAGER + ".commit()")
                .addStatement("return $LReturnValue", methodName)
                .build();
    }

    private String translateMethodToSuperCallParams(ExecutableElement method) {
        return method.getParameters().stream().map(variableElement -> variableElement.getSimpleName().toString()).collect(Collectors.joining(", "));
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

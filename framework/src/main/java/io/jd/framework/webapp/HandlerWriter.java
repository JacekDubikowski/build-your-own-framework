package io.jd.framework.webapp;

import com.squareup.javapoet.*;
import jakarta.inject.Singleton;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import java.util.List;

class HandlerWriter {

    private final ExecutableElement httpMethodElement;
    private final ExecutableElement produceElement;
    private final ExecutableElement processElement;
    private final ExecutableElement pathElement;
    private final Types typeUtils;
    private final TypeMirror requestType;

    HandlerWriter(ProcessingEnvironment processingEnv) {
        this.typeUtils = processingEnv.getTypeUtils();
        var handlerInterfaceElement = processingEnv.getElementUtils().getTypeElement(RequestHandler.class.getCanonicalName());
        this.httpMethodElement = getMethodElement(handlerInterfaceElement, "method");
        this.produceElement = getMethodElement(handlerInterfaceElement, "produce");
        this.processElement = getMethodElement(handlerInterfaceElement, "process");
        this.pathElement = getMethodElement(handlerInterfaceElement, "path");
        this.requestType = processingEnv.getElementUtils()
                .getTypeElement(Request.class.getCanonicalName())
                .asType();
    }

    private static MethodSpec constructor(TypeName typeName) {
        return MethodSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder(typeName, "controller").build())
                .addCode("this.controller = controller;")
                .build();
    }

    private static ExecutableElement getMethodElement(TypeElement typeElement, String elementName) {
        return ElementFilter.methodsIn(typeElement.getEnclosedElements()).stream()
                .filter(it -> it.getSimpleName().toString().equals(elementName))
                .findFirst()
                .get();
    }

    TypeSpec buildHandler(String handlerMethodName, ExecutableElement handler, TypeName typeName, RequestHandle annotation) {
        return TypeSpec.classBuilder(handlerMethodName)
                .addField(FieldSpec.builder(typeName, "controller", Modifier.FINAL, Modifier.PRIVATE).build())
                .addMethod(constructor(typeName))
                .addAnnotation(Singleton.class)
                .addSuperinterface(TypeName.get(RequestHandler.class))
                .addModifiers(Modifier.FINAL)
                .addMethods(List.of(
                        produce(annotation.produce()),
                        path(annotation.value()),
                        method(annotation.method()),
                        process(handler)
                ))
                .build();
    }

    private MethodSpec method(HttpMethod httpMethod) {
        return MethodSpec.overriding(httpMethodElement)
                .addCode("return $T.$L;", httpMethod.getClass(), httpMethod.toString())
                .build();
    }

    private MethodSpec path(String value) {
        return MethodSpec.overriding(pathElement)
                .addCode("return $S;", value)
                .build();
    }

    private MethodSpec produce(String produce) {
        return MethodSpec.overriding(produceElement)
                .addCode("return $S;", produce)
                .build();
    }

    private MethodSpec process(ExecutableElement handlerMethod) {
        var controllerCall = controllerCall(handlerMethod, handlerMethod.getParameters());
        TypeKind handlerMethodType = handlerMethod.getReturnType().getKind();
        return handlerMethodType == TypeKind.VOID
                ? voidMethod(controllerCall)
                : valueReturningMethod(controllerCall);
    }

    private MethodSpec valueReturningMethod(CodeBlock controllerCall) {
        return MethodSpec.overriding(processElement)
                .addStatement("return $L", controllerCall)
                .build();
    }

    private MethodSpec voidMethod(CodeBlock controllerCall) {
        return MethodSpec.overriding(processElement)
                .addStatement(controllerCall)
                .addStatement("return $T.noContent()", Response.class)
                .build();
    }

    private CodeBlock controllerCall(ExecutableElement handlerMethod, List<? extends VariableElement> parameters) {
        if (parameters.size() > 1 || !doesParamTypesMatchRequest(parameters)) {
            throw new RuntimeException("Too many parameters or type of param is not Request");
        }
        var methodCallParams = parameters.stream().map(VariableElement::getSimpleName).map(Name::toString)
                .findFirst()
                .map(__ -> "(arg0)")
                .orElse("()");
        return CodeBlock.builder()
                .add("controller.$L$L", handlerMethod.getSimpleName().toString(), methodCallParams)
                .build();
    }

    private boolean doesParamTypesMatchRequest(List<? extends VariableElement> parameters) {
        var requestType = this.requestType;
        return parameters.stream().map(VariableElement::asType)
                .allMatch(paramType -> typeUtils.isSameType(paramType, requestType));
    }
}
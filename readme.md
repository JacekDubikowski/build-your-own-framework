# Build your own framework

## Part 1 - Problem

### No framework at all

The application consists of

* ParticipationService and ManualTransactionParticipationService
* EventRepository and EventRepositoryImpl
* ParticipantRepository and ParticipantRepositoryImpl
* Event and EventId
* Participant and ParticipantId

and its goal is to call `participationService.participate(...)`.

It could look like the below code:

```java
public class App {
    public static void main(String[] args) {
        ParticipationService participationService = new ManualTransactionParticipationService(
                new ParticipantRepositoryImpl(),
                new EventRepositoryImpl(),
                new TransactionalManagerStub()
        );
        participationService.participate(new ParticipantId(), new EventId());
    }
}
```

As we can see, the application main method is responsible for providing implementation of interfaces that
ParticipationServiceImpl depends on. Furthermore, it must know which ParticipationService implementation should be
created. The application main method doesn't rely on abstractions for sure.

Can we improve the situation?

## Part 2 - Theory

### Dependency Inversion Principle

By [Wikipedia](https://en.wikipedia.org/wiki/Dependency_inversion_principle) dependency inversion principle states that:
> A. High-level modules should not import anything from low-level modules. Both should depend on abstractions (e.g., interfaces).
>
> B. Abstractions should not depend on details. Details (concrete implementations) should depend on abstractions.

In the previous section, we saw the code that could use some *dependency inversion* (*DI* in shorthand). There is one
well known design pattern that would make it simple to implement the principle.

### Dependency Injection Principle

[Wikipedia](https://en.wikipedia.org/wiki/Dependency_injection) describes *Dependency Injection* as
> a design pattern in which an object or function receives other objects or functions that it depends on.

But how is it done? The pattern separates object creation from its usage. The required objects are provided ("injected")
during runtime and the pattern implementation handles the creation of dependencies.

*NOTE: Dependency Injection is implementation
of [Inversion of control](https://www.wikiwand.com/en/Inversion_of_control)!*

### Available Dependency Injection solutions

There at least a few DI framework that widely adopted in Java world.

* [Spring](https://spring.io) - dependency injection was initial part of the spring project and still is the core
  concept for framework
* [Guice](https://github.com/google/guice) - Google's framework just for DI,
* [Dagger](https://dagger.dev/dev-guide/) - popular in the Android world,
* [Micronaut](https://micronaut.io) - as part of the framework,
* [Quarkus](https://quarkus.io/guides/cdi-reference) - as part of the framework.

Most of them use annotations as one of possible way to configure the bindings. By bindings, I mean, configuration what
implementations should be used or what should be provided to create objects.

The DI is so popular that there was Java Specification Request for [it](https://jcp.org/en/jsr/detail?id=330).

Example from [Micronaut documentation](https://docs.micronaut.io/1.0.0/guide/index.html#beans):

```groovy
import javax.inject.*

interface Engine {
    int getCylinders()

    String start()
}

@Singleton
class V8Engine implements Engine {
    int cylinders = 8

    String start() {
        "Starting V8"
    }
}

@Singleton
class Vehicle {
    final Engine engine

    Vehicle(Engine engine) {
        this.engine = engine
    }

    String start() {
        engine.start()
    }
}
```

In the example we can see the common interface *Engine* with the implementation annotated with *@Singleton*. There is
also the *Vehicle* class that is also annotated *@Singleton*. Therefore, you can simply get *Vehicle* instance with
injected Engine using Micronaut's *BeanContext*. The *V8Engine* will be used for *Engine* implementation as it is the
only candidate.

### Processing of annotations

#### Runtime based

The most popular Java framework, which is Spring, processes annotations in runtime. All dependencies are resolved in
runtime and the solution is heavily based on reflection mechanism. This is one of the possible way to handle annotations
and if you would like to follow that lead please refer to mentioned before
[Java Own Framework - step by step](https://github.com/Patresss/Java-Own-Framework---step-by-step).

#### Compile based

However, there is the other approach. The dependency injection managed during [*annotation
processing*](https://www.youtube.com/watch?v=xswPPwYPAFM) that happens in compile time. It has become popular lately
thanks to Micronaut and Quarkus as they utilize the approach.

The annotation processing isn't just for dependency injection and was mainly use for different thing in the past. One
could recall libraries like [Lombok](https://projectlombok.org) or [MapStruct]() that use the mechanism.

#### Annotation Processor and Processing

The annotation processing is process that happens during compile time. Javac compiler (maybe some other java compilers
like [Jikes](https://en.wikipedia.org/wiki/Jikes)) can use one or more annotation processors during the compilation. The
processing can be used for **generating** and **not modifying** files. Additionally, the processing allows one to do
compile time checks like checking if all fields are final and fail the compilation with proper message.

Annotation processor are written in Java and to be used during the compilation, however, the processor must be compiled
before being used, so it cannot directly process itself. The build tools
like [Maven](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPaths)
or [Gradle](https://docs.gradle.org/4.6/release-notes.html#convenient-declaration-of-annotation-processor-dependencies)
have support for using the processors.

The processing happens in rounds. In every round the compiler searches for annotated elements. Since Java 8, you can
basically annotate everything in the program. Then the compiler matches annotated elements to the processors that
declared being interested in processing them. Any generated files, become input for the next round of the compilation.
If there are no more files to process the compilation ends.

### Writing annotation processor

To write annotation processor you must create the implementation
of *[Processor](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/annotation/processing/Processor.html)*
interface.

The *Processor* defines six methods.

* `void init(ProcessingEnv processingEnv)` - The method in which you can do the initialization of the processor using *
  processingEnv*. The ProcessingEnv interface provides various utilities to work within annotation processing framework,
  like *Filer* or *Messager*. Framework guarantees to provide the implementation in the method.
* `SourceVersion getSupportedSourceVersion()` - As the Javadoc states returns the latest Java version the processor
  works with.
* `Set<String> getSupportedOptions()` - The definition of options that can be passed to processor. We won't use the
  method.
* `Set<String> getSupportedAnnotationTypes()` - Returns annotations that the processor is interested in processing. The
  way to specify annotation is quite complex. Please refer to Javadocs for deeper knowledge.
* `Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText)`
    - The method provides completions for annotations. We are not going to use it.
* `boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv)` - All the work should be done
  here. The *annotations* parameter consists of all annotation interfaces that match the supported annotations. The *
  roundEnv* include information about the current round of processing. The return value tells the compiler if the
  processor claimed the annotations that would prevent subsequent processors from working on them or not and subsequent
  processors may work with them.

Fortunately, the creator of the tool prepared for
us *[AbstractProcessor](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/annotation/processing/AbstractProcessor.html)*
to extends and simplify our job. However, the API of *AbstractProcessor* is just slightly different and provide some
default implementations for the methods.

Once your implementation is ready. You must somehow notify the compiler to use your processor. The `javac` has some
flags for annotation processing but this is not the way you should work with it. To notify the compiler about processor,
you have to specify its name in *META-INF/services/javax.annotation.processing.Processor* file. The name must be fully
qualified and the file can contain more than one processor. The latter approch works with the build tools. No one builds
their project using javac, right?

## Part 3 - Your own DI framework

Note: The annotation processor is a flexible tool. The presented solution highly unlikely to be the only option.

Here comes the main dish of the repository. We are going to build our DI framework together. As the outcome we would
like to see the below code to work.

```java
interface SoftDrink {
    String name();
}

@Singleton
class Water implements SoftDrink {

    @Override
    String name() {
        return "Water";
    }
}

public class App {
    public static void main(String[] args) {
        BeanProvider provider = BeanProviderFactory.getInstance();
        var bean = beanProvider.provider(SoftDrink.class);
        System.out.println(bean.name()); // prints "Water"
    }
}

```

We can see a few in the code. First, we need annotation to point classes to be provided by framework. I made the
decision to use standardised `jakarta.inject.*` library for annotation and to be more precise just
the `jakarta.inject.Singleton`. The same as used by *Micronaut*.

The second thing that we can be sure about is that we need some *BeanProvider*. The frameworks like to call it using
word `Context` like `ApplicationContext`.

The third think is that we need to have some annotation processor that would allow us to get the instances we expect
doing most of the work in compile time. The presented code would be perfectly valid for runtime solution too.

For the sake of simplicity we would assume the framework:

* handles classes annotated with *@Singleton* that has one constructor only,
* would utilise singleton scope and each bean will have only one instance,
* doesn't handle the annotation on one of the constructors or static factory method,
* doesn't
  handle [@Configuration](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/annotation/Configuration.html)
  or [@Factory](https://docs.micronaut.io/2.4.3/api/io/micronaut/context/annotation/Factory.html) style classes.

### How should the framework work?

If the solution was based on the reflection it would be easy to come up with ideas around it and start the coding. The
reflection is powerful but limited in options.

On contrary, the annotation processing approach is both powerful and offers many ways to achieve the goal. Therefore,
the design is the point where we should start. Even if it turns out to be clumsy, it can be worked on during the
development.

The below diagram shows the high level architecture of the desired solution.

![Framework "class" diagram](/docs/Framework.png)

As you can see, we need *BeanProcessor* to generate implementations of *BeanDefinition* for each bean. 
Then the *BeanDefinition*s are picked by *BaseBeanProvider* that implements *BeanProvider*. 
In the application code we use *BeanProvider* that is created for us by *BeanProviderFactory*. 
We also use interface *ScopeProvider<T>* that is supposed to handle scope of the bean lifespan.
In the example we care only for singleton scope.

### Implementation

The framework itself is placed in gradle subproject called *framework*.

#### Basic interfaces

Let's start with the [*BeanDefinition* interface](/framework/src/main/java/io/jd/framework/BeanDefinition.java).

```java
package io.jd.framework;

public interface BeanDefinition<T> {
    T create(BeanProvider beanProvider);

    Class<T> type();
}
```

It has only two methods, `type()` to provider Class object for the bean class and one to build the bean itself.
The `create(...)` method accepts *BeanProvider* to get its dependencies needed during build time as it is not supposed
to create them, hence the dependency injection.

The framework will also need the mentioned [BeanProvider](/framework/src/main/java/io/jd/framework/BeanProvider.java)
that is also an interface with just two methods.

```java
package io.jd.framework;

public interface BeanProvider {
    <T> T provide(Class<T> beanType);

    <T> Iterable<T> provideAll(Class<T> beanType);
}
```

The `provideAll(...)` method provides all beans tht match the parameter `Class<T> beanType`. By match I mean, that the
given bean is subtype/or is the same type as given `beanType`. The `provide(...)` method is basically the same thing,
but provide only one matching bean. If there is no or more than one bean, exception is thrown.

#### Annotation processor

The annotation processor is expected to find classes annotated with *@Singleton*. Then check if they are valid (no
interfaces, abstract classes, just one constructor). And at the end to create the implementation of *BeanDefinition* for
each annotated class.

So we should start by implementing it **right**?

We will get back to it later, now let's focus on implementation.

##### Step 1 - define the processor

Let's define our processor:

```java
import javax.annotation.processing.AbstractProcessor;

class BeanProcessor extends AbstractProcessor { // 1
    
}
```
1. Our processor will extend the provided *AbstractProcessor* instead of doing full implementation of *Processor
interface.

The [actual implementation](/framework/src/main/java/io/jd/framework/processor/BeanProcessor.java) differs from what you are seeing. 
Don't worry, it will be used to full extend in the next **part** of the article. 
The simplified version shown here is enough to do the actual DI work.

##### Step 2 - add annotations!?

```java
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;

@SupportedAnnotationTypes({"jakarta.inject.Singleton"}) // 1
@SupportedSourceVersion(SourceVersion.RELEASE_17) // 2
class BeanProcessor extends AbstractProcessor {

}
```
Thanks to usage of *AbstractProcess* we don't have to override some methods. The annotations can be used instead:
1. `@SupportedAnnotationTypes` corresponds to *Processor.getSupportedAnnotationTypes* and is used to build the returned value.
   As defined, the processor care only for `@jakarta.inject.Singleton`.
2. `@SupportedSourceVersion(SourceVersion.RELEASE_17)` corresponds to *Processor.getSupportedSourceVersion* and is used to build the returned value.
   The processor would support language up to level of Java 17.

##### Step 3 - override the `process` method

Since now, please assume that all code is included in the BeanProcessor class body.

```java
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) { // 1
        try {
            processBeans(roundEnv); // 2
        } catch (Exception e) {
            processingEnv.getMessager() // 3
                .printMessage(ERROR, "Exception occurred %s".formatted(e));
        }
        return false; // 4
    }
```

1. The `annotations` param provide the set of annotation elements, to be more precise *TypeElement*s.
   It may seem unusual, as everyone is used to *java.lang.Class* or broader *java.lang.reflect.Type*. 
   These are runtime representations. 

   On the other hand, there is also compile time representation.

   Let me introduce the [*Element* interface](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/Element.html) 
   that is common interface for all language level constructs such as classes, modules, variables, packages etc. 
   It is worth mentioning that there are subtypes corresponding to the constructs like *PackageElement* or [*TypeElement*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/TypeElement.html).
   
   The processor code is going to use the *Element*s a lot.
2. As the processor should catch any exception to log it, we will just provide try and catch here.
   So the actual processing will be provided in the `BeanProcessor.processBeans`.
3. The annotation processor framework provides the [*Messager*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/annotation/processing/Messager.html) instance to the user through `processingEnv` field of *AbstractProcessor*.
   The *Messager* is a way to report any errors, warnings etc.  
   It defines four overloaded methods `printMessage(...)` and the first parameter of the methods is used to define message type using [Diagnostic.Kind enum](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/tools/Diagnostic.Kind.html).
   In the code there is an example of error message.
4. There is no need to claim the annotations so the method returns `false`.

##### Step 4 - write the acutal processing

```java
    private void processBeans(RoundEnvironment roundEnv) {
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(Singleton.class); // 1
        Set<TypeElement> types = ElementFilter.typesIn(annotated); // 2
        var typeDependencyResolver = new TypeDependencyResolver(); // 3
        types.stream().map(t -> typeDependencyResolver.resolve(t, processingEnv.getMessager())) // 4
                .forEach(this::writeDefinition); // 5
    }
```

1. First, *RoundEnvironment* is used to provide all element from the compilation round annotated with *@Singleton*.
2. Then the [*ElementFilter*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/util/ElementFilter.html) is used to get only *TypeElement*s out of `annotated`.
   It could be wise to fail here, when `annotated` is diffrent in size than `types`, but one can basically annotate anything with *@Singleton*.
   Therefore, we won't care for anything other than [*TypeElement*s](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/TypeElement.html).
   They represent classes and interfaces elements during compilation.
   
   *ElementFilter* is utility class which filters *Iterable<? extends Element>* or *Set<? extends Element>* to get expected elements with type narrowing to expected *Element* implementation.
3. As the next step we instantiate *TypeDependencyResolver* which is part of our framework. The class is responsible for getting type element,
   checking if it has only one constructor and what are the constructor parameters. We would cover its code later on.
4. Then we resolve our dependencies using *TypeResolver* to be able to build our *BeanDefinition* instance.
5. The last thing to do is to write Java files definitions that we would cover in Step 5.

Getting back to TypeDefinitionResolver, the below code shows the implementation:

```java
public class TypeDependencyResolver {

    public Dependency resolve(TypeElement element, Messager messager) {
        return isConcreteClass(element) // 1
                ? resolveConcreteClass(element, messager) // 2
                : failOnInvalidElement(element, messager); // 3
    }

    private Dependency resolveConcreteClass(TypeElement element, Messager messager) {
        var constructors = ElementFilter.constructorsIn(element.getEnclosedElements()); // 4
        return constructors.size() == 1 // 5
                ? resolveDependency(element, constructors) // 6
                : failOnTooManyConstructors(element, messager, constructors); // 7
    }

    private boolean isConcreteClass(TypeElement element) {
        return element.getKind().isClass() && !element.getModifiers().contains(Modifier.ABSTRACT);
    }

    private Dependency resolveDependency(TypeElement element, List<ExecutableElement> constructors) { // 8
        ExecutableElement constructor = constructors.get(0);
        return new Dependency(element, constructor.getParameters().stream().map(VariableElement::asType).toList());
    }
    
    ...
}
```

1. At the beginning the `TypeElement element` is checked for being non abstract class nor interface. 
   As the *TypeElement* can represent them too.
2. In case the `element` is concrete class we follow resolving process.
3. In case the `element` is not concrete class, we short circuit the process and fail. 
   The method implementation can be seen [here](framework/src/main/java/io/jd/framework/processor/TypeDependencyResolver.java).
4. The already known to us *ElementFilter* gets the constructors of the `element`.
5. There is a check for our `element` having just one constructor.
6. In case there is just one constructor, we follow the process.
7. In case there is more than one, the compilation fails.
   The method implementation can be seen [here](framework/src/main/java/io/jd/framework/processor/TypeDependencyResolver.java).
8. The only constructor is used to create *Dependency* object with element and its dependencies.
   It will be used for writing the actual Java code.
   I would be beneficial to see the *Dependency* implementation:
   ```java
   public record Dependency(TypeElement type, List<TypeMirror> dependencies) { 
   }
   ```
   [It ain't much, but it's honest work](https://i.kym-cdn.com/entries/icons/original/000/028/021/work.jpg).
   If you are not aware of *Records* please check the [link](https://docs.oracle.com/en/java/javase/14/language/records.html).
   
   You may have noticed the strange type [*TypeMirror*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/type/TypeMirror.html).
   It represents a type in Java language (literally language, as this is compile time thing). 

##### Step 5 - writing definitions

###### How can I write Java source code?

To write Java code during annotation processing, you can use anything to be honest. 
As long as you end up with *CharSequence*/*String*/*byte[]* you are good to go. 

In the examples you may find in the Internet, it is popular to use *StringBuffer*.
To be honest, I find it inconvenient to write any source code like that. 
There is better solution available for us.

[JavaPoet](https://github.com/square/javapoet) is library for writing Java source code using JavaAPI.
I am not goint to write much about it right now, as you will see it in action in the next section.

###### Missing part of BeanProcessor

Getting back to *BeanProcessor*. Some part of the file were not revealed yet. Let us get back to it:
```java
    private void writeDefinition(Dependency dependency) {
        JavaFile javaFile = new DefinitionWriter(dependency.type(), dependency.dependencies()).createDefinition(); // 1
        writeFile(javaFile);
    }

    private void writeFile(JavaFile javaFile) { // 2
        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(ERROR, "Failed to write definition %s".formatted(javaFile));
        }
    }
```

The writing is done in two steps:
1. Creating actual *BeanDefinition* that is done by *DefinitionWriter* and is contained in JavaFile instance. 
2. Writing the implementation to the actual file using provided in `processingEnv` [Filer](https://cr.openjdk.java.net/~iris/se/17/latestSpec/api/java.compiler/javax/annotation/processing/Filer.html) instance. 
   *Filer* is special interface that supports file creation by an annotation processor. 
   Should writing fail, the compilation will fail and error message will be printed.

The creation of Java code takes place in *DefinitionWriter* and you will see the implementation in a moment.
However, the question is how should such definition look like. I think that example would serve better than 1000 words.

###### Example of what should be written 

For the below Bean:
```java
@Singleton
public class ServiceC {
    private final ServiceA serviceA;
    private final ServiceB serviceB;

    public ServiceC(ServiceA serviceA, ServiceB serviceB) {
        this.serviceA = serviceA;
        this.serviceB = serviceB;
    }
}
```
The definition should look like:
```java
public class $ServiceC$Definition implements BeanDefinition<ServiceC> { // 1
  private final ScopeProvider<ServiceC> provider =  // 2
          ScopeProvider.singletonScope(beanProvider -> new ServiceC(beanProvider.provide(ServiceA.class), beanProvider.provide(ServiceB.class)));

  @Override
  public ServiceC create(BeanProvider beanProvider) { // 3
    return provider.apply(beanProvider);
  }

  @Override
  public Class<ServiceC> type() { // 4
    return ServiceC.class;
  }
}
```
There are four elements here:
1. Inconvenient name to prevent people from using it directly. The class should implement `BeanDefinition<BeanType>`.
2. Field of type *ScopeProvider* that is responsible for instantiation of bean and ensuring its lifetime (scope).
   Singleton scope is the only scope the framework covers, so the *ScopeProvider.singletonScope()* method will be only used.

   The `Function<BeanProvider, Bean>` that is used to instantiate the bean is passed to the method `ScopeProvider.singletonScope`.

   The implementation of the *ScopeProvider* will be covered later.
   For now, it is enough to embrace that it will ensure just one instance of the bean in our DI context.

   If you are impatient, the source code is available [here](/framework/src/main/java/io/jd/framework/ScopeProvider.java).
3. The actual `create` method that uses `provider` and connects it with `beanProvider` through `apply` method.
4. The implementation of `type` method, which is simple task.

The example shows, the only bean specific thing is type passed to BeanDefinition declaration, `new` call and field/returned types.

###### Implementation of *DefinitionWriter*

Let us see the Java code that writes Java code. Link to the [code](/framework/src/main/java/io/jd/framework/processor/DefinitionWriter.java).

```java
class DefinitionWriter {
    private final TypeElement definedClass; // 1
    private final List<TypeMirror> constructorParameterTypes; // 1
    private final ClassName definedClassName; // 1

    DefinitionWriter(TypeElement definedClass, List<TypeMirror> constructorParameterTypes) {
        this.definedClass = definedClass;
        this.constructorParameterTypes = constructorParameterTypes;
        this.definedClassName = ClassName.get(definedClass);
    }

    public JavaFile createDefinition() {
        ParameterizedTypeName parameterizedBeanDefinition = ParameterizedTypeName.get(ClassName.get(BeanDefinition.class), definedClassName); // 3
        var definitionSpec = TypeSpec.classBuilder("$%s$Definition".formatted(definedClassName.simpleName())) // 2
                .addModifiers(PUBLIC) // Making the class public
                .addSuperinterface(parameterizedBeanDefinition) // 3
                .addMethod(createMethodSpec()) // 4
                .addMethod(typeMethodSpec()) // 5
                .addField(scopeProvider()) // 6
                .build();
        return JavaFile.builder(definedClassName.packageName(), definitionSpec).build(); // 7
    }
    
    private MethodSpec createMethodSpec() { // 4
        return MethodSpec.methodBuilder("create")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(ParameterSpec.builder(BeanProvider.class, "beanProvider").build())
                .addStatement("return provider.apply(beanProvider)")
                .returns(definedClassName)
                .build();
    }

    private MethodSpec typeMethodSpec() { // 5
        var classTypeForDefinedTyped = ParameterizedTypeName.get(ClassName.get(Class.class), definedClassName);
        return MethodSpec.methodBuilder("type")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addStatement("return $T.class", definedClass)
                .returns(classTypeForDefinedTyped)
                .build();
    }

    private FieldSpec scopeProvider() { // 6
        ParameterizedTypeName scopeProviderType = ParameterizedTypeName.get(ClassName.get(ScopeProvider.class), definedClassName);
        return FieldSpec.builder(scopeProviderType, "provider", Modifier.FINAL, Modifier.PRIVATE)
                .initializer(constructorInvocation())
                .build();
    }
    
    private CodeBlock constructorInvocation() {
        var typeNames = constructorParameterTypes.stream().map(TypeName::get).toList();
        var constructorParameters = typeNames.stream().map(__ -> "beanProvider.provide($T.class)")
                .collect(Collectors.joining(", "));
        return CodeBlock.builder()
                .add("ScopeProvider.singletonScope(")
                .add("beanProvider -> ")
                .add("new ")
                .add("$T", definedClassName)
                .add("(" + constructorParameters + ")", typeNames.toArray())
                .add(")")
                .build();
    }
}
```

Phew, that is a lot. Don't be afraid it's fairly simple.
1. There are three instance fields. 
   `TypeElement definedClass` is our bean, 
   `List<TypeMirror> constructorParameterTypes` contains parameters for bean constructor (who would guess, right?)
   `ClassName definedClassName` is JavaPoet object, created out of `definedClass` and represents fully qualified name for classes.
2. *TypeSpec* is JavaPoet class to represent Java type creation (classes and interfaces). 
   It is created using `classBuilder` static method, in which we pass our strange name, constructed based on the actual bean type name.
3. `ParameterizedTypeName.get(ClassName.get(BeanDefinition.class), definedClassName)` creates code that represents `BeanDefinition<BeanTypeName>`
   that is applied as superinterface of our class through `addSuperinterface` method.
4. The `create()` method implementation not that hard and self-explanatory. Please just look at `createMethodSpec()` method and its application.
5. The same applies for `type()` method as for the `create()`.
6. The `scopeProvider()` is similar to the previous methods, however, the tricky part is to invoke constructor. 
   The `constructorInvocation()` is responsible for creating the constructor invocation. 
   For every parameter, we just call `BeanProvider.provide` to get the dependency and we keep the order of the constructor parameters.
   Thankfully, the order is preserved thanks to *List*, *Dependency* and *TypeDependencyResolver* classes and their properties.

Ok, the *BeanDefinition*s are ready. Now, we have to collect them somehow to provide the framework user with their beans.

###### Did we miss something?

Shouldn't have we started with tests of the annotation processor? But how can the annotation processor be tested?

##### Annotation processor testing

The annotation processor is rather poorly prepared for being tested. The one way is to create separate project/Gradle or
Maven submodule. It would then use the annotation processor and compilation failure would mean something is wrong. It
doesn't sound good right?

The other option is to utilise the [compile-testing](https://github.com/google/compile-testing) library created by
Google. It simplifies the testing process, even though the tool isn't perfect. Please find the
tutorial [here](https://chermehdi.com/posts/compiler-testing-tutorial/).

To see if the annotation processor works, both approaches were introduced. The *compile-testing* was used for "unit
tests" and *integrationTest* module for "integration tests".

Please refer to [build.gradle](/framework/build.gradle) file from *framework* subproject.

Please refer to [test dir](/framework/src/test/java) and [integrationTest dir](framework/src/integrationTest/java).

## Part 4 - Transactions

## Afterwords

### My inspiration

The repository is based on the other existing, awesome and fabulous
repository [Java Own Framework - step by step](https://github.com/Patresss/Java-Own-Framework---step-by-step).

Kudos to [Patresss](https://github.com/Patresss)!

The solution presented was created based on [Micronaut](https://micronaut.io). My work with the framework made me
interested in annotation processing.

Kudos to Micronaut team for their excellent work!

### The repository

The repository is meant to be worked on in the future using iterative approach. It is not done yet and I hope that I
will find the time to develop it further.

The code is neither the best possible nor handling all corner cases. It was never the point to create fully-fledged
framework.

Nevertheless, if you have advice for me, or you have found a bug or would like to see some changes, please create the
issue. I might pick it up one day and I will be very grateful.

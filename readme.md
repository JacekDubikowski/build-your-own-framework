# Build your own framework

## Introduction

A majority of developers in the JVM world work on various web applications, most of which are based on a framework like Spring or Micronaut.
However, some people state that frameworks produce too an big overhead.
I decided to see how valid such claims are and how much work is necessary to replicate what frameworks provide us out-of-the-box.

This text isn’t about whether or not it is feasible to use a framework or when to use it. 
It is about writing your framework - tinkering is the best way of learning!

PS. The readme is also available in the form of three articles:

1. [Start and Dependency Injection](https://medium.com/p/9824be4fb9a7)
2. [Transactional](https://medium.com/p/d2d6aa5c63a1)
3. *Web and controllers* - will be available in the future.


## Part 1 - No framework

For the sake of simplicity, we will use a demo app code for the first part.
The application consists of

* Singular service
* Two repositories
* Two POJOs

## Part 1 - No framework

The starting point of an application without a framework would look like the [code](/testapp/src/main/java/io/jd/testapp/NoFrameworkApp.java) below:

```java
public class NoFrameworkApp {
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

As we can see, the application’s main method is responsible for providing the implementation of interfaces that *ManualTransactionParticipationService* depends on. 
The implementer must know which ParticipationService implementation should be created in main method. 
When using a framework, programmers typically don’t need to create instances and dependencies on their own. 
They rely on the core feature of the frameworks - **Dependency Injection**.

So, let’s take a look at a simple implementation of the dependency injection container based on annotation processing.

## What is a Dependency Injection?

### Dependency Injection Pattern

*Dependency Injection*, or *DI*, is a pattern for providing class instances its instance variables (its dependencies).

But how is this done? The pattern separates responsibility for object creation from its usage. 
The required objects are provided ("injected") during runtime, 
and the pattern's implementation handles the creation and lifecycle of the dependencies.

The feature has its advantages, like decreased coupling, simplified testing and increased flexibility. 
But also drawbacks: framework dependence, harder debugging or more work at the beginning of the project. 

*NOTE: Dependency Injection is the implementation of [Inversion of control](https://www.martinfowler.com/bliki/InversionOfControl.html)!*

### Available Dependency Injection solutions

There are at least a few DI frameworks widely adopted in the Java world.

* [Spring](https://spring.io) - DI was the initial part of this project, and it’s still the core concept for the framework.
* [Guice](https://github.com/google/guice) - Google's DI framework/library.
* [Dagger](https://dagger.dev/dev-guide/) - popular in the Android world.
* [Micronaut](https://micronaut.io) - part of the framework.
* [Quarkus](https://quarkus.io/guides/cdi-reference) - part of the framework.
* [Java/Jakarta CDI](https://www.cdi-spec.org/) - standard DI framework that originates in Java EE 6.

Most of them use annotations as one of the possible ways to configure the bindings. 
By bindings, I mean the configuration of which implementations should be used for interfaces or which dependencies should be provided to create objects.

In fact, DI is so popular that there was a [Java Specification Request](https://jcp.org/en/jsr/detail?id=330) made for it.

### Annotations handling

#### Runtime-based handling

Spring, the most popular Java framework, processes annotations in runtime. 
The solution is heavily based on the reflection mechanism.
The reflection-based approach is one of the possible ways to handle annotations, and if you would like to follow that lead, please refer to
[Java Own Framework - step by step](https://github.com/Patresss/Java-Own-Framework---step-by-step).

#### Compile-based handling

In addition to runtime handling, there is another approach.
The **part** of the dependency injection can happen during [*annotation processing*](https://www.youtube.com/watch?v=xswPPwYPAFM) a process that occurs during compile time.
It has become popular lately thanks to Micronaut and Quarkus as they utilise the approach.

Annotation processing isn’t just for dependency injection. 
It is a part of various tools.
For example, in libraries like [Lombok](https://projectlombok.org) or [MapStruct](https://mapstruct.org).

#### Annotation Processing and Processors

The purpose of annotation processing is to **generate not modify** files. 
It can also make some compile-time checks, like ensuring that all class fields are final. 
If something is wrong, the processor may fail the compilation and provide the programmer with information about an error.

Annotation processors are written in Java and are used by `javac` during the compilation.
However, programmers must compile the processor before using it. 
cannot directly process itself.

The processing happens in rounds. In every round, the compiler searches for annotated elements. 
Then the compiler matches annotated elements to the processors that declared being interested in processing them.
Any generated files become input for the next round of the compilation.
If there are no more files to process, the compilation ends.

##### How to observe the work of annotation processors

There are two compiler flags `-XprintProcessorInfo` and `-XprintRounds` that will present the information about the compilation process and the compilation rounds.

```shell
Round 1:
        input files: {io.jd.Data}
        annotations: [io.jd.AllFieldsFinal]
        last round: false
Processor io.jd.AnnotationProcessor matches [/io.jd.SomeAnnotation] and returns true.
Round 2:
        input files: {}
        annotations: []
        last round: true

```

You can find an example config for Gradle [here](/framework/build.gradle).

### How to write an annotation processor

To write an annotation processor, you must create the *[Processor](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/annotation/processing/Processor.html)*
interface implementation.

The *Processor* defines six methods, which is a lot to implement.
Fortunately, the tool's creator prepared the
*[AbstractProcessor](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/annotation/processing/AbstractProcessor.html)*
to be extended, thus simplify programmer's job.
The *AbstractProcessor*'s API is slightly different from the *Processor*'s and provides some default implementations of the methods for us.

Once the implementation is ready, you must notify the compiler to use your processor. The `javac` has some
flags for annotation processing, but this is not how you should work with it. To notify the compiler about the processor,
you must specify its name in *META-INF/services/javax.annotation.processing.Processor* file. The name must be fully
qualified, and the file can contain more than one processor. The latter approach works with the build tools. No one builds
their project using javac, right?

##### Build tools support

The build tools
like [Maven](https://maven.apache.org/plugins/maven-compiler-plugin/compile-mojo.html#annotationProcessorPaths)
or [Gradle](https://docs.gradle.org/4.6/release-notes.html#convenient-declaration-of-annotation-processor-dependencies)
have support for using the processors.

## Creating your own DI framework

As mentioned above, the [Java Own Framework - step by step](https://github.com/Patresss/Java-Own-Framework---step-by-step) article covers how the DI’s runtime annotation processing works.
As a counterpart, I will gladly show the basic compile-time framework. 
The approach has some advantages over the 'classic' one. 
You can read more about it [in the Micronaut release notes](https://micronaut.io/2018/09/30/micronaut-1-0-rc1-and-the-power-of-ahead-of-time-compilation/#:~:text=REFLECTION%20AND%20MAKING%20JAVA%20FRAMEWORKS%20MORE%20EFFICIENT).
Neither the framework we are building nor Micronaut is reflection-free, but it relies on it partially and in a limited manner.

Note: An annotation processor is a flexible tool. The presented solution is highly unlikely to be the only option.

Here comes the main dish of the repository. 
We are going to build our DI framework together. The goal is to make the code below work.

```java
interface Water {
    String name();
}

@Singleton
class SparklingWater implements Water {

    @Override
    String name() {
        return "Bubbles";
    }
}

public class App {
    public static void main(String[] args) {
        BeanProvider provider = BeanProviderFactory.getInstance();
        var bean = beanProvider.provider(SoftDrink.class);
        System.out.println(bean.name()); // prints "Bubbles"
    }
}
```

We can make some assumptions based on the code above. 
First, we need the framework to provide annotations for pointing classes. 
I decided to use the standardised `jakarta.inject.*` library for annotation. 
To be more precise, just the `jakarta.inject.Singleton`.
The same is used by *Micronaut*.

The second thing we can be sure about is that we need a *BeanProvider*. 
The frameworks like to refer to it using the word `Context`, like `ApplicationContext`.

The third necessary thing is an annotation processor that will process the mentioned annotation(s). 
It should produce classes allowing the framework to provide the expected dependencies in runtime.

The framework should use the reflection mechanism as little as possible.

For the sake of simplicity, we would assume the framework:

* handles concrete classes annotated with *@Singleton* that have one constructor only,
* utilises singleton scope (each bean will have only one instance for a given *BeanProvider*).

### How should the framework work?

The annotation processing approach is powerful and offers many ways to achieve the goal. 
Therefore, the design is the point where we should start.
We will begin with a basic version, which we will develop gradually as the text develops.

The diagram below shows the high-level architecture of the desired solution.

![Framework "class" diagram](./docs/Framework.png)

As you can see, we need a *BeanProcessor* to generate implementations of the *BeanDefinition* for each bean. 
Then the *BeanDefinition*s are picked by *BaseBeanProvider*, which implements the [*BeanProvider*](/framework/src/main/java/io/jd/framework/BeanProvider.java) (not in the diagram). 
In the application code, we use the *BaseBeanProvider*, created for us by the *BeanProviderFactory*. 
We also use the *ScopeProvider<T>* interface that is supposed to handle the scope of the bean lifespan.
In the example, as mentioned, we only care about the singleton scope.

### Implementation of the framework

The framework itself is placed in the Gradle subproject called *framework*.

#### Basic interfaces

Let's start with the [*BeanDefinition* interface](/framework/src/main/java/io/jd/framework/BeanDefinition.java).

```java
package io.jd.framework;

public interface BeanDefinition<T> {
    T create(BeanProvider beanProvider);

    Class<T> type();
}
```

The interface has only two methods: `type()` to provide a *Class* object for the bean class and one to build the bean itself.
The `create(...)` method accepts the *BeanProvider* to get its dependencies needed during build time as it is not supposed
to create them, hence the DI.

The framework will also need the [BeanProvider](/framework/src/main/java/io/jd/framework/BeanProvider.java), interface with just two methods.

```java
package io.jd.framework;

public interface BeanProvider {
    <T> T provide(Class<T> beanType);

    <T> Iterable<T> provideAll(Class<T> beanType);
}
```

The `provideAll(...)` method provides all beans that match the parameter `Class<T> beanType`. 
By match, I mean that the given bean is subtype or is the same type as the given `beanType`. 
The `provide(...)` method is almost the same but provides only one matching bean. 
An exception is thrown in the case of no beans or more than one bean.

#### Annotation processor

We expect the annotation processor to find classes annotated with *@Singleton*. 
Then, check if they are valid (no interfaces, abstract classes, just one constructor).
The final step is creating the implementation of the *BeanDefinition* for each annotated class.

So we should start by implementing it, **right**?

The test-driven-development would object. 
We will get back to the tests later. 
Now, let’s focus on implementation.

##### Step 1 - define the processor

Let's define our processor:

```java
import javax.annotation.processing.AbstractProcessor;

class BeanProcessor extends AbstractProcessor {
    
}
```

Our processor will extend the provided *AbstractProcessor* instead of fully implementing the *Processor* interface.

The [actual implementation](/framework/src/main/java/io/jd/framework/processor/BeanProcessor.java) differs from what you are seeing. 
Don't worry; it will be used to the full extent in the next **step** of the text. 
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

Thanks to the usage of the *AbstractProcess*, we don't have to override some methods. 
The annotations can be used instead:

1. `@SupportedAnnotationTypes` corresponds to *Processor.getSupportedAnnotationTypes* and is used to build the returned value.
   As defined, the processor cares only for `@jakarta.inject.Singleton`.
2. `@SupportedSourceVersion(SourceVersion.RELEASE_17)` corresponds to *Processor.getSupportedSourceVersion* and is used to build the returned value.
   The processor will support language up to the level of Java 17.

##### Step 3 - override the `process` method

Please assume that the code below is included in the BeanProcessor class body.

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

1. The `annotations` param provides a set of annotations represented as *Element*s. 
   The annotations are represented at least by the *TypeElement*s interface.
   It may seem unusual, as everyone is used to *java.lang.Class* or broader *java.lang.reflect.Type*,
   which is runtime representations. 

   On the other hand, there is also the compile-time representation.

   Let me introduce the [*Element* interface](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/Element.html),
   the common interface for all language-level compile-time constructs such as classes, modules, variables, packages. 
   It is worth mentioning that there are subtypes corresponding to the constructs like *PackageElement* or [*TypeElement*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/TypeElement.html).
   
   The processor code is going to use the *Element*s a lot.
2. As the processor should catch any exception and log it, we will use the `try` and `catch` clauses here.
   The `BeanProcessor.processBeans` method will provide the actual annotation processing.
3. The annotation processor framework provides the [*Messager*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/annotation/processing/Messager.html) instance to the user through the `processingEnv` field of *AbstractProcessor*.
   The *Messager* is a way to report any errors, warnings, etc.  
   It defines four overloaded methods `printMessage(...)`, and the first parameter of the methods is used to define message type using [Diagnostic.Kind enum](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/tools/Diagnostic.Kind.html).
   In the code, there is an example of an error message. 
   If a processor throws an exception, the compilation will fail without extra diagnostic data.
4. There is no need to claim the annotations, so the method returns `false`.

##### Step 4 - write the actual processing

```java
    private void processBeans(RoundEnvironment roundEnv) {
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(Singleton.class); // 1
        Set<TypeElement> types = ElementFilter.typesIn(annotated); // 2
        var typeDependencyResolver = new TypeDependencyResolver(); // 3
        types.stream().map(t -> typeDependencyResolver.resolve(t, processingEnv.getMessager())) // 4
                .forEach(this::writeDefinition); // 5
    }
```

1. First, the *RoundEnvironment* is used to provide all elements from the compilation round annotated with *@Singleton*.
2. Then the [*ElementFilter*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/util/ElementFilter.html) is used to get only *TypeElement*s out of `annotated`.
   It could be wise to fail here when `annotated` differs in size from `types`, but one can annotate anything with *@Singleton*, and we don't want to handle that.
   Therefore, we won't care for anything other than [*TypeElement*s](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/TypeElement.html).
   They represent class and interface elements during compilation.
   
   The *ElementFilter* is a utility class that filters *Iterable<? extends Element>* or *Set<? extends Element>* to get elements matching criteria with type narrowed to matching *Element* implementation.
3. As the next step, we instantiate the *TypeDependencyResolver*, which is part of our framework. The class is responsible for getting the type element,
   checking if it has only one constructor and what are the constructor parameters. We will cover its code later on.
4. Then we resolve our dependencies using the *TypeResolver* to be able to build our *BeanDefinition* instance.
5. The last thing to do is write Java files with definitions. We will cover it in Step 5.

Getting back to the *TypeDefinitionResolver*, the code below shows the implementation:

```java
public class TypeDependencyResolver {

    public Dependency resolve(TypeElement element, Messager messager) {
       var constructors = ElementFilter.constructorsIn(element.getEnclosedElements()); // 1
       return constructors.size() == 1 // 2
               ? resolveDependency(element, constructors) // 3
               : failOnTooManyConstructors(element, messager, constructors); // 4
    }

    private Dependency resolveDependency(TypeElement element, List<ExecutableElement> constructors) { // 5
        ExecutableElement constructor = constructors.get(0);
        return new Dependency(element, constructor.getParameters().stream().map(VariableElement::asType).toList());
    }
    ...
}
```

1. The *ElementFilter*, which we’re already familiar with, gets the constructors of the `element`.
2. A check is carried out to ensure `element` has just one constructor.
3. If there is one constructor, we follow the process.
4. In case there is more than one, the compilation fails.
   You can see the `failOnTooManyConstructors` method implementation [here](framework/src/main/java/io/jd/framework/processor/TypeDependencyResolver.java).
5. The single constructor creates a *Dependency* object with the element and its dependencies.
   It will be used for writing the actual Java code.
   Seeing the *Dependency* implementation would be beneficial, so please take a look:
   ```java
   public final class Dependency {
       private final TypeElement type;
       private final List<TypeMirror> dependencies;
   
       ...
   
       public TypeElement type() {
           return type;
       }
   
       public List<TypeMirror> dependencies() {
           return dependencies;
       }
       ...
   }
   ```
   
   [It ain't much, but it's honest work](https://i.kym-cdn.com/entries/icons/original/000/028/021/work.jpg).
   You may have noticed the strange type [*TypeMirror*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/type/TypeMirror.html).
   It represents a type in Java language (literally language, as this is a compile-time thing). 

##### Step 5 - Writing definitions

###### How can I write Java source code?

To write Java code during annotation processing, you can use almost anything.
You are good to go as long as you end up with *CharSequence*/*String*/*byte[]*. 

In examples on the Internet, you will find that it is popular to use *StringBuffer*.
Honestly, I find it inconvenient to write any source code like that. 
There is a better solution available for us.

[JavaPoet](https://github.com/square/javapoet) is a library for writing Java source code using JavaAPI.
You will see it in action in the next section.

###### Missing part of BeanProcessor

Getting back to *BeanProcessor*. 
Some parts of the file haven’t been revealed yet. 
Let us get back to it. Let us get back to it:

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

1. The *DefinitionWriter* creates the *BeanDefinition*, and a JavaFile instance contains it. 
2. The programmer writes the implementation to the actual file using provided via `processingEnv` [Filer](https://cr.openjdk.java.net/~iris/se/17/latestSpec/api/java.compiler/javax/annotation/processing/Filer.html) instance. 
   Should writing fail, the compilation will fail, and the compiler will print the error message.
   
*Filer* is an interface that supports file creation for an annotation processor. 
The place for the generated files to be stored is configured through the `-s` javac flag. 
However, most of the time, build tools handle it for you. 
In that case, the files are stored in a directory like `build/generated/sources/annotationProcessor/java` for Gradle 
or similar for different build tools.

The creation of Java code takes place in *DefinitionWriter*, and you will see the implementation in a moment.
However, the question is what such a definition looks like. I think an example will show it best.

###### An example of what should be written 

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

The definition should look like the code below:

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

1. An inconvenient name to prevent people from using it directly. The class should implement `BeanDefinition<BeanType>`.
2. A field of type *ScopeProvider*, responsible for instantiation of bean and ensuring its lifetime (scope).
   Singleton scope is the only scope the framework covers, so the *ScopeProvider.singletonScope()* method will be the only one used.

   The `Function<BeanProvider, Bean>`, used to instantiate the bean is passed to the method `ScopeProvider.singletonScope`.

   I will cover the implementation of the *ScopeProvider* later.
   For now, it is enough to know that it will ensure just one instance of the bean in our DI context.

   However, if you are curious, the source code is available [here](/framework/src/main/java/io/jd/framework/ScopeProvider.java).
3. The actual `create` method uses the `provider` and connects it with the`beanProvider` through the `apply` method.
4. The implementation of the `type` method is a simple task.

The example shows that the only bean-specific things are the type passed to BeanDefinition declaration, `new` call, and field/returned types.

###### Implementation of the *DefinitionWriter*

To keep this concise, I will omit the private methods’ code, the constructor and some small snippets.
Let us see the overview of Java code that writes Java code.
Here is a link to the full [code](/framework/src/main/java/io/jd/framework/processor/DefinitionWriter.java).

```java
class DefinitionWriter {
    private final TypeElement definedClass; // 1
    private final List<TypeMirror> constructorParameterTypes; // 1
    private final ClassName definedClassName; // 1

    public JavaFile createDefinition() {
        ParameterizedTypeName parameterizedBeanDefinition = ParameterizedTypeName.get(ClassName.get(BeanDefinition.class), definedClassName); // 3
        var definitionSpec = TypeSpec.classBuilder("$%s$Definition".formatted(definedClassName.simpleName())) // 2
                .addSuperinterface(parameterizedBeanDefinition) // 3
                .addMethod(createMethodSpec()) // 4
                .addMethod(typeMethodSpec()) // 5
                .addField(scopeProvider()) // 6
                .build();
        return JavaFile.builder(definedClassName.packageName(), definitionSpec).build(); // 7
    }
    
    private MethodSpec createMethodSpec() { ... } // 4

    private MethodSpec typeMethodSpec() { ... } // 5

    private FieldSpec scopeProvider() { ... }  // 6
    
    private CodeBlock singletonScopeInitializer() { ... }  // 6
}
```

Phew, that is a lot. Don't be afraid it's simpler than it looks.

1. There are three instance fields:
   * `TypeElement definedClass` is our bean, 
   * `List<TypeMirror> constructorParameterTypes` contains parameters for bean constructor (who would guess, right?),
   * `ClassName definedClassName` is the JavaPoet object, created out of `definedClass`. It represents a fully qualified name for classes.
2. *TypeSpec* is a JavaPoet class representing Java type creation (classes and interfaces). 
   It is created using the `classBuilder` static method, in which we pass our strange name, constructed based on the actual bean type name.
3. `ParameterizedTypeName.get(ClassName.get(BeanDefinition.class), definedClassName)` creates code that represents `BeanDefinition<BeanTypeName>`,
   which is applied as a super interface of our class through the `addSuperinterface` method.
4. The `create()` method implementation is not that hard, and self-explanatory. 
   Please look at the `createMethodSpec()` method and its application.
5. The same applies to the `type()` method as for the `create()`.
6. The `scopeProvider()` is similar to the previous methods. 
   However, the tricky part is to invoke the constructor. 
   The `singletonScopeInitializer()` is responsible for creating a constructor call wrapped in `ScopeProvider.singletonScope(beanProvider -> ...`). 
   We call `BeanProvider.provide` for every parameter to get the dependency and keep the calls in the order of the constructor parameters.

Ok, the *BeanDefinition*s are ready. Now, we move on to the *ScopeProvider*.

###### ScopeProvider implementation

```java
public interface ScopeProvider<T> extends Function<BeanProvider, T> { // 1

    static <T> ScopeProvider<T> singletonScope(Function<BeanProvider, T> delegate) { // 2
        return new SingletonProvider<>(delegate);
    }
}

final class SingletonProvider<T> implements ScopeProvider<T> { // 3
    private final Function<BeanProvider, T> delegate;
    private volatile T value;

    SingletonProvider(Function<BeanProvider, T> delegate) {
        this.delegate = delegate;
    }

    public synchronized T apply(BeanProvider beanProvider) {
        if (value == null) {
            value = delegate.apply(beanProvider);
        }
        return value;
    }
}
```

1. You can see the sealed interface definition that extends `Function<BeanProvider, T>`. 
   So the `Function.apply()` method is available.
2. Factory method for SingletonProvider
3. Implementation of the SingletonScope is based on any kind of lazy value implementation in Java.
   In the synchronized `apply` method, we onlu create the instance of our bean if there isn't one.
   The value field is marked as `volatile` to prevent issues in a multithreaded environment.

Now we are ready. It is time for the runtime part of the framework.

##### Step 6 - runtime provisioning of beans

Runtime provisioning is the last part of the framework to work on. The *BeanProvider* interface has already been defined. 
Now we just need the implementation to do the actual provisioning.

The *BaseBeanProvider* must have access to all instantiated *BeanDefinition*s. 
This is because the *BaseBeanProvider* shouldn't be responsible for creating and providing the beans.

###### The BeanProviderFactory

Due to the mentioned fact, the *BeanProviderFactory* took responsibility via the `static BeanProvider getInstance(String... packages)` method.
Where `packages` parameter defines places to look for the *BeanDefinition*s present on the classpath.
This is the code:

```java
public class BeanProviderFactory {

    private static final QueryFunction<Store, Class<?>> TYPE_QUERY = SubTypes.of(BeanDefinition.class).asClass(); // 2

    public static BeanProvider getInstance(String... packages) { // 1
        ConfigurationBuilder reflectionsConfig = new ConfigurationBuilder() // 3
                .forPackages("io.jd") // 4
                .forPackages(packages) // 4
                .filterInputsBy(createPackageFilter(packages)); // 4
        var reflections = new Reflections(reflectionsConfig); // 5
        var definitions = definitions(reflections); // 6
        return new BaseBeanProvider(definitions); // 8
    }

    private static FilterBuilder createPackageFilter(String[] packages) { // 4
       var filter = new FilterBuilder().includePackage("io.jd");
       Arrays.asList(packages).forEach(filter::includePackage);
       return filter;
    }
    
    private static List<? extends BeanDefinition<?>> definitions(Reflections reflections) { // 6
        return reflections
                .get(TYPE_QUERY)
                .stream()
                .map(BeanProviderFactory::getInstance) // 7
                .toList();                                                                  
    }

    private static BeanDefinition<?> getInstance(Class<?> e) { // 7
        try {
            return (BeanDefinition<?>) e.getDeclaredConstructors()[0].newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new FailedToInstantiateBeanDefinitionException(e, ex);
        }
    }
}
```

1. The method is responsible for getting an instance of the *BeanProvider*.
2. Here is where it gets interesting. I define constant `TYPE_QUERY` with a very specific type from the [Reflections library](https://github.com/ronmamo/reflections).
   The project [README.md](https://github.com/ronmamo/reflections/blob/master/README.md) defines it as:

   > *Reflections* scans and indexes your project's classpath metadata, allowing reverse transitive query of the type system on runtime.
   
I encourage you to read more about it, but I will just explain how it is used in the code.
The defined *QueryFunction* will be used to scan the classpath in runtime to find all subtypes of the *BeanDefinition*.

3. The configuration is created for the *Reflections* object. It will be used in the next part of the code.
4. The configuration is defined through parameters and the package filter that the BeanProviderFactory will scan the `io.jd` package and the passed `packages` param. 
   Thanks to that, the framework only provides beans from the expected packages. 
5. The Reflections object is created. It will be responsible for performing our query later in the code.
6. The `reflections` object performs the `TYPE_QUERY`.
   It will create all the *BeanDefinition* instances using `static BeanDefinition<?> getInstance(Class<?> e)`.
7. The method that creates instances of *BeanDefinition* uses the reflection.
   When there's an exception, the code wraps it in a custom RuntimeException. 
   The code of the custom exception is [here](/framework/src/main/java/io/jd/framework/FailedToInstantiateBeanDefinitionException.java).
8. The instance of *BeanProvider* interface in the form of BaseBeanProvider instance, which source will be presented in the next few paragraphs.

###### BaseBeanProvider

So, how is the *BaseBeanProvider* implemented? It is easy to embrace.
The source code in the repository is very similar, but (**Spoiler alert!**) changed to handle `@Transactional` in [Part 4](#Part-4---Transactions).

```java
class BaseBeanProvider implements BeanProvider {
    private final List<? extends BeanDefinition<?>> definitions;

    public BaseBeanProvider(List<? extends BeanDefinition<?>> definitions) {
        this.definitions = definitions;
    }

    @Override
    public <T> List<T> provideAll(Class<T> beanType) { // 1
        return definitions.stream().filter(def -> beanType.isAssignableFrom(def.type()))
                .map(def -> beanType.cast(def.create(this)))
                .toList();
    }
    
    @Override
    public <T> T provide(Class<T> beanType) { // 2
        var beans = provideAll(beanType);     // 2
        if (beans.isEmpty()) { // 3
            throw new IllegalStateException("No bean of given type: '%s'".formatted(beanType.getCanonicalName()));
        } else if (beans.size() > 1) { // 4
            throw new IllegalStateException("More than one bean of given type: '%s'".formatted(beanType.getCanonicalName()));
        } else {
            return beans.get(0); // 5
        }
    }
}
```

1. `provideAll(Class<T> beanType)` takes all of the *BeanDefinition* and finds all `type()` methods, which returns `Class<?>` that is 
   subtype or exactly provided `beanType`. Thanks to that, it can collect all matching beans.
2. `provide(Class<T> beanType)` is also simple. It reuses the `provideAll` method and then takes all matching beans.
3. The piece of code makes check if there is any bean matching the `beanType` and throws an exception if not.
4. The piece of code makes check if there is more than one bean matching the `beanType` and throw an exception if yes.
5. If there is just one matching bean, it is returned.

**That's it!**

We got all the parts. Now we should check if it works.

###### Did we miss something?

Shouldn't we have started with tests of the annotation processor? How can the annotation processor be tested?

##### Annotation processor testing

The annotation processor is rather poorly prepared for being tested.
One way to test it is to create a separate project/Gradle or Maven submodule.
It would then use the annotation processor, and compilation failure would mean something is wrong.
It doesn't sound good, right?

The other option is to utilise the [compile-testing](https://github.com/google/compile-testing) library created by
Google. It simplifies the testing process, even though the tool isn't perfect. Please find the
tutorial on how to use it [here](https://chermehdi.com/posts/compiler-testing-tutorial/).

I introduced both approaches in the text's repository. The *compile-testing* was used for "unit
tests", and *integrationTest* module was used for "integration tests".

You can find the test implementation and configuration in the *framework* subproject's files below:

1. [build.gradle](/framework/build.gradle)
2. [test dir](/framework/src/test/java)
3. [integrationTest dir](/framework/src/integrationTest/java)

##### Step 7 - working framework

In the beginning, there was [*NoFrameworkApp*](/testapp/src/main/java/io/jd/testapp/NoFrameworkApp.java):

```java
public class NoFrameworkApp {
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
If the main is run, we got the three lines printed:

```shell
Begin transaction
Participant: 'Participant[]' takes part in event: 'Event[]'
Commit transaction
```

It looks like this with [*FrameworkApp*](/testapp/src/main/java/io/jd/testapp/FrameworkApp.java):

```java
public class FrameworkApp {
    public static void main(String[] args) {
        BeanProvider provider = BeanProviderFactory.getInstance();
        ParticipationService participationService = provider.provide(ParticipationService.class);
        participationService.participate(new ParticipantId(), new EventId());
    }
}
```

However, to make it work, we have to add *@Singleton* here and there. 
Please refer to the source code in the [directory](/testapp/src/main/java/io/jd/testapp).
If we run that main, we will get the same result:

```shell
Begin transaction
Participant: 'Participant[]' takes part in event: 'Event[]'
Commit transaction
```

Therefore, we can call it a **success**. 
The framework **works like a charm!**

##### What's next?

Once you checked the result of running the code from the previous paragraph, you saw that there were additional messages. 
They are about the beginning and committing a transaction.

Handling the transactions is also typical for frameworks. I will cover how to handle transactions in the next part.

## Part 4 - Transactions

The three main Java app frameworks provide support for declarative transactions through annotations.

* [Spring Framework](https://docs.spring.io/spring-framework/docs/4.2.x/spring-framework-reference/html/transaction.html#tx-decl-explained)
* [Micronaut](https://micronaut-projects.github.io/micronaut-data/snapshot/guide/#transactions)
* [Quarkus](https://quarkus.io/guides/transaction)

So why wouldn't we like to have something like that in our framework?
The repository **[Java Own Framework - step by step](https://github.com/Patresss/Java-Own-Framework---step-by-step)** shows how to do it purely in runtime. I want to show you the compile-time version today. 

[Micronaut](https://micronaut.io) has heavily inspired all code examples in this text.

### What exactly is declarative transaction support?

At the end of the previous part, we have seen logs for the running app:

```text
Begin transaction
Participant: 'Participant[]' takes part in event: 'Event[]'
Commit transaction
```

As you can guess, the transaction is already there.
It is managed by *[TransactionManager](https://jakarta.ee/specifications/transactions/2.0/apidocs/jakarta/transaction/transactionmanager)* instance in [ManualTransactionParticipationService](testapp/src/main/java/io/jd/testapp/ManualTransactionParticipationService.java).
The `Begin transaction` and `Commit transaction` messages are printed by a fake _TransactionManager_ implementation called *[TransactionalManagerStub](testapp/src/main/java/io/jd/testapp/TransactionalManagerStub.java)*.

If we take a look at the *ManualTransactionParticipationService* code:

```java
@Singleton
public class ManualTransactionParticipationService implements ParticipationService {
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    private final TransactionManager transactionManager;

   // constructor

   @Override
    public void participate(ParticipantId participantId, EventId eventId) {
        try {
            transactionManager.begin();
            var participant = participantRepository.getParticipant(participantId);
            var event = eventRepository.findEvent(eventId);
            eventRepository.store(event.addParticipant(participant));

            System.out.printf("Participant: '%s' takes part in event: '%s'%n", participant, event);

            transactionManager.commit();
        } catch (Exception e) {
            rollback();
            throw new RuntimeException(e);
        }
    }

    private void rollback() {
        try {
            transactionManager.rollback();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }
}
```

We see that the transaction adds lots of boilerplate. 
Wouldn't it be easier to write code like this:

```java
@Singleton
public class DeclarativeTransactionsParticipationService implements ParticipationService {
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;
    
    // constructor

    @Override
    @Transactional
    public void participate(ParticipantId participantId, EventId eventId) {
            var participant = participantRepository.getParticipant(participantId);
            var event = eventRepository.findEvent(eventId);
            eventRepository.store(event.addParticipant(participant));
            
            System.out.printf("Participant: '%s' takes part in event: '%s'%n", participant, event);
    }
}
```

From one on, being able to write code like that is our target.
We want to handle transactions by adding the *@Transactional* annotation to the method of our interest.

### The *@Transactional* annotation and *TransactionManager*

First, it would be beneficial to have an annotation to achieve the outcome. As I wanted to use the standard one instead of writing my own, I chose the
*[@Transactional](https://javadoc.io/static/jakarta.transaction/jakarta.transaction-api/2.0.1/jakarta/transaction/Transactional.html)* annotation and 
the *[TransactionManager](https://javadoc.io/static/jakarta.transaction/jakarta.transaction-api/2.0.1/jakarta/transaction/TransactionManager.html)* interface 
from the [Jakarta EE Transactions 2.0 specification](https://jakarta.ee/specifications/transactions/2.0/).

### How is the transactional handling going to work

Once a method is annotated with *@Transactional*, we want the annotation processor to generate transaction handling code.
For the sake of simplicity, the processor will generate code only for methods of concrete classes.

Since the processor can only generate new code, it will create a subclass of the class with annotated methods. 
Therefore, the class cannot be final. The methods cannot be final, private or static. 
Non-annotated methods won’t be touched at all.

To get a better idea, please look at the example below.

For the below class:

```java
@Singleton
public class RepositoryA {

    @Transactional
    void voidMethod() {
    }

    int intMethod() {
        return 1;
    }
}
```

The annotation processor should generate the following:

```java
@Singleton
class RepositoryA$Intercepted extends RepositoryA {
  private final TransactionManager transactionManager;

  RepositoryA$Intercepted(TransactionManager transactionManager) {
    super();
    this.transactionManager = transactionManager;
  }

  @Override
  void voidMethod() {
    // transaction handling code
  }
}
```

The example presents a simplified version of what will be generated, but you probably get the idea.
The actual code generation and other issues will be shown later on.
The generated code will be simple. 
It won't care about transaction propagation.
It will wrap checked exceptions into the *RuntimeException* and rethrow them in that form.

The problem is that if you want transactions, you cannot directly create an instance of the class with annotated methods using *new* or any other factory method. 
You must rely on the framework created in the first part to provide it, as only the generated class will have the expected transactional code.

The only extra thing worth noticing in the example is the generated class name. 
For the rest of this project, if the annotation processor ever creates replacements for some classes, their names will include the *Intercepted* word.

### Handling @Transactional

As transaction handling is the main subject of this text, we will get straight to it.

Processing the *@Transactional* annotation is not a mandatory part of our framework.
It should be used based on the user's decision.
Therefore, the code responsible for it will be called *TransactionalPlugin*, as this is a pluggable feature.

Let's look at the code below
(the code also is available [here](/framework/src/main/java/io/jd/framework/transactional/TransactionalPlugin.java)).

```java
public class TransactionalPlugin implements ProcessorPlugin { // 7
   private TransactionalMessenger transactionalMessenger; // 3

   @Override
    public Collection<JavaFile> process(Set<? extends Element> annotated) { // 1
        Set<ExecutableElement> transactionalMethods = ElementFilter.methodsIn(annotated); // 2
        validateMethods(transactionalMethods); // 3
        Map<TypeElement, List<ExecutableElement>> typeToTransactionalMethods = transactionalMethods.stream() // 4
                .collect(groupingBy(element -> (TypeElement) element.getEnclosingElement())); // 4
        return typeToTransactionalMethods.entrySet()
                .stream()
                .map(this::writeTransactional) // 5
                .toList();
    }

    private void validateMethods(Set<ExecutableElement> transactionalMethods) { // 3
        raiseForPrivate(transactionalMethods);
        raiseForStatic(transactionalMethods);
        raiseForFinalMethods(transactionalMethods);
        raiseForFinalClass(transactionalMethods);
    }

    private JavaFile writeTransactional(Map.Entry<TypeElement, List<ExecutableElement>> typeElementListEntry) { // 5
        var transactionalType = typeElementListEntry.getKey();
        var transactionalMethods = typeElementListEntry.getValue();
        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(transactionalType);
        return new TransactionalInterceptedWriter(transactionalType, transactionalMethods, packageElement) // 6
                .createDefinition(processingEnv.getMessager()); // 6
    }
    
   // more methods ...
}
```

Now, it is time for us to dive deeply into the provided source.

1. *Set<? extends Element> annotated* contains all [*Element*s](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/Element.html) annotated with *@Transactional*.
2. In the first step, we filter all methods out of the annotated set of elements using [ElementFilter](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/util/ElementFilter.html).
3. Then, the annotated elements are validated against the previously mentioned rules.
   I introduced the utility class *TransactionalMessenger* [(code here)](/framework/src/main/java/io/jd/framework/transactional/TransactionalMessenger.java).
   Its sole responsibility is to wrap [*Messager*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/annotation/processing/Messager.html) and
   provide a unified API for raising errors associated with the *@Transactional* processing.
   Every *raiseForSth* method calls *TransactionalMessenger* providing information about the error.
   The *raiseForSth* methods' code is skipped to keep the example concise and manageable.
4. Now, we group the annotated methods by classes that the methods are declared.
   In *Java*, you can only create a method in a class or interface.
   However, the plugin accepts only concrete class methods and raises errors for others.
   Therefore, we can be sure that calling *element.getEnclosingElement(_)* where an element is the 
   annotated method will return class representation - *[TypeElement](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/TypeElement.html)*.
5. Once we have the mentioned grouping, we can write the code. We need to intercept classes that are 
   keys in the grouping and write transactional versions of methods that are values of the mapping.
6. The last part is to write the code. 
   The logic is stored in *TransactionalInterceptedWriter*, so we can move to see its code.
7. As the *TransactionalPlugin* must be somehow plugged into our framework workings, the class implements
   the *ProcessorPlugin* interface. 
   How it all works will be described after we finish with the transaction handling, as it is not the main topic here.

### Writing the code with *TransactionalInterceptedWriter*

For code generation, I will use the proven [JavaPoet](https://github.com/square/javapoet) library.

The code of the *TransactionalInterceptedWriter* is quite complicated.
The thing that requires special attention is writing transactional versions of *void* methods and value-returning methods.
Unfortunately, *Java* language has the *void* type contrary to *Kotlin*, *Scala*, *Rust* and others.

We will get to the mentioned part later. Now let's start with instance fields and constructor.

#### Instance fields and constants

The *Writer* constructor is fairly simple, so it can be omitted.

```java
class TransactionalInterceptedWriter {
    private static final String TRANSACTION_MANAGER = "transactionManager";
    private static final Modifier[] PRIVATE_FINAL_MODIFIERS = {Modifier.PRIVATE, Modifier.FINAL};
    
    private final TypeElement transactionalElement; // 1 
    private final List<ExecutableElement> transactionalMethods; // 2
    private final PackageElement packageElement; // 3
}
```

The constants are fairly simple, and their names are self-explanatory.

The class instance fields are more interesting.

1. The *transactionalElement* stores the *TypeElement* representation of the class with the annotated methods. 
   The class will be referred to as intercepted class or superclass.
2. The *transactionalMethods* stores the [*ExecutableElement*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/ExecutableElement.html) representation of the annotated methods of the *transactionalElement* class.
3. The *packageElement* stores the [*PackageElement*](https://docs.oracle.com/en/java/javase/17/docs/api/java.compiler/javax/lang/model/element/PackageElement.html) representation of the package in which *transactionalElement* is defined.

#### Intercepting class definition

We will start with the most high-level thing. Let's see how the intercepting class is written, but without going into details.

```java
class TransactionalInterceptedWriter {
    
    public JavaFile createDefinition(Messager messager) {
        TypeSpec typeSpec = TypeSpec.classBuilder("%s$Intercepted".formatted(transactionalElement.getSimpleName().toString())) // 1
                .addAnnotation(Singleton.class) // 2
                .superclass(transactionalElement.asType()) // 3
                .addSuperinterface(TypeName.get(Intercepted.class)) // 4
                .addMethod(interceptedTypeMethod()) // 4
                .addField(TransactionManager.class, TRANSACTION_MANAGER, PRIVATE_FINAL_MODIFIERS) // 5
                .addMethod(constructor(messager)) // 6
                .addMethods(transactionalMethodDefinitions()) // 7
                .build();
        return JavaFile.builder(packageElement.getQualifiedName().toString(), typeSpec).build(); // 8
    }
    
}
```

1. First of all, the class must have a name. 
   As mentioned before the generated class will be called the old one but with an extra *$Intercepted* part. 
   For example, *Repository* will be changed into *Repository$Intercepted*. 
   Therefore, we know that the type before *$* is intercepted by the generated class.
2. The created class must be annotated with *@Singleton*, so the DI solution from the first part will pick it up.
3. To fulfil its role, the generated class will extend the class with methods annotated with *@Transactional*.
   We have already talked about it above.
4. The class will also implement the *Intercepted* interface, which will be covered later.
   The interface is related to the provisioning of the intercepted instances.
   This requires the generated class to implement an extra method.
   I will describe how it works at the end of the text, as this is unrelated to transactions.
5. To handle transactions, the class needs a *TransactionalManager* field. 
   Adding the field is very straightforward.
6. The class must have a constructor that will call *super(requiredDependencies)* and set the *transactionManager* field.
7. The class will override the methods annotated in its superclass.
8. The generated code will be stored in the *JavaFile* object to be written to a real file later.

Now, having the high-level view, we can dive into the details where needed. So let's start with writing the constructor.

#### Constructor

To provide the transactional capability, the constructor must call the constructor of its superclass via the *super* keyword, passing the parameters in the correct order.
The *transactionManager* field of the intercepting class also must be populated.

```java
class TransactionalInterceptedWriter {

    private MethodSpec constructor(Messager messager) {
        Dependency dependency = new TypeDependencyResolver().resolve(transactionalElement, messager); // 1
        var typeNames = dependency.dependencies().stream().map(TypeName::get).toList(); // 1
        
        var constructorParameters = typeNames.stream() // 2
                .map(typeName -> ParameterSpec.builder(typeName, "$" + typeNames.indexOf(typeName)).build()) // 2
                .toList();
        
        var superCallParams = IntStream.range(0, typeNames.size()) // 3
                .mapToObj(integer -> "$" + integer) // 3
                .collect(Collectors.joining(", ")); // 3

        return MethodSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder(TransactionManager.class, TRANSACTION_MANAGER).build()) // 2
                .addParameters(constructorParameters) // 2
                .addCode(CodeBlock.builder()
                        .addStatement("super($L)", superCallParams) // 3
                        .addStatement("this.$L = $L", TRANSACTION_MANAGER, TRANSACTION_MANAGER) // 4
                        .build())
                .build();
    }
}
```

1. The first thing that is done to create a constructor is finding out what the dependencies of the intercepted class are.
   To do it in a convenient way, we will reuse [*TypeDependencyResolver*](/framework/src/main/java/io/jd/framework/processor/TypeDependencyResolver.java), created for the DI solution.
   You can read more about it [here](#step-4---write-the-actual-processing).
2. Having the dependencies of the superclass, we can create parameters for the constructor.
   The *transactionManager* is the first parameter, and the rest is provided conveniently as *Type ${position in the constructorParameters list}*.
3. Having the intercepting class constructor params, we can prepare the content of the *super* call. 
   Then it can be added to the *super* call in the constructor.
4. The last thing to do is to also set up the *transactionManager* field.

The generated constructor may look like the code below:

```java
class TestRepository$Intercepted {
   TestRepository$Intercepted(TransactionManager transactionManager,
                              ParticipantRepository $0, 
                              EventRepository $1) {
      super($0, $1);
      this.transactionManager = transactionManager;
   }
}
```

#### Overriding transactional methods

In the case of generating the methods, we will start with an example.

```java
@Singleton
class RepositoryA$Intercepted extends RepositoryA { 
    
  @Override
  void voidMethod() {
    try {
      transactionManager.begin();
      super.voidMethod();
      transactionManager.commit();
    }
    catch (Exception e) {
      try {
        transactionManager.rollback();
      }
      catch (Exception innerException) {
        throw new RuntimeException(innerException);
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  int intMethod() {
    try {
      transactionManager.begin();
      var intMethodReturnValue = (int) super.intMethod();
      transactionManager.commit();
      return intMethodReturnValue;
    }
    catch (Exception e) {
      try {
        transactionManager.rollback();
      }
      catch (Exception innerException) {
        throw new RuntimeException(innerException);
      }
      throw new RuntimeException(e);
    }
  }
}
```

*TransactionManager*'s methods are defined with the checked exception.
Therefore, transactional methods need to include try/catch blocks.
In the try block, the *begin* and *commit* must be called, as well as a *rollback* in the catch clause.
If the return type isn't void, the result of *super* method call results must be stored in a variable.

So high-level method to generate such a call looks like this:

```java
class TransactionalInterceptedWriter {
   private MethodSpec generateTransactionalMethod(ExecutableElement executableElement) {
      var methodName = executableElement.getSimpleName().toString();
      var transactionalMethodCall = transactionalMethodCall(executableElement);
      var methodCode = tryClause(transactionalMethodCall, catchClause());
      return MethodSpec.methodBuilder(methodName)
              .addModifiers(executableElement.getModifiers())
              .addParameters(executableElement.getParameters().stream().map(ParameterSpec::get).toList())
              .addAnnotation(Override.class)
              .addCode(methodCode)
              .returns(TypeName.get(executableElement.getReturnType()))
              .addTypeVariables(getTypeVariableIfNeeded(executableElement).stream().toList())
              .build();
   }
}
```

In this and the previous part of the Readme.md I have shown a lot of code, mostly containing JavaPoet usage. 
My hope is that now you get how the JavaPoet works. 
From now on, I will try to minimise the boilerplate JavaPoet code by omitting it in the examples or sharing it as Gists. 
The full code is still present in the repository, of course.

#### *Catch* and *try* blocks

The try and catch blocks code is quite simple. So as mentioned before, here are the gists:

[https://gist.github.com/JacekDubikowski/167bcaaab151f9d4ad6f033ef1543cec](https://gist.github.com/JacekDubikowski/167bcaaab151f9d4ad6f033ef1543cec)

[https://gist.github.com/JacekDubikowski/8b691faf3e0aba2e04d03211823794ff](https://gist.github.com/JacekDubikowski/8b691faf3e0aba2e04d03211823794ff)

#### Super method calls

Once we have *try* and *catch* blocks handled, we can focus on the actual super method call.

```java
class TransactionalInterceptedWriter {
   private CodeBlock transactionalMethodCall(ExecutableElement executableElement) {
      return executableElement.getReturnType().getKind() == TypeKind.VOID // 1
              ? transactionalVoidCall(executableElement)
              : returningTransactionalMethodCall(executableElement);
   }

   private CodeBlock transactionalVoidCall(ExecutableElement method) { // 2
      var params = translateMethodToSuperCallParams(method);
      return CodeBlock.builder()
              .addStatement(TRANSACTION_MANAGER + ".begin()")
              .addStatement("super.$L(%s)".formatted(params), method.getSimpleName())
              .addStatement(TRANSACTION_MANAGER + ".commit()")
              .build();
   }

   private CodeBlock returningTransactionalMethodCall(ExecutableElement method) { // 3
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
      return method.getParameters().stream().map(variableElement -> variableElement.getSimpleName().toString())
              .collect(Collectors.joining(", "));
   }
}
```

The code generation is really simple here.

1. The first step is deciding upon the method call based on the return type of the super method.
2. A void call is generated. This is very simple, as there is no need to store the results of the super call.
3. Finally, the value returning method call is generated. The result is stored in a variable to be returned after the commit.

##### The full code 

The full code of the [*TransactionalInterceptedWriter*](/framework/src/main/java/io/jd/framework/transactional/TransactionalInterceptedWriter.java).

### That's all for transactions

This is everything I have prepared for you in transaction handling.

The [code](/testapp-transactional/src/main/java/io/jd/testapp/DeclarativeTransactionsParticipationService.java) 
below could be used, and support for transactions can be provided.

```java
@Singleton
public class DeclarativeTransactionsParticipationService implements ParticipationService {
    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;

    public DeclarativeTransactionsParticipationService(
            ParticipantRepository participantRepository, 
            ventRepository eventRepository
    ) {
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    @Transactional
    public void participate(ParticipantId participantId, EventId eventId) {
            var participant = participantRepository.getParticipant(participantId);
            var event = eventRepository.findEvent(eventId);
            eventRepository.store(event.addParticipant(participant));
            
            System.out.printf("Participant: '%s' takes part in event: '%s'%n", participant, event);
    }
}
```

However, we must be sure to get the expected instance during the runtime, right?
Let us check how to make it all work within our framework.
To reach the goal, we need two more things.

1. The *TransactionPlugin* must be used during compilation.
2. We must make our framework provide only intercepted instances.

### Plugging the transaction handling into the framework

We have seen the code that handles transaction processing. 
Now, we have to make use of the *TransactionalPlugin* in our framework.
To keep everything simple, I created an interface *ProcessorPlugin* which will be a way to register extensions.
Thanks to that, the whole transaction processing code is held in separate classes.

```java
public interface ProcessorPlugin {
    void init(ProcessingEnvironment processingEnv); // 1

    Collection<JavaFile> process(Set<? extends Element> annotated); // 2

    Class<? extends Annotation> reactsTo(); // 3
}
```

The interface has three methods.

1. The *init* method is responsible for the initialisation of the plugin.
2. The *process* method does the actual processing. Therefore, it returns generated Java files.
3. The *reactsTo* method provides information about annotation that the plugin is interested in.

The plugins are hardwired so far and are used as presented in the [code](/framework/src/main/java/io/jd/framework/processor/BeanProcessor.java):

```java
public class BeanProcessor extends AbstractProcessor {
    private List<ProcessorPlugin> plugins = List.of();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) { // 1
        super.init(processingEnv);
        plugins = List.of(new TransactionalPlugin());
        plugins.forEach(processorPlugin -> processorPlugin.init(processingEnv));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) { // 2
        try {
            runPluginsProcessing(roundEnv);
            // rest of the processing 
        } catch (Exception e) {
           // exception handling
        }
        // return
    }

    private void runPluginsProcessing(RoundEnvironment roundEnv) { // 3
        plugins.stream().map(processorPlugin -> processorPlugin.process(roundEnv.getElementsAnnotatedWith(processorPlugin.reactsTo())))
                .flatMap(Collection::stream)
                .forEach(this::writeFile); // 4
    }
    
    private void writeFile(JavaFile javaFile) {} // 4
}
```

As you can see:

1. Once the processor is initialised, it also initialises its plugins. 
   In the more real-world code, the plugin discovery could possibly be run here.
2. The processor starts its processing by running the plugins.
3. All the plugins are run with elements annotated with the annotation that the plugin reacts to.
4. The files generated by plugins are written to the actual files in some */generated* directory.

### Implementation of the *ProcessorPlugin* for the *TransactionalPlugin*

In the main part of the text, I omitted some code of the *TransactionalPlugin* related to the *ProcessorPlugin* implementation.
Now, you can see the missing parts of the code below.

```java
public class TransactionalPlugin implements ProcessorPlugin { 

    @Override
    public void init(ProcessingEnvironment processingEnv) { // 1
        this.processingEnv = processingEnv;
        transactionalMessenger = new TransactionalMessenger(processingEnv.getMessager());
    }

    @Override
    public Class<? extends Annotation> reactsTo() { // 2
        return Transactional.class;
    }
}
```

1. The *init* method implementation is fairly simple. It just sets *processingEnv* and creates *TransactionalMessenger*.
2. The *reactsTo* method implementation states that the plugin is interested in *@Transactional* annotation.
   Who would guess, right?

The provided code is nothing big.
It is easy to notice that the most interesting thing was the *process* method shown before.

### Provisioning of intercepted class

In the "production" code, the framework must be able to provision the intercepted instances.
To make this possible, I introduced the interface below.

```java
public interface Intercepted {
    Class<?> interceptedType();
}
```

This is very simple, yet very important.
Thanks to the interface, we can be sure which type has its intercepted version.
You may have remembered from the main part that our *$Intercepted* classes have implemented the interface.
So how was this done?

#### Implementing the interface

The implementation of the interface is quite simple.
For the *RepositoryA*:

```java
@Singleton
public class RepositoryA {
    // some @Transactional methods
}
```

It will be implemented as:

```java
@Singleton
class RepositoryA$Intercepted extends RepositoryA {

   @Override
   public Class interceptedType() {
      return RepositoryA.class;
   }
   
   // Overridden transactional methods
}
```

In the source code of *TransactionalInterceptedWriter*, it would just add a few extra lines:

```java
class TransactionalInterceptedWriter {
   private MethodSpec interceptedTypeMethod() {
      return MethodSpec.methodBuilder("interceptedType")
              .addAnnotation(Override.class)
              .addModifiers(PUBLIC)
              .addStatement("return $T.class", TypeName.get(transactionalElement.asType()))
              .returns(ClassName.get(Class.class))
              .build();
   }
}
```

Now, we can differentiate *Intercepted* classes from regular ones
and point out types that have their intercepted versions.

#### Using only intercepting classes during provisioning

To get only the intercepted version and not the original one, we need to update the *BaseBeanProvider*.
The simplified code was shown in the previous part about DI. Now, it needs an extra step.

```java
class BaseBeanProvider implements BeanProvider {
   @Override
   public <T> List<T> provideAll(Class<T> beanType) {
      var allBeans = definitions.stream().filter(def -> beanType.isAssignableFrom(def.type()))
              .map(def -> beanType.cast(def.create(this)))
              .toList(); // 1
      var interceptedTypes = allBeans.stream().filter(bean -> Intercepted.class.isAssignableFrom(bean.getClass()))
              .map(bean -> ((Intercepted) bean).interceptedType())
              .toList(); // 2
      return allBeans.stream().filter(not(bean -> interceptedTypes.contains(bean.getClass()))).toList(); // 3
   }
}
```

1. Firstly, we find all the beans matching the needed type.
2. Then, we find the beans that implement the *Intercepted* type.
3. In the end, we return a list of the matching beans filtering out
   the beans that are among the types with their intercepted version.

### It works

Now the whole solution works as expected. 
The framework provides the *$Intercepted* instances that handle transactions for us.
In the next part and final part, we will look at *RestController*s, so stay tuned!

## Part 5 - Controllers

In the previous two parts, we covered the basics of annotation processing and showed how to use it for dependency injection and declarative transaction handling.
In this one, we will continue our journey by building on the knowledge we gained and exploring how to create a REST controller using the annotation processor mechanism.

The three main Java app frameworks provide support for creating controllers through annotations.

* [Spring Framework](https://spring.io/guides/tutorials/rest/)
* [Micronaut](https://docs.micronaut.io/2.0.0.M2/guide/index.html#httpServer)
* [Quarkus](https://quarkus.io/guides/rest-json)

### What are we going to build?

Let me show you what we will achieve in the text.

A REST controller is a web application component that handles HTTP requests and responses using the REST architectural style.

Once you start the demo [application](/testapp-web/src/main/java/io/jd/testapp/FrameworkApp.java) from the event organisation domain, it will print the port you can use for communication.

```text
Port: <port-value>
```

Thanks to that, you can easily interact with the app using, for example, curl:

```shell
-> % curl -v http://localhost:<port-value>/participate -d '{"eventId": "id", "participationId": "partId"}'
...
< HTTP/1.1 200 OK
< Date: Sat, 25 Feb 2023 11:01:18 GMT
< Content-Type: application/json
< Content-Length: 74
< Server: Jetty(11.0.13)
< 
{"accepted":{"eventId":{"value":"id"},"participantId":{"value":"partId"}}}
```

The class responsible for this response is [ParticipationController](/testapp-web/src/main/java/io/jd/testapp/ParticipationController.java):

```java
@Singleton
public class ParticipationController {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ParticipationService participationService;

    public ParticipationController(ParticipationService participationService) {
        this.participationService = participationService;
    }

    @RequestHandle(value = "/participate", method = HttpMethod.POST)
    String participate(Request request) throws IOException {
        var participationDTO = objectMapper.readValue(request.body(), ParticipationDTO.class);
        participationService.participate(participationDTO.participantId(), participationDTO.eventId());
        return objectMapper.writeValueAsString(Map.of("accepted", participationDTO));
    }

}
```

### The solution and its architecture

The *Controller* above wouldn't work without proper annotation processing and some server ([Jetty](https://www.eclipse.org/jetty/)) that handles the traffic.
Hence, I split the text into two parts.
The first would cover the annotation processing part.
The second would explain how it was used alongside Jetty.
First of all, why I picked [Jetty](https://www.eclipse.org/jetty/) for my text?
The answer is not complicated, it is one of the most popular servers out there, and I thought its API would serve my text well.
Nevertheless, any server could have been used.

Additionally, the architecture of the solution can follow the same simple division. 
Please look at the simplified diagram:

![Web "class" diagram](./docs/Web.png)

As you can see, the previously created and used *BeanProcessor* would use the new *WebPlugin*. 
The plugin is responsible for creating implementations of *RequestHandler* based on *RequestHandle* annotations in the code.
Then the server side would pick it up using DI from the previous part and translate it to a working Jetty solution using
*FrameworkHandler* class that extends *Jetty* native *AbstractHandler*.

### Assumptions and verification

In the text, I will present a very simplistic approach to show you the idea that could lead to a fully-fledged framework.
So proper and complex error handling, complicated path matching, resolving path variables, query params, and the request body is out of scope. 
I also do not care here for REST levels and HATEOAS. Nevertheless, I will show places where you can add code to handle that.
The endpoint method can accept at most one parameter represented in the form of *io.jd.framework.webapp.Request*. 
I will discuss it later on.

To see if the code works, I created one [acceptance test](netty-web/src/test/java/io/jd/framework/tests/ServerTest.java) that, once working, would make us satisfied with the result we have achieved.

```java
class ServerTest {
    @Test
    void testSlashInt2() throws IOException, InterruptedException {
        var request = getRequest("/int2");

        var response = client.send(request, ofString());

        assertEquals(200, response.statusCode());
        assertDoesNotThrow(() -> Integer.parseInt(response.body()));
        assertEquals("text/plain;charset=utf-8", getContentTypeValue(response));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 11, 100, 400})
    void testSlashInt3(int value) throws IOException, InterruptedException {
        var request = postRequest(BodyPublishers.ofString(String.valueOf(value)), "/int3");

        var response = client.send(request, ofString());

        assertEquals(response.statusCode(), 200);
        assertEquals(Integer.parseInt(response.body()), value);
        assertEquals(getContentTypeValue(response), APPLICATION_JSON);
    }

    @Test
    void testSlashVoid() throws IOException, InterruptedException {
        var request = postRequest(noBody(), "/void");

        var response = client.send(request, ofString());

        assertEquals(response.statusCode(), 204);
        assertEquals(response.body(), "");
    }

    @Test
    void testSlashReturnErrorResponse() throws IOException, InterruptedException {
        var request = putRequest(noBody(), "/return-error-response");

        var response = client.send(request, ofString());

        assertEquals(500, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    void testSlashResource() throws IOException, InterruptedException {
        var request = getRequest("/resource");

        var response = client.send(request, ofString());

        assertEquals(200, response.statusCode());
        assertEquals("{\"key\":\"value\"}", response.body());
    }

    @Test
    void testSlashError() throws IOException, InterruptedException {
        var request = getRequest("/error");

        var response = client.send(request, ofString());

        assertEquals(500, response.statusCode());
        assertEquals("{\"errorMessage\": \"Expected error message\"}", response.body());
    }
}
```

### Annotation processing

As you surely noticed, a new annotation was introduced. 
The [*@RequestHandle*](framework/src/main/java/io/jd/framework/webapp/RequestHandle.java) is very simple and responsible for declaring the HTTP endpoint.

```java
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface RequestHandle {
   HttpMethod method();
   String value() default "/";
   String produce() default MediaType.APPLICATION_JSON;
}
```

It is translated during annotation processing to the implementation of the [RequestHandler](framework/src/main/java/io/jd/framework/webapp/RequestHandler.java) interface.

```java
public interface RequestHandler {
    HttpMethod method();
    String produce(); // What content type is produced
    String path(); // What path should be matched
    Object process(Request request) throws Exception; // Actual request processing
}
```

So for any declared endpoint, there will be one corresponding *RequestHandler* generated automatically.

The autogenerated implementation for the endpoint which is declared below:

```java
@Singleton
public class ExampleController {
    @RequestHandle(value = "/int3", method = HttpMethod.POST)
    public int getIntFromString(Request request) {
        return Integer.parseInt(request.body());
    }
}
```

would look like this:

```java
@Singleton // 1
final class ExampleController$getIntFromString$1$handler implements RequestHandler { // 2
  private final ExampleController controller; // 3

  @Override
  public String produce() { 
    return "application/json";
  }

  @Override
  public String path() {
    return "/int3";
  }

  @Override
  public HttpMethod method() {
    return HttpMethod.POST;
  }

  @Override
  public Object process(Request arg0) throws Exception { // 4
    return controller.getIntFromString(arg0);
  }
}
```

1. It is annotated with *@Singleton* so that our DI solution can inject it.
2. The name consists of 4 parts, once split on the '$' sign:
   1. ExampleController - the class name the endpoint was declared in.
   2. getIntFromString - the method's name annotated with *@RequestHandle*
   3. 1 - ordinal number, provided for cases when two methods have the same name but different parameter lists.
   4. handler - an indicator that it is just a handler.
3. The controller field is used to do the actual processing.
4. The *process* method that uses the controller to process the request returns *Object* so the solution can be flexible.

Now that we have covered the outcome of the annotation processing, 
it's time to see how the annotation processor does this.
This process is crucial to understanding how our framework works. 
So, let's delve into the magic.

#### Annotation processor work

First, lets us see the [*WebPlugin*](framework/src/main/java/io/jd/framework/webapp/WebPlugin.java). 
It extends [ProcessorPlugin](framework/src/main/java/io/jd/framework/processor/ProcessorPlugin.java) introduced before to split the processor’s responsibility among plugins.

The code is reasonably simple (error logging is skipped):

```java
public class WebPlugin implements ProcessorPlugin {
   private HandlerWriter handlerWriter; // That would be covered later on
   private ProcessingEnvironment processingEnv; // For utils usage

   @Override
   public Class<? extends Annotation> reactsTo() { // The plugin is interested in RequestHandle annotation
      return RequestHandle.class;
   }

   @Override
   public Collection<JavaFile> process(Set<? extends Element> annotated) { // 1
      Map<NameData, List<ExecutableElement>> nameToMethodMaps = ElementFilter.methodsIn(annotated)
              .stream()
              .collect(groupingBy(NameData::new));

      var resultFiles = new ArrayList<JavaFile>();
      for (var entry : nameToMethodMaps.entrySet()) {
         var generatedFiles = handle(entry.getKey(), entry.getValue());
         resultFiles.addAll(generatedFiles);
      }
      return resultFiles;
   }

   private List<JavaFile> handle(NameData nameData, List<ExecutableElement> handlers) { // 3
      var resultFiles = new ArrayList<JavaFile>();
      for (int i = 0; i < handlers.size(); i++) {
         JavaFile generatedFile = handle(nameData, new IndexedValue<>(i+1, handlers.get(i)));
         resultFiles.add(generatedFile);
      }
      return resultFiles;
   }

   private JavaFile handle(NameData nameData, IndexedValue<ExecutableElement> indexedValue) { // 4
      var handlerMethod = indexedValue.value();
      var typeSpec = createType(nameData, indexedValue, handlerMethod);
      var packageName = ProcessingEnvUtils.getPackageName(processingEnv, handlerMethod);
      return JavaFile.builder(packageName, typeSpec).build();
   }

   private TypeSpec createType(NameData nameData, IndexedValue<ExecutableElement> indexedValue,
                               ExecutableElement handlerMethod) { // 5
      return handlerWriter.buildHandler(
              nameData.toHandlerMethodName(indexedValue.index()),
              handlerMethod,
              TypeName.get(handlerMethod.getEnclosingElement().asType()),
              handlerMethod.getAnnotation(RequestHandle.class)
      );
   }

   private record NameData(Name controllerName, Name handleName) { // 2
      NameData(ExecutableElement element) {
         this(element.getEnclosingElement().getSimpleName(), element.getSimpleName());
      }

      String toHandlerMethodName(int index) {
         return "%s$%s$%s$handler".formatted(controllerName.toString(), handleName.toString(), index);
      }
   }
}
```

1. The *process* method founds methods annotated with *RequestHandle*.
   Then group the methods by the class they are declared in and the method name.
   The methods grouped that way become the building block for creating the handlers.
2. NameData record is just a container for the name of the class the method was declared in and the method name.
   It also provides a method to create a handler implementation name once provided with an index value.
3. In the first step, we just enumerate the methods that share a common class and method name.
4. In the *handle* method, the type is created using *HandlerWriter* and creates the *JavaFile*, result merging it with the package data.
5. The *createType* method is just called to the *HandlerWriter* with a class name to be created, 
   the element representing the annotated method, the controller's name and the *RequestHandle* annotation instance of the method.

As you can see, no code generation is done in the plugin itself. 
The whole [JavaPoet](https://github.com/square/javapoet) soup is in the [*HandlerWriter*](framework/src/main/java/io/jd/framework/webapp/HandlerWriter.java).
So let's see it. However, it will try to minimise the JavaPoet boilerplate to the minimum. 
If you read this, I think you get the *JavaPoet*'s idea.

```java
class HandlerWriter {
    // the Element fields representing *RequestHandler#method*, *RequestHandler#prodice*, *RequestHandler#process*, *RequestHandler#path*
    private final ExecutableElement httpMethodElement; 
    private final ExecutableElement producesElement;
    private final ExecutableElement processElement;
    private final ExecutableElement pathElement;

    private final Types typeUtils; // taken out of *ProcessingEnv#getTypeUtils*
    private final TypeMirror requestType; // 1

    TypeSpec buildHandler(String handlerMethodName, ExecutableElement handler, TypeName typeName, RequestHandle annotation) {
        // The created handler has just one field with the Controller that declared the endpoint.
        // Is annotated with @Singleton and extends RequestHandler. Does not declare any other than the overridden method.
        return TypeSpec.classBuilder(handlerMethodName)
                ...
                .build();
    }

    // constructor, produce, path and method overriding code is omitted. Refer to the source code if you want to see this.

   private MethodSpec process(ExecutableElement handlerMethod) { // 2
      var controllerCall = controllerCall(handlerMethod, handlerMethod.getParameters());
      TypeKind handlerMethodType = handlerMethod.getReturnType().getKind();

      return handlerMethodType == TypeKind.VOID
              ? voidMethod(controllerCall)
              : valueReturningMethod(controllerCall);
   }

    private MethodSpec valueReturningMethod(CodeBlock controllerCall) { // 3
        return MethodSpec.overriding(processElement)
                .addStatement("return $L", controllerCall)
                .build();
    }

    private MethodSpec voidMethod(CodeBlock controllerCall) { // 4
        return MethodSpec.overriding(processElement)
                .addStatement(controllerCall)
                .addStatement("return $T.noContent()", Response.class)
                .build();
    }

    private CodeBlock controllerCall(ExecutableElement handlerMethod, List<? extends VariableElement> parameters) { // 5
        // validation that there is at most one param of Request type was omitted
        var methodCallParams = parameters.stream().map(VariableElement::getSimpleName).map(Name::toString)
                .findFirst()
                .map(__ -> "(arg0)")
                .orElse("()");
        return CodeBlock.builder()
                .add("controller.$L$L", handlerMethod.getSimpleName().toString(), methodCallParams)
                .build();
    }
}
```

All the fields are populated in the constructor that I omitted. 
The constructor of the handler being written accepts just the Controller that contains the method to be called in the process method.


1. The *requestType* field represents the compile-time type of the [*Request*](framework/src/main/java/io/jd/framework/webapp/Request.java).
   The endpoint must be able to accept some parameters and get the data out of it, hence the class.
   In our simplified context, the interface is simplistic.
   
   ```java
   public interface Request {
       String body();
   }
   ```
   
   In the real world, this interface should have multiple methods to access headers, body and other stuff like that.
2. In the first part of the *process* method, the controller code is prepared, 
   and then we just check if the controller is supposed to return something or not.
3. The value-returning case is straightforward. The process method just returns the result of the call to the controller.
4. The void method is a little different. We just need to return something to be used for the HTTP response, but there is nothing.
   What was I supposed to do? I introduced the [*Response*](framework/src/main/java/io/jd/framework/webapp/Response.java) interface to solve the problem.
   The interface is very simple and provides one utility method to create a response with no body and 204 No Content status.
   
   ```java
   public interface Response {
       int statusCode();
   
       String body();
   
   static Response noContent() {
      // instance that just returns body as null and 204 status code
   }
   ```
   
   But why return the *Object* from the method as there is a proper type for it?
   The solution is very elastic,
5. and the return type can be interpreted later in the code.
   Thanks to that, we can provide reasonable default behaviour when a return type is just a regular object.
   You will see the matching code shortly, don't worry.
5. The *controllerCall* is a very simple method in our case.
   We start by validating if there is at most one parameter, which is of the expected type.
   This is the place where you would be able to provide extensive support for request bodies, path variables, and query params

This is it for the annotation processing side. Let us dive deep into the server side now.

### Server side

As mentioned, all you need to do is provide a matching glue code for Jetty, which would work.

Let's start with translating our handlers to Jetty handlers. 
So please take a look at [*FrameworkHandler*](netty-web/src/main/java/io/jd/framework/tests/FrameworkHandler.java).

```java
class FrameworkHandler extends AbstractHandler {
    FrameworkHandler(RequestHandler handler) {
        this.handler = handler;
    }

   @Override
   public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
      if (methodAndPathMatches(baseRequest)) { // 1
         response.setCharacterEncoding("utf-8"); // 2
         response.setContentType(handler.produce()); // 2
         handle(response, request); // 3
         baseRequest.setHandled(true); // 
      }
   }

   private boolean methodAndPathMatches(Request baseRequest) { // 1
      return Objects.equals(baseRequest.getHttpURI().getPath(), handler.path())
              && Objects.equals(baseRequest.getMethod(), handler.method().toString());
   }

   private void handle(HttpServletResponse response, HttpServletRequest request) throws IOException { // 3
      var frameworkRequest = SimpleRequest.of(HttpMethod.valueOf(request.getMethod()), readWholeBody(request));
      try {
         var result = handler.process(frameworkRequest);
         handleWorkResult(response, result);
      } catch (Exception e) {
         response.getWriter().print("{\"errorMessage\": \"%s\"}".formatted(e.getMessage()));
         response.setStatus(500);
      }
   }

   private static void handleWorkResult(HttpServletResponse response, Object result) throws IOException { // 3
      if (result instanceof Response r) {
         processResponse(response, r);
      } else {
         process(response, result);
      }
   }

   private static void processResponse(HttpServletResponse response, Response r) throws IOException { // 3.1
      response.setStatus(r.statusCode());
      if (r.body() != null) {
         response.getWriter().print(r.body());
      }
   }

   private static void process(HttpServletResponse response, Object result) throws IOException { // 3.2
      response.setStatus(200);
      response.getWriter().print(result.toString());
   }
}
```

This is also a very simple solution. 
It is not even close to a production solution, but it serves its purpose. 
This is the part in the code where error handling could be provided as well as serialisation and deserialisation if needed. 
As mentioned before, this is the place where the code will differentiate between return types. 
The *Response* type would be treated differently than any other type of object.


1. The first important thing is to match the path. 
   Our framework doesn't support the path variables or path patterns, so the matching is very simple.
   This is the place where you could implement more complex matching, to be able to provide a production-ready solution.
2. Some basic attributes are passed to the response like encoding and content type (from the handler).
3. We just create a simple instance of the *Request* that would be used to call the handler and call the *RequestHandler#process* method.
   Then we just interpret the response.
   1. If the result of the handler call is *Response* instance, then we just translate its status code and body to the response.
   2. If the result is not *Response* instance, then we return status 200 OK and just write the value to the response the way it is.
In this example, the caller should worry if the object would be written as proper JSON for example, but in the real world, it is of course the job of the framework.  
   In case of any error 500 status code with a JSON error body is returned to the caller.

Having all that in mind, we can just look at the [*ServerContainer*](netty-web/src/main/java/io/jd/framework/tests/ServerContainer.java).

```java
@Singleton
public class ServerContainer {
    private final HandlerCollection handlers = new HandlerCollection();
    private volatile Server server = null;

    ServerContainer(Collection<RequestHandler> handlers) { // 1
        handlers.stream()
                .map(FrameworkHandler::new)
                .forEach(this.handlers::addHandler);
    }

    public synchronized void start() throws Exception { // 2
        if (server == null) {
           server = new Server();
           var connector = new ServerConnector(server);
           server.addConnector(connector);
           server.setHandler(this.handlers);
        }
        server.start();
    }

    public int port() { // 3
        return server.getURI().getPort();
    }

    public void stop() throws Exception { // 4
        server.stop();
    }
}
```

The server container is just a kind of utility. 

1. In the constructor, the *RequestHandler*s are mapped to *FrameworkHandler*s.
2. Once a server is started using, who would guess, the *start* method, handlers are passed to it for traffic handling.
3. After the start, the port method returns the port that the server is listening to.
4. The *stop* method also serves as name states.

From now on, all tests pass, and the example application works.
If you want to write the code to use it, just ask *BeanProvider* for an instance of the *ServerContainer* and run the server as in the example [here](testapp-web/src/main/java/io/jd/testapp/FrameworkApp.java).

### Epilogue

Congratulations! You made it to the end of our journey together (not just the web part). 
It’s been a pleasure having you join me on this coding adventure.

In the readme of this repository, we explored the process of creating an educational Java framework that relies on annotation processing. 
It all started by creating a dependency injection framework. 
Subsequently, we incorporated the @Transactional annotation to support transaction management. 
In the final stage, we enriched our framework with a web component by creating HTTP endpoints using annotations. 
I sincerely hope you appreciate the framework as much as I do now. 
While I understand it will never be put to practical use, it's simple, easy to explain, and a great way to learn something new.

We learned much about the annotation processing API and its potential throughout this journey. 
We discovered that the annotation processor is a flexible tool, but it does have its limits. 
However, once you get used to the API, changes can be introduced rapidly and efficiently.

Remarkably, metaprogramming capabilities were integrated without the need to learn a separate meta-language, and the code was written in full Java using IDE support. 
I noticed that the complexity of the API is on par with the Reflection API, which is widely spread yet working in runtime (so you cannot see the effects of your metaprogramming directly).

Evidently, there is also a gain in the speed of the code as reflection tends to be slow and annotation processing is fast enough to go unnoticed during project compilation. 
Additionally, debugging tends to be simpler as the source code exists and can be checked, contrary to, e.g. proxies created in runtime.

Furthermore, I expect the usage of the annotation processor to grow in the coming years as it has already been adopted by Micronaut and Quarkus frameworks. 
The annotation processor approach seems to click with GraalVM and native Java applications, giving the processor chance to shine in the cloud environment.

I hope I have convinced you that better, more efficient, and more robust frameworks can be created using the approach.
I would love to hear that the text inspired you to take the next step by attempting to create your own annotation processor someday. Remember, the possibilities are limitless, and the coding landscape is constantly changing, and Java is as well. As JDK continues to evolve, new features could enhance the usefulness of the annotation processor even further.

## Afterwords

### My inspiration

The repository is based on another existing, awesome, and fabulous
repository [Java Own Framework - step by step](https://github.com/Patresss/Java-Own-Framework---step-by-step).

Kudos to [Patresss](https://github.com/Patresss)!

The solution presented was created based on [Micronaut](https://micronaut.io). My work with the framework made me
interested in annotation processing.

Kudos to the Micronaut team for their excellent work!

### The repository

The repository is meant to be worked on in the future using an iterative approach. It is not done yet and I hope that I
will find the time to develop it further.

The code is neither the best possible nor handling all corner cases. It was never the point to create a fully-fledged
framework.

Nevertheless, please create the issue if you have advice for me, have found a bug, or would like to see some changes. I might pick it up one day and I will be very grateful.

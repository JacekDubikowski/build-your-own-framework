# Build your own framework

##### Inspiration

The repository is based on the other existing, awesome and fabulous
repository [Java Own Framework - step by step](https://github.com/Patresss/Java-Own-Framework---step-by-step).

Kudos to [Patresss](https://github.com/Patresss)!

## Part 1

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

### Dependency injection Principle

[Wikipedia](https://en.wikipedia.org/wiki/Dependency_injection) describes *Dependency Injection* as
> a design pattern in which an object or function receives other objects or functions that it depends on.

But how is it done? The pattern separates object creation from its usage. The required objects are provided ("injected")
during runtime and the pattern implementation handles the creation of dependencies.

### Available Dependency Injection solutions

There at least a few DI framework that widely adopted in Java world.

* [Spring](https://spring.io) - dependency injection was initial part of the spring project and still is the core
  concept for framework,
* [Guice](https://github.com/google/guice) - Google's framework,
* [Dagger](https://dagger.dev/dev-guide/) - popular in the Android world,
* [Micronaut](https://micronaut.io) - as part of the framework,
* [Quarkus](https://quarkus.io/guides/cdi-reference) - part of the framework.

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

### Processing of annotations

The most popular Java framework, which is Spring, processes annotations in runtime. 
All of the dependencies are resolved in runtime and the solution is heavily based on reflection mechanism. 
This is one of the possible way to handle anntations and if you would like to follow that lead please refer to mentioned before
 [Java Own Framework - step by step](https://github.com/Patresss/Java-Own-Framework---step-by-step). 

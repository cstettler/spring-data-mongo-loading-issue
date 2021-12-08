# Spring Data (Mongo) Loading Issue

This repository contains a minimal sample application that can be used to reproduce an issue when loading entities via a Spring Data (Mongo**) repository in a parallel manner.

** Note: the issue seems not to be related to Mongo specifically, but to Spring Data in general, but has only been seen and analyzed in the context of Mongo.

## Issue

Loading several entities with properties containing a type discriminator in a parallel way (i.e. from a rest controller in a parallel stream) sometimes results in the following error:

```
org.springframework.beans.BeanInstantiationException: Failed to instantiate [com.innoq.cstettler.springdatamongoloadingissue.Nested]: Specified class is an interface
```

The following aspects seem to be relevant in order to run into this issue:

- the entity must contain a field of an interface type that contains several concrete implementations as values (see `com.innoq.cstettler.springdatamongoloadingissue.Entity#nested`) 
- the entities have to be loaded from a separate thread other than the http handler thread (e.g. from a worker thread of the common pool, see `com.innoq.cstettler.springdatamongoloadingissue.EntityController#getEntities`)
- the application has to be run from the Spring Boot jar (e.g. from command line or a Docker image), not from the IDE

## Demo

Perform the following steps to reproduce the issue in the minimal sample application:

- build the project using Maven (`./mvnw build verify`)
- start the application from the command line (`java -jar target/spring-data-mongo-loading-issue-0.0.1-SNAPSHOT.jar`)
- invoke the entity endpoint (`curl -X GET http://localhost:8080/entities`)

The console will show the said exception.

## Possible Explanation

Whenever an entity with a field of an interface type is loaded, Spring Data (Mongo) needs to decide on the concrete implementation to be instantiated based on the data in the document.
One way to control this decision is using the type discriminator (e.g. via `@BsonDiscriminator`).
When defined, Spring Data (Mongo) persists an additional property (by default called `_class`) as part of the document containing the fully qualified class name of the concrete type of the field as its value.
Spring Data (Mongo) the uses this additional property when reading the document to decide on the concrete type to be instantiated in order to map the data from the document back to the entity.

Spring Data (Mongo) internally uses the `SimpleTypeInformationMapper` to look up the `_class` field and to load the corresponding class.
Once successfully looked up and loaded, this lookup is cached for performance reasons.
If the lookup fails, Spring Data (Mongo) falls back to the statically defined interface type, but later fails to instantiate the interface type (as expected).
In addition, the lookup is never done again, therefore all subsequent loading attempts for an entity with the same concrete field value will fail with the said exception.

Loading the class in the `SimpleTypeInformationMapper` is done via the `ClassUtils` utility class which internally uses the class loader of the current thread, if defined.

Debugging the issue showed that when invoking the parallel loading from http handler thread of the controller, the correct thread context class loader (i.e. `org.springframework.boot.loader.LaunchedURLClassLoader`) is set in the http handler thread, but any thread of the common thread pool used by `Stream.parallel()` has a different / wrong class loader (i.e. `jdk.internal.loader.ClassLoaders$AppClassLoader`) set.
When running the application from the command line using the Spring Boot jar, only the `org.springframework.boot.loader.LaunchedURLClassLoader` class loader is able to load the concrete implementation class, but not the `jdk.internal.loader.ClassLoaders$AppClassLoader` class loader.

## Potential Workaround

Setting the thread context class loader of the http handler thread as the class loader for every worker thread before invoking the Spring Data Mongo repository method prevents the error from occurring.

This can be simulated by setting running the minimal sample application with the `enable-workaround` property set to `true` (`java -jar target/spring-data-mongo-loading-issue-0.0.1-SNAPSHOT.jar --enable-workaround=true`)

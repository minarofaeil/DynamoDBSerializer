# POJO/Record-Based DynamoDB Serializer Generator

DynamoDBSerializer library generates **type-safe, reflection-free DynamoDB
serializers** for POJOs (Conventional JavaBeans) or records based on simple annotations. All
serialization logic is produced at compile time, so no runtime reflection
or configuration is required.

## Overview

Annotate a class (or a separate provider interface) with `@Serialize`,
and a concrete `Serializer<T>` implementation is generated automatically 
at compile time. The serializer converts between instances of the annotated type
and a `Map<String, AttributeValue>` suitable for use with the DynamoDB SDK.

All mapping logic is known at compile time, and the generated serializer
contains direct, type-checked and null-checked field access.

## Why Not Use the Enhanced DynamoDB Client?

The AWS Enhanced DynamoDB client has several limitations when used in
systems that need clean separation between domain models and persistence,
when reflection is problematic, or when classes cannot be modified:

-   **Reflection-based mapping**

    -   Adds runtime overhead.
    -   Defers mapping errors to runtime.
    -   Can conflict with reflection-restricted environments.

-   **Domain model coupling**

    -   Requires DynamoDB-specific annotations on business objects.
    -   Forces a persistence dependency into models that should remain
        library- or service-agnostic.

-   **Not suitable for generated or external POJOs**

    -   POJOs generated from systems such as Smithy cannot be easily
        annotated.
    -   Third-party model classes cannot be changed.

-   **Limited transparency and control**

    -   Mapping is partly implicit and not validated at compile time.

-   **Problematic Collection Deserialization**

    -   Because of Java Runtime Type Erasure, a reflection-based solution 
        cannot know the actual type of collections, potentially populating
        collections with wrong types.

## Why Not Use StaticTableSchema?

`StaticTableSchema` avoids reflection, but you must write and maintain
the schema manually. This is error-prone and not substantially simpler
than writing a serializer yourself.

This library eliminates that work entirely.

## Benefits

-   **Zero reflection**\
    All mapping logic is generated, with no runtime inspection.

-   **No DynamoDB dependencies in domain objects**\
    Only the `@Serialize` annotation is required, and it can be placed
    on:

    -   The type itself, or
    -   A separate provider interface if the type cannot be modified.

-   **Works with generated or third-party POJOs**\
    Ideal for models generated from Smithy or other IDLs.

-   **Type-correct handling of collections**\
    DynamoDB stores number sets (`NS`), string sets (`SS`), and list (`L`)
    types without Java generic type information.\
    With runtime reflection, deserialization of fields such as
    `List<Float>` or `List<MyType>` can be ambiguous due to Java
    type erasure.\
    Generated serializers avoid this issue because:

    -   Element types are known at compile time.
    -   The serializer emits unambiguous deserialization logic for the
        exact generic type.\
        This prevents incorrect fallback conversions and makes
        collection handling predictable.

-   **Predictable performance**\
    No reflective lookup, no dynamic converters, no runtime schema
    evaluation.

-   **Compile-time validation**\
    Mapping errors surface during compilation, not at runtime.

-   **Straightforward usage**\
    Annotate → compile → use the generated serializer.

## Example

### Annotating a POJO or Record

```java
@Serialize
public record MyDataType(String id, int count) {
}
```

The processor generates `MyDataTypeSerializer`

Usage:

```java
Serializer<MyDataType> serializer = MyDataTypeSerializer.create();
```

### When the Type Cannot Be Modified (Generated or 3rd Party)

```java
@Serialize(MyDataType.class)
public interface MyDataTypeSerializerProvider {
}
```

This produces the same serializer class.

## When to Use This Library

Use this library when:

-   You need DynamoDB serialization without using reflection.
-   You want domain models to remain independent of persistence
    concerns.
-   Your POJOs are generated and cannot be annotated.
-   You need reliable handling of generics and collection element types.
-   You prefer compile-time generation over runtime mapping.

## When Not to Use It

You may not want to use this library if:

-   You rely on advanced Enhanced Client features.
-   You prefer runtime schema customization.

## Summary

This library provides an efficient, reliable alternative to the enhanced
DynamoDB client and manual *StaticTableSchema* definitions. It offers:

-   No reflection
-   No persistence coupling
-   No manual schemas
-   No handwritten serializers
-   Correct handling of generics and collections

Simply annotate your classes and use the generated serializers.

## Installation

This library is not published to a maven repo (yet - work-in-progress). Until it is published,
follow these steps in a gradle project or similar for other setups.

1. Checkout this git repo to a local path, let's say `/home/myself/workspace/DynamoDBSerializer`.
2. In `settings.gradle` of the root of your project
```groovy
includeBuild('/home/myself/workspace/DynamoDBSerializer') {
    dependencySubstitution {
        substitute(module("ca.fineapps.util:DynamoDBSerializer")).using(project(":DynamoDBSerializer"))
        substitute(module("ca.fineapps.util:DynamoDBSerializerProcessor")).using(project(":DynamoDBSerializerProcessor"))
    }
}
```
3. In `build.gradle` of the project that needs serialization
```java
dependencies {
    ...
    implementation 'ca.fineapps.util:DynamoDBSerializer'
    annotationProcessor 'ca.fineapps.util:DynamoDBSerializerProcessor'
    ...
}
```
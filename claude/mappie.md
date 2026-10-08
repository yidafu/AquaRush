# Object Mapping with Mappie

> Imported by the root `CLAUDE.md`. Type-safe object-to-object mapping between DTOs, domain models, and API representations.

This project uses [Mappie](https://mappie.tech/) for type-safe object-to-object mapping between DTOs, domain models, and API representations. Mappie is a Kotlin compiler plugin that generates mapper code at compile-time (no reflection at runtime).

**Why Mappie**:
- Compile-time code generation for better performance
- Type-safe mappings with compile-time verification
- Kotlin-first design with native Kotlin feature support
- No runtime dependencies

**Installation**:

Add to `build.gradle.kts`:

```kt
plugins {
    id("tech.mappie.plugin") version("版本号")
}
```

When using mappie version below 1.0.0 or when you want to add the mappie-api dependency manually:

```kt
dependencies {
    implementation("tech.mappie:mappie-api:版本号")
}
```

**Configuration**:

Mappie can be configured via Gradle or per Mapper:

```kt
mappie {
    useDefaultArguments = false // Disable using default arguments in implicit mappings
    strictness {
        enums = false // Do not report an error if not all enum sources are mapped
        platformTypeNullability = true // Enable strict nullability checks for platform types
        visibility = true // Allow calling constructors not visible from the calling scope
    }
    reporting {
        enabled = true // Enable report generation
    }
}
```

Local configuration can be applied via annotations on the mapper class, overriding global settings:

| Gradle Option | Annotation | Default |
| --------------- | ------------ | --------- |
| useDefaultArguments | @UseDefaultArguments | true |
| strictness.enums | @UseStrictEnums | true |
| strictness.platformTypeNullability | @UseStrictPlatformTypeNullabilityValidation | true |
| strictness.visibility | @UseStrictVisibility | false |

**Basic Usage**:

**Key Features**:
- Automatic field mapping by name
- Enum mapping support
- Custom mapping functions
- Constructor-based mapping

**Enum Mapping**:

Mappie supports mapping enum classes by extending from `EnumMappie`. If both source and target are enum classes with identical entries, Mappie resolves names automatically:

```kotlin
enum class Color { RED, GREEN, BLUE }
enum class Colour { RED, GREEN, BLUE }

// Simple enum mapper - automatic mapping by name
object ColorMapper : EnumMappie<Color, Colour>()
```

For enums with different entries, use `fromEnumEntry` to explicitly map source entries to target:

```kotlin
enum class Color { RED, GREEN, BLUE, ORANGE }
enum class Colour { RED, GREEN, BLUE, OTHER }

// Explicit enum mapping with fromEnumEntry
object ColorMapper : EnumMappie<Color, Colour>() {
    override fun map(from: Color): Colour = mapping {
        Colour.OTHER fromEnumEntry Color.ORANGE
    }
}
```

**Inferring Implicit Mappings**:

Mappie infers implicit mappings by name, type, default arguments, getter- and setter methods, and other mappers that are defined. An implicit mapping for a target property is inferred automatically if it has the same name as a source property, and it is assignable from that source property. If it is not assignable, Mappie will check if there is a single mapper defined that can map the source type to the target type, and will automatically apply it. Mappie comes with several mappers out of the box. See Built-in Mappers.

For example, suppose we have a data class Person and a data class PersonDto:

```kotlin
data class Person(val name: String, val age: Int)

data class PersonDto(val name: String, val age: Int)
```

The properties of Person match the parameters of the primary constructor of PersonDto, and as such, no explicit mappings have to be defined. We can simply construct such a mapper by writing:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>()
```

which will generate a mapper which calls the primary constructor of PersonDto assigned to the fields of Person.

**Mapper Generation**:

Mappie can also generate mappers automatically. When a source type and a target type do not have an existing mapper, and one can be written without any explicit mappings, it will be generated automatically.

For example, suppose we have the data classes Person and PersonDto containing Gender and GenderDto enum classes:

```kotlin
data class Person(val name: String, val gender: Gender)
enum class Gender { MALE, FEMALE, OTHER }

data class PersonDto(val name: String, val gender: GenderDto)
enum class GenderDto { MALE, FEMALE, OTHER }
```

We can generate a mapper from Person to PersonDto by writing:

```kotlin
class PersonMapper : ObjectMappie<Person, PersonDto>()
```

and the nested mapper from Gender to GenderDto will be generated automatically as they both contain the same enum entries.

**Default Arguments**:

Mappie also considers default arguments as a possibility.

For example, suppose PersonDto is defined as:

```kotlin
data class PersonDto(
    val name: String,
    val age: Int,
    val hasChildren: Boolean = false,
)
```

Mappie will use the default argument `false` for `hasChildren` if no explicit mapping is defined. This is enabled by default and can be disabled by setting the configuration option `useDefaultArguments` to false.

**Constructing Explicit Mappings**:

Not all classes one wants to map are equivalent. Mappie supports defining explicit mappings for those which cannot be resolved automatically. This can be done via properties, values, or expressions as described in the coming sections.

Suppose we have a data class Person, and we have the data class PersonDto which has the property description which is not defined in Person:

```kotlin
data class Person(
    val name: String,
    val age: Int,
)

data class PersonDto(
    val name: String,
    val age: Int,
    val description: String,
)
```

If one would define a mapper without an explicit mapping for description, Mappie will give a compile-time error stating that the target description has no source defined. The target property can be assigned in different ways:

- mapping via a source property
- mapping via a value; or
- mapping via an expression

**Mapping via a Source Property**:

Targets can be set via the operator `fromProperty`. This will set the target to the given source property.

For example, the following snippet will construct a mapper where PersonDto.description is set to Person.name:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        PersonDto::description fromProperty from::name
    }
}
```

The target type is not always assignable from the source type. There are several ways to handle this. One way is to define a mapper from the source type to the target type. This can be applied explicitly using The Via Operator, or be implicitly applied by Mappie.

It is also possible to transform the property. For example to tweak the value, handle nullability, or transform the source in some other way. See The Transform Operator for some guidelines.

**Nullability**:

When mapping from a nullable type to a non-nullable type, one has several options. The most flexible option is to use the transform operator.

When the transformation logic is applying a simple non-null assertion operator, or a requireNotNull function call, `to::x fromPropertyNotNull from::y` steps in as an equivalent alternative to:

```kotlin
to::x fromProperty from::y transform { it!! }
```

**Mapping via a Value**:

Targets can be set via the operator `fromValue`. This will set the target to the given value.

For example, the following snippet will construct a mapper where PersonDto.description is set to "unknown":

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        PersonDto::description fromValue "unknown"
    }
}
```

**Mapping via an Expression**:

Targets can be set via the operator `fromExpression`. This will set the target to the given lambda result.

The difference between fromExpression and fromValue is that fromExpression will take a lambda function as a parameter, which takes the original source as a parameter. Allowing for more flexibility.

For example, the following snippet will construct a mapper where PersonDto.description is set to "Description: ${from.name}":

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        PersonDto::description fromExpression { from ->
            "Description: ${from.name}"
        }
    }
}
```

All mappings can be defined using fromExpression, but to keep the mappings clean and give Mappie the most information to suggest improvements to your code, fromProperty combined with either via or transform is preferred.

**Handling non-referenceable Targets**:

We can use the `to` function to refer to constructor parameters which do not have a property or to refer to a setter method.

For example, suppose that we use the same example as above, but PersonDto.description does not declare a backing property:

```kotlin
data class PersonDto(
    val name: String,
    val age: Int,
    description: String,
)
```

We cannot reference description via a property reference Person::description. To target the constructor parameter, we can use `to("description")` to reference the constructor parameter:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        to("description") fromValue "a constant"
    }
}
```

**Using a Specific Constructor**:

We can force Mappie to select a specific constructor using the different overloads of mapping. We can force a specific constructor by passing the types of the constructor parameters as type arguments to the call of mapping and passing a constructor reference. For example, suppose PersonDto is defined as:

```kotlin
data class PersonDto(
    val name: String,
    val age: Int,
    val description: String,
) {
    constructor(name: String, age: Int) : this(name, age, "description")
}
```

we can reference the primary constructor via:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person) =
        mapping<String, String, Int>(::PersonDto)
}
```

and we can reference the secondary constructor via:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person) =
        mapping<String, Int>(::PersonDto)
}
```

**The to Alias**:

We can access the target properties via the target type of the mapper. This can clutter the mapping definition when many explicit mappings are defined. Mappie defines a special `to` property which can be used instead of the target type.

For example, we can use to refer to the property streetname of PersonDto:

```kotlin
object PersonMapper : ObjectMappie<Person, PersonDto>() {
    override fun map(from: Person): PersonDto = mapping {
        to::streetname fromProperty from.address::street
    }
}
```

where `to::streetname` is equivalent to `PersonDto::streetname`.

**When to Use**:
- DTO to Domain model conversion
- API response to internal model transformation
- Entity toVO/VO to Entity conversions
- Any object structure mapping between layers

See [Mappie Documentation](https://mappie.tech/) for more details.

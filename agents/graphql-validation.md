# GraphQL Input Validation Reference

> Moved out of `spec/tech/` (usage detail, not spec). Schema-declared validation via the
> project's own codegen plugin; resolver auth patterns live in `resolvers.md`.

Validation rules are declared **in the GraphQL schema as directives**, turned into Jakarta
Validation annotations by the codegen plugin, and enforced at runtime before resolver methods
run. Business code never calls a Validator manually.

## Chain

1. **Schema**: input fields carry validation directives (`shared-config/graphql/shared-types.graphqls` defines the directives);
2. **Codegen**: `pnpm codegen` runs the workspace plugin `@graphql-codegen/kotlin-validation`
   (source `graphql-codegen-kotlin-validation/`), which writes `@field:` Jakarta annotations into
   `modules/aqua-foundation/src/main/graphql-gen/schema.kt` (never hand-edit; frontend types in
   `frontend/common/src/types/graphql.ts` get no annotations);
3. **Runtime**: Spring GraphQL argument binding validates annotated inputs before the resolver
   method executes — an invalid input never reaches business code;
4. **Error mapping**: `ValidationExceptionHandler` (aqua-foundation `common/graphql/exceptions/`)
   converts `ConstraintViolationException` / custom `ValidationException` into GraphQL errors.

## Supported directives

Mapping lives in `graphql-codegen-kotlin-validation/src/directive-mapping.ts`
(`VALIDATION_DIRECTIVES`); applicable positions: `ARGUMENT_DEFINITION` and `INPUT_FIELD_DEFINITION`.

| Directive | Jakarta annotation | Params |
|---|---|---|
| `@notBlank` | `@NotBlank` | `message` |
| `@size` | `@Size` | `min`, `max`, `message` |
| `@email` | `@Email` | `message` |
| `@pattern` | `@Pattern` | `regexp` (Java syntax), `message` |
| `@range` | `@Min` + `@Max` | `min`, `max`, `message` (special-cased in visitor) |
| `@min` / `@max` | `@Min` / `@Max` | `value`, `message` |
| `@positive` / `@negative` | `@Positive` / `@Negative` | `message`; `OrZero` variants too |
| `@decimalMin` / `@decimalMax` / `@digits` | same-named | |
| `@future` / `@past` | `@Future` / `@Past` | `message` |
| `@notNull` / `@null` | `@NotNull` / `@Null` | `message` |
| `@assertTrue` / `@assertFalse` | `@AssertTrue` / `@AssertFalse` | `message` |

Number validation covers Int/Long/Float/Double/BigDecimal; optional fields are validated only
when a value is provided; custom `message`s are user-facing — write them in Chinese.

## Example

```graphql
type Mutation {
  createUser(input: CreateUserInput!): User!
}

input CreateUserInput {
  username: String!
    @notBlank(message: "用户名不能为空")
    @size(min: 3, max: 20, message: "用户名长度必须在3-20个字符之间")
    @pattern(regexp: "^[a-zA-Z0-9_\\-]+$", message: "用户名只能包含字母、数字、下划线和连字符")
  email: String!
    @notBlank(message: "邮箱不能为空")
    @email(message: "邮箱格式不正确")
  age: Int @range(min: 1, max: 150, message: "年龄必须在1-150之间")
}

type Query {
  getUsers(
    page: Int! @range(min: 1, max: 1000, message: "页码必须在1-1000之间")
    size: Int! @range(min: 1, max: 100, message: "每页数量必须在1-100之间")
  ): UserPage!
}
```

Generated Kotlin (illustrative):

```kotlin
package dev.yidafu.aqua.common.graphql.generated

import jakarta.validation.constraints.*

data class CreateUserInput(
  @field:NotBlank(message = "用户名不能为空")
  @field:Size(min = "3", max = "20", message = "用户名长度必须在3-20个字符之间")
  @field:Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "用户名只能包含字母、数字、下划线和连字符")
  val username: kotlin.String,

  @field:NotBlank(message = "邮箱不能为空")
  @field:Email(message = "邮箱格式不正确")
  val email: kotlin.String,

  @field:Min(value = "1", message = "年龄必须在1-150之间")
  @field:Max(value = "150", message = "年龄必须在1-150之间")
  val age: kotlin.Int?,
)
```

The `@field:` prefix is what makes field validation work in Kotlin — keep it when extending the
visitor.

## Error format

```json
{
  "errors": [
    {
      "message": "用户名不能为空",
      "path": ["createUser"],
      "extensions": {
        "validationError": true,
        "errorType": "VALIDATION_ERROR"
      }
    }
  ],
  "data": null
}
```

## Resolver usage

Validation is automatic — resolvers just take typed inputs (never `Map<String, Any>`), optionally
with `@Valid` on the `@Argument`. See `resolvers.md` for the full resolver/auth patterns.

## Plugin maintenance

Plugin layout (`graphql-codegen-kotlin-validation/`, pnpm workspace package, jest-tested):

```
src/
├── index.ts              # plugin entry
├── visitor.ts            # writes @field: annotations
├── validation-enhancer.ts
├── directive-mapping.ts  # VALIDATION_DIRECTIVES / VALIDATION_PARAM_MAPPING
└── config.ts             # validationAnnotations option
```

Wiring in root `graphql-codegen.yml`: plugin `- "@graphql-codegen/kotlin-validation"` with
`validationAnnotations: true` under the `schema.kt` generate target.

**Adding a directive**: map it in `directive-mapping.ts` (+ params in `VALIDATION_PARAM_MAPPING`),
declare the directive definition in the shared schema, `npm run build` inside the plugin package,
then `pnpm codegen`.

# GraphQL Resolver Development Reference

> Imported by the root `CLAUDE.md`. Spring GraphQL annotation-based resolver patterns used across `aqua-admin` and `aqua-client`.

This project uses Spring GraphQL with annotation-based resolvers. All resolvers should follow these patterns.

**Resolver Structure**:

```kotlin
@Controller("clientOrderQueryResolver")  // Spring GraphQL controller
class OrderQueryResolver(
  private val orderQueryService: OrderQueryService,
) {
  // Query methods
}
```

**Available Annotations**:

| Annotation | Purpose |
|------------|---------|
| `@QueryMapping` | Marks a method as GraphQL query (read operation) |
| `@MutationMapping` | Marks a method as GraphQL mutation (write operation) |
| `@Argument` | Injects GraphQL input argument |
| `@AuthenticationPrincipal` | Injects authenticated user (UserPrincipal) |
| `@PreAuthorize` | Method-level security: `isAuthenticated()` for client resolvers; `hasAuthority('PERMISSION_<CODE>')` / `hasRole('ADMIN')` for admin resolvers |
| `@Valid` | Enables Jakarta validation on input DTOs |

**Query Method Example**:

```kotlin
@QueryMapping
@PreAuthorize("isAuthenticated()")
fun myOrders(
  @AuthenticationPrincipal userPrincipal: UserPrincipal,
): List<OrderModel> = orderQueryService.findOrdersByUserId(userPrincipal.id)

@QueryMapping
fun order(
  @Argument orderId: Long,
  @AuthenticationPrincipal userPrincipal: UserPrincipal,
): OrderModel? = orderQueryService.findOrderByIdAndUserId(orderId, userPrincipal.id)
```

**Mutation Method Example**:

```kotlin
@MutationMapping
@PreAuthorize("isAuthenticated()")
fun createOrder(
  @Argument @Valid input: CreateOrderInput,
  @AuthenticationPrincipal userPrincipal: UserPrincipal,
): OrderModel {
  val request = CreateOrderInputMapper.map(input).copy(userId = userPrincipal.id)
  return orderMutationService.createOrder(request)
}
```

**Working with Input DTOs**:

```kotlin
// Simple argument
@Argument orderId: Long

// With default value
@Argument id: Long = 0

// Nullable argument
@Argument status: OrderStatus?

// With validation
@Argument @Valid input: CreateProductInput
```

**Input type placement**: Input types are **never inlined in resolvers** — do not declare `data class XxxInput` inside a resolver (e.g. in a `companion object`). Service-local GraphQL input DTOs live in the service `<admin|client>/<domain>/dto/` package (e.g. `dev.yidafu.aqua.admin.user.dto`), typically with a `fromInput(...)` factory that maps from the generated GraphQL input type (`dev.yidafu.aqua.common.graphql.generated.*`); cross-module DTOs stay in `aqua-foundation` `api/dto`.

**UserPrincipal Usage**:

The `UserPrincipal` class provides authenticated user information:

```kotlin
data class UserPrincipal(
  val id: Long,
  private val _username: String,
  val userType: String,  // USER, WORKER, ADMIN
  private val _authorities: Collection<GrantedAuthority>,
) : UserDetails {
  // Methods
  fun hasRole(role: String): Boolean
  fun hasAuthority(authority: String): Boolean
}
```

Common usage patterns:
- `userPrincipal.id` - Get current user ID
- `userPrincipal.userType` - Get user type (USER/ADMIN/WORKER)
- `userPrincipal.hasRole("ADMIN")` - Check role

**BaseGraphQLResolver**:

Extend `BaseGraphQLResolver` for shared user-resolution and logging helpers. Available protected methods: `getCurrentUser(userPrincipal)` (falls back to the GraphQL context when the param is null), `requireAuthenticated(userPrincipal)`, and `logOperation(userPrincipal, operation, parameters)`. Note: authorization is NOT done via helper calls here — it is declared with `@PreAuthorize` on the resolver methods themselves.

```kotlin
@Controller
class MyResolver(
  private val service: MyService,
) : BaseGraphQLResolver() {

  @QueryMapping
  @PreAuthorize("hasAuthority('PERMISSION_ORDER_WRITE')")  // admin resolver
  fun updateOrder(
    @Argument input: UpdateOrderInput,
    @AuthenticationPrincipal userPrincipal: UserPrincipal,
  ): Result {
    logOperation(userPrincipal, "updateOrder")
    return service.updateOrder(input, userPrincipal.id)
  }
}
```

**Common Patterns**:

1. **Client vs Admin Resolvers**: Use `@ClientService` (guarded by `aqua.mode=client`) for client-facing APIs, `@AdminService` (guarded by `aqua.mode=admin`) for admin APIs
2. **Admin permission guards**: annotate each admin resolver method with `@PreAuthorize("hasAuthority('PERMISSION_<CODE>')")` using the exact code from `AdminPermission`; client resolvers use `@PreAuthorize("isAuthenticated()")`
3. **Naming Convention**: Resolver name should match GraphQL schema type name
4. **Error Handling**: Throw `IllegalArgumentException` for not found, handle authorization via `@PreAuthorize`
5. **Validation**: Use `@Valid` with input DTOs for automatic validation
6. **Input type placement**: never inline `data class XxxInput` in a resolver — service-local GraphQL input DTOs go in `<domain>/dto/`, cross-module DTOs in `aqua-foundation` `api/dto`

**File Location**: Resolvers live in the service modules under `<domain>/resolvers/` (e.g. `services/aqua-admin/.../admin/order/resolvers/`, `services/aqua-client/.../client/marketing/resolver/`). Thin module adapters may stay in the module (e.g. `analytics/reconciliation/resolvers`).

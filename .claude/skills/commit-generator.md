# Commit Message Generator

A comprehensive skill for generating high-quality Git commit messages based on staged changes, following Angular convention and AquaRush project patterns.

## Features

- Analyzes staged changes to understand the scope and impact
- Reviews recent commit history to maintain consistency
- Follows Angular commit message convention
- Identifies appropriate commit type (feat, fix, docs, style, refactor, test, chore)
- Suggests appropriate scope based on module structure
- Generates detailed commit body when needed
- Includes required co-authorship information

## Process

1. **Check Repository State**
   - Verify we're in a git repository
   - Check for staged changes
   - Identify modified files and modules

2. **Analyze Changes**
   - Categorize changes by type (feature, bug fix, documentation, etc.)
   - Determine scope based on affected modules (aqua-user, aqua-order, etc.)
   - Assess impact and complexity

3. **Review History**
   - Get recent commit messages for style reference
   - Understand project-specific conventions
   - Maintain consistency with previous commits

4. **Generate Message**
   - Create appropriate commit type and scope
   - Write concise description (50-72 characters)
   - Add detailed body if changes are complex
   - Include proper attribution

## Commit Types Used in AquaRush

- `feat`: New features (e.g., user authentication, payment integration)
- `fix`: Bug fixes (e.g., database connection issues, UI fixes)
- `docs`: Documentation changes (README, API docs, comments)
- `style`: Code formatting without logic changes (ktlint, imports)
- `refactor`: Code refactoring without feature changes
- `test`: Adding or modifying tests
- `chore`: Build, dependency, or configuration changes

## Common Scopes

- `user`: User management and authentication
- `order`: Order processing and management
- `delivery`: Delivery worker and task management
- `product`: Product catalog and inventory
- `payment`: Payment processing and integration
- `common`: Shared utilities and infrastructure
- `admin`: Admin dashboard functionality
- `schema`: Database schema and migrations
- `frontend`: Client-side applications

## Example Outputs

Simple feature:
```
feat(user): add WeChat Mini Program authentication

Implement OAuth2 flow with WeChat API for user login
and registration in Mini Program clients.

```

Bug fix:
```
fix(order): resolve payment timeout handling

Fix race condition where payment timeout events were
not properly processed due to missing transaction rollback.

```

## Safety Checks

- Refuses to commit files that may contain secrets (.env, credentials)
- Validates that changes are appropriate for commit
- Ensures commit message follows project standards
- Checks for breaking changes and adds appropriate warnings

# Commit Message Generation & Automation Scripts

> Imported by the root `CLAUDE.md`. How to commit following Angular convention, and the Python-first scripting standards.

## Commit Message Generation

This project includes automated commit message generation tools to maintain consistent commit history following Angular convention.

### Available Tools

1. **Python Scripts (Default)**: `./scripts/*.py`
   - **Default choice for all automation tasks**
   - Use `./scripts/analyze_code.py` for code analysis and pattern detection
   - Use `./scripts/check_models.py` to verify model consistency
   - Use `./scripts/generate_commit.py` for automated commit messages
   - Python scripts provide better error handling and cross-platform compatibility

2. **Interactive Script**: `./scripts/generate-commit.sh`
   - Analyzes staged changes automatically
   - Suggests appropriate commit type and scope
   - Validates Angular commit format
   - Includes proper attribution
   - Use when Python scripts are not available

3. **Slash Command**: `/commit` (when available)
   - Generates commit messages based on changes
   - Follows project-specific patterns
   - Includes co-authorship information

### Commit Types Used

- `feat`: New features and functionality
- `fix`: Bug fixes and error corrections
- `docs`: Documentation and README changes
- `style`: Code formatting without logic changes
- `refactor`: Code restructuring without feature changes
- `test`: Adding or modifying tests
- `chore`: Build, dependency, or configuration changes

### Common Scopes

- `user`: User management and authentication modules
- `order`: Order processing and management
- `delivery`: Delivery worker and task management
- `product`: Product catalog and inventory
- `payment`: Payment processing and WeChat Pay integration
- `marketing`: Marketing domain (coupons, promotions, tickets, flash sale, gift, first-order)
- `ops`: Operations config (banner, notice, feedback, store profile, delivery fee rule, system config)
- `common`: Shared utilities and infrastructure
- `admin`: Admin dashboard functionality
- `schema`: Database schema and migrations
- `frontend`: Client-side applications (Mini Programs, Admin Dashboard)

### Usage Example

```bash
# Stage your changes
git add modules/aqua-user/src/main/kotlin/dev/yidafu/aqua/user/service/AuthService.kt

# Preferred: Use Python script for commit generation
./scripts/generate_commit.py

# Alternative: Use shell script
./scripts/generate-commit.sh

# Or use the traditional way with template
git commit -m "$(cat <<'EOF'
feat(user): implement JWT authentication with WeChat integration

Add JWT token generation and validation with WeChat Mini Program
OAuth2 flow for secure user authentication.
EOF
)"
```

### Script Preference Guidelines

**Always prefer Python scripts over shell scripts when available:**

- **Python scripts** (`*.py`) are the default choice for:
  - Code analysis and pattern detection
  - Model consistency checks
  - Automated commit message generation
  - File structure validation
  - Cross-platform compatibility

- **Shell scripts** (`*.sh`) are used for:
  - Simple build and deployment tasks
  - System service management
  - When Python is not available in the environment

## Automation and Scripting

**IMPORTANT**: This project prioritizes Python scripts for all automation and analysis tasks. Python scripts provide better error handling, cross-platform compatibility, and more maintainable code.

### Python Script Standards

- **Default choice**: Always use Python scripts (`*.py`) over shell scripts (`*.sh`) when both are available
- **Location**: All Python scripts are located in `./scripts/` directory
- **Python version**: Compatible with Python 3.8+
- **Dependencies**: Use minimal external dependencies, prefer standard library
- **Error handling**: Include proper exception handling and user-friendly error messages
- **Documentation**: Include docstrings and usage examples

### Common Python Scripts

1. **Code Analysis**: `./scripts/analyze_code.py`
   - Analyzes code patterns and structure
   - Detects inconsistencies and missing implementations
   - Generates reports on code quality

2. **Model Validation**: `./scripts/check_models.py`
   - Validates entity model consistency
   - Checks for missing required fields and annotations
   - Ensures proper implementation of interfaces

3. **Commit Generation**: `./scripts/generate_commit.py`
   - Automatically generates Angular-style commit messages
   - Analyzes git changes to suggest appropriate commit types
   - Includes co-authorship and attribution information

4. **File Structure**: `./scripts/validate_structure.py`
   - Validates project structure and organization
   - Checks for missing required files and directories
   - Ensures consistency across modules

### When to Use Shell Scripts

Shell scripts are still used for specific scenarios:

- System service management (systemctl, service commands)
- Simple build and deployment pipelines
- Environment setup and configuration
- When Python dependencies are not available

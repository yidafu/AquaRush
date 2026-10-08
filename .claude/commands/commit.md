# Generate Commit Message

Generate a Git commit message based on the current staged changes and recent commit history following Angular convention format.

## Usage

```bash
/commit
```

## What it does

1. Analyzes the current git status and staged changes
2. Reviews recent commit messages to understand the project's commit style
3. Generates a commit message following Angular convention:
   - feat: new feature
   - fix: bug fix
   - docs: documentation changes
   - style: formatting, missing semi colons, etc; no code change
   - refactor: refactoring production code
   - test: adding tests, refactoring test; no production code change
   - chore: updating build tasks, package manager configs, etc; no production code change

## Generated commit format

```
<type>(<scope>): <description>

[optional body]
```

## Important Notes

- **Do NOT add** any Claude Code attribution or co-authorship signatures
- Keep the commit message clean and professional
- Only include the commit title and optional body

## Requirements

- Must have staged changes (`git add` has been run)
- Changes should be related to AquaRush project development
- Will refuse to commit files that may contain secrets or sensitive data

#!/bin/bash

# Generate Commit Message Script for AquaRush
# This script generates a Git commit message with markdown formatting based on staged changes

set -e

echo "🔍 Analyzing staged changes..."

# Check if we're in a git repository
if ! git rev-parse --git-head > /dev/null 2>&1; then
    echo "❌ Error: Not in a Git repository"
    exit 1
fi

# Check for staged changes
if ! git diff --cached --quiet; then
    echo "📝 Found staged changes:"
    git diff --cached --name-only | head -10
else
    echo "❌ No staged changes found. Please run 'git add' first."
    exit 1
fi

# Get recent commit history for context
echo ""
echo "📜 Recent commit messages:"
git log --oneline -5

# Get staged changes summary
echo ""
echo "📊 Staged changes summary:"
echo "Files modified: $(git diff --cached --name-only | wc -l | tr -d ' ')"
echo "Lines added: $(git diff --cached --numstat | awk '{sum += $1} END {print sum}')"
echo "Lines removed: $(git diff --cached --numstat | awk '{sum += $2} END {print sum}')"

# Analyze file types to suggest commit type
staged_files=$(git diff --cached --name-only)
commit_type="chore"  # default
scopes=()
descriptions=()

for file in $staged_files; do
    if [[ $file == modules/aqua-user/* ]]; then
        scopes+=("user")
        if [[ $file == *.kt && $file != *test* ]]; then
            commit_type="feat"
            descriptions+=("User management functionality")
        fi
    elif [[ $file == modules/aqua-order/* ]]; then
        scopes+=("order")
        commit_type="feat"
        descriptions+=("Order processing and management")
    elif [[ $file == modules/aqua-delivery/* ]]; then
        scopes+=("delivery")
        commit_type="feat"
        descriptions+=("Delivery worker and task management")
    elif [[ $file == modules/aqua-product/* ]]; then
        scopes+=("product")
        commit_type="feat"
        descriptions+=("Product catalog and inventory")
    elif [[ $file == modules/aqua-payment/* ]]; then
        scopes+=("payment")
        commit_type="feat"
        descriptions+=("Payment processing and transactions")
    elif [[ $file == modules/aqua-common/* ]]; then
        scopes+=("common")
        descriptions+=("Shared utilities and infrastructure")
    elif [[ $file == services/* ]]; then
        scopes+=("services")
        commit_type="refactor"
        descriptions+=("Service layer configuration and structure")
    elif [[ $file == frontend/* ]]; then
        scopes+=("frontend")
        commit_type="feat"
        descriptions+=("Client-side applications and UI")
    elif [[ $file == *.md ]]; then
        commit_type="docs"
        descriptions+=("Documentation and README updates")
    elif [[ $file == *.gradle* || $file == package*.json || $file == *requirements*.txt ]]; then
        commit_type="chore"
        descriptions+=("Build configuration and dependencies")
    elif [[ $file == *.sql ]]; then
        scopes+=("schema")
        commit_type="feat"
        descriptions+=("Database schema and migrations")
    fi
done

# Remove duplicate scopes
unique_scopes=($(printf "%s\n" "${scopes[@]}" | sort -u))
scope=$(IFS=,; echo "${unique_scopes[*]}")

echo ""
echo "💡 Suggested commit type: $commit_type"
echo "🎯 Suggested scope: $scope"

# Generate detailed changes summary
echo ""
echo "📋 Changes by category:"
for description in "${descriptions[@]}"; do
    echo "  - $description"
done

# Prompt for commit message
echo ""
echo "✏️  Please enter your commit message (or press Ctrl+C to cancel):"
read -p "Commit title (format: type(scope): description): " commit_title

if [[ -z "$commit_title" ]]; then
    echo "❌ No commit message provided"
    exit 1
fi

# Validate Angular commit format
if [[ ! "$commit_title" =~ ^[a-z]+\([^)]+\): .+$ ]]; then
    echo "⚠️  Warning: Commit message doesn't follow Angular format (type(scope): description)"
    echo "   Consider using format like: feat(user): add authentication feature"
    echo ""
    read -p "Continue anyway? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Ask for detailed body
echo ""
read -p "Add detailed commit body? (Y/n): " want_body
want_body=${want_body:-Y}

commit_body=""
if [[ "$want_body" =~ ^[Yy]$ ]]; then
    echo "📝 Enter commit body (press Enter twice to finish):"
    while IFS= read -r line; do
        if [[ -z "$line" && -n "$commit_body" ]]; then
            break
        fi
        commit_body+="$line"$'\n'
    done
fi

# Generate changes summary for commit body
if [[ -z "$commit_body" ]]; then
    # Auto-generate body based on changes
    echo "🔄 Auto-generating commit body from changes..."

    # Group changes by module
    user_files=$(git diff --cached --name-only | grep "modules/aqua-user/" | wc -l | tr -d ' ')
    order_files=$(git diff --cached --name-only | grep "modules/aqua-order/" | wc -l | tr -d ' ')
    delivery_files=$(git diff --cached --name-only | grep "modules/aqua-delivery/" | wc -l | tr -d ' ')
    product_files=$(git diff --cached --name-only | grep "modules/aqua-product/" | wc -l | tr -d ' ')
    payment_files=$(git diff --cached --name-only | grep "modules/aqua-payment/" | wc -l | tr -d ' ')
    common_files=$(git diff --cached --name-only | grep "modules/aqua-common/" | wc -l | tr -d ' ')
    service_files=$(git diff --cached --name-only | grep "services/" | wc -l | tr -d ' ')
    frontend_files=$(git diff --cached --name-only | grep "frontend/" | wc -l | tr -d ' ')

    commit_body="## 📊 Changes Summary"$'\n'$'\n'

    if [[ $user_files -gt 0 ]]; then
        commit_body+="- **User Module**: $user_files files modified"$'\n'
    fi
    if [[ $order_files -gt 0 ]]; then
        commit_body+="- **Order Module**: $order_files files modified"$'\n'
    fi
    if [[ $delivery_files -gt 0 ]]; then
        commit_body+="- **Delivery Module**: $delivery_files files modified"$'\n'
    fi
    if [[ $product_files -gt 0 ]]; then
        commit_body+="- **Product Module**: $product_files files modified"$'\n'
    fi
    if [[ $payment_files -gt 0 ]]; then
        commit_body+="- **Payment Module**: $payment_files files modified"$'\n'
    fi
    if [[ $common_files -gt 0 ]]; then
        commit_body+="- **Common Module**: $common_files files modified"$'\n'
    fi
    if [[ $service_files -gt 0 ]]; then
        commit_body+="- **Services**: $service_files files modified"$'\n'
    fi
    if [[ $frontend_files -gt 0 ]]; then
        commit_body+="- **Frontend**: $frontend_files files modified"$'\n'
    fi

    # Add impact assessment
    total_files=$(git diff --cached --name-only | wc -l | tr -d ' ')
    lines_added=$(git diff --cached --numstat | awk '{sum += $1} END {print sum}')
    lines_removed=$(git diff --cached --numstat | awk '{sum += $2} END {print sum}')

    commit_body+=$'\n'$'\n'"## 📈 Impact"$'\n'
    commit_body+="- **Files Changed**: $total_files"$'\n'
    commit_body+="- **Lines Added**: $lines_added"$'\n'
    commit_body+="- **Lines Removed**: $lines_removed"$'\n'

    # Add testing requirements if this is a feature
    if [[ "$commit_type" == "feat" ]]; then
        commit_body+=$'\n'$'\n'"## 🧪 Testing Requirements"$'\n'
        commit_body+="- [ ] Unit tests pass"$'\n'
        commit_body+="- [ ] Integration tests pass"$'\n'
        commit_body+="- [ ] Manual testing completed"$'\n'
    fi
fi

# Create commit with markdown formatting
full_commit_message="$commit_title

$commit_body"

echo ""
echo "🚀 Creating commit with markdown formatting..."
echo "$full_commit_message" | git commit -

echo ""
echo "✅ Commit created successfully!"

# Show the new commit
echo ""
echo "📋 New commit:"
git log --pretty=format:"%h - %an (%ar): %s" -1

echo ""
echo "📖 Commit details:"
git log --pretty=format:"%B" -1
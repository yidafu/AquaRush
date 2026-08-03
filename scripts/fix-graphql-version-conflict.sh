#!/bin/bash

# Fix GraphQL Version Conflict Script
# This script resolves the "Cannot use GraphQLScalarType from another module or realm" error

set -e

echo "🔧 Fixing GraphQL version conflict..."

# 1. Clean up existing node_modules and lock files
echo "🧹 Cleaning up existing dependencies..."
rm -rf node_modules
rm -rf graphql-codegen-kotlin-validation/node_modules
rm -f package-lock.json
rm -f graphql-codegen-kotlin-validation/package-lock.json

# 2. Clear npm cache to ensure fresh install
echo "💾 Clearing npm cache..."
npm cache clean --force

# 3. Install dependencies with exact GraphQL version
echo "📦 Installing dependencies with consolidated GraphQL version..."
npm install

# 4. Install the custom plugin dependencies
echo "🔌 Installing custom plugin dependencies..."
cd graphql-codegen-kotlin-validation
npm install
cd ..

# 5. Verify GraphQL versions are consistent
echo "✅ Verifying GraphQL versions..."
ROOT_GRAPHQL_VERSION=$(node -e "console.log(require('./node_modules/graphql/package.json').version)")
PLUGIN_GRAPHQL_VERSION=$(node -e "console.log(require('./graphql-codegen-kotlin-validation/node_modules/graphql/package.json').version)")

echo "Root GraphQL version: $ROOT_GRAPHQL_VERSION"
echo "Plugin GraphQL version: $PLUGIN_GRAPHQL_VERSION"

if [ "$ROOT_GRAPHQL_VERSION" != "$PLUGIN_GRAPHQL_VERSION" ]; then
    echo "❌ GraphQL versions still don't match!"
    echo "Root: $ROOT_GRAPHQL_VERSION, Plugin: $PLUGIN_GRAPHQL_VERSION"
    exit 1
fi

echo "✅ GraphQL versions are now consistent!"

# 6. Test the code generation
echo "🧪 Testing GraphQL code generation..."
npx graphql-codegen --config graphql-codegen.yml --dry-run || {
    echo "❌ GraphQL codegen dry run failed"
    exit 1
}

echo "🎉 GraphQL version conflict has been resolved!"
echo "You can now run: npx graphql-codegen --config graphql-codegen.yml"
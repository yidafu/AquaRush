#!/usr/bin/env node

/**
 * Enhanced Kotlin Codegen Script
 *
 * This script runs the standard GraphQL codegen and then enhances the generated Kotlin code
 * with Jakarta Validation annotations based on GraphQL directives.
 */

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Configuration
const SCHEMA_FILES = [
  'shared-config/graphql/shared-types.graphqls',
  'services/aqua-client/src/main/resources/graphql/client-schema.graphqls',
  'services/aqua-admin/src/main/resources/graphql/admin-schema.graphqls'
];

const OUTPUT_FILE = 'modules/aqua-common/src/main/graphql-gen/schema.kt';

// Validation directive to annotation mapping
const VALIDATION_MAPPING = {
  '@notBlank': {
    annotation: 'NotBlank',
    package: 'jakarta.validation.constraints',
    params: { 'message': 'message' }
  },
  '@size': {
    annotation: 'Size',
    package: 'jakarta.validation.constraints',
    params: { 'min': 'min', 'max': 'max', 'message': 'message' }
  },
  '@email': {
    annotation: 'Email',
    package: 'jakarta.validation.constraints',
    params: { 'message': 'message' }
  },
  '@pattern': {
    annotation: 'Pattern',
    package: 'jakarta.validation.constraints',
    params: { 'regexp': 'regexp', 'message': 'message' }
  },
  '@range': {
    annotation: 'Range',
    package: 'jakarta.validation.constraints',
    params: { 'min': 'min', 'max': 'max', 'message': 'message' }
  },
  '@positive': {
    annotation: 'Positive',
    package: 'jakarta.validation.constraints',
    params: { 'message': 'message' }
  },
  '@future': {
    annotation: 'Future',
    package: 'jakarta.validation.constraints',
    params: { 'message': 'message' }
  },
  '@past': {
    annotation: 'Past',
    package: 'jakarta.validation.constraints',
    params: { 'message': 'message' }
  },
  '@min': {
    annotation: 'Min',
    package: 'jakarta.validation.constraints',
    params: { 'value': 'value', 'message': 'message' }
  },
  '@max': {
    annotation: 'Max',
    package: 'jakarta.validation.constraints',
    params: { 'value': 'value', 'message': 'message' }
  },
  '@notNull': {
    annotation: 'NotNull',
    package: 'jakarta.validation.constraints',
    params: { 'message': 'message' }
  }
};

/**
 * Parse GraphQL schema and extract input types with validation directives
 */
function parseGraphQLSchema() {
  const inputTypes = new Map();

  for (const schemaFile of SCHEMA_FILES) {
    if (!fs.existsSync(schemaFile)) {
      console.warn(`Schema file not found: ${schemaFile}`);
      continue;
    }

    const content = fs.readFileSync(schemaFile, 'utf8');

    // Find input type definitions
    const inputTypeRegex = /input\s+(\w+)\s*\{([^}]+)\}/g;
    let match;

    while ((match = inputTypeRegex.exec(content)) !== null) {
      const typeName = match[1];
      const body = match[2];

      // Parse fields with directives
      const fields = [];
      const fieldRegex = /(\w+)\s*:\s*([^!?\n\s][^!\n]*?)(?:\s+([^]*?))?(?=\s+\w+\s*:|$)/g;
      let fieldMatch;

      while ((fieldMatch = fieldRegex.exec(body)) !== null) {
        const fieldName = fieldMatch[1];
        const fieldType = fieldMatch[2].trim();
        const directivesText = fieldMatch[3] || '';

        // Parse directives
        const directives = [];
        const directiveRegex = /@(\w+)\(([^)]*)\)|@(\w+)/g;
        let directiveMatch;

        while ((directiveMatch = directiveRegex.exec(directivesText)) !== null) {
          const directiveName = `@${directiveMatch[1] || directiveMatch[3]}`;
          const directiveArgs = directiveMatch[2] || '';

          const mapping = VALIDATION_MAPPING[directiveName];
          if (mapping) {
            const args = {};
            if (directiveArgs) {
              const argPairs = directiveArgs.split(',').map(arg => arg.trim());
              for (const argPair of argPairs) {
                const [key, ...valueParts] = argPair.split(':');
                const value = valueParts.join(':').trim().replace(/^["']|["']$/g, '');
                args[key] = value;
              }
            }

            directives.push({
              name: directiveName,
              annotation: mapping.annotation,
              package: mapping.package,
              args,
              params: mapping.params || {}
            });
          }
        }

        if (directives.length > 0) {
          fields.push({
            name: fieldName,
            type: fieldType,
            directives
          });
        }
      }

      if (fields.length > 0) {
        inputTypes.set(typeName, fields);
      }
    }
  }

  return inputTypes;
}

/**
 * Enhance generated Kotlin code with validation annotations
 */
function enhanceKotlinCode(originalCode, inputTypes) {
  let enhancedCode = originalCode;

  // Add Jakarta Validation imports if needed
  if (inputTypes.size > 0) {
    const imports = new Set();
    inputTypes.forEach(fields => {
      fields.forEach(field => {
        field.directives.forEach(directive => {
          if (directive.package === 'jakarta.validation.constraints') {
            imports.add('import jakarta.validation.constraints.*');
          }
        });
      });
    });

    if (imports.size > 0) {
      const packageRegex = /(package\s+[^\n]+)/;
      const match = enhancedCode.match(packageRegex);
      if (match) {
        enhancedCode = enhancedCode.replace(packageRegex,
          `${match[1]}\n\n${Array.from(imports).join('\n')}`);
      }
    }
  }

  // Add validation annotations to data classes
  inputTypes.forEach((fields, typeName) => {
    // Find the data class definition
    const dataClassRegex = new RegExp(`data class ${typeName}\\s*\\(([^}]+)\\)\\s*\\{([^}]+)\\}`, 's');
    const match = enhancedCode.match(dataClassRegex);

    if (match) {
      const classBody = match[1];
      const constructorBody = match[2];

      // Parse and enhance each field
      const lines = classBody.split('\n').map(line => line.trim());
      const enhancedLines = [];

      for (const line of lines) {
        if (line.startsWith('val ')) {
          const fieldMatch = line.match(/val\s+(\w+):\s*(.+?)(?:\s*=\s*(.+))?$/);
          if (fieldMatch) {
            const fieldName = fieldMatch[1];
            const fieldType = fieldMatch[2];
            const defaultValue = fieldMatch[3] || '';

            // Find directives for this field
            const fieldDirectives = fields.find(f => f.name === fieldName)?.directives || [];

            // Add validation annotations
            if (fieldDirectives.length > 0) {
              fieldDirectives.forEach(directive => {
                const args = [];
                for (const [param, field] of Object.entries(directive.args)) {
                  const mappedParam = directive.annotation === 'Pattern' && param === 'regexp'
                    ? 'regexp'
                    : directive.params[param] || param;
                  args.push(`${mappedParam} = "${field}"`);
                }

                const annotation = args.length > 0
                  ? `@field:${directive.annotation}(${args.join(', ')})`
                  : `@field:${directive.annotation}`;

                enhancedLines.push(`    ${annotation}`);
              });
            }
          }
        }

        enhancedLines.push(line);
      }

      const enhancedClassBody = enhancedLines.join('\n');
      enhancedCode = enhancedCode.replace(dataClassRegex,
        `data class ${typeName}(\n${enhancedClassBody}) {\n${constructorBody}}`);
    }
  });

  return enhancedCode;
}

/**
 * Main function
 */
function main() {
  console.log('🚀 Running Enhanced Kotlin Codegen with Jakarta Validation...');

  try {
    // Step 1: Parse GraphQL schema
    console.log('📖 Parsing GraphQL schema...');
    const inputTypes = parseGraphQLSchema();

    if (inputTypes.size === 0) {
      console.log('⚠️  No validation directives found in schema.');
      console.log('📊 Processing with standard codegen...');
    } else {
      console.log(`✅ Found ${inputTypes.size} input types with validation directives:`);
      inputTypes.forEach((fields, typeName) => {
        console.log(`  - ${typeName} (${fields.length} fields with directives)`);
      });
    }

    // Step 2: Run standard codegen
    console.log('\n📦 Running standard GraphQL codegen...');
    execSync('npx graphql-codegen --config codegen.yml', { stdio: 'inherit' });

    // Step 3: Enhance generated code
    if (inputTypes.size > 0) {
      console.log('\n✨ Enhancing generated code with validation annotations...');

      if (fs.existsSync(OUTPUT_FILE)) {
        const originalCode = fs.readFileSync(OUTPUT_FILE, 'utf8');
        const enhancedCode = enhanceKotlinCode(originalCode, inputTypes);

        fs.writeFileSync(OUTPUT_FILE, enhancedCode, 'utf8');
        console.log('✅ Code enhancement completed successfully!');

        // Show statistics
        const annotationCount = Array.from(inputTypes.values())
          .reduce((total, fields) => total + fields.reduce((sum, field) => sum + field.directives.length, 0), 0);

        console.log(`📊 Added ${annotationCount} validation annotations to ${inputTypes.size} classes.`);

      } else {
        console.error(`❌ Generated file not found: ${OUTPUT_FILE}`);
      }
    }

    console.log('\n🎉 Enhanced codegen completed successfully!');

  } catch (error) {
    console.error('❌ Error during enhanced codegen:', error.message);
    process.exit(1);
  }
}

// Run the script
if (require.main === module) {
  main();
}

module.exports = { main, parseGraphQLSchema, enhanceKotlinCode };
#!/usr/bin/env node

/**
 * Enhanced Kotlin Codegen Script V2
 *
 * This script enhances the generated Kotlin code with Jakarta Validation annotations
 * based on GraphQL directives.
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
    package: 'jakarta.validation.constraints'
  },
  '@size': {
    annotation: 'Size',
    package: 'jakarta.validation.constraints',
    params: { 'min': 'min', 'max': 'max', 'message': 'message' }
  },
  '@email': {
    annotation: 'Email',
    package: 'jakarta.validation.constraints'
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
    package: 'jakarta.validation.constraints'
  },
  '@future': {
    annotation: 'Future',
    package: 'jakarta.validation.constraints'
  },
  '@past': {
    annotation: 'Past',
    package: 'jakarta.validation.constraints'
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
    const inputTypeRegex = /input\s+(\w+)\s*\{([^}]+)\}/gs;
    let match;

    while ((match = inputTypeRegex.exec(content)) !== null) {
      const typeName = match[1];
      const body = match[2];

      // Parse fields with directives
      const fields = [];

      // Split by lines and process each field
      const lines = body.split('\n').map(line => line.trim()).filter(line => line && !line.startsWith('#'));

      for (let i = 0; i < lines.length; i++) {
        let line = lines[i];

        // Collect directives that follow the field definition
        let directivesText = '';

        // Check if this line has directives or if the next lines have directives for this field
        if (line.includes('@')) {
          // This line contains field and possibly directives
          const parts = line.split(/@/);
          if (parts.length > 1) {
            line = parts[0].trim();
            directivesText = '@' + parts.slice(1).join('@');
          }
        } else if (i + 1 < lines.length && lines[i + 1].startsWith('@')) {
          // The next lines contain directives for this field
          const directives = [];
          i++;
          while (i < lines.length && lines[i].startsWith('@')) {
            directives.push(lines[i]);
            i++;
          }
          i--; // Back up one since the for loop will increment
          directivesText = directives.join(' ');
        }

        // Match field definition
        const fieldMatch = line.match(/^(\w+)\s*:\s*([^!?\s=]+(?:[?!][^,\n]*)?)(?:\s*=\s*([^,\n]+))?$/);

        if (fieldMatch) {
          const fieldName = fieldMatch[1];
          const fieldType = fieldMatch[2];
          const defaultValue = fieldMatch[3] || '';

          // Parse directives
          const directives = [];

          // Match all directives
          const directiveRegex = /@(\w+)(?:\(([^)]*)\))?/g;
          let directiveMatch;

          while ((directiveMatch = directiveRegex.exec(directivesText)) !== null) {
            const directiveName = `@${directiveMatch[1]}`;
            const directiveArgs = directiveMatch[2] || '';

            const mapping = VALIDATION_MAPPING[directiveName];
            if (mapping) {
              const args = {};
              if (directiveArgs) {
                // Parse arguments like "min: 2, max: 50, message: \"Name too long\""
                const argPairs = directiveArgs.split(',').map(arg => arg.trim());
                for (const argPair of argPairs) {
                  const [key, ...valueParts] = argPair.split(':');
                  if (key && valueParts.length > 0) {
                    const value = valueParts.join(':').trim().replace(/^["']|["']$/g, '');
                    args[key] = value;
                  }
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
      }

      if (fields.length > 0) {
        // Merge with existing fields if type already exists
        if (inputTypes.has(typeName)) {
          const existingFields = inputTypes.get(typeName);
          fields.forEach(field => {
            if (!existingFields.find(f => f.name === field.name)) {
              existingFields.push(field);
            }
          });
        } else {
          inputTypes.set(typeName, fields);
        }
      }
    }
  }

  return inputTypes;
}

/**
 * Generate Kotlin annotation string
 */
function generateKotlinAnnotation(directive) {
  const args = [];

  // Build annotation parameters
  for (const [param, field] of Object.entries(directive.args)) {
    const mappedParam = directive.params[param] || param;
    let value = field;

    // Handle special case for Pattern.regexp
    if (directive.annotation === 'Pattern' && param === 'regexp') {
      value = `"${value}"`;
    } else if (param === 'message') {
      value = `"${value}"`;
    } else {
      // Numeric values don't need quotes
      value = isNaN(value) ? `"${value}"` : value;
    }

    args.push(`${mappedParam} = ${value}`);
  }

  const annotationStr = args.length > 0
    ? `@field:${directive.annotation}(${args.join(', ')})`
    : `@field:${directive.annotation}`;

  return annotationStr;
}

/**
 * Enhance generated Kotlin code with validation annotations
 */
function enhanceKotlinCode(originalCode, inputTypes) {
  let enhancedCode = originalCode;

  // Add Jakarta Validation imports if needed
  if (inputTypes.size > 0) {
    const imports = new Set(['import jakarta.validation.constraints.*']);

    const packageRegex = /(package\s+[^\n]+)/;
    const match = enhancedCode.match(packageRegex);
    if (match) {
      enhancedCode = enhancedCode.replace(packageRegex,
        `${match[1]}\n\n${Array.from(imports).join('\n')}`);
    }
  }

  // Process each data class
  inputTypes.forEach((fields, typeName) => {
    // Find the data class definition - more robust regex
    const dataClassRegex = new RegExp(
      `data class ${typeName}\\s*\\(([^)]*?)\\)\\s*\\{([^}]*constructor\\([^}]*\\)[^}]*?)\\}`,
      's'
    );

    const match = enhancedCode.match(dataClassRegex);

    if (match) {
      const constructorParams = match[1];
      const constructorBody = match[2];

      // Split constructor parameters by lines
      const paramLines = constructorParams.split('\n').map(line => line.trim()).filter(line => line);
      const enhancedParamLines = [];

      for (const line of paramLines) {
        // Match val/var declaration
        const valMatch = line.match(/^(val|var)\s+(\w+)\s*:\s*([^,\n]+(?:\s*\?\s*)?[^,\n]*)(?:\s*=\s*([^,\n]*))?/);

        if (valMatch) {
          const keyword = valMatch[1];
          const fieldName = valMatch[2];
          const fieldType = valMatch[3];
          const defaultValue = valMatch[4] || '';

          // Find directives for this field
          const fieldDirectives = fields.find(f => f.name === fieldName)?.directives || [];

          // Add validation annotations before the val/var declaration
          if (fieldDirectives.length > 0) {
            fieldDirectives.forEach(directive => {
              enhancedParamLines.push(generateKotlinAnnotation(directive));
            });
          }

          // Reconstruct the line
          const reconstructedLine = `${keyword} ${fieldName}: ${fieldType}${defaultValue ? ' = ' + defaultValue : ''}`;
          enhancedParamLines.push(reconstructedLine);
        } else {
          // Keep the line as-is if it doesn't match
          enhancedParamLines.push(line);
        }
      }

      const enhancedConstructorParams = enhancedParamLines.join(',\n');
      enhancedCode = enhancedCode.replace(dataClassRegex,
        `data class ${typeName}(\n${enhancedConstructorParams}\n) {\n${constructorBody}\n}`);
    }
  });

  return enhancedCode;
}

/**
 * Main function
 */
function main() {
  console.log('🚀 Running Enhanced Kotlin Codegen V2 with Jakarta Validation...');

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
    execSync('npx graphql-codegen --config graphql-codegen.yml', { stdio: 'inherit' });

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
    console.error(error.stack);
    process.exit(1);
  }
}

// Run the script
if (require.main === module) {
  main();
}

module.exports = { main, parseGraphQLSchema, enhanceKotlinCode };
import { CodegenPlugin } from '@graphql-codegen/typescript';
import { KotlinWithValidationVisitor, VALIDATION_DIRECTIVE_MAPPING } from './kotlin-with-validation-visitor.js';
import { GraphQLSchema, parse, visit, Kind } from 'graphql';
import { BaseDocumentsVisitor, BaseVisitor, getConfigValue, getPossibleTypes, LoadedFragment, normalizePath, } from '@graphql-codegen/visitor-plugin-common';
import extname from 'path/extname';

export interface KotlinWithValidationPluginConfig {
  /**
   * Target package name for generated Kotlin classes
   */
  package: string;

  /**
   * List type implementation (default: 'List')
   */
  listType?: string;

  /**
   * Enum values mapping
   */
  enumValues?: { [enumName: string]: string | { [enumValue: string]: string } };

  /**
   * Whether to generate types along with resolvers
   */
  withTypes?: boolean;

  /**
   * Whether to omit @JvmStatic annotations
   */
  omitJvmStatic?: boolean;

  /**
   * Whether to generate Jakarta Validation annotations from GraphQL directives
   */
  generateValidationAnnotations?: boolean;

  /**
   * Custom validation package name
   */
  validationPackage?: string;

  /**
   * Custom scalars mapping
   */
  scalars?: { [name: string]: string };

  /**
   * Naming convention for types
   */
  namingConvention?: string;
}

const plugin: CodegenPlugin<KotlinWithValidationPluginConfig> = {
  plugin: (schema: GraphQLSchema, documents: any[], config: KotlinWithValidationPluginConfig) => {
    const visitor = new KotlinWithValidationVisitor(config, schema, config.package);

    const imports = new Set<string>();

    // Add Jakarta Validation imports if enabled
    if (config.generateValidationAnnotations !== false) {
      imports.add('jakarta.validation.constraints.*');
    }

    const processedFiles = new Set<string>();

    const result = {
      prepend: [
        `// Generated with Jakarta Validation annotations support`,
        `package ${config.package}`,
        imports.size > 0 ? `import ${Array.from(imports).join('\nimport ')}` : '',
        '',
      ].filter(Boolean),
      content: documents.map(document => {
        const filePath = document.location;
        if (processedFiles.has(filePath)) {
          return '';
        }
        processedFiles.add(filePath);

        const definitions = document.definitions;

        // Process each definition using our visitor
        const processedDefinitions = definitions.map((def: any) => {
          // Handle different GraphQL definition types
          switch (def.kind) {
            case Kind.INPUT_OBJECT_TYPE_DEFINITION:
              return visitor.InputObjectTypeDefinition(def);
            case Kind.OBJECT_TYPE_DEFINITION:
              return visitor.ObjectTypeDefinition(def);
            case Kind.ENUM_TYPE_DEFINITION:
              return visitor.EnumTypeDefinition(def);
            default:
              return '';
          }
        }).filter(Boolean);

        return processedDefinitions.join('\n\n');
      }).join('\n\n'),
    };

    return result;
  },

  addToSchema: (schema: string) => {
    // Add validation directives to the schema if they don't exist
    if (!schema.includes('@notBlank')) {
      return `
# Jakarta Validation Directives
directive @notBlank(message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @size(min: Int, max: Int, message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @email(message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @pattern(regexp: String!, message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @range(min: Float, max: Float, message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @positive(message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @future(message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @past(message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @min(value: Float!, message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @max(value: Float!, message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION
directive @notNull(message: String) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION

${schema}`;
    }
    return schema;
  },
};

export default plugin;

// Export the visitor for potential reuse
export { KotlinWithValidationVisitor, VALIDATION_DIRECTIVE_MAPPING };
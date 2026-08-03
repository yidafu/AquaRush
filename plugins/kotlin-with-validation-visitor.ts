import {
  EnumTypeDefinitionNode,
  EnumValueDefinitionNode,
  FieldDefinitionNode,
  GraphQLSchema,
  InputObjectTypeDefinitionNode,
  InputValueDefinitionNode,
  isEnumType,
  isInputObjectType,
  isObjectType,
  isScalarType,
  Kind,
  ObjectTypeDefinitionNode,
  TypeNode,
  ValueNode,
  DirectiveNode,
} from 'graphql';
import { wrapTypeWithModifiers } from '@graphql-codegen/java-common';
import {
  BaseVisitor,
  buildScalarsFromConfig,
  EnumValuesMap,
  getBaseTypeNode,
  indent,
  indentMultiline,
  ParsedConfig,
  transformComment,
} from '@graphql-codegen/visitor-plugin-common';

export const KOTLIN_SCALARS = {
  ID: 'Any',
  String: 'String',
  Boolean: 'Boolean',
  Int: 'Int',
  Float: 'Float',
};

export interface KotlinWithValidationParsedConfig extends ParsedConfig {
  package: string;
  listType: string;
  enumValues: EnumValuesMap;
  withTypes: boolean;
  omitJvmStatic: boolean;
  generateValidationAnnotations: boolean;
  validationPackage: string;
}

export interface FieldDefinitionReturnType {
  inputTransformer?: ((typeName: string) => string) | FieldDefinitionNode;
  node: FieldDefinitionNode;
}

// Jakarta Validation directive mappings
export const VALIDATION_DIRECTIVE_MAPPING: Record<string, string> = {
  '@notBlank': 'jakarta.validation.constraints.NotBlank',
  '@size': 'jakarta.validation.constraints.Size',
  '@email': 'jakarta.validation.constraints.Email',
  '@pattern': 'jakarta.validation.constraints.Pattern',
  '@range': 'jakarta.validation.constraints.Range',
  '@positive': 'jakarta.validation.constraints.Positive',
  '@future': 'jakarta.validation.constraints.Future',
  '@past': 'jakarta.validation.constraints.Past',
  '@min': 'jakarta.validation.constraints.Min',
  '@max': 'jakarta.validation.constraints.Max',
  '@notNull': 'jakarta.validation.constraints.NotNull',
  '@null': 'jakarta.validation.constraints.Null',
  '@assertTrue': 'jakarta.validation.constraints.AssertTrue',
  '@assertFalse': 'jakarta.validation.constraints.AssertFalse',
  '@negative': 'jakarta.validation.constraints.Negative',
  '@negativeOrZero': 'jakarta.validation.constraints.NegativeOrZero',
  '@positiveOrZero': 'jakarta.validation.constraints.PositiveOrZero',
  '@decimalMin': 'jakarta.validation.constraints.DecimalMin',
  '@decimalMax': 'jakarta.validation.constraints.DecimalMax',
  '@digits': 'jakarta.validation.constraints.Digits',
};

export class KotlinWithValidationVisitor extends BaseVisitor<
  any,
  KotlinWithValidationParsedConfig
> {
  constructor(
    rawConfig: any,
    private _schema: GraphQLSchema,
    defaultPackageName: string,
  ) {
    super(rawConfig, {
      enumValues: rawConfig.enumValues || {},
      listType: rawConfig.listType || 'Iterable',
      withTypes: rawConfig.withTypes || false,
      package: rawConfig.package || defaultPackageName,
      scalars: buildScalarsFromConfig(_schema, rawConfig, KOTLIN_SCALARS),
      omitJvmStatic: rawConfig.omitJvmStatic || false,
      generateValidationAnnotations: rawConfig.generateValidationAnnotations !== false,
      validationPackage: rawConfig.validationPackage || 'jakarta.validation.constraints',
    });
  }

  public getPackageName(): string {
    return `package ${this.config.package}\n`;
  }

  /**
   * Generate Jakarta Validation import statements
   */
  private generateValidationImports(): string {
    if (!this.config.generateValidationAnnotations) {
      return '';
    }

    const imports = new Set<string>();

    // Collect all used validation annotations from the schema
    Object.values(VALIDATION_DIRECTIVE_MAPPING).forEach(annotation => {
      if (annotation.startsWith('jakarta.validation.constraints')) {
        imports.add('jakarta.validation.constraints.*');
      }
    });

    if (imports.size > 0) {
      return Array.from(imports).map(imp => `import ${imp}`).join('\n') + '\n';
    }

    return '';
  }

  /**
   * Parse validation directives and generate annotation strings
   */
  private parseValidationDirectives(directives: readonly DirectiveNode[]): string[] {
    if (!this.config.generateValidationAnnotations || !directives || directives.length === 0) {
      return [];
    }

    const annotations: string[] = [];

    for (const directive of directives) {
      const directiveName = `@${directive.name.value}`;
      const annotationFullName = VALIDATION_DIRECTIVE_MAPPING[directiveName];

      if (!annotationFullName) {
        continue; // Skip unknown directives
      }

      const annotationName = annotationFullName.split('.').pop()!;
      const args = directive.arguments || [];

      if (args.length === 0) {
        annotations.push(`@field:${annotationName}`);
        continue;
      }

      // Process directive arguments
      const processedArgs = args.map(arg => {
        const argName = arg.name.value;
        const valueNode = arg.value;

        // Format the value based on its type
        let formattedValue: string;
        switch (valueNode.kind) {
          case 'StringValue':
            formattedValue = `"${valueNode.value}"`;
            break;
          case 'IntValue':
          case 'FloatValue':
          case 'BooleanValue':
            formattedValue = valueNode.value.toString();
            break;
          case 'EnumValue':
            formattedValue = valueNode.value;
            break;
          default:
            formattedValue = `"${valueNode.value}"`;
        }

        // Map GraphQL argument names to Jakarta Validation parameter names
        const paramNameMap: Record<string, string> = {
          'value': 'value',
          'min': 'min',
          'max': 'max',
          'message': 'message',
          'regexp': 'regexp',
        };

        const paramName = paramNameMap[argName] || argName;
        return `${paramName} = ${formattedValue}`;
      });

      annotations.push(`@field:${annotationName}(${processedArgs.join(', ')})`);
    }

    return annotations;
  }

  protected getEnumValue(enumName: string, enumOption: string): string {
    if (
      this.config.enumValues[enumName] &&
      typeof this.config.enumValues[enumName] === 'object' &&
      this.config.enumValues[enumName][enumOption]
    ) {
      return this.config.enumValues[enumName][enumOption];
    }

    return enumOption;
  }

  EnumValueDefinition(node: EnumValueDefinitionNode): (enumName: string) => string {
    return (enumName: string) => {
      return indent(
        `${this.convertName(node, {
          useTypesPrefix: false,
          useTypesSuffix: false,
          transformUnderscore: true,
        })}("${this.getEnumValue(enumName, node.name.value)}")`,
      );
    };
  }

  EnumTypeDefinition(node: EnumTypeDefinitionNode): string {
    const comment = transformComment(node.description, 0);
    const enumName = this.convertName(node.name);
    const enumValues = indentMultiline(
      node.values.map(enumValue => (enumValue as any)(node.name.value)).join(',\n') + ';',
      2,
    );

    return `${comment}enum class ${enumName}(val label: String) {
${enumValues}

  companion object {
    ${this.config.omitJvmStatic ? '' : '@JvmStatic'}
    fun valueOfLabel(label: String): ${enumName}? {
      return values().find { it.label == label }
    }
  }
}`;
  }

  protected resolveInputFieldType(typeNode: TypeNode): {
    baseType: string;
    typeName: string;
    isScalar: boolean;
    isArray: boolean;
    nullable: boolean;
  } {
    const innerType = getBaseTypeNode(typeNode);
    const schemaType = this._schema.getType(innerType.name.value);
    const isArray =
      typeNode.kind === Kind.LIST_TYPE ||
      (typeNode.kind === Kind.NON_NULL_TYPE && typeNode.type.kind === Kind.LIST_TYPE);
    let result: {
      baseType: string;
      typeName: string;
      isScalar: boolean;
      isArray: boolean;
      nullable: boolean;
    } = null;
    const nullable = typeNode.kind !== Kind.NON_NULL_TYPE;

    if (isScalarType(schemaType)) {
      if (this.config.scalars[schemaType.name]) {
        result = {
          baseType: this.scalars[schemaType.name],
          typeName: this.scalars[schemaType.name],
          isScalar: true,
          isArray,
          nullable,
        };
      } else {
        result = { isArray, baseType: 'Any', typeName: 'Any', isScalar: true, nullable };
      }
    } else if (isInputObjectType(schemaType)) {
      const convertedName = this.convertName(schemaType.name);
      const typeName = convertedName.endsWith('Input') ? convertedName : `${convertedName}Input`;
      result = {
        baseType: typeName,
        typeName,
        isScalar: false,
        isArray,
        nullable,
      };
    } else if (isEnumType(schemaType) || isObjectType(schemaType)) {
      result = {
        isArray,
        baseType: this.convertName(schemaType.name),
        typeName: this.convertName(schemaType.name),
        isScalar: true,
        nullable,
      };
    } else {
      result = { isArray, baseType: 'Any', typeName: 'Any', isScalar: true, nullable };
    }

    if (result) {
      result.typeName = wrapTypeWithModifiers(result.typeName, typeNode, this.config.listType);
    }

    return result;
  }

  /**
   * Enhanced input transformer with validation annotations
   */
  protected buildInputTransfomer(
    name: string,
    inputValueArray: ReadonlyArray<InputValueDefinitionNode>,
  ): string {
    const hasValidationAnnotations = this.config.generateValidationAnnotations &&
      inputValueArray.some(field => field.directives && field.directives.length > 0);

    const classMembers = inputValueArray
      .map(arg => {
        const typeToUse = this.resolveInputFieldType(arg.type);
        const initialValue = this.initialValue(typeToUse.typeName, arg.defaultValue);
        const initial = initialValue ? ` = ${initialValue}` : typeToUse.nullable ? ' = null' : '';

        // Generate validation annotations
        const validationAnnotations = this.parseValidationDirectives(arg.directives || []);
        const annotationLines = validationAnnotations.map(ann => indent(ann, 2)).join('\n');

        const fieldDeclaration = indent(
          `val ${arg.name.value}: ${typeToUse.typeName}${typeToUse.nullable ? '?' : ''}${initial}`,
          2,
        );

        return annotationLines ? `${annotationLines}\n${fieldDeclaration}` : fieldDeclaration;
      })
      .join(',\n');

    let suppress = '';
    const ctorSet = inputValueArray
      .map(arg => {
        const typeToUse = this.resolveInputFieldType(arg.type);
        const initialValue = this.initialValue(typeToUse.typeName, arg.defaultValue);
        const fallback = initialValue ? ` ?: ${initialValue}` : '';

        if (typeToUse.isArray && !typeToUse.isScalar) {
          suppress = '@Suppress("UNCHECKED_CAST")\n  ';
          return indent(
            `args["${arg.name.value}"]${typeToUse.nullable || fallback ? '?' : '!!'}.let { ${
              arg.name.value
            } -> (${arg.name.value} as List<Map<String, Any>>).map { ${
              typeToUse.baseType
            }(it) } }${fallback}`,
            3,
          );
        }
        if (typeToUse.isScalar) {
          return indent(
            `args["${arg.name.value}"] as ${typeToUse.typeName}${
              typeToUse.nullable || fallback ? '?' : ''
            }${fallback}`,
            3,
          );
        }
        if (typeToUse.nullable || fallback) {
          suppress = '@Suppress("UNCHECKED_CAST")\n  ';
          return indent(
            `args["${arg.name.value}"]?.let { ${typeToUse.typeName}(it as Map<String, Any>) }${fallback}`,
            3,
          );
        }
        suppress = '@Suppress("UNCHECKED_CAST")\n  ';
        return indent(`${typeToUse.typeName}(args["${arg.name.value}"] as Map<String, Any>)`, 3);
      })
      .join(',\n');

    // Generate class header with imports if needed
    const validationImports = hasValidationAnnotations ? this.generateValidationImports() : '';
    const classHeader = validationImports ? `// Jakarta Validation imports\n${validationImports}\n` : '';

    // language=kotlin
    return `${classHeader}data class ${name}(
${classMembers}
) {
  ${suppress}constructor(args: Map<String, Any>) : this(
${ctorSet}
  )
}`;
  }

  protected buildTypeTransfomer(
    name: string,
    typeValueArray: ReadonlyArray<FieldDefinitionNode>,
  ): string {
    const classMembers = typeValueArray
      .map(arg => {
        if (!arg.type) {
          return '';
        }
        const typeToUse = this.resolveInputFieldType(arg.type);

        return indent(
          `val ${arg.name.value}: ${typeToUse.typeName}${typeToUse.nullable ? '?' : ''}`,
          2,
        );
      })
      .join(',\n');

    // language=kotlin
    return `data class ${name}(
${classMembers}
)`;
  }

  protected initialValue(typeName: string, defaultValue?: ValueNode): string | undefined {
    if (defaultValue) {
      if (
        defaultValue.kind === 'IntValue' ||
        defaultValue.kind === 'FloatValue' ||
        defaultValue.kind === 'BooleanValue'
      ) {
        return `${defaultValue.value}`;
      }
      if (defaultValue.kind === 'StringValue') {
        return `"""${defaultValue.value}""".trimIndent()`;
      }
      if (defaultValue.kind === 'EnumValue') {
        return `${typeName}.${defaultValue.value}`;
      }
      if (defaultValue.kind === 'ListValue') {
        const list = defaultValue.values
          .map(value => {
            return this.initialValue(typeName, value);
          })
          .join(', ');
        return `listOf(${list})`;
      }
    }

    return undefined;
  }

  FieldDefinition(node: FieldDefinitionNode): FieldDefinitionReturnType {
    if (node.arguments.length > 0) {
      const inputTransformer = (typeName: string) => {
        const transformerName = `${this.convertName(typeName, {
          useTypesPrefix: true,
        })}${this.convertName(node.name.value, { useTypesPrefix: false })}Args`;

        return this.buildInputTransfomer(transformerName, node.arguments);
      };

      return { node, inputTransformer };
    }
    return { node };
  }

  InputObjectTypeDefinition(node: InputObjectTypeDefinitionNode): string {
    const convertedName = this.convertName(node);
    const name = convertedName.endsWith('Input') ? convertedName : `${convertedName}Input`;

    return this.buildInputTransfomer(name, node.fields);
  }

  ObjectTypeDefinition(node: ObjectTypeDefinitionNode): string {
    const name = this.convertName(node);
    const fields = node.fields as unknown as FieldDefinitionReturnType[];

    const fieldNodes = [];
    const argsTypes = [];
    fields.forEach(({ node, inputTransformer }) => {
      if (node) {
        fieldNodes.push(node);
      }
      if (inputTransformer) {
        argsTypes.push(inputTransformer);
      }
    });

    let types = argsTypes.map(f => (f as any)(node.name.value)).filter(r => r);
    if (this.config.withTypes) {
      types = types.concat([this.buildTypeTransfomer(name, fieldNodes)]);
    }
    return types.join('\n');
  }
}
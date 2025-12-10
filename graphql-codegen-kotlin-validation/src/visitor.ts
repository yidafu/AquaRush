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
import { KotlinResolversPluginRawConfig } from './config.js';
import {
  VALIDATION_DIRECTIVES,
  parseDirectiveArgs,
} from './directive-mapping.js';

export const KOTLIN_SCALARS = {
  ID: { input: 'Any', output: 'Any' },
  String: { input: 'String', output: 'String' },
  Boolean: { input: 'Boolean', output: 'Boolean' },
  Int: { input: 'Int', output: 'Int' },
  Float: { input: 'Float', output: 'Float' },
  // ID: 'Any',
  // String: 'String',
  // Boolean: 'Boolean',
  // Int: 'Int',
  // Float: 'Float',
};

export interface KotlinResolverParsedConfig extends ParsedConfig {
  package: string;
  listType: string;
  enumValues: EnumValuesMap;
  withTypes: boolean;
  omitJvmStatic: boolean;
}

export interface FieldDefinitionReturnType {
  inputTransformer?: ((typeName: string) => string) | FieldDefinitionNode;
  node: FieldDefinitionNode;
}

export class KotlinResolversVisitor extends BaseVisitor<
  KotlinResolversPluginRawConfig,
  KotlinResolverParsedConfig
> {
  constructor(
    rawConfig: KotlinResolversPluginRawConfig,
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
    });
  }

  public getPackageName(): string {
    return `package ${this.config.package}\n`;
  }

  /**
   * Check if a type name is a scalar by checking known scalar names
   * This helps avoid instance comparison issues when scalars are defined in multiple schema files
   */
  private isScalarByName(typeName: string): boolean {
    const knownScalars = [
      // Built-in scalars
      'String', 'Int', 'Float', 'Boolean', 'ID',
      // Custom scalars used in the project
      'Long', 'BigDecimal', 'LocalDateTime', 'Map'
    ];
    return knownScalars.includes(typeName) || typeName in this.config.scalars;
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
        const scalarInfo = this.scalars[schemaType.name];
        // Handle both string and object formats for scalars
        const scalarType = typeof scalarInfo === 'string' ? scalarInfo : scalarInfo.input || 'Any';
        result = {
          baseType: scalarType,
          typeName: scalarType,
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
   * 提取字段的验证注解
   */
  private extractValidationAnnotations(field: InputValueDefinitionNode): string[] {
    if (!field.directives || field.directives.length === 0) {
      return [];
    }

    const annotations: string[] = [];

    for (const directive of field.directives) {
      const directiveName = `@${directive.name.value}`;

      
      // 检查是否为其他验证指令
      if (VALIDATION_DIRECTIVES[directiveName]) {
        const annotationName = VALIDATION_DIRECTIVES[directiveName];

        // 解析指令参数
        let annotationParams = '';
        if (directive.arguments && directive.arguments.length > 0) {
          const params = parseDirectiveArgs(directiveName, Array.from(directive.arguments));
          annotationParams = `(${params.join(', ')})`;
        }

        annotations.push(`${annotationName}${annotationParams}`);
      }
    }

    return annotations;
  }

  /**
   * 格式化验证注解
   */
  private formatValidationAnnotations(annotations: string[]): string[] {
    // 所有验证注解都需要 @field: 前缀，因为它们是字段注解而不是类注解
    const prefix = '@field:';
    return annotations.map(annotation => `${prefix}${annotation}`);
  }

  /**
   * 为字段添加验证注解
   */
  private addValidationAnnotations(
    field: InputValueDefinitionNode,
    _typeInfo: { nullable: boolean }
  ): string[] {
    const annotations = this.extractValidationAnnotations(field);

    if (annotations.length === 0) {
      return [];
    }

    return this.formatValidationAnnotations(annotations);
  }

  /**
   * 提取对象类型字段的验证注解
   */
  private extractValidationAnnotationsForField(field: FieldDefinitionNode): string[] {
    if (!field.directives || field.directives.length === 0) {
      return [];
    }

    const annotations: string[] = [];

    for (const directive of field.directives) {
      const directiveName = `@${directive.name.value}`;

      
      // 检查是否为其他验证指令
      if (VALIDATION_DIRECTIVES[directiveName]) {
        const annotationName = VALIDATION_DIRECTIVES[directiveName];

        // 解析指令参数
        let annotationParams = '';
        if (directive.arguments && directive.arguments.length > 0) {
          const params = parseDirectiveArgs(directiveName, Array.from(directive.arguments));
          annotationParams = `(${params.join(', ')})`;
        }

        annotations.push(`${annotationName}${annotationParams}`);
      }
    }

    return annotations;
  }

  /**
   * 为对象类型字段添加验证注解到构造函数参数
   */
  private addValidationAnnotationsForField(
    field: FieldDefinitionNode,
    typeInfo: { nullable: boolean }
  ): string[] {
    const annotations = this.extractValidationAnnotationsForField(field);

    if (annotations.length === 0) {
      return [];
    }

    // 对于对象类型字段，注解直接添加到构造函数参数上
    return annotations;
  }

  protected buildInputTransfomer(
    name: string,
    inputValueArray: ReadonlyArray<InputValueDefinitionNode>,
  ): string {
    const classMembers = (inputValueArray || [])
      .map(arg => {
        const typeToUse = this.resolveInputFieldType(arg.type);
        const initialValue = this.initialValue(typeToUse.typeName, arg.defaultValue);
        const initial = initialValue ? ` = ${initialValue}` : typeToUse.nullable ? ' = null' : '';

        // 获取验证注解
        const validationAnnotations = this.addValidationAnnotations(arg, typeToUse);

        // 构建字段声明，包括注解
        let fieldDeclaration = '';
        if (validationAnnotations.length > 0) {
          // 添加验证注解
          fieldDeclaration += validationAnnotations.map(ann => indent(ann, 2)).join('\n') + '\n';
        }
        // 添加字段声明
        fieldDeclaration += indent(
          `val ${arg.name.value}: ${typeToUse.typeName}${typeToUse.nullable ? '?' : ''}${initial}`,
          2,
        );

        return fieldDeclaration;
      })
      .join(',\n');
    let suppress = '';
    const ctorSet = (inputValueArray || [])
      .map(arg => {
        const typeToUse = this.resolveInputFieldType(arg.type);
        const initialValue = this.initialValue(typeToUse.typeName, arg.defaultValue);
        const fallback = initialValue ? ` ?: ${initialValue}` : '';

        if (typeToUse.isArray && !typeToUse.isScalar) {
          suppress = '@Suppress("UNCHECKED_CAST")\n  ';
          return indent(
            `args["${arg.name.value}"]${typeToUse.nullable || fallback ? '?' : '!!'}.let { ${arg.name.value
            } -> (${arg.name.value} as List<Map<String, Any>>).map { ${typeToUse.baseType
            }(it) } }${fallback}`,
            3,
          );
        }
        if (typeToUse.isScalar) {
          return indent(
            `args["${arg.name.value}"] as ${typeToUse.typeName}${typeToUse.nullable || fallback ? '?' : ''
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

    // language=kotlin
    return `data class ${name}(
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
    const classMembers = (typeValueArray || [])
      .map(arg => {
        if (!arg.type) {
          return '';
        }
        const typeToUse = this.resolveInputFieldType(arg.type);

        // 获取验证注解
        const validationAnnotations = this.addValidationAnnotationsForField(arg, typeToUse);

        // 构建字段声明，包括注解
        let fieldDeclaration = '';
        if (validationAnnotations.length > 0) {
          // 添加验证注解
          fieldDeclaration += validationAnnotations.map(ann => indent(ann, 2)).join('\n') + '\n';
        }

        // 添加字段声明
        fieldDeclaration += indent(
          `val ${arg.name.value}: ${typeToUse.typeName}${typeToUse.nullable ? '?' : ''}`,
          2,
        );

        return fieldDeclaration;
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
      // Variable
      // ObjectValue
      // ObjectField
    }

    return undefined;
  }

  FieldDefinition(node: FieldDefinitionNode): FieldDefinitionReturnType {
    if (node.arguments.length > 0) {
      const inputTransformer = (typeName: string) => {
        const transformerName = `${this.convertName(typeName, {
          useTypesPrefix: true,
        })}${this.convertName(node.name.value, { useTypesPrefix: false })}Args`;

        // Pass arguments with directives to buildInputTransformer
        return this.buildInputTransfomer(transformerName, node.arguments);
      };

      // Preserve field directives for type transformer
      const fieldWithDirectives = {
        ...node,
        directives: node.directives || []
      };

      return { node: fieldWithDirectives, inputTransformer };
    }

    // Preserve field directives even when there are no arguments
    const fieldWithDirectives = {
      ...node,
      directives: node.directives || []
    };

    return { node: fieldWithDirectives };
  }

  InputObjectTypeDefinition(node: InputObjectTypeDefinitionNode): string {
    const convertedName = this.convertName(node);
    const name = convertedName.endsWith('Input') ? convertedName : `${convertedName}Input`;

    return this.buildInputTransfomer(name, node.fields);
  }

  ObjectTypeDefinition(node: ObjectTypeDefinitionNode): string {
    const name = this.convertName(node);
    const fields = (node.fields as unknown as FieldDefinitionReturnType[]) || [];

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

    let types = (argsTypes || []).map(f => (f as any)(node.name.value)).filter(r => r);
    if (this.config.withTypes) {
      types = types.concat([this.buildTypeTransfomer(name, fieldNodes)]);
    }

    return types.join('\n');
  }
}

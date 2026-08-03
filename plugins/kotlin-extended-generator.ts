import { CodegenPlugin } from '@graphql-codegen/typescript';
import {
  visit,
  GraphQLSchema,
  Kind,
  InputObjectTypeDefinitionNode,
  InputValueDefinitionNode,
  DirectiveNode,
  DocumentNode
} from 'graphql';
import { KotlinVisitor } from '@graphql-codegen/kotlin';

/**
 * 扩展的 Kotlin 生成器插件
 * 自动将 GraphQL directives 转换为 Jakarta Validation 注解
 */
export interface KotlinExtendedConfig {
  /**
   * Kotlin 包名
   */
  package?: string;
  /**
   * 是否生成 Jakarta Validation 注解
   */
  generateValidations?: boolean;
  /**
   * 验证注解包名
   */
  validationPackage?: string;
  /**
   * 注解应用位置：'field' | 'parameter' | 'both'
   */
  annotationTarget?: 'field' | 'parameter' | 'both';
}

const plugin: CodegenPlugin<KotlinExtendedConfig> = {
  plugin: (schema: GraphQLSchema, documents: DocumentNode[], config: KotlinExtendedConfig, info) => {
    const {
      package: pkgName = 'generated.types',
      generateValidations = true,
      validationPackage = 'jakarta.validation.constraints',
      annotationTarget = 'field'
    } = config;

    const processedTypes = new Set<string>();

    return {
      prepend: [
        `// Generated with validation annotations`,
        `package ${pkgName}`,
        generateValidations ? `import ${validationPackage}.*` : '',
        '',
        `// Data classes with validation annotations`,
      ].filter(Boolean),
      content: documents.map(doc => {
        const visitor = new ValidationAwareKotlinVisitor(schema, config, info);
        return visit(doc, {
          [Kind.INPUT_OBJECT_TYPE_DEFINITION]: (node: InputObjectTypeDefinitionNode) => {
            if (processedTypes.has(node.name.value)) {
              return null;
            }
            processedTypes.add(node.name.value);

            return visitor.generateDataClassWithValidation(node);
          },
        });
      }).join('\n\n'),
    };
  },
};

/**
 * 验证感知的 Kotlin 访问器
 */
class ValidationAwareKotlinVisitor {
  private directiveToAnnotationMap: Record<string, string>;

  constructor(
    private schema: GraphQLSchema,
    private config: KotlinExtendedConfig,
    private info: any
  ) {
    this.directiveToAnnotationMap = this.createDirectiveMapping();
  }

  /**
   * 创建指令到注解的映射
   */
  private createDirectiveMapping(): Record<string, string> {
    return {
      '@notBlank': 'NotBlank',
      '@size': 'Size',
      '@email': 'Email',
      '@pattern': 'Pattern',
      '@range': 'Range',
      '@positive': 'Positive',
      '@future': 'Future',
      '@past': 'Past',
      '@min': 'Min',
      '@max': 'Max',
      '@notNull': 'NotNull',
      '@null': 'Null',
      '@assertTrue': 'AssertTrue',
      '@assertFalse': 'AssertFalse',
      '@negative': 'Negative',
      '@negativeOrZero': 'NegativeOrZero',
      '@positiveOrZero': 'PositiveOrZero',
      '@decimalMin': 'DecimalMin',
      '@decimalMax': 'DecimalMax',
      '@digits': 'Digits',
    };
  }

  /**
   * 生成带有验证注解的数据类
   */
  generateDataClassWithValidation(node: InputObjectTypeDefinitionNode): string {
    const className = node.name.value;
    const fields = node.fields || [];

    const fieldDefinitions = fields.map(field => this.generateFieldWithValidation(field));

    const constructorFields = fields.map(field =>
      `  ${field.name.value}: ${this.mapGraphqlTypeToKotlin(field)}${this.getDefaultValue(field)}`
    );

    return `data class ${className}(
${constructorFields.join(',\n')}) {
  constructor(args: Map<String, Any>) : this(
${this.generateConstructorMapping(fields)}
  )
}

${this.generateValidationExtension(className, fields)}`;
  }

  /**
   * 生成字段定义，包含验证注解
   */
  private generateFieldWithValidation(field: InputValueDefinitionNode): string {
    const fieldName = field.name.value;
    const fieldType = this.mapGraphqlTypeToKotlin(field);
    const defaultValue = this.getDefaultValue(field);
    const annotations = this.generateValidationAnnotations(field);

    const annotationPrefix = this.config.annotationTarget === 'field' ? '@field:' : '';

    return annotations.length > 0
      ? `  ${annotationPrefix}${annotations.join('\n  ' + annotationPrefix)}`
      : '';
  }

  /**
   * 生成验证注解
   */
  private generateValidationAnnotations(field: InputValueDefinitionNode): string[] {
    if (!field.directives) return [];

    return field.directives
      .map(directive => this.mapDirectiveToAnnotation(directive))
      .filter(Boolean) as string[];
  }

  /**
   * 将 GraphQL 指令映射到 Jakarta Validation 注解
   */
  private mapDirectiveToAnnotation(directive: DirectiveNode): string | null {
    const directiveName = `@${directive.name.value}`;
    const annotationName = this.directiveToAnnotationMap[directiveName];

    if (!annotationName) return null;

    const args = directive.arguments || [];
    if (args.length === 0) {
      return annotationName;
    }

    const mappedArgs = args.map(arg => {
      const argName = arg.name.value;
      const value = this.formatArgumentValue(arg.value);

      // 参数名映射
      const paramNameMap: Record<string, string> = {
        'value': 'value',
        'min': 'min',
        'max': 'max',
        'message': 'message',
        'regexp': 'regexp',
      };

      const mappedArgName = paramNameMap[argName] || argName;
      return `${mappedArgName} = ${value}`;
    });

    return `${annotationName}(${mappedArgs.join(', ')})`;
  }

  /**
   * 格式化参数值
   */
  private formatArgumentValue(valueNode: any): string {
    switch (valueNode.kind) {
      case Kind.STRING:
        return `"${valueNode.value}"`;
      case Kind.INT:
      case Kind.FLOAT:
        return valueNode.value;
      case Kind.BOOLEAN:
        return valueNode.value;
      case Kind.NULL:
        return 'null';
      default:
        return `"${valueNode.value}"`;
    }
  }

  /**
   * 映射 GraphQL 类型到 Kotlin 类型
   */
  private mapGraphqlTypeToKotlin(field: InputValueDefinitionNode): string {
    const type = field.type;
    let baseType: string;
    let isNullable = false;

    // 处理类型
    if (type.kind === Kind.NON_NULL_TYPE) {
      baseType = this.getBaseTypeName(type.type);
    } else {
      baseType = this.getBaseTypeName(type);
      isNullable = true;
    }

    // 处理列表类型
    if (type.kind === Kind.LIST_TYPE || (type.type && type.type.kind === Kind.LIST_TYPE)) {
      baseType = `List<${baseType}>`;
    }

    // GraphQL 标量类型映射
    const typeMapping: Record<string, string> = {
      'String': 'kotlin.String',
      'Int': 'kotlin.Int',
      'Float': 'kotlin.Double',
      'Boolean': 'kotlin.Boolean',
      'Long': 'kotlin.Long',
      'BigDecimal': 'java.math.BigDecimal',
      'LocalDateTime': 'java.time.LocalDateTime',
      'Map': 'Map<String, Any>',
    };

    const kotlinType = typeMapping[baseType] || baseType;
    return isNullable ? `${kotlinType}?` : kotlinType;
  }

  /**
   * 获取基础类型名
   */
  private getBaseTypeName(typeNode: any): string {
    if (typeNode.kind === Kind.NAMED_TYPE) {
      return typeNode.name.value;
    }
    if (typeNode.type) {
      return this.getBaseTypeName(typeNode.type);
    }
    return 'String';
  }

  /**
   * 获取默认值
   */
  private getDefaultValue(field: InputValueDefinitionNode): string {
    if (!field.defaultValue) return '';

    const value = this.formatArgumentValue(field.defaultValue);
    return ` = ${value}`;
  }

  /**
   * 生成构造函数映射
   */
  private generateConstructorMapping(fields: InputValueDefinitionNode[]): string {
    return fields.map(field => {
      const fieldName = field.name.value;
      const defaultValue = this.getDefaultValue(field);
      const castType = this.mapGraphqlTypeToKotlin(field).replace('?', '');

      return `      args["${fieldName}"] as ${castType}?${defaultValue}`;
    }).join(',\n');
  }

  /**
   * 生成验证扩展函数
   */
  private generateValidationExtension(className: string, fields: InputValueDefinitionNode[]): string {
    const validationFields = fields.filter(field => field.directives && field.directives.length > 0);

    if (validationFields.length === 0) return '';

    return `/**
 * Validation extension for ${className}
 */
fun ${className}.validate(): List<String> {
  val errors = mutableListOf<String>()

${validationFields.map(field => this.generateFieldValidation(field)).join('\n')}

  return errors
}

/**
 * Check if ${className} is valid
 */
fun ${className}.isValid(): Boolean = validate().isEmpty()`;
  }

  /**
   * 生成字段验证逻辑
   */
  private generateFieldValidation(field: InputValueDefinitionNode): string {
    const fieldName = field.name.value;
    let validationCode = `  // Validate ${fieldName}`;

    for (const directive of field.directives || []) {
      validationCode += '\n  ' + this.generateFieldValidationLogic(fieldName, directive);
    }

    return validationCode;
  }

  /**
   * 生成字段验证逻辑
   */
  private generateFieldValidationLogic(fieldName: string, directive: DirectiveNode): string {
    const directiveName = `@${directive.name.value}`;

    switch (directiveName) {
      case '@notBlank':
        return `if (${fieldName}.isNullOrBlank()) {
    errors.add("${fieldName} cannot be blank")
  }`;

      case '@size':
        const min = directive.arguments?.find(arg => arg.name.value === 'min')?.value;
        const max = directive.arguments?.find(arg => arg.name.value === 'max')?.value;
        const minCheck = min ? `${fieldName}.length < ${min.value}` : 'false';
        const maxCheck = max ? `${fieldName}.length > ${max.value}` : 'false';
        return `if (!${fieldName}.isNullOrEmpty() && (${minCheck} || ${maxCheck})) {
    errors.add("${fieldName} size must be between ${min?.value || 0} and ${max?.value || '∞'}")
  }`;

      case '@email':
        return `if (!${fieldName}.isNullOrEmpty() && !${fieldName}.contains("@")) {
    errors.add("${fieldName} must be a valid email")
  }`;

      case '@positive':
        return `if (${fieldName} != null && ${fieldName} <= 0) {
    errors.add("${fieldName} must be positive")
  }`;

      default:
        return `// TODO: Implement validation for ${directiveName}`;
    }
  }
}

export default plugin;
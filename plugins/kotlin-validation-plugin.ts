import { CodegenPlugin, TypescriptPluginConfig } from '@graphql-codegen/typescript';
import { visit, GraphQLSchema, Kind, OperationDefinitionNode, FieldNode, ArgumentNode, InputValueDefinitionNode, DirectiveNode } from 'graphql';
import { TypeScriptOperationVariablesToObject } from '@graphql-codegen/visitor-plugin-common';

/**
 * Kotlin Validation Plugin
 * 将 GraphQL 验证 directives 转换为 Jakarta Validation 注解
 */
export interface KotlinValidationPluginConfig extends TypescriptPluginConfig {
  /**
   * 是否生成 Jakarta Validation 注解
   */
  generateAnnotations?: boolean;
  /**
   * 注解包名
   */
  annotationPackage?: string;
  /**
   * Directive 到注解的映射
   */
  directiveMappings?: Record<string, string>;
}

const plugin: CodegenPlugin<KotlinValidationPluginConfig> = {
  plugin: (schema, documents, config, info) => {
    const { generateAnnotations = true, annotationPackage = 'jakarta.validation.constraints', directiveMappings = getDefaultDirectiveMappings() } = config;

    const visitor = new ValidationDirectiveVisitor(schema, config, info);
    const processedInputs = new Set<string>();

    return {
      prepend: [
        generateAnnotations ? `// Generated Jakarta Validation Annotations` : '',
        generateAnnotations ? `package ${config.package || 'generated.validation'}` : '',
      ],
      content: documents.map(document => {
        const content = visit(document.document, {
          [Kind.INPUT_OBJECT_TYPE_DEFINITION]: (node) => {
            if (processedInputs.has(node.name.value)) {
              return null;
            }
            processedInputs.add(node.name.value);

            return generateValidationClass(node, config);
          },
        });

        return content;
      }).filter(Boolean).join('\n\n'),
    };
  },
};

/**
 * 验证指令访问器
 */
class ValidationDirectiveVisitor {
  constructor(
    private schema: GraphQLSchema,
    private config: KotlinValidationPluginConfig,
    private info: any
  ) {}

  /**
   * 处理字段定义上的验证指令
   */
  processFieldValidation(field: InputValueDefinitionNode): string[] {
    const annotations: string[] = [];

    if (!field.directives) return annotations;

    for (const directive of field.directives) {
      const annotation = this.generateAnnotation(directive);
      if (annotation) {
        annotations.push(annotation);
      }
    }

    return annotations;
  }

  /**
   * 生成单个注解
   */
  private generateAnnotation(directive: DirectiveNode): string | null {
    const directiveName = directive.name.value;
    const mapping = this.config.directiveMappings?.[directiveName];

    if (!mapping) return null;

    const args = directive.arguments || [];
    const argStrings = args.map(arg => {
      const valueNode = arg.value;
      let value: string;

      switch (valueNode.kind) {
        case Kind.STRING:
          value = `"${valueNode.value}"`;
          break;
        case Kind.INT:
          value = valueNode.value;
          break;
        case Kind.FLOAT:
          value = valueNode.value;
          break;
        case Kind.BOOLEAN:
          value = valueNode.value;
          break;
        default:
          value = `"${valueNode.value}"`;
      }

      return `${arg.name.value} = ${value}`;
    });

    return `@${mapping}${argStrings.length > 0 ? `(${argStrings.join(', ')})` : ''}`;
  }
}

/**
 * 获取默认的 directive 到注解映射
 */
function getDefaultDirectiveMappings(): Record<string, string> {
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
 * 生成验证类代码
 */
function generateValidationClass(inputNode: any, config: KotlinValidationPluginConfig): string {
  const className = inputNode.name.value;
  const fields = inputNode.fields || [];

  const fieldValidations = fields.map((field: any) => {
    const fieldName = field.name.value;
    const visitor = new ValidationDirectiveVisitor({}, config, {});
    const annotations = visitor.processFieldValidation(field);

    if (annotations.length === 0) return null;

    return `  // ${fieldName} field validations\n${annotations.map(ann => `  ${ann}`).join('\n')}`;
  }).filter(Boolean);

  if (fieldValidations.length === 0) {
    return `// No validation directives found for ${className}`;
  }

  return `// Validation metadata for ${className}
${fieldValidations.join('\n\n')}`;
}

export default plugin;
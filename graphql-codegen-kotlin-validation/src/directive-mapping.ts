/**
 * GraphQL 验证指令到 Jakarta Validation 注解的映射
 */

export const VALIDATION_DIRECTIVES: Record<string, string> = {
  '@notBlank': 'NotBlank',
  '@size': 'Size',
  '@email': 'Email',
  '@pattern': 'Pattern',
  // @range is handled specially in the visitor - converted to @Min and @Max
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
  '@digits': 'Digits'
};

/**
 * 验证指令参数映射
 */
export const VALIDATION_PARAM_MAPPING: Record<string, Record<string, string>> = {
  '@size': {
    'min': 'min',
    'max': 'max',
    'message': 'message'
  },
  '@pattern': {
    'regexp': 'regexp',
    'message': 'message'
  },
  '@range': {
    'min': 'min',
    'max': 'max',
    'message': 'message'
  },
  '@min': {
    'value': 'value',
    'message': 'message'
  },
  '@max': {
    'value': 'value',
    'message': 'message'
  },
  '@decimalMin': {
    'value': 'value',
    'message': 'message'
  },
  '@decimalMax': {
    'value': 'value',
    'message': 'message'
  },
  '@digits': {
    'integer': 'integer',
    'fraction': 'fraction',
    'message': 'message'
  }
};

/**
 * 解析 GraphQL 指令参数
 */
export function parseDirectiveArgs(
  directiveName: string,
  args: any[]
): string[] {
  const paramMapping = VALIDATION_PARAM_MAPPING[directiveName] || {};
  return args.map(arg => {
    const argName = arg.name.value;
    const mappedArgName = paramMapping[argName] || argName;
    const value = formatArgValue(arg.value);
    return `${mappedArgName} = ${value}`;
  });
}

/**
 * 格式化参数值
 */
function formatArgValue(value: any): string {
  switch (value.kind) {
    case 'StringValue':
      // Escape backslashes in string values
      return `"${value.value.replace(/\\/g, '\\')}"`;
    case 'IntValue':
    case 'FloatValue':
    case 'BooleanValue':
      return value.value;
    default:
      // Escape backslashes in string values for default case
      return `"${value.value.replace(/\\/g, '\\')}"`;
  }
}

/**
 * 检查是否为验证指令
 */
export function isValidationDirective(directiveName: string): boolean {
  return VALIDATION_DIRECTIVES.hasOwnProperty(`@${directiveName}`);
}

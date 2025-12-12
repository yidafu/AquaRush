/**
 * 简化的验证注解增强器
 * 为生成的 Kotlin 代码添加 Jakarta Validation 注解
 */
export class ValidationEnhancer {
  private enabled: boolean;
  private annotationTarget: 'field' | 'property';

  constructor(
    enabled: boolean = false,
    annotationTarget: 'field' | 'property' = 'field'
  ) {
    this.enabled = enabled;
    this.annotationTarget = annotationTarget;
  }

  /**
   * 增强生成的 Kotlin 代码
   */
  enhance(code: string, documents: any[]): string {
    if (!this.enabled) {
      return code || '';
    }

    // 确保 code 是字符串
    if (!code || typeof code !== 'string') {
      return code || '';
    }

    try {
      // 添加 Jakarta Validation 导入
      return this.addValidationImports(code);
    } catch (error) {
      return code;
    }
  }

  /**
   * 添加 Jakarta Validation 导入语句
   */
  private addValidationImports(code: string): string {
    // 确保 code 是字符串且不为空
    if (!code || typeof code !== 'string') {
      return code || '';
    }

    // 检查是否已经包含验证导入
    const hasValidationImports = code.includes('jakarta.validation.constraints');

    if (!hasValidationImports) {
      // 在 package 语句后添加导入
      const packageRegex = /(package\s+[^\n]+)/;
      const match = code.match(packageRegex);
      if (match) {
        return code.replace(
          packageRegex,
          `${match[1]}\n\nimport jakarta.validation.constraints.*`
        );
      } else {
        // 如果没有 package 语句，在代码开头添加导入
        return `import jakarta.validation.constraints.*\n\n${code}`;
      }
    }

    return code;
  }
}

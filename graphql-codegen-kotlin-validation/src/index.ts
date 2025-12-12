
import { dirname, normalize } from 'path';
import { GraphQLSchema } from 'graphql';
import { buildPackageNameFromPath } from '@graphql-codegen/java-common';
import {
  getCachedDocumentNodeFromSchema,
  oldVisit,
  PluginFunction,
  Types,
} from '@graphql-codegen/plugin-helpers';
import { KotlinResolversPluginRawConfig } from './config.js';
import { KotlinResolversVisitor } from './visitor.js';

export const plugin: PluginFunction<KotlinResolversPluginRawConfig> = async (
  schema: GraphQLSchema,
  documents: Types.DocumentFile[],
  config: KotlinResolversPluginRawConfig,
  info: any
): Promise<string> => {
  const outputFile = info?.outputFile;
  const relevantPath = dirname(normalize(outputFile || ''));
  const defaultPackageName = buildPackageNameFromPath(relevantPath);

  // Ensure scalars are properly merged if they exist in multiple schema files
  const mergedConfig = {
    ...config,
    scalars: {
      Long: "java.lang.Long",
      BigDecimal: "java.math.BigDecimal",
      LocalDateTime: "java.time.LocalDateTime",
      Map: "Map<String, Any>",
      ...((config.scalars as any) || {})
    }
  };

  const visitor = new KotlinResolversVisitor(mergedConfig, schema, defaultPackageName);
  const astNode = getCachedDocumentNodeFromSchema(schema);
  const visitorResult = oldVisit(astNode, { leave: visitor as any });
  const packageName = visitor.getPackageName();
  let blockContent = visitorResult.definitions.filter((d: any) => typeof d === 'string').join('\n\n');

  // Apply validation annotations if enabled
  if (config.validationAnnotations) {
    const { ValidationEnhancer } = await import('./validation-enhancer.js');
    const validationEnhancer = new ValidationEnhancer(
      true,
      config.annotationTarget || 'field'
    );
    blockContent = validationEnhancer.enhance(blockContent, documents);
  }

  return [packageName, blockContent].join('\n');
};
export const addToSchema = /* GraphQL */``

const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const projectRoot = __dirname;
const workspaceRoot = path.resolve(projectRoot, '../..'); // AquaRush repo root

const config = getDefaultConfig(projectRoot);

// Watch the monorepo so Metro transpiles @aquarush/common (raw TS, no build step).
config.watchFolders = [workspaceRoot];

// Resolve from the app's node_modules first, then the workspace root's (pnpm hoists most deps),
// then pnpm's virtual store flat list (bare-name symlinks to every installed package).
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, 'node_modules'),
  path.resolve(workspaceRoot, 'node_modules'),
  path.resolve(workspaceRoot, 'node_modules/.pnpm/node_modules'),
];

// Keep Metro's default hierarchical lookup ON so transitive/peer deps resolve through pnpm's
// per-package symlinks in the virtual store (e.g. @expo/metro-runtime -> @expo/log-box,
// expo -> expo-asset). The nodeModulesPaths above act as the fallback for workspace/@aquarush.
// (Previously this was `disableHierarchicalLookup = true`, which made every non-direct dep
// resolve only through the flat `.pnpm/node_modules` list and broke those transitive deps.)

// expo-router / @react-navigation depend on query-string@^7 (CommonJS), but the flat
// `.pnpm/node_modules` list above exposes query-string@9 (ESM-only). Bundling that ESM
// build through Metro's CJS interop makes `queryString.stringify` undefined at runtime
// ("queryString.stringify is not a function"). Resolve query-string from expo-router's
// own pinned (v7 CJS) install instead — Node's resolution follows the pnpm symlink into
// the virtual store, where expo-router's strict dependency lives.
const expoRouterDir = path.dirname(require.resolve('expo-router/package.json'));
const queryStringV7 = require.resolve('query-string', { paths: [expoRouterDir] });
const defaultResolver = config.resolver.resolveRequest;

config.resolver.resolveRequest = (context, moduleName, platform) => {
  if (moduleName === 'query-string') {
    return {
      type: 'sourceFile',
      filePath: queryStringV7,
    };
  }
  return defaultResolver
    ? defaultResolver(context, moduleName, platform)
    : context.resolveRequest(context, moduleName, platform);
};

module.exports = config;

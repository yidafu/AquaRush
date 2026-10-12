/** jsdom 缺 TextEncoder/TextDecoder(react 等依赖),jest 启动前补齐。 */
const { TextEncoder, TextDecoder } = require('node:util')
if (typeof globalThis.TextEncoder === 'undefined') globalThis.TextEncoder = TextEncoder
if (typeof globalThis.TextDecoder === 'undefined') globalThis.TextDecoder = TextDecoder

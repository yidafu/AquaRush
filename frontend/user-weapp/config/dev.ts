import type { UserConfigExport } from "@tarojs/cli";

export default {

  mini: {},
  h5: {
    devServer: {
      fs: {
        strict: false
      }
    },
    legacy: true,
  }
} satisfies UserConfigExport<'vite'>

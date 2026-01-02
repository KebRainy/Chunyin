module.exports = {
  devServer: {
    port: 8080,
    hot: true,
    liveReload: false, // 禁用 liveReload，使用 HMR
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  configureWebpack: {
    optimization: {
      splitChunks: {
        chunks: 'all',
        cacheGroups: {
          default: false,
          vendors: false,
          // 将 node_modules 中的包单独打包
          vendor: {
            name: 'chunk-vendors',
            test: /[\\/]node_modules[\\/]/,
            priority: 10,
            chunks: 'all'
          },
          // 将公共代码单独打包
          common: {
            name: 'chunk-common',
            minChunks: 2,
            priority: 5,
            chunks: 'all',
            reuseExistingChunk: true
          }
        }
      }
    }
  },
  chainWebpack: config => {
    // 修复 chunk 加载失败问题 - 只在 preload 插件存在时修改
    if (config.plugins.has('preload')) {
      config.plugin('preload').tap(options => {
        if (options && options[0]) {
          options[0].fileBlacklist = options[0].fileBlacklist || []
          options[0].fileBlacklist.push(/\.map$/, /hot-update\.js$/, /runtime\..*\.js$/)
        }
        return options
      })
    }
    
    // 优化 chunk 文件名
    if (process.env.NODE_ENV === 'production') {
      config.output.filename('js/[name].[contenthash:8].js')
      config.output.chunkFilename('js/[name].[contenthash:8].js')
    }
  }
}

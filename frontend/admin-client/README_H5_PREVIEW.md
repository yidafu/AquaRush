# H5 Product Preview for Admin Client

This feature allows the admin client to display a local H5 version of the product detail page from the user-weapp, providing instant preview capabilities without external dependencies.

## How It Works

The admin client now includes a compiled H5 version of the product detail page that can be embedded directly in the admin interface for real-time preview.

## Build Commands

### Development
```bash
# Build H5 files once
cd frontend/user-weapp
npm run build:h5:admin

# Start admin client development server
cd ../admin-client
npm run dev
```

### Production Build
```bash
# Build H5 and admin client together
cd frontend/admin-client
npm run build:with-weapp
```

### Development with Live Reload
```bash
# Run H5 build in watch mode and admin dev server concurrently
cd frontend/admin-client
npm run dev:with-weapp
```

## File Structure

```
frontend/admin-client/public/weapp/
├── index.html                 # Main HTML file with proper paths
├── js/                        # Compiled JavaScript files
├── css/                       # Compiled CSS files
├── static/                    # Static assets (images, icons)
└── ...                        # Other build artifacts
```

## Configuration

### Environment Variables
- `BUILD_PAGES`: Comma-separated list of pages to include (default: all pages)
- `OUTPUT_DIR`: Custom output directory for H5 files
- `PUBLIC_PATH`: Custom public path for assets
- `NODE_ENV`: Set to 'production' for production builds

### Build Scripts
- `build:h5:admin`: Build H5 for admin client (development)
- `build:h5:admin:watch`: Watch mode for development
- `build:h5:admin:prod`: Production build with optimizations

## Preview Components

### WeChatPreview.tsx
Basic preview component that embeds the H5 product detail page.

### EnhancedWeChatPreview.tsx
Advanced preview component with controls:
- Refresh preview
- Fullscreen mode
- Save and refresh functionality

Both components automatically use local H5 files from `/weapp/` path.

## Features

- **Offline Support**: H5 files are served locally, no external dependencies
- **Version Synchronization**: H5 and admin versions always in sync
- **Performance**: Instant loading without network requests
- **Preview Mode**: Product detail page detects admin preview and adjusts UI accordingly
- **Hot Reload**: Changes to product detail trigger automatic H5 rebuild in development

## Preview Mode Behavior

When loaded in admin preview, the product detail page:
- Shows "预览模式" (Preview Mode) indicator
- Hides purchase buttons and checkout functionality
- Disables customer service calls
- Maintains full product display capabilities
- Optimizes styling for iframe display

## Troubleshooting

### H5 Files Not Loading
1. Run `npm run build:h5:admin` in user-weapp directory
2. Check that `/public/weapp/` directory exists and contains files
3. Verify admin development server is running

### Build Issues
1. Ensure Taro dependencies are properly installed
2. Check that user-weapp project builds successfully
3. Verify no syntax errors in product detail page

### Preview Not Updating
1. In development, use `npm run dev:with-weapp` for automatic rebuilds
2. Otherwise, manually rebuild H5 files after making changes
3. Clear browser cache if needed

## Technical Details

### Route Filtering
The build system filters entry points to include only the product detail page for admin builds, reducing bundle size.

### Path Configuration
H5 files use `/weapp/` public path for proper integration with admin client routing.

### Asset Management
Images and static files are automatically copied to the admin client's public directory.

## Future Enhancements

- Support for multiple pages in admin preview
- Device mockup frames for realistic preview
- Interactive preview controls
- Automatic cache busting for development updates
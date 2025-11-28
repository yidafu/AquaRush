# Aqua Storage Module

## Overview

The `aqua-storage` module provides comprehensive file storage and management capabilities for the AquaRush system. It supports file upload, storage, retrieval, and image processing with configurable storage strategies.

## Features

- **Multiple File Types Support**: Images, videos, audio, documents, archives, and more
- **Image Processing**: Resize, quality adjustment, format conversion, watermarking using Thumbnailator
- **Storage Strategies**: Local file system (with S3 and OSS support ready)
- **File Metadata Management**: Complete file information tracking
- **RESTful APIs**: Comprehensive set of endpoints for file operations
- **Caching**: Intelligent caching for processed images
- **Security**: File validation, size limits, and access control

## Architecture

```
aqua-storage/
├── controller/          # REST API controllers
├── domain/              # Domain models and entities
│   ├── entity/          # JPA entities
│   └── enums/           # Enumeration types
├── dto/                 # Data transfer objects
├── service/             # Business logic
│   ├── impl/            # Service implementations
│   └── storage/         # Storage strategy implementations
├── repository/          # Data access layer
├── config/              # Configuration classes
├── exception/           # Custom exceptions
└── test/                # Unit tests
```

## Key Components

### 1. FileMetadata Entity
Stores comprehensive file information including metadata, checksums, and access control.

### 2. Storage Strategy Pattern
Flexible storage abstraction supporting multiple backends:
- `LocalStorageStrategy`: Local file system storage
- `S3StorageStrategy`: AWS S3 (ready for implementation)
- `OSSStorageStrategy`: Alibaba Cloud OSS (ready for implementation)

### 3. Image Processing Service
Real-time image processing with caching:
- Resize and quality adjustment
- Format conversion (JPEG, PNG, WEBP)
- Watermarking capabilities

### 4. REST API Endpoints

#### File Operations
- `POST /api/v1/storage/files` - Upload file
- `GET /api/v1/storage/files/{id}` - Retrieve file
- `DELETE /api/v1/storage/files/{id}` - Delete file
- `GET /api/v1/storage/files/{id}/metadata` - Get file metadata

#### Image Processing
- `GET /api/v1/storage/files/{id}/image` - Get processed image with parameters
  - `width`, `height`: Resize dimensions
  - `quality`: Image quality (0.1-1.0)
  - `format`: Output format (JPEG, PNG, WEBP)
  - `watermark`: Enable watermark
  - `watermarkText`: Custom watermark text

#### Search and Listing
- `GET /api/v1/storage/files` - List files with pagination
- `GET /api/v1/storage/files/by-type/{fileType}` - Filter by file type
- `GET /api/v1/storage/files/by-owner/{ownerId}` - Filter by owner
- `GET /api/v1/storage/files/search` - Search files by name

## Configuration

### Application Properties

```yaml
# Storage Configuration
storage:
  type: local  # local, s3, oss
  local:
    base-path: /var/aqua/storage
    max-file-size: 104857600  # 100MB
    allowed-extensions:
      - jpg, jpeg, png, gif, bmp, webp
      - mp4, avi, mov, wmv, flv
      - mp3, wav, flac, aac
      - pdf, doc, docx, txt
      - xls, xlsx, csv
      - ppt, pptx
      - html, css, js
      - zip, rar, 7z, tar, gz
      - exe, msi, sh, bat
      - bak, backup

# Image Processing Configuration
image-processing:
  default-quality: 0.8
  max-width: 4096
  max-height: 4096
  supported-formats: [JPEG, PNG, WEBP, GIF, BMP]
  enable-cache: true
  cache-expiration-hours: 24
  default-watermark-text: "AquaRush"
```

## Usage Examples

### Upload a File

```bash
curl -X POST http://localhost:8080/api/v1/storage/files \
  -F "file=@example.jpg" \
  -F "fileType=IMAGE" \
  -F "description=Product image" \
  -F "isPublic=true" \
  -F "ownerId=123"
```

### Process an Image

```bash
curl "http://localhost:8080/api/v1/storage/files/123/image?width=800&height=600&quality=0.8&format=JPEG&watermark=true&watermarkText=Copyright" \
  --output processed_image.jpg
```

### List Files

```bash
curl "http://localhost:8080/api/v1/storage/files?page=0&size=20&sort=createdAt&direction=desc"
```

## File Types Supported

- **IMAGE**: jpg, jpeg, png, gif, bmp, webp
- **VIDEO**: mp4, avi, mov, wmv, flv
- **AUDIO**: mp3, wav, flac, aac
- **DOCUMENT**: pdf, doc, docx, txt
- **SPREADSHEET**: xls, xlsx, csv
- **PRESENTATION**: ppt, pptx
- **FRONTEND**: html, css, js
- **ARCHIVE**: zip, rar, 7z, tar, gz
- **EXECUTABLE**: exe, msi, sh, bat
- **BACKUP**: bak, backup
- **OTHER**: All other file types

## Security Features

- File size validation
- File extension validation
- MIME type detection using Apache Tika
- SHA-256 checksum calculation for duplicate detection
- Public/private file access control
- Comprehensive error handling

## Performance Optimizations

- Intelligent caching for processed images
- File organization by type and date
- Lazy loading of image processing
- Efficient database queries with pagination
- Automatic cleanup of temporary files

## Database Schema

The module uses a `file_metadata` table with the following key columns:

- `id`: Primary key
- `file_name`: Original filename
- `storage_path`: Storage location
- `file_type`: Enum for file categorization
- `file_size`: Size in bytes
- `mime_type`: Detected MIME type
- `checksum`: SHA-256 hash for integrity
- `is_public`: Access control flag
- `owner_id`: File owner reference
- `created_at`, `updated_at`: Timestamps

## Testing

The module includes comprehensive unit tests covering:

- Image processing operations
- File storage and retrieval
- Metadata management
- Storage strategy implementations
- API endpoint functionality

Run tests with:
```bash
./gradlew :modules:aqua-storage:test
```

## Dependencies

- **Thumbnailator**: Image processing
- **Apache Tika**: File type detection
- **AWS SDK**: S3 integration (optional)
- **Aliyun OSS SDK**: Alibaba Cloud integration (optional)
- **Spring Boot**: Framework and data access

## Future Enhancements

- Complete S3 and OSS storage implementations
- File versioning support
- Batch upload capabilities
- Resumable uploads
- CDN integration
- File sharing functionality
- Advanced image filters and effects
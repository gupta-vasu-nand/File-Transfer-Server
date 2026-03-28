# File Transfer Server - Complete Documentation

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Web Interface](#web-interface)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Usage Examples](#usage-examples)
- [Access from Other Devices](#access-from-other-devices)
- [Troubleshooting](#troubleshooting)
- [Architecture](#architecture)
- [Logging](#logging)
- [Security](#security)
- [Development](#development)
- [License](#license)

---

## Overview

The **File Transfer Server** is a Spring Boot application written in Kotlin that allows nearby devices on the same local network to upload, download, list, and delete files. It acts as a central file repository accessible via REST APIs and a beautiful web interface, making it easy to share files across computers, smartphones, and tablets without external cloud services.

### Key Benefits
- **No external dependencies** – runs entirely on your local machine
- **Cross-platform** – accessible from any device with a web browser or HTTP client
- **Beautiful Web Interface** – modern, responsive UI for easy file browsing and downloading
- **Simple setup** – configure via environment variables or YAML
- **Production-ready** – includes comprehensive logging, error handling, and security measures
- **Zero configuration** – works out of the box with sensible defaults

---

## Features

### Core Features
- **Upload files** via multipart/form-data (supports large files with streaming)
- **Download files** with proper Content-Disposition headers
- **List files** with metadata (size, content type, last modified)
- **Delete files** by filename
- **File validation** – size limit (configurable) and extension whitelist
- **Path traversal protection** – sanitizes filenames
- **Global exception handling** with structured error responses
- **Actuator endpoints** for health checks and metrics
- **Logging** with rotation and size limits
- **Cross-network access** – binds to `0.0.0.0` for LAN visibility

### Web Interface Features
- **Modern, responsive design** – works on desktop, tablet, and mobile
- **Auto IP detection** – automatically detects server IP address
- **File statistics** – shows total files, total size, and file types
- **Smart file icons** – displays appropriate icons based on file type
- **Beautiful file cards** – each file displayed with metadata and download button
- **One-click downloads** – click any file card to download
- **Auto-refresh** – updates file list every 30 seconds
- **Persistent settings** – remembers your server IP and port
- **Error handling** – clear error messages with troubleshooting tips
- **Mobile-friendly** – optimized for touch screens and small displays

---

## Prerequisites

- **Java 17** or higher (JDK)
- **Gradle** (or use the included wrapper)
- **Network** – all devices must be connected to the same local network
- **Modern web browser** – Chrome, Firefox, Safari, Edge (for web interface)

---

## Quick Start

### 1. Clone or Download
```bash
git clone https://github.com/gupta-vasu-nand/File-Transfer-Server
cd file-transfer-server
```

### 2. Build the Application
```bash
./gradlew clean build
```

### 3. Run the Server
```bash
./gradlew bootRun
```

The server starts on port `9090` by default and is accessible at:
- **Web Interface**: `http://localhost:9090`
- **API Endpoints**: `http://localhost:9090/api/files`
- **Health Check**: `http://localhost:9090/actuator/health`

### 4. Start Sharing Files
1. Open your browser and go to `http://localhost:9090`
2. Click "Auto Detect IP" or enter your IP address manually
3. Click "Connect" to see all available files
4. Click any "Download" button to download files

---

## Web Interface

### Accessing the Web Interface

When you run the server, simply open your browser and navigate to:
```
http://localhost:9090
```

The web interface provides a beautiful, user-friendly way to browse and download files from your server.

### Features of the Web Interface

#### 1. **Network Configuration Panel**
- Displays instructions to find your IP address
- Input fields for server IP and port
- "Auto Detect IP" button that tries to find your network IP automatically
- "Connect" button to load files from the server
- Saves your settings for future visits

#### 2. **Statistics Dashboard**
Shows real-time statistics about your files:
- **Total Files** – number of files in the storage
- **Total Size** – combined size of all files
- **File Types** – count of unique file types (video, audio, image, etc.)

#### 3. **File Browser Grid**
Each file is displayed as a beautiful card showing:
- **File Icon** – visual representation based on file type
- **File Name** – full filename (truncated if too long)
- **File Size** – human-readable size (MB, GB)
- **File Type** – category (VIDEO, AUDIO, IMAGE, etc.)
- **Last Modified** – date and time of last modification
- **Download Button** – one-click download

#### 4. **Responsive Design**
- Desktop: 3-4 columns of file cards
- Tablet: 2 columns of file cards
- Mobile: 1 column of file cards

### Screenshots


---

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `FILE_STORAGE_PATH` | Directory where files are stored | `/tmp/File-Transfer-Storage` (or `C:/Users/Username/Downloads` on Windows) |
| `SERVER_PORT` | HTTP port | `9090` |
| `FILE_MAX_SIZE` | Maximum file size in bytes | `1073741824` (1 GB) |
| `ALLOWED_EXTENSIONS` | Comma-separated list of allowed file extensions (e.g., `mp3,jpg,pdf`) | `*` (all) |
| `SWAGGER_ENABLED` | Enable/disable Swagger UI | `false` |

#### Example
```bash
FILE_STORAGE_PATH=/mnt/external-drive/files \
SERVER_PORT=8080 \
ALLOWED_EXTENSIONS=mp4,jpg,png \
./gradlew bootRun
```

### application.yml

You can also modify `src/main/resources/application.yml` directly:

```yaml
server:
  port: 9090
  address: 0.0.0.0
  tomcat:
    max-swallow-size: -1
    max-http-form-post-size: -1
    connection-timeout: 5m

spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 1GB
      max-request-size: 1GB
      file-size-threshold: 2KB
  web:
    resources:
      static-locations: classpath:/static/
      add-mappings: true

file:
  storage:
    path: /tmp/File-Transfer-Storage
  max-size: 1073741824
  allowed-extensions: *   # or e.g., mp3,mp4,jpg,png,pdf

logging:
  level:
    root: INFO
    org.vng.filetransferserver: DEBUG
  file:
    name: ./logs/file-transfer-server.log

# Optional: Disable Swagger if not needed
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

---

## API Endpoints

All endpoints are under `/api/files`. Base URL: `http://<server-ip>:<port>/api/files`

### 1. Upload a File
- **URL**: `/upload`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Parameters**:
    - `file` (required): the file to upload
- **Success Response**:
    - **Code**: 200
    - **Body**:
      ```json
      {
        "filename": "unique_filename.txt",
        "originalFilename": "original.txt",
        "size": 12345,
        "contentType": "text/plain",
        "uploadTime": "2026-03-28T15:20:27",
        "downloadUrl": "http://localhost:9090/api/files/unique_filename.txt",
        "message": "File uploaded successfully"
      }
      ```

### 2. List All Files
- **URL**: `/`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Body**:
      ```json
      [
        {
          "filename": "unique_filename.txt",
          "size": 12345,
          "sizeFormatted": "12.06 KB",
          "contentType": "text/plain",
          "lastModified": "2026-03-28T15:20:27Z",
          "downloadUrl": "http://localhost:9090/api/files/unique_filename.txt"
        }
      ]
      ```

### 3. Download a File
- **URL**: `/{filename}`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Headers**: `Content-Disposition: attachment; filename="..."`
    - **Body**: file content

### 4. Delete a File
- **URL**: `/{filename}`
- **Method**: `DELETE`
- **Success Response**:
    - **Code**: 200
    - **Body**:
      ```json
      {
        "message": "File deleted successfully",
        "filename": "unique_filename.txt",
        "deleted": "true"
      }
      ```

### 5. Get File Metadata
- **URL**: `/{filename}/metadata`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Body**: same as list entry

### Error Responses
All errors return a consistent JSON structure with HTTP status code:
```json
{
  "timestamp": "2026-03-28T15:20:27",
  "status": 404,
  "error": "Not Found",
  "message": "File not found: missing.txt",
  "path": "/api/files/missing.txt"
}
```

---

## Usage Examples

### Using Web Interface (Recommended for Users)

1. **Start the server**:
   ```bash
   ./gradlew bootRun
   ```

2. **Open browser** and go to `http://localhost:9090`

3. **Connect to server**:
    - Click "Auto Detect IP" or enter your IP manually
    - Click "Connect"

4. **Browse files**: All files appear as beautiful cards

5. **Download files**: Click "Download" button on any file

### Using curl (For Developers)

#### Upload a file
```bash
curl -X POST http://localhost:9090/api/files/upload \
  -F "file=@/home/user/pictures/photo.jpg" \
  -w "\n"
```

#### List files
```bash
curl http://localhost:9090/api/files | jq '.'   # jq for pretty print
```

#### Download a file
```bash
curl -O http://localhost:9090/api/files/photo_20260328_152030_abc123.jpg
```

#### Delete a file
```bash
curl -X DELETE http://localhost:9090/api/files/photo_20260328_152030_abc123.jpg
```

### Using Python
```python
import requests

# Upload
files = {'file': open('test.txt', 'rb')}
response = requests.post('http://localhost:9090/api/files/upload', files=files)
print(response.json())

# List
response = requests.get('http://localhost:9090/api/files')
print(response.json())

# Download
response = requests.get('http://localhost:9090/api/files/somefile.txt')
with open('downloaded.txt', 'wb') as f:
    f.write(response.content)
```

### Using JavaScript (Browser)
```javascript
// List files and display in browser
fetch('http://localhost:9090/api/files')
  .then(response => response.json())
  .then(files => {
    files.forEach(file => {
      console.log(`${file.filename} - ${file.sizeFormatted}`);
    });
  });
```

---

## Access from Other Devices

### 1. Find your computer's IP address
- **Windows**: Open Command Prompt and run `ipconfig` (look for IPv4 under your active network adapter)
- **Linux/Mac**: Run `ifconfig` or `ip addr`

Example: `172.19.20.91`

### 2. Access from other devices on the same network
- **Web Interface**: `http://172.19.20.91:9090`
- **API Endpoints**: `http://172.19.20.91:9090/api/files`

### 3. Share the web interface link
Send this link to anyone on your network:
```
http://172.19.20.91:9090
```

They can open it in their browser and immediately see all available files!

### 4. Allow the port through firewall
If other devices cannot connect, you may need to allow inbound connections on port `9090`.

#### Windows Firewall (Admin Command Prompt)
```bash
netsh advfirewall firewall add rule name="File Transfer Server" dir=in action=allow protocol=TCP localport=9090
```

#### Linux (UFW)
```bash
sudo ufw allow 9090/tcp
```

#### Mac
System Preferences → Security & Privacy → Firewall → Firewall Options → Add `java` or the application.

---

## Troubleshooting

### Common Errors and Solutions

#### 1. **Web interface shows "Cannot connect to server"**
- **Cause**: Server not running or wrong IP/port
- **Solution**:
    - Make sure server is running (`./gradlew bootRun`)
    - Verify IP address with `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
    - Check if port is correct (default: 9090)
    - Try clicking "Auto Detect IP" button

#### 2. **No files appear in web interface**
- **Cause**: Storage directory is empty or wrong path
- **Solution**:
    - Upload some files using curl: `curl -X POST http://localhost:9090/api/files/upload -F "file=@test.txt"`
    - Check storage path in logs
    - Verify `FILE_STORAGE_PATH` environment variable

#### 3. **Cannot access from mobile phone**
- **Cause**: Firewall blocking or different network
- **Solution**:
    - Ensure both devices on same Wi-Fi network
    - Add firewall rule for port 9090
    - Try accessing via computer name: `http://DESKTOP-NAME:9090`
    - Check if network has client isolation (public Wi-Fi often blocks device-to-device)

#### 4. **`NoSuchMethodError: ControllerAdviceBean.<init>`**
- **Cause**: Version incompatibility between Spring Boot and SpringDoc
- **Solution**: Disable Swagger in `application.yml`:
  ```yaml
  springdoc:
    api-docs:
      enabled: false
    swagger-ui:
      enabled: false
  ```

#### 5. **File upload fails with `MaxUploadSizeExceededException`**
- **Cause**: File size exceeds configured limit
- **Solution**: Increase limit via environment variable:
  ```bash
  FILE_MAX_SIZE=2147483648 ./gradlew bootRun  # 2GB
  ```

#### 6. **`FileStorageException` – cannot write to storage directory**
- **Cause**: Permission issues or invalid path
- **Solution**:
    - Check storage path exists and is writable
    - Use forward slashes in path: `C:/Users/Username/Downloads`
    - Set environment variable: `FILE_STORAGE_PATH=/path/to/storage`

### Debugging Tips
- **Enable debug logging** in `application.yml`:
  ```yaml
  logging:
    level:
      org.springframework.web: DEBUG
      org.vng.filetransferserver: DEBUG
  ```
- **Check logs**: `./logs/file-transfer-server.log`
- **Test API directly**: `curl http://localhost:9090/api/files`
- **Check server health**: `curl http://localhost:9090/actuator/health`
- **Verify port listening**: `netstat -an | findstr :9090` (Windows) or `lsof -i :9090` (Mac/Linux)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Web Browser                            │
│                  (Mobile, Desktop, Tablet)                  │
│                    http://IP:9090                          │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Static Resources                         │
│                   (index.html, CSS, JS)                     │
│                  Served by Spring Boot                      │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   REST Controller                           │
│               (FileController.kt)                          │
│          Endpoints: /api/files/*                           │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│               (FileService + Impl)                         │
│         Business logic, validation, streaming               │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   File System                               │
│          (StorageConfig, FileUtils)                        │
│            Physical file storage                           │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

- **Web Interface**: `src/main/resources/static/index.html` – beautiful file browser UI
- **Controller**: Handles HTTP requests, maps to service methods, returns responses
- **Service**: Contains business logic (validation, file operations, streaming)
- **Config**: Reads configuration, initializes storage directory
- **DTOs**: Data transfer objects for request/response
- **Exception Handling**: Global handler for consistent error responses
- **Utilities**: Filename sanitization, unique name generation, file size formatting

---

## Logging

Logs are stored in `./logs/` directory with rotation:

- `file-transfer-server.log` – all logs (INFO and above)
- `file-transfer-server-error.log` – only ERROR level logs

Rotation: daily, 10MB per file, keep 30 days.

To change logging behavior, modify `src/main/resources/logback-spring.xml`.

---

## Security

- **Path Traversal Prevention**: All filenames are sanitized using `FileUtils.sanitizeFilename` to remove `../` and other dangerous patterns
- **File Size Limits**: Configurable via `file.max-size` to prevent DoS attacks
- **Extension Whitelist**: Optional restriction via `ALLOWED_EXTENSIONS` to only allow specific file types
- **Network Isolation**: The server binds to all interfaces (`0.0.0.0`), but access is limited to your local network
- **No Authentication**: Designed for trusted local networks; do not expose to public internet

---

## Development

### Project Structure
```
src/main/
├── kotlin/org/vng/filetransferserver/
│   ├── controller/           # REST endpoints
│   │   └── FileController.kt
│   ├── service/              # Business logic
│   │   ├── FileService.kt
│   │   └── impl/
│   │       └── FileServiceImpl.kt
│   ├── dto/                  # Data transfer objects
│   │   ├── FileUploadResponseDTO.kt
│   │   ├── FileMetadataDTO.kt
│   │   └── ErrorResponseDTO.kt
│   ├── config/               # Configuration classes
│   │   ├── StorageConfig.kt
│   │   └── WebConfig.kt
│   ├── exception/            # Custom exceptions & global handler
│   │   ├── CustomExceptions.kt
│   │   └── GlobalExceptionHandler.kt
│   ├── util/                 # Helper classes
│   │   └── FileUtils.kt
│   └── FileTransferServerApplication.kt
└── resources/
    ├── static/               # Static web resources
    │   └── index.html        # Web interface
    ├── application.yml       # Application configuration
    └── logback-spring.xml    # Logging configuration
```

### Running Tests
```bash
./gradlew test
```

### Building the JAR
```bash
./gradlew bootJar
```

The JAR will be in `build/libs/`. Run it with:
```bash
java -jar build/libs/file-transfer-server-0.0.1-SNAPSHOT.jar
```

### Customizing the Web Interface
The web interface is a single HTML file located at `src/main/resources/static/index.html`. You can:
- Modify colors and styling in the `<style>` section
- Change icons and file type detection logic
- Add new features like file preview or search
- Customize the layout for your needs

### Contributing
Feel free to fork and submit pull requests. Please ensure code follows Kotlin conventions and includes appropriate tests.

---

## Appendix

### Example Workflow

1. **Start server**:
   ```bash
   ./gradlew bootRun
   ```

2. **Open web interface**:
    - Browser: `http://localhost:9090`
    - Click "Auto Detect IP" or enter IP manually
    - Click "Connect"

3. **Upload files** (via curl):
   ```bash
   curl -X POST http://localhost:9090/api/files/upload -F "file=@/home/user/Desktop/photo.jpg"
   ```

4. **Share with friend**:
    - Send link: `http://172.19.20.91:9090`
    - Friend opens in browser, sees all files
    - Friend clicks download on any file

5. **Download from mobile**:
    - Open browser on phone
    - Enter `http://172.19.20.91:9090`
    - Browse and download files directly to phone

6. **Delete after sharing** (via curl):
   ```bash
   curl -X DELETE http://localhost:9090/api/files/photo_20260328_152030_abc123.jpg
   ```

### Useful curl Options
- `-v` – verbose output (shows headers)
- `-w "\n"` – adds a newline after response
- `--progress-bar` – shows upload progress
- `-O` – saves file with original name
- `-o filename` – saves file with custom name

### Web Interface Keyboard Shortcuts
- `Enter` – Connect after entering IP/port
- `Tab` – Navigate between input fields
- `Esc` – Close error messages

### Browser Compatibility
- **Chrome** 90+ – Full support
- **Firefox** 88+ – Full support
- **Safari** 14+ – Full support
- **Edge** 90+ – Full support
- **Mobile browsers** – Full support (iOS, Android)

---

## License

This project is open-source and available under the MIT License. See the `LICENSE` file for details.

---

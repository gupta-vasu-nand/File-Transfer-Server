# File Transfer Server - Complete Documentation

![Cover](/src/main/resources/static/Collage_.png)

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Web Interface](#web-interface)
- [Media Streaming Interface](#media-streaming-interface)
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

The **File Transfer Server** is a Spring Boot application written in Kotlin that allows nearby devices on the same local
network to upload, download, list, delete files, and stream media content. It acts as a central file repository
accessible via REST APIs and two specialized web interfaces - a file browser for general file management and a media
streaming player for video and audio content. This makes it easy to share files and stream media across computers,
smartphones, and tablets without external cloud services.

### Key Benefits

- **No external dependencies** – runs entirely on your local machine
- **Cross-platform** – accessible from any device with a web browser or HTTP client
- **Beautiful Web Interfaces** – modern, responsive UI for file browsing and media streaming
- **Media Streaming Support** – stream videos and audio with seeking, pause, and playback controls
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
- **Media streaming** with HTTP range request support for seeking
- **File validation** – size limit (configurable) and extension whitelist
- **Path traversal protection** – sanitizes filenames
- **Global exception handling** with structured error responses
- **Actuator endpoints** for health checks and metrics
- **Logging** with rotation and size limits
- **Cross-network access** – binds to `0.0.0.0` for LAN visibility

### File Browser Interface Features

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

### Media Streaming Interface Features

- **Video Player** – full-featured HTML5 video player with native controls
- **Audio Player** – dedicated audio player with visualization
- **Seeking Support** – ability to seek to any position in the media file
- **Range Request Support** – efficient streaming with HTTP byte range requests
- **Media Information Display** – shows file size, format, and resolution
- **Download Option** – direct download link for offline playback
- **Progressive Loading** – media loads progressively for smooth playback
- **Connection Resilience** – handles network interruptions gracefully
- **Codec Detection** – identifies video and audio codec information
- **Responsive Design** – player adapts to screen size for optimal viewing

---

## Prerequisites

- **Java 17** or higher (JDK)
- **Gradle** (or use the included wrapper)
- **Network** – all devices must be connected to the same local network
- **Modern web browser** – Chrome, Firefox, Safari, Edge (for web interface and media streaming)

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

- **File Browser Interface**: `http://localhost:9090`
- **Media Streaming Interface**: `http://localhost:9090/player.html`
- **API Endpoints**: `http://localhost:9090/api/files`
- **Media Streaming API**: `http://localhost:9090/api/stream?path={filename}`
- **Health Check**: `http://localhost:9090/actuator/health`

### 4. Start Using the Server

1. Open your browser and go to `http://localhost:9090` for file management
2. Or go to `http://localhost:9090/player.html` for media streaming
3. Click "Auto Detect IP" or enter your IP address manually
4. Click "Connect" to see available files
5. Click any file to stream or download

---

## Web Interface

### Screenshots
![Screenshot1](/src/main/resources/static/dashboard.png)
![Screenshot1](/src/main/resources/static/search_preview.png)

![Screenshot1](/src/main/resources/static/video_preview.png)
![Screenshot1](/src/main/resources/static/music_preview.png)
![Screenshot1](/src/main/resources/static/images_preview.png)

### Accessing the File Browser

When you run the server, simply open your browser and navigate to:

```
http://localhost:9090
```

The file browser interface provides a user-friendly way to browse and download files from your server.

### Features of the File Browser

#### 1. Network Configuration Panel

- Displays instructions to find your IP address
- Input fields for server IP and port
- "Auto Detect IP" button that tries to find your network IP automatically
- "Connect" button to load files from the server
- Saves your settings for future visits

#### 2. Statistics Dashboard

Shows real-time statistics about your files:

- **Total Files** – number of files in the storage
- **Total Size** – combined size of all files
- **File Types** – count of unique file types (video, audio, image, etc.)

#### 3. File Browser Grid

Each file is displayed as a card showing:

- **File Icon** – visual representation based on file type
- **File Name** – full filename (truncated if too long)
- **File Size** – human-readable size (MB, GB)
- **File Type** – category (VIDEO, AUDIO, IMAGE, etc.)
- **Last Modified** – date and time of last modification
- **Download Button** – one-click download

#### 4. Responsive Design

- Desktop: 3-4 columns of file cards
- Tablet: 2 columns of file cards
- Mobile: 1 column of file cards

---

## Media Streaming Interface

### Accessing the Media Player

For streaming video and audio content, navigate to:

```
http://localhost:9090/player.html
```

The media streaming interface provides a dedicated player for seamless playback of video and audio files stored on your
server.

### Features of the Media Streaming Interface

#### 1. Server Connection Panel

- Input fields for server IP address and port
- "Auto Detect IP" button for automatic server discovery
- "Connect" button to establish connection and load media library
- Connection status indicator showing success or failure
- Persistent storage of connection settings

#### 2. Media Library View

- **Filter Controls** – toggle between All Media, Videos, and Audio
- **Search Functionality** – real-time search by filename
- **Media Grid Display** – responsive grid layout showing all media files
- **Media Cards** – each card displays:
    - File type icon (video or audio)
    - Filename with truncation for long names
    - Formatted file size
    - Resolution (for videos)
    - Bitrate information (when available)

#### 3. Video Player

- **Native HTML5 Controls** – play, pause, volume, fullscreen
- **Seeking Support** – click anywhere on the progress bar to jump to any position
- **Buffering Indicator** – visual feedback during loading
- **Time Display** – current position and total duration
- **Playback Speed** – adjustable via browser controls
- **Picture-in-Picture** – available in supported browsers
- **Keyboard Shortcuts** – spacebar for play/pause, arrow keys for seeking

#### 4. Audio Player

- **Dedicated Audio Controls** – play, pause, volume
- **Visualization** – waveform visualization (browser dependent)
- **Track Information** – filename and metadata display
- **Continuous Playback** – plays in background while browsing
- **Playlist Support** – queue multiple audio files

#### 5. Media Information Panel

When a media file is playing, the interface displays:

- **Current Track** – filename of the playing media
- **File Size** – total size of the media file
- **Format Information** – video/audio codec information
- **Resolution** – video dimensions (for video files)
- **Bitrate** – estimated bitrate of the media
- **Download Link** – direct download option for offline playback

#### 6. Technical Features

- **Range Request Support** – enables seeking without downloading entire file
- **Progressive Loading** – media loads incrementally for immediate playback
- **Bandwidth Optimization** – only requests needed portions of the file
- **Error Recovery** – automatically retries on connection issues
- **Codec Detection** – identifies and displays media codec information
- **Browser Compatibility** – adapts player based on browser capabilities

### Media Player Controls

#### Video Player Controls

- **Play/Pause** – start and stop playback
- **Volume Control** – adjust audio level with mute option
- **Progress Bar** – seek to any position by clicking or dragging
- **Time Display** – shows current time and total duration
- **Fullscreen** – toggle fullscreen mode
- **Picture-in-Picture** – pop out player for multitasking
- **Playback Speed** – adjust speed (0.5x, 1x, 1.5x, 2x)

#### Audio Player Controls

- **Play/Pause** – start and stop audio playback
- **Volume Control** – adjust audio level
- **Progress Bar** – seek within audio track
- **Time Display** – current position and duration

---

## Configuration

### Environment Variables

| Variable             | Description                                                           | Default                                                                    |
|----------------------|-----------------------------------------------------------------------|----------------------------------------------------------------------------|
| `FILE_STORAGE_PATH`  | Directory where files are stored                                      | `/tmp/File-Transfer-Storage` (or `C:/Users/Username/Downloads` on Windows) |
| `SERVER_PORT`        | HTTP port                                                             | `9090`                                                                     |
| `FILE_MAX_SIZE`      | Maximum file size in bytes                                            | `1073741824` (1 GB)                                                        |
| `ALLOWED_EXTENSIONS` | Comma-separated list of allowed file extensions (e.g., `mp3,jpg,pdf`) | `*` (all)                                                                  |

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

springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

---

## API Endpoints

All endpoints are under `/api`. Base URL: `http://<server-ip>:<port>/api`

### File Management Endpoints

#### 1. Upload a File

- **URL**: `/files/upload`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **Parameters**:
    - `file` (required): the file to upload
    - `path` (optional): folder path to upload to
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

#### 2. List All Files

- **URL**: `/files`
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

#### 3. Download a File

- **URL**: `/files/{filename}`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Headers**: `Content-Disposition: attachment; filename="..."`
    - **Body**: file content

#### 4. Delete a File

- **URL**: `/files/{filename}`
- **Method**: `DELETE`
- **Success Response**:
    - **Code**: 200
    - **Body**:
      ```json
      {
        "message": "File deleted successfully",
        "filename": "unique_filename.txt",
        "deleted": true
      }
      ```

#### 5. Get File Metadata

- **URL**: `/files/{filename}/metadata`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Body**: same as list entry

### Folder Management Endpoints

#### 6. Get Folder Contents

- **URL**: `/folders/contents?path={path}`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Body**:
      ```json
      {
        "currentPath": "Videos",
        "parentPath": "",
        "folders": [
          {
            "name": "Subfolder",
            "path": "Videos/Subfolder",
            "fileCount": 5,
            "totalSize": 10485760,
            "totalSizeFormatted": "10 MB",
            "lastModified": "2026-03-28T15:20:27Z"
          }
        ],
        "files": [
          {
            "filename": "video.mp4",
            "path": "Videos/video.mp4",
            "size": 10485760,
            "sizeFormatted": "10 MB",
            "contentType": "video/mp4",
            "lastModified": "2026-03-28T15:20:27Z"
          }
        ],
        "totalSize": 10485760,
        "totalSizeFormatted": "10 MB",
        "fileCount": 1,
        "folderCount": 1
      }
      ```

#### 7. Create Folder

- **URL**: `/folders?path={path}`
- **Method**: `POST`
- **Success Response**: 200 OK

#### 8. Delete Folder

- **URL**: `/folders/{path}`
- **Method**: `DELETE`
- **Success Response**: 200 OK

#### 9. Get Folder Tree

- **URL**: `/folders/tree`
- **Method**: `GET`
- **Success Response**: Returns hierarchical folder structure

### Media Streaming Endpoints

#### 10. Stream Media File

- **URL**: `/stream?path={path}`
- **Method**: `GET`
- **Headers**: Supports `Range` header for seeking
- **Success Response**:
    - **Code**: 200 (full file) or 206 (partial content)
    - **Headers**:
        - `Accept-Ranges: bytes`
        - `Content-Type: video/mp4` or appropriate MIME type
        - `Content-Range: bytes start-end/total` (for partial content)
    - **Body**: media file content

#### 11. Get Media Information

- **URL**: `/media/info?path={path}`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Body**:
      ```json
      {
        "filename": "video.mp4",
        "title": "video",
        "path": "Videos/video.mp4",
        "parentFolder": "Videos",
        "folderPath": "Videos",
        "size": 123456789,
        "sizeFormatted": "117.74 MB",
        "contentType": "video/mp4",
        "lastModified": "2026-03-28T15:20:27Z",
        "duration": null,
        "bitrate": null,
        "resolution": null,
        "videoCodec": null,
        "audioCodec": null,
        "hasAudio": true,
        "streamUrl": "/api/stream?path=Videos/video.mp4",
        "downloadUrl": "/api/files/Videos/video.mp4"
      }
      ```

#### 12. List All Media Files

- **URL**: `/media`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Body**: Array of media information objects

### Search Endpoints

#### 13. Search Files

- **URL**: `/search?query={query}&folder={folder}`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Body**:
      ```json
      {
        "query": "vacation",
        "results": [...],
        "count": 3
      }
      ```

### System Endpoints

#### 14. Get System Information

- **URL**: `/system/info`
- **Method**: `GET`
- **Success Response**:
    - **Code**: 200
    - **Body**:
      ```json
      {
        "totalSpace": 500000000000,
        "totalSpaceFormatted": "465.66 GB",
        "freeSpace": 200000000000,
        "freeSpaceFormatted": "186.26 GB",
        "usedSpace": 300000000000,
        "usedSpaceFormatted": "279.40 GB",
        "usagePercentage": 60
      }
      ```

### Batch Operations

#### 15. Batch Operations

- **URL**: `/batch`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "operation": "delete",
    "files": ["file1.txt", "file2.txt"],
    "destination": null
  }
  ```
- **Success Response**: Returns success/failure counts

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

#### File Browser

1. **Start the server**:
   ```bash
   ./gradlew bootRun
   ```

2. **Open browser** and go to `http://localhost:9090`

3. **Connect to server**:
    - Click "Auto Detect IP" or enter your IP manually
    - Click "Connect"

4. **Browse files**: All files appear as cards

5. **Download files**: Click "Download" button on any file

#### Media Player

1. **Open media player** at `http://localhost:9090/player.html`

2. **Connect to server**:
    - Enter server IP and port
    - Click "Connect"

3. **Browse media**: Filter by video or audio

4. **Play media**: Click any media card to start streaming

### Using curl (For Developers)

#### Upload a file

```bash
curl -X POST http://localhost:9090/api/files/upload \
  -F "file=@/home/user/pictures/photo.jpg" \
  -w "\n"
```

#### Upload to specific folder

```bash
curl -X POST http://localhost:9090/api/files/upload \
  -F "file=@/home/user/pictures/photo.jpg" \
  -F "path=Images/Vacation" \
  -w "\n"
```

#### List files

```bash
curl http://localhost:9090/api/files | jq '.'
```

#### Get folder contents

```bash
curl "http://localhost:9090/api/folders/contents?path=Videos"
```

#### Download a file

```bash
curl -O http://localhost:9090/api/files/photo_20260328_152030_abc123.jpg
```

#### Create a folder

```bash
curl -X POST "http://localhost:9090/api/folders?path=NewFolder"
```

#### Delete a folder

```bash
curl -X DELETE "http://localhost:9090/api/folders/NewFolder"
```

#### Stream video with range request (seeking)

```bash
curl -H "Range: bytes=0-1000000" "http://localhost:9090/api/stream?path=videos/video.mp4"
```

#### Get media information

```bash
curl "http://localhost:9090/api/media/info?path=videos/video.mp4"
```

#### Search for files

```bash
curl "http://localhost:9090/api/search?query=vacation&folder=Photos"
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
data = {'path': 'Documents'}
response = requests.post('http://localhost:9090/api/files/upload', files=files, data=data)
print(response.json())

# List files
response = requests.get('http://localhost:9090/api/files')
print(response.json())

# Get folder contents
response = requests.get('http://localhost:9090/api/folders/contents', params={'path': 'Videos'})
print(response.json())

# Create folder
response = requests.post('http://localhost:9090/api/folders', params={'path': 'NewFolder'})
print(response.status_code)

# Search files
response = requests.get('http://localhost:9090/api/search', params={'query': 'vacation'})
print(response.json())

# Stream video with range
headers = {'Range': 'bytes=0-1000000'}
response = requests.get('http://localhost:9090/api/stream', params={'path': 'video.mp4'}, headers=headers, stream=True)
with open('video_part.mp4', 'wb') as f:
    for chunk in response.iter_content(chunk_size=8192):
        f.write(chunk)

# Get media info
response = requests.get('http://localhost:9090/api/media/info', params={'path': 'video.mp4'})
print(response.json())
```

### Using JavaScript (Browser)

```javascript
// List media files
fetch('http://localhost:9090/api/media')
    .then(response => response.json())
    .then(files => {
        files.forEach(file => {
            console.log(`${file.title} - ${file.sizeFormatted}`);
        });
    });

// Get folder contents
fetch('http://localhost:9090/api/folders/contents?path=Videos')
    .then(response => response.json())
    .then(data => {
        console.log('Folders:', data.folders);
        console.log('Files:', data.files);
    });

// Search files
fetch('http://localhost:9090/api/search?query=vacation')
    .then(response => response.json())
    .then(result => {
        console.log(`Found ${result.count} files`);
    });

// Upload file
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('path', 'Documents');
fetch('http://localhost:9090/api/files/upload', {
    method: 'POST',
    body: formData
}).then(response => response.json())
    .then(data => console.log('Uploaded:', data));

// Create folder
fetch('http://localhost:9090/api/folders?path=NewFolder', {
    method: 'POST'
}).then(() => console.log('Folder created'));

// Stream video with HTML5 video element
const videoElement = document.createElement('video');
videoElement.src = 'http://localhost:9090/api/stream?path=video.mp4';
videoElement.controls = true;
document.body.appendChild(videoElement);
```

---

## Access from Other Devices

### 1. Find your computer's IP address

- **Windows**: Open Command Prompt and run `ipconfig` (look for IPv4 under your active network adapter)
- **Linux/Mac**: Run `ifconfig` or `ip addr`

Example: `172.19.20.91`

### 2. Access from other devices on the same network

- **File Browser**: `http://172.19.20.91:9090`
- **Media Player**: `http://172.19.20.91:9090/player.html`
- **API Endpoints**: `http://172.19.20.91:9090/api/files`

### 3. Share the web interface links

Send these links to anyone on your network:

```
http://172.19.20.91:9090              # File browser
http://172.19.20.91:9090/player.html  # Media player
```

They can open it in their browser and immediately see all available files or stream media.

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

System Preferences -> Security & Privacy -> Firewall -> Firewall Options -> Add `java` or the application.

---

## Troubleshooting

### Common Errors and Solutions

#### 1. Web interface shows "Cannot connect to server"

- **Cause**: Server not running or wrong IP/port
- **Solution**:
    - Make sure server is running (`./gradlew bootRun`)
    - Verify IP address with `ipconfig` (Windows) or `ifconfig` (Mac/Linux)
    - Check if port is correct (default: 9090)
    - Try clicking "Auto Detect IP" button

#### 2. No files appear in web interface

- **Cause**: Storage directory is empty or wrong path
- **Solution**:
    - Upload some files using curl: `curl -X POST http://localhost:9090/api/files/upload -F "file=@test.txt"`
    - Check storage path in logs
    - Verify `FILE_STORAGE_PATH` environment variable

#### 3. Video won't play or has no audio

- **Cause**: Unsupported video/audio codec or missing codec support
- **Solution**:
    - Check media information panel for codec details
    - Use the download link to play the file locally with VLC Media Player
    - Convert the file to a more compatible format (H.264/AAC for video, MP3/AAC for audio)

#### 4. Video seeking doesn't work

- **Cause**: Server doesn't support range requests or file is too large
- **Solution**:
    - Ensure the streaming endpoint is being used (`/api/stream?path=...`)
    - Check that your browser supports HTML5 video with range requests
    - For very large files, allow time for initial buffering

#### 5. Cannot access from mobile phone

- **Cause**: Firewall blocking or different network
- **Solution**:
    - Ensure both devices on same Wi-Fi network
    - Add firewall rule for port 9090
    - Try accessing via computer name: `http://DESKTOP-NAME:9090`
    - Check if network has client isolation (public Wi-Fi often blocks device-to-device)

#### 6. File upload fails with `MaxUploadSizeExceededException`

- **Cause**: File size exceeds configured limit
- **Solution**: Increase limit via environment variable:
  ```bash
  FILE_MAX_SIZE=2147483648 ./gradlew bootRun  # 2GB
  ```

#### 7. `FileStorageException` – cannot write to storage directory

- **Cause**: Permission issues or invalid path
- **Solution**:
    - Check storage path exists and is writable
    - Use forward slashes in path: `C:/Users/Username/Downloads`
    - Set environment variable: `FILE_STORAGE_PATH=/path/to/storage`

#### 8. ClientAbortException in logs

- **Cause**: Client disconnected during streaming (normal behavior during page refresh)
- **Solution**: This is not an error, just informational. No action needed.

#### 9. "Connection reset by peer" error

- **Cause**: Client refreshed the page or navigated away while media was streaming
- **Solution**: This is expected behavior. The server handles it gracefully.

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
- **Test streaming with curl**: `curl -H "Range: bytes=0-1000" "http://localhost:9090/api/stream?path=video.mp4"`

---

## Architecture

```
+---------------------------------------------------------------------+
|                        Web Browser                                  |
|                  (Mobile, Desktop, Tablet)                          |
|              http://IP:9090 (File Browser)                          |
|           http://IP:9090/player.html (Media Player)                 |
+---------------------------------------------------------------------+
                                   |
                                   v
+---------------------------------------------------------------------+
|                      Static Resources                               |
|            (index.html, player.html, CSS, JS)                       |
|                  Served by Spring Boot                              |
+---------------------------------------------------------------------+
                                   |
                                   v
+---------------------------------------------------------------------+
|                      REST Controller                                |
|                  (MediaController.kt)                               |
|     Endpoints: /api/files/*, /api/stream, /api/media/*              |
|                /api/folders/*, /api/search                          |
+---------------------------------------------------------------------+
                                   |
                                   v
+---------------------------------------------------------------------+
|                      Service Layer                                  |
|         (FileService + StreamingService + Impl)                     |
|     Business logic, validation, streaming, range support            |
|              Folder navigation, search, batch operations            |
+---------------------------------------------------------------------+
                                   |
                                   v
+---------------------------------------------------------------------+
|                      File System                                    |
|          (StorageConfig, FileUtils)                                 |
|            Physical file storage                                    |
+---------------------------------------------------------------------+
```

### Key Components

- **File Browser Interface**: `src/main/resources/static/index.html` – file browser UI
- **Media Player Interface**: `src/main/resources/static/player.html` – media streaming player
- **Controller**: Handles HTTP requests, maps to service methods, returns responses
- **File Service**: Manages file operations (upload, download, list, delete, move, copy)
- **Folder Service**: Handles folder creation, deletion, navigation, tree structure
- **Streaming Service**: Handles media streaming with range request support
- **Search Service**: Recursive file search across all folders
- **Config**: Reads configuration, initializes storage directory
- **DTOs**: Data transfer objects for request/response
- **Exception Handling**: Global handler for consistent error responses
- **Utilities**: Filename sanitization, path normalization, file size formatting

---

## Logging

Logs are stored in `./logs/` directory with rotation:

- `file-transfer-server.log` – all logs (INFO and above)
- `file-transfer-server.%d{yyyy-MM-dd}.log` – daily rotated logs

Rotation: daily, 10MB per file, keep 30 days, total size cap 1GB.

To change logging behavior, modify `src/main/resources/logback-spring.xml`.

---

## Security

- **Path Traversal Prevention**: All filenames are sanitized using `FileUtils.sanitizeFilename` to remove `../` and
  other dangerous patterns
- **File Size Limits**: Configurable via `file.max-size` to prevent DoS attacks
- **Extension Whitelist**: Optional restriction via `ALLOWED_EXTENSIONS` to only allow specific file types
- **Network Isolation**: The server binds to all interfaces (`0.0.0.0`), but access is limited to your local network
- **No Authentication**: Designed for trusted local networks; do not expose to public internet
- **Streaming Security**: Range requests are validated to prevent abuse
- **Filename Sanitization**: Removes special characters that could be used for path traversal

---

## Development

### Project Structure

```
src/main/
├── kotlin/org/vng/filetransferserver/
│   ├── controller/           # REST endpoints
│   │   └── MediaController.kt
│   ├── service/              # Business logic
│   │   ├── FileService.kt
│   │   ├── StreamingService.kt
│   │   └── impl/
│   │       ├── FileServiceImpl.kt
│   │       └── StreamingServiceImpl.kt
│   ├── dto/                  # Data transfer objects
│   │   ├── FileUploadResponseDTO.kt
│   │   ├── FileMetadataDTO.kt
│   │   ├── MediaInfoDTO.kt
│   │   ├── FolderContentsDTO.kt
│   │   ├── FolderInfoDTO.kt
│   │   ├── FileSystemInfoDTO.kt
│   │   ├── SearchResultDTO.kt
│   │   ├── BatchOperationDTO.kt
│   │   ├── BatchOperationResponseDTO.kt
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
    │   ├── index.html        # File browser interface
    │   └── player.html       # Media streaming interface
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

### Customizing the Web Interfaces

The web interfaces are HTML files located at `src/main/resources/static/`:

- `index.html` – file browser interface
- `player.html` – media streaming interface

You can:

- Modify colors and styling in the `<style>` section
- Change icons and file type detection logic
- Add new features like file preview or playlist support
- Customize the layout for your needs
- Add thumbnail generation for media files

### Contributing

Feel free to fork and submit pull requests. Please ensure code follows Kotlin conventions and includes appropriate
tests.

---

## Appendix

### Example Workflow

#### File Sharing Workflow

1. **Start server**:
   ```bash
   ./gradlew bootRun
   ```

2. **Open file browser**:
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

#### Media Streaming Workflow

1. **Start server**:
   ```bash
   ./gradlew bootRun
   ```

2. **Open media player**:
    - Browser: `http://localhost:9090/player.html`
    - Enter server IP and port
    - Click "Connect"

3. **Browse media library**:
    - Filter by video or audio
    - Search for specific content

4. **Stream content**:
    - Click any video or audio file
    - Player loads and begins streaming
    - Use controls to seek, pause, or adjust volume

5. **Download for offline**:
    - Click download link in media info panel
    - Save file for offline playback

### Useful curl Options

- `-v` – verbose output (shows headers)
- `-w "\n"` – adds a newline after response
- `--progress-bar` – shows upload progress
- `-O` – saves file with original name
- `-o filename` – saves file with custom name
- `-H "Range: bytes=0-1000"` – request specific byte range for streaming

### Web Interface Keyboard Shortcuts

- `Enter` – Connect after entering IP/port
- `Tab` – Navigate between input fields
- `Esc` – Close error messages

### Media Player Keyboard Shortcuts

- `Space` – Play/Pause
- `Left Arrow` – Seek backward 10 seconds
- `Right Arrow` – Seek forward 10 seconds
- `Up Arrow` – Increase volume
- `Down Arrow` – Decrease volume
- `F` – Toggle fullscreen
- `M` – Mute/Unmute
- `S` – Toggle shuffle
- `A` – Toggle ambient mode
- `I` – Picture-in-Picture
- `Shift+N` – Next track
- `Shift+P` – Previous track
- `Shift+>` – Increase playback speed
- `Shift+<` – Decrease playback speed
- `1-9` – Seek to 10%-90% of video

### Browser Compatibility

- **Chrome** 90+ – Full support, best performance
- **Firefox** 88+ – Full support
- **Safari** 14+ – Full support (limited codec support)
- **Edge** 90+ – Full support
- **Mobile browsers** – Full support (iOS, Android)
- **Video Codec Support**: H.264/AAC (universal), H.265/HEVC (limited), VP9/Opus (Chrome/Firefox)

---

## License

This project is open-source and available under the MIT License. See the `LICENSE` file for details.
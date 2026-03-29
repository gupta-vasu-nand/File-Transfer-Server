package org.vng.filetransferserver.service

import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import jakarta.servlet.http.HttpServletRequest
import org.vng.filetransferserver.dto.MediaInfoDTO

interface StreamingService {
    fun streamMedia(filename: String, request: HttpServletRequest): ResponseEntity<Resource>
    fun getMediaInfo(filename: String): MediaInfoDTO
    fun getThumbnail(filename: String): Resource?
    fun getPreview(filename: String): Resource?
}
package org.vng.filetransferserver.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

interface StreamingService {
    fun streamMedia(path: String, request: HttpServletRequest, response: HttpServletResponse)
    fun getMediaInfo(path: String): Map<String, Any?>
    fun listMediaFiles(): List<Map<String, Any?>>
}
package io.github.zerumi.is1_250924_12060.model

import org.springframework.core.io.InputStreamResource
import java.io.Serializable
import java.time.ZonedDateTime

data class FileResponse(
    var filename: String,
    var contentType: String,
    var fileSize: Long,
    var createdTime: ZonedDateTime,
    var stream: InputStreamResource,
) : Serializable

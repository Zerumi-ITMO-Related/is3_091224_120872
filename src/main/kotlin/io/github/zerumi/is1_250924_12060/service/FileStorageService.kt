package io.github.zerumi.is1_250924_12060.service

import com.jlefebure.spring.boot.minio.MinioService
import io.github.zerumi.is1_250924_12060.model.FileResponse
import io.minio.StatObjectResponse
import jakarta.transaction.Transactional
import org.springframework.core.io.InputStreamResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.nio.file.Path

@Service
class FileStorageService(
    val minioService: MinioService,
) {
    @Transactional
    fun addFile(file: MultipartFile, filename: String): StatObjectResponse {
        val path = Path.of(filename)
        minioService.upload(path, file.inputStream, file.contentType)
        val metadata = minioService.getMetadata(path)
        return metadata
    }

    fun deleteFile(filename: String) {
        val path = Path.of(filename)
        val metadata = minioService.getMetadata(path)
        minioService.remove(path)
    }

    fun getFile(filename: String): FileResponse {
        val path = Path.of(filename)
        val metadata = minioService.getMetadata(path)

        val inputStream: InputStream = minioService.get(path)
        val inputStreamResource = InputStreamResource(inputStream)

        return FileResponse(
            filename = metadata.`object`(),
            fileSize = metadata.size(),
            contentType = metadata.contentType(),
            createdTime = metadata.lastModified(),
            stream = inputStreamResource
        )
    }

    fun getFileDetails(fileName: String): StatObjectResponse {
        val path = Path.of(fileName)
        val metadata = minioService.getMetadata(path)
        return metadata
    }
}

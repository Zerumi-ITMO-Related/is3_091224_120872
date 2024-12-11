package io.github.zerumi.is1_250924_12060.controller

import io.github.zerumi.is1_250924_12060.service.FileStorageService
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/file")
class FileController(
    val fileStorageService: FileStorageService,
) {
    @GetMapping("/{name}")
    @ResponseStatus(HttpStatus.OK)
    fun getFile(@PathVariable name: String): ResponseEntity<InputStreamResource> {
        val file = fileStorageService.getFile(name)
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(file.fileSize)
            .header("Content-disposition", "attachment; filename=" + file.filename).body(file.stream);
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFile(@PathVariable name: String) = fileStorageService.deleteFile(name)
}
package io.github.zerumi.is1_250924_12060.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.jlefebure.spring.boot.minio.MinioHealthIndicator
import io.github.zerumi.is1_250924_12060.controller.ModelController
import io.github.zerumi.is1_250924_12060.dto.HumanBeingDTO
import io.github.zerumi.is1_250924_12060.dto.HumanBeingFullDTO
import io.github.zerumi.is1_250924_12060.entity.ImportLogEntryEntity
import io.github.zerumi.is1_250924_12060.model.ImportLogEntry
import io.github.zerumi.is1_250924_12060.model.UserModel
import io.github.zerumi.is1_250924_12060.repository.ImportLogRepository
import io.github.zerumi.is1_250924_12060.repository.ModelRepository
import io.github.zerumi.is1_250924_12060.repository.UserRepository
import io.github.zerumi.is1_250924_12060.validation.ModelValidator
import jakarta.transaction.Transactional
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.Status
import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.time.ZonedDateTime

@Service
class MassModelImportService(
    val modelController: ModelController,
    val modelService: ModelService,
    val modelRepository: ModelRepository,
    val modelValidator: ModelValidator,
    val importLogRepository: ImportLogRepository,
    val userRepository: UserRepository,
    val userService: UserService,
    val fileService: FileStorageService,
    val minioHealthIndicator: MinioHealthIndicator,
) {
    @Transactional(dontRollbackOn = [IllegalArgumentException::class])
    fun processInput(file: MultipartFile, user: UserModel): List<HumanBeingFullDTO> {
        val filename = getRandomString(10) + (file.originalFilename ?: "file")
        try {
            require(prepareDB())
            require(prepareMinIOStorage())

            fileService.addFile(file, filename)
            val dtos = decodeHumanBeingsFromYaml(file.inputStream)
            val entities =
                dtos.map { modelController.convertToModel(it, user) }.map { modelService.convertToEntity(it) }
            if (modelValidator.validateModel(entities)) {
                val result = modelRepository.saveAll(entities)
                writeSuccessfulLog(user, entities.size, filename)
                return result.map { modelService.convertToModel(it) }.map { modelController.convertToDto(it) }
            } else throw IllegalArgumentException("Unique coordinates count exceeded!")
        } catch (e: Throwable) {
            writeUnsuccessfulLog(user, filename)
            throw e
        }
    }

    fun prepareMinIOStorage(): Boolean = minioHealthIndicator.health().status == Status.UP
    fun prepareDB(): Boolean = importLogRepository.count() >= 0

    fun decodeHumanBeingsFromYaml(inputStream: InputStream): List<HumanBeingDTO> {
        val objectMapper = ObjectMapper(YAMLFactory()).registerModule(
            KotlinModule.Builder()
                .configure(KotlinFeature.NullToEmptyCollection, true)
                .configure(KotlinFeature.NullToEmptyMap, true)
                .configure(KotlinFeature.NullIsSameAsDefault, true)
                .configure(KotlinFeature.SingletonSupport, true)
                .configure(KotlinFeature.StrictNullChecks, true)
                .build()
        )
        return objectMapper.readValue(inputStream)
    }

    fun getLog(user: UserModel): List<ImportLogEntry> {
        return if (user.authorities.contains(GrantedAuthority { "ADMIN" })) {
            importLogRepository.findAll().map { convertToModel(it) }
        } else {
            importLogRepository.findAllByUserEntity(userRepository.getReferenceById(user.id))
                .map { convertToModel(it) }
        }
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    fun writeSuccessfulLog(user: UserModel, count: Int, filename: String) {
        importLogRepository.save(
            ImportLogEntryEntity(
                userEntity = userRepository.getReferenceById(user.id),
                successful = true,
                importedModels = count,
                timestamp = ZonedDateTime.now(),
                filename = filename
            )
        )
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    fun writeUnsuccessfulLog(user: UserModel, filename: String) {
        importLogRepository.save(
            ImportLogEntryEntity(
                userEntity = userRepository.getReferenceById(user.id),
                successful = false,
                importedModels = 0,
                timestamp = ZonedDateTime.now(),
                filename = filename
            )
        )
    }

    fun convertToModel(importLogEntryEntity: ImportLogEntryEntity): ImportLogEntry = ImportLogEntry(
        id = importLogEntryEntity.id ?: -1,
        user = userService.convertEntityToModel(importLogEntryEntity.userEntity),
        successful = importLogEntryEntity.successful,
        importedCount = importLogEntryEntity.importedModels,
        timestamp = importLogEntryEntity.timestamp,
        filename = importLogEntryEntity.filename
    )

    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}

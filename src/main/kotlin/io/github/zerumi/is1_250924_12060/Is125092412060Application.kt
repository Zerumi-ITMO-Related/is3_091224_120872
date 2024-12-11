package io.github.zerumi.is1_250924_12060

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScans

@SpringBootApplication
@ComponentScans(value = [ComponentScan("io.github.zerumi.is1_250924_12060"),
    ComponentScan("com.jlefebure.spring.boot.minio")])
class Is125092412060Application

fun main(args: Array<String>) {
    runApplication<Is125092412060Application>(*args)
}

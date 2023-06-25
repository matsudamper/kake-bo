package net.matsudamper.money.backend.base

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

object CustomLogger {
    object ParseFail {
        private val logger = LoggerFactory.getLogger("ParseFail")!!

        fun log(clazz: KClass<*>, info: String) {
            logger.info("[$clazz]: $info")
        }
    }
    object General {
        private val logger = LoggerFactory.getLogger("General")!!

        fun info(message: String) {
            logger.info(message)
        }
        fun debug(message: String) {
            logger.debug(message)
        }
    }
}

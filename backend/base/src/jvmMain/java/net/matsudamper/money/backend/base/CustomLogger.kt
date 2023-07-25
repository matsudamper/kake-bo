package net.matsudamper.money.backend.base

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

public object CustomLogger {
    public object ParseFail {
        private val logger = LoggerFactory.getLogger("ParseFail")!!

        public fun log(clazz: KClass<*>, info: String) {
            logger.info("[$clazz]: $info")
        }
    }
    public object General {
        private val logger = LoggerFactory.getLogger("General")!!

        public fun info(message: String) {
            logger.info(message)
        }
        public fun debug(message: String) {
            logger.debug(message)
        }
    }
}

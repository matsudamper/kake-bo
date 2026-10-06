package net.matsudamper.money.backend.base

import kotlin.reflect.KClass
import org.slf4j.LoggerFactory

public object CustomLogger {
    public object ParseFail {
        private val logger = LoggerFactory.getLogger(ParseFail::class.java)!!

        public fun log(
            clazz: KClass<*>,
            info: String,
        ) {
            logger.info("[$clazz]: $info")
        }
    }

    public object General {
        private val logger = LoggerFactory.getLogger(General::class.java)!!

        public fun info(message: String) {
            logger.info(message)
        }

        public fun debug(message: String) {
            logger.debug(message)
        }
    }
}

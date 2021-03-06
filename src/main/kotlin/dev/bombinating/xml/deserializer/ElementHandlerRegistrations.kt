/*
 * Copyright 2020 Andrew Geery
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.bombinating.xml.deserializer

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Implementation of [HandlerRegistrations].
 *
 * @param T type of the object the handlers are populating
 * @param namespaceAware whether namespaces should be taken into account when finding handlers
 */
class ElementHandlerRegistrations<T>(private val namespaceAware: Boolean) : HandlerRegistrations<T> {
    private val handlers: MutableMap<XmlElementRegistration, HandlerContext<T>.() -> Unit> = mutableMapOf()

    /**
     * Maps the XML element name to the handler
     *
     * @receiver name of the XML element to associate with the handler
     * @param handler handler for processing the XML element
     */
    operator fun String.invoke(handler: HandlerContext<T>.() -> Unit) {
        logger.debug { "Adding a handler for element name '$this' with no namespace" }
        handlers[XmlElementRegistration(name = this)] = handler
    }

    operator fun XmlElementName.invoke(handler: HandlerContext<T>.() -> Unit) {
        logger.debug { "Adding a handler for element name '$name' with namespace '$namespace'" }
        handlers[XmlElementRegistration(namespace = namespace, name = name)] = handler
    }

    override operator fun invoke(context: HandlerContext<T>): Boolean {
        val key = XmlElementRegistration(context.element.name, namespaceAware)
        val registeredHandler = handlers[key]
        return registeredHandler?.let { handler ->
            try {
                logger.debug { "Handler found for element '$key'" }
                handler(context)
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                logger.warn { "Exception invoking handler for element name '$key': $e" }
                val ex = ProcessingException(type = ProcessingType.Handler, element = context.element, cause = e)
                context.config.processingExceptionHandler?.let {
                    logger.debug { "Invoking processing exception handler" }
                    it.invoke(ex)
                } ?: throw ex.also {
                    logger.debug("No processing exception handler defined")
                }
            }
            true
        } ?: false.also { logger.debug { "No handler registered for element name '$key'" } }
    }

}

/**
 * Creates [HandlerRegistrations] for type [T]
 *
 * @param T type of object the handlers are populating
 * @param namespaceAware whether namespaces should be taken into account when finding handlers
 * @param initializer lambda for defining handlers
 * @return [HandlerRegistrations] created by the [initializer]
 */
fun <T> handlers(
    namespaceAware: Boolean = false,
    initializer: ElementHandlerRegistrations<T>.() -> Unit
): HandlerRegistrations<T> =
    ElementHandlerRegistrations<T>(namespaceAware).apply {
        logger.debug { "Starting to register handlers" }
        initializer(this)
        logger.debug { "Finished registering handlers" }
    }


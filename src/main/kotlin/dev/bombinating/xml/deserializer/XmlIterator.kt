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
import java.io.Reader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.XMLEvent

private val logger = KotlinLogging.logger {}

/**
 * Lazily parses the XML data in the [Reader] and returns a [Sequence] of objects of type [T].
 *
 * @receiver [Reader] over an XML document
 * @param T type of the objects in the [Sequence]
 * @param handlers instructions about how to parse XML elements
 * @param config optional configuration of the XML parser with regard to missing handlers, etc.
 * @param factory optional lambda for specifying how to create instances of type [T].
 * If not specified, reflection will be used to invoke the no-args constructor.
 * @return [Sequence] of objects of type [T]
 */
inline fun <reified T> Reader.parse(
    handlers: HandlerRegistrations<T>,
    config: XmlContextParserConfig = XmlContextParserConfig(),
    noinline factory: () -> T = { T::class.java.newInstance() }
): Sequence<T> = XmlIterator(this, handlers, config, factory).asSequence()

/**
 * [Iterator] of type [T] created by parsing the XML data in the [Reader] using the [HandlerRegistrations] information.
 * Objects of type [T] are created using the [factory] lambda.
 * Lambdas for handling various situations in the XML processing (e.g., no handler registered for a given element)
 * are specified in the [XmlContextParserConfig] object.
 *
 * @param T type of objects the [Iterator] emits
 * @param reader [Reader] containing XML data
 * @param handlers instructions for how to parse XML elements
 * @param config optional configuration of the XML parser with regard to missing handlers, etc.
 * @param factory lambda for specifying how to create instances of type [T].
 */
@PublishedApi
internal class XmlIterator<T>(
    reader: Reader,
    private val handlers: HandlerRegistrations<T>,
    private val config: XmlContextParserConfig = XmlContextParserConfig(),
    private val factory: () -> T
) : Iterator<T> {

    private val eventReader: DepthAwareXmIterator
    private var nextResult: T? = null
    private var hasMore: Boolean? = null
    private var startDepth: Int? = null

    init {
        try {
            eventReader = DepthAwareXMLEventReader(XMLInputFactory.newInstance().createXMLEventReader(reader))
        } catch (e: XMLStreamException) {
            logger.error("Error reading XML: $e")
            throw InvalidXmlException(e)
        }
    }

    private fun computeNext(): T? {
        nextResult = null
        while (eventReader.hasNext()) {
            val e = try {
                eventReader.next()
            } catch (e: XMLStreamException) {
                logger.warn("Error reading next XML event: $e")
                throw InvalidXmlException(e)
            }
            if (handleEvent(e)) {
                break
            }
        }
        hasMore = nextResult != null
        return nextResult
    }

    fun handleEvent(e: XMLEvent): Boolean =
        if (e.isStartElement && (startDepth == null || eventReader.depth == startDepth)) {
            val startElement = e.asStartElement()
            logger.debug { "Next start element: $startElement" }
            val element = StartElementXmlElement(startElement, eventReader::text)
            val t = try {
                factory()
            } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
                logger.error { "Error invoking the object factory: $e" }
                val ex = ProcessingException(type = ProcessingType.Factory, element = element, cause = e)
                config.processingExceptionHandler?.invoke(ex)
                    ?: throw ProcessingException(ProcessingType.Factory, element, e)
                null
            }
            if (t != null) {
                logger.debug { "Searching for handler for element name '${element.name.name}'" }
                val parentDepth = eventReader.depth
                if (handlers.invoke(
                        HandlerContextParserConfig(
                            WrappedDepthAwareXmIterator(eventReader), element, t, config
                        )
                    )
                ) {
                    logger.debug { "Element successfully processed" }
                    startDepth = parentDepth
                    nextResult = t
                    true
                } else {
                    logger.debug { "Element not handled" }
                    config.missingElementHandler?.invoke(element)
                    false
                }
            } else {
                false
            }
        } else {
            false
        }

    override fun hasNext(): Boolean = hasMore ?: computeNext() != null

    override fun next(): T = (nextResult ?: computeNext())?.also { hasMore = null }
        ?: throw NoSuchElementException()

}

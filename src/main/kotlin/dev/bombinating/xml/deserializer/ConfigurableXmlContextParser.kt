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
 * Implementation of [XmlContextParser]
 *
 * @param T type of objects the [Iterator] emits
 * @param reader source of the XML data
 * @param config configuration of how the XML is parsed
 */
internal class ConfigurableXmlContextParser<T>(
    private val reader: DepthAwareXmIterator,
    private val config: XmlContextParserConfig
) : XmlContextParser<T> {
    override fun <T> parse(element: XmlElement, handlers: HandlerRegistrations<T>, factory: () -> T): T {
        val t = factory()
        while (reader.hasNext()) {
            val e = reader.next()
            if (e.isStartElement) {
                logger.debug { "Start element: ${e.asStartElement()}, depth: ${reader.depth}" }
                val xmlElement = StartElementXmlElement(e.asStartElement(), reader::text)
                handlers(HandlerContextParserConfig(WrappedDepthAwareXmIterator(reader), xmlElement, t, config))
            } else if (e.isEndElement) {
                val endElement = e.asEndElement()
                logger.debug { "End element: $endElement, depth: ${reader.depth}" }
                if (reader.depth < 0) {
                    logger.debug { "Finished $endElement subtree - the depth is < 0>" }
                    break
                }
            }
        }
        return t
    }
}

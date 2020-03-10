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

import javax.xml.stream.XMLEventReader

/**
 * Implementation of []HandlerContext] based on [XMLEventReader]
 *
 * @param reader XML reader for the data
 * @param element XML element being handled
 * @param obj object being populated by the handlers
 * @param config XML parsing configuration
 */
data class HandlerContextParserConfig<T>(
    private val reader: DepthAwareXmIterator,
    override val element: XmlElement,
    override val obj: T,
    override val config: XmlContextParserConfig
) : HandlerContext<T> {
    override fun <P> createParser(): XmlContextParser<P> = ConfigurableXmlContextParser(reader, config)
}

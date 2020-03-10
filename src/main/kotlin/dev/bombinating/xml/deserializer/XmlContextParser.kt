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

/**
 * Parser for a chunk of XML data.
 */
interface XmlContextParser<T> {
    /**
     * Converts the [element] into an object of type [T] using the [handlers] and [factory].
     *
     * @param element the XML element to parse
     * @param handlers definition of how to handle XML elements
     * @param factory lambda for creating an object of type [T]
     * @return object of type [T] created by the [factory] and populated by the [handlers]
     */
    fun <T> parse(element: XmlElement, handlers: HandlerRegistrations<T>, factory: () -> T): T
}

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
import javax.xml.stream.events.XMLEvent

/**
 * Implementation of [DepthAwareXmIterator] using [XMLEventReader].
 *
 * @param reader [XMLEventReader] used to implement the [DepthAwareXmIterator] interface
 */
class DepthAwareXMLEventReader(private val reader: XMLEventReader) : DepthAwareXmIterator {
    private var _depth: Int = 0

    override val depth: Int
        get() = _depth

    override val text: String
        get() {
            _depth--
            return reader.elementText
        }

    override fun hasNext(): Boolean = reader.hasNext() && _depth >= 0

    override fun next(): XMLEvent {
        val event = reader.nextEvent()
        when {
            event.isStartElement -> {
                _depth++
            }
            event.isEndElement -> {
                _depth--
            }
        }
        return event
    }
}

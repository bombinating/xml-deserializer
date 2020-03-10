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

import javax.xml.namespace.QName
import javax.xml.stream.events.StartElement

/**
 * Implementation of [XmlElement] based on a [StartElement] and a lambda for determining the text value.
 *
 * @param startElement [StartElement] the [XmlElement] is based on
 * @param textLambda lambda for getting the text value of the element
 */
internal class StartElementXmlElement(
    private val startElement: StartElement,
    private val textLambda: () -> String
) : XmlElement {
    override val name: XmlElementName by lazy { QNameXmlName(startElement.name) }
    override val text: String by lazy { textLambda() }
    override fun get(key: String): String? = startElement.getAttributeByName(QName(key))?.value
}

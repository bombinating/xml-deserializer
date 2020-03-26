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

/**
 * [XmlElementName] implementation based on [QName]
 *
 * @param qname [QName] object the [XmlElementName] is based on
 */
internal class QNameXmlName(private val qname: QName) : XmlElementName {

    val prefix: String?
        get() = qname.prefix

    override val namespace: String?
        get() = qname.namespaceURI

    override val name: String
        get() = qname.localPart

}

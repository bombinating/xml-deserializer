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
 * Information related to an XML element.
 */
interface XmlElement {
    /**
     * Name of the XML element (e.g., "Person" in `<Person>...</Person>`)
     */
    val name: XmlElementName

    /**
     *  Text value of the XML element (e.g., "Fred" in `<Person>Fred</Person>`)
     */
    val text: String

    /**
     * Return the attribute value associated with the [key]
     *
     * @param key attribute name
     * @return attribute value (null if the attribute does not exist)
     */
    operator fun get(key: String): String?
}

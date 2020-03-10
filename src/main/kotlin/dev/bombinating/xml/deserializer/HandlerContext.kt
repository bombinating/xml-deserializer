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
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty1

private val logger = KotlinLogging.logger {}

/**
 * Context for parsing XML using an object of type [T].
 *
 * @param T type of the object being populated by the handler
 */
interface HandlerContext<T> {
    /**
     * The XML element being processed
     */
    val element: XmlElement

    /**
     * The object being populated by the handler
     */
    val obj: T

    /**
     * XML parser configuration
     */
    val config: XmlContextParserConfig

    /**
     * Create a parser of type <P>
     */
    fun <P> createParser(): XmlContextParser<P>
}

/**
 * Convenience property for getting the text of the current element.
 */
val HandlerContext<*>.text: String
    get() = element.text

/**
 * Specify how a given context is to be handled.
 *
 * @receiver [HandlerContext] that holds the object being populated and the XML being processed
 * @param T type of the object being populated by the handler
 * @param handlers how to handle XML elements
 * @param initializer how to initialize an already-created object of type [T]
 * @return an object of type [T] populated from the XML data
 */
fun <T> HandlerContext<T>.use(handlers: HandlerRegistrations<T>, initializer: (T.() -> Unit)? = null): T =
    createParser<T>().parse(element, handlers) {
        initializer?.invoke(obj)
        obj
    }

/**
 * Parses the [HandlerContext] to an object of type [P] populated from the XML data.
 *
 * @receiver [HandlerContext] context from which the new parser is being created
 * @param P type of the object to create
 * @param handlers how to handle XML elements
 * @param factory lambda for creating an object of type P
 */
inline fun <reified P> HandlerContext<*>.parse(
    handlers: HandlerRegistrations<P>,
    noinline factory: () -> P = { P::class.java.newInstance() }
): P = createParser<P>().parse(element, handlers, factory)

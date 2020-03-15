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
 * If the [nullCond] lambda returns true, returns null; otherwise, returns the object.
 *
 * @receiver object to invoke the [nullCond] lambda on
 * @param nullCond lambda that takes the @receiver and returns whether to return null
 * @return if the [nullCond] lambda returns true, null; otherwise the @receiver
 */
fun <T> T.nullIf(nullCond: (T) -> Boolean): T? = if (nullCond(this)) null else this

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

data class Address(
    var type: AddressType? = null,
    var street1: String? = null,
    var startMonth: Int? = null,
    var endMonth: Int? = null,
    var state: Locality? = null,
    var country: Locality? = null
) {
    companion object {
        val handlers =
            handlers<Address> {
                "Street1" { obj.street1 = text }
                "State" {
                    obj.state = parse(Locality.handlers)
                }
                "Country" {
                    obj.country = parse(Locality.handlers)
                }
            }
    }

    val empty: Boolean
        get() = (type == null) && (street1 == null) && (startMonth == null)
                && (endMonth == null) && (state == null) && (country == null)

}
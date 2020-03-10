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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val logger = KotlinLogging.logger {}

data class Phone(
    var desc: String? = null,
    var number: String? = null,
    var ext: String? = null
) {
    companion object {
        val handlers = handlers<Phone> {
            "PhoneNumber" { obj.number = text }
            "PhoneExtension" { obj.ext = text }
        }
    }
}

enum class AddressType(val xmlValue: String) {
    Permanent("permanent"),
    Vacation("vacation");

    companion object {
        private val map = values().map { it.xmlValue to it }.toMap()
        operator fun get(xmlValue: String?) = xmlValue?.let {
            map[it] ?: throw RuntimeException("XML value '$xmlValue' not found")
        }
    }
}

data class Address(
    var type: AddressType? = null,
    var street1: String? = null,
    var startMonth: Int? = null,
    var endMonth: Int? = null
) {
    companion object {
        val handlers = handlers<Address> {
            "Street1" { obj.street1 = text }
        }
    }
}

data class Person(
    var firstName: String? = null,
    var lastName: String? = null,
    var age: Int? = null,
    var address: Address? = null,
    var phones: MutableList<Phone>? = null,
    var height: Int? = null
) {
    companion object {
        val handlers = handlers<Person> {
            "FirstName" { obj.firstName = text }
            "LastName" { obj.lastName = text }
            "Height" { obj.height = text.toInt() }
            "Address" {
                obj.address = parse(Address.handlers) {
                    Address(
                        type = element["type"]?.let { AddressType[it] },
                        startMonth = element["start"]?.toInt(),
                        endMonth = element["end"]?.toInt()
                    )
                }
            }
            "Phone" {
                obj.addPhone(parse(Phone.handlers) {
                    Phone(desc = element["description"])
                })
            }
        }
    }

    fun addPhone(phone: Phone) {
        if (phones == null) {
            phones = mutableListOf()
        }
        phones?.add(phone)
    }

}

class XmlIteratorTest {

    @Test
    fun `invalid xml`() {
        assertThrows<InvalidXmlException> { "bad".reader().parse(handlers<Person> {}).toList() }
    }

    @Test
    fun `handler attribute processor conversion exception`() {
        val reader = """
            |<People>
            |   <Person age="old"/>
            |</People>
        """.trimMargin().reader()
        val ex = assertThrows<ProcessingException> {
            reader.parse(handlers<Person> {
                "Person" {
                    use(Person.handlers) {
                        age = element["age"]?.toInt()
                    }
                }
            }).toList()
        }
        assertEquals(ProcessingType.Handler, ex.type)
    }

    @Test
    fun `handler element processor conversion exception`() {
        val reader = """
            |<People>
            |   <Person>
            |       <Height>Tall</Height>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val ex = assertThrows<ProcessingException> {
            reader.parse(handlers<Person> {
                "Person" { use(Person.handlers) }
            }).toList()
        }
        assertEquals(ProcessingType.Handler, ex.type)
    }

    @Test
    fun `factory throws exception`() {
        val reader = "<People></People>".reader()
        val ex = assertThrows<ProcessingException> {
            reader.parse(handlers<Person> {
                "Person" { use(Person.handlers) }
            }) { throw RuntimeException("Exception creating Person object") }.toList()
        }
        assertEquals(ProcessingType.Factory, ex.type)
    }

    @Test
    fun `continue processing after exception in factory`() {
        val firstName = "Jane"
        val reader = """
            |<People>
            |   <Person>
            |       <FirstName>Paul</FirstName>
            |   </Person>
            |   <Person>
            |       <FirstName>$firstName</FirstName>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val config = XmlContextParserConfig(processingExceptionHandler = {
            logger.error { "Factory error on element '${element.name}': ${this.cause}" }
        })
        var count = 0
        val factory: () -> Person = {
            if (count++ == 1) {
                throw RuntimeException("Exception on creating first instance")
            } else {
                Person()
            }
        }
        val people = reader.parse(handlers<Person> {
            "Person" { use(Person.handlers) }
        }, config, factory).toList()
        assertEquals(1, people.size)
        assertEquals(firstName, people[0].firstName)
    }

    @Test
    fun `continue processing after exception in handler`() {
        val firstName = "Paul"
        val reader = """
            |<People>
            |   <Person>
            |       <FirstName>$firstName</FirstName>
            |       <Height>Tall</Height>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val config = XmlContextParserConfig(processingExceptionHandler = {
            logger.error { "Error parsing element '${element.name}': ${this.cause}" }
        })
        val people = reader.parse(handlers<Person> {
            "Person" { use(Person.handlers) }
        }, config).toList()
        val person = if (people.isNotEmpty()) people[0] else null
        assertEquals(firstName, person?.firstName)
        assertNull(person?.height)
    }

    @Test
    fun `read non-existent attribute name`() {
        val firstName = "Fred"
        val reader = """
            |<People>
            |   <Person>
            |       <FirstName>$firstName</FirstName>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val people = reader.parse(handlers<Person> {
            "Person" {
                use(Person.handlers) {
                    Person(lastName = element["blah"])
                }
            }
        }).toList()
        assertEquals(1, people.size)
        val person = people[0]
        assertEquals(firstName, person.firstName)
    }

    @Test
    fun `missing element handler invoked`() {
        val elementName = "People"
        val reader = "<$elementName></$elementName>".reader()
        val config = XmlContextParserConfig(
            missingElementHandler = { throw RuntimeException("Missing element '${name.name}'") }
        )
        val ex = assertThrows<RuntimeException> {
            reader.parse(handlers<Person> {
                "Person" { use(Person.handlers) }
            }, config = config).toList()
        }
        assertEquals("Missing element '$elementName'", ex.localizedMessage)
    }

    @Test
    fun `process xml element text`() {
        val firstName = "Fred"
        val lastName = "Smith"
        val reader = """
            |<People>
            |   <Person>
            |       <FirstName>$firstName</FirstName>
            |       <LastName>$lastName</LastName>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val people = reader.parse(handlers<Person> {
            "Person" { use(Person.handlers) }
        }).toList()
        assertEquals(1, people.size)
        val person = people[0]
        assertEquals(firstName, person.firstName)
        assertEquals(lastName, person.lastName)
    }

    @Test
    fun `empty element text`() {
        val firstName = "Fred"
        val lastName = ""
        val reader = """
            |<People>
            |   <Person>
            |       <FirstName>$firstName</FirstName>
            |       <LastName>$lastName</LastName>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val handlers = handlers<Person> {
            "Person" { use(Person.handlers) }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        val person = people[0]
        assertEquals(firstName, person.firstName)
        assertEquals(lastName, person.lastName)
    }

    @Test
    fun `start end element`() {
        val firstName = "Fred"
        val reader = """
            |<People>
            |   <Person>
            |       <FirstName>$firstName</FirstName>
            |       <LastName/>
            |   </Person>
            |</People>
        """.trimMargin().reader()

        val handlers = handlers<Person> {
            "Person" { use(Person.handlers) }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        val person = people[0]
        assertEquals(firstName, person.firstName)
        assertEquals("", person.lastName)
    }

    @Test
    fun `process xml attribute`() {
        val personAge = 10
        val reader = """
            |<People>
            |   <Person age="$personAge">
            |   </Person>
            |</People>
        """.trimMargin().reader()

        val handlers = handlers<Person> {
            "Person" {
                use(Person.handlers) {
                    age = element["age"]?.toInt()
                }
            }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        val person = people[0]
        assertEquals(personAge, person.age)
    }

    @Test
    fun `single nested handlers`() {
        val addressType = AddressType.Vacation
        val startMonth = 1
        val street1 = "100 Main St"
        val reader = """
            |<People>
            |   <Person>
            |       <Address type="${addressType.xmlValue}" start="$startMonth">
            |           <Street1>$street1</Street1>
            |       </Address>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val handlers = handlers<Person> {
            "Person" {
                use(Person.handlers)
            }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        val person = people[0]
        assertEquals(addressType, person.address?.type)
        assertEquals(street1, person.address?.street1)
        assertEquals(startMonth, person.address?.startMonth)
    }

    @Test
    fun `multiple nested elements`() {
        val phone1 = "111-222-3333"
        val ext1 = "1234"
        val phone2 = "444-555-6666"
        val ext2 = "5678"
        val reader = """
            |<People>
            |   <Person>
            |       <Phone description="after hours">
            |           <PhoneNumber>$phone1</PhoneNumber>
            |           <PhoneExtension>$ext1</PhoneExtension>
            |       </Phone>
            |       <Phone description="business hours">
            |           <PhoneNumber>$phone2</PhoneNumber>
            |           <PhoneExtension>$ext2</PhoneExtension>
            |       </Phone>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val handlers = handlers<Person> {
            "Person" {
                use(Person.handlers)
            }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        val person = people[0]
        assertEquals(2, person.phones?.size)
        assertEquals(phone1, person.phones?.get(0)?.number)
        assertEquals(ext1, person.phones?.get(0)?.ext)
        assertEquals(phone2, person.phones?.get(1)?.number)
        assertEquals(ext2, person.phones?.get(1)?.ext)
    }

    @Test
    fun `unhandled child element with same name as parent`() {
        val person1Name = "Test1"
        val person2Name = "Test2"
        val reader = """
            |<People>
            |   <Person>
            |       <FirstName>$person1Name</FirstName>
            |       <Children>
            |           <Person>
            |               <FirstName>Child1</FirstName>
            |           </Person>
            |       </Children>
            |   </Person>
            |   <Person>
            |       <FirstName>$person2Name</FirstName>
            |   </Person>
            |</People>
        """.trimMargin().reader()
        val handlers = handlers<Person> {
            "FirstName" { obj.firstName = text }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(2, people.size)
        assertEquals(person1Name, people[0].firstName)
        assertEquals(person2Name, people[1].firstName)
    }

}

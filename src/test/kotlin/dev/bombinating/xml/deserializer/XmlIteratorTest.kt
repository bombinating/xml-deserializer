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
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val logger = KotlinLogging.logger {}

private const val ns1: String = "http://example1.com"
private const val ns2: String = "http://example2.com"

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
        assertNull(person.address)
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
    fun `doubly nested handlers`() {
        val stateCode = "MN"
        val stateDesc = "Minnesota"
        val countryCode = "US"
        val countryDesc = "United States"
        val reader = """
            |<People>
            |   <Person>
            |       <Address>
            |           <State>
            |               <Code>$stateCode</Code>
            |               <Desc>$stateDesc</Desc>
            |           </State>
            |           <Country>
            |               <Code>$countryCode</Code>
            |               <Desc>$countryDesc</Desc>
            |           </Country>
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
        assertEquals(stateCode, person.address?.state?.code)
        assertEquals(countryCode, person.address?.country?.code)
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
            |       <Address>
            |           <Street1>100 Main St</Street1>
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
        assertEquals(2, person.phones.size)
        val phone1Info = person.phones[0]
        val phone2Info = person.phones[1]
        assertEquals(phone1, phone1Info.number)
        assertEquals(ext1, phone1Info.ext)
        assertEquals(phone2, phone2Info.number)
        assertEquals(ext2, phone2Info.ext)
        assertNotNull(person.address)
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

    @Test
    fun `no handler for subtree`() {
        val title = "Test #1"
        val tag = "Test Tag"
        val author = "Test Author"
        val status = "Published"
        val reader = """
            |<Posts>
            |   <Post>
            |       <Title>$title</Title>
            |       <Metadata>
            |           <Status>$status</Status>
            |           <Tags>
            |               <Tag>$tag</Tag>
            |           </Tags>
            |           <Author>$author</Author>
            |       </Metadata>
            |   </Post>
            |</Posts>
        """.trimMargin().reader()
        val handlers = handlers<Post> {
            "Posts" { use(Post.handlers) }
        }
        val posts = reader.parse(handlers).toList()
        assertEquals(1, posts.size)
        val post = posts[0]
        assertEquals(title, post.title)
        assertEquals(status, post.metadata?.status)
        assertEquals(tag, post.metadata?.tags?.get(0))
        assertEquals(author, post.metadata?.author)
    }

    @Test
    fun `namespace aware with namespace handlers test`() {
        val person1Name = "Test1"
        val reader = """
            |<ns1:People xmlns:ns1="$ns1">
            |   <ns1:Person>
            |       <ns1:FirstName>$person1Name</ns1:FirstName>
            |   </ns1:Person>
            |</ns1:People>
        """.trimMargin().reader()
        val personHandlers = handlers<Person>(namespaceAware = true) {
            ns1["FirstName"] { obj.firstName = text }
        }
        val handlers = handlers<Person> {
            "Person" { use(personHandlers) }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        assertEquals(person1Name, people[0].firstName)
    }

    @Test
    fun `namespace aware with top level namespace handlers test`() {
        val person1Name = "Test1"
        val reader = """
            |<ns1:People xmlns:ns1="$ns1">
            |   <ns1:Person>
            |       <ns1:FirstName>$person1Name</ns1:FirstName>
            |   </ns1:Person>
            |</ns1:People>
        """.trimMargin().reader()
        val personHandlers = handlers<Person>(namespaceAware = true) {
            ns1["FirstName"] { obj.firstName = text }
        }
        val handlers = handlers<Person>(namespaceAware = true) {
            ns1["Person"] { use(personHandlers) }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        assertEquals(person1Name, people[0].firstName)
    }

    @Test
    fun `namespace aware without namespace handlers test`() {
        val person1Name = "Test1"
        val reader = """
            |<ns1:People xmlns:ns1="$ns1">
            |   <ns1:Person>
            |       <ns1:FirstName>$person1Name</ns1:FirstName>
            |   </ns1:Person>
            |</ns1:People>
        """.trimMargin().reader()
        val handlers = handlers<Person>(namespaceAware = true) {
            "FirstName" { obj.firstName = text }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(0, people.size)
    }

    @Test
    fun `namespaces without namespace aware parsing test`() {
        val person1Name = "Test1"
        val reader = """
            |<ns1:People xmlns:ns1="$ns1">
            |   <ns1:Person>
            |       <ns1:FirstName>$person1Name</ns1:FirstName>
            |   </ns1:Person>
            |</ns1:People>
        """.trimMargin().reader()
        val handlers = handlers<Person> {
            "FirstName" { obj.firstName = text }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        assertEquals(person1Name, people[0].firstName)
    }

    @Test
    fun `multiple namespaces aware parsing test`() {
        val person1FirstName = "First"
        val person1LastName = "Last"
        val reader = """
            |<ns1:People xmlns:ns1="$ns1" xmlns:ns2="$ns2">
            |   <ns1:Person>
            |       <ns1:FirstName>$person1FirstName</ns1:FirstName>
            |       <ns2:LastName>$person1LastName</ns2:LastName>
            |   </ns1:Person>
            |</ns1:People>
        """.trimMargin().reader()
        val personHandlers = handlers<Person>(namespaceAware = true) {
            ns1["FirstName"] { obj.firstName = text }
            ns2["LastName"] { obj.lastName = text }
        }
        val handlers = handlers<Person> {
            "Person" { use(personHandlers) }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        assertEquals(person1FirstName, people[0].firstName)
        assertEquals(person1LastName, people[0].lastName)
    }

    @Test
    fun `namespaces aware without namespace element parsing test`() {
        val person1FirstName = "First"
        val person1LastName = "Last"
        val reader = """
            |<ns1:People xmlns:ns1="$ns1" xmlns:ns2="$ns2">
            |   <ns1:Person>
            |       <ns1:FirstName>$person1FirstName</ns1:FirstName>
            |       <LastName>$person1LastName</LastName>
            |   </ns1:Person>
            |</ns1:People>
        """.trimMargin().reader()
        val personHandlers = handlers<Person>(namespaceAware = true) {
            ns1["FirstName"] { obj.firstName = text }
            ns1["LastName"] { obj.lastName = text }
        }
        val handlers = handlers<Person> {
            "Person" { use(personHandlers) }
        }
        val people = reader.parse(handlers).toList()
        assertEquals(1, people.size)
        assertEquals(person1FirstName, people[0].firstName)
        assertNull(people[0].lastName)
    }

}

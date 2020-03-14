package dev.bombinating.xml.deserializer

data class Person(
    var firstName: String? = null,
    var lastName: String? = null,
    var age: Int? = null,
    var address: Address? = null,
    val phones: MutableList<Phone> = mutableListOf(),
    var height: Int? = null
) {
    companion object {
        val handlers =
            handlers<Person> {
                "FirstName" { obj.firstName = text }
                "LastName" { obj.lastName = text }
                "Height" { obj.height = text.toInt() }
                "Address" {
                    obj.address =
                        parse(Address.handlers) {
                            Address(
                                type = element["type"]?.let { AddressType[it] },
                                startMonth = element["start"]?.toInt(),
                                endMonth = element["end"]?.toInt()
                            )
                        }
                }
                "Phone" {
                    obj.phones.add(parse(Phone.handlers) {
                        Phone(desc = element["description"])
                    })
                }
            }
    }

}
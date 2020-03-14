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
}
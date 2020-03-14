package dev.bombinating.xml.deserializer

data class Phone(
    var desc: String? = null,
    var number: String? = null,
    var ext: String? = null
) {
    companion object {
        val handlers =
            handlers<Phone> {
                "PhoneNumber" { obj.number = text }
                "PhoneExtension" { obj.ext = text }
            }
    }
}
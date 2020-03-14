package dev.bombinating.xml.deserializer

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
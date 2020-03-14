package dev.bombinating.xml.deserializer

data class Locality(var code: String? = null, var desc: String? = null) {
    companion object {
        val handlers =
            handlers<Locality> {
                "Code" { obj.code = text }
                "Desc" { obj.desc = text }
            }
    }
}
package dev.bombinating.xml.deserializer

data class Metadata(
    var author: String? = null,
    var status: String? = null,
    val tags: MutableList<String> = mutableListOf())
{
    companion object {
        val handlers =
            handlers<Metadata> {
                "Author" { obj.author = text }
                "Tag" { obj.tags.add(text) }
                "Status" { obj.status = text }
            }
    }
}
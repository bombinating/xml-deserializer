package dev.bombinating.xml.deserializer

data class Post(
    var title: String? = null,
    var metadata: Metadata? = null
) {
    companion object {
        val handlers = handlers<Post> {
            "Title" { obj.title = text }
            "Metadata" { obj.metadata = parse(Metadata.handlers) }
        }
    }
}
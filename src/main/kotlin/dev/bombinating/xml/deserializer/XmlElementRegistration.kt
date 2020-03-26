package dev.bombinating.xml.deserializer

data class XmlElementRegistration(
    override val namespace: String? = null,
    override val name: String
) : XmlElementName {
    constructor(source: XmlElementName, includeNamespace: Boolean)
            : this(namespace = if (includeNamespace) source.namespace else null, name = source.name)
}

operator fun String.get(name: String) = XmlElementRegistration(namespace = this, name = name)

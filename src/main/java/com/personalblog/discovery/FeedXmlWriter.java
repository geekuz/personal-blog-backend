package com.personalblog.discovery;

import com.personalblog.post.Post;
import com.personalblog.post.PublishedPostRef;
import com.personalblog.tag.Tag;
import java.io.StringWriter;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/** Renders the RSS 2.0 feed and XML sitemap. StAX handles escaping; characters XML 1.0 forbids are dropped. */
final class FeedXmlWriter {
    static final List<String> STATIC_PATHS = List.of(
        "/",
        "/projects",
        "/projects/otabek-dev",
        "/projects/java-load-balancer",
        "/projects/build-your-own-sort",
        "/about",
        "/contact"
    );

    private static final XMLOutputFactory FACTORY = XMLOutputFactory.newFactory();
    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
    private static final String ATOM_NS = "http://www.w3.org/2005/Atom";
    private static final String SITEMAP_NS = "http://www.sitemaps.org/schemas/sitemap/0.9";
    private static final Pattern INVALID_XML_CHARS =
        Pattern.compile("[^\\x09\\x0A\\x0D\\x20-\\x{D7FF}\\x{E000}-\\x{FFFD}\\x{10000}-\\x{10FFFF}]");

    private FeedXmlWriter() {}

    static String rss(SiteInfo site, List<Post> posts) {
        return write(xml -> {
            xml.writeStartElement("rss");
            xml.writeAttribute("version", "2.0");
            xml.writeNamespace("atom", ATOM_NS);
            xml.writeStartElement("channel");
            element(xml, "title", site.title());
            element(xml, "link", site.pageUrl("/"));
            element(xml, "description", site.description());
            element(xml, "language", "en");
            xml.writeEmptyElement("atom", "link", ATOM_NS);
            xml.writeAttribute("href", site.pageUrl("/feed.xml"));
            xml.writeAttribute("rel", "self");
            xml.writeAttribute("type", "application/rss+xml");
            if (!posts.isEmpty()) element(xml, "lastBuildDate", RFC_1123.format(posts.getFirst().getPublishedAt()));
            for (Post post : posts) item(xml, site, post);
            xml.writeEndElement();
            xml.writeEndElement();
        });
    }

    static String sitemap(SiteInfo site, List<PublishedPostRef> posts) {
        return write(xml -> {
            xml.writeStartElement("urlset");
            xml.writeDefaultNamespace(SITEMAP_NS);
            for (String path : STATIC_PATHS) {
                xml.writeStartElement("url");
                element(xml, "loc", site.pageUrl(path));
                xml.writeEndElement();
            }
            for (PublishedPostRef post : posts) {
                xml.writeStartElement("url");
                element(xml, "loc", site.postUrl(post.slug()));
                element(xml, "lastmod", ISO_DATE.format(post.updatedAt()));
                xml.writeEndElement();
            }
            xml.writeEndElement();
        });
    }

    private static void item(XMLStreamWriter xml, SiteInfo site, Post post) throws XMLStreamException {
        String url = site.postUrl(post.getSlug());
        xml.writeStartElement("item");
        element(xml, "title", post.getTitle());
        element(xml, "link", url);
        xml.writeStartElement("guid");
        xml.writeAttribute("isPermaLink", "true");
        xml.writeCharacters(url);
        xml.writeEndElement();
        element(xml, "description", post.getSummary());
        element(xml, "pubDate", RFC_1123.format(post.getPublishedAt()));
        for (Tag tag : post.getTags()) element(xml, "category", tag.getName());
        xml.writeEndElement();
    }

    private static void element(XMLStreamWriter xml, String name, String text) throws XMLStreamException {
        xml.writeStartElement(name);
        xml.writeCharacters(text == null ? "" : INVALID_XML_CHARS.matcher(text).replaceAll(""));
        xml.writeEndElement();
    }

    private static String write(XmlBody body) {
        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter xml = FACTORY.createXMLStreamWriter(out);
            xml.writeStartDocument("UTF-8", "1.0");
            body.write(xml);
            xml.writeEndDocument();
            xml.close();
        } catch (XMLStreamException ex) {
            throw new IllegalStateException("Could not render XML document", ex);
        }
        return out.toString();
    }

    @FunctionalInterface
    private interface XmlBody {
        void write(XMLStreamWriter xml) throws XMLStreamException;
    }
}

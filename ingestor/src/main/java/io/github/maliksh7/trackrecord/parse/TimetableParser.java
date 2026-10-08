package io.github.maliksh7.trackrecord.parse;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.github.maliksh7.trackrecord.parse.ParsedTimetable.Event;
import io.github.maliksh7.trackrecord.parse.ParsedTimetable.Message;
import io.github.maliksh7.trackrecord.parse.ParsedTimetable.Stop;
import io.github.maliksh7.trackrecord.parse.ParsedTimetable.TripLabel;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parses IRIS timetable XML with the JDK DOM parser. DOM (rather than a binding library) because
 * element order differs between endpoints (e.g. {@code <tl>} comes first in /plan but not in /fchg)
 * and many attributes are optional.
 */
@Component
public class TimetableParser {

    private final DocumentBuilderFactory factory;

    public TimetableParser() {
        factory = DocumentBuilderFactory.newInstance();
        try {
            // Third-party XML: refuse DTDs and external entities (XXE).
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setExpandEntityReferences(false);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    public ParsedTimetable parse(String xml) {
        return parse(xml, null);
    }

    /** /plan responses may omit the station EVA, so callers pass the one they asked for. */
    public ParsedTimetable parse(String xml, String fallbackEva) {
        Element root = document(xml);
        if (!"timetable".equals(root.getTagName())) {
            throw new IllegalArgumentException("Expected <timetable> root, got <" + root.getTagName() + ">");
        }
        String rootEva = attr(root, "eva") != null ? attr(root, "eva") : fallbackEva;
        List<Stop> stops = new ArrayList<>();
        for (Element s : children(root, "s")) {
            stops.add(stop(s, rootEva));
        }
        return new ParsedTimetable(rootEva, attr(root, "station"), List.copyOf(stops));
    }

    private Stop stop(Element s, String rootEva) {
        String eva = attr(s, "eva") != null ? attr(s, "eva") : rootEva;
        Element tl = firstChild(s, "tl");
        Element ar = firstChild(s, "ar");
        Element dp = firstChild(s, "dp");

        List<Message> messages = new ArrayList<>(messages(s, "s"));
        if (ar != null) messages.addAll(messages(ar, "ar"));
        if (dp != null) messages.addAll(messages(dp, "dp"));

        return new Stop(attr(s, "id"), eva,
                tl == null ? null : new TripLabel(attr(tl, "f"), attr(tl, "t"), attr(tl, "o"), attr(tl, "c"), attr(tl, "n")),
                event(ar), event(dp), List.copyOf(messages));
    }

    private static Event event(Element e) {
        if (e == null) {
            return null;
        }
        return new Event(
                IrisTime.parse(attr(e, "pt")), attr(e, "pp"), attr(e, "ppth"), attr(e, "l"),
                IrisTime.parse(attr(e, "ct")), attr(e, "cp"), attr(e, "cpth"), attr(e, "cs"));
    }

    private static List<Message> messages(Element parent, String scope) {
        List<Message> out = new ArrayList<>();
        for (Element m : children(parent, "m")) {
            out.add(new Message(attr(m, "id"), scope, attr(m, "t"), intAttr(m, "c"), attr(m, "cat"),
                    intAttr(m, "pr"), IrisTime.parse(attr(m, "ts"))));
        }
        return out;
    }

    private Element document(String xml) {
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e; // surface as an exception only; the JDK default also prints to stderr
                }
            });
            return builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("Unparseable timetable XML: " + e.getMessage(), e);
        }
    }

    private static List<Element> children(Element parent, String tag) {
        List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element el && tag.equals(el.getTagName())) {
                out.add(el);
            }
        }
        return out;
    }

    private static Element firstChild(Element parent, String tag) {
        List<Element> found = children(parent, tag);
        return found.isEmpty() ? null : found.getFirst();
    }

    private static String attr(Element e, String name) {
        String v = e.getAttribute(name);
        return v.isEmpty() ? null : v;
    }

    private static Integer intAttr(Element e, String name) {
        String v = attr(e, name);
        if (v == null) {
            return null;
        }
        try {
            return Integer.valueOf(v);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

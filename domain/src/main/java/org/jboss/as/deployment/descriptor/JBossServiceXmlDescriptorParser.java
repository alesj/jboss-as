/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.deployment.descriptor;

import org.jboss.as.model.ParseResult;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class JBossServiceXmlDescriptorParser implements XMLElementReader<ParseResult<JBossServiceXmlDescriptor>>, XMLStreamConstants {

    private enum Element {
        MBEAN(new QName("mbean")),
        CONSTRUCTOR(new QName("constructor")),
        ARG(new QName("arg")),
        UNKNOWN(null);

        private final QName qName;

        private static final Map<QName, Element> QNAME_MAP = new HashMap<QName, Element>();

        static {
            for(Element element : Element.values()) {
                QNAME_MAP.put(element.qName, element);
            }
        }

        private Element(final QName qName) {
            this.qName = qName;
        }

        static Element of(QName qName) {
            final Element element = QNAME_MAP.get(qName);
            return element == null ? UNKNOWN : element;
        }
    }

    private enum Attribute {
        NAME(new QName("name")),
        CODE(new QName("code")),
        INTERFACE(new QName("interface")),
        XMBEAN_DD(new QName("xmbean-dd")),
        XMBEAN_CODE(new QName("xmbean-code")),
        TYPE(new QName("type")),
        VALUE(new QName("value")),
        UNKNOWN(null);

        private final QName qName;

        private static final Map<QName, Attribute> QNAME_MAP = new HashMap<QName, Attribute>();

        static {
            for(Attribute attribute : Attribute.values()) {
                QNAME_MAP.put(attribute.qName, attribute);
            }
        }

        private Attribute(final QName qName) {
            this.qName = qName;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = QNAME_MAP.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    public void readElement(final XMLExtendedStreamReader reader, final ParseResult<JBossServiceXmlDescriptor> value) throws XMLStreamException {
        final List<JBossServiceConfig> serviceConfigs = new ArrayList<JBossServiceConfig>();
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case COMMENT:
                    break;
                case END_ELEMENT:
                    value.setResult(new JBossServiceXmlDescriptor(serviceConfigs));
                    return;
                case START_ELEMENT:
                    switch(Element.of(reader.getName())) {
                        case MBEAN:
                            serviceConfigs.add(parseMBean(reader));
                            break;
                        case UNKNOWN:
                            unexpectedContent(reader);
                            break;
                    }
                    break;
            }
        }
    }

    private JBossServiceConfig parseMBean(final XMLExtendedStreamReader reader) throws XMLStreamException  {
        // Handle Attributes
        final JBossServiceConfig serviceConfig = new JBossServiceConfig();
        
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.NAME, Attribute.CODE);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch(attribute) {
                case NAME:
                    serviceConfig.setName(attributeValue);
                    break;
                case CODE:
                    serviceConfig.setCode(attributeValue);
                    break;
                case INTERFACE:
                    serviceConfig.setInterfaceName(attributeValue);
                    break;
                case XMBEAN_DD:
                    serviceConfig.setXmbeanDD(attributeValue);
                    break;
                case XMBEAN_CODE:
                    serviceConfig.setXmbeanCode(attributeValue);
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT:
                    return serviceConfig;
                case START_ELEMENT:
                    switch(Element.of(reader.getName())) {
                        case CONSTRUCTOR:
                            serviceConfig.setConstructorConfig(parseConstructor(reader));
                            break;
                        case UNKNOWN:
                            unexpectedContent(reader);
                            break;
                    }
                    break;
            }
        }
        throw unexpectedContent(reader);
    }

    private JBossServiceConstructorConfig parseConstructor(final XMLExtendedStreamReader reader) throws XMLStreamException {
        final JBossServiceConstructorConfig constructorConfig = new JBossServiceConstructorConfig();
        final List<JBossServiceConstructorConfig.Argument> arguments = new ArrayList<JBossServiceConstructorConfig.Argument>();

        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case END_ELEMENT:
                    constructorConfig.setArguments(arguments.toArray(new JBossServiceConstructorConfig.Argument[arguments.size()]));
                    return constructorConfig;
                case START_ELEMENT:
                    switch(Element.of(reader.getName())) {
                        case ARG:
                            arguments.add(parseArgument(reader));
                            break;
                        case UNKNOWN:
                            unexpectedContent(reader);
                            break;
                    }
                    break;
            }
        }
        throw unexpectedContent(reader);
    }

    private JBossServiceConstructorConfig.Argument parseArgument(XMLExtendedStreamReader reader) throws XMLStreamException {
        String type = null;
        String value = null;
        
        final int count = reader.getAttributeCount();
        final Set<Attribute> required = EnumSet.of(Attribute.TYPE, Attribute.VALUE);
        for(int i = 0; i < count; i++) {
            final Attribute attribute = Attribute.of(reader.getAttributeName(i));
            required.remove(attribute);
            final String attributeValue = reader.getAttributeValue(i);

            switch(attribute) {
                case TYPE:
                    type = attributeValue;
                    break;
                case VALUE:
                    value = attributeValue;
                    break;
                default:
                    throw unexpectedContent(reader);
            }
        }
        if(!required.isEmpty()) {
            throw missingAttributes(reader.getLocation(), required);
        }
        reader.discardRemainder();
        return new JBossServiceConstructorConfig.Argument(type, value);
    }

    private static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE: kind = "attribute"; break;
            case XMLStreamConstants.CDATA: kind = "cdata"; break;
            case XMLStreamConstants.CHARACTERS: kind = "characters"; break;
            case XMLStreamConstants.COMMENT: kind = "comment"; break;
            case XMLStreamConstants.DTD: kind = "dtd"; break;
            case XMLStreamConstants.END_DOCUMENT: kind = "document end"; break;
            case XMLStreamConstants.END_ELEMENT: kind = "element end"; break;
            case XMLStreamConstants.ENTITY_DECLARATION: kind = "entity decl"; break;
            case XMLStreamConstants.ENTITY_REFERENCE: kind = "entity ref"; break;
            case XMLStreamConstants.NAMESPACE: kind = "namespace"; break;
            case XMLStreamConstants.NOTATION_DECLARATION: kind = "notation decl"; break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION: kind = "processing instruction"; break;
            case XMLStreamConstants.SPACE: kind = "whitespace"; break;
            case XMLStreamConstants.START_DOCUMENT: kind = "document start"; break;
            case XMLStreamConstants.START_ELEMENT: kind = "element start"; break;
            default: kind = "unknown"; break;
        }
        final StringBuilder b = new StringBuilder("Unexpected content of type '").append(kind).append('\'');
        if (reader.hasName()) {
            b.append(" named '").append(reader.getName()).append('\'');
        }
        if (reader.hasText()) {
            b.append(", text is: '").append(reader.getText()).append('\'');
        }
        return new XMLStreamException(b.toString(), reader.getLocation());
    }

    private static XMLStreamException missingAttributes(final Location location, final Set<Attribute> required) {
        final StringBuilder b = new StringBuilder("Missing one or more required attributes:");
        for (Attribute attribute : required) {
            b.append(' ').append(attribute);
        }
        return new XMLStreamException(b.toString(), location);
    }

    public static void main(String[] args) throws Exception {
        final JBossServiceXmlDescriptorParser parser = new JBossServiceXmlDescriptorParser();

        final XMLMapper xmlMapper = XMLMapper.Factory.create();
        xmlMapper.registerRootElement(new QName("server"), parser);
        final XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        String xml ="" +
            "<server>" +
            "   <mbean name=\"test\" code=\"testCode\">" +
            "       <constructor>" +
            "           <arg type=\"java.lang.String\" value=\"test\"/>" +
            "       </constructor>" +
            "   </mbean>" +
            "   <mbean name=\"test2\" code=\"testCode2\">" +
            "   </mbean>" +
            "</server>";
        final XMLStreamReader reader = inputFactory.createXMLStreamReader(new StringReader(xml));

        final ParseResult<JBossServiceXmlDescriptor> jBossServiceXmlDescriptorParseResult = new ParseResult<JBossServiceXmlDescriptor>();
        xmlMapper.parseDocument(jBossServiceXmlDescriptorParseResult, reader);
        System.out.println(jBossServiceXmlDescriptorParseResult.getResult().getServiceConfigs());
    }
}
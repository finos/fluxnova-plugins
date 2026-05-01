package org.finos.fluxnova.bpm.engine.shared.xml;

import org.finos.fluxnova.bpm.engine.impl.util.xml.Parse;
import org.finos.fluxnova.bpm.engine.impl.util.xml.Parser;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class BpmnXmlParser extends Parser {

    @Override
    public Parse createParse() {
        return new BpmnXmlParse(this);
    }

    @Override
    protected SAXParser getSaxParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        setXxeProcessing(factory);
        return factory.newSAXParser();
    }
}
package org.finos.fluxnova.bpm.engine.ai.agent.extract;

import org.finos.fluxnova.bpm.engine.impl.util.xml.Element;

import java.util.ArrayList;
import java.util.List;

public class AgentConfigElementWalker {

    public List<Element> walk(Element root) {
        return collect(root);
    }

    private List<Element> collect(Element parent) {
        List<Element> elements = new ArrayList<>();
        for (Element child : parent.elements()) {
            elements.add(child);
            if (!"extensionElements".equals(child.getTagName())) {
                elements.addAll(collect(child));
            }
        }
        return elements;
    }
}
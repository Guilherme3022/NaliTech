package com.ledgerflow.modules.parser.impl;

import com.ledgerflow.modules.parser.DocumentParser;
import com.ledgerflow.modules.parser.model.ParseResult;
import com.ledgerflow.modules.parser.model.RawMovement;
import com.ledgerflow.shared.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Component
public class XmlParser implements DocumentParser {

    @Override
    public boolean supports(String extension) {
        return "xml".equals(extension);
    }

    @Override
    public ParseResult parse(byte[] content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            var document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
            document.getDocumentElement().normalize();

            List<RawMovement> movements = new ArrayList<>();
            NodeList all = document.getElementsByTagName("*");
            for (int i = 0; i < all.getLength(); i++) {
                Element element = (Element) all.item(i);
                Map<String, String> fields = readCanonicalChildren(element);
                if (fields.containsKey("data") || fields.containsKey("valor")) {
                    movements.add(new RawMovement(
                            fields.get("data"), fields.get("valor"),
                            fields.get("descricao"), fields.get("documento")));
                }
            }
            return ParseResult.of(movements);
        } catch (Exception ex) {
            throw new BusinessException("Falha ao ler XML: " + ex.getMessage(),
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private Map<String, String> readCanonicalChildren(Element element) {
        Map<String, String> fields = new HashMap<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String tag = child.getNodeName().toLowerCase();
            String value = child.getTextContent().trim();
            if (ColumnResolver.DATA.contains(tag)) {
                fields.put("data", value);
            } else if (ColumnResolver.VALOR.contains(tag)) {
                fields.put("valor", value);
            } else if (ColumnResolver.DESCRICAO.contains(tag)) {
                fields.put("descricao", value);
            } else if (ColumnResolver.DOCUMENTO.contains(tag)) {
                fields.put("documento", value);
            }
        }
        return fields;
    }
}

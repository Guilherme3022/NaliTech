package com.ledgerflow.modules.parser;

import com.ledgerflow.modules.parser.model.ParseResult;

public interface DocumentParser {

    boolean supports(String extension);

    ParseResult parse(byte[] content);
}

package com.nalitech.modules.parser;

import com.nalitech.modules.parser.model.ParseResult;

public interface DocumentParser {

    boolean supports(String extension);

    ParseResult parse(byte[] content);
}

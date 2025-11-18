package com.logicalastrology.scraper;

import java.io.IOException;
import java.util.Map;

public interface BaseScraper {
    Map<String, String> fetchAll() throws IOException;
}

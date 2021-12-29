package org.redquark.hotspring.pdf.configs;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDFOperationsConfig {

    @Bean
    public PDFMergerUtility getPDFMergerUtility() {
        return new PDFMergerUtility();
    }
}

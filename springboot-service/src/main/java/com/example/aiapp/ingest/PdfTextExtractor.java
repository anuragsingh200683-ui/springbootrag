package com.example.aiapp.ingest;

import com.example.aiapp.exception.PdfExtractionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/** Extracts plain text from a stored PDF, mirroring fastapi-service's pypdf-based pdf_service.py. */
@Component
public class PdfTextExtractor {

    public String extractText(String filePath) {
        try (PDDocument document = Loader.loadPDF(new File(filePath))) {
            return new PDFTextStripper().getText(document);
        } catch (IOException e) {
            throw new PdfExtractionException("Failed to extract text from PDF: " + filePath, e);
        }
    }
}

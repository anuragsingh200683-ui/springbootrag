package com.example.aiapp.ingest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Hand-rolled recursive-character splitter, mirroring LangChain's
 * RecursiveCharacterTextSplitter closely enough for chunk-boundary purposes: split on
 * paragraph breaks, then lines, then sentences, then words, then hard character
 * boundaries as a last resort, then greedily merge the resulting pieces into chunks
 * up to chunkSize characters with chunkOverlap characters carried over between
 * consecutive chunks.
 */
@Component
public class TextChunker {

    private static final List<String> SEPARATORS = List.of("\n\n", "\n", ". ", " ", "");

    private final int chunkSize;
    private final int chunkOverlap;

    public TextChunker(@Value("${app.qa.chunk-size}") int chunkSize,
                        @Value("${app.qa.chunk-overlap}") int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    public List<String> split(String text) {
        List<String> atoms = new ArrayList<>();
        splitRecursively(text, 0, atoms);
        return merge(atoms);
    }

    private void splitRecursively(String text, int separatorIndex, List<String> out) {
        if (text.isEmpty()) {
            return;
        }
        if (text.length() <= chunkSize) {
            out.add(text);
            return;
        }
        if (separatorIndex >= SEPARATORS.size()) {
            for (int i = 0; i < text.length(); i += chunkSize) {
                out.add(text.substring(i, Math.min(i + chunkSize, text.length())));
            }
            return;
        }

        String separator = SEPARATORS.get(separatorIndex);
        // Split right after each separator occurrence (zero-width lookbehind) instead of
        // consuming it, so every part keeps its own trailing separator attached and merge()'s
        // plain concatenation reconstructs the original spacing instead of gluing words together.
        String[] parts = separator.isEmpty()
                ? splitIntoChars(text)
                : text.split("(?<=" + Pattern.quote(separator) + ")", -1);
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (part.length() <= chunkSize) {
                out.add(part);
            } else {
                splitRecursively(part, separatorIndex + 1, out);
            }
        }
    }

    private String[] splitIntoChars(String text) {
        String[] chars = new String[text.length()];
        for (int i = 0; i < text.length(); i++) {
            chars[i] = String.valueOf(text.charAt(i));
        }
        return chars;
    }

    private List<String> merge(List<String> atoms) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String atom : atoms) {
            if (current.length() > 0 && current.length() + atom.length() > chunkSize) {
                chunks.add(current.toString().strip());
                String tail = current.length() > chunkOverlap
                        ? current.substring(current.length() - chunkOverlap)
                        : current.toString();
                current = new StringBuilder(tail);
            }
            current.append(atom);
        }
        if (current.length() > 0 && !current.toString().isBlank()) {
            chunks.add(current.toString().strip());
        }
        return chunks;
    }
}

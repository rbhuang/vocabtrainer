package com.vocabtrainer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * CSV 词表文件读写工具
 */
class CsvUtils {

    static Path desktopPath(String filename) {
        return Paths.get(System.getProperty("user.home"), "Desktop", filename);
    }

    static List<String[]> loadWords(Path filePath) {
        Map<String, String> map = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            reader.mark(1);
            if (reader.read() != '\uFEFF') reader.reset();
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = parseLine(line);
                if (parts.length >= 2) {
                    String word = parts[0].trim();
                    String meaning = parts[1].trim();
                    if (!word.isEmpty()) map.putIfAbsent(word, meaning);
                }
            }
        } catch (IOException ignored) {
        }
        List<String[]> result = new ArrayList<>(map.size());
        map.forEach((k, v) -> result.add(new String[]{k, v}));
        return result;
    }

    static void saveWords(List<String[]> words, Path filePath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            for (String[] entry : words) {
                writer.write(escape(entry[0]) + "," + escape(entry[1]));
                writer.newLine();
            }
        }
    }

    static String[] parseLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    sb.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString());
        return fields.toArray(new String[0]);
    }

    static String escape(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

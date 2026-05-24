package com.vocabtrainer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvUtilsTest {

    @TempDir
    Path tempDir;

    // --- parseLine ---

    @Test
    void parseLine_simpleFields() {
        String[] result = CsvUtils.parseLine("hello,world");
        assertArrayEquals(new String[]{"hello", "world"}, result);
    }

    @Test
    void parseLine_quotedField() {
        String[] result = CsvUtils.parseLine("\"hello,world\",test");
        assertArrayEquals(new String[]{"hello,world", "test"}, result);
    }

    @Test
    void parseLine_escapedQuoteInField() {
        // CSV: "say ""hi""",meaning -> [say "hi", meaning]
        String[] result = CsvUtils.parseLine("\"say \"\"hi\"\"\",meaning");
        assertArrayEquals(new String[]{"say \"hi\"", "meaning"}, result);
    }

    @Test
    void parseLine_emptyFields() {
        String[] result = CsvUtils.parseLine(",");
        assertArrayEquals(new String[]{"", ""}, result);
    }

    @Test
    void parseLine_singleField() {
        String[] result = CsvUtils.parseLine("only");
        assertArrayEquals(new String[]{"only"}, result);
    }

    @Test
    void parseLine_threeFields() {
        String[] result = CsvUtils.parseLine("a,b,c");
        assertArrayEquals(new String[]{"a", "b", "c"}, result);
    }

    @Test
    void parseLine_quotedWithNewline() {
        String[] result = CsvUtils.parseLine("\"line1\nline2\",val");
        assertEquals("line1\nline2", result[0]);
        assertEquals("val", result[1]);
    }

    // --- escape ---

    @Test
    void escape_noSpecialChars() {
        assertEquals("hello", CsvUtils.escape("hello"));
    }

    @Test
    void escape_withComma() {
        assertEquals("\"hello,world\"", CsvUtils.escape("hello,world"));
    }

    @Test
    void escape_withQuote() {
        assertEquals("\"say \"\"hi\"\"\"", CsvUtils.escape("say \"hi\""));
    }

    @Test
    void escape_withNewline() {
        assertEquals("\"line1\nline2\"", CsvUtils.escape("line1\nline2"));
    }

    @Test
    void escape_emptyString() {
        assertEquals("", CsvUtils.escape(""));
    }

    // --- loadWords ---

    @Test
    void loadWords_normalCsv() throws IOException {
        Path file = tempDir.resolve("words.csv");
        Files.writeString(file, "apple,苹果\nbanana,香蕉\n", StandardCharsets.UTF_8);

        List<String[]> words = CsvUtils.loadWords(file);
        assertEquals(2, words.size());
        assertArrayEquals(new String[]{"apple", "苹果"}, words.get(0));
        assertArrayEquals(new String[]{"banana", "香蕉"}, words.get(1));
    }

    @Test
    void loadWords_withBOM() throws IOException {
        Path file = tempDir.resolve("bom.csv");
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = "word,意思\n".getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(content, 0, all, bom.length, content.length);
        Files.write(file, all);

        List<String[]> words = CsvUtils.loadWords(file);
        assertEquals(1, words.size());
        assertEquals("word", words.get(0)[0]);
    }

    @Test
    void loadWords_withoutBOM() throws IOException {
        Path file = tempDir.resolve("nobom.csv");
        Files.writeString(file, "test,测试\n", StandardCharsets.UTF_8);

        List<String[]> words = CsvUtils.loadWords(file);
        assertEquals(1, words.size());
        assertEquals("test", words.get(0)[0]);
    }

    @Test
    void loadWords_duplicateKeys() throws IOException {
        Path file = tempDir.resolve("dup.csv");
        Files.writeString(file, "apple,苹果\napple,苹果2\n", StandardCharsets.UTF_8);

        List<String[]> words = CsvUtils.loadWords(file);
        assertEquals(1, words.size());
        assertEquals("苹果", words.get(0)[1]); // putIfAbsent keeps first
    }

    @Test
    void loadWords_skipsEmptyWord() throws IOException {
        Path file = tempDir.resolve("empty.csv");
        Files.writeString(file, ",意思\napple,苹果\n", StandardCharsets.UTF_8);

        List<String[]> words = CsvUtils.loadWords(file);
        assertEquals(1, words.size());
        assertEquals("apple", words.get(0)[0]);
    }

    @Test
    void loadWords_skipsInsufficientFields() throws IOException {
        Path file = tempDir.resolve("short.csv");
        Files.writeString(file, "onlyword\napple,苹果\n", StandardCharsets.UTF_8);

        List<String[]> words = CsvUtils.loadWords(file);
        assertEquals(1, words.size());
        assertEquals("apple", words.get(0)[0]);
    }

    @Test
    void loadWords_nonexistentFile() {
        Path file = tempDir.resolve("nonexistent.csv");
        List<String[]> words = CsvUtils.loadWords(file);
        assertTrue(words.isEmpty());
    }

    @Test
    void loadWords_trimWhitespace() throws IOException {
        Path file = tempDir.resolve("ws.csv");
        Files.writeString(file, " apple , 苹果 \n", StandardCharsets.UTF_8);

        List<String[]> words = CsvUtils.loadWords(file);
        assertEquals("apple", words.get(0)[0]);
        assertEquals("苹果", words.get(0)[1]);
    }

    // --- saveWords ---

    @Test
    void saveWords_normal() throws IOException {
        Path file = tempDir.resolve("out.csv");
        List<String[]> words = List.of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"}
        );
        CsvUtils.saveWords(words, file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals("apple,苹果", lines.get(0));
        assertEquals("banana,香蕉", lines.get(1));
    }

    @Test
    void saveWords_withSpecialChars() throws IOException {
        Path file = tempDir.resolve("special.csv");
        List<String[]> words = List.<String[]>of(
                new String[]{"say \"hi\"", "含,逗号"}
        );
        CsvUtils.saveWords(words, file);

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("\"say \"\"hi\"\"\""));
        assertTrue(content.contains("\"含,逗号\""));
    }

    @Test
    void saveWords_roundTrip() throws IOException {
        Path file = tempDir.resolve("round.csv");
        List<String[]> original = List.of(
                new String[]{"hello", "你好"},
                new String[]{"world", "世界"}
        );
        CsvUtils.saveWords(original, file);
        List<String[]> loaded = CsvUtils.loadWords(file);

        assertEquals(original.size(), loaded.size());
        for (int i = 0; i < original.size(); i++) {
            assertArrayEquals(original.get(i), loaded.get(i));
        }
    }

    // --- desktopPath ---

    @Test
    void desktopPath_containsDesktop() {
        Path p = CsvUtils.desktopPath("test.csv");
        assertTrue(p.toString().contains("Desktop"));
        assertTrue(p.toString().endsWith("test.csv"));
    }
}

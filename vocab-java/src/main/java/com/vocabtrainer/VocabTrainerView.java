package com.vocabtrainer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VocabTrainerView {

    private final VBox root;
    private final Label wordLabel;
    private final Label meaningLabel;
    private final Label statusLabel;
    private final Label progressLabel;
    private final Button btnKeep;
    private final Button btnRemove;
    private final Button btnSave;
    private final Spinner<Integer> countdownSpinner;

    private List<String[]> wordsList;
    private int totalWords;
    private int defaultCountdown = 3;
    private int countdown;
    private String currentWord = "";
    private String currentMeaning = "";
    private Timeline timer;
    private final Random random = new Random();

    public VocabTrainerView() {
        root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        // 加载词表
        Path tmpPath = getResourcePath("words_tmp.csv");
        Path csvPath = getResourcePath("words.csv");
        if (Files.exists(tmpPath)) {
            wordsList = loadWords(tmpPath);
        } else {
            wordsList = loadWords(csvPath);
        }
        totalWords = wordsList.size();

        // 单词显示
        wordLabel = new Label("");
        wordLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        wordLabel.setAlignment(Pos.CENTER);
        wordLabel.setMaxWidth(Double.MAX_VALUE);
        wordLabel.setStyle("-fx-alignment: center;");

        // 释义显示
        meaningLabel = new Label("");
        meaningLabel.setFont(Font.font("Arial", 20));
        meaningLabel.setAlignment(Pos.CENTER);
        meaningLabel.setMaxWidth(Double.MAX_VALUE);
        meaningLabel.setWrapText(true);
        meaningLabel.setStyle("-fx-alignment: center;");

        // 倒计时/状态
        statusLabel = new Label("");
        statusLabel.setFont(Font.font("Arial", 14));
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        statusLabel.setStyle("-fx-alignment: center;");

        // 按钮
        btnKeep = new Button("保留 (Y)");
        btnKeep.setFont(Font.font("Arial", 14));
        btnKeep.setOnAction(e -> keepWord());

        btnRemove = new Button("记住了 (N)");
        btnRemove.setFont(Font.font("Arial", 14));
        btnRemove.setOnAction(e -> removeWord());

        btnSave = new Button("保存退出 (S)");
        btnSave.setFont(Font.font("Arial", 14));
        btnSave.setOnAction(e -> saveAndQuit());

        HBox btnLayout = new HBox(10, btnKeep, btnRemove, btnSave);
        btnLayout.setAlignment(Pos.CENTER);

        // 倒计时设置
        Label timerLabel = new Label("倒计时(秒):");
        timerLabel.setFont(Font.font("Arial", 12));
        countdownSpinner = new Spinner<>(1, 30, defaultCountdown);
        countdownSpinner.setPrefWidth(70);
        countdownSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            defaultCountdown = newVal;
        });

        HBox timerLayout = new HBox(8, timerLabel, countdownSpinner);
        timerLayout.setAlignment(Pos.CENTER_LEFT);

        // 剩余单词数
        progressLabel = new Label("");
        progressLabel.setFont(Font.font("Arial", 12));
        progressLabel.setAlignment(Pos.CENTER);
        progressLabel.setMaxWidth(Double.MAX_VALUE);
        progressLabel.setStyle("-fx-alignment: center;");

        // 快捷键提示
        Label shortcutLabel = new Label("快捷键: Y=保留  N=记住了  S=保存退出");
        shortcutLabel.setFont(Font.font("Arial", 11));
        shortcutLabel.setStyle("-fx-text-fill: gray; -fx-alignment: center;");
        shortcutLabel.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(wordLabel, meaningLabel, statusLabel, btnLayout,
                timerLayout, progressLabel, shortcutLabel);

        // 初始化定时器
        timer = new Timeline();
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> tick()));

        if (!wordsList.isEmpty()) {
            nextWord();
        } else {
            wordLabel.setText("未找到词表文件");
        }
    }

    public VBox getRoot() {
        return root;
    }

    public void handleKey(String key) {
        if (btnKeep.isDisabled()) return;
        switch (key) {
            case "y" -> keepWord();
            case "n" -> removeWord();
            case "s" -> saveAndQuit();
        }
    }

    private void nextWord() {
        int idx = random.nextInt(wordsList.size());
        String[] entry = wordsList.get(idx);
        currentWord = entry[0];
        currentMeaning = entry[1];

        wordLabel.setText(currentWord);
        meaningLabel.setText("");
        countdown = defaultCountdown;
        statusLabel.setText("倒计时: " + countdown + " 秒...");
        progressLabel.setText("剩余: " + wordsList.size() + " / " + totalWords);
        setButtonsEnabled(false);
        timer.play();
    }

    private void tick() {
        countdown--;
        if (countdown > 0) {
            statusLabel.setText("倒计时: " + countdown + " 秒...");
        } else {
            timer.stop();
            meaningLabel.setText(currentMeaning);
            setButtonsEnabled(true);

        }
    }

    private void keepWord() {
        if (wordsList.isEmpty()) return;
        timer.stop();
        meaningLabel.setText(currentMeaning);
        statusLabel.setText("🚩 '" + currentWord + "' 会再次出现");

        Timeline delay = new Timeline(new KeyFrame(Duration.millis(800), e -> nextWord()));
        delay.play();
    }

    private void removeWord() {
        if (wordsList.isEmpty()) return;
        timer.stop();
        meaningLabel.setText(currentMeaning);

        wordsList.removeIf(entry -> entry[0].equals(currentWord) && entry[1].equals(currentMeaning));
        statusLabel.setText("✨ '" + currentWord + "' 已移除");
        progressLabel.setText("剩余: " + wordsList.size() + " / " + totalWords);

        if (wordsList.isEmpty()) {
            setButtonsEnabled(false);
            wordLabel.setText("🎉 恭喜！");
            meaningLabel.setText("你已完成全部 " + totalWords + " 个单词！");
            statusLabel.setText("");
        } else {
            Timeline delay = new Timeline(new KeyFrame(Duration.millis(800), e -> nextWord()));
            delay.play();
        }
    }

    private void saveAndQuit() {
        if (!wordsList.isEmpty()) {
            saveWords(wordsList, getResourcePath("words_tmp.csv"));
        }
        timer.stop();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("已保存");
        alert.setHeaderText(null);
        alert.setContentText("已保存 " + wordsList.size() + " 个单词到 words_tmp.csv");
        alert.showAndWait();

        // 退出应用
        javafx.application.Platform.exit();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnKeep.setDisable(!enabled);
        btnRemove.setDisable(!enabled);
        btnSave.setDisable(!enabled);
    }

    private Path getResourcePath(String filename) {
        String home = System.getProperty("user.home");
        return Paths.get(home, "Desktop", filename);
    }

    private List<String[]> loadWords(Path filePath) {
        Map<String, String> wordsMap = new LinkedHashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            // Skip BOM if present
            reader.mark(1);
            int firstChar = reader.read();
            if (firstChar != '\uFEFF') {
                reader.reset();
            }

            while ((line = reader.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length >= 2) {
                    String word = parts[0].trim();
                    String meaning = parts[1].trim();
                    if (!word.isEmpty()) {
                        wordsMap.putIfAbsent(word, meaning);
                    }
                }
            }
        } catch (IOException e) {
            // File not found or read error
        }

        List<String[]> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : wordsMap.entrySet()) {
            result.add(new String[]{entry.getKey(), entry.getValue()});
        }
        return result;
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }

    private void saveWords(List<String[]> words, Path filePath) {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            for (String[] entry : words) {
                writer.write(escapeCsv(entry[0]) + "," + escapeCsv(entry[1]));
                writer.newLine();
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("保存失败");
            alert.setContentText("无法保存文件: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

package com.vocabtrainer;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * 纯 UI 视图：构建界面组件，暴露显示方法供 Controller 调用
 */
public class VocabTrainerView {

    private static final int DEFAULT_COUNTDOWN = 3;

    private final VBox root;
    private final Label wordLabel, meaningLabel, statusLabel, progressLabel;
    private final Button btnKeep, btnRemove, btnPause, btnQuit;
    private final Spinner<Integer> countdownSpinner;
    private final CheckBox ttsCheckbox;

    public VocabTrainerView() {
        root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        // 标签
        wordLabel = makeLabel(32, true);
        meaningLabel = makeLabel(20, false);
        meaningLabel.setWrapText(true);
        statusLabel = makeLabel(14, false);
        progressLabel = makeLabel(12, false);

        // 按钮（事件由 Controller 绑定）
        btnKeep = makeButton("保留 (Y)");
        btnRemove = makeButton("记住了 (N)");
        btnPause = makeButton("暂停 (P)");
        btnQuit = makeButton("退出 (X)");
        HBox btnRow = new HBox(10, btnKeep, btnRemove, btnPause, btnQuit);
        btnRow.setAlignment(Pos.CENTER);

        // 设置行
        Label timerLabel = new Label("倒计时(秒):");
        timerLabel.setFont(Font.font("Arial", 12));
        countdownSpinner = new Spinner<>(1, 30, DEFAULT_COUNTDOWN);
        countdownSpinner.setPrefWidth(70);

        ttsCheckbox = new CheckBox("🔊 文字转语音");
        ttsCheckbox.setFont(Font.font("Arial", 12));

        HBox settingsRow = new HBox(20,
                new HBox(8, timerLabel, countdownSpinner),
                ttsCheckbox);
        settingsRow.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(wordLabel, meaningLabel, statusLabel,
                btnRow, settingsRow, progressLabel);
    }

    // --- 提供给 Controller 绑定事件 ---

    public Button getBtnKeep() { return btnKeep; }
    public Button getBtnRemove() { return btnRemove; }
    public Button getBtnPause() { return btnPause; }
    public Button getBtnQuit() { return btnQuit; }
    public Spinner<Integer> getCountdownSpinner() { return countdownSpinner; }
    public CheckBox getTtsCheckbox() { return ttsCheckbox; }

    // --- 显示方法（供 Controller 调用）---

    public VBox getRoot() { return root; }

    public void showWord(String word) {
        wordLabel.setText(word);
    }

    public void showMeaning(String meaning) {
        meaningLabel.setText(meaning);
    }

    public void showStatus(String text) {
        statusLabel.setText(text);
    }

    public String getStatus() {
        return statusLabel.getText();
    }

    public void showProgress(int remaining, int total) {
        progressLabel.setText("剩余: " + remaining + " / " + total);
    }

    public void showNoWords() {
        wordLabel.setText("未找到词表文件");
    }

    public void showCompleted(int total) {
        wordLabel.setText("🎉 恭喜！");
        meaningLabel.setText("你已完成全部 " + total + " 个单词！");
        statusLabel.setText("");
    }

    public void setActionEnabled(boolean enabled) {
        btnKeep.setDisable(!enabled);
        btnRemove.setDisable(!enabled);
    }

    public boolean isActionEnabled() {
        return !btnKeep.isDisabled();
    }

    public void showPaused(boolean isPaused) {
        btnPause.setText(isPaused ? "继续 (P)" : "暂停 (P)");
    }

    // --- 工厂方法 ---

    private static Label makeLabel(int size, boolean bold) {
        Label label = new Label();
        label.setFont(Font.font("Arial", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        label.setAlignment(Pos.CENTER);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setStyle("-fx-alignment: center;");
        return label;
    }

    private static Button makeButton(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", 14));
        return btn;
    }
}

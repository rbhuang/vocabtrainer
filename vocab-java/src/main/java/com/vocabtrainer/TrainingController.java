package com.vocabtrainer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

/**
 * 训练控制器：管理训练状态、定时器、TTS，驱动 View 更新
 */
public class TrainingController {

    private static final String WORDS_FILE = "words.csv";
    private static final String WORDS_TMP_FILE = "words_tmp.csv";
    private static final int DEFAULT_COUNTDOWN = 3;
    private static final int AUTO_KEEP_DELAY_MS = 3000;
    private static final int TRANSITION_DELAY_MS = 800;

    private final VocabTrainerView view;
    private final TTSPlayer tts = new TTSPlayer();
    private final Random random = new Random();

    // 定时器
    private final Timeline countdownTimer;
    private final Timeline autoKeepTimer;

    // 状态
    private List<String[]> wordsList;
    private int totalWords;
    private int defaultCountdown = DEFAULT_COUNTDOWN;
    private int countdown;
    private String currentWord = "", currentMeaning = "";
    private boolean paused = false;
    private boolean waitingForTts = false;
    private String savedStatus = "";

    public TrainingController(VocabTrainerView view) {
        this.view = view;

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        autoKeepTimer = new Timeline(new KeyFrame(Duration.millis(AUTO_KEEP_DELAY_MS), e -> autoKeep()));
        autoKeepTimer.setCycleCount(1);

        wordsList = loadInitialWords();
        totalWords = wordsList.size();

        if (!wordsList.isEmpty()) {
            nextWord();
        } else {
            view.showNoWords();
        }
    }

    /** 测试用构造器：注入词表，跳过文件加载 */
    TrainingController(VocabTrainerView view, List<String[]> words) {
        this.view = view;

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        autoKeepTimer = new Timeline(new KeyFrame(Duration.millis(AUTO_KEEP_DELAY_MS), e -> autoKeep()));
        autoKeepTimer.setCycleCount(1);

        wordsList = new java.util.ArrayList<>(words);
        totalWords = wordsList.size();

        if (!wordsList.isEmpty()) {
            nextWord();
        } else {
            view.showNoWords();
        }
    }

    // --- 公共接口（由 View/App 调用）---

    public void handleKey(String key) {
        if ("p".equals(key)) { togglePause(); return; }
        if (!view.isActionEnabled() || paused) return;
        switch (key) {
            case "y" -> keepWord();
            case "n" -> removeWord();
            case "x" -> quit();
        }
    }

    public void onCountdownChanged(int value) {
        defaultCountdown = value;
    }

    public void onTtsToggled(boolean enabled) {
        tts.setEnabled(enabled);
        if (!enabled) tts.stop();
    }

    public void onClose() {
        cancelAutoKeep();
        countdownTimer.stop();
        if (!wordsList.isEmpty()) {
            try {
                CsvUtils.saveWords(wordsList, CsvUtils.desktopPath(WORDS_TMP_FILE));
            } catch (IOException e) {
                System.err.println("保存失败: " + e.getMessage());
            }
        }
    }

    public void keepWord() {
        if (wordsList.isEmpty()) return;
        cancelAutoKeep();
        countdownTimer.stop();
        view.showMeaning(currentMeaning);
        delay(this::nextWord);
    }

    public void removeWord() {
        if (wordsList.isEmpty()) return;
        cancelAutoKeep();
        countdownTimer.stop();
        view.showMeaning(currentMeaning);
        wordsList.removeIf(e -> e[0].equals(currentWord) && e[1].equals(currentMeaning));
        view.showStatus("✨ '" + currentWord + "' 已移除");
        view.showProgress(wordsList.size(), totalWords);

        if (wordsList.isEmpty()) {
            view.setActionEnabled(false);
            view.showCompleted(totalWords);
        } else {
            delay(this::nextWord);
        }
    }

    public void togglePause() {
        if (paused) resume(); else pause();
    }

    public void quit() {
        onClose();
        Platform.exit();
    }

    // --- 内部逻辑 ---

    void nextWord() {
        String[] entry = wordsList.get(random.nextInt(wordsList.size()));
        currentWord = entry[0];
        currentMeaning = entry[1];

        view.showWord(currentWord);
        view.showMeaning("");
        tts.speak(currentWord);
        countdown = defaultCountdown;
        view.showStatus("倒计时: " + countdown + " 秒...");
        view.showProgress(wordsList.size(), totalWords);
        view.setActionEnabled(false);
        countdownTimer.play();
    }

    void tick() {
        countdown--;
        if (countdown > 0) {
            view.showStatus("倒计时: " + countdown + " 秒...");
            return;
        }
        countdownTimer.stop();
        view.showMeaning(currentMeaning);
        view.showStatus("");
        view.setActionEnabled(true);

        if (tts.isEnabled()) {
            waitingForTts = true;
            tts.speak(currentMeaning, () -> Platform.runLater(() -> {
                if (waitingForTts && view.isActionEnabled()) {
                    waitingForTts = false;
                    autoKeepTimer.playFromStart();
                }
            }));
        } else {
            autoKeepTimer.playFromStart();
        }
    }

    void autoKeep() {
        if (view.isActionEnabled()) keepWord();
    }

    void pause() {
        paused = true;
        countdownTimer.stop();
        autoKeepTimer.stop();
        tts.stop();
        view.setActionEnabled(false);
        view.showPaused(true);
        savedStatus = view.getStatus();
        view.showStatus("⏸ 已暂停");
    }

    void resume() {
        paused = false;
        view.showPaused(false);
        view.showStatus(savedStatus);
        if (countdown > 0) {
            view.setActionEnabled(false);
            countdownTimer.play();
        } else {
            view.setActionEnabled(true);
            if (!waitingForTts) autoKeepTimer.playFromStart();
        }
    }

    void cancelAutoKeep() {
        waitingForTts = false;
        autoKeepTimer.stop();
        tts.stop();
    }

    void delay(Runnable action) {
        new Timeline(new KeyFrame(Duration.millis(TRANSITION_DELAY_MS), e -> action.run())).play();
    }

    static List<String[]> loadInitialWords() {
        Path tmp = CsvUtils.desktopPath(WORDS_TMP_FILE);
        if (Files.exists(tmp)) return CsvUtils.loadWords(tmp);
        return CsvUtils.loadWords(CsvUtils.desktopPath(WORDS_FILE));
    }
}

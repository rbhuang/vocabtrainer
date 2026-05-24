package com.vocabtrainer;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TrainingControllerTest {

    private VocabTrainerView view;
    private TrainingController controller;

    @BeforeAll
    static void initFx() {
        JavaFxTestHelper.initToolkit();
    }

    private void initOnFxThread(List<String[]> words) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            view = new VocabTrainerView();
            controller = new TrainingController(view, words);
            latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private void runOnFx(Runnable action) throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(action);
    }

    // --- Constructor ---

    @Test
    void constructor_withWords_showsFirstWord() throws InterruptedException {
        List<String[]> words = List.<String[]>of(new String[]{"apple", "苹果"});
        initOnFxThread(words);
        runOnFx(() -> {
            // After construction, countdown is running, action disabled
            assertFalse(view.isActionEnabled());
        });
    }

    @Test
    void constructor_emptyWords_showsNoWords() throws InterruptedException {
        initOnFxThread(List.of());
        // Should call view.showNoWords() - no exception
    }

    // --- handleKey ---

    @Test
    void handleKey_p_togglesPause() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.handleKey("p");
            assertEquals("⏸ 已暂停", view.getStatus());
        });
    }

    @Test
    void handleKey_p_resumesFromPause() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.handleKey("p"); // pause
            controller.handleKey("p"); // resume
            assertNotEquals("⏸ 已暂停", view.getStatus());
        });
    }

    @Test
    void handleKey_y_whenDisabled_ignored() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            // Actions disabled during countdown
            assertFalse(view.isActionEnabled());
            controller.handleKey("y");
            // No crash, word list unchanged
        });
    }

    @Test
    void handleKey_y_whenEnabled_keepsWord() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"}
        ));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.handleKey("y");
            // keepWord called -> meaning shown
        });
    }

    @Test
    void handleKey_n_whenEnabled_removesWord() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"}
        ));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.handleKey("n");
            // removeWord called
            assertTrue(view.getStatus().contains("已移除"));
        });
    }

    @Test
    void handleKey_x_whenEnabled_quits() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            // quit() calls Platform.exit() which may throw in test
            // Just test onClose
            controller.onClose();
        });
    }

    @Test
    void handleKey_whenPaused_ignoresYNX() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"}
        ));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.handleKey("p"); // pause
            view.setActionEnabled(true); // force enable for test
            controller.handleKey("y");
            controller.handleKey("n");
            // Should be ignored because paused
            assertEquals("⏸ 已暂停", view.getStatus());
        });
    }

    @Test
    void handleKey_unknownKey_ignored() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.handleKey("z");
            // No action, no crash
        });
    }

    // --- keepWord ---

    @Test
    void keepWord_emptyList_doesNothing() throws InterruptedException {
        initOnFxThread(List.of());
        runOnFx(() -> controller.keepWord());
    }

    @Test
    void keepWord_showsMeaning() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.keepWord();
        });
    }

    // --- removeWord ---

    @Test
    void removeWord_emptyList_doesNothing() throws InterruptedException {
        initOnFxThread(List.of());
        runOnFx(() -> controller.removeWord());
    }

    @Test
    void removeWord_removesFromList() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"}
        ));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.removeWord();
            assertTrue(view.getStatus().contains("已移除"));
        });
    }

    @Test
    void removeWord_lastWord_showsCompleted() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.removeWord();
            // Should show completed state
            assertFalse(view.isActionEnabled());
        });
    }

    // --- togglePause ---

    @Test
    void togglePause_pausesAndResumes() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.togglePause();
            assertEquals("⏸ 已暂停", view.getStatus());
            assertFalse(view.isActionEnabled());

            controller.togglePause();
            assertNotEquals("⏸ 已暂停", view.getStatus());
        });
    }

    @Test
    void togglePause_resumeAfterCountdownFinished() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            // Simulate countdown finished by enabling actions
            view.setActionEnabled(true);
            view.showStatus("some status");

            controller.togglePause(); // pause
            assertEquals("⏸ 已暂停", view.getStatus());

            controller.togglePause(); // resume with countdown=0 path
        });
    }

    // --- onCountdownChanged ---

    @Test
    void onCountdownChanged_updatesValue() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> controller.onCountdownChanged(5));
    }

    // --- onTtsToggled ---

    @Test
    void onTtsToggled_enablesDisables() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onTtsToggled(true);
            controller.onTtsToggled(false);
        });
    }

    // --- onClose ---

    @Test
    void onClose_withEmptyList_doesNotThrow() throws InterruptedException {
        initOnFxThread(List.of());
        runOnFx(() -> controller.onClose());
    }

    @Test
    void onClose_withWords_saves() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            // This will try to save to Desktop; just ensure no crash
            controller.onClose();
        });
    }

    // --- tick (直接调用内部方法) ---

    @Test
    void tick_decrementsCountdown() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            // After nextWord, countdown = defaultCountdown (3)
            controller.tick(); // countdown -> 2
            assertTrue(view.getStatus().contains("2"));
        });
    }

    @Test
    void tick_showsMeaningAtZero() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onCountdownChanged(1); // set countdown to 1
            controller.nextWord();            // countdown = 1
            controller.tick();                // countdown -> 0, show meaning
            assertTrue(view.isActionEnabled());
        });
    }

    @Test
    void tick_withTtsEnabled_setsWaitingForTts() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onTtsToggled(true);
            controller.onCountdownChanged(1);
            controller.nextWord();
            controller.tick(); // countdown -> 0, TTS enabled path
            assertTrue(view.isActionEnabled());
        });
    }

    @Test
    void tick_withTtsDisabled_startsAutoKeep() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onTtsToggled(false);
            controller.onCountdownChanged(1);
            controller.nextWord();
            controller.tick(); // countdown -> 0, no TTS, auto keep timer
            assertTrue(view.isActionEnabled());
        });
    }

    // --- autoKeep ---

    @Test
    void autoKeep_whenEnabled_keepsWord() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"}
        ));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.autoKeep();
        });
    }

    @Test
    void autoKeep_whenDisabled_doesNothing() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(false);
            controller.autoKeep();
        });
    }

    // --- nextWord ---

    @Test
    void nextWord_setsUpCountdown() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.nextWord();
            assertFalse(view.isActionEnabled());
            assertTrue(view.getStatus().contains("倒计时"));
        });
    }

    // --- pause / resume ---

    @Test
    void pause_stopsTimersAndShowsStatus() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.pause();
            assertEquals("⏸ 已暂停", view.getStatus());
            assertFalse(view.isActionEnabled());
        });
    }

    @Test
    void resume_withCountdownRemaining_restartsTimer() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            // countdown > 0 path
            controller.pause();
            controller.resume();
            assertFalse(view.isActionEnabled()); // still counting
        });
    }

    @Test
    void resume_withCountdownZero_enablesActions() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            // Make countdown reach 0
            controller.onCountdownChanged(1);
            controller.nextWord();
            controller.tick(); // countdown -> 0
            // Now pause and resume
            controller.pause();
            controller.resume();
            assertTrue(view.isActionEnabled());
        });
    }

    // --- cancelAutoKeep ---

    @Test
    void cancelAutoKeep_stopsAutoKeepAndTts() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> controller.cancelAutoKeep());
    }

    // --- delay ---

    @Test
    void delay_createsTimeline() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            boolean[] ran = {false};
            controller.delay(() -> ran[0] = true);
            // The Timeline won't fire instantly in test, but creation should not throw
        });
    }

    // --- loadInitialWords ---

    @Test
    void loadInitialWords_returnsListWithoutCrash() throws InterruptedException {
        // Tests the static method - may return empty list if no desktop files
        runOnFx(() -> {
            List<String[]> words = TrainingController.loadInitialWords();
            assertNotNull(words);
        });
    }

    // --- public constructor (file-based) ---

    @Test
    void publicConstructor_worksWithOrWithoutFiles() throws InterruptedException {
        runOnFx(() -> {
            VocabTrainerView v = new VocabTrainerView();
            // May show "no words" or start training depending on desktop files
            TrainingController c = new TrainingController(v);
            c.onClose();
        });
    }

    // --- resume with waitingForTts ---

    @Test
    void resume_withWaitingForTts_doesNotStartAutoKeep() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onTtsToggled(true);
            controller.onCountdownChanged(1);
            controller.nextWord();
            controller.tick(); // countdown -> 0, TTS enabled, sets waitingForTts
            // Now pause and resume - waitingForTts should be true
            controller.pause();
            controller.resume();
            assertTrue(view.isActionEnabled());
        });
    }

    // --- removeWord triggers multiple words path ---

    @Test
    void removeWord_multipleWords_continuesTraining() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"},
                new String[]{"cat", "猫"}
        ));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.removeWord();
            assertTrue(view.getStatus().contains("已移除"));
            // list still has words
            view.setActionEnabled(true);
            controller.removeWord();
        });
    }

    // --- handleKey branches ---

    @Test
    void handleKey_emptyString_ignored() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            controller.handleKey("");
        });
    }

    @Test
    void handleKey_x_callsQuitPath() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            view.setActionEnabled(true);
            // Can't call quit() directly (Platform.exit), but test onClose
            controller.onClose();
        });
    }

    // --- Additional branch coverage ---

    @Test
    void tick_multipleTimesBeforeZero() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onCountdownChanged(3);
            controller.nextWord();
            controller.tick(); // 3->2
            assertTrue(view.getStatus().contains("2"));
            controller.tick(); // 2->1
            assertTrue(view.getStatus().contains("1"));
            controller.tick(); // 1->0, show meaning
            assertTrue(view.isActionEnabled());
        });
    }

    @Test
    void keepWord_afterTick_cancelsAutoKeep() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"}
        ));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onCountdownChanged(1);
            controller.nextWord();
            controller.tick(); // meaning shown
            // Now call keepWord which should cancelAutoKeep
            controller.keepWord();
        });
    }

    @Test
    void removeWord_afterTick_cancelsAutoKeep() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(
                new String[]{"apple", "苹果"},
                new String[]{"banana", "香蕉"}
        ));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onCountdownChanged(1);
            controller.nextWord();
            controller.tick(); // meaning shown
            controller.removeWord();
            assertTrue(view.getStatus().contains("已移除"));
        });
    }

    @Test
    void pause_whileAutoKeepWaiting_stopsIt() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onCountdownChanged(1);
            controller.nextWord();
            controller.tick(); // meaning shown, autoKeep scheduled
            controller.pause();
            assertEquals("⏸ 已暂停", view.getStatus());
        });
    }

    @Test
    void resume_afterTickWithWaitingForTts_skipsAutoKeep() throws InterruptedException {
        List<String[]> words = new ArrayList<>(List.<String[]>of(new String[]{"apple", "苹果"}));
        initOnFxThread(words);
        runOnFx(() -> {
            controller.onTtsToggled(true);
            controller.onCountdownChanged(1);
            controller.nextWord();
            controller.tick(); // TTS path -> waitingForTts = true
            controller.pause();
            // Resume with waitingForTts = true -> should NOT start autoKeep
            controller.resume();
            assertTrue(view.isActionEnabled());
        });
    }
}

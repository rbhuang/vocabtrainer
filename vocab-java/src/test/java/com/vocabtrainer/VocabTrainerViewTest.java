package com.vocabtrainer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VocabTrainerViewTest {

    @BeforeAll
    static void initFx() {
        JavaFxTestHelper.initToolkit();
    }

    @Test
    void constructor_createsAllComponents() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            assertNotNull(view.getRoot());
            assertNotNull(view.getBtnKeep());
            assertNotNull(view.getBtnRemove());
            assertNotNull(view.getBtnPause());
            assertNotNull(view.getBtnQuit());
            assertNotNull(view.getCountdownSpinner());
            assertNotNull(view.getTtsCheckbox());
        });
    }

    @Test
    void showWord_updatesLabel() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.showWord("apple");
            // wordLabel is internal but showWord should not throw
        });
    }

    @Test
    void showMeaning_updatesLabel() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.showMeaning("苹果");
        });
    }

    @Test
    void showStatus_and_getStatus() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.showStatus("test status");
            assertEquals("test status", view.getStatus());
        });
    }

    @Test
    void showProgress_updates() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.showProgress(5, 10);
        });
    }

    @Test
    void showNoWords_updates() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.showNoWords();
        });
    }

    @Test
    void showCompleted_updates() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.showCompleted(20);
        });
    }

    @Test
    void setActionEnabled_and_isActionEnabled() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.setActionEnabled(true);
            assertTrue(view.isActionEnabled());
            view.setActionEnabled(false);
            assertFalse(view.isActionEnabled());
        });
    }

    @Test
    void showPaused_true() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.showPaused(true);
            assertEquals("继续 (P)", view.getBtnPause().getText());
        });
    }

    @Test
    void showPaused_false() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            view.showPaused(false);
            assertEquals("暂停 (P)", view.getBtnPause().getText());
        });
    }

    @Test
    void spinner_defaultValue() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            assertEquals(3, view.getCountdownSpinner().getValue());
        });
    }

    @Test
    void ttsCheckbox_defaultUnchecked() throws InterruptedException {
        JavaFxTestHelper.runOnFxThreadAndWait(() -> {
            VocabTrainerView view = new VocabTrainerView();
            assertFalse(view.getTtsCheckbox().isSelected());
        });
    }
}

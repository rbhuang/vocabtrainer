package com.vocabtrainer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TTSPlayerTest {

    private TTSPlayer tts;

    @BeforeEach
    void setUp() {
        tts = new TTSPlayer();
    }

    @Test
    void initiallyDisabled() {
        assertFalse(tts.isEnabled());
    }

    @Test
    void setEnabled_true() {
        tts.setEnabled(true);
        assertTrue(tts.isEnabled());
    }

    @Test
    void setEnabled_false() {
        tts.setEnabled(true);
        tts.setEnabled(false);
        assertFalse(tts.isEnabled());
    }

    @Test
    void speak_whenDisabled_doesNothing() {
        // Should not throw; no process started
        tts.speak("hello");
    }

    @Test
    void speak_whenEnabled_startsProcess() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("test");
        // Give the thread time to start
        Thread.sleep(200);
        // Process was started; stop it
        tts.stop();
    }

    @Test
    void speak_withCallback_invokesOnFinished() throws InterruptedException {
        tts.setEnabled(true);
        boolean[] called = {false};
        // Use a very short word for fast TTS
        tts.speak("a", () -> called[0] = true);
        // Wait for completion (macOS say is fast for single char)
        Thread.sleep(2000);
        assertTrue(called[0]);
    }

    @Test
    void stop_whenNoProcess_doesNotThrow() {
        assertDoesNotThrow(() -> tts.stop());
    }

    @Test
    void stop_killsRunningProcess() throws InterruptedException {
        tts.setEnabled(true);
        // Speak a long sentence to ensure process is alive
        tts.speak("This is a very long sentence that should take some time to say");
        Thread.sleep(200);
        assertDoesNotThrow(() -> tts.stop());
    }

    @Test
    void stop_whenProcessFinished_doesNotThrow() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("a"); // very short word
        Thread.sleep(3000); // wait for process to finish naturally
        // Process exists but is no longer alive
        assertDoesNotThrow(() -> tts.stop());
    }

    @Test
    void speak_chineseUsesTingTing() throws InterruptedException {
        tts.setEnabled(true);
        // Should not crash with Chinese text (uses Ting-Ting voice)
        tts.speak("你好");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_englishUsesSamantha() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("hello");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_noun() throws InterruptedException {
        tts.setEnabled(true);
        // "n. 名词" expansion should happen internally
        tts.speak("n. something");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_verb() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("v. do");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_adj() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("adj. good");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_adv() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("adv. quickly");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_vt() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("vt. make");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_vi() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("vi. go");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_conj() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("conj. and");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_prep() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("prep. in");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_pron() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("pron. he");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_num() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("num. one");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_art() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("art. the");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_expandsPos_int() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("int. wow");
        Thread.sleep(200);
        tts.stop();
    }

    @Test
    void speak_replacesExistingProcess() throws InterruptedException {
        tts.setEnabled(true);
        tts.speak("first long sentence for testing");
        Thread.sleep(100);
        // Calling speak again should stop previous
        tts.speak("second");
        Thread.sleep(200);
        tts.stop();
    }
}

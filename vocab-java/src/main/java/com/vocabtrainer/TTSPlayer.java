package com.vocabtrainer;

import java.util.regex.Pattern;

/**
 * macOS TTS 播放器，使用系统 say 命令
 */
class TTSPlayer {

    private static final Pattern CJK = Pattern.compile("[\\u4e00-\\u9fff]");
    private static final String[][] POS_MAP = {
        {"\\badj\\.", "形容词"}, {"\\badv\\.", "副词"},
        {"\\bconj\\.", "连词"}, {"\\bprep\\.", "介词"},
        {"\\bpron\\.", "代词"}, {"\\bnum\\.", "数词"},
        {"\\bart\\.", "冠词"}, {"\\bint\\.", "感叹词"},
        {"\\bvt\\.", "及物动词"}, {"\\bvi\\.", "不及物动词"},
        {"\\bv\\.", "动词"}, {"\\bn\\.", "名词"},
    };

    private volatile boolean enabled;
    private volatile Process process;

    void setEnabled(boolean enabled) { this.enabled = enabled; }
    boolean isEnabled() { return enabled; }

    void stop() {
        Process p = process;
        if (p != null && p.isAlive()) {
            p.destroyForcibly();
        }
    }

    void speak(String text) {
        speak(text, null);
    }

    void speak(String text, Runnable onFinished) {
        if (!enabled) return;
        stop();
        String ttsText = expandPos(text);
        String voice = CJK.matcher(ttsText).find() ? "Ting-Ting" : "Samantha";

        Thread t = new Thread(() -> {
            try {
                process = new ProcessBuilder("say", "-v", voice, ttsText).start();
                process.waitFor();
            } catch (Exception e) {
                System.err.println("TTS error: " + e.getMessage());
            } finally {
                if (onFinished != null) onFinished.run();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private static String expandPos(String text) {
        for (String[] pair : POS_MAP) {
            text = text.replaceAll(pair[0], pair[1]);
        }
        return text;
    }
}

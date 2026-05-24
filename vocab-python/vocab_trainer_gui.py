import csv
import random
import os
import sys
import re
import threading
import tempfile
import subprocess
import asyncio
from PyQt6.QtWidgets import (
    QApplication, QWidget, QVBoxLayout, QHBoxLayout,
    QLabel, QPushButton, QSpinBox, QCheckBox
)
from PyQt6.QtCore import QTimer, Qt, QEvent, pyqtSignal
from PyQt6.QtGui import QFont

try:
    import edge_tts
    TTS_AVAILABLE = True
except ImportError:
    TTS_AVAILABLE = False

# --- 常量 ---
WORDS_FILE = 'words.csv'
WORDS_TMP_FILE = 'words_tmp.csv'
DEFAULT_COUNTDOWN = 3
AUTO_KEEP_DELAY_MS = 3000
TRANSITION_DELAY_MS = 800

POS_ABBREVIATIONS = [
    (r'\badj\.', '形容词'), (r'\badv\.', '副词'),
    (r'\bconj\.', '连词'), (r'\bprep\.', '介词'),
    (r'\bpron\.', '代词'), (r'\bnum\.', '数词'),
    (r'\bart\.', '冠词'), (r'\bint\.', '感叹词'),
    (r'\bvt\.', '及物动词'), (r'\bvi\.', '不及物动词'),
    (r'\bv\.', '动词'), (r'\bn\.', '名词'),
]


# --- 工具函数 ---

def resource_path(filename):
    return os.path.join(os.path.expanduser('~'), 'Desktop', filename)


def load_words(file_path):
    words_dict = {}
    try:
        with open(file_path, mode='r', encoding='utf-8-sig') as f:
            for row in csv.reader(f):
                if len(row) >= 2:
                    word, meaning = row[0].strip(), row[1].strip()
                    words_dict.setdefault(word, meaning)
        return list(words_dict.items())
    except FileNotFoundError:
        return []


def save_words(words_list, file_path):
    with open(file_path, mode='w', encoding='utf-8', newline='') as f:
        csv.writer(f).writerows(words_list)


# --- TTS 播放器 ---

class TTSPlayer:
    """后台 edge-tts 语音播放器"""

    def __init__(self, on_finished=None):
        self.enabled = False
        self._process = None
        self._stop_event = threading.Event()
        self._on_finished = on_finished

    def stop(self):
        self._stop_event.set()
        if self._process and self._process.poll() is None:
            self._process.terminate()

    def speak(self, text):
        if not self.enabled or not TTS_AVAILABLE:
            return
        self.stop()
        self._stop_event = threading.Event()
        stop = self._stop_event

        def _run():
            try:
                tts_text = self._expand_pos(text)
                voice = self._detect_voice(tts_text)
                with tempfile.NamedTemporaryFile(suffix='.mp3', delete=False) as f:
                    tmp_path = f.name
                asyncio.run(edge_tts.Communicate(tts_text, voice).save(tmp_path))
                if stop.is_set():
                    os.unlink(tmp_path)
                    return
                self._process = subprocess.Popen(['afplay', tmp_path])
                self._process.wait()
                try:
                    os.unlink(tmp_path)
                except OSError:
                    pass
            except Exception as e:
                print(f"TTS error: {e}")
            finally:
                if not stop.is_set() and self._on_finished:
                    self._on_finished()

        threading.Thread(target=_run, daemon=True).start()

    @staticmethod
    def _detect_voice(text):
        if re.search(r'[\u4e00-\u9fff]', text):
            return "zh-CN-XiaoxiaoNeural"
        return "en-US-AriaNeural"

    @staticmethod
    def _expand_pos(text):
        for pattern, replacement in POS_ABBREVIATIONS:
            text = re.sub(pattern, replacement, text)
        return text


# --- 主界面 ---

class VocabTrainer(QWidget):
    tts_finished = pyqtSignal()

    SHORTCUTS = {'y': 'keep_word', 'n': 'remove_word', 'x': 'close'}

    def __init__(self):
        super().__init__()
        self.setWindowTitle("单词背诵训练器")
        self.setMinimumSize(500, 350)

        self.words_list = self._load_initial_words()
        self.total_words = len(self.words_list)
        self.current_word = ""
        self.current_meaning = ""
        self.default_countdown = DEFAULT_COUNTDOWN
        self.countdown = self.default_countdown
        self._waiting_for_tts = False
        self._paused = False
        self._saved_status = ""

        self.tts = TTSPlayer(on_finished=self.tts_finished.emit)

        self._init_ui()
        self._init_timers()
        self.tts_finished.connect(self._on_tts_finished)
        QApplication.instance().installEventFilter(self)

        if self.words_list:
            self.next_word()
        else:
            self.word_label.setText("未找到词表文件")

    # --- 初始化 ---

    @staticmethod
    def _load_initial_words():
        tmp_path = resource_path(WORDS_TMP_FILE)
        if os.path.exists(tmp_path):
            return load_words(tmp_path)
        return load_words(resource_path(WORDS_FILE))

    def _init_timers(self):
        self.countdown_timer = QTimer()
        self.countdown_timer.timeout.connect(self._tick)
        self.auto_keep_timer = QTimer()
        self.auto_keep_timer.setSingleShot(True)
        self.auto_keep_timer.timeout.connect(self._auto_keep)

    def _init_ui(self):
        layout = QVBoxLayout()
        layout.setSpacing(15)

        self.word_label = self._make_label("", 32, bold=True)
        self.meaning_label = self._make_label("", 20, wrap=True)
        self.status_label = self._make_label("", 14)
        self.progress_label = self._make_label("", 12)

        for w in (self.word_label, self.meaning_label, self.status_label):
            layout.addWidget(w)
        layout.addLayout(self._make_buttons())
        layout.addLayout(self._make_settings_row())
        layout.addWidget(self.progress_label)

        self.setLayout(layout)

    @staticmethod
    def _make_label(text, size, bold=False, wrap=False):
        label = QLabel(text)
        weight = QFont.Weight.Bold if bold else QFont.Weight.Normal
        label.setFont(QFont("Arial", size, weight))
        label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        if wrap:
            label.setWordWrap(True)
        return label

    def _make_buttons(self):
        layout = QHBoxLayout()
        font = QFont("Arial", 14)
        buttons = [
            ("保留 (Y)", self.keep_word),
            ("记住了 (N)", self.remove_word),
            ("暂停 (P)", self.toggle_pause),
            ("退出 (X)", self.close),
        ]
        for text, slot in buttons:
            btn = QPushButton(text)
            btn.setFont(font)
            btn.clicked.connect(slot)
            layout.addWidget(btn)
        self.btn_keep, self.btn_remove, self.btn_pause, self.btn_quit = (
            layout.itemAt(i).widget() for i in range(4)
        )
        return layout

    def _make_settings_row(self):
        row = QHBoxLayout()
        font = QFont("Arial", 12)

        timer_label = QLabel("倒计时(秒):")
        timer_label.setFont(font)
        row.addWidget(timer_label)

        self.countdown_spin = QSpinBox()
        self.countdown_spin.setRange(1, 30)
        self.countdown_spin.setValue(self.default_countdown)
        self.countdown_spin.setFont(font)
        self.countdown_spin.valueChanged.connect(self._on_countdown_changed)
        row.addWidget(self.countdown_spin)

        row.addStretch()

        self.tts_checkbox = QCheckBox("🔊 文字转语音")
        self.tts_checkbox.setFont(font)
        self.tts_checkbox.setChecked(False)
        self.tts_checkbox.setEnabled(TTS_AVAILABLE)
        if not TTS_AVAILABLE:
            self.tts_checkbox.setToolTip("需要安装 edge-tts: pip install edge-tts")
        self.tts_checkbox.toggled.connect(self._on_tts_toggled)
        row.addWidget(self.tts_checkbox)

        return row

    # --- 事件处理 ---

    def eventFilter(self, obj, event):
        if event.type() == QEvent.Type.KeyPress:
            key = event.text().lower()
            if key == 'p':
                self.toggle_pause()
                return True
            if self.btn_keep.isEnabled() and not self._paused:
                method = self.SHORTCUTS.get(key)
                if method:
                    getattr(self, method)()
                    return True
        return super().eventFilter(obj, event)

    def closeEvent(self, event):
        self._cancel_auto_keep()
        self.countdown_timer.stop()
        self.tts.stop()
        if self.words_list:
            save_words(self.words_list, resource_path(WORDS_TMP_FILE))
        super().closeEvent(event)

    def _on_countdown_changed(self, value):
        self.default_countdown = value

    def _on_tts_toggled(self, checked):
        self.tts.enabled = checked
        if not checked:
            self.tts.stop()

    def toggle_pause(self):
        if self._paused:
            self._resume()
        else:
            self._pause()

    def _pause(self):
        self._paused = True
        self.countdown_timer.stop()
        self.auto_keep_timer.stop()
        self.tts.stop()
        self._set_buttons_enabled(False)
        self.btn_pause.setText("继续 (P)")
        self._saved_status = self.status_label.text()
        self.status_label.setText("⏸ 已暂停")

    def _resume(self):
        self._paused = False
        self.btn_pause.setText("暂停 (P)")
        self.status_label.setText(self._saved_status)
        if self.countdown > 0:
            self._set_buttons_enabled(False)
            self.countdown_timer.start(1000)
        else:
            self._set_buttons_enabled(True)
            if not self._waiting_for_tts:
                self.auto_keep_timer.start(AUTO_KEEP_DELAY_MS)

    # --- 核心训练逻辑 ---

    def next_word(self):
        self.current_word, self.current_meaning = random.choice(self.words_list)
        self.word_label.setText(self.current_word)
        self.meaning_label.setText("")
        self.tts.speak(self.current_word)
        self.countdown = self.default_countdown
        self.status_label.setText(f"倒计时: {self.countdown} 秒...")
        self._update_progress()
        self._set_buttons_enabled(False)
        self.countdown_timer.start(1000)

    def _tick(self):
        self.countdown -= 1
        if self.countdown > 0:
            self.status_label.setText(f"倒计时: {self.countdown} 秒...")
            return

        self.countdown_timer.stop()
        self.meaning_label.setText(self.current_meaning)
        self.status_label.setText("")
        self._set_buttons_enabled(True)

        if self.tts.enabled:
            self._waiting_for_tts = True
            self.tts.speak(self.current_meaning)
        else:
            self.auto_keep_timer.start(AUTO_KEEP_DELAY_MS)

    def _on_tts_finished(self):
        if self._waiting_for_tts and self.btn_keep.isEnabled():
            self._waiting_for_tts = False
            self.auto_keep_timer.start(AUTO_KEEP_DELAY_MS)

    def _auto_keep(self):
        if self.btn_keep.isEnabled():
            self.keep_word()

    def _cancel_auto_keep(self):
        self._waiting_for_tts = False
        self.auto_keep_timer.stop()

    def keep_word(self):
        if not self.words_list:
            return
        self._cancel_auto_keep()
        self.countdown_timer.stop()
        self.meaning_label.setText(self.current_meaning)
        QTimer.singleShot(TRANSITION_DELAY_MS, self.next_word)

    def remove_word(self):
        if not self.words_list:
            return
        self._cancel_auto_keep()
        self.countdown_timer.stop()
        self.meaning_label.setText(self.current_meaning)
        self.words_list.remove((self.current_word, self.current_meaning))
        self.status_label.setText(f"✨ '{self.current_word}' 已移除")
        self._update_progress()

        if not self.words_list:
            self._set_buttons_enabled(False)
            self.word_label.setText("🎉 恭喜！")
            self.meaning_label.setText(f"你已完成全部 {self.total_words} 个单词！")
            self.status_label.setText("")
        else:
            QTimer.singleShot(TRANSITION_DELAY_MS, self.next_word)

    # --- 辅助方法 ---

    def _update_progress(self):
        self.progress_label.setText(f"剩余: {len(self.words_list)} / {self.total_words}")

    def _set_buttons_enabled(self, enabled):
        self.btn_keep.setEnabled(enabled)
        self.btn_remove.setEnabled(enabled)


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = VocabTrainer()
    window.show()
    sys.exit(app.exec())

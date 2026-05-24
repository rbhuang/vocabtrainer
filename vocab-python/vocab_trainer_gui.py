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
    QLabel, QPushButton, QMessageBox, QSpinBox, QCheckBox
)
from PyQt6.QtCore import QTimer, Qt, QEvent, pyqtSignal
from PyQt6.QtGui import QFont

try:
    import edge_tts
    TTS_AVAILABLE = True
except ImportError:
    TTS_AVAILABLE = False


def resource_path(filename):
    """返回桌面目录下的文件路径"""
    desktop = os.path.join(os.path.expanduser('~'), 'Desktop')
    return os.path.join(desktop, filename)


def load_words(file_path):
    """从 CSV 加载单词并去重"""
    words_dict = {}
    try:
        with open(file_path, mode='r', encoding='utf-8-sig') as f:
            reader = csv.reader(f)
            for row in reader:
                if len(row) >= 2:
                    word, meaning = row[0].strip(), row[1].strip()
                    if word not in words_dict:
                        words_dict[word] = meaning
        return list(words_dict.items())
    except FileNotFoundError:
        return []


def save_words(words_list, file_path='words_tmp.csv'):
    """将当前词表保存到临时 CSV 文件"""
    with open(file_path, mode='w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        for word, meaning in words_list:
            writer.writerow([word, meaning])


class VocabTrainer(QWidget):
    tts_finished = pyqtSignal()

    def __init__(self):
        super().__init__()
        self.setWindowTitle("单词背诵训练器")
        self.setMinimumSize(500, 350)

        # 加载词表
        tmp_path = resource_path('words_tmp.csv')
        csv_path = resource_path('words.csv')
        if os.path.exists(tmp_path):
            self.words_list = load_words(tmp_path)
        else:
            self.words_list = load_words(csv_path)

        self.total_words = len(self.words_list)
        self.default_countdown = 3
        self.countdown = self.default_countdown
        self.current_word = ""
        self.current_meaning = ""
        self.tts_enabled = False
        self.tts_process = None
        self._tts_stop = threading.Event()
        self.auto_keep_timer = None
        self._waiting_for_tts = False

        self.init_ui()
        self.tts_finished.connect(self._on_tts_finished)
        QApplication.instance().installEventFilter(self)
        self.timer = QTimer()
        self.timer.timeout.connect(self.tick)

        if self.words_list:
            self.next_word()
        else:
            self.word_label.setText("未找到词表文件")

    def init_ui(self):
        layout = QVBoxLayout()
        layout.setSpacing(15)

        # 单词显示
        self.word_label = QLabel("")
        self.word_label.setFont(QFont("Arial", 32, QFont.Weight.Bold))
        self.word_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        layout.addWidget(self.word_label)

        # 释义显示
        self.meaning_label = QLabel("")
        self.meaning_label.setFont(QFont("Arial", 20))
        self.meaning_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.meaning_label.setWordWrap(True)
        layout.addWidget(self.meaning_label)

        # 倒计时/状态
        self.status_label = QLabel("")
        self.status_label.setFont(QFont("Arial", 14))
        self.status_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        layout.addWidget(self.status_label)

        # 按钮
        btn_layout = QHBoxLayout()

        self.btn_keep = QPushButton("保留 (Y)")
        self.btn_keep.setFont(QFont("Arial", 14))
        self.btn_keep.clicked.connect(self.keep_word)
        btn_layout.addWidget(self.btn_keep)

        self.btn_remove = QPushButton("记住了 (N)")
        self.btn_remove.setFont(QFont("Arial", 14))
        self.btn_remove.clicked.connect(self.remove_word)
        btn_layout.addWidget(self.btn_remove)

        self.btn_save = QPushButton("保存退出 (S)")
        self.btn_save.setFont(QFont("Arial", 14))
        self.btn_save.clicked.connect(self.save_and_quit)
        btn_layout.addWidget(self.btn_save)

        layout.addLayout(btn_layout)

        # 倒计时设置
        timer_layout = QHBoxLayout()
        timer_label = QLabel("倒计时(秒):")
        timer_label.setFont(QFont("Arial", 12))
        timer_layout.addWidget(timer_label)
        self.countdown_spin = QSpinBox()
        self.countdown_spin.setRange(1, 30)
        self.countdown_spin.setValue(self.default_countdown)
        self.countdown_spin.setFont(QFont("Arial", 12))
        self.countdown_spin.valueChanged.connect(self.on_countdown_changed)
        timer_layout.addWidget(self.countdown_spin)
        timer_layout.addStretch()
        self.tts_checkbox = QCheckBox("\U0001f50a 文字转语音")
        self.tts_checkbox.setFont(QFont("Arial", 12))
        self.tts_checkbox.setChecked(False)
        self.tts_checkbox.setEnabled(TTS_AVAILABLE)
        if not TTS_AVAILABLE:
            self.tts_checkbox.setToolTip("需要安装 edge-tts: pip install edge-tts")
        self.tts_checkbox.toggled.connect(self._on_tts_toggled)
        timer_layout.addWidget(self.tts_checkbox)
        layout.addLayout(timer_layout)

        # 剩余单词数
        self.progress_label = QLabel("")
        self.progress_label.setFont(QFont("Arial", 12))
        self.progress_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        layout.addWidget(self.progress_label)

        # 快捷键提示
        shortcut_label = QLabel("快捷键: Y=保留  N=记住了  S=保存退出")
        shortcut_label.setFont(QFont("Arial", 11))
        shortcut_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        shortcut_label.setStyleSheet("color: gray;")
        layout.addWidget(shortcut_label)

        self.setLayout(layout)

        # 快捷键通过 eventFilter 处理

    def eventFilter(self, obj, event):
        """拦截所有按键事件，确保快捷键在任何焦点下都生效"""
        if event.type() == QEvent.Type.KeyPress:
            key = event.text().lower()
            if self.btn_keep.isEnabled():
                if key == 'y':
                    self.keep_word()
                    return True
                elif key == 'n':
                    self.remove_word()
                    return True
                elif key == 's':
                    self.save_and_quit()
                    return True
        return super().eventFilter(obj, event)

    def keyPressEvent(self, event):
        """全局按键监听，不受焦点影响"""
        key = event.text().lower()
        if self.btn_keep.isEnabled():
            if key == 'y':
                self.keep_word()
                return
            elif key == 'n':
                self.remove_word()
                return
            elif key == 's':
                self.save_and_quit()
                return
        super().keyPressEvent(event)

    def on_countdown_changed(self, value):
        self.default_countdown = value

    def _on_tts_toggled(self, checked):
        self.tts_enabled = checked
        if not checked:
            self._stop_tts()

    def _stop_tts(self):
        """停止当前TTS播放"""
        self._tts_stop.set()
        if self.tts_process and self.tts_process.poll() is None:
            self.tts_process.terminate()

    _POS_ABBREVS = [
        (r'\badj\.', '形容词'),
        (r'\badv\.', '副词'),
        (r'\bconj\.', '连词'),
        (r'\bprep\.', '介词'),
        (r'\bpron\.', '代词'),
        (r'\bnum\.', '数词'),
        (r'\bart\.', '冠词'),
        (r'\bint\.', '感叹词'),
        (r'\bvt\.', '及物动词'),
        (r'\bvi\.', '不及物动词'),
        (r'\bv\.', '动词'),
        (r'\bn\.', '名词'),
    ]

    def _expand_pos(self, text):
        """将词性缩写展开为中文"""
        for pattern, replacement in self._POS_ABBREVS:
            text = re.sub(pattern, replacement, text)
        return text

    def _speak(self, text):
        """后台生成并播放TTS"""
        if not self.tts_enabled or not TTS_AVAILABLE:
            return
        self._stop_tts()
        self._tts_stop = threading.Event()
        stop_event = self._tts_stop

        def _run():
            try:
                tts_text = self._expand_pos(text)
                if re.search(r'[\u4e00-\u9fff]', tts_text):
                    voice = "zh-CN-XiaoxiaoNeural"
                else:
                    voice = "en-US-AriaNeural"
                with tempfile.NamedTemporaryFile(suffix='.mp3', delete=False) as f:
                    tmp_path = f.name
                asyncio.run(edge_tts.Communicate(tts_text, voice).save(tmp_path))
                if stop_event.is_set():
                    os.unlink(tmp_path)
                    return
                self.tts_process = subprocess.Popen(['afplay', tmp_path])
                self.tts_process.wait()
                try:
                    os.unlink(tmp_path)
                except OSError:
                    pass
                if not stop_event.is_set():
                    self.tts_finished.emit()
            except Exception as e:
                print(f"TTS error: {e}")
                if not stop_event.is_set():
                    self.tts_finished.emit()

        threading.Thread(target=_run, daemon=True).start()

    def next_word(self):
        """抽取下一个单词并开始倒计时"""
        self.current_word, self.current_meaning = random.choice(self.words_list)
        self.word_label.setText(self.current_word)
        self.meaning_label.setText("")
        self._speak(self.current_word)
        self.countdown = self.default_countdown
        self.status_label.setText(f"倒计时: {self.countdown} 秒...")
        self.progress_label.setText(f"剩余: {len(self.words_list)} / {self.total_words}")
        self.set_buttons_enabled(False)
        self.timer.start(1000)

    def tick(self):
        """每秒倒计时"""
        self.countdown -= 1
        if self.countdown > 0:
            self.status_label.setText(f"倒计时: {self.countdown} 秒...")
        else:
            self.timer.stop()
            self.meaning_label.setText(self.current_meaning)
            self.set_buttons_enabled(True)
            if self.tts_enabled:
                self._waiting_for_tts = True
                self._speak(self.current_meaning)
                self.status_label.setText("请选择：保留 / 记住了 / 保存退出 (语音播完后自动保留)")
            else:
                self.status_label.setText("请选择：保留 / 记住了 / 保存退出 (3秒后自动保留)")
                self.auto_keep_timer = QTimer.singleShot(3000, self._auto_keep)

    def _on_tts_finished(self):
        """TTS播放完成回调"""
        if self._waiting_for_tts and self.btn_keep.isEnabled():
            self._waiting_for_tts = False
            self.status_label.setText("请选择：保留 / 记住了 / 保存退出 (3秒后自动保留)")
            self.auto_keep_timer = QTimer.singleShot(3000, self._auto_keep)

    def _auto_keep(self):
        """自动保留当前单词"""
        if self.btn_keep.isEnabled():
            self.keep_word()

    def _cancel_auto_keep(self):
        """取消自动保留定时器"""
        self._waiting_for_tts = False
        self.auto_keep_timer = None

    def keep_word(self):
        """保留单词"""
        if not self.words_list:
            return
        self._cancel_auto_keep()
        self.timer.stop()
        self.meaning_label.setText(self.current_meaning)
        self.status_label.setText(f"🚩 '{self.current_word}' 会再次出现")
        QTimer.singleShot(800, self.next_word)

    def remove_word(self):
        """移除单词"""
        if not self.words_list:
            return
        self._cancel_auto_keep()
        self.timer.stop()
        self.meaning_label.setText(self.current_meaning)
        self.words_list.remove((self.current_word, self.current_meaning))
        self.status_label.setText(f"✨ '{self.current_word}' 已移除")
        self.progress_label.setText(f"剩余: {len(self.words_list)} / {self.total_words}")

        if not self.words_list:
            self.set_buttons_enabled(False)
            self.word_label.setText("🎉 恭喜！")
            self.meaning_label.setText(f"你已完成全部 {self.total_words} 个单词！")
            self.status_label.setText("")
        else:
            QTimer.singleShot(800, self.next_word)

    def save_and_quit(self):
        """保存进度并退出"""
        self._cancel_auto_keep()
        if self.words_list:
            save_words(self.words_list, resource_path('words_tmp.csv'))
        self.timer.stop()
        QMessageBox.information(self, "已保存", f"已保存 {len(self.words_list)} 个单词到 words_tmp.csv")
        self.close()

    def closeEvent(self, event):
        self._stop_tts()
        super().closeEvent(event)

    def set_buttons_enabled(self, enabled):
        self.btn_keep.setEnabled(enabled)
        self.btn_remove.setEnabled(enabled)
        self.btn_save.setEnabled(enabled)


if __name__ == "__main__":
    app = QApplication(sys.argv)
    window = VocabTrainer()
    window.show()
    sys.exit(app.exec())

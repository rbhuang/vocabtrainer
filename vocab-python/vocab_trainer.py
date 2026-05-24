import csv
import time
import random
import os
import sys
import tty
import termios
import select

def load_words(file_path):
    """从 CSV 加载单词并去重"""
    words_dict = {}
    try:
        with open(file_path, mode='r', encoding='utf-8-sig') as f:
            reader = csv.reader(f)
            for row in reader:
                if len(row) >= 2:
                    word, meaning = row[0].strip(), row[1].strip()
                    # 自动去重：如果单词已存在则跳过，确保最终为 300 个独立单词
                    if word not in words_dict:
                        words_dict[word] = meaning
        return list(words_dict.items())
    except FileNotFoundError:
        print(f"错误：找不到文件 {file_path}，请确保文件在当前目录下。")
        return []

def save_words(words_list, file_path='words_tmp.csv'):
    """将当前词表保存到临时 CSV 文件"""
    with open(file_path, mode='w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        for word, meaning in words_list:
            writer.writerow([word, meaning])
    print(f"💾 已保存 {len(words_list)} 个单词到 {file_path}")

def clear_screen():
    """清除屏幕，让界面更整洁"""
    os.system('cls' if os.name == 'nt' else 'clear')

def get_key(timeout=None):
    """读取单个按键，无需回车。timeout为None时无限等待。"""
    fd = sys.stdin.fileno()
    old_settings = termios.tcgetattr(fd)
    try:
        tty.setraw(fd)
        if timeout is not None:
            ready, _, _ = select.select([sys.stdin], [], [], timeout)
            if not ready:
                return None
        ch = sys.stdin.read(1)
        return ch.lower()
    finally:
        termios.tcsetattr(fd, termios.TCSADRAIN, old_settings)

def start_training():
    if os.path.exists('words_tmp.csv'):
        words_list = load_words('words_tmp.csv')
        print("📂 检测到上次保存的进度，已从 words_tmp.csv 加载。")
    else:
        words_list = load_words('words.csv')
    
    if not words_list:
        return

    total_words = len(words_list)
    print(f"✅ 已成功加载 {total_words} 个独立单词。")
    print("背诵规则：显示单词后有 3 秒思考时间，随后显示释义。")
    print("输入 'y' 保留单词（下次还会出现），输入 'n' 移除单词（彻底记住）。")
    print("输入 's' 保存当前进度并退出。")
    input("按回车键开始挑战...")

    while words_list:
        # 🔄 随机抽取机制 [1]
        current_word, meaning = random.choice(words_list)
        
        clear_screen()
        print(f"\n📝 单词: {current_word}")
        print("-" * 30)
        
        # ⏳ 3秒倒计时，期间可随时输入 y/n/s
        print("(可随时按 y/n/s 响应)")
        user_input = None
        for i in range(3, 0, -1):
            print(f"倒计时: {i} 秒...", end='\r')
            key = get_key(timeout=1.0)
            if key in ('y', 'n', 's'):
                user_input = key
                break
        
        # 💡 显示释义 [3]
        print(f"\n💡 释义: {meaning}")
        print("-" * 30)

        # 📥 如果倒计时期间未输入，则等待用户按键
        if user_input is None:
            print("保留此单词吗？(y/n/s): ", end='', flush=True)
            while True:
                key = get_key()
                if key in ('y', 'n', 's'):
                    user_input = key
                    print(user_input)
                    break
        
        if user_input == 's':
            save_words(words_list)
            return
        elif user_input == 'n':
            # 彻底记住，从词库移除
            words_list.remove((current_word, meaning))
            print(f"✨ 太棒了！'{current_word}' 已从计划中移除。")
        else:
            # 保留单词，继续留在循环中
            print(f"🚩 没关系，'{current_word}' 会再次出现。")
        
        print(f"当前剩余单词量: {len(words_list)}")
        time.sleep(1)

    clear_screen()
    print(f"🎉 恭喜！你已经完成了所有 {total_words} 个单词的挑战！")

if __name__ == "__main__":
    start_training()
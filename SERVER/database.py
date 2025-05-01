import sqlite3
from sqlite3 import Error

DATABASE_NAME = "lecture_system.db"

def create_connection():
    """Создаёт подключение к SQLite базе данных"""
    conn = None
    try:
        conn = sqlite3.connect(DATABASE_NAME)
        print(f"Успешное подключение к SQLite DB {DATABASE_NAME}")
        return conn
    except Error as e:
        print(f"Ошибка при подключении к SQLite: {e}")
    return conn

def create_tables(conn):
    """Создаёт все таблицы в базе данных"""
    sql_commands = [
        """CREATE TABLE IF NOT EXISTS user (
            user_id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            email TEXT UNIQUE NOT NULL,
            password_hash TEXT NOT NULL
        )""",
        
        """CREATE TABLE IF NOT EXISTS session (
            session_id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            title TEXT NOT NULL,
            start_time TEXT,
            end_time TEXT,
            status TEXT CHECK(status IN ('active', 'completed', 'processed', 'failed')),
            camera_settings INTEGER,
            FOREIGN KEY (user_id) REFERENCES user(user_id)
        )""",
        
        """CREATE TABLE IF NOT EXISTS photo (
            photo_id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id INTEGER NOT NULL,
            created_date TEXT,
            status TEXT CHECK(status IN ('raw', 'processed')),
            has_changes INTEGER CHECK(has_changes IN (0, 1)),
            file_path TEXT,
            FOREIGN KEY (session_id) REFERENCES session(session_id)
        )""",
        
        """CREATE TABLE IF NOT EXISTS pdf (
            pdf_id INTEGER PRIMARY KEY AUTOINCREMENT,
            session_id INTEGER UNIQUE NOT NULL,
            created_date TEXT,
            file_path TEXT,
            FOREIGN KEY (session_id) REFERENCES session(session_id)
        )"""
    ]
    
    try:
        c = conn.cursor()
        for command in sql_commands:
            c.execute(command)
        conn.commit()
        print("Все таблицы успешно созданы")
    except Error as e:
        print(f"Ошибка при создании таблиц: {e}")

def create_indexes(conn):
    """Создаёт необходимые индексы"""
    index_commands = [
        "CREATE INDEX IF NOT EXISTS idx_user_email ON user(email)",
        "CREATE INDEX IF NOT EXISTS idx_session_user ON session(user_id)",
        "CREATE INDEX IF NOT EXISTS idx_session_status ON session(status)",
        "CREATE INDEX IF NOT EXISTS idx_photo_session ON photo(session_id)",
        "CREATE INDEX IF NOT EXISTS idx_photo_changes ON photo(has_changes)"
    ]
    
    try:
        c = conn.cursor()
        for command in index_commands:
            c.execute(command)
        conn.commit()
        print("Все индексы успешно созданы")
    except Error as e:
        print(f"Ошибка при создании индексов: {e}")

def initialize_database():
    """Инициализирует базу данных"""
    conn = create_connection()
    if conn is not None:
        create_tables(conn)
        create_indexes(conn)
        conn.close()
    else:
        print("Ошибка! Не удалось создать подключение к БД")

if __name__ == '__main__':
    initialize_database()
    print(f"База данных {DATABASE_NAME} готова к использованию")
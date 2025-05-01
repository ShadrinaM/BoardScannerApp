import sqlite3
# Подключаемся к базе
conn = sqlite3.connect('lecture_system.db')
cursor = conn.cursor()
# Получаем список всех таблиц
print("\nВсе таблицы в базе:")
cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
for table in cursor.fetchall():
    print(f"- {table[0]}")
# Получаем схему каждой таблицы
print("\nПодробная схема:")
cursor.execute("SELECT sql FROM sqlite_master WHERE sql NOT NULL;")
for schema in cursor.fetchall():
    print(schema[0])
conn.close()
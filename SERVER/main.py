from flask import Flask, request, jsonify
import sqlite3
from database import create_connection
import uuid
import os

app = Flask(__name__)
DATABASE_NAME = "lecture_system.db"

# Инициализация БД при запуске
if not os.path.exists(DATABASE_NAME):
    from database import initialize_database
    initialize_database()

# API Endpoints
@app.route('/api/register', methods=['POST'])
def register():
    """Регистрация нового пользователя"""
    data = request.json
    try:
        conn = create_connection()
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO user (name, email, password_hash) VALUES (?, ?, ?)",
            (data['name'], data['email'], data['password_hash'])
        )
        conn.commit()
        user_id = cursor.lastrowid
        conn.close()
        return jsonify({"status": "success", "user_id": user_id}), 201
    except sqlite3.IntegrityError:
        return jsonify({"status": "error", "message": "Email already exists"}), 400
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/login', methods=['POST'])
def login():
    """Аутентификация пользователя"""
    data = request.json
    try:
        conn = create_connection()
        cursor = conn.cursor()
        cursor.execute(
            "SELECT user_id, name, email FROM user WHERE email = ? AND password_hash = ?",
            (data['email'], data['password_hash'])
        )
        user = cursor.fetchone()
        conn.close()
        
        if user:
            return jsonify({
                "status": "success",
                "user": {
                    "user_id": user[0],
                    "name": user[1],
                    "email": user[2]
                }
            }), 200
        else:
            return jsonify({"status": "error", "message": "Invalid credentials"}), 401
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/api/sessions', methods=['POST'])
def create_session():
    """Создание новой сессии записи"""
    data = request.json
    try:
        conn = create_connection()
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO session (user_id, title, start_time, status, camera_settings) VALUES (?, ?, datetime('now'), 'active', ?)",
            (data['user_id'], data['title'], data.get('camera_settings', 0))
        )
        session_id = cursor.lastrowid
        conn.commit()
        conn.close()
        return jsonify({"status": "success", "session_id": session_id}), 201
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
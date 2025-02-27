from flask import Flask, request, jsonify
from flask_mysqldb import MySQL
from flask_cors import CORS
from flask_socketio import SocketIO, emit
from werkzeug.utils import secure_filename
import subprocess
import os

app = Flask(__name__)
CORS(app)
socketio = SocketIO(app, cors_allowed_origins="*")

app.config['MYSQL_USER'] = 'anny'
app.config['MYSQL_PASSWORD'] = 'anny123456'
app.config['MYSQL_DB'] = 'app'
app.config['MYSQL_HOST'] = 'localhost'

mysql = MySQL(app)

device_states = {
    "Fan": {"status": "OFF", "last_user": None},
    "Light": {"status": "OFF", "last_user": None},
    "AC": {"status": "OFF", "last_user": None}
}

@app.route('/login', methods=['POST'])
def login():
    try:
        data = request.get_json()
        if not data:
            return jsonify(success=False, error="No JSON data provided")

        name = data.get('name')
        pwd = data.get('pwd')

        if not name:
            return jsonify(success=False, error="Missing 'name' in JSON data")

        conn = mysql.connection
        cursor = conn.cursor()

        if pwd:
            cursor.execute("SELECT * FROM user WHERE name = %s AND pwd = %s", (name, pwd))
        else:
            cursor.execute("SELECT * FROM user WHERE name = %s", (name,))
        
        results = cursor.fetchall()
        cursor.close()

        if results:
            return jsonify(success=True)
        else:
            return jsonify(success=False)
    except Exception as e:
        return jsonify(success=False, error=str(e))


UPLOAD_FOLDER = 'face_dataset/faces'
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

ALLOWED_EXTENSIONS = {'jpg', 'jpeg', 'png'}

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

@app.route('/uploadImagePath', methods=['POST'])
def upload_image_path():
    try:
        if 'image' not in request.files:
            return jsonify(success=False, error="No file part")
        
        file = request.files['image']
        if file.filename == '':
            return jsonify(success=False, error="No selected file")
        
        if file and allowed_file(file.filename):
            filename = secure_filename(file.filename)
            save_path = os.path.join(app.config['UPLOAD_FOLDER'], filename)
            save_path = save_path.replace("\\", "/")
            file.save(save_path)

            print(f"Image saved to: {save_path}")
            
            script_path = '"D:/SmartHomeControlSystem/backend/face/face_recognition/opencv_realtime.py"'
            result = subprocess.run(
                ['python', script_path, '--image_path', save_path],
                capture_output=True, text=True
            )

            print(f"stdout: {result.stdout}")
            print(f"stderr: {result.stderr}")
            print(f"returncode: {result.returncode}")

            if result.returncode == 0:
                recognized_name = result.stdout.strip().split('\n')[-1]
                print(f"Recognized name: {recognized_name}")

                login_response = app.test_client().post('/login', json={
                    'name': recognized_name
                })

                login_data = login_response.get_json()

                if login_data.get('success'):
                    return jsonify(success=True, username=recognized_name)
                else:
                    return jsonify(success=False, error="Login failed")
            else:
                return jsonify(success=False, error="Face recognition script failed.")
        else:
            return jsonify(success=False, error="Invalid file format or no file provided")
    except Exception as e:
        return jsonify(success=False, error=str(e))
        
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=3000, debug=True)


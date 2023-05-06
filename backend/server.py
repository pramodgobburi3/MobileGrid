import os
from flask import Flask, request
import firebase_admin
import uuid
from firebase_admin import credentials, db, storage
from responses import *
from worker_util import *
from dotenv import load_dotenv

load_dotenv()

required_keys = ['scriptName', 'scriptArgs', 'upload']

cred = credentials.Certificate('serviceAccountKey.json')
firebase_admin.initialize_app(cred, {
  'databaseURL': os.environ.get('FIREBASE_DATABASE_URL'),
  'storageBucket': os.environ.get('FIREBASE_STORAGE_BUCKET')
})

tasks_db = db.reference('tasks')
devices_db = db.reference('devices')
bucket = storage.bucket()

app = Flask(__name__)
app.config['UPLOAD_FOLDER'] = 'uploads'

def validate_json(json_object, required_keys):
  if not json_object:
    return False

  for key in required_keys:
    if key not in json_object:
      return False

  return True


@app.route("/task", methods=["POST"])
def create_task():
  task_definition = request.get_json()
  
  if not task_definition:
    return bad_request_response("No task definition provided")
  
  if validate_json(task_definition, required_keys):
    task_id = str(uuid.uuid4())
    task_definition['status'] = "awaiting_upload"
    tasks_db.child(task_id).set(task_definition)
    return success_response(task_id)
  else:
    return bad_request_response("Missing required keys")

@app.route("/task/<id>", methods=["POST"])
def upload(id):
  task_ref = tasks_db.child(id)
  task_snapshot = task_ref.get()

  if task_snapshot is None:
    return bad_request_response("Invalid task")
  
  file = request.files.get('file')

  if not file:
    return bad_request_response("No file provided")
  
  if file.filename != id + ".zip":
    return bad_request_response("Invalid file, the filename must match the id")
  
  file_path = os.path.join(os.getcwd(), app.config['UPLOAD_FOLDER'], file.filename)
  file.save(file_path)

  send_to_file_upload_worker(file_path, file.filename, id)
  return success_response("Successfully uploaded file")

@app.route("/device/<device_id>/task/<task_id>/complete", methods=["POST"])
def complete_task(device_id, task_id):
  task_ref = tasks_db.child(task_id)
  task_snapshot = task_ref.get()
  device_ref = devices_db.child(device_id)
  device_snapshot = device_ref.get()

  if task_snapshot is None:
    return bad_request_response("Invalid task")
  if device_snapshot is None:
    return bad_request_response("Invalid device identifier")
  task_ref.update({'status': 'completed'})
  task_ref.child("assignedTaskId").delete();
  device_ref.update({'status': 'available'})
  send_to_pending_scheduler(device_id)
  return success_response("")

if __name__ == "__main__":
  app.run(host='0.0.0.0', debug=True)
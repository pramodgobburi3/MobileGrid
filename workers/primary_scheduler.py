import os
import json
import pika
import firebase_admin
from firebase_admin import db, credentials
from dotenv import load_dotenv

def get_conenction_params():
  creds = pika.PlainCredentials(
    os.environ.get('RABBITMQ_USERNAME'), 
    os.environ.get('RABBITMQ_PASSWORD')
  )
  params = pika.ConnectionParameters(
    os.environ.get('RABBITMQ_HOST'),
    5672, 
    os.environ.get('RABBITMQ_VIRTUAL_HOST'), 
    credentials=creds
  )
  return params

def initialize_firebase():
  if not firebase_admin._apps:
    cred = credentials.Certificate('../backend/serviceAccountKey.json')
    firebase_admin.initialize_app(cred, {
      'databaseURL': os.environ.get('FIREBASE_DATABASE_URL'),
      'storageBucket': os.environ.get('FIREBASE_STORAGE_BUCKET')
    })

def find_available_device():
  devices_ref = db.reference('devices')
  snapshot = devices_ref.order_by_child('status').equal_to('available').limit_to_first(1).get()

  if snapshot:
    device_id = list(snapshot.keys())[0]
    return device_id
  return None

def assign_task_to_device(task_id):
  device_id = find_available_device()

  if device_id:
    device_ref = db.reference('devices').child(device_id)
    task_ref = db.reference('tasks').child(task_id)
    if task_ref.child('status').get() == 'available':
      device_ref.update({'assignedTaskId': task_id, 'status': 'assigned'})
      task_ref.update({'status': 'assigned'})
      print(f"Task {task_id} assigned to device {device_id}")
    else:
      print(f"Invalid status for task {task_id}")
  else:
      print(f"No available device for task {task_id}. Task placed on hold.")

def process_assign_task(ch, method, properties, body):
  data = json.loads(body)
  task_id = data.get('task_id')
  assign_task_to_device(task_id)
  ch.basic_ack(delivery_tag=method.delivery_tag)

def start_worker():
  params = get_conenction_params()
  connection = pika.BlockingConnection(params)
  channel = connection.channel()
  channel.queue_declare(queue='assign_task_queue', durable=True)
  channel.basic_qos(prefetch_count=1)
  channel.basic_consume(queue='assign_task_queue', on_message_callback=process_assign_task)
  channel.start_consuming()

if __name__ == '__main__':
  load_dotenv()
  initialize_firebase()
  start_worker()

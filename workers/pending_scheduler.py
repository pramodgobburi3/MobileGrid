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
    cred = credentials.Certificate('backend/serviceAccountKey.json')
    firebase_admin.initialize_app(cred, {
      'databaseURL': os.environ.get('FIREBASE_DATABASE_URL'),
      'storageBucket': os.environ.get('FIREBASE_STORAGE_BUCKET')
    })

def find_pending_task():
  tasks_ref = db.reference('tasks')
  snapshot = tasks_ref.order_by_child('status').equal_to('available').limit_to_first(1).get()

  if snapshot:
    task_id = list(snapshot.keys())[0]
    return task_id
  return None

def assign_device_to_task(device_id):
  device_ref = db.reference('devices').child(device_id)
  task_id = find_pending_task()

  if device_ref.get() is None or task_id is None:
    print(f"Unable to assign task {task_id} to device {device_id}")
    return
  task_ref = db.reference('tasks').child(task_id)
  device_ref.update({'assignedTaskId': task_id, 'status': 'assigned'})
  task_ref.update({'status': "assigned"})
  print(f"Device {device_id} assigned to device {task_id}")
  
def process_pending_device(ch, method, properties, body):
  data = json.loads(body)
  device_id = data.get('device_id')
  assign_device_to_task(device_id)
  ch.basic_ack(delivery_tag=method.delivery_tag)

def start_worker():
  params = get_conenction_params()
  connection = pika.BlockingConnection(params)
  channel = connection.channel()
  channel.queue_declare(queue='pending_device_queue', durable=True)
  channel.basic_qos(prefetch_count=1)
  channel.basic_consume(queue='pending_device_queue', on_message_callback=process_pending_device)
  channel.start_consuming()

if __name__ == '__main__':
  load_dotenv()
  initialize_firebase()
  start_worker()

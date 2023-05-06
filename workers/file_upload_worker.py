import os
import pika
import json
import firebase_admin
from firebase_admin import storage, db, credentials
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

def upload_to_firebase(file_path, destination_path):
  bucket = storage.bucket()
  blob = bucket.blob(destination_path)
  with open(file_path, 'rb') as f:
    blob.upload_from_file(f)
    print(f"File uploaded to {destination_path}")
    os.remove(file_path)

def update_db(task_id):
  task_ref = db.reference('tasks').child(task_id)
  task_ref.update({'status': 'available'})

def send_to_schedule_worker(task_id):
  params = get_conenction_params()
  connection = pika.BlockingConnection(params)
  channel = connection.channel()
  channel.queue_declare(queue='assign_task_queue', durable=True)
  message = json.dumps({'task_id': task_id})
  channel.basic_publish(exchange='', routing_key='assign_task_queue', body=message)
  connection.close()

def process_upload(ch, method, properties, body):
  data = json.loads(body)
  print(f"Recieved upload task with data: {data}")
  file_path = data.get('file_path')
  destination_path = data.get('destination_path')
  task_id = data.get('task_id')
  upload_to_firebase(file_path, destination_path)
  update_db(task_id)
  send_to_schedule_worker(task_id)
  ch.basic_ack(delivery_tag=method.delivery_tag)

def start_worker():
  params = get_conenction_params()
  connection = pika.BlockingConnection(params)
  channel = connection.channel()
  channel.queue_declare(queue='upload_queue', durable=True)
  channel.basic_qos(prefetch_count=1)
  channel.basic_consume(queue='upload_queue', on_message_callback=process_upload)
  channel.start_consuming()

if __name__ == '__main__':
  load_dotenv()
  initialize_firebase()
  start_worker()
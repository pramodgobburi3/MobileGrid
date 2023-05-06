import pika
import json
import os

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

def send_to_file_upload_worker(file_path, destination_path, task_id):
  params = get_conenction_params()
  connection = pika.BlockingConnection(params)
  channel = connection.channel()
  channel.queue_declare(queue='upload_queue', durable=True)
  message = json.dumps({'file_path': file_path, 'destination_path': destination_path, 'task_id': task_id})
  channel.basic_publish(exchange='', routing_key='upload_queue', body=message)
  connection.close()

def send_to_pending_scheduler(device_id):
  params = get_conenction_params()
  connection = pika.BlockingConnection(params)
  channel = connection.channel()
  channel.queue_declare(queue='pending_device_queue', durable=True)
  message = json.dumps({'device_id': device_id})
  channel.basic_publish(exchange='', routing_key='pending_device_queue', body=message)
  connection.close()
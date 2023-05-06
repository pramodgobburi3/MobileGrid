
def success_response(payload=None):
  return {
    "status": 200,
    "payload": payload
  }, 200

def created_response(payload=None):
  return {
    "status": 201,
    "payload": payload
  }, 201

def bad_request_response(payload=None):
  return {
    "status": 400,
    "payload": payload
  }, 400

def unauthorized_response(payload=None):
  return {
    "status": 401,
    "payload": payload
  }, 401

def forbidden_response(payload=None):
  return {
    "status": 403,
    "payload": payload
  }, 403

def not_found_response(payload=None):
  return {
    "status": 404,
    "payload": payload
  }, 404

def server_error_response(payload=None):
  return {
    "status": 500,
    "payload": payload
  }, 500

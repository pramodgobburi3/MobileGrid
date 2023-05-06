
# MobileGrid

A Cloud and Device Approach to Distributed Computing




## Project Structure

###### The project is split into three major components
- Frontend (Android application)
- Backend (Flask application)
- Workers (RabbitMQ workers written in Python)


## Requirements
- Python 3.7 or above
- Pip
- Virtualenv (preferred)
- Android Studio (Electric Eel preferred)
- RabbitMQ server
- Firebase Project
## Installation
Setup a virtual environment, and activate it.


#### Installing requirements for backend and workers

```bash
  pip install -r requirements.txt
```
#### Installing the frontend requiements
Open the frontend directory using Android Studio to trigger automatic dependency installation and indexing.
    
## Environment Variables

To run this project, you will need to add the following environment variables to your .env file at the root level.

`FIREBASE_DATABASE_URL`

`FIREBASE_STORAGE_BUCKET`

`RABBITMQ_USERNAME`

`RABBITMQ_PASSWORD`

`RABBITMQ_HOST`

`RABBITMQ_VIRTUAL_HOST`


## Configuration

Before you can run the project, you must do the following:

#### Setup a firebase project
- Download the service account key and save it inside the `/backend` directory as `serviceAccountKey.json`
- Download the `google-services.json` file from Firebase and add it to the `/frontend/app` directory.
#### Configure the android application to point to the your local flask application.
- First navigate to `java/com.example.mobilenet/api/CompleteTaskRequest` and update the URL on line 29.
- Navigate to `res/xml/network_security_config.xml` and update the domain on line 4.


## Run Locally

Run the backend server

```bash
  cd backend
  python3 server.py
```

Run the workers

```bash
  cd workers
```
```bash
  python3 file_upload_worker.py
```
```bash
  python3 primary_scheduler.py
```
```bash
  python3 pending_scheduler.py
```




## API Reference 

#### Create a new task

```http
  application/json: POST /task
```

| Parameter | Type     | Description                |
| :-------- | :------- | :------------------------- |
| `scriptName` | `string` | **Required**. Name of script file to be executed on device |
| `scriptArgs` | `string` | Arguments for the script |
| `upload` | `string` | **Required**. Name of directory to be uploaded upon task completion |

#### Upload task resources

```http
  multipart/form-data: POST /task/${id}
```

| Parameter | Type     | Description                       |
| :-------- | :------- | :-------------------------------- |
| `file`      | `file` | **Required**. Resource file(s) for a task (zip allowed) |



package com.example.mobilenet.models;

public class Task {
    private String taskId;
    private String scriptName;
    private String scriptArgs;
    private String status;
    private String upload;

    public Task() {}

    public Task(String taskId, String scriptName, String scriptArgs, String status, String upload) {
        this.taskId = taskId;
        this.scriptName = scriptName;
        this.scriptArgs = scriptArgs;
        this.status = status;
        this.upload = upload;
    }


    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public String getScriptArgs() {
        return scriptArgs;
    }

    public void setScriptArgs(String scriptArgs) {
        this.scriptArgs = scriptArgs;
    }

    public String getUpload() {
        return upload;
    }

    public void setUpload(String upload) {
        this.upload = upload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

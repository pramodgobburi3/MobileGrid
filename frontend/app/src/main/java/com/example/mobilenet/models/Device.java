package com.example.mobilenet.models;

import com.google.firebase.database.DatabaseReference;

public class Device {

    private String firebaseIdentifier = "devices";

    private String identifier;
    private String status;
    private String version;
    private long totalMemory;


    public Device() {}

    public Device(String status, String version, long totalMemory) {
        this.status = status;
        this.version = version;
        this.totalMemory = totalMemory;
    }

    public Device(String firebaseIdentifier, String identifier, String status, String version, long totalMemory, String assignedTaskId) {
        this.firebaseIdentifier = firebaseIdentifier;
        this.identifier = identifier;
        this.status = status;
        this.version = version;
        this.totalMemory = totalMemory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public boolean syncToFirebase(DatabaseReference reference) {
        return reference.child(firebaseIdentifier).child(this.identifier)
                .setValue(this).isSuccessful();
    }

}

package com.example.mobilenet;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.example.mobilenet.api.CompleteTaskRequest;
import com.example.mobilenet.models.Device;
import com.example.mobilenet.models.Task;
import com.example.mobilenet.util.DeviceStatus;
import com.example.mobilenet.util.FileUtil;
import com.example.mobilenet.util.FirebaseStorageUtil;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements FirebaseStorageUtil.DownloadCompleteListener,
        FirebaseStorageUtil.UploadCompleteListener, PythonRuntime.PythonRuntimeListener{

    FirebaseDatabase database;
    DatabaseReference deviceReference;
    DatabaseReference taskReference;

    ValueEventListener assignedTaskValueEventListener;

    TextView textView;

    Task currentTask = null;

    Device device = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.outputTxt);
        textView.setMovementMethod(new ScrollingMovementMethod());

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        textView.setText("");
        initializeFirebaseDatabase();
        getDeviceData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (device != null && !device.getStatus().equals(DeviceStatus.ASSIGNED.label)) {
            updateDeviceStatus(DeviceStatus.AVAILABLE.label);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (device != null && !device.getStatus().equals(DeviceStatus.ASSIGNED.label)) {
            updateDeviceStatus(DeviceStatus.UNAVAILABLE.label);
        }
    }

    private void updateDeviceStatus(String status) {
        if (deviceReference != null) {
            deviceReference.child(getDeviceIdentifier()).child("status").setValue(status)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("DEVICE_STATUS_UPDATE", "Device status updated to: " + status);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("DEVICE_STATUS_UPDATE", "Error updating device status: ", e);
                        }
                    });
        }
    }

    private void completeTask() {
        appendToConsole("Updating server...");
        new CompleteTaskRequest(new CompleteTaskRequest.CompleteTaskRequestCallback() {
            @Override
            public void onRequestComplete(int responseCode) {
                currentTask = null;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appendToConsole("Successfully completed task");
                    }
                });
            }

            @Override
            public void onRequestFailed(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        appendToConsole("Unable to complete task");
                    }
                });
            }
        }).execute(getDeviceIdentifier(), currentTask.getTaskId());
    }

    private void setupPythonRuntime() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(() -> {
            handler.post(() -> {
                appendToConsole("Running script...");
                appendToConsole(this.getString(R.string.scriptOutputHeader));
            });
            PythonRuntime py = new PythonRuntime(Python.getInstance(), "executor");

            py.executeWithListener(this,
                    currentTask.getTaskId(),
                    currentTask.getScriptName(),
                    currentTask.getScriptArgs());
        });
    }

    private void fetchTaskResources() {
        appendToConsole("Fetching task resources...");
        FirebaseStorageUtil.downloadZip(this, currentTask, this);
    }

    private void processUpload() {
        appendToConsole("Uploading results...");
        try {
            FirebaseStorageUtil.upload(this, currentTask, this);
        } catch (IOException e) {
            appendToConsole("Unable to upload results");
            throw new RuntimeException(e);
        }
    }

    private void getDeviceData() {
        appendToConsole("Fetching device info...");
        deviceReference.child(getDeviceIdentifier()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the device object
                    device = dataSnapshot.getValue(Device.class);
                    if (device != null) {
                        String data = "Device Version: " + device.getVersion() +
                                "\nDevice Status: " + device.getStatus() +
                                "\nDevice Memory: " + device.getTotalMemory();
                        appendToConsole(data);
                        setupAssignedTaskListener();
                    }
                } else {
                    registerDevice();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                appendToConsole("DATABASE_ERROR: " + databaseError.getMessage());
            }
        });
    }

    private void setupAssignedTaskListener() {
        appendToConsole("Awaiting task...");
        DatabaseReference assignedTaskRef = deviceReference.child(getDeviceIdentifier()).child("assignedTaskId");
        assignedTaskValueEventListener = assignedTaskRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String assignedTaskId = dataSnapshot.getValue(String.class);
                if (assignedTaskId != null && currentTask == null) {
                    getTaskData(assignedTaskId);
                } else {
                    Log.d("ASSIGNED_TASK", "No assigned task");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ASSIGNED_TASK", "Error listening to assignedTaskId changes: ", databaseError.toException());
            }
        });
    }

    private void getTaskData(String taskId) {
        appendToConsole("Fetching task info...");
        taskReference.child(taskId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Task task = snapshot.getValue(Task.class);
                if (task != null) {
                    task.setTaskId(taskId);
                    String data = "Task ID: " + task.getTaskId() +
                            "\nScript Name: " + task.getScriptName() +
                            "\nScript Args: " + task.getScriptArgs() +
                            "\nUpload name: " + task.getUpload();
                    appendToConsole(data);
                    currentTask = task;
                    fetchTaskResources();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                appendToConsole("DATABASE_ERROR: " + error.getMessage());
            }
        });
    }

    private void registerDevice() {
        appendToConsole("Registering device...");
        Device device = new Device();
        device.setStatus(DeviceStatus.AVAILABLE.label);
        device.setVersion(getDeviceVersion());
        device.setTotalMemory(getDeviceMemory());
        deviceReference.child(getDeviceIdentifier()).setValue(device);
        appendToConsole("Successfully registered device!");
        getDeviceData();
    }


    private void initializeFirebaseDatabase() {
        if (database == null) {
            database = FirebaseDatabase.getInstance();
        }
        deviceReference = database.getReference("devices");
        taskReference = database.getReference("tasks");
    }

    private long getDeviceMemory() {
        ActivityManager actManager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);
        return memInfo.totalMem;
    }

    private String getDeviceIdentifier() {
        return Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    private String getDeviceVersion() {
        return Build.VERSION.RELEASE;
    }

    private void appendToConsole(String text) {
        if (textView != null) {
            String prevData = textView.getText().toString();
            if (prevData.isEmpty()) {
                textView.setText(text);
            } else {
                textView.setText(String.format("%s\n%s", prevData, text));
            }
            final Layout layout = textView.getLayout();
            if(layout != null){
                int scrollDelta = layout.getLineBottom(textView.getLineCount() - 1)
                        - textView.getScrollY() - textView.getHeight();
                if(scrollDelta > 0)
                    textView.scrollBy(0, scrollDelta);
            }
        }
    }

    private void removeTaskResources() {
        appendToConsole("Attempting to remove local task resources...");
        File zipFile = new File(this.getFilesDir(), currentTask.getTaskId() + ".zip");
        if (zipFile.exists()) {
            zipFile.delete();
        }
        File taskDir = new File(this.getFilesDir(), currentTask.getTaskId());
        if(FileUtil.deleteRecursive(taskDir)) {
            appendToConsole("Successfully removed local task resources");
        } else {
            appendToConsole("Failed to remove local task resources");
        }
    }

    private void extractResources() {
        appendToConsole("Extracting resources...");
        File zipPath = new File(this.getFilesDir(), currentTask.getTaskId() + ".zip");
        try {
            FileUtil.unzip(zipPath.getPath(), this.getFilesDir().getPath());
            appendToConsole("Successfully extracted resources");
            setupPythonRuntime();
        } catch (IOException e) {
            appendToConsole("Unable to extract resources");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDownloadComplete() {
        appendToConsole("Successfully downloaded task resources");
        extractResources();
    }

    @Override
    public void onDownloadError() {
        appendToConsole("Failed to download task resources");
    }

    @Override
    public void onUploadComplete() {
        appendToConsole("Successfully uploaded task results");
        removeTaskResources();
        completeTask();
    }

    @Override
    public void onUploadError() {
        appendToConsole("Unable to upload task results");
    }

    @Override
    public void onIterate(String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                appendToConsole(value);
            }
        });
    }

    @Override
    public void onFinish() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                appendToConsole("Script finished");
                appendToConsole(MainActivity.super.getString(R.string.separator));
                processUpload();
            }
        });
    }
}

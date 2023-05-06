package com.example.mobilenet.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.example.mobilenet.models.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class FirebaseStorageUtil {

    public static String TAG = FirebaseStorageUtil.class.getSimpleName();

    public static void downloadZip(Context context, Task task, DownloadCompleteListener listener) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        downloadZipHelper(context, storageRef.child(task.getTaskId()+".zip"), context.getFilesDir(), listener);
    }

    public static void downloadDirectory(Context context, Task task, DownloadCompleteListener listener) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        File localDir = new File(context.getFilesDir(), task.getTaskId());
        downloadDirectoryHelper(context, storageRef.child(task.getTaskId()), localDir, listener);
    }

    public static void upload(Context context, Task task, UploadCompleteListener listener) throws IOException {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference uploadRef = storageRef.child("uploads").child(task.getTaskId());
        File taskDir = new File(context.getFilesDir(), task.getTaskId());
        File uploadDir = new File(taskDir, task.getUpload());
        File outputZip = new File(taskDir, task.getUpload() + ".zip");
        FileUtil.zipDirectory(uploadDir, outputZip);
        if (outputZip.exists()) {
            uploadHelper(context, uploadRef, outputZip, listener);
        } else {
            listener.onUploadError();
        }
     }

    public static void uploadHelper(Context context, StorageReference dirRef, File localDir, UploadCompleteListener listener) {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.execute(() -> {
            Uri fUri = Uri.fromFile(localDir);
            dirRef.child(localDir.getName()).putFile(fUri).addOnSuccessListener(taskSnapshot -> {
                listener.onUploadComplete();
                executorService.shutdownNow();
            }).addOnFailureListener(exception -> {
                listener.onUploadError();
                executorService.shutdownNow();
            });
        });
    }

    public static void downloadZipHelper(Context context, StorageReference zipRef, File localDir, DownloadCompleteListener listener) {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.execute(() -> {
           String filename = zipRef.getName();
           File outputFile = new File(localDir, filename);
           zipRef.getFile(outputFile).addOnSuccessListener(taskSnapshot -> {
               listener.onDownloadComplete();
           });
        });
    }

    public static void downloadDirectoryHelper(Context context, StorageReference dirRef, File localDir, DownloadCompleteListener listener) {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        executorService.execute(() -> {
            dirRef.listAll().addOnSuccessListener(listResult -> {
                if (!localDir.exists()) {
                    localDir.mkdirs();
                }

                List<StorageReference> subdirectories = listResult.getPrefixes();

                int numFiles = listResult.getItems().size();
                AtomicInteger completedFilesInDir = new AtomicInteger(0);

                for (StorageReference fileRef : listResult.getItems()) {
                    String filename = fileRef.getName();
                    File outputFile = new File(localDir, filename);
                    fileRef.getFile(outputFile).addOnSuccessListener(taskSnapshot -> {
                        Log.e(TAG, "local temp file created: " + outputFile.toString());
                        int newCompletedFilesInDir = completedFilesInDir.incrementAndGet();
                        if (newCompletedFilesInDir == numFiles) {
                            if (subdirectories.size() == 0) {
                                listener.onDownloadComplete();
                                executorService.shutdownNow();
                            }
                            AtomicInteger completedSubdirectories = new AtomicInteger(0);
                            for (StorageReference directoryRef : subdirectories) {
                                String directoryName = directoryRef.getName();
                                Log.e(TAG, "located directory " + directoryName);
                                File subdirectory = new File(localDir, directoryName);
                                downloadDirectoryHelper(context, directoryRef, subdirectory, new DownloadCompleteListener() {
                                    @Override
                                    public void onDownloadComplete() {
                                        completedSubdirectories.incrementAndGet();
                                        if (completedSubdirectories.get() == subdirectories.size()) {
                                            listener.onDownloadComplete();
                                            executorService.shutdownNow();
                                        }
                                    }

                                    @Override
                                    public void onDownloadError() {
                                        listener.onDownloadError();
                                        executorService.shutdownNow();
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(exception -> {
                        Log.e(TAG, "local temp file not created: " + exception.toString());
                        listener.onDownloadError();
                        executorService.shutdownNow();
                    });
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error listing files in the directory", e);
                listener.onDownloadError();
                executorService.shutdownNow();
            });
        });
    }
    public interface DownloadCompleteListener {
        void onDownloadComplete();

        void onDownloadError();
    }

    public interface UploadCompleteListener {
        void onUploadComplete();
        void onUploadError();
    }
}




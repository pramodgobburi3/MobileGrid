package com.example.mobilenet.util;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtil {

    public static boolean deleteRecursive(java.io.File fileOrDirectory) {
        boolean isDeleted = true;

        if (fileOrDirectory.isDirectory()) {
            for (java.io.File child : fileOrDirectory.listFiles()) {
                if (!deleteRecursive(child)) {
                    isDeleted = false;
                }
            }
        }

        if (!fileOrDirectory.delete()) {
            isDeleted = false;
        }
        return isDeleted;
    }

    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();

        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // Ensure the parent directory exists
                File parentDir = new File(filePath).getParentFile();
                assert parentDir != null;
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }
                extractFile(zipIn, filePath);
            } else {
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    public static void zipDirectory(File sourceDirectory, File outputZipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputZipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            addFileToZip(sourceDirectory, sourceDirectory, zos);
        } catch (IOException e) {
            throw e;
        }
    }

    private static void addFileToZip(File rootDirectory, File sourceFile, ZipOutputStream zos) throws IOException {
        if (sourceFile.isDirectory()) {
            for (File file : sourceFile.listFiles()) {
                addFileToZip(rootDirectory, file, zos);
            }
        } else {
            try (FileInputStream fis = new FileInputStream(sourceFile);
                 BufferedInputStream bis = new BufferedInputStream(fis)) {
                String filePath = rootDirectory.toURI().relativize(sourceFile.toURI()).getPath();
                ZipEntry zipEntry = new ZipEntry(filePath);
                zos.putNextEntry(zipEntry);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    zos.write(buffer, 0, bytesRead);
                }
                zos.closeEntry();
            }
        }
    }


    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(zipIn);
        FileOutputStream fos = new FileOutputStream(filePath);
        byte[] bytesIn = new byte[4096];
        int read = 0;
        while ((read = bis.read(bytesIn)) != -1) {
            fos.write(bytesIn, 0, read);
        }
        fos.close();
    }
}

package com.example.pr_1_file_dupe.service;

import com.example.pr_1_file_dupe.DataStore;
import com.example.pr_1_file_dupe.FileData;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class FileScanner {

    private int skippedCount = 0;
    private int throttleCounter = 0;
    private int totalFilesScanned = 0; 
    
    private Set<Long> seenSizes = new HashSet<>();
    private Set<Long> duplicateSizes = new HashSet<>();

    // 🔥 THE FIX: The Soft Cancel flag requested by your DashboardController
    private volatile boolean isCancelled = false;

    public void stopGracefully() {
        this.isCancelled = true;
    }

    public List<FileData> scanFullSystem(Consumer<String> progressCallback) {
        totalFilesScanned = 0; 
        skippedCount = 0;
        seenSizes.clear();
        duplicateSizes.clear();
        isCancelled = false;
        
        File[] roots = File.listRoots();
        DataStore store = new DataStore();
        boolean skipHidden = store.isSkipHidden();
        long minSizeInBytes = store.getMinFileSizeKB() * 1024;

        System.out.println("🔍 Starting ENTERPRISE TWO-PASS SYSTEM SCAN...");
        
        // ==========================================
        // 🔥 PASS 1: Indexing Sizes
        // ==========================================
        if (progressCallback != null) progressCallback.accept("0:::PASS 1: Indexing file sizes to save memory...");
        
        for (File root : roots) {
            if (isCancelled) break;
            passOneRecursive(root, skipHidden, minSizeInBytes, progressCallback);
        }
        
        seenSizes.clear(); // Free up memory
        
        // If cancelled during Pass 1, we have no paths to show yet.
        if (isCancelled && duplicateSizes.isEmpty()) {
            if (progressCallback != null) progressCallback.accept("0:::Scan cancelled before paths could be collected.");
            return new ArrayList<>();
        }

        // ==========================================
        // 🔥 PASS 2: Collecting Paths
        // ==========================================
        if (progressCallback != null) progressCallback.accept("0:::PASS 2: Collecting matching files...");
        
        List<FileData> potentialDuplicates = new ArrayList<>();
        
        for (File root : roots) {
            if (isCancelled) break; // If cancelled in Pass 2, it will still return what it found!
            passTwoRecursive(root, potentialDuplicates, skipHidden, minSizeInBytes, progressCallback);
        }
        
        if (isCancelled && progressCallback != null) {
            progressCallback.accept("0:::Scan Stopped. Loading partial results...");
        }
        
        duplicateSizes.clear(); 
        return potentialDuplicates;
    }

    public List<FileData> scanDirectory(String path, Consumer<String> progressCallback) {
        totalFilesScanned = 0; 
        skippedCount = 0;
        seenSizes.clear();
        duplicateSizes.clear();
        isCancelled = false;
        
        File root = new File(path);
        if (!root.exists()) return new ArrayList<>();

        DataStore store = new DataStore();
        boolean skipHidden = store.isSkipHidden();
        long minSizeInBytes = store.getMinFileSizeKB() * 1024;

        if (progressCallback != null) progressCallback.accept("0:::PASS 1: Indexing file sizes...");
        passOneRecursive(root, skipHidden, minSizeInBytes, progressCallback);
        
        seenSizes.clear(); 
        
        if (progressCallback != null) progressCallback.accept("0:::PASS 2: Collecting matching files...");
        List<FileData> potentialDuplicates = new ArrayList<>();
        passTwoRecursive(root, potentialDuplicates, skipHidden, minSizeInBytes, progressCallback);
        
        duplicateSizes.clear();
        return potentialDuplicates;
    }

    // ==========================================
    // RECURSIVE LOGIC: PASS 1
    // ==========================================
    private void passOneRecursive(File folder, boolean skipHidden, long minSizeInBytes, Consumer<String> progressCallback) {
        if (isCancelled) return; // 🔥 Graceful stop check
        if (!isAllowedFolder(folder)) return;

        File[] files = folder.listFiles();
        if (files == null) { skippedCount++; return; }

        for (File file : files) {
            if (isCancelled) return;

            try {
                if (skipHidden && file.isHidden()) { skippedCount++; continue; }

                if (file.isDirectory()) {
                    passOneRecursive(file, skipHidden, minSizeInBytes, progressCallback);
                } else {
                    if (!file.canRead()) { skippedCount++; continue; }
                    
                    long size = file.length();
                    if (size < minSizeInBytes) { skippedCount++; continue; }

                    totalFilesScanned++;

                    if (!seenSizes.add(size)) {
                        duplicateSizes.add(size);
                    }

                    throttleCounter++;
                    if (throttleCounter % 60 == 0 && progressCallback != null) {
                        progressCallback.accept(totalFilesScanned + ":::(Pass 1) Indexing: \n" + file.getAbsolutePath());
                    }
                }
            } catch (SecurityException se) { skippedCount++; }
        }
    }

    // ==========================================
    // RECURSIVE LOGIC: PASS 2
    // ==========================================
    private void passTwoRecursive(File folder, List<FileData> fileList, boolean skipHidden, long minSizeInBytes, Consumer<String> progressCallback) {
        if (isCancelled) return; // 🔥 Graceful stop check
        if (!isAllowedFolder(folder)) return;

        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (isCancelled) return;

            try {
                if (skipHidden && file.isHidden()) continue;

                if (file.isDirectory()) {
                    passTwoRecursive(file, fileList, skipHidden, minSizeInBytes, progressCallback);
                } else {
                    if (!file.canRead()) continue;
                    
                    long size = file.length();
                    if (size < minSizeInBytes) continue;

                    if (duplicateSizes.contains(size)) {
                        String name = file.getName();
                        fileList.add(new FileData(name, getFileExtension(name), size, file.getAbsolutePath()));
                    }

                    throttleCounter++;
                    if (throttleCounter % 60 == 0 && progressCallback != null) {
                        progressCallback.accept(totalFilesScanned + ":::(Pass 2) Collecting: " + file.getAbsolutePath());
                    }
                }
            } catch (SecurityException se) {}
        }
    }

    // ==========================================
    // SHARED DEFENSE MECHANISM (Linux Snap & Timeshift Fix)
    // ==========================================
    private boolean isAllowedFolder(File folder) {
        if (java.nio.file.Files.isSymbolicLink(folder.toPath())) {
            skippedCount++; 
            return false;
        }
        
        // 🔥 THE FIX: Aggressive Linux path checking. 
        // This stops it from falling into the infinite /snap and /timeshift loops.
        String absPath = folder.getAbsolutePath().toLowerCase();
        if (absPath.contains("/snap/") || absPath.contains("/run/") || 
            absPath.contains("/proc/") || absPath.contains("/sys/") || 
            absPath.contains("/dev/") || absPath.contains("/timeshift/")) {
            skippedCount++;
            return false;
        }
        
        String name = folder.getName().toLowerCase();
        if (name.equals("windows") || name.equals("system32") || 
            name.equals("$recycle.bin") || name.equals("system volume information") ||
            name.equals("programdata") || name.equals("program files") || 
            name.equals("program files (x86)") || name.equals(".android") || 
            name.equals("appdata") || name.equals("boot") || name.equals("timeshift")) {
            skippedCount++; 
            return false;
        }
        return true;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) return "unknown";
        return fileName.substring(lastDotIndex + 1);
    }
}
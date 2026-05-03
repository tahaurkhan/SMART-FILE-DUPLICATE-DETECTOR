package com.example.pr_1_file_dupe;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class DuplicateFinder {

    // 🔥 NEW: Progress callback support
    public Map<String, List<FileData>> findDuplicates(List<FileData> allFiles) {
        return findDuplicates(allFiles, null);
    }

    public Map<String, List<FileData>> findDuplicates(List<FileData> allFiles, Consumer<String> progressCallback) {
        System.out.println("Starting duplicate analysis...");

        // 1. Load hash cache
        HashDatabase hashDB = new HashDatabase();

        DataStore store = new DataStore();
        String selectedAlgorithm = store.getHashAlgorithm();
        System.out.println("Using Algorithm: " + selectedAlgorithm);

        // Step 1: Group by Size
        if (progressCallback != null) {
            progressCallback.accept("0:::Grouping files by size...");
        }
        
        Map<Long, List<FileData>> sizeMap = new HashMap<>();
        for (FileData file : allFiles) {
            sizeMap.computeIfAbsent(file.getSize(), k -> new ArrayList<>()).add(file);
        }

        // Step 2: Calculate total files to hash
        int totalToHash = 0;
        for (List<FileData> sameSizeFiles : sizeMap.values()) {
            if (sameSizeFiles.size() > 1) {
                totalToHash += sameSizeFiles.size();
            }
        }

        if (progressCallback != null) {
            progressCallback.accept("0:::Found " + totalToHash + " potential duplicates. Computing hashes...");
        }

        // Step 3: Group by Hash with PROGRESS REPORTING
        Map<String, List<FileData>> hashMap = new HashMap<>();
        int processed = 0;
        int hashedCount = 0;
        int cachedCount = 0;
        int throttleCounter = 0;

        for (List<FileData> sameSizeFiles : sizeMap.values()) {
            if (sameSizeFiles.size() > 1) {
                for (FileData file : sameSizeFiles) {
                    try {
                        File f = new File(file.getPath());
                        long lastModified = f.lastModified();

                        String hash = hashDB.getCachedHash(file.getPath(), lastModified);

                        if (hash == null) {
                            hash = HashUtil.getFileChecksum(file.getPath(), selectedAlgorithm);
                            hashDB.putHash(file.getPath(), hash, lastModified);
                            hashedCount++;
                        } else {
                            cachedCount++;
                        }

                        hashMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(file);
                        
                        processed++;
                        throttleCounter++;
                        
                        // 🔥 REPORT PROGRESS EVERY 20 FILES (prevent UI lag)
                        if (progressCallback != null && throttleCounter % 20 == 0) {
                            int percent = (int) ((processed * 100.0) / totalToHash);
                            String shortPath = file.getName();
                            if (file.getPath().length() > 50) {
                                shortPath = "..." + file.getPath().substring(file.getPath().length() - 47);
                            }
                            progressCallback.accept(processed + ":::Hashing (" + percent + "%): " + shortPath);
                        }

                    } catch (Exception e) {
                        System.out.println("Skipped unreadable file: " + file.getName());
                    }
                }
            }
        }

        hashDB.save();
        
        if (progressCallback != null) {
            progressCallback.accept(processed + ":::Finalizing results...");
        }
        
        System.out.println("✅ Hash analysis complete: " + hashedCount + " hashed, " + cachedCount + " from cache");

        // Step 4: Filter duplicates
        return getOnlyDuplicates(hashMap);
    }

    public Map<String, List<FileData>> getOnlyDuplicates(
            Map<String, List<FileData>> allResults) {

        Map<String, List<FileData>> duplicatesOnly = new HashMap<>();

        for (Map.Entry<String, List<FileData>> entry : allResults.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicatesOnly.put(entry.getKey(), entry.getValue());
            }
        }

        return duplicatesOnly;
    }
}
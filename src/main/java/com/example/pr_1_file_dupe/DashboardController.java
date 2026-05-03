package com.example.pr_1_file_dupe;

import com.example.pr_1_file_dupe.service.FileScanner;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

public class DashboardController {

    @FXML private TextField pathInputField;
    @FXML private Button    scanButton;
    @FXML private VBox      loadingBox;
    @FXML private Label     totalSavedLabel;
    @FXML private Label     groupsFoundLabel;
    @FXML private Label     scannedCountLabel;
    
    @FXML private ProgressBar scanProgressBar;
    @FXML private Label       scanDetailsLabel;
    @FXML private Label       scanCountLabel;

    public static Map<String, List<FileData>> lastScanResults;
    private Tooltip pathTooltip;
    
    private Task<Map<String, List<FileData>>> currentScanTask;

    @FXML
    public void initialize() {
        DataStore store = new DataStore();

        String lastFolder = store.getLastFolder();
        if (lastFolder != null && !lastFolder.isEmpty()) {
            pathInputField.setText(lastFolder);
        }

        pathTooltip = new Tooltip();
        pathTooltip.setShowDelay(javafx.util.Duration.seconds(1));
        Tooltip.install(pathInputField, pathTooltip);
        
        pathInputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                pathTooltip.setText(newVal);
            }
        });

        long savedBytes = Long.parseLong(store.getTotalSaved());
        String formattedSize = (savedBytes >= 1024L * 1024 * 1024)
                ? String.format("%.1f GB", savedBytes / (1024.0 * 1024 * 1024))
                : (savedBytes >= 1024 * 1024)
                ? String.format("%.1f MB", savedBytes / (1024.0 * 1024))
                : String.format("%.1f KB", savedBytes / 1024.0);

        totalSavedLabel.setText(formattedSize);
        groupsFoundLabel.setText(store.getTotalGroups());
        scannedCountLabel.setText(store.getTotalScanned());
    }

    @FXML
    public void browseFolder() {
        javafx.stage.DirectoryChooser chooser = new javafx.stage.DirectoryChooser();
        chooser.setTitle("Select Folder to Scan");
        
        DataStore store = new DataStore();
        String lastFolder = store.getLastFolder();
        if (lastFolder != null && !lastFolder.isEmpty()) {
            java.io.File lastDir = new java.io.File(lastFolder);
            if (lastDir.exists() && lastDir.isDirectory()) {
                chooser.setInitialDirectory(lastDir);
            } else {
                chooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
            }
        } else {
            chooser.setInitialDirectory(new java.io.File(System.getProperty("user.home")));
        }

        java.io.File selected = chooser.showDialog(pathInputField.getScene().getWindow());
        if (selected != null) {
            pathInputField.setText(selected.getAbsolutePath());
            store.setLastFolder(selected.getAbsolutePath());
        }
    }

    @FXML
    public void startScan(ActionEvent event) {
        String targetFolder = pathInputField.getText();

        if (targetFolder == null || targetFolder.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Path");
            alert.setHeaderText(null);
            alert.setContentText("Please enter or browse a folder path first.");
            alert.showAndWait();
            return;
        }

        prepareUIForScan(targetFolder);

        System.out.println("🔍 Initializing scanner for: " + targetFolder);

        com.example.pr_1_file_dupe.utils.SoundManager.play(com.example.pr_1_file_dupe.utils.SoundManager.Sound.SCAN_START);
        
        currentScanTask = new Task<>() {
            @Override
            protected Map<String, List<FileData>> call() throws Exception {
                FileScanner scanner = new FileScanner();
                List<FileData> scannedFiles = scanner.scanDirectory(targetFolder, path -> updateMessage(path));
                
                if (isCancelled()) return null;
                
                updateMessage("0:::Analyzing hashes for duplicates...");
                
                // 🔥 FIXED: Pass progress callback to DuplicateFinder!
                return new DuplicateFinder().findDuplicates(scannedFiles, path -> updateMessage(path));
            }
        };

        attachWindowsListener(currentScanTask);

        currentScanTask.setOnSucceeded(e -> handleScanSuccess(currentScanTask));
        currentScanTask.setOnFailed(e -> handleScanFailure(currentScanTask));

        Thread t = new Thread(currentScanTask);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    public void startFullSystemScan(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Full System Scan");
        confirm.setHeaderText("Scan All Drives?");
        confirm.setContentText(
            "This will scan ALL accessible files on your computer.\n\n" +
            "• First scan: 10-30 minutes (builds hash cache)\n" +
            "• Next scans: MUCH faster (uses cached hashes)\n" +
            "• Skips system folders and locked files\n\n" +
            "Continue?"
        );
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        
        prepareUIForScan("FULL SYSTEM");

        System.out.println("🌐 Initializing FULL SYSTEM scan...");
        
        com.example.pr_1_file_dupe.utils.SoundManager.play(com.example.pr_1_file_dupe.utils.SoundManager.Sound.SCAN_START);

        currentScanTask = new Task<>() {
            @Override
            protected Map<String, List<FileData>> call() throws Exception {
                FileScanner scanner = new FileScanner();
                List<FileData> scannedFiles = scanner.scanFullSystem(path -> updateMessage(path));
                
                if (isCancelled()) return null;
                
                updateMessage("0:::Analyzing hashes for duplicates...");
                
                // 🔥 FIXED: Pass progress callback to DuplicateFinder!
                return new DuplicateFinder().findDuplicates(scannedFiles, path -> updateMessage(path));
            }
        };

        attachWindowsListener(currentScanTask);

        currentScanTask.setOnSucceeded(e -> {
            if (currentScanTask.isCancelled() || currentScanTask.getValue() == null) return;
            
            Map<String, List<FileData>> duplicates = currentScanTask.getValue();
            lastScanResults = duplicates;

            int totalFiles = duplicates.values().stream().mapToInt(List::size).sum();
            new DataStore().updateStats(0, duplicates.size(), totalFiles);

            initialize();

            loadingBox.setVisible(false);
            scanButton.setDisable(false);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("System Scan Complete");
            alert.setHeaderText("✅ Full system scan complete!");
            alert.setContentText(
                "Found " + duplicates.size() + " duplicate groups\n" +
                "Total files: " + totalFiles + "\n\n" +
                "💡 Next scan will be MUCH faster!"
            );
            alert.showAndWait();

            try {
                javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) pathInputField.getScene().getRoot();
                javafx.scene.control.Button btnDup = (javafx.scene.control.Button) root.lookup("#btnDuplicates");
                
                if (btnDup != null) {
                    btnDup.fire();
                }
            } catch (Exception ex) {
                System.out.println("Auto-switch failed, but scan data is saved.");
            }
        });

        currentScanTask.setOnFailed(e -> handleScanFailure(currentScanTask));

        Thread t = new Thread(currentScanTask);
        t.setDaemon(true);
        t.start();
    }
    
    @FXML
    public void cancelScan(ActionEvent event) {
        if (currentScanTask != null && currentScanTask.isRunning()) {
            System.out.println("🛑 Scan cancelled by user.");
            currentScanTask.cancel(true);
            
            loadingBox.setVisible(false);
            scanButton.setDisable(false);
            if (scanDetailsLabel != null) scanDetailsLabel.textProperty().unbind();
            
            new Alert(Alert.AlertType.INFORMATION, "Scan was successfully cancelled.").showAndWait();
        }
    }

    private void prepareUIForScan(String folder) {
        if (!folder.equals("FULL SYSTEM")) {
            new DataStore().setLastFolder(folder);
        }
        loadingBox.setVisible(true);
        scanButton.setDisable(true);
        if(scanDetailsLabel != null) {
            scanDetailsLabel.textProperty().unbind(); 
            scanDetailsLabel.setText("Name: Preparing...");
            if (scanCountLabel != null) {
                scanCountLabel.setText("Items scanned: 0");
            }
        }
    }

    private void attachWindowsListener(Task<?> scanTask) {
        scanTask.messageProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.contains(":::")) {
                String[] parts = newVal.split(":::");
                javafx.application.Platform.runLater(() -> {
                    if (scanCountLabel != null) scanCountLabel.setText("Items scanned: " + parts[0]);
                    String path = parts[1];
                    if (path.length() > 65) path = path.substring(0, 15) + "..." + path.substring(path.length() - 45);
                    if (scanDetailsLabel != null) scanDetailsLabel.setText("Name: " + path);
                });
            } else {
                javafx.application.Platform.runLater(() -> {
                    if (scanDetailsLabel != null) scanDetailsLabel.setText(newVal);
                });
            }
        });
    }

    private void handleScanSuccess(Task<Map<String, List<FileData>>> scanTask) {
        if (scanTask.isCancelled() || scanTask.getValue() == null) return;
        
        Map<String, List<FileData>> duplicates = scanTask.getValue();
        lastScanResults = duplicates;

        int totalFiles = duplicates.values().stream().mapToInt(List::size).sum();
        new DataStore().updateStats(0, duplicates.size(), totalFiles);

        initialize();
        loadingBox.setVisible(false);
        scanButton.setDisable(false);
        
        if(scanDetailsLabel != null) {
            scanDetailsLabel.textProperty().unbind();
        }

        com.example.pr_1_file_dupe.utils.SoundManager.play(com.example.pr_1_file_dupe.utils.SoundManager.Sound.SCAN_COMPLETE);

        try {
            javafx.scene.layout.BorderPane root = (javafx.scene.layout.BorderPane) pathInputField.getScene().getRoot();
            javafx.scene.control.Button btnDup = (javafx.scene.control.Button) root.lookup("#btnDuplicates");
            if (btnDup != null) btnDup.fire();
        } catch (Exception ex) {
            System.out.println("Auto-switch failed, but scan data is saved.");
        }
    }   

    private void handleScanFailure(Task<Map<String, List<FileData>>> scanTask) {
        if (scanTask.isCancelled()) return;
        
        loadingBox.setVisible(false);
        scanButton.setDisable(false);
        
        if(scanDetailsLabel != null) {
            scanDetailsLabel.textProperty().unbind();
        }
        
        Throwable ex = scanTask.getException();
        if (ex != null) ex.printStackTrace();
        
        new Alert(Alert.AlertType.ERROR, "Scan failed: " + (ex != null ? ex.getMessage() : "Unknown error")).showAndWait();
    }
}
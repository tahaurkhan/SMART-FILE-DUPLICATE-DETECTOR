package com.example.pr_1_file_dupe;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainController {

    @FXML private BorderPane mainLayout;
    @FXML private VBox       sidebarPane;
    @FXML private Button     hamburgerBtn;
    @FXML private Button     btnFiles;
    @FXML private Button     btnDuplicates;
    @FXML private Button     btnCategories;
    @FXML private Button     btnRecovery;
    @FXML private Button     btnSettings;
    @FXML private StackPane  contentPane;

    private Button  activeButton   = null;
    private boolean sidebarVisible = true;
    private double  currentZoom    = 1.0;
    
    private Map<String, Parent> viewCache = new HashMap<>();

    private Parent dashboardView = null;
    
    // 🔥 NEW IDE-STYLE ZOOM ENGINE
    private ScrollPane masterScrollPane = new ScrollPane();
    private Group zoomGroup = new Group();
    private StackPane centerWrapper = new StackPane();

    @FXML
    public void initialize() {
<<<<<<< HEAD
        DataStore store = new DataStore();
        com.example.pr_1_file_dupe.utils.SoundManager.setSoundEnabled(store.isSoundEnabled());
        com.example.pr_1_file_dupe.utils.SoundManager.setVolume(store.getSoundVolume());

=======
>>>>>>> 056546b (some sound work)
        if (mainLayout.getScene() != null) {
            mainLayout.getScene().getStylesheets().add(getClass().getResource("/com/example/pr_1_file_dupe/CSS/application.css").toExternalForm());
        } else {
            mainLayout.sceneProperty().addListener((obs, oldScene, newScene) -> {
<<<<<<< HEAD
                if (newScene != null) {
                    newScene.getStylesheets().clear();
                    newScene.getStylesheets().add(getClass().getResource("/com/example/pr_1_file_dupe/CSS/application.css").toExternalForm());
                }
=======
                if (newScene != null) ThemeManager.apply(newScene);
>>>>>>> 056546b (some sound work)
            });
        }

        // 🔥 ASSEMBLE THE MASTER SCROLL WRAPPER
        centerWrapper.getChildren().add(zoomGroup);
        masterScrollPane.setContent(centerWrapper);
        masterScrollPane.setFitToWidth(true);
        masterScrollPane.setFitToHeight(true);
        masterScrollPane.setStyle("-fx-background: #f8fcfd; -fx-background-color: transparent; -fx-border-color: transparent;");

        showFiles(null);
    }

    // 🔥 PLACES ALL SCREENS SAFELY INSIDE THE ZOOM ENGINE
    private void setMainContent(Parent view) {
        zoomGroup.getChildren().setAll(view);
        if (mainLayout.getCenter() != masterScrollPane) {
            mainLayout.setCenter(masterScrollPane);
        }
    }

    @FXML 
    public void showFiles(ActionEvent e) {
        setActive(btnFiles);
        com.example.pr_1_file_dupe.utils.SoundManager.play(com.example.pr_1_file_dupe.utils.SoundManager.Sound.NAVIGATION);
        
        try {
            if (dashboardView == null) {
                dashboardView = new FXMLLoader(getClass().getResource("/com/example/pr_1_file_dupe/fxml/dashboard.fxml")).load();
            }
            setMainContent(dashboardView);
            applyZoom(); // Re-apply zoom immediately when switching back
        } catch (IOException ex) {
            ex.printStackTrace();
            showError("Error loading Dashboard: " + ex.getMessage());
        }
    }

    @FXML 
    public void showDuplicates(ActionEvent e) {
        setActive(btnDuplicates);
<<<<<<< HEAD
        java.net.URL url = getClass().getResource("/com/example/pr_1_file_dupe/fxml/duplicate.fxml");
        if (url == null) { showError("dupelicate.fxml not found."); return; }
        try { 
            Parent view = new FXMLLoader(url).load();
            setMainContent(view);
            applyZoom();
        } 
        catch (Exception ex) { ex.printStackTrace(); showError("Error loading Duplicates: " + ex.getMessage()); }
=======
        loadScreen("/com/example/pr_1_file_dupe/fxml/dupelicate.fxml");
>>>>>>> 056546b (some sound work)
    }

    @FXML 
    public void showCategories(ActionEvent e) {
        if (DashboardController.lastScanResults == null || DashboardController.lastScanResults.isEmpty()) {
            showError("Please run a scan from the Dashboard first.");
            return; 
        }
<<<<<<< HEAD
=======
        loadScreen("/com/example/pr_1_file_dupe/fxml/categories.fxml");
    }
>>>>>>> 056546b (some sound work)

        setActive(btnCategories);
        try {
            Parent view = new FXMLLoader(getClass().getResource("/com/example/pr_1_file_dupe/fxml/categories.fxml")).load();
            setMainContent(view);
            applyZoom();
        } catch (Exception ex) {
            ex.printStackTrace();
            showError("Could not load the Categories screen.");
        }
    }
    
    @FXML 
    public void showRecovery(ActionEvent e) {
        setActive(btnRecovery);
        loadScreen("/com/example/pr_1_file_dupe/fxml/recovery.fxml");
    }

    @FXML 
    public void openSetting(ActionEvent e) {
        setActive(btnSettings);
        loadScreen("/com/example/pr_1_file_dupe/fxml/settings.fxml");
    }

    private void loadScreen(String fxmlPath) {
        try {
<<<<<<< HEAD
            java.net.URL settingsUrl = getClass().getResource("/com/example/pr_1_file_dupe/fxml/settings.fxml");
            if (settingsUrl == null) { showError("settings.fxml not found."); return; }
            Parent view = new FXMLLoader(settingsUrl).load();
            setMainContent(view);
            applyZoom();
        } catch (IOException ex) { ex.printStackTrace(); showError("Error loading Settings: " + ex.getMessage()); }
=======
            if (!viewCache.containsKey(fxmlPath)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                Parent screen = loader.load();
                
                if (fxmlPath.contains("categories")) {
                    CategoriesController controller = loader.getController();
                    controller.generateChart(DashboardController.lastScanResults);
                }
                
                viewCache.put(fxmlPath, screen);
            }
            
            Parent activeScreen = viewCache.get(fxmlPath);
            activeScreen.setScaleX(currentZoom);
            activeScreen.setScaleY(currentZoom);
            mainLayout.setCenter(activeScreen);
            
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("CRITICAL ERROR LOADING: " + fxmlPath);
        }
>>>>>>> 056546b (some sound work)
    }

    @FXML
    public void toggleSidebar() {
        double targetWidth = sidebarVisible ? 45 : 170;
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(200),
                        new KeyValue(sidebarPane.prefWidthProperty(), targetWidth),
                        new KeyValue(sidebarPane.minWidthProperty(), targetWidth),
                        new KeyValue(sidebarPane.maxWidthProperty(), targetWidth)
                )
        );
        timeline.setOnFinished(ev -> {
            boolean nowVisible = targetWidth > 45;
            // Extra safety checks for the animation
            if(btnFiles != null) btnFiles.setText(nowVisible ? "🗂  Files" : "🗂");
            if(btnDuplicates != null) btnDuplicates.setText(nowVisible ? "⧉   Duplicates" : "⧉");
            if(btnCategories != null) btnCategories.setText(nowVisible ? "📊  Categories" : "📊");
            if(btnRecovery != null) btnRecovery.setText(nowVisible ? "♻  Recovery" : "♻");
            if(btnSettings != null) btnSettings.setText(nowVisible ? "⚙  Settings" : "⚙");
        });
        sidebarVisible = !sidebarVisible;
        timeline.play();
    }

    // 🔥 ADDED: The missing menu methods causing the LoadException!
    @FXML public void menuOpenFolder() { showFiles(null); }
    @FXML public void menuNewScan() { showFiles(null); }
    @FXML public void menuSelectAll() { System.out.println("Select All clicked"); }
    @FXML public void menuDeselectAll() { System.out.println("Deselect All clicked"); }
    @FXML public void menuDeleteSelected() { System.out.println("Delete Selected clicked"); }

    @FXML public void menuQuit() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Quit");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to quit?");
        confirm.showAndWait().ifPresent(btn -> { if (btn == javafx.scene.control.ButtonType.OK) javafx.application.Platform.exit(); });
    }

    @FXML public void menuZoomIn() { currentZoom = Math.min(currentZoom + 0.1, 2.0); applyZoom(); }
    @FXML public void menuZoomOut() { currentZoom = Math.max(currentZoom - 0.1, 0.6); applyZoom(); }
    @FXML public void menuZoomReset() { currentZoom = 1.0; applyZoom(); }

    // 🔥 APPLIES ZOOM TO THE INNER GROUP (Triggers the scrollbars perfectly)
    private void applyZoom() {
        if (!zoomGroup.getChildren().isEmpty()) {
            javafx.scene.Node view = zoomGroup.getChildren().get(0);
            view.setScaleX(currentZoom);
            view.setScaleY(currentZoom);
        }
    }

    @FXML public void menuAbout() {
        try {
            java.net.URL aboutUrl = getClass().getResource("/com/example/pr_1_file_dupe/fxml/about.fxml");
            if (aboutUrl != null) {
<<<<<<< HEAD
                Parent view = new FXMLLoader(aboutUrl).load();
                setMainContent(view);
                applyZoom();
            } else {
                Alert about = new Alert(Alert.AlertType.INFORMATION);
                about.setTitle("About");
                about.setHeaderText("Duplicate File Detector  v1.0");
                about.setContentText("A smart tool to find and remove duplicate files.");
                about.showAndWait();
=======
                mainLayout.setCenter(new FXMLLoader(aboutUrl).load());
>>>>>>> 056546b (some sound work)
            }
        } catch (IOException ex) { showError("Error loading About screen."); }
    }

    @FXML
    public void menuReportBug() {
        try {
            String mailto = "mailto:x.tahaur@gmail.com,guptapraveen67984@gmail.com?subject=Bug%20Report";
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("cmd /c start " + mailto);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + mailto);
            } else {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString("x.tahaur@gmail.com, guptapraveen67984@gmail.com");
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                showError("Direct mail opening is restricted on Linux. We copied the support email to your clipboard!");
            }
        } catch (Exception e) {
            showError("Could not open email client. Please email us at:\nx.tahaur@gmail.com");
        }
    }

    private void setActive(Button clicked) {
        // 🔥 ADDED: The null check to prevent silent startup crashes
        if (clicked == null) return;
        
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-item-active");
            if (!activeButton.getStyleClass().contains("nav-item")) activeButton.getStyleClass().add("nav-item");
        }
        clicked.getStyleClass().remove("nav-item");
        if (!clicked.getStyleClass().contains("nav-item-active")) clicked.getStyleClass().add("nav-item-active");
        activeButton = clicked;
    }

<<<<<<< HEAD
    private void loadScreen(String fxmlPath) {
    	com.example.pr_1_file_dupe.utils.SoundManager.play(com.example.pr_1_file_dupe.utils.SoundManager.Sound.NAVIGATION);
        
        try { 
            Parent view = new FXMLLoader(getClass().getResource(fxmlPath)).load();
            setMainContent(view);
            applyZoom();
        } 
        catch (IOException e) { showError("Error loading screen: " + fxmlPath); }
    }

=======
>>>>>>> 056546b (some sound work)
    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Notice");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
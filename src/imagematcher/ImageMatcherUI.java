package imagematcher;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;

public class ImageMatcherUI {

    private TextField imageTextField;
    private TextField directoryTextField;
    private Slider distanceSlider;
    private ListView<ResultItem> resultList;
    private String lastOpenedFolderPath;

    public void start(Stage primaryStage) {
        primaryStage.setTitle("PicPenguin");
        primaryStage.getIcons().add(new Image("imagematcher/logo.png"));

        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: linear-gradient(to bottom, #185C6A, #dadada);");

        GridPane gridPane = createGridPane();
        borderPane.setTop(gridPane);

        resultList = new ListView<>();
        resultList.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ResultItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(item.getImageView());
                    setText(item.getImagePath() + " (Distance: " + item.getDistance() + ")");
                }
            }
        });

        borderPane.setCenter(resultList);

        Button matchButton = new Button("Find image");
        Button cancelButton = new Button("Cancel");;
        ProgressBar progressBar = new ProgressBar();

        cancelButton.getStyleClass().add("action-button");
        cancelButton.setDisable(true);
        cancelButton.setVisible(false);

        progressBar.setPrefWidth(200);
        progressBar.setDisable(true);
        progressBar.setVisible(false);

        matchButton.getStyleClass().add("action-button");
        matchButton.setStyle("-fx-background-color: #4caf50; -fx-text-fill: black;");
        matchButton.setOnAction(e -> {
            matchButton.setDisable(true);
            cancelButton.setDisable(false);
            cancelButton.setVisible(true);
            progressBar.setDisable(false);
            progressBar.setVisible(true);

            String imagePath = imageTextField.getText();
            String directoryPath = directoryTextField.getText();

            // Run the image matching operation in a separate thread
            Thread matchingThread = new Thread(() -> {
                // Show the progress indicator while finding images
                Platform.runLater(() -> progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));

                ImageUtils.matchImages(imagePath, directoryPath, distanceSlider.getValue(), resultList, () -> {
                    Platform.runLater(() -> {
                        matchButton.setDisable(false);
                        cancelButton.setDisable(true);
                        cancelButton.setVisible(false);
                        progressBar.setDisable(true);
                        progressBar.setVisible(false);
                    });
                });
            });
            matchingThread.start();
        });

        cancelButton.setOnAction(e -> {
            matchButton.setDisable(false);
            cancelButton.setDisable(true);
            cancelButton.setVisible(false);
            progressBar.setDisable(true);
            progressBar.setVisible(false);
            ImageUtils.cancelMatching();
        });

        HBox buttonContainer = new HBox(10, matchButton, cancelButton, progressBar);
        buttonContainer.setPadding(new Insets(10));

        borderPane.setBottom(buttonContainer);
        BorderPane.setMargin(matchButton, new Insets(10));

        Scene scene = new Scene(borderPane, 800, 400);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane createGridPane() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(10));

        Label imageLabel = new Label("Image Path:");
        imageLabel.setStyle("-fx-font-weight: bold;");
        imageTextField = new TextField();
        imageTextField.setStyle("-fx-pref-width: 300;");
        Button imageBrowseButton = new Button("Browse");
        imageBrowseButton.setStyle("-fx-background-color: #2196f3; -fx-text-fill: black;");
        imageBrowseButton.setOnAction(e -> browseImage());

        Label directoryLabel = new Label("Directory Path:");
        directoryLabel.setStyle("-fx-font-weight: bold;");
        directoryTextField = new TextField();
        directoryTextField.setStyle("-fx-pref-width: 300;");
        Button directoryBrowseButton = new Button("Browse");
        directoryBrowseButton.setStyle("-fx-background-color: #2196f3; -fx-text-fill: black;");
        directoryBrowseButton.setOnAction(e -> browseDirectory());

        gridPane.add(imageLabel, 0, 0);
        gridPane.add(imageTextField, 1, 0);
        gridPane.add(imageBrowseButton, 2, 0);
        gridPane.add(directoryLabel, 0, 1);
        gridPane.add(directoryTextField, 1, 1);
        gridPane.add(directoryBrowseButton, 2, 1);

        createDistanceSlider(gridPane);

        return gridPane;
    }

    private void browseImage() {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Image files", "*.jpg", "*.jpeg", "*.png", "*.bmp");
        fileChooser.getExtensionFilters().add(extFilter);

        // Set the last opened folder path as the initial directory
        if (lastOpenedFolderPath != null && !lastOpenedFolderPath.isEmpty()) {
            File lastOpenedFolder = new File(lastOpenedFolderPath);
            if (lastOpenedFolder.exists() && lastOpenedFolder.isDirectory()) {
                fileChooser.setInitialDirectory(lastOpenedFolder);
            }
        }

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            imageTextField.setText(file.getAbsolutePath());

            // Store the path of the selected file's parent directory as the last opened folder path
            File parentDirectory = file.getParentFile();
            if (parentDirectory != null) {
                lastOpenedFolderPath = parentDirectory.getAbsolutePath();
            }
        }
    }

    private void browseDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();

        // Set the last opened folder path as the initial directory
        if (lastOpenedFolderPath != null && !lastOpenedFolderPath.isEmpty()) {
            File lastOpenedFolder = new File(lastOpenedFolderPath);
            if (lastOpenedFolder.exists() && lastOpenedFolder.isDirectory()) {
                directoryChooser.setInitialDirectory(lastOpenedFolder);
            }
        }

        File directory = directoryChooser.showDialog(null);
        if (directory != null) {
            directoryTextField.setText(directory.getAbsolutePath());

            // Store the selected directory path as the last opened folder path
            lastOpenedFolderPath = directory.getAbsolutePath();
        }
    }

    private void createDistanceSlider(GridPane gridPane) {
        Label distanceLabel = new Label("Distance Threshold:");
        distanceLabel.setStyle("-fx-font-weight: bold;");
        distanceSlider = new Slider(0, 100, 50);
        distanceSlider.setBlockIncrement(1);
        distanceSlider.setShowTickLabels(true);
        distanceSlider.setShowTickMarks(true);
        distanceSlider.setMajorTickUnit(10);
        distanceSlider.setMinorTickCount(1);
        distanceSlider.setSnapToTicks(true);
        gridPane.add(distanceLabel, 0, 2, 2, 1);
        gridPane.add(distanceSlider, 0, 3, 2, 1);
    }
}
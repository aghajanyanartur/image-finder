// ImageUtils.java
package imagematcher;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.*;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.SIFT;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ImageUtils {
    private static final int RESIZE_WIDTH = 400;
    private static final int RESIZE_HEIGHT = 300;
    private static boolean isCancelled = false;

    public static void matchImages(String imagePath, String directoryPath, double distanceThreshold, ListView<ResultItem> resultList,
                                   Runnable onCompletion) {
        Platform.runLater(() -> resultList.getItems().clear()); // Clear previous results

        Mat queryImage = Imgcodecs.imread(imagePath);
        Mat resizedQueryImage = resizeImage(queryImage);
        SIFT sift = SIFT.create();
        DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);
        MatOfKeyPoint queryKeypoints = new MatOfKeyPoint();
        Mat queryDescriptors = new Mat();
        detectAndComputeFeatures(sift, resizedQueryImage, queryKeypoints, queryDescriptors);

        File dir = new File(directoryPath);
        List<File> imageFiles = searchImageFiles(dir);

        if (!imageFiles.isEmpty()) {
            ConcurrentLinkedQueue<ResultItem> resultListItems = performImageMatching(imageFiles, imagePath, distanceThreshold, sift, descriptorMatcher, queryDescriptors);

            if (!resultListItems.isEmpty()) {
                showMatchingResults(resultList, resultListItems);
            } else {
                showNoSimilarImageFound();
            }
        } else {
            showNoImageFilesFound();
        }

        Platform.runLater(onCompletion);
    }

    private static Mat resizeImage(Mat image) {
        Mat resizedImage = new Mat();
        Imgproc.resize(image, resizedImage, new Size(RESIZE_WIDTH, RESIZE_HEIGHT));
        return resizedImage;
    }

    private static void detectAndComputeFeatures(SIFT sift, Mat image, MatOfKeyPoint keypoints, Mat descriptors) {
        sift.detectAndCompute(image, new Mat(), keypoints, descriptors);
    }

    private static ConcurrentLinkedQueue<ResultItem> performImageMatching(List<File> imageFiles, String imagePath, double distanceThreshold,
                                                                          SIFT sift, DescriptorMatcher descriptorMatcher, Mat queryDescriptors) {
        ConcurrentLinkedQueue<ResultItem> resultListItems = new ConcurrentLinkedQueue<>();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        int batchSize = 10;
        int totalBatches = (imageFiles.size() + batchSize - 1) / batchSize;

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            if (isCancelled) {
                isCancelled = false;
                break;
            }

            int startIndex = batchIndex * batchSize;
            int endIndex = Math.min(startIndex + batchSize, imageFiles.size());

            executor.execute(() -> {
                for (int i = startIndex; i < endIndex; i++) {
                    if (isCancelled) {
                        isCancelled = false;
                        break;
                    }
                    File file = imageFiles.get(i);
                    if (file.getAbsolutePath().equals(imagePath)) {
                        continue;
                    }
                    Mat trainImage = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);

                    if (!trainImage.empty()) {
                        Mat resizedTrainImage = resizeImage(trainImage);
                        MatOfKeyPoint trainKeypoints = new MatOfKeyPoint();
                        Mat trainDescriptors = new Mat();
                        detectAndComputeFeatures(sift, resizedTrainImage, trainKeypoints, trainDescriptors);
                        MatOfDMatch matches = new MatOfDMatch();
                        descriptorMatcher.match(queryDescriptors, trainDescriptors, matches);
                        double distance = calculateDistance(matches);
                        if (distance < distanceThreshold) {
                            ImageView imageView = createResizedImageView(file.getAbsolutePath());
                            ResultItem resultItem = new ResultItem(imageView, file.getAbsolutePath(), distance);
                            resultListItems.add(resultItem);
                        }
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return resultListItems;
    }

    private static ImageView createResizedImageView(String imagePath) {
        ImageView imageView = new ImageView(new Image("file:" + imagePath));
        imageView.setFitWidth(50);
        imageView.setFitHeight(50);
        return imageView;
    }

    public static void cancelMatching() {
        isCancelled = true;
    }

    private static List<File> searchImageFiles(File directory) {
        List<File> imageFiles = new ArrayList<>();
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        imageFiles.addAll(searchImageFiles(file));  // Recursively search in subdirectories
                    } else if (isImageFile(file)) {
                        imageFiles.add(file);
                    }
                }
            }
        }
        return imageFiles;
    }

    private static boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".bmp");
    }

    private static double calculateDistance(MatOfDMatch matches) {
        List<DMatch> matchesList = matches.toList();
        double totalDistance = 0;
        for (DMatch match : matchesList) {
            totalDistance += match.distance;
        }
        return totalDistance / matchesList.size();
    }


    private static void showMatchingResults(ListView<ResultItem> resultList, ConcurrentLinkedQueue<ResultItem> resultListItems) {
        Platform.runLater(() -> {
            ObservableList<ResultItem> observableList = FXCollections.observableArrayList(resultListItems);
            resultList.setItems(observableList);

            // Add a double-click event handler to open the file location
            resultList.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2) {
                    ResultItem selectedResult = resultList.getSelectionModel().getSelectedItem();
                    if (selectedResult != null) {
                        File file = new File(selectedResult.getImagePath());
                        openFileLocation(file);
                    }
                }
            });
        });
    }

    private static void showNoSimilarImageFound() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Similar Image Found");
            alert.setHeaderText(null);
            alert.setContentText("No similar image found within the specified distance threshold.");
            alert.showAndWait();
        });
    }

    private static void showNoImageFilesFound() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Image Files Found");
            alert.setHeaderText(null);
            alert.setContentText("No image files found in the specified directory.");
            alert.showAndWait();
        });
    }

    public static void openFileLocation(File file) {
        if (Desktop.isDesktopSupported()) {
            try {
                String osName = System.getProperty("os.name").toLowerCase();

                if (osName.contains("win")) { // Windows
                    // Select the file in the opened folder using the "explorer" command
                    Runtime.getRuntime().exec("explorer /select,\"" + file.getAbsolutePath() + "\"");
                } else if (osName.contains("mac")) { // macOS
                    // Select the file in the opened folder using the "open" command
                    Runtime.getRuntime().exec("open -R \"" + file.getAbsolutePath() + "\"");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
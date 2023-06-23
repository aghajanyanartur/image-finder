package imagematcher;

import javafx.application.Application;
import javafx.stage.Stage;
import org.opencv.core.Core;

public class ImageMatcherApp extends Application {
    public static void main(String[] args) {
        System.load("C:\\Users\\Lenovo\\Downloads\\opencv\\opencv\\build\\java\\x64\\opencv_java453.dll");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        ImageMatcherUI imageMatcherUI = new ImageMatcherUI();
        imageMatcherUI.start(primaryStage);
    }
}
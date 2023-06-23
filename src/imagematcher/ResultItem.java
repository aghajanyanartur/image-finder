package imagematcher;

import javafx.scene.image.ImageView;

public class ResultItem {
    private ImageView imageView;
    private String filePath;
    private double distance;

    public ResultItem(ImageView imageView, String filePath, double distance) {
        this.imageView = imageView;
        this.filePath = filePath;
        this.distance = distance;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public String getImagePath() {
        return filePath;
    }

    public double getDistance() {
        return distance;
    }
}
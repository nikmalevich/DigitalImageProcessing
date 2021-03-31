package sample;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Controller {

    private final FileChooser fileChooser = new FileChooser();

    @FXML
    private Button button;

    @FXML
    private ListView<VBox> listView;

    public void onClick() {
        File file = fileChooser.showOpenDialog(button.getScene().getWindow());
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "invalid file " + file.getName()).show();
            throw new RuntimeException();
        }

        VBox vBox = new VBox();
        vBox.getChildren().add(new Label("исходное изображение"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(image, null)));

        listView.getItems().add(vBox);

        int threshold = getGlobalThresholdingHistogramT(image);

        BufferedImage result = applyGlobalBinarizationLowerThreshold(image, threshold);

        vBox = new VBox();
        vBox.getChildren().add(new Label("глобальная бинаризация с нижним порогом"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);

        result = applyGlobalBinarizationUpperThreshold(image, threshold);

        vBox = new VBox();
        vBox.getChildren().add(new Label("глобальная бинаризация с верхним порогом"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);

        result = applyAdaptiveThresholding(image);

        vBox = new VBox();
        vBox.getChildren().add(new Label("адаптивная пороговая обработка"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);

        int[][] mask = new int[][]{{-1, -1, -1}, {-1, 8, -1}, {-1, -1, -1}};
        result = applyLineOrDotImageSegmentation(image, mask);

        vBox = new VBox();
        vBox.getChildren().add(new Label("обнаружение точек"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);

        mask = new int[][]{{-1, -1, -1}, {2, 2, 2}, {-1, -1, -1}};
        result = applyLineOrDotImageSegmentation(image, mask);

        vBox = new VBox();
        vBox.getChildren().add(new Label("обнаружение вертикальных линий"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);

        mask = new int[][]{{-1, 2, -1}, {-1, 2, -1}, {-1, 2, -1}};
        result = applyLineOrDotImageSegmentation(image, mask);

        vBox = new VBox();
        vBox.getChildren().add(new Label("обнаружение горизонтальных линий"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);

        mask = new int[][]{{-1, -1, 2}, {-1, 2, -1}, {2, -1, -1}};
        result = applyLineOrDotImageSegmentation(image, mask);

        vBox = new VBox();
        vBox.getChildren().add(new Label("обнаружение диагональных +45 линий"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);

        mask = new int[][]{{2, -1, -1}, {-1, 2, -1}, {-1, -1, 2}};
        result = applyLineOrDotImageSegmentation(image, mask);

        vBox = new VBox();
        vBox.getChildren().add(new Label("обнаружение диагональных -45 линий"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);

        result = applyBorderImageSegmentation(image);

        vBox = new VBox();
        vBox.getChildren().add(new Label("обнаружение границ"));
        vBox.getChildren().add(new ImageView(SwingFXUtils.toFXImage(result, null)));
        listView.getItems().add(vBox);
    }

    private int getGlobalThresholdingHistogramT(BufferedImage image) {
        int t = 100;
        int e = 1;
        int prevT = 0;

        while (Math.abs(t - prevT) > e) {
            int sumGroup1 = 0;
            int numGroup1 = 0;
            int sumGroup2 = 0;
            int numGroup2 = 0;

            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    Color color = new Color(image.getRGB(x, y));
                    int brightness = (int) (100 * Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2]);

                    if (brightness > t) {
                        sumGroup1 += brightness;
                        numGroup1++;
                    } else {
                        sumGroup2 += brightness;
                        numGroup2++;
                    }
                }
            }

            double averageGroup1 = 0;
            double averageGroup2 = 0;

            if (numGroup1 != 0) {
                averageGroup1 = 1. * sumGroup1 / numGroup1;
            }

            if (numGroup2 != 0) {
                averageGroup2 = 1. * sumGroup2 / numGroup2;
            }

            prevT = t;
            t = (int) ((averageGroup1 + averageGroup2) / 2);
        }

        return t;
    }

    private BufferedImage applyGlobalBinarizationLowerThreshold(BufferedImage image, int threshold) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color color = new Color(image.getRGB(x, y));
                int brightness = (int) (100 * Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2]);

                if (brightness > threshold) {
                    result.setRGB(x, y, Color.white.getRGB());
                } else {
                    result.setRGB(x, y, Color.black.getRGB());
                }
            }
        }

        return result;
    }

    private BufferedImage applyGlobalBinarizationUpperThreshold(BufferedImage image, int threshold) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                Color color = new Color(image.getRGB(x, y));
                int brightness = (int) (100 * Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2]);

                if (brightness >= threshold) {
                    result.setRGB(x, y, Color.black.getRGB());
                } else {
                    result.setRGB(x, y, Color.white.getRGB());
                }
            }
        }

        return result;
    }

    private double[] getAdaptiveThresholdingParameters(BufferedImage image, int centerX, int centerY) {
        int k = 1;

        while (true) {
            double a = 2. / 3;
            int max = 0;
            int min = Integer.MAX_VALUE;
            int sum = 0;

            for (int x = centerX - k; x <= centerX + k; x++) {
                for (int y = centerY - k; y <= centerY + k; y++) {
                    int brightness = 0;

                    if (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight()) {
                        Color color = new Color(image.getRGB(x, y));
                        brightness = (int) (100 * Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2]);
                    }

                    max = Math.max(max, brightness);
                    min = Math.min(min, brightness);
                    sum += brightness;
                }
            }

            double p = 1. / (2 * k + 1) / (2 * k + 1) * sum;
            double dMax = Math.abs(max - p);
            double dMin = Math.abs(min - p);

            if (dMax > dMin) {
                return new double[]{a * (2. / 3 * min + 1. / 3 * p), k, p};
            } else if (dMax < dMin) {
                return new double[]{a * (1. / 3 * min + 2. / 3 * p), k, p};
            } else if (dMax == dMin && max == min) {
                return new double[]{a * p, k, p};
            } else {
                k++;
            }
        }
    }

    private BufferedImage applyAdaptiveThresholding(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());

        ArrayList<ArrayList<double[]>> parameters = new ArrayList<>();

        for (int x = 0; x < image.getWidth(); x++) {
            parameters.add(new ArrayList<>());

            for (int y = 0; y < image.getHeight(); y++) {
                double[] pixelParameters = getAdaptiveThresholdingParameters(image, x, y);

                parameters.get(x).add(pixelParameters);
            }
        }

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int t = (int) parameters.get(x).get(y)[0];
                int k = (int) parameters.get(x).get(y)[1];
                Color color = new Color(image.getRGB(x, y));
                int brightness = (int) (100 * Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2]);

                double maxDifference = 0;

                for (int windowX = x - k; windowX <= x + k; windowX++) {
                    for (int windowY = y - k; windowY <= y + k; windowY++) {
                        if (windowX >= 0 && windowX < image.getWidth() && windowY >= 0 && windowY < image.getHeight()) {
                            maxDifference = Math.max(maxDifference, Math.abs(brightness - parameters.get(windowX).get(windowY)[2]));
                        }
                    }
                }

                if (maxDifference > t) {
                    result.setRGB(x, y, Color.white.getRGB());
                } else {
                    result.setRGB(x, y, Color.black.getRGB());
                }
            }
        }

        return result;
    }

    private BufferedImage applyLineOrDotImageSegmentation(BufferedImage image, int[][] mask) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        int t = 100;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int response = 0;

                for (int maskX = x - 1; maskX <= x + 1; maskX++) {
                    for (int maskY = y - 1; maskY <= y + 1; maskY++) {
                        if (maskX >= 0 && maskX < image.getWidth() && maskY >= 0 && maskY < image.getHeight()) {
                            Color color = new Color(image.getRGB(maskX, maskY));
                            int brightness = (int) (100 * Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2]);

                            response += brightness * mask[maskX - x + 1][maskY - y + 1];
                        }
                    }
                }

                if (Math.abs(response) > t) {
                    result.setRGB(x, y, Color.white.getRGB());
                } else {
                    result.setRGB(x, y, Color.black.getRGB());
                }
            }
        }

        return result;
    }

    private BufferedImage applyBorderImageSegmentation(BufferedImage image) {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        int[][] gX = new int[][]{{-1, -2, -1}, {0, 0, 0}, {1, 2, 1}};
        int[][] gY = new int[][]{{-1, 0, 1}, {-2, 0, 2}, {1, 2, 1}};

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int[] responseX = new int[3];
                int[] responseY = new int[3];

                for (int maskX = x - 1; maskX <= x + 1; maskX++) {
                    for (int maskY = y - 1; maskY <= y + 1; maskY++) {
                        if (maskX >= 0 && maskX < image.getWidth() && maskY >= 0 && maskY < image.getHeight()) {
                            Color color = new Color(image.getRGB(maskX, maskY));


                            responseX[0] += color.getRed() * gX[maskX - x + 1][maskY - y + 1];
                            responseX[1] += color.getGreen() * gX[maskX - x + 1][maskY - y + 1];
                            responseX[2] += color.getBlue() * gX[maskX - x + 1][maskY - y + 1];
                            responseY[0] += color.getRed() * gY[maskX - x + 1][maskY - y + 1];
                            responseY[1] += color.getGreen() * gY[maskX - x + 1][maskY - y + 1];
                            responseY[2] += color.getBlue() * gY[maskX - x + 1][maskY - y + 1];
                        }
                    }
                }

                responseX[0] = Math.min(255, Math.abs(responseX[0]));
                responseX[1] = Math.min(255, Math.abs(responseX[1]));
                responseX[2] = Math.min(255, Math.abs(responseX[2]));
                responseY[0] = Math.min(255, Math.abs(responseY[0]));
                responseY[1] = Math.min(255, Math.abs(responseY[1]));
                responseY[2] = Math.min(255, Math.abs(responseY[2]));

                int[] response = new int[]{Math.min(responseX[0] + responseY[0], 255), Math.min(responseX[1] + responseY[1], 255), Math.min(responseX[2] + responseY[2], 255)};

                result.setRGB(x, y, new Color(response[0], response[1], response[2]).getRGB());
            }
        }

        return result;
    }
}

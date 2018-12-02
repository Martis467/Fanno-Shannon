package main.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class MainUI implements Initializable {

    @FXML
    private Slider slider;
    @FXML
    private TextField sliderValue;
    @FXML
    private ProgressBar progressBar;

    private Timeline timer; //Runs in the background

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        timer = new Timeline(new KeyFrame(Duration.seconds(0.02), event ->
            sliderValue.setText(String.valueOf((int)slider.getValue()))
        ));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    public void selectFileForCompression(ActionEvent actionEvent) {
        
    }

    public void compressFile(ActionEvent actionEvent) {

    }

    public void selectFileForDecompression(ActionEvent actionEvent) {

    }
}

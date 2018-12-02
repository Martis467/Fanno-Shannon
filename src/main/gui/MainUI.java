package main.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import main.models.Encode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class MainUI implements Initializable {

    @FXML
    private TextField filePathText;
    @FXML
    private TabPane rootPane;
    @FXML
    private Slider slider;
    @FXML
    private TextField sliderValue;

    private Timeline timer; //Runs in the background
    private URL filepathUrl;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filePathText.setText("");
        timer = new Timeline(new KeyFrame(Duration.seconds(0.02), event ->
            sliderValue.setText(String.valueOf((int)slider.getValue()))
        ));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    public void selectFileForCompression(ActionEvent actionEvent) throws MalformedURLException {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose file");
        File defaultDir = new File(System.getProperty("user.dir"));
        fc.setInitialDirectory(defaultDir);
        File selectedFile = fc.showOpenDialog(rootPane.getScene().getWindow());
        System.out.println(selectedFile.getPath());
        filepathUrl = Paths.get(selectedFile.toURI()).toUri().toURL();
        filePathText.setText(filepathUrl.getPath().substring(1).replace("%20", " "));
    }

    public void compressFile(ActionEvent actionEvent) {

        if(filePathText.getText().isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("Please select a file to compress");
            alert.showAndWait();
            return;
        }

        int wordLength = Integer.parseInt(sliderValue.getText());

        Encode encoder = new Encode(wordLength);
        encoder.encode(filepathUrl);
    }

    public void selectFileForDecompression(ActionEvent actionEvent) {

    }
}

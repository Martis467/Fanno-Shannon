package main.gui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import main.models.Decode;
import main.models.Encode;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ResourceBundle;

public class MainUI implements Initializable {

    @FXML
    private TextField decompressorFilePathText;
    @FXML
    private Text compressorFile;
    @FXML
    private Text compressorEncoded;
    @FXML
    private Text compressorComparison;
    @FXML
    private Text decompressorFile;
    @FXML
    private Text decompressorDecoded;
    @FXML
    private Text decompressorComparison;
    @FXML
    private TextField compressorFilePathText;
    @FXML
    private TabPane rootPane;
    @FXML
    private Slider slider;
    @FXML
    private TextField sliderValue;

    private Timeline timer; //Runs in the background
    private URL compressionFilePathUrl;
    private URL decompressionFilePathUrl;
    DecimalFormat df = new DecimalFormat("#.##");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideFields();
        compressorFilePathText.setText("");

        timer = new Timeline(new KeyFrame(Duration.seconds(0.02), event ->
            sliderValue.setText(String.valueOf((int)slider.getValue()))
        ));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    public void selectFileForCompression(ActionEvent actionEvent) throws MalformedURLException {
        File selectedFile = choosePath();
        compressorFile.setText(String.valueOf(selectedFile.length()));
        compressorFile.setVisible(true);
        compressionFilePathUrl = Paths.get(selectedFile.toURI()).toUri().toURL();
        compressorFilePathText.setText(compressionFilePathUrl.getPath().substring(1).replace("%20", " "));
    }

    public void compressFile(ActionEvent actionEvent) {

        if(compressorFilePathText.getText().isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("Please select a file to compress");
            alert.showAndWait();
            return;
        }

        int wordLength = Integer.parseInt(sliderValue.getText());

        Encode encoder = new Encode(wordLength);
        encoder.encode(compressionFilePathUrl);
        long encodedFileSize = encoder.getFileSize();
        compressorEncoded.setText(String.valueOf(encodedFileSize));
        compressorEncoded.setVisible(true);

        //Compare compressed and decompressed file
        long fileSize = Long.parseLong(compressorFile.getText());
        compressorComparison.setText(String.valueOf(df.format(fileSize/encodedFileSize)));
        compressorComparison.setVisible(true);

    }

    public void selectFileForDecompression(ActionEvent actionEvent) throws MalformedURLException {
        File selectedFile = choosePath();
        decompressorFile.setVisible(true);
        decompressorFile.setText(String.valueOf(selectedFile.length()));
        decompressionFilePathUrl = Paths.get(selectedFile.toURI()).toUri().toURL();
        decompressorFilePathText.setText(decompressionFilePathUrl.getPath().substring(1).replace("%20", " "));
    }

    private File choosePath()
    {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose file");
        File defaultDir = new File(System.getProperty("user.dir"));
        fc.setInitialDirectory(defaultDir);
        File selectedFile = fc.showOpenDialog(rootPane.getScene().getWindow());
        System.out.println(selectedFile.getPath());
        return selectedFile;
    }

    public void decompressFile(ActionEvent actionEvent) {
        if(decompressorFilePathText.getText().isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setContentText("Please select a file to decompress");
            alert.showAndWait();
            return;
        }

        Decode decoder = new Decode();
        decoder.decode(decompressionFilePathUrl);

        long decodedFileSize = decoder.getFileSize();
        decompressorDecoded.setText(String.valueOf(decodedFileSize));
        decompressorDecoded.setVisible(true);

        //Compare decompressed and compressed file
        long fileSize = Long.parseLong(decompressorFile.getText());
        decompressorComparison.setText(String.valueOf(df.format(fileSize/decodedFileSize)));
        decompressorComparison.setVisible(true);
    }

    private void hideFields() {
        compressorFile.setVisible(false);
        compressorEncoded.setVisible(false);
        compressorComparison.setVisible(false);

        decompressorFile.setVisible(false);
        decompressorDecoded.setVisible(false);
        decompressorComparison.setVisible(false);
    }
}

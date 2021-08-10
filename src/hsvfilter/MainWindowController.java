/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hsvFilter;

import generated.Filter;
import generated.Parameters;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point3D;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.converter.NumberStringConverter;
import javax.xml.bind.JAXB;
import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import static org.opencv.imgcodecs.Imgcodecs.imread;
import static org.opencv.imgcodecs.Imgcodecs.imwrite;
import org.opencv.imgproc.Imgproc;

/**
 *
 * @author otsuka
 */
public class MainWindowController implements Initializable {

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @FXML
    private AnchorPane ap;
    @FXML
    private Pane sp;
    @FXML
    private TextField txtProjectName;
    @FXML
    private Button btnLoad;
    @FXML
    private Button btnSave;
    @FXML
    private ComboBox<String> selFilter;
    @FXML
    private Button btnSearchSegment;
    @FXML
    private Button btnSearchSkin;
    @FXML
    private Button btnSplitIntoHSV;

    @FXML
    private TextField txtHueBegin;
    @FXML
    private TextField txtHueEnd;
    @FXML
    private Slider sldHueBegin;
    @FXML
    private Slider sldHueEnd;

    @FXML
    private TextField txtSatMin;
    @FXML
    private TextField txtSatMax;
    @FXML
    private Slider sldSatMin;
    @FXML
    private Slider sldSatMax;

    @FXML
    private TextField txtValMin;
    @FXML
    private TextField txtValMax;
    @FXML
    private Slider sldValMin;
    @FXML
    private Slider sldValMax;

    @FXML
    private TextField txtParam1;
    @FXML
    private TextField txtParam2;
    @FXML
    private Slider sldParam1;
    @FXML
    private Slider sldParam2;

    @FXML
    private ToggleButton btnSwitch;

    @FXML
    private ImageView imageView;

    @FXML
    private Label txtXY;
    @FXML
    private Label txtRGB;
    @FXML
    private Label txtHSV;

    Stage me;
    FileChooser fileChooser;
    Mat imageBGR, imageHSV, imageResult;
    File imageFile;
    File parameterFile;
    Parameters parameters;
    ArrayList<Circle> circles = new ArrayList();
//    static final File SYSTEM_PROPERTY_FILE = new File("conf.txt");
    static final File SYSTEM_PROPERTY_FILE = new File("ColorFilter.xml");
    private File lastDirectory;
    private ObservableList<Filter> filters;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        createMonitoredSlider(sldHueBegin, txtHueBegin);
        createMonitoredSlider(sldHueEnd, txtHueEnd);
        createMonitoredSlider(sldSatMin, txtSatMin);
        createMonitoredSlider(sldSatMax, txtSatMax);
        createMonitoredSlider(sldValMin, txtValMin);
        createMonitoredSlider(sldValMax, txtValMax);
        createMonitoredSlider(sldParam1, txtParam1);
        createMonitoredSlider(sldParam2, txtParam2);
        fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File("."));
        imageHSV = new Mat();
        imageResult = new Mat();
        selFilter.getItems().add("Laser");
        selFilter.getItems().add("Glare");
        selFilter.getItems().add("Scalemarker");
        parameters = new Parameters();
        loadParameter(SYSTEM_PROPERTY_FILE);

    }

    @FXML
    private void onSelFilterChanged(ActionEvent ev) {
        if (selFilter.getValue().equals("Laser")) {
            setFilter(parameters.getLaser().getFilter());
        } else if (selFilter.getValue().equals("Glare")) {
            setFilter(parameters.getGlare().getFilter());
        } else if (selFilter.getValue().equals("Scalemarker")) {
            setFilter(parameters.getScalemarker().getFilter());
        }
    }

    private void createMonitoredSlider(Slider slider, TextField txtField) {

        txtField.textProperty().bindBidirectional(slider.valueProperty(), new NumberStringConverter());
        slider.valueChangingProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(
                    ObservableValue<? extends Boolean> observableValue,
                    Boolean wasChanging,
                    Boolean changing) {
//                String valueString = String.format("%d", slider.getValue());

                if (changing) {
                } else {
                    updateImage();
                }
            }
        });

    }

    @FXML
    private void onBtLoadImageClicked() {
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        fileChooser.setInitialDirectory(lastDirectory);
        imageFile = fileChooser.showOpenDialog(null);
        if (imageFile != null) {
            try {
// 2021.07.19 n_otsuka  imread は使えない。(imread/imwrite に非ASCII文字を含むファイル名を渡すと読込に失敗するので)
//            imageBGR = imread(path, 1);
                imageBGR = Imgcodecs.imdecode(new MatOfByte(Files.readAllBytes(imageFile.toPath())), Imgcodecs.CV_LOAD_IMAGE_UNCHANGED);

                System.out.println(String.format("dims:%d, depth:%d, channels:%d\n", imageBGR.dims(), imageBGR.depth(), imageBGR.channels()));
                Imgproc.cvtColor(imageBGR, imageHSV, Imgproc.COLOR_BGR2HSV);
                txtProjectName.setText(imageFile.getName());
                lastDirectory = new File(imageFile.getParent());
                updateImage();

            } catch (CvException | IOException ex) {
                new Alert(Alert.AlertType.WARNING, "Failed to load image." + ex.getMessage(), ButtonType.OK).showAndWait();
            }
        }
    }

    @FXML
    private void onBtLoadClicked() {
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("*.xml", "*.xml"));
        parameterFile = fileChooser.showOpenDialog(null);
        if (parameterFile != null && parameterFile.isFile()) {
            loadParameter(parameterFile);
            updateImage();
        }
    }

    @FXML
    private void onBtSaveClicked() {
        fileChooser.getExtensionFilters().clear();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("*.txt", "*.txt"));
        parameterFile = fileChooser.showSaveDialog(null);
        if (parameterFile != null) {
            saveParameter(parameterFile);
        }
    }

    @FXML
    private void onBtnSwitchClicked(ActionEvent ev) {
        if (btnSwitch.isSelected()) {
            imageView.setImage(OpenCvUtils.mat2Image(imageBGR));
        } else {
            imageView.setImage(OpenCvUtils.mat2Image(imageResult));
        }
    }

    @FXML
    private void onBtnSearchSegmentClicked(ActionEvent ev) {
        Mat imgWork;
        updateImage();
        boolean done = false;

        Mat history = new Mat();
        while (!done) {
            searchContour(200, 0.0);
        }
    }

    @FXML
    private void onBtnSearchSkinClicked(ActionEvent ev) {

    }

    @FXML
    private void onMouseClicked(MouseEvent ev) {
        txtXY.setText(String.format("(X:%4.0f Y:%4.0f)", ev.getX(), ev.getY()));
        byte bytes[] = new byte[3];
        imageHSV.get((int) ev.getY(), (int) ev.getX(), bytes);

        int h = Byte.toUnsignedInt(bytes[0]);
        int s = Byte.toUnsignedInt(bytes[1]);
        int v = Byte.toUnsignedInt(bytes[2]);
        txtHSV.setText(String.format("(H:%3d S:%3d V:%3d)", h, s, v));
        imageBGR.get((int) ev.getY(), (int) ev.getX(), bytes);
        int b = Byte.toUnsignedInt(bytes[0]);
        int g = Byte.toUnsignedInt(bytes[1]);
        int r = Byte.toUnsignedInt(bytes[2]);
        txtRGB.setText(String.format("(B:%3d G:%3d R:%3d)", b, g, r));

    }

    @FXML
    private void onBtnSplitIntoHSVClicked(ActionEvent ev) {
        splitIntoHSV();
        new Alert(Alert.AlertType.INFORMATION, "保存しました").showAndWait();
    }

    private void loadParameter(File parameterFile) {
        try (InputStream is = new FileInputStream(parameterFile)) {
            parameters = JAXB.unmarshal(is, Parameters.class);
        } catch (FileNotFoundException ex) {
            //logger.error("TsTracker property load failed:", e);
            ex.printStackTrace();
        } catch (IOException ex) {
            //logger.error("TsTracker property load failed:", e);
            ex.printStackTrace();
        }

    }
    
    private void setFilter(Filter filter) {
        sldHueBegin.setValue(filter.getHMin());
        sldHueEnd.setValue(filter.getHMax());
        sldSatMin.setValue(filter.getSMin());
        sldSatMax.setValue(filter.getSMax());
        sldValMin.setValue(filter.getVMin());
        sldValMax.setValue(filter.getVMax());
        updateImage();
    }

    private void saveParameter(File parameterFile) {
        JAXB.marshal(parameters, parameterFile);
    }

    private void updateImage() {
        double hueBegin = sldHueBegin.getValue();
        double hueEnd = sldHueEnd.getValue();
        double satMin = sldSatMin.getValue();
        double satMax = sldSatMax.getValue();
        double valMin = sldValMin.getValue();
        double valMax = sldValMax.getValue();

        Mat hsv = new Mat();
        if (hueBegin <= hueEnd) {
            Core.inRange(imageHSV, new Scalar(hueBegin, satMin, valMin), new Scalar(hueEnd, satMax, valMax), imageResult);
        } else {
            Mat upper = new Mat();
            Mat lower = new Mat();
            Core.inRange(imageHSV, new Scalar(hueBegin, satMin, valMin), new Scalar(179, satMax, valMax), lower);
            Core.inRange(imageHSV, new Scalar(0, satMin, valMin), new Scalar(hueEnd, satMax, valMax), upper);
            Core.add(lower, upper, imageResult);
        }
        Image img = OpenCvUtils.mat2Image(imageResult);
        sp.setPrefSize(img.getWidth(), img.getHeight());
        imageView.setImage(img);
        xxx();

    }

    private void xxx() {
        for (Circle c : circles) {
            sp.getChildren().remove(c);
        }
        double param1 = sldParam1.getValue();
        double param2 = sldParam2.getValue();
        Mat circles = new Mat();
        Point3D[] circlesList = null;
        Imgproc.HoughCircles(imageResult, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 10, param1, param2, 16, 24);
        System.out.println("circles.cols()=" + circles.cols());
        for (int i = 0; i < circles.cols(); i++) {
            double[] vCircle = circles.get(0, i);
            Circle c = new Circle(vCircle[0], vCircle[1], vCircle[2]);
            c.setStroke(Color.BLUE);
            c.setFill(null);
            sp.getChildren().add(c);
        }
    }

    private boolean searchContour(int contourSize, double incline) {

        double hueBegin = sldHueBegin.getValue();
        double hueEnd = sldHueEnd.getValue();
        double satMin = sldSatMin.getValue();
        double satMax = sldSatMax.getValue();
        double valMin = sldValMin.getValue();
        double valMax = sldValMax.getValue();

        boolean found = false;
        while (true) {
            int result = findLaserImage(contourSize, incline, hueBegin, hueEnd, satMin, satMax, valMin, valMax);
            if (result == -1) { // too small.

            } else if (result == 1) {   // too large

            } else {
                found = true;
                break;
            }
        }
        return false;

    }

    private int findLaserImage(int contourSize, double incline, double hueBegin, double hueEnd, double satMin, double satMax, double valMin, double valMax) {
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat history = new Mat();
        // findContours(Mat image, List<MatOfPoint> contours, Mat hierarchy, int mode, int method)
        Imgproc.findContours(imageResult, contours, history,
                0, // CV_RETR_EXTERNAL 
                1 // CV_CHAIN_APPROX_NONE
        );

        return 0;

    }

    private void splitIntoHSV() {
        List<Mat> channels = new ArrayList<Mat>(3);
        Core.split(imageHSV, channels);
        imwrite(imageFile.getParent() + "/H.bmp", channels.get(0));
        imwrite(imageFile.getParent() + "/S.bmp", channels.get(1));
        imwrite(imageFile.getParent() + "/V.bmp", channels.get(2));
    }

    public byte[] convertFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }
}

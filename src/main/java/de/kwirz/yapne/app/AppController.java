package de.kwirz.yapne.app;

import javafx.application.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.logging.*;

import de.kwirz.yapne.model.PetriNet;
import de.kwirz.yapne.presentation.*;
import de.kwirz.yapne.utils.Settings;
import de.kwirz.yapne.io.PnmlParser;


public class AppController implements Initializable {
	
	private static final Logger logger = Logger.getLogger(AppController.class.getName());
	
	private boolean isDirty = false;
	private String currentFileName = "";
    private AppMode mode = AppMode.EDITING;

    @FXML
    private Text statusBar;

    private static final int STATUS_BAR_MESSAGE_DURATION = 5000; // ms

    @FXML
    private PetriNetPresentation canvas;
    
    private Stage primaryStage;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        canvas.setOnMouseClickedForEachElement(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                handleMouseEvent(mouseEvent);
            }
        });

        canvas.setOnMouseDraggedForEachElement(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                handleMouseEvent(mouseEvent);
            }
        });

        showStatusMessage("Bereit");

    	logger.info("initialized controller");
    }
    
    public void setPrimaryStage(Stage stage) {
    	primaryStage = stage;
    }

    @FXML
    void quit() {
        Platform.exit();
    }

    @FXML
    public void newDocument() {
    	isDirty = false;
    	currentFileName = "";
    	canvas.getChildren().clear();
    }
    
    @FXML
    public void openDocument() {
        Settings settings = Settings.getInstance();
        final String initialDirectory = settings.getValue("last_directory", System.getProperty("user.home"));

    	final FileChooser fileChooser = FileChooserBuilder.create()
    			.title("Open PNML File")
    			.extensionFilters(new FileChooser.ExtensionFilter("PNML files (*.pnml)", "*.pnml"))
                .initialDirectory(new File(initialDirectory))
    			.build();
    	
    	final File file = fileChooser.showOpenDialog(primaryStage);

        if (file == null) // no selection
            return;

        // Store directory
        try {
            settings.setValue("last_directory", file.getParent());
        } catch (IOException e) {
            MessageBox.error("couldn't store settings", primaryStage);
        }

        String source = null;
		try {
			source = new String(Files.readAllBytes(Paths.get(file.getPath())));
		} catch (IOException e) {
			MessageBox.error("couldn't read file " + file.getPath(), primaryStage);
			return;
		}
		
		assert source != null;
		
        PnmlParser parser = new PnmlParser();
        canvas.setModel(parser.parse(source));



    	currentFileName = file.getPath();
    	isDirty = false;
    }

    @FXML
    public void saveDocument() {
        PetriNet model = canvas.getModel();

        assert model != null;

        File file = null;

        if (!currentFileName.isEmpty()) {
            file = new File(currentFileName);
        } else {
            final String initialDirectory =
                    Settings.getInstance().getValue("initial_directory", System.getProperty("user.home"));
            final FileChooser fileChooser = FileChooserBuilder.create()
                    .title("Save PNML file")
                    .initialDirectory(new File(initialDirectory))
                    .build();
            file = fileChooser.showSaveDialog(primaryStage);

            if (file == null) {
                logger.log(Level.WARNING, "no file to save choosen");
                return;
            }
        }

        assert file != null;

        try {
            if (!file.toPath().getFileName().endsWith(".pnml")) {
                file = new File(file.toPath().toString() + ".pnml");
            }

            if (!file.exists())
                file.createNewFile();

            FileWriter writer = new FileWriter(file);
            writer.write(canvas.getModel().toXml());
            writer.close();

        } catch (IOException e) {
                e.printStackTrace();
        }

    }

    @FXML
    public void handleModeChange(ActionEvent event) {
        assert event.getEventType() == ActionEvent.ACTION;

        ToggleButton sourceButton = ((ToggleButton) event.getSource());

        if (!sourceButton.isSelected()) {
            mode = AppMode.EDITING;
        } else {
            switch(sourceButton.getId()) {
                case "place_mode_tb" : mode = AppMode.PLACE_CREATION; break;
                case "transition_mode_tb" : mode = AppMode.TRANSITION_CREATION; break;
                case "arc_mode_tb" : mode = AppMode.ARC_CREATION; break;
                default: throw new IllegalArgumentException("this place will be never reached");
            }
        }

        logger.log(Level.INFO, "new mode: " + mode);
    }

    @FXML
    private void handleMouseEvent(MouseEvent event) {
        final Object source = event.getSource();
        final Object target = event.getTarget();
        final EventType<? extends Event> eventType = event.getEventType();

        switch (mode) {

            case EDITING:
                if ( eventType == MouseEvent.MOUSE_DRAGGED && source instanceof PetriNetNodePresentation ) {
                    canvas.selectElement((PetriNetNodePresentation) source);
                    canvas.moveNode((PetriNetNodePresentation) source,
                            new Point2D(event.getSceneX(), event.getSceneY()));
                } else if ( eventType == MouseEvent.MOUSE_CLICKED && source instanceof PetriNetElementPresentation) {
                    canvas.selectElement((PetriNetElementPresentation) source);
                } else if ( eventType == MouseEvent.MOUSE_CLICKED && target instanceof  PetriNetPresentation ) {
                    canvas.unselectElement(canvas.getSelectedElement());
                }
                break;

            case PLACE_CREATION:
                if (    eventType == MouseEvent.MOUSE_CLICKED &&
                        source instanceof PetriNetPresentation &&
                        target instanceof PetriNetPresentation ) {
                    canvas.createPlace(event.getSceneX(), event.getSceneY());
                }
                break;

            case TRANSITION_CREATION:
                if (    eventType == MouseEvent.MOUSE_CLICKED &&
                        source instanceof PetriNetPresentation &&
                        target instanceof PetriNetPresentation ) {
                    canvas.createTransition(event.getSceneX(), event.getSceneY());
                }
                break;

            case ARC_CREATION:
                try {
                    if (eventType == MouseEvent.MOUSE_CLICKED) {
                        if (canvas.getSelectedElement() == null) {
                            if (source instanceof PetriNetNodePresentation)
                                canvas.selectElement((PetriNetElementPresentation) source);

                        } else { // element is selected
                            if (source instanceof PetriNetPresentation &&
                                    target instanceof PetriNetPresentation) {
                                canvas.unselectElement(canvas.getSelectedElement());
                            } else if (source instanceof PetriNetNodePresentation &&
                                    !canvas.getSelectedElement().getClass().equals(source.getClass())) {
                                canvas.createArc((PetriNetNodePresentation) canvas.getSelectedElement(),
                                        (PetriNetNodePresentation) source);
                                canvas.unselectElement(canvas.getSelectedElement());
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    showStatusMessage(e.getMessage(), 10000);
                }

                break;

            default:
                throw new RuntimeException("this place will be never reached");
        }
    }

    @FXML
    public void handleKeyEvent(KeyEvent event) {
        if (mode.equals(AppMode.EDITING) &&
                event.getEventType().equals(KeyEvent.KEY_PRESSED) &&
                (event.getCode().equals(KeyCode.BACK_SPACE)
                        || event.getCode().equals(KeyCode.DELETE))) {
            canvas.removeSelectedElement();
        }
    }

    @FXML
    public void about() {
    	MessageBox.about("YAPNE v1.0", primaryStage);
    }
    
    @FXML
    public void settings() {
    	Dialogs.showAndWait(new SettingsDialog(), primaryStage);
        canvas.refresh();
    }

    private void showStatusMessage(String message) {
        showStatusMessage(message, STATUS_BAR_MESSAGE_DURATION);
    }

    private void showStatusMessage(String message, int duration) {
        if (duration <= 0)
            throw new IllegalArgumentException("duration must be greater than 0");

        Timer timer = new Timer();
        statusBar.setText(message);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                statusBar.setText("");
            }
        }, duration);
    }

}

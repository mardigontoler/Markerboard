package markerboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.TemplateException;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.prefs.Preferences;


public class MarkerBoardFX extends Application {

    // constants for preferences names
    private static final String TEMPLATE_PERSIST = "TEMPLATE_PERSIST";
    private static final String DATA_MODEL_PERSIST = "DATA_MODEL_PERSIST";
    private static final String INCLUDE_QUOTES_PERSIST = "INCLUDE_QUOTES_PERSIST";

    private Preferences prefs = Preferences.userRoot().node(this.getClass().getName());


    private ObjectMapper objectMapper = new ObjectMapper();

    private TextArea templateBox;
    private TextArea dataBox;
    private TextArea outputBox;
    private CheckBox includeQuotesChkBox;

    private final double prefWidth = 1300;
    private final double prefHeight = 780;



    @Override
    public void start(Stage stage) {

        stage.setTitle("markerboard.Markerboard");

        // get last stored template & datamodel from preferences
        String lastTemplate = prefs.get(TEMPLATE_PERSIST, "");
        String lastDataModel = prefs.get(DATA_MODEL_PERSIST, "");
        // get other params
        boolean lastIncludeQuotes = prefs.getBoolean(INCLUDE_QUOTES_PERSIST, true);

        templateBox = new TextArea(lastTemplate);
        templateBox.setPromptText("template");

        dataBox = new TextArea(lastDataModel);
        dataBox.setPromptText("data model");

        outputBox = new TextArea();
        outputBox.setPromptText("output");
        outputBox.setEditable(false);


        Button renderBtn = new Button();
        renderBtn.setText("Render");
        renderBtn.setOnAction(this.handleRenderTemplate);

        includeQuotesChkBox = new CheckBox("Include quotes on TextNode");
        includeQuotesChkBox.setOnAction(event -> {FreemarkerEngine.setFreemarkerIncludeJacksonTextNodeQuotes(includeQuotesChkBox.isSelected());});
        includeQuotesChkBox.setSelected(lastIncludeQuotes);


        ToolBar toolbar = new ToolBar(renderBtn, includeQuotesChkBox);
        toolbar.setPadding(new Insets(5,5,5,5));


        GridPane gridPane = new GridPane();
        gridPane.add(templateBox, 0, 0);
        GridPane.setVgrow(templateBox, Priority.ALWAYS);
        GridPane.setHgrow(templateBox, Priority.ALWAYS);
        gridPane.add(dataBox, 0, 1);
        GridPane.setVgrow(dataBox, Priority.ALWAYS);
        GridPane.setHgrow(dataBox, Priority.ALWAYS);
        gridPane.add(outputBox, 1, 0, 1, 2);
        GridPane.setVgrow(outputBox, Priority.ALWAYS);
        GridPane.setHgrow(outputBox, Priority.ALWAYS);
        gridPane.setVgap(5);
        gridPane.setHgap(5);
        gridPane.setPadding(new Insets(5,5,5,5));


        VBox outerVBox = new VBox(toolbar, gridPane);
        VBox.setVgrow(gridPane, Priority.ALWAYS);

        Scene scene = new Scene(outerVBox, prefWidth, prefHeight);
        stage.setScene(scene);
        stage.show();

    }



    private EventHandler<ActionEvent> handleRenderTemplate = actionEvent -> {
        try {
            ObjectNode rootNode = (ObjectNode) objectMapper.readTree(dataBox.getText());
            String template = templateBox.getText();
            String processedTemplate = FreemarkerEngine.processTemplate(rootNode, template);
            outputBox.setText(processedTemplate);
        }
        catch (IOException | TemplateException ex){
            outputBox.setText(ex.getMessage());
        }
    };




    @Override
    public void stop() {
        // store text boxes so they can be reloaded later
        prefs.put(TEMPLATE_PERSIST, templateBox.getText());
        prefs.put(DATA_MODEL_PERSIST, dataBox.getText());
        prefs.putBoolean(INCLUDE_QUOTES_PERSIST, includeQuotesChkBox.isSelected());
    }



    public static void main(String[] args) {
        launch();
    }
}

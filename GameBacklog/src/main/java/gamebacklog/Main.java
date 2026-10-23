package gamebacklog;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) { new MainController(stage, getHostServices()); }

    public static void main(String[] args) { launch(args); }
}

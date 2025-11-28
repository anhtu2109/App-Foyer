import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Bootstrap class for the restaurant management UI.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/interface/view/MainView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(App.class.getResource("/interface/style/style.css").toExternalForm());
        primaryStage.setTitle("Restaurant Orders");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

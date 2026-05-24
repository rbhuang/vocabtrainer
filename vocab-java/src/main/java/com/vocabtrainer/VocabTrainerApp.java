package com.vocabtrainer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class VocabTrainerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        VocabTrainerView view = new VocabTrainerView();
        TrainingController controller = new TrainingController(view);

        // 绑定 UI 事件 → Controller
        view.getBtnKeep().setOnAction(e -> controller.keepWord());
        view.getBtnRemove().setOnAction(e -> controller.removeWord());
        view.getBtnPause().setOnAction(e -> controller.togglePause());
        view.getBtnQuit().setOnAction(e -> controller.quit());
        view.getCountdownSpinner().valueProperty().addListener((o, ov, nv) -> controller.onCountdownChanged(nv));
        view.getTtsCheckbox().selectedProperty().addListener((o, ov, nv) -> controller.onTtsToggled(nv));

        Scene scene = new Scene(view.getRoot(), 500, 380);
        scene.setOnKeyPressed(event -> controller.handleKey(event.getText().toLowerCase()));

        primaryStage.setTitle("单词背诵训练器");
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(380);
        primaryStage.setOnCloseRequest(event -> controller.onClose());

        try {
            Image icon = new Image(getClass().getResourceAsStream("/icon.png"));
            primaryStage.getIcons().add(icon);
        } catch (Exception ignored) {
        }

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

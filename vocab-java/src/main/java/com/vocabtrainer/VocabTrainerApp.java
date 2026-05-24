package com.vocabtrainer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class VocabTrainerApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        VocabTrainerView view = new VocabTrainerView();
        Scene scene = new Scene(view.getRoot(), 500, 380);

        // 全局键盘快捷键
        scene.setOnKeyPressed(event -> {
            String key = event.getText().toLowerCase();
            view.handleKey(key);
        });

        primaryStage.setTitle("单词背诵训练器");
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(380);

        // 加载图标
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

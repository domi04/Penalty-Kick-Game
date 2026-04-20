package com.deadball;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;

import com.deadball.core.Game;
import com.deadball.scene3d.GameScene3D;
import com.deadball.utils.GameConstants;

public class Main extends Application {
    private Game game;
    private GameScene3D gameScene3D;
    private Canvas hudCanvas;
    private Scene scene;
    private double lastFrameTime;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        game = new Game();
        gameScene3D = new GameScene3D();

        SubScene sub3D = new SubScene(
                gameScene3D.getRoot(),
                GameConstants.SCREEN_WIDTH,
                GameConstants.SCREEN_HEIGHT,
                true,
                SceneAntialiasing.BALANCED);
        sub3D.setFill(Color.web("#87b8d8"));

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(40);
        camera.setNearClip(0.1);
        camera.setFarClip(10000);
        Point3D eye = GameScene3D.cameraPosition();
        camera.getTransforms().setAll(
                new Rotate(11, Rotate.X_AXIS),
                new Translate(eye.getX(), eye.getY(), eye.getZ()));
        sub3D.setCamera(camera);
        sub3D.setOnMouseClicked(e -> hudCanvas.requestFocus());

        hudCanvas = new Canvas(GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT);
        hudCanvas.setFocusTraversable(true);

        StackPane root = new StackPane(sub3D, hudCanvas);

        scene = new Scene(root, GameConstants.SCREEN_WIDTH, GameConstants.SCREEN_HEIGHT);

        // Capture phase so Q/ESC quit even when the HUD canvas has keyboard focus.
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.Q || e.getCode() == KeyCode.ESCAPE) {
                primaryStage.close();
                e.consume();
            }
        });

        scene.setOnKeyPressed(event -> {
            game.handleKeyPress(event);
        });

        scene.setOnKeyReleased(game::handleKeyRelease);

        hudCanvas.setOnMouseClicked((MouseEvent event) -> {
            game.handleMouseClick(event);
            if (game.isExitRequested()) {
                this.primaryStage.close();
            }
        });

        root.setFocusTraversable(true);
        scene.setOnMouseClicked(e -> root.requestFocus());

        primaryStage.setTitle("Dead Ball - A Penalty Kick Showdown");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        Platform.runLater(() -> {
            root.requestFocus();
            hudCanvas.requestFocus();
        });

        lastFrameTime = System.nanoTime() / 1_000_000_000.0;
        startGameLoop();
    }

    private void startGameLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double currentTime = now / 1_000_000_000.0;
                double deltaTime = currentTime - lastFrameTime;
                lastFrameTime = currentTime;

                deltaTime = Math.min(deltaTime, 0.016);

                game.update(deltaTime);
                gameScene3D.sync(game);

                GraphicsContext gc = hudCanvas.getGraphicsContext2D();
                game.renderUiOverlay(gc);

                if (game.isExitRequested()) {
                    Main.this.primaryStage.close();
                }
            }
        };
        timer.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

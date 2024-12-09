package com.example.quizgame.client;

import com.exemple.quizgame.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class QuizGameApp extends Application {
    private QuizGameGrpc.QuizGameBlockingStub stub;
    private String currentPlayer;
    private Quiz currentQuiz;
    private int currentQuestionIndex = 0;
    private GetQuizResponse quizResponse;

    // Updated colors for dark theme
    private static final String BACKGROUND_COLOR = "#121212";
    private static final String PRIMARY_COLOR = "#BB86FC";
    private static final String SECONDARY_COLOR = "#03DAC6";
    private static final String TEXT_COLOR = "#FFFFFF";
    private static final String SURFACE_COLOR = "#1E1E1E";

    private void applyModernTheme(Scene scene) {
        String css = ".root { -fx-background-color: " + BACKGROUND_COLOR + "; }" +
                ".quiz-option { -fx-background-color: " + SURFACE_COLOR + ";" +
                "-fx-text-fill: " + TEXT_COLOR + "; -fx-padding: 15px; -fx-background-radius: 8px;" +
                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 0); }" +
                ".quiz-option:hover { -fx-background-color: derive(" + SURFACE_COLOR + ", 10%);" +
                "-fx-effect: dropshadow(gaussian, rgba(187,134,252,0.3), 15, 0, 0, 0); }" +
                ".button { -fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: " + BACKGROUND_COLOR +
                "; -fx-background-radius: 20px; -fx-cursor: hand; }";

        try {
            Path tempCssFile = Files.createTempFile("quiz_theme", ".css");
            Files.write(tempCssFile, css.getBytes());
            scene.getStylesheets().add(tempCssFile.toUri().toURL().toExternalForm());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void applyEntranceAnimation(Node control) {
        TranslateTransition translate = new TranslateTransition(Duration.millis(800), control);
        translate.setFromY(50);
        translate.setToY(0);
        translate.setInterpolator(Interpolator.SPLINE(0.1, 0.9, 0.2, 1.0));

        FadeTransition fade = new FadeTransition(Duration.millis(1000), control);
        fade.setFromValue(0);
        fade.setToValue(1);

        ParallelTransition parallel = new ParallelTransition(translate, fade);
        parallel.play();
    }

    @Override
    public void start(Stage primaryStage) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .build();
        stub = QuizGameGrpc.newBlockingStub(channel);

        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        Scene scene = new Scene(mainLayout, 800, 600);
        applyModernTheme(scene);

        primaryStage.setTitle("Quiz Game");
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true);
        primaryStage.setFullScreenExitHint("");
        primaryStage.setOpacity(0.95);
        primaryStage.show();

        showPlayerRegistration(mainLayout, primaryStage);

        primaryStage.setOnCloseRequest(e -> {
            channel.shutdown();
            Platform.exit();
        });
    }

    private void showPlayerRegistration(VBox mainLayout, Stage primaryStage) {
        // Replace ASCII art with an image
        Image logoImage = new Image(getClass().getResourceAsStream("/images/Quiz.jpg"));
        ImageView logoView = new ImageView(logoImage);
        logoView.setFitWidth(300);
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);

        // Apply entrance animation
        applyEntranceAnimation(logoView);

        Label titleLabel = new Label("WELCOME TO BRAHIM QUIZ!");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_COLOR + ";");

        TextField playerNameField = new TextField();
        playerNameField.setPromptText("Enter your name");
        playerNameField.setMaxWidth(200);
        styleControl(playerNameField);

        Button startButton = new Button("Start Game");
        styleControl(startButton);
        startButton.setOnAction(e -> {
            String playerName = playerNameField.getText().trim();
            if (!playerName.isEmpty()) {
                try {
                    registerPlayer(playerName);
                    currentPlayer = playerName;
                    loadQuizzes(mainLayout, primaryStage);
                } catch (Exception ex) {
                    showError("Registration Error", ex.getMessage());
                }
            } else {
                showError("Invalid Input", "Please enter your name");
            }
        });

        playerNameField.setOnAction(e -> startButton.fire());

        mainLayout.getChildren().addAll(logoView, titleLabel, playerNameField, startButton);
        applyEntranceAnimation(titleLabel);
        applyEntranceAnimation(playerNameField);
        applyEntranceAnimation(startButton);
    }

    private void loadQuizzes(VBox mainLayout, Stage primaryStage) {
        try {
            GetQuizRequest quizRequest = GetQuizRequest.newBuilder().build();
            quizResponse = stub.getQuiz(quizRequest);

            if (!quizResponse.getQuizList().isEmpty()) {
                currentQuestionIndex = 0;
                showQuestion(mainLayout, primaryStage);
            }
        } catch (Exception e) {
            showError("Error", "Failed to load quizzes: " + e.getMessage());
        }
    }

    private void showQuestion(VBox mainLayout, Stage primaryStage) {
        mainLayout.getChildren().clear();
        currentQuiz = quizResponse.getQuiz(currentQuestionIndex);

        Label questionLabel = new Label("Question " + (currentQuestionIndex + 1) + "/" + quizResponse.getQuizCount());
        questionLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: " + TEXT_COLOR + ";");

        Label questionText = new Label(currentQuiz.getQuestion());
        questionText.setWrapText(true);
        questionText.setStyle("-fx-font-size: 16px; -fx-text-fill: " + TEXT_COLOR + ";");

        VBox answersBox = new VBox(10);
        Button[] answerButtons = {
                new Button(currentQuiz.getAnswer1()),
                new Button(currentQuiz.getAnswer2()),
                new Button(currentQuiz.getAnswer3()),
                new Button(currentQuiz.getAnswer4())
        };

        for (Button btn : answerButtons) {
            btn.getStyleClass().add("quiz-option");
            btn.setOnAction(e -> handleQuizOptionClick(btn, answersBox, mainLayout, primaryStage));
            answersBox.getChildren().add(btn);
        }

        mainLayout.getChildren().addAll(questionLabel, questionText, answersBox);
        applyEntranceAnimation(questionLabel);
        applyEntranceAnimation(questionText);
        applyEntranceAnimation(answersBox);
    }

    private void handleQuizOptionClick(Button selectedButton, VBox answersBox, VBox mainLayout, Stage primaryStage) {
        int selectedAnswer = answersBox.getChildren().indexOf(selectedButton) + 1;
        submitAnswer(selectedAnswer, mainLayout, primaryStage);
    }

    private void submitAnswer(int answer, VBox mainLayout, Stage primaryStage) {
        try {
            PlayRequest playRequest = PlayRequest.newBuilder()
                    .setPlayerName(currentPlayer)
                    .setQuizId(currentQuiz.getId())
                    .setAnswer(answer)
                    .build();

            PlayResponse playResponse = stub.play(playRequest);
            showAnswerResult(playResponse.getCorrectAnswer() == answer, mainLayout, primaryStage);
        } catch (Exception e) {
            showError("Error", e.getMessage());
        }
    }

    private void showAnswerResult(boolean correct, VBox mainLayout, Stage primaryStage) {
        // Update layout and show results here
    }

    private void registerPlayer(String playerName) {
        stub.registerPlayer(RegisterPlayerRequest.newBuilder().setPlayerName(playerName).build());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void styleControl(Control control) {
        if (control instanceof Button) {
            control.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

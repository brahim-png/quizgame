package com.example.quizgame.server;

import com.exemple.quizgame.proto.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ServiceQuizGame extends QuizGameGrpc.QuizGameImplBase {
    private static final Logger logger = LoggerFactory.getLogger(ServiceQuizGame.class);
    private final List<Player> players = new ArrayList<>();
    private final List<Quiz> quizzes = new ArrayList<>();

    public ServiceQuizGame() {
        initializeQuizQuestions();
    }

    private void initializeQuizQuestions() {
        quizzes.add(Quiz.newBuilder()
                .setId(1)
                .setQuestion("Which is the largest desert in the world?")
                .setAnswer1("Sahara")
                .setAnswer2("Arabian Desert")
                .setAnswer3("Gobi Desert")
                .setAnswer4("Antarctic Desert")
                .setCorrectAnswer(4)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setId(2)
                .setQuestion("Which famous scientist developed the theory of relativity?")
                .setAnswer1("Isaac Newton")
                .setAnswer2("Albert Einstein")
                .setAnswer3("Nikola Tesla")
                .setAnswer4("Galileo Galilei")
                .setCorrectAnswer(2)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setId(3)
                .setQuestion("Which city is known as the Big Apple?")
                .setAnswer1("Los Angeles")
                .setAnswer2("New York")
                .setAnswer3("Chicago")
                .setAnswer4("Miami")
                .setCorrectAnswer(2)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setId(4)
                .setQuestion("What is the capital of Canada?")
                .setAnswer1("Ottawa")
                .setAnswer2("Toronto")
                .setAnswer3("Vancouver")
                .setAnswer4("Montreal")
                .setCorrectAnswer(1)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setId(5)
                .setQuestion("Who wrote the play 'Romeo and Juliet'?")
                .setAnswer1("Charles Dickens")
                .setAnswer2("William Shakespeare")
                .setAnswer3("George Orwell")
                .setAnswer4("Jane Austen")
                .setCorrectAnswer(2)
                .build());

        quizzes.add(Quiz.newBuilder()
                .setId(6)
                .setQuestion("What is the chemical symbol for gold?")
                .setAnswer1("Au")
                .setAnswer2("Ag")
                .setAnswer3("Pb")
                .setAnswer4("Fe")
                .setCorrectAnswer(1)
                .build());

        logger.info("Initialized {} quiz questions", quizzes.size());
    }

    @Override
    public void registerPlayer(RegisterPlayerRequest request, StreamObserver<RegisterPlayerResponse> responseObserver) {
        try {
            String playerName = request.getPlayerName();
            logger.info("Registering player: {}", playerName);

            // Validate player name
            if (playerName == null || playerName.trim().isEmpty()) {
                responseObserver.onError(
                        Status.INVALID_ARGUMENT
                                .withDescription("Player name cannot be empty")
                                .asException()
                );
                return;
            }

            // Check for duplicate player
            boolean playerExists = players.stream()
                    .anyMatch(p -> p.getPlayerName().equals(playerName));

            if (playerExists) {
                responseObserver.onError(
                        Status.ALREADY_EXISTS
                                .withDescription("Player already registered: " + playerName)
                                .asException()
                );
                return;
            }

            // Create and add new player
            Player player = Player.newBuilder()
                    .setPlayerName(playerName)
                    .setScore(0)
                    .build();
            players.add(player);

            // Send response
            RegisterPlayerResponse response = RegisterPlayerResponse.newBuilder()
                    .setPlayer(player)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            logger.info("New player registered: {}", playerName);

        } catch (Exception e) {
            logger.error("Error registering player: ", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .withCause(e)
                            .asException()
            );
        }
    }

    @Override
    public void getQuiz(GetQuizRequest request, StreamObserver<GetQuizResponse> responseObserver) {
        try {
            GetQuizResponse response = GetQuizResponse.newBuilder()
                    .addAllQuiz(quizzes)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error retrieving quizzes: ", e);
            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Internal server error")
                            .withCause(e)
                            .asException()
            );
        }
    }

    @Override
    public void getQuestion(GetQuestionRequest request, StreamObserver<GetQuestionResponse> responseObserver) {
        Quiz quiz = quizzes.stream()
                .filter(q -> q.getId() == request.getQuizId())
                .findFirst()
                .orElse(null);

        if (quiz != null) {
            GetQuestionResponse response = GetQuestionResponse.newBuilder()
                    .setQuiz(quiz)
                    .build();
            responseObserver.onNext(response);
        } else {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Quiz not found with ID: " + request.getQuizId())
                            .asException()
            );
        }
        responseObserver.onCompleted();
    }

    @Override
    public void play(PlayRequest request, StreamObserver<PlayResponse> responseObserver) {
        Player player = players.stream()
                .filter(p -> p.getPlayerName().equals(request.getPlayerName()))
                .findFirst()
                .orElse(null);

        Quiz quiz = quizzes.stream()
                .filter(q -> q.getId() == request.getQuizId())
                .findFirst()
                .orElse(null);

        if (player != null && quiz != null) {
            int score = player.getScore();
            if (quiz.getCorrectAnswer() == request.getAnswer()) {
                score++;
            }

            // Create a final variable for the updated player
            final Player updatedPlayer = player.toBuilder().setScore(score).build();

            // Update player score
            players.removeIf(p -> p.getPlayerName().equals(updatedPlayer.getPlayerName()));
            players.add(updatedPlayer);

            PlayResponse response = PlayResponse.newBuilder()
                    .setPlayer(updatedPlayer)
                    .setCorrectAnswer(quiz.getCorrectAnswer())
                    .build();
            responseObserver.onNext(response);
        } else {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Player or Quiz not found")
                            .asException()
            );
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getPlayerScores(GetPlayerScoresRequest request, StreamObserver<GetPlayerScoresResponse> responseObserver) {
        GetPlayerScoresResponse response = GetPlayerScoresResponse.newBuilder()
                .addAllPlayers(players)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}

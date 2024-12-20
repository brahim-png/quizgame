package com.example.quizgame.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class QuizGameServer {

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(50051)
                .addService(new ServiceQuizGame())
                .build()
                .start();

        System.out.println("Server started on port 50051");
        server.awaitTermination();
    }
}

package network.client;

import network.client.service.AccountClientService;
import network.client.service.MatchClientService;
import network.protocol.MessageType;
import network.protocol.NetworkJsonCodec;
import network.protocol.NetworkMessage;
import network.protocol.auth.LoginRequest;
import network.protocol.auth.LoginResponse;
import network.protocol.matchmaking.InviteReceived;
import network.protocol.matchmaking.MatchFoundDto;

import java.util.Scanner;

public class MatchmakingClientMain {
    private MatchmakingClientMain() {
    }

    public static void main(String[] args) {

        NetworkClient networkClient =
                new NetworkClient();

        NetworkJsonCodec codec =
                new NetworkJsonCodec();

        Scanner scanner =
                new Scanner(System.in);


        try {

            networkClient.connect(
                    "127.0.0.1",
                    5050
            );


            AccountClientService accountService =
                    new AccountClientService(
                            networkClient
                    );

            MatchClientService matchService =
                    new MatchClientService(
                            networkClient
                    );

            networkClient.setEventListener(
                    message -> {

                        try {

                            handleServerEvent(
                                    message,
                                    codec
                            );

                        } catch (Exception exception) {

                            System.err.println(
                                    "[EVENT ERROR] "
                                            + exception.getMessage()
                            );

                            exception.printStackTrace();
                        }
                    }
            );

            System.out.print(
                    "Username: "
            );

            String username =
                    scanner.nextLine()
                            .trim();


            System.out.print(
                    "Password: "
            );

            String password =
                    scanner.nextLine();


            LoginResponse loginResponse =
                    accountService.login(
                                    new LoginRequest(
                                            username,
                                            password,
                                            false
                                    )
                            )
                            .join();


            if (!loginResponse.isSuccess()) {

                System.err.println(
                        "LOGIN FAILED: "
                                + loginResponse.getMessage()
                );

                return;
            }


            System.out.println();
            System.out.println(
                    "LOGIN OK: " + username
            );

            printCommands();


            while (true) {

                System.out.print(
                        "> "
                );

                String line =
                        scanner.nextLine()
                                .trim();


                if (line.isEmpty()) {
                    continue;
                }


                String[] parts =
                        line.split(
                                "\\s+",
                                2
                        );


                String command =
                        parts[0]
                                .toLowerCase();


                switch (command) {

                    case "queue" -> {

                        var response =
                                matchService
                                        .joinRandomQueue()
                                        .join();

                        System.out.println(
                                "[QUEUE] success="
                                        + response.success()
                                        + " waiting="
                                        + response.waiting()
                                        + " message="
                                        + response.message()
                        );
                    }


                    case "leave" -> {

                        var response =
                                matchService
                                        .leaveRandomQueue()
                                        .join();

                        System.out.println(
                                "[LEAVE] success="
                                        + response.success()
                                        + " waiting="
                                        + response.waiting()
                                        + " message="
                                        + response.message()
                        );
                    }


                    case "challenge" -> {

                        if (parts.length < 2
                                || parts[1].isBlank()) {

                            System.out.println(
                                    "Usage: challenge <username>"
                            );

                            continue;
                        }


                        String targetUsername =
                                parts[1].trim();


                        var response =
                                matchService
                                        .challenge(
                                                targetUsername
                                        )
                                        .join();


                        System.out.println(
                                "[CHALLENGE] success="
                                        + response.success()
                                        + " message="
                                        + response.message()
                        );
                    }


                    case "accept" -> {

                        if (parts.length < 2
                                || parts[1].isBlank()) {

                            System.out.println(
                                    "Usage: accept <challengerUsername>"
                            );

                            continue;
                        }


                        var response =
                                matchService
                                        .respondToInvite(
                                                parts[1].trim(),
                                                true
                                        )
                                        .join();


                        System.out.println(
                                "[ACCEPT] success="
                                        + response.success()
                                        + " message="
                                        + response.message()
                        );
                    }


                    case "reject" -> {

                        if (parts.length < 2
                                || parts[1].isBlank()) {

                            System.out.println(
                                    "Usage: reject <challengerUsername>"
                            );

                            continue;
                        }


                        var response =
                                matchService
                                        .respondToInvite(
                                                parts[1].trim(),
                                                false
                                        )
                                        .join();


                        System.out.println(
                                "[REJECT] success="
                                        + response.success()
                                        + " message="
                                        + response.message()
                        );
                    }


                    case "help" ->
                            printCommands();


                    case "exit" -> {

                        networkClient.close();

                        return;
                    }


                    default ->

                            System.out.println(
                                    "Unknown command. Type help."
                            );
                }
            }

        } catch (Exception exception) {

            System.err.println(
                    "CLIENT ERROR: "
                            + exception.getMessage()
            );

            exception.printStackTrace();

        } finally {

            networkClient.close();
        }
    }


    private static void handleServerEvent(
            NetworkMessage message,
            NetworkJsonCodec codec
    ) throws Exception {

        if (message == null) {
            return;
        }


        System.out.println();
        System.out.println(
                "[SERVER EVENT] "
                        + message.getType()
        );


        if (message.getType()
                == MessageType.MATCHMAKING_INVITE_RECEIVED) {

            InviteReceived invite =
                    codec.decodePayload(
                            message.getPayload(),
                            InviteReceived.class
                    );


            System.out.println(
                    "Challenge from: "
                            + invite.challengerUsername()
            );

            System.out.println(
                    "Use: accept "
                            + invite.challengerUsername()
            );

            System.out.println(
                    "or:  reject "
                            + invite.challengerUsername()
            );

            return;
        }


        if (message.getType()
                == MessageType.MATCHMAKING_INVITE_REJECTED) {

            System.out.println(
                    "Challenge rejected/cancelled: "
                            + message.getPayload()
            );

            return;
        }


        if (message.getType()
                == MessageType.MATCHMAKING_MATCH_FOUND) {

            MatchFoundDto match =
                    codec.decodePayload(
                            message.getPayload(),
                            MatchFoundDto.class
                    );


            System.out.println(
                    "MATCH FOUND"
            );

            System.out.println(
                    "matchId  = "
                            + match.matchId()
            );

            System.out.println(
                    "opponent = "
                            + match.opponentUsername()
            );

            System.out.println(
                    "role     = "
                            + match.role()
            );

            return;
        }


        System.out.println(
                "payload = "
                        + message.getPayload()
        );
    }


    private static void printCommands() {

        System.out.println();
        System.out.println(
                "Commands:"
        );

        System.out.println(
                "  queue"
        );

        System.out.println(
                "  leave"
        );

        System.out.println(
                "  challenge <username>"
        );

        System.out.println(
                "  accept <challengerUsername>"
        );

        System.out.println(
                "  reject <challengerUsername>"
        );

        System.out.println(
                "  help"
        );

        System.out.println(
                "  exit"
        );

        System.out.println();
    }
}

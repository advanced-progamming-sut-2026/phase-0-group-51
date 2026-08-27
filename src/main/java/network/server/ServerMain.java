package network.server;

import java.io.IOException;

public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) {
        int port = parsePort(args);
        GameServer server = new GameServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));

        try {
            server.start();
        } catch (IOException exception) {
            System.err.println("Server failed: " + exception.getMessage());
            server.close();
        }
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return GameServer.DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            System.err.println("Invalid port '" + args[0] + "'. Using " + GameServer.DEFAULT_PORT + ".");
            return GameServer.DEFAULT_PORT;
        }
    }
}

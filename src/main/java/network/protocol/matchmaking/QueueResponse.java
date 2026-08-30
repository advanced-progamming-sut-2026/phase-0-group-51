package network.protocol.matchmaking;

public record QueueResponse( boolean success, String message, boolean waiting) {
}

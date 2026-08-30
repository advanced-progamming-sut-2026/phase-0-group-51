package network.server.matchmaking;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class RandomQueue {

    private final Queue<String> queue =
            new ArrayDeque<>();

    private final Set<String> queued =
            new HashSet<>();

    public synchronized boolean enqueue(
            String username
    ) {
        if (username == null
                || username.isBlank()
                || queued.contains(username)) {

            return false;
        }

        queued.add(username);
        queue.offer(username);

        return true;
    }

    public synchronized String dequeue() {

        String username = queue.poll();

        if (username != null) {
            queued.remove(username);
        }

        return username;
    }

    public synchronized boolean remove(
            String username
    ) {
        if (!queued.remove(username)) {
            return false;
        }

        queue.remove(username);
        return true;
    }

    public synchronized boolean contains(
            String username
    ) {
        return username != null
                && queued.contains(username);
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized void clear() {
        queue.clear();
        queued.clear();
    }
}
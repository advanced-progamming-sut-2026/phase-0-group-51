package network.server.matchmaking;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

        String normalized =
                normalize(username);

        if (normalized == null) {
            return false;
        }

        if (queued.contains(normalized)) {
            return false;
        }

        queued.add(normalized);

        queue.offer(normalized);

        return true;
    }


    public synchronized String dequeue() {

        String username =
                queue.poll();

        if (username != null) {

            queued.remove(username);
        }

        return username;
    }


    public synchronized boolean remove(
            String username
    ) {

        String normalized =
                normalize(username);

        if (normalized == null) {
            return false;
        }

        if (!queued.remove(normalized)) {
            return false;
        }

        queue.remove(normalized);

        return true;
    }


    public synchronized boolean contains(
            String username
    ) {

        String normalized =
                normalize(username);

        return normalized != null
                && queued.contains(normalized);
    }


    public synchronized int size() {

        return queue.size();
    }


    public synchronized boolean isEmpty() {

        return queue.isEmpty();
    }


    public synchronized List<String> snapshot() {

        return new ArrayList<>(queue);
    }


    public synchronized void clear() {

        queue.clear();

        queued.clear();
    }


    private String normalize(
            String username
    ) {

        if (username == null) {
            return null;
        }

        String normalized =
                username.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}
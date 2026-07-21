package models.games;

import models.Board.Board;
import models.Board.Tile;

import java.util.List;
import java.util.Locale;

public class TerminalMapRenderer {
    private static final int ROW_PREFIX_WIDTH = 8;
    private static final int CELL_WIDTH = 8;
    private static final int CELL_CONTENT_LENGTH = 4;

    private TerminalMapRenderer() {
    }

    public static String render(
            String title,
            List<String> metrics,
            String details,
            Board board,
            CellFormatter cellFormatter,
            RowLabelFormatter rowLabelFormatter,
            RowSuffixFormatter rowSuffixFormatter,
            int dividerBeforeColumn
    ) {
        StringBuilder output = new StringBuilder();
        int width = mapWidth(board, dividerBeforeColumn);

        appendTitle(output, title, width);
        appendMetrics(output, metrics, width);
        appendDetails(output, details);
        appendBoard(
                output,
                board,
                cellFormatter,
                rowLabelFormatter,
                rowSuffixFormatter,
                dividerBeforeColumn
        );
        return output.toString();
    }

    private static int mapWidth(Board board, int dividerBeforeColumn) {
        int dividerWidth = validDivider(board, dividerBeforeColumn) ? 1 : 0;
        return ROW_PREFIX_WIDTH + board.getColumnCount() * CELL_WIDTH + dividerWidth;
    }

    private static void appendTitle(
            StringBuilder output,
            String title,
            int width
    ) {
        String normalizedTitle = title == null || title.isBlank()
                ? "MAP"
                : title.trim();
        String centered = " " + normalizedTitle + " ";
        int remaining = Math.max(6, width - centered.length());
        int left = remaining / 2;
        int right = remaining - left;

        output.append("=".repeat(left))
                .append(centered)
                .append("=".repeat(right))
                .append('\n');
    }

    private static void appendMetrics(
            StringBuilder output,
            List<String> metrics,
            int width
    ) {
        if (metrics == null || metrics.isEmpty()) {
            output.append('\n');
            return;
        }

        StringBuilder line = new StringBuilder();
        for (String metric : metrics) {
            if (metric == null || metric.isBlank()) {
                continue;
            }
            String value = metric.trim();
            int separatorLength = line.isEmpty() ? 0 : 4;
            if (!line.isEmpty()
                    && line.length() + separatorLength + value.length() > width) {
                output.append(line).append('\n');
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append("    ");
            }
            line.append(value);
        }
        if (!line.isEmpty()) {
            output.append(line).append('\n');
        }
        output.append('\n');
    }

    private static void appendDetails(
            StringBuilder output,
            String details
    ) {
        if (details == null || details.isBlank()) {
            return;
        }
        output.append(details.stripTrailing()).append("\n\n");
    }

    private static void appendBoard(
            StringBuilder output,
            Board board,
            CellFormatter cellFormatter,
            RowLabelFormatter rowLabelFormatter,
            RowSuffixFormatter rowSuffixFormatter,
            int dividerBeforeColumn
    ) {
        appendColumnHeader(output, board, dividerBeforeColumn);
        appendBorder(output, board, dividerBeforeColumn);

        for (int lane = 0; lane < board.getLaneCount(); lane++) {
            appendRow(
                    output,
                    board,
                    lane,
                    cellFormatter,
                    rowLabelFormatter,
                    rowSuffixFormatter,
                    dividerBeforeColumn
            );
            appendBorder(output, board, dividerBeforeColumn);
        }
    }

    private static void appendColumnHeader(
            StringBuilder output,
            Board board,
            int dividerBeforeColumn
    ) {
        output.append(" ".repeat(ROW_PREFIX_WIDTH));
        for (int column = 0; column < board.getColumnCount(); column++) {
            appendDivider(output, board, column, dividerBeforeColumn, '|');
            output.append(String.format(
                    Locale.ROOT,
                    "%" + CELL_WIDTH + "d",
                    column + 1
            ));
        }
        output.append('\n');
    }

    private static void appendBorder(
            StringBuilder output,
            Board board,
            int dividerBeforeColumn
    ) {
        output.append(" ".repeat(ROW_PREFIX_WIDTH - 1)).append('+');
        for (int column = 0; column < board.getColumnCount(); column++) {
            appendDivider(
                    output,
                    board,
                    column,
                    dividerBeforeColumn,
                    '+'
            );
            output.append("-------+");
        }
        output.append('\n');
    }

    private static void appendRow(
            StringBuilder output,
            Board board,
            int lane,
            CellFormatter cellFormatter,
            RowLabelFormatter rowLabelFormatter,
            RowSuffixFormatter rowSuffixFormatter,
            int dividerBeforeColumn
    ) {
        String label = rowLabelFormatter == null
                ? "Row " + (lane + 1)
                : rowLabelFormatter.format(lane);
        output.append(String.format(Locale.ROOT, "%-7s|", label));

        for (int column = 0; column < board.getColumnCount(); column++) {
            appendDivider(
                    output,
                    board,
                    column,
                    dividerBeforeColumn,
                    '|'
            );
            Tile tile = board.getTile(lane, column);
            String cell = normalizeCell(cellFormatter.format(tile));
            output.append(String.format(Locale.ROOT, " %-6s|", cell));
        }

        if (rowSuffixFormatter != null) {
            String suffix = rowSuffixFormatter.format(lane);
            if (suffix != null && !suffix.isBlank()) {
                output.append("  ").append(suffix.trim());
            }
        }
        output.append('\n');
    }

    private static void appendDivider(
            StringBuilder output,
            Board board,
            int column,
            int dividerBeforeColumn,
            char marker
    ) {
        if (validDivider(board, dividerBeforeColumn)
                && column == dividerBeforeColumn) {
            output.append(marker);
        }
    }

    private static boolean validDivider(
            Board board,
            int dividerBeforeColumn
    ) {
        return dividerBeforeColumn > 0
                && dividerBeforeColumn < board.getColumnCount();
    }

    private static String normalizeCell(String cell) {
        if (cell == null || cell.isBlank()) {
            return ".".repeat(CELL_CONTENT_LENGTH);
        }
        String normalized = cell.trim();
        if (normalized.length() > CELL_CONTENT_LENGTH) {
            return normalized.substring(0, CELL_CONTENT_LENGTH);
        }
        return normalized + ".".repeat(CELL_CONTENT_LENGTH - normalized.length());
    }

    @FunctionalInterface
    public interface CellFormatter {
        String format(Tile tile);
    }

    @FunctionalInterface
    public interface RowLabelFormatter {
        String format(int lane);
    }

    @FunctionalInterface
    public interface RowSuffixFormatter {
        String format(int lane);
    }
}

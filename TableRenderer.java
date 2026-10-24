import java.util.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class TableRenderer {
    private String title;
    private List<String> headers = new ArrayList<>();
    private List<List<String>> rows = new ArrayList<>();
    private int pageSize = 10;
    private int currentPage = 1;
    private String emptyMessage = "(No Records Found)";

    public TableRenderer() {
    }

    public TableRenderer(String title, String[] headers) {
        this.title = title;
        if (headers != null) {
            this.headers.addAll(Arrays.asList(headers));
        }
    }

    public TableRenderer(String... headers) {
        this(null, headers);
    }

    public TableRenderer withTitle(String title) { 
        this.title = title;
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setEmptyMessage(String emptyMessage) {
        if (emptyMessage != null) {
            this.emptyMessage = emptyMessage;
        }
    }

    public TableRenderer withPageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        this.pageSize = pageSize;
        return this;
    }

    public TableRenderer withPage(int page) {
        if (page <= 0) {
            throw new IllegalArgumentException("Page number must be greater than 0");
        }
        this.currentPage = page;
        return this;
    }

    public TableRenderer addHeaders(String... headers) {
        this.headers.clear();
        this.headers.addAll(Arrays.asList(headers));
        return this;
    }

    public TableRenderer addRow(String... rows) {
        List<String> list = new ArrayList<>();
        int colCount = Math.max(headers.size(), rows.length);
        for (int i = 0; i < colCount; i++) {
            if (i < rows.length && rows[i] != null) {
                list.add(rows[i]);
            } else {
                list.add("");
            }
        }
        if (headers.size() < list.size()) {
            for (int i = headers.size(); i < list.size(); i++) {
                headers.add("");
            }
        }
        this.rows.add(list);
        return this;
    }

    public void render() {
        if (headers.isEmpty()) {
            System.out.println("(No items to display)");
            return;
        }

        int[] widths = calculateColumnWidths();

        printOuterBorder(widths);

        if (title != null && !title.isEmpty()) {
            printTitle(title, widths);
            printOuterBorder(widths);
        }

        printRow(headers, widths);
        printBorder(widths);

        if (rows.isEmpty()) {
            List<String> emptyRow = new ArrayList<>();
            emptyRow.add(emptyMessage);
            for (int i = 1; i < headers.size(); i++) {
                emptyRow.add("");
            }
            printRow(emptyRow, widths);
            printBorder(widths);
            return;
        }

        int totalPages = (rows.size() + pageSize - 1) / pageSize;
        int page = Math.min(currentPage, totalPages);
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, rows.size());

        for (int i = start; i < end; i++) {
            printRow(rows.get(i), widths);
        }

        printBorder(widths);
        if (totalPages > 1) {
            System.out.println("Page " + page + " of " + totalPages);
        }
    }

    private void printOuterBorder(int[] widths) {
        int borderWidth = 1;
        for (int width : widths) {
            borderWidth += width + 3;
        }
        System.out.println("+" + "-".repeat(Math.max(1, borderWidth - 2)) + "+");
    }

    private void printBorder(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append("+");
        }
        System.out.println(sb);
    }

    private void printTitle(String title, int[] widths) {
        int totalWidth = 1;
        for (int width : widths) {
            totalWidth += width + 3;
        }
        int innerWidth = totalWidth - 2;

        if (title.length() > innerWidth) {
            title = title.substring(0, Math.max(0, innerWidth - 3)) + "...";
        }

        int padding = (innerWidth - title.length()) / 2;
        StringBuilder sb = new StringBuilder("|");
        sb.append(" ".repeat(Math.max(0, padding)));
        sb.append(title);
        while (sb.length() < totalWidth - 1) {
            sb.append(" ");
        }
        sb.append("|");
        System.out.println(sb);
    }

    private void printRow(List<String> values, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String val = (i < values.size() && values.get(i) != null) ? values.get(i) : "";
            sb.append(String.format(" %-" + widths[i] + "s |", val));
        }
        System.out.println(sb);
    }

    private int[] calculateColumnWidths() {
        int[] widths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            widths[i] = headers.get(i).length();
        }

        for (List<String> row : rows) {
            for (int i = 0; i < Math.min(row.size(), headers.size()); i++) {
                if (row.get(i) != null && row.get(i).length() > widths[i]) {
                    widths[i] = row.get(i).length();
                }
            }
        }
        return widths;
    }
}

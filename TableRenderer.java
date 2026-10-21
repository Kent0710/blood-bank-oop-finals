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

    public TableRenderer withTitle(String title) { 
        this.title = title;
        return this;
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
        if (rows.length != headers.size()){
            throw new IllegalArgumentException("Excpeted " + headers.size() + " columns, but got " + rows.length);
        }
        this.rows.add(Arrays.asList(rows));
        return this;
    }

    public void render() {
        if (headers.isEmpty()){
            System.out.println("(No items to display)");
            return;
        }

        int[] widths = calculateColumnWidths();

        printBorder(widths);

        if(title != null && !title.isEmpty()) {
            printTitle(title, widths);
            printBorder(widths);
        }

        printRow(headers, widths);
        printBorder(widths);

        if(rows.isEmpty()){
            List<String> emptyMessage = new ArrayList<>();
            emptyMessage.add("(No Records Found)");

            for (int i = 1; i < headers.size(); i++){
                emptyMessage.add("");
            }
            printRow(emptyMessage, widths);
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
        System.out.println("Page " + page + " of " + totalPages);

    }

    private int[] calculateColumnWidths() {
        int[] widths = new int[headers.size()];

        for (int column = 0; column < headers.size(); column++) {
            widths[column] = display(headers.get(column)).length();
        }

        for (List<String> row : rows) {
            for (int column = 0; column < row.size(); column++) {
                widths[column] = Math.max(widths[column], display(row.get(column)).length());
            }
        }

        return widths;
    }

    private void printBorder(int[] widths) {
        StringBuilder line = new StringBuilder("+");

        for (int width : widths) {
            line.append("-".repeat(width + 2)).append("+");
        }

        System.out.println(line);
    }

    private void printRow(List<String> values, int[] widths) {
        StringBuilder line = new StringBuilder("|");

        for (int column = 0; column < widths.length; column++) {
            String value = display(values.get(column));
            line.append(" ").append(value);
            line.append(" ".repeat(widths[column] - value.length()));
            line.append(" |");
        }

        System.out.println(line);
    }

    private void printTitle(String title, int[] widths) {
        int tableWidth = 1;

        for (int width : widths) {
            tableWidth += width + 3;
        }

        int padding = Math.max(0, tableWidth - 2 - title.length());
        int leftPadding = padding / 2;
        int rightPadding = padding - leftPadding;

        System.out.println("|"
            + " ".repeat(leftPadding)
            + title
            + " ".repeat(rightPadding)
            + "|");
    }

    private String display(String value) {
        return value == null ? "" : value;
    }

}


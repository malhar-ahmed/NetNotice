package netnotice.common;

import java.util.Locale;

/** Labels come from the activity's wire question, including for late joiners. */
public enum ActivityType {
    QUIZ("Quiz", "answer"), POLL("Poll", "vote");

    public final String title;
    private final String unit;
    ActivityType(String title, String unit) { this.title = title; this.unit = unit; }

    public static ActivityType from(String question) {
        return question != null && question.regionMatches(true, 0, "[quiz]", 0, 6) ? QUIZ : POLL;
    }

    public static String displayQuestion(String question) {
        return from(question) == QUIZ ? question.substring(6).stripLeading() : question;
    }

    public String count(int number) { return number + " " + unit + (number == 1 ? "" : "s"); }

    public String results(boolean closed, String[] choices, int[] counts, int mine) {
        int total = java.util.Arrays.stream(counts).sum();
        StringBuilder text = new StringBuilder(title + " results" + (closed ? " · Final" : " · Live"));
        text.append(" · ").append(count(total));
        for (int i = 0; i < choices.length; i++) {
            int n = i < counts.length ? counts[i] : 0;
            text.append(String.format(Locale.ROOT, "%n%c. %s   %s (%.0f%%)%s", 'A'+i, choices[i], count(n),
                total == 0 ? 0 : 100.0*n/total, i == mine ? "  · Your " + unit : ""));
        }
        return text.toString();
    }
}

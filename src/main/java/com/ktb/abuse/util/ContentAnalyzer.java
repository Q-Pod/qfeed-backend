package com.ktb.abuse.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class ContentAnalyzer {

    private static final Pattern KOREAN_PATTERN = Pattern.compile("[가-힣]");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    private static final Pattern WORD_PATTERN = Pattern.compile("[가-힣a-zA-Z0-9]+");

    private ContentAnalyzer() {
    }

    public static double calculateWhitespaceRatio(String content) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        long whitespaceCount = WHITESPACE_PATTERN.matcher(content).results().count();
        return (double) whitespaceCount / content.length();
    }

    public static int findMaxRepeatCharCount(String content) {
        if (content == null || content.length() < 2) {
            return 0;
        }
        int maxRepeat = 1;
        int currentRepeat = 1;
        char prevChar = content.charAt(0);

        for (int i = 1; i < content.length(); i++) {
            char currentChar = content.charAt(i);
            if (currentChar == prevChar) {
                currentRepeat++;
                maxRepeat = Math.max(maxRepeat, currentRepeat);
            } else {
                currentRepeat = 1;
                prevChar = currentChar;
            }
        }
        return maxRepeat;
    }

    public static double calculateKoreanEnglishRatio(String content) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        long koreanCount = KOREAN_PATTERN.matcher(content).results().count();
        long englishCount = ENGLISH_PATTERN.matcher(content).results().count();
        long validChars = koreanCount + englishCount;

        return (double) validChars / content.length();
    }

    public static double calculateWordRepeatRatio(String content) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        Map<String, Integer> wordCounts = new HashMap<>();
        var matcher = WORD_PATTERN.matcher(content.toLowerCase());
        int totalWords = 0;

        while (matcher.find()) {
            String word = matcher.group();
            if (word.length() >= 2) {
                wordCounts.merge(word, 1, Integer::sum);
                totalWords++;
            }
        }

        if (totalWords == 0) {
            return 0.0;
        }

        long repeatedCount = wordCounts.values().stream()
                .filter(count -> count > 1)
                .mapToInt(Integer::intValue)
                .sum();

        return (double) repeatedCount / totalWords;
    }

    public static int calculateContentLength(String content) {
        if (content == null) {
            return 0;
        }
        return content.trim().length();
    }

    public static boolean hasMinimumLength(String content, int minLength) {
        return calculateContentLength(content) >= minLength;
    }
}

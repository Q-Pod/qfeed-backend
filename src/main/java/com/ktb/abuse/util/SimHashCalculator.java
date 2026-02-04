package com.ktb.abuse.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class SimHashCalculator {

    private static final int HASH_BITS = 64;
    private static final int SHINGLE_SIZE = 3;

    private SimHashCalculator() {
    }

    public static String calculateSha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static String calculateSimHash(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "0".repeat(16);
        }

        List<String> shingles = createShingles(content.toLowerCase(), SHINGLE_SIZE);
        if (shingles.isEmpty()) {
            return "0".repeat(16);
        }

        int[] vector = new int[HASH_BITS];

        for (String shingle : shingles) {
            long hash = hashShingle(shingle);
            for (int i = 0; i < HASH_BITS; i++) {
                if (((hash >> i) & 1) == 1) {
                    vector[i]++;
                } else {
                    vector[i]--;
                }
            }
        }

        long simHash = 0L;
        for (int i = 0; i < HASH_BITS; i++) {
            if (vector[i] > 0) {
                simHash |= (1L << i);
            }
        }

        return String.format("%016x", simHash);
    }

    public static double calculateSimilarity(String simHash1, String simHash2) {
        if (simHash1 == null || simHash2 == null) {
            return 0.0;
        }
        if (simHash1.equals(simHash2)) {
            return 1.0;
        }

        long hash1 = Long.parseUnsignedLong(simHash1, 16);
        long hash2 = Long.parseUnsignedLong(simHash2, 16);

        int hammingDistance = Long.bitCount(hash1 ^ hash2);
        return 1.0 - ((double) hammingDistance / HASH_BITS);
    }

    private static List<String> createShingles(String text, int size) {
        List<String> shingles = new ArrayList<>();
        String normalized = text.replaceAll("\\s+", " ").trim();

        if (normalized.length() < size) {
            if (!normalized.isEmpty()) {
                shingles.add(normalized);
            }
            return shingles;
        }

        for (int i = 0; i <= normalized.length() - size; i++) {
            shingles.add(normalized.substring(i, i + size));
        }
        return shingles;
    }

    private static long hashShingle(String shingle) {
        long hash = 0;
        for (char c : shingle.toCharArray()) {
            hash = hash * 31 + c;
        }
        return hash;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

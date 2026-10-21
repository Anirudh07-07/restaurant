package com.kaveri.util;

public final class PhoneNumberUtils {

    private PhoneNumberUtils() {}

    /**
     * Normalizes phone number to canonical E.164 without leading plus (Meta WhatsApp format).
     * e.g., "+91 82355-20520" -> "918235520520"
     * "8235520520" -> "918235520520" (assuming 10-digit Indian mobile)
     * "08235520520" -> "918235520520"
     */
    public static String normalize(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }
        String digits = rawPhone.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return "";
        }

        // If leading zero (e.g. 08235520520)
        if (digits.startsWith("0")) {
            digits = digits.substring(1);
        }

        // If standard 10 digit Indian number
        if (digits.length() == 10) {
            return "91" + digits;
        }

        return digits;
    }

    /**
     * Checks if two phone numbers match after normalization.
     */
    public static boolean isMatching(String phoneA, String phoneB) {
        if (phoneA == null || phoneB == null) {
            return false;
        }
        String normA = normalize(phoneA);
        String normB = normalize(phoneB);
        if (normA.isEmpty() || normB.isEmpty()) {
            return false;
        }

        if (normA.equals(normB)) {
            return true;
        }

        // Also check last 10 digits for domestic comparison
        if (normA.length() >= 10 && normB.length() >= 10) {
            String tailA = normA.substring(normA.length() - 10);
            String tailB = normB.substring(normB.length() - 10);
            return tailA.equals(tailB);
        }

        return false;
    }
}


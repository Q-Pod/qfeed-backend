package com.ktb.abuse.core;

import lombok.Getter;

@Getter
public class AbuseGuardResult {

    public enum Status {
        ACCEPT,
        ACCEPT_NO_FEEDBACK,
        REJECT
    }

    private final Status status;
    private final String guardName;
    private final String reason;
    private final int qualityScore;

    private AbuseGuardResult(Status status, String guardName, String reason, int qualityScore) {
        this.status = status;
        this.guardName = guardName;
        this.reason = reason;
        this.qualityScore = qualityScore;
    }

    public static AbuseGuardResult accept() {
        return new AbuseGuardResult(Status.ACCEPT, null, null, 100);
    }

    public static AbuseGuardResult accept(int qualityScore) {
        return new AbuseGuardResult(Status.ACCEPT, null, null, qualityScore);
    }

    public static AbuseGuardResult acceptNoFeedback(String guardName, String reason, int qualityScore) {
        return new AbuseGuardResult(Status.ACCEPT_NO_FEEDBACK, guardName, reason, qualityScore);
    }

    public static AbuseGuardResult reject(String guardName, String reason) {
        return new AbuseGuardResult(Status.REJECT, guardName, reason, 0);
    }

    public boolean isAccepted() {
        return status == Status.ACCEPT || status == Status.ACCEPT_NO_FEEDBACK;
    }

    public boolean isRejected() {
        return status == Status.REJECT;
    }

    public boolean shouldProvideFeedback() {
        return status == Status.ACCEPT;
    }
}

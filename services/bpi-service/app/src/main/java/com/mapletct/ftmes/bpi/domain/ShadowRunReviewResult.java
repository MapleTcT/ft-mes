package com.mapletct.ftmes.bpi.domain;

public record ShadowRunReviewResult(
        ShadowRunView run,
        ShadowRunBatchReviewView review) {
}

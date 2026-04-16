package com.piotrgala.marketsnapshot.service;

public enum SnapshotDataSource {
    LIVE("live"),
    CACHED("cached"),
    MIXED("mixed");

    private final String label;

    SnapshotDataSource(String label) {
        this.label = label;
    }

    public SnapshotDataSource combine(SnapshotDataSource other) {
        if (this == other) {
            return this;
        }
        return MIXED;
    }

    public String label() {
        return label;
    }
}

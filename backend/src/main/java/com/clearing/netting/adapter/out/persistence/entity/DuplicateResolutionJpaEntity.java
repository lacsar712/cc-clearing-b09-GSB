package com.clearing.netting.adapter.out.persistence.entity;

import com.clearing.netting.domain.model.DuplicateResolutionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "duplicate_group_resolutions")
public class DuplicateResolutionJpaEntity {

    @Id
    @Column(length = 512)
    private String groupKey;

    @Column(nullable = false)
    private int openCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DuplicateResolutionType resolutionType;

    @Column(length = 512)
    private String reason;

    @Column(length = 64)
    private String resolvedBy;

    @Column(nullable = false)
    private Instant resolvedAt;

    public String getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(String groupKey) {
        this.groupKey = groupKey;
    }

    public int getOpenCount() {
        return openCount;
    }

    public void setOpenCount(int openCount) {
        this.openCount = openCount;
    }

    public DuplicateResolutionType getResolutionType() {
        return resolutionType;
    }

    public void setResolutionType(DuplicateResolutionType resolutionType) {
        this.resolutionType = resolutionType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

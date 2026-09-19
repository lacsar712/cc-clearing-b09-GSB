package com.clearing.netting.adapter.out.persistence;

import com.clearing.netting.adapter.out.persistence.entity.DuplicateResolutionJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.MemberJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.NetPositionJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.NettingRunJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.ObligationJpaEntity;
import com.clearing.netting.adapter.out.persistence.entity.UserJpaEntity;
import com.clearing.netting.domain.model.DuplicateGroupResolution;
import com.clearing.netting.domain.model.Member;
import com.clearing.netting.domain.model.NetPosition;
import com.clearing.netting.domain.model.NettingRun;
import com.clearing.netting.domain.model.TradeObligation;
import com.clearing.netting.domain.model.UserAccount;

final class PersistenceMapper {

    private PersistenceMapper() {
    }

    static Member toDomain(MemberJpaEntity e) {
        return new Member(e.getMemberId(), e.getName(), e.getStatus());
    }

    static MemberJpaEntity toEntity(Member m) {
        MemberJpaEntity e = new MemberJpaEntity();
        e.setMemberId(m.getMemberId());
        e.setName(m.getName());
        e.setStatus(m.getStatus());
        return e;
    }

    static TradeObligation toDomain(ObligationJpaEntity e) {
        return new TradeObligation(
                e.getObligationId(),
                e.getPayerMemberId(),
                e.getPayeeMemberId(),
                e.getCurrency(),
                e.getAmount(),
                e.getTradeDate(),
                e.getSettleDate(),
                e.getStatus(),
                e.getNettingRunId(),
                e.getCreatedAt(),
                e.getCancelReason());
    }

    static ObligationJpaEntity toEntity(TradeObligation o) {
        ObligationJpaEntity e = new ObligationJpaEntity();
        e.setObligationId(o.getObligationId());
        e.setPayerMemberId(o.getPayerMemberId());
        e.setPayeeMemberId(o.getPayeeMemberId());
        e.setCurrency(o.getCurrency());
        e.setAmount(o.getAmount());
        e.setTradeDate(o.getTradeDate());
        e.setSettleDate(o.getSettleDate());
        e.setStatus(o.getStatus());
        e.setNettingRunId(o.getNettingRunId());
        e.setCreatedAt(o.getCreatedAt());
        e.setCancelReason(o.getCancelReason());
        return e;
    }

    static DuplicateGroupResolution toDomain(DuplicateResolutionJpaEntity e) {
        return new DuplicateGroupResolution(
                e.getGroupKey(),
                e.getOpenCount(),
                e.getResolutionType(),
                e.getReason(),
                e.getResolvedBy(),
                e.getResolvedAt());
    }

    static DuplicateResolutionJpaEntity toEntity(DuplicateGroupResolution r) {
        DuplicateResolutionJpaEntity e = new DuplicateResolutionJpaEntity();
        e.setGroupKey(r.getGroupKey());
        e.setOpenCount(r.getOpenCount());
        e.setResolutionType(r.getResolutionType());
        e.setReason(r.getReason());
        e.setResolvedBy(r.getResolvedBy());
        e.setResolvedAt(r.getResolvedAt());
        return e;
    }

    static NettingRun toDomain(NettingRunJpaEntity e) {
        return new NettingRun(
                e.getRunId(),
                e.getSettleDate(),
                e.getCurrency(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getFailureReason());
    }

    static NettingRunJpaEntity toEntity(NettingRun r) {
        NettingRunJpaEntity e = new NettingRunJpaEntity();
        e.setRunId(r.getRunId());
        e.setSettleDate(r.getSettleDate());
        e.setCurrency(r.getCurrency());
        e.setStatus(r.getStatus());
        e.setCreatedAt(r.getCreatedAt());
        e.setFailureReason(r.getFailureReason());
        return e;
    }

    static NetPosition toDomain(NetPositionJpaEntity e) {
        return new NetPosition(
                e.getPositionId(),
                e.getRunId(),
                e.getMemberId(),
                e.getCurrency(),
                e.getNetAmount());
    }

    static NetPositionJpaEntity toEntity(NetPosition p) {
        NetPositionJpaEntity e = new NetPositionJpaEntity();
        e.setPositionId(p.getPositionId());
        e.setRunId(p.getRunId());
        e.setMemberId(p.getMemberId());
        e.setCurrency(p.getCurrency());
        e.setNetAmount(p.getNetAmount());
        return e;
    }

    static UserAccount toDomain(UserJpaEntity e) {
        return new UserAccount(e.getUserId(), e.getUsername(), e.getPasswordHash(), e.getRole());
    }

    static UserJpaEntity toEntity(UserAccount u) {
        UserJpaEntity e = new UserJpaEntity();
        e.setUserId(u.getUserId());
        e.setUsername(u.getUsername());
        e.setPasswordHash(u.getPasswordHash());
        e.setRole(u.getRole());
        return e;
    }
}

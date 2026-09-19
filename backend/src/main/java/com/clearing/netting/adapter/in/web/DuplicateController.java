package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.auth.AuthContext;
import com.clearing.netting.adapter.in.web.auth.AuthUser;
import com.clearing.netting.application.DuplicateDetectionApplicationService;
import com.clearing.netting.domain.model.DuplicateGroup;
import com.clearing.netting.domain.model.DuplicateGroupResolution;
import com.clearing.netting.domain.model.DuplicateResolutionType;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/duplicates")
public class DuplicateController {

    private final DuplicateDetectionApplicationService duplicateService;

    public DuplicateController(DuplicateDetectionApplicationService duplicateService) {
        this.duplicateService = duplicateService;
    }

    @GetMapping
    public List<DuplicateGroupResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settleDate,
            @RequestParam(required = false) String currency) {
        AuthContext.require();
        return duplicateService.listGroups(settleDate, currency).stream()
                .map(DuplicateGroupResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/cancel")
    public DuplicateGroupResponse cancel(@Valid @RequestBody CancelDuplicateRequest request) {
        AuthContext.requireOperator();
        AuthUser user = AuthContext.require();
        return DuplicateGroupResponse.from(duplicateService.cancelDuplicate(
                request.groupKey(), request.obligationId(), request.reason(), user.username()));
    }

    @PostMapping("/review")
    public DuplicateGroupResponse review(@Valid @RequestBody ReviewDuplicateRequest request) {
        AuthContext.requireOperator();
        AuthUser user = AuthContext.require();
        return DuplicateGroupResponse.from(duplicateService.reviewGroup(
                request.groupKey(), request.note(), user.username()));
    }

    public record CancelDuplicateRequest(
            @NotBlank String groupKey,
            @NotBlank String obligationId,
            @NotBlank String reason) {
    }

    public record ReviewDuplicateRequest(
            @NotBlank String groupKey,
            String note) {
    }

    public record DuplicateObligationBrief(
            String obligationId,
            BigDecimal amount,
            ObligationStatus status,
            Instant createdAt) {
        static DuplicateObligationBrief from(TradeObligation o) {
            return new DuplicateObligationBrief(
                    o.getObligationId(),
                    o.getAmount(),
                    o.getStatus(),
                    o.getCreatedAt());
        }
    }

    public record DuplicateGroupResponse(
            String groupKey,
            LocalDate settleDate,
            String currency,
            String payerMemberId,
            String payeeMemberId,
            BigDecimal amount,
            List<DuplicateObligationBrief> obligations,
            DuplicateResolutionType resolutionType,
            String resolutionReason,
            String resolvedBy,
            Instant resolvedAt,
            boolean pending) {
        static DuplicateGroupResponse from(DuplicateGroup g) {
            DuplicateGroupResolution r = g.resolution();
            return new DuplicateGroupResponse(
                    g.groupKey(),
                    g.settleDate(),
                    g.currency(),
                    g.payerMemberId(),
                    g.payeeMemberId(),
                    g.amount(),
                    g.obligations().stream().map(DuplicateObligationBrief::from).collect(Collectors.toList()),
                    r == null ? null : r.getResolutionType(),
                    r == null ? null : r.getReason(),
                    r == null ? null : r.getResolvedBy(),
                    r == null ? null : r.getResolvedAt(),
                    g.pending());
        }
    }
}

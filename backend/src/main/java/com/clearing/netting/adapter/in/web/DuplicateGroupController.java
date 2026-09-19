package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.auth.AuthContext;
import com.clearing.netting.adapter.in.web.auth.AuthUser;
import com.clearing.netting.application.DuplicateDetectionApplicationService;
import com.clearing.netting.domain.model.DuplicateGroup;
import com.clearing.netting.domain.model.DuplicateGroupStatus;
import com.clearing.netting.domain.model.DuplicateKey;
import com.clearing.netting.domain.model.ObligationStatus;
import com.clearing.netting.domain.model.TradeObligation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/duplicate-groups")
public class DuplicateGroupController {

    private final DuplicateDetectionApplicationService duplicateService;

    public DuplicateGroupController(DuplicateDetectionApplicationService duplicateService) {
        this.duplicateService = duplicateService;
    }

    @GetMapping
    public List<GroupResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settleDate,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) DuplicateGroupStatus status) {
        AuthContext.require();
        return duplicateService.listGroups(settleDate, currency, status).stream()
                .map(GroupResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping("/review")
    public GroupResponse review(@Valid @RequestBody ReviewRequest request) {
        AuthContext.requireOperator();
        AuthUser user = AuthContext.require();
        DuplicateKey key = new DuplicateKey(
                request.settleDate(),
                request.currency(),
                request.payerMemberId(),
                request.payeeMemberId(),
                request.amount());
        return GroupResponse.from(duplicateService.review(key, user.username()));
    }

    public record ReviewRequest(
            @NotNull LocalDate settleDate,
            @NotBlank String currency,
            @NotBlank String payerMemberId,
            @NotBlank String payeeMemberId,
            @NotNull @DecimalMin("0.00000001") BigDecimal amount) {
    }

    public record GroupResponse(
            String groupId,
            LocalDate settleDate,
            String currency,
            String payerMemberId,
            String payeeMemberId,
            BigDecimal amount,
            int obligationCount,
            DuplicateGroupStatus status,
            Instant reviewedAt,
            String reviewedBy,
            List<ObligationItem> obligations) {
        static GroupResponse from(DuplicateGroup g) {
            return new GroupResponse(
                    g.getGroupId(),
                    g.getKey().settleDate(),
                    g.getKey().currency(),
                    g.getKey().payerMemberId(),
                    g.getKey().payeeMemberId(),
                    g.getKey().amount(),
                    g.getObligations().size(),
                    g.getStatus(),
                    g.getReviewedAt(),
                    g.getReviewedBy(),
                    g.getObligations().stream().map(ObligationItem::from).collect(Collectors.toList()));
        }
    }

    public record ObligationItem(
            String obligationId,
            BigDecimal amount,
            Instant createdAt,
            ObligationStatus status) {
        static ObligationItem from(TradeObligation o) {
            return new ObligationItem(o.getObligationId(), o.getAmount(), o.getCreatedAt(), o.getStatus());
        }
    }
}

package com.forex.transaction.service;

import com.forex.transaction.document.Transaction;
import com.forex.transaction.document.Transaction.Status;
import com.forex.transaction.dto.TransactionDtos.*;
import com.forex.transaction.repository.TransactionRepository;
import com.forex.transaction.security.ForexUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repo;
    private final MongoTemplate          mongo;  // for dynamic query building

    // ── Convert & record ───────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse convert(
            ConvertRequest request,
            ForexUserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        request.validate();

        BigDecimal converted = request.amount()
                .multiply(request.exchangeRate())
                .setScale(6, RoundingMode.HALF_UP);

        Transaction tx = Transaction.builder()
                .userId(principal.getUserId())
                .userEmail(principal.getEmail())
                .fromCurrency(request.fromCurrency())
                .toCurrency(request.toCurrency())
                .sourceAmount(request.amount())
                .convertedAmount(converted)
                .exchangeRate(request.exchangeRate())
                .status(Status.COMPLETED)
                .ipAddress(resolveIp(httpRequest))
                .notes(request.notes())
                .build();

        Transaction saved = repo.save(tx);
        log.info("Transaction {} saved — userId={} {}→{} amount={}",
                saved.getId(), principal.getUserId(),
                request.fromCurrency(), request.toCurrency(), request.amount());

        return toResponse(saved);
    }

    // ── Get single (user must own it) ──────────────────────────────────────────

    public TransactionResponse getById(String id, ForexUserPrincipal principal) {
        return repo.findByIdAndUserId(id, principal.getUserId())
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException(
                        "Transaction not found: " + id));
    }

    // ── Filtered history ───────────────────────────────────────────────────────

    public PagedResponse<TransactionResponse> getHistory(
            TransactionFilter filter,
            ForexUserPrincipal principal
    ) {
        // Build dynamic query with MongoTemplate + Criteria
        // This is the MongoDB equivalent of a JPQL dynamic WHERE clause
        Criteria criteria = Criteria.where("userId").is(principal.getUserId());

        if (filter.from()   != null) criteria = criteria.and("fromCurrency").is(filter.from());
        if (filter.to()     != null) criteria = criteria.and("toCurrency").is(filter.to());
        if (filter.status() != null) criteria = criteria.and("status").is(filter.status());
        if (filter.after()  != null) criteria = criteria.and("createdAt").gte(filter.after());
        if (filter.before() != null) criteria = criteria.and("createdAt").lte(filter.before());

        Query query = new Query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "createdAt"));

        long total = mongo.count(query, Transaction.class);

        query.with(PageRequest.of(filter.page(), filter.size()));
        List<Transaction> results = mongo.find(query, Transaction.class);

        Page<Transaction> page = new PageImpl<>(
                results,
                PageRequest.of(filter.page(), filter.size()),
                total
        );

        return toPagedResponse(page);
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    public UserStatsResponse getStats(ForexUserPrincipal principal) {
        Long userId = principal.getUserId();

        long count = repo.countByUserId(userId);

        BigDecimal volume = repo.sumSourceAmountByUserId(userId)
                .map(TransactionRepository.BigDecimalWrapper::getTotal)
                .orElse(BigDecimal.ZERO);

        String topPair = repo.findTopPairByUserId(userId)
                .map(p -> p.get_id().getFrom() + "/" + p.get_id().getTo())
                .orElse("N/A");

        var lastAt = repo.findLastTransactionAt(userId)
                .map(TransactionRepository.InstantWrapper::getCreatedAt)
                .orElse(null);

        return new UserStatsResponse(
                userId,
                principal.getEmail(),
                count,
                volume,
                topPair,
                lastAt
        );
    }

    // ── Admin: all transactions paginated ─────────────────────────────────────

    public PagedResponse<TransactionResponse> getAll(int page, int size) {
        Page<Transaction> result = repo.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, Math.min(size, 100)));
        return toPagedResponse(result);
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getUserId(),
                t.getUserEmail(),
                t.getFromCurrency(),
                t.getToCurrency(),
                t.getSourceAmount(),
                t.getConvertedAmount(),
                t.getExchangeRate(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getIpAddress(),
                t.getNotes()
        );
    }

    private PagedResponse<TransactionResponse> toPagedResponse(Page<Transaction> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim()
                                   : request.getRemoteAddr();
    }
}

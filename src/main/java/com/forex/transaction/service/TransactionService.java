package com.forex.transaction.service;

import com.forex.transaction.document.Transaction;
import com.forex.transaction.document.Transaction.Status;
import com.forex.transaction.dto.TransactionDtos.*;
import com.forex.transaction.repository.TransactionRepository;
import com.forex.transaction.security.ForexUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repo;
    private final MongoTemplate         mongo;

    // ── Resolve userId from either JWT principal or web session ───────────────

    /**
     * Derives a stable Long userId from whatever principal is in the SecurityContext.
     *
     * - JWT API call:  ForexUserPrincipal already carries a Long userId.
     * - Web UI call:   UserDetails.getUsername() is the Mongo username string;
     *                  we hash it to a stable Long so history queries are consistent.
     */
    public static Long resolveUserId(Authentication auth) {
        if (auth == null) throw new IllegalStateException("Not authenticated");

        Object principal = auth.getPrincipal();

        if (principal instanceof ForexUserPrincipal fp) {
            return fp.getUserId();
        }

        if (principal instanceof UserDetails ud) {
            // Deterministic hash of username → stable Long (same user always gets same id)
            return (long) ud.getUsername().hashCode() & 0xFFFFFFFFL;
        }

        throw new IllegalStateException("Unknown principal type: " + principal.getClass());
    }

    public static String resolveEmail(Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof ForexUserPrincipal fp) return fp.getEmail();
        if (principal instanceof UserDetails ud)       return ud.getUsername();
        return "unknown";
    }

    // ── Convert & record ───────────────────────────────────────────────────────

    @Transactional
    public TransactionResponse convert(
            ConvertRequest request,
            Authentication auth,
            HttpServletRequest httpRequest
    ) {
        request.validate();

        Long   userId    = resolveUserId(auth);
        String userEmail = resolveEmail(auth);

        BigDecimal converted = request.amount()
                .multiply(request.exchangeRate())
                .setScale(6, RoundingMode.HALF_UP);

        Transaction tx = Transaction.builder()
                .userId(userId)
                .userEmail(userEmail)
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
        log.info("Transaction {} — userId={} {}→{} amount={}",
                saved.getId(), userId,
                request.fromCurrency(), request.toCurrency(), request.amount());

        return toResponse(saved);
    }

    // ── Get single ─────────────────────────────────────────────────────────────

    public TransactionResponse getById(String id, Authentication auth) {
        Long userId = resolveUserId(auth);
        return repo.findByIdAndUserId(id, userId)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + id));
    }

    // ── Filtered history ───────────────────────────────────────────────────────

    public PagedResponse<TransactionResponse> getHistory(
            TransactionFilter filter,
            Authentication auth
    ) {
        Long userId = resolveUserId(auth);

        Criteria criteria = Criteria.where("userId").is(userId);
        if (filter.from()   != null) criteria = criteria.and("fromCurrency").is(filter.from());
        if (filter.to()     != null) criteria = criteria.and("toCurrency").is(filter.to());
        if (filter.status() != null) criteria = criteria.and("status").is(filter.status());
        if (filter.after()  != null) criteria = criteria.and("createdAt").gte(filter.after());
        if (filter.before() != null) criteria = criteria.and("createdAt").lte(filter.before());

        Query query = new Query(criteria).with(Sort.by(Sort.Direction.DESC, "createdAt"));
        long total = mongo.count(query, Transaction.class);
        query.with(PageRequest.of(filter.page(), filter.size()));
        List<Transaction> results = mongo.find(query, Transaction.class);

        Page<Transaction> page = new PageImpl<>(
                results, PageRequest.of(filter.page(), filter.size()), total);
        return toPagedResponse(page);
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    public UserStatsResponse getStats(Authentication auth) {
        Long   userId    = resolveUserId(auth);
        String userEmail = resolveEmail(auth);

        long count = repo.countByUserId(userId);

        // Use MongoTemplate aggregation to avoid Spring Data @Aggregation projection bug
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("userId").is(userId)
                                .and("status").is(Status.COMPLETED.name())
                ),
                Aggregation.group().sum("sourceAmount").as("total")
        );
        AggregationResults<Document> results =
                mongo.aggregate(agg, "transactions", Document.class);

        BigDecimal volume = Optional.ofNullable(results.getUniqueMappedResult())
                .map(doc -> doc.get("total"))
                .map(v -> new BigDecimal(v.toString()))
                .orElse(BigDecimal.ZERO);

        String topPair = repo.findTopPairByUserId(userId)
                .map(p -> p.get_id().getFrom() + "/" + p.get_id().getTo())
                .orElse("N/A");

        var lastAt = repo.findLastTransactionAt(userId)
                .map(TransactionRepository.InstantWrapper::getCreatedAt)
                .orElse(null);

        return new UserStatsResponse(userId, userEmail, count, volume, topPair, lastAt);
    }

    // ── Admin ──────────────────────────────────────────────────────────────────

    public PagedResponse<TransactionResponse> getAll(int page, int size) {
        Page<Transaction> result = repo.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, Math.min(size, 100)));
        return toPagedResponse(result);
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getUserId(), t.getUserEmail(),
                t.getFromCurrency(), t.getToCurrency(),
                t.getSourceAmount(), t.getConvertedAmount(), t.getExchangeRate(),
                t.getStatus(), t.getCreatedAt(), t.getIpAddress(), t.getNotes()
        );
    }

    private PagedResponse<TransactionResponse> toPagedResponse(Page<Transaction> page) {
        return new PagedResponse<>(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(),
                page.isFirst(), page.isLast()
        );
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}

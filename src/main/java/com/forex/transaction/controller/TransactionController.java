package com.forex.transaction.controller;

import com.forex.transaction.document.Transaction.Status;
import com.forex.transaction.dto.TransactionDtos.*;
import com.forex.transaction.security.ForexUserPrincipal;
import com.forex.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Forex conversion history and stats")
@SecurityRequirement(name = "bearerAuth")   // Swagger shows the lock icon
public class TransactionController {

    private final TransactionService service;

    // ── POST /api/v1/transactions/convert ──────────────────────────────────────

    @PostMapping("/convert")
    @Operation(
        summary = "Record a conversion",
        description = "Converts an amount and persists the transaction to MongoDB. "
                    + "The exchange rate is provided by the caller (Exchange Rate Service)."
    )
    public ResponseEntity<TransactionResponse> convert(
            @Valid @RequestBody ConvertRequest request,
            @AuthenticationPrincipal ForexUserPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.convert(request, principal, httpRequest));
    }

    // ── GET /api/v1/transactions/{id} ──────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get one transaction by ID", description = "User can only fetch their own")
    public ResponseEntity<TransactionResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal ForexUserPrincipal principal
    ) {
        return ResponseEntity.ok(service.getById(id, principal));
    }

    // ── GET /api/v1/transactions/history ──────────────────────────────────────

    @GetMapping("/history")
    @Operation(
        summary = "Paginated transaction history",
        description = "Filter by currency pair, status, and date range. All params optional."
    )
    public ResponseEntity<PagedResponse<TransactionResponse>> history(
            @AuthenticationPrincipal ForexUserPrincipal principal,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        TransactionFilter filter = new TransactionFilter(from, to, status, after, before, page, size);
        return ResponseEntity.ok(service.getHistory(filter, principal));
    }

    // ── GET /api/v1/transactions/stats ─────────────────────────────────────────

    @GetMapping("/stats")
    @Operation(
        summary = "User stats",
        description = "Total count, volume, top currency pair, and last transaction time"
    )
    public ResponseEntity<UserStatsResponse> stats(
            @AuthenticationPrincipal ForexUserPrincipal principal
    ) {
        return ResponseEntity.ok(service.getStats(principal));
    }

    // ── GET /api/v1/transactions/admin/all (ADMIN only) ────────────────────────

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: all transactions", description = "Requires ADMIN role")
    public ResponseEntity<PagedResponse<TransactionResponse>> adminAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.getAll(page, size));
    }
}

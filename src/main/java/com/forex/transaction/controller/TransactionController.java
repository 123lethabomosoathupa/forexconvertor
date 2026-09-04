package com.forex.transaction.controller;

import com.forex.transaction.document.Transaction.Status;
import com.forex.transaction.dto.TransactionDtos.*;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Forex conversion history and stats")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService service;

    @PostMapping("/convert")
    @Operation(summary = "Record a conversion")
    public ResponseEntity<TransactionResponse> convert(
            @Valid @RequestBody ConvertRequest request,
            Authentication auth,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.convert(request, auth, httpRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one transaction by ID")
    public ResponseEntity<TransactionResponse> getById(
            @PathVariable String id,
            Authentication auth
    ) {
        return ResponseEntity.ok(service.getById(id, auth));
    }

    @GetMapping("/history")
    @Operation(summary = "Paginated transaction history")
    public ResponseEntity<PagedResponse<TransactionResponse>> history(
            Authentication auth,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant after,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        TransactionFilter filter = new TransactionFilter(from, to, status, after, before, page, size);
        return ResponseEntity.ok(service.getHistory(filter, auth));
    }

    @GetMapping("/stats")
    @Operation(summary = "User stats summary")
    public ResponseEntity<UserStatsResponse> stats(Authentication auth) {
        return ResponseEntity.ok(service.getStats(auth));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Admin: all transactions")
    public ResponseEntity<PagedResponse<TransactionResponse>> adminAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(service.getAll(page, size));
    }
}

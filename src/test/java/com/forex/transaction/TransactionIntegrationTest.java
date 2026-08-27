package com.forex.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forex.transaction.document.Transaction;
import com.forex.transaction.dto.TransactionDtos.ConvertRequest;
import com.forex.transaction.repository.TransactionRepository;
import com.forex.transaction.security.ForexUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests using Flapdoodle embedded MongoDB.
 * No Mongo installation needed — starts in-process.
 *
 * JWT filter is bypassed by injecting authentication directly via
 * SecurityMockMvcRequestPostProcessors.authentication().
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {

    @Autowired MockMvc          mockMvc;
    @Autowired ObjectMapper     mapper;
    @Autowired TransactionRepository repo;

    private static final String BASE = "/api/v1/transactions";

    // Reusable mock principal
    private final ForexUserPrincipal USER = new ForexUserPrincipal(42L, "sipho@forex.local");

    private UsernamePasswordAuthenticationToken auth(ForexUserPrincipal p) {
        return new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void cleanUp() {
        repo.deleteAll();
    }

    // ── Convert ────────────────────────────────────────────────────────────────

    @Test
    void convert_shouldReturn201AndPersist() throws Exception {
        ConvertRequest req = new ConvertRequest(
                "USD", "ZAR", new BigDecimal("1000.00"),
                new BigDecimal("18.62"), "Test conversion");

        mockMvc.perform(post(BASE + "/convert")
                        .with(authentication(auth(USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.toCurrency").value("ZAR"))
                .andExpect(jsonPath("$.convertedAmount").value(18620.00))
                .andExpect(jsonPath("$.userId").value(42))
                .andExpect(jsonPath("$.userEmail").value("sipho@forex.local"));

        assertThat(repo.count()).isEqualTo(1);
    }

    @Test
    void convert_sameCurrency_shouldReturn400() throws Exception {
        ConvertRequest req = new ConvertRequest(
                "USD", "USD", new BigDecimal("100"), new BigDecimal("1.0"), null);

        mockMvc.perform(post(BASE + "/convert")
                        .with(authentication(auth(USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void convert_invalidCurrencyCode_shouldReturn400() throws Exception {
        ConvertRequest req = new ConvertRequest(
                "us", "ZAR", new BigDecimal("100"), new BigDecimal("18.62"), null);

        mockMvc.perform(post(BASE + "/convert")
                        .with(authentication(auth(USER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // ── Get by ID ──────────────────────────────────────────────────────────────

    @Test
    void getById_ownTransaction_shouldReturn200() throws Exception {
        Transaction saved = repo.save(Transaction.builder()
                .userId(42L).userEmail("sipho@forex.local")
                .fromCurrency("USD").toCurrency("ZAR")
                .sourceAmount(new BigDecimal("500"))
                .convertedAmount(new BigDecimal("9310"))
                .exchangeRate(new BigDecimal("18.62"))
                .build());

        mockMvc.perform(get(BASE + "/" + saved.getId())
                        .with(authentication(auth(USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()));
    }

    @Test
    void getById_otherUsersTransaction_shouldReturn404() throws Exception {
        ForexUserPrincipal otherUser = new ForexUserPrincipal(99L, "other@forex.local");
        Transaction saved = repo.save(Transaction.builder()
                .userId(42L).userEmail("sipho@forex.local")
                .fromCurrency("USD").toCurrency("ZAR")
                .sourceAmount(new BigDecimal("500"))
                .convertedAmount(new BigDecimal("9310"))
                .exchangeRate(new BigDecimal("18.62"))
                .build());

        mockMvc.perform(get(BASE + "/" + saved.getId())
                        .with(authentication(auth(otherUser))))
                .andExpect(status().isNotFound());
    }

    // ── History ────────────────────────────────────────────────────────────────

    @Test
    void history_shouldReturnOnlyOwnTransactions() throws Exception {
        // 2 for USER, 1 for someone else
        repo.saveAll(List.of(
                Transaction.builder().userId(42L).userEmail("sipho@forex.local")
                        .fromCurrency("USD").toCurrency("ZAR")
                        .sourceAmount(BigDecimal.TEN).convertedAmount(BigDecimal.TEN)
                        .exchangeRate(BigDecimal.ONE).build(),
                Transaction.builder().userId(42L).userEmail("sipho@forex.local")
                        .fromCurrency("GBP").toCurrency("ZAR")
                        .sourceAmount(BigDecimal.TEN).convertedAmount(BigDecimal.TEN)
                        .exchangeRate(BigDecimal.ONE).build(),
                Transaction.builder().userId(99L).userEmail("other@forex.local")
                        .fromCurrency("EUR").toCurrency("USD")
                        .sourceAmount(BigDecimal.TEN).convertedAmount(BigDecimal.TEN)
                        .exchangeRate(BigDecimal.ONE).build()
        ));

        mockMvc.perform(get(BASE + "/history")
                        .with(authentication(auth(USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    @Test
    void stats_shouldReturnCorrectCount() throws Exception {
        repo.saveAll(List.of(
                Transaction.builder().userId(42L).userEmail("sipho@forex.local")
                        .fromCurrency("USD").toCurrency("ZAR")
                        .sourceAmount(new BigDecimal("1000")).convertedAmount(new BigDecimal("18620"))
                        .exchangeRate(new BigDecimal("18.62")).build(),
                Transaction.builder().userId(42L).userEmail("sipho@forex.local")
                        .fromCurrency("USD").toCurrency("ZAR")
                        .sourceAmount(new BigDecimal("500")).convertedAmount(new BigDecimal("9310"))
                        .exchangeRate(new BigDecimal("18.62")).build()
        ));

        mockMvc.perform(get(BASE + "/stats")
                        .with(authentication(auth(USER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(2))
                .andExpect(jsonPath("$.topPair").value("USD/ZAR"));
    }

    // ── No auth ────────────────────────────────────────────────────────────────

    @Test
    void convert_withNoAuth_shouldReturn403() throws Exception {
        ConvertRequest req = new ConvertRequest(
                "USD", "ZAR", new BigDecimal("100"), new BigDecimal("18.62"), null);

        mockMvc.perform(post(BASE + "/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}

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

@SpringBootTest
@AutoConfigureMockMvc
class TransactionIntegrationTest {

    @Autowired MockMvc          mockMvc;
    @Autowired ObjectMapper     mapper;
    @Autowired TransactionRepository repo;

    private static final String BASE = "/api/v1/transactions";

    private final ForexUserPrincipal USER = new ForexUserPrincipal(42L, "sipho@forex.local");

    private UsernamePasswordAuthenticationToken auth(ForexUserPrincipal p) {
        return new UsernamePasswordAuthenticationToken(
                p, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @BeforeEach
    void cleanUp() {
        repo.deleteAll();
    }

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
                .andExpect(jsonPath("$.convertedAmount").value(18620.0))
                .andExpect(jsonPath("$.userId").value(42));

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
    void history_shouldReturnOnlyOwnTransactions() throws Exception {
        repo.saveAll(List.of(
                Transaction.builder().userId(42L).userEmail("sipho@forex.local")
                        .fromCurrency("USD").toCurrency("ZAR")
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
                .andExpect(jsonPath("$.totalElements").value(1));
    }

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

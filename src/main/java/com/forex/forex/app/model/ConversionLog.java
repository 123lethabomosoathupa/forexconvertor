package com.forex.forexapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "conversion_logs")
public class ConversionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "from_currency", nullable = false, length = 10)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 10)
    private String toCurrency;

    @Column(nullable = false)
    private double amount;

    @Column(nullable = false)
    private double rate;

    @Column(name = "converted_amount", nullable = false)
    private double convertedAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ConversionLog() {}

    public ConversionLog(AppUser user, String fromCurrency, String toCurrency,
                         double amount, double rate, double convertedAmount) {
        this.user            = user;
        this.fromCurrency    = fromCurrency;
        this.toCurrency      = toCurrency;
        this.amount          = amount;
        this.rate            = rate;
        this.convertedAmount = convertedAmount;
        this.createdAt       = LocalDateTime.now();
    }

    public Long          getId()              { return id; }
    public AppUser       getUser()            { return user; }
    public String        getFromCurrency()    { return fromCurrency; }
    public String        getToCurrency()      { return toCurrency; }
    public double        getAmount()          { return amount; }
    public double        getRate()            { return rate; }
    public double        getConvertedAmount() { return convertedAmount; }
    public LocalDateTime getCreatedAt()       { return createdAt; }

    public String getFormattedTime() {
        return createdAt.format(DateTimeFormatter.ofPattern("dd MMM HH:mm"));
    }
}
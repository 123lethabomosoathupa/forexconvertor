package com.forex.forexapp.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rate_alerts")
public class RateAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "from_currency", nullable = false, length = 10)
    private String fromCurrency;

    @Column(name = "to_currency", nullable = false, length = 10)
    private String toCurrency;

    @Column(name = "target_rate", nullable = false)
    private double targetRate;

    @Column(nullable = false, length = 10)
    private String direction;

    @Column(nullable = false)
    private boolean triggered = false;

    public RateAlert() {}

    public RateAlert(AppUser user, String fromCurrency, String toCurrency,
                     double targetRate, String direction) {
        this.user         = user;
        this.fromCurrency = fromCurrency;
        this.toCurrency   = toCurrency;
        this.targetRate   = targetRate;
        this.direction    = direction;
    }

    public Long    getId()           { return id; }
    public AppUser getUser()         { return user; }
    public String  getFromCurrency() { return fromCurrency; }
    public String  getToCurrency()   { return toCurrency; }
    public double  getTargetRate()   { return targetRate; }
    public String  getDirection()    { return direction; }
    public boolean isTriggered()     { return triggered; }

    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }
    public void setToCurrency(String toCurrency)     { this.toCurrency = toCurrency; }
    public void setTargetRate(double targetRate)      { this.targetRate = targetRate; }
    public void setDirection(String direction)        { this.direction = direction; }
    public void setTriggered(boolean triggered)       { this.triggered = triggered; }
}
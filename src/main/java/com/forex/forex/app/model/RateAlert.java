package com.forex.forexapp.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.index.Indexed;

@Document(collection = "rate_alerts")
public class RateAlert {

    @Id
    private String id;          // MongoDB ObjectId — String, not Long

    // Store user ID and email directly instead of a @ManyToOne reference
    // MongoDB has no foreign keys — embed the identity from the user object
    @Indexed
    private String userId;
    private String userEmail;

    @Field("from_currency")
    private String fromCurrency;

    @Field("to_currency")
    private String toCurrency;

    @Field("target_rate")
    private double targetRate;

    private String direction;

    private boolean triggered = false;

    public RateAlert() {}

    public RateAlert(AppUser user, String fromCurrency, String toCurrency,
                     double targetRate, String direction) {
        this.userId       = user.getId();
        this.userEmail    = user.getEmail();
        this.fromCurrency = fromCurrency;
        this.toCurrency   = toCurrency;
        this.targetRate   = targetRate;
        this.direction    = direction;
    }

    public String  getId()           { return id; }
    public String  getUserId()       { return userId; }
    public String  getUserEmail()    { return userEmail; }
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
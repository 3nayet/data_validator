package com.data;

import java.math.BigDecimal;

public class ValidationRes {
    private boolean zero;
    private boolean invalid;
    private BigDecimal value;

    public boolean isZero () {
        return zero;
    }

    public void setZero (boolean zero) {
        this.zero = zero;
    }

    public boolean isInvalid () {
        return invalid;
    }

    public void setInvalid (boolean invalid) {
        this.invalid = invalid;
    }

    public BigDecimal getValue () {
        return value;
    }

    public void setValue (BigDecimal value) {
        this.value = value;
    }
}

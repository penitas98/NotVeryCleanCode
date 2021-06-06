package domain;

import java.math.BigDecimal;

public class BatchTotal {

    private String transactionSign;
    private BigDecimal creditValue;
    private BigDecimal debitValue;

    public BatchTotal() {
        creditValue = BigDecimal.ZERO;
        debitValue = BigDecimal.ZERO;
    }

    public void setCreditValue(BigDecimal creditValue) {
        this.creditValue = creditValue;
    }

    public BigDecimal getCreditValue() {
        return creditValue;
    }

    public void setDebitValue(BigDecimal debitValue) {
        this.debitValue = debitValue;
    }

    public BigDecimal getDebitValue() {
        return debitValue;
    }

    public BigDecimal getCreditCounterValueForDebit() {
        return debitValue;
    }

    public void setTransactionSign(String sign) {
        this.transactionSign = sign;
    }
}

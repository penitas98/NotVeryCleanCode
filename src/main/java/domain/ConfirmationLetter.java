package domain;

import record.parser.FileExtension;

import java.math.BigDecimal;
import java.util.List;

public class ConfirmationLetter {
    public void setExtension(FileExtension extension) {
    }

    public void setCurrency(Currency currency) {

    }

    public void setHashTotalCredit(String credit) {

    }

    public void setHashTotalDebit(String debit) {

    }

    public void setBatchTotalDebit(String toString) {
    }

    public void setBatchTotalCredit(String toString) {
    }

    public void setTotalProcessedRecords(String toString) {

    }

    public void setTransactionCost(String toString) {

    }

    public void setTransferType(String collectionType) {

    }

    public void setBanks(List<AmountAndRecordsPerBank> bankMap) {
    }

    public void setCreditingErrors(List<FaultRecord> faultyRecords) {

    }

    public void setClient(Client client) {

    }

    public void setBranchName(String branchName) {
    }

    public void setRetrievedAmountEur(BigDecimal bigDecimal) {

    }

    public void setRetrievedAmountUsd(BigDecimal bigDecimal) {

    }

    public void setRetrievedAmountFL(BigDecimal bigDecimal) {

    }

    public void setTotalRetrievedRecords(Integer totalRecords) {

    }
}

package confirmationLetter;

import java.math.BigDecimal;
import java.util.*;

import dao.CurrencyDao;
import domain.*;
import domain.Currency;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import record.command.FileUploadCommand;
import record.domain.TempRecord;
import record.parser.FileExtension;
import record.service.impl.Constants;

public class ConfirmationLetterGenerator {

	private LetterSelector letterSelector;

	public OurOwnByteArrayOutputStream letter(RequestContext context, FileUploadCommand fileUploadCommand,
			Client client, HashBatchRecordsBalance hashBatchRecordsBalance, String branchName,
			List&lt;AmountAndRecordsPerBank&gt; bankMap, List&lt;com.example.record.domain.FaultRecord&gt; faultyRecords,
			FileExtension extension, List&lt;Record&gt; records, List&lt;TempRecord&gt; faultyAccountNumberRecordList,
			List&lt;TempRecord&gt; sansDuplicateFaultRecordsList) {

		ConfirmationLetter letter = createConfirmationLetter(fileUploadCommand, client, hashBatchRecordsBalance,
				branchName, bankMap, faultyRecords, extension, records, faultyAccountNumberRecordList,
				sansDuplicateFaultRecordsList);

		OurOwnByteArrayOutputStream arrayOutputStream = generateConfirmationLetterAsPDF(client, letter);

		context.getConversationScope().asMap().put("dsbByteArrayOutputStream", arrayOutputStream);

		return arrayOutputStream;
	}

	public OurOwnByteArrayOutputStream generateConfirmationLetterAsPDF(
			Client client, ConfirmationLetter letter) {
		return letterSelector.generateLetter(client.getCreditDebit(), letter);
	}

	public ConfirmationLetter createConfirmationLetter(
			FileUploadCommand fileUploadCommand, Client client,
			HashBatchRecordsBalance hashBatchRecordsBalance, String branchName,
			List&lt;AmountAndRecordsPerBank&gt; bankMap,
			List&lt;com.example.record.domain.FaultRecord&gt; faultyRecords,
			FileExtension extension, List&lt;Record&gt; records,
			List&lt;TempRecord&gt; faultyAccountNumberRecordList,
			List&lt;TempRecord&gt; sansDuplicateFaultRecordsList) {

		ConfirmationLetter letter = new ConfirmationLetter();

		letter.setCurrency(records.get(0).getCurrency());
		letter.setExtension(extension);

		letter.setHashTotalCredit(hashBatchRecordsBalance.getHashTotalCredit().toString());
		letter.setHashTotalDebit(hashBatchRecordsBalance.getHashTotalDebit().toString());

		letter.setTotalProcessedRecords(hashBatchRecordsBalance.getRecordsTotal().toString());

		letter.setTransferType(hashBatchRecordsBalance.getCollectionType());
		letter.setBanks(bankMap);

		letter.setCreditingErrors(faultyRecords);
		letter.setClient(client);
		letter.setBranchName(branchName);

		ConfirmationLetterTotalsCalculator calculator
			= new ConfirmationLetterTotalsCalculator();
		Map&lt;String, BigDecimal&gt; recordAmount
			= calculator.calculateRetrievedAmounts(
					Collections.&lt;GenericRecord&gt;unmodifiableList(records),
					Collections.&lt;GenericRecord&gt;unmodifiableList(faultyAccountNumberRecordList),
					Collections.&lt;GenericRecord&gt;unmodifiableList(sansDuplicateFaultRecordsList),
					client);

		letter.setRetrievedAmountEur(recordAmount.get(Constants.CURRENCY_EURO));
		letter.setRetrievedAmountFL(recordAmount.get(Constants.CURRENCY_FL));
		letter.setRetrievedAmountUsd(recordAmount.get(Constants.CURRENCY_FL));

		letter.setBatchTotalDebit(hashBatchRecordsBalance.calculateTotalOverBatches(client.getAmountDivider(),
				Constants.CREDIT).toString());
		letter.setBatchTotalCredit(hashBatchRecordsBalance.calculateTotalOverBatches(client.getAmountDivider(),
				Constants.DEBIT).toString());

		letter.setTransactionCost(getTransactionCost(fileUploadCommand, hashBatchRecordsBalance));
		letter.setTotalRetrievedRecords(fileUploadCommand.getTotalRecords());
		return letter;
	}

	public String getTransactionCost(FileUploadCommand fileUploadCommand,
			HashBatchRecordsBalance hashBatchRecordsBalance) {

		String transactionCost = "";

		if (fileUploadCommand.hasFee()) {
			transactionCost = hashBatchRecordsBalance.getTotalFee().toString();
		}

		return transactionCost;
	}

	public void setLetterSelector(LetterSelector letterSelector) {
		this.letterSelector = letterSelector;
	}

}

public class ConfirmationLetterTotalsCalculator {

	private List&lt;String&gt; currencies = Arrays.asList(Constants.CURRENCY_EURO,
			Constants.CURRENCY_FL, Constants.CURRENCY_USD);

	private class CreditDebitHolder {

		private Map&lt;String, Map&lt;String, BigDecimal&gt;&gt; values = new HashMap&lt;String, Map&lt;String, BigDecimal&gt;&gt;();

		public CreditDebitHolder() {
			values.put(Constants.DEBIT, new HashMap&lt;String, BigDecimal&gt;());
			values.put(Constants.CREDIT, new HashMap&lt;String, BigDecimal&gt;());
		}

		public BigDecimal getValue(String sign, String currency) {
			BigDecimal value = values.get(sign).get(currency);
			if (value == null) {
				value = BigDecimal.ZERO;
			}
			return value;
		}

		public void setValue(String currency, String sign, BigDecimal value) {
			values.get(sign).put(currency, value);
		}
	}

	private class RecordFilterStrategy {
		boolean filter(GenericRecord record) {
			return true;
		}
	}

	private RecordFilterStrategy sansAmountsFilter = new RecordFilterStrategy();
	private RecordFilterStrategy faultyAmountsFilter = new RecordFilterStrategy();
	private RecordFilterStrategy recordAmountsFilter = new RecordFilterStrategy() {
		boolean filter(GenericRecord record) {
			return record.isCounterTransferRecord() && !record.hasFee();
		}
	};
	private RecordFilterStrategy balancedFilter = new RecordFilterStrategy() {
		boolean filter(GenericRecord record) {
			return !record.hasFee() && record.isDebitRecord();
		}
	};

	private CreditDebitHolder recordAmounts = new CreditDebitHolder();
	private CreditDebitHolder sansAmounts = new CreditDebitHolder();
	private CreditDebitHolder faultyAccountRecordAmounts = new CreditDebitHolder();
	private CreditDebitHolder balancedAmounts = new CreditDebitHolder();

	private Utils utils;
	private Client client;

	public Map&lt;String, BigDecimal&gt; calculateRetrievedAmounts(
			List&lt;GenericRecord&gt; records,
			List&lt;GenericRecord&gt; faultyAccountNumberRecordList,
			List&lt;GenericRecord&gt; sansDuplicateFaultRecordsList,
			Client client) {

		this.client = client;

		if (client.isBalanced()) {
			calculateTotalsOverRecords(records, balancedAmounts, balancedFilter);
		} else {
			calculateTotalsOverRecords(records, recordAmounts, recordAmountsFilter);
			calculateTotalsOverRecords(sansDuplicateFaultRecordsList, sansAmounts, sansAmountsFilter);
			calculateTotalsOverRecords(faultyAccountNumberRecordList, faultyAccountRecordAmounts, faultyAmountsFilter);
		}
		return calculateOverallTotalsForAllCurrencies();
	}

	private void calculateTotalsOverRecords(List&lt;GenericRecord&gt; records,
			CreditDebitHolder amountsHolder,
			RecordFilterStrategy filterCommand) {

		for (GenericRecord record : records) {
			if (filterCommand.filter(record)) {
				addAmountToSignedTotal(record, amountsHolder);
			}
		}
	}

	private void addAmountToSignedTotal(GenericRecord record,
			CreditDebitHolder amountsHolder) {

		setRecordSignToClientSignIfUnset(record);
		setRecordCurrencyCodeToClientIfUnset(record);

		String currency = Currency.getCurrencyByCode(record.getCurrencyNumericCode());
		amountsHolder.setValue(currency, record.getSign(),
				amountsHolder.getValue(currency, record.getSign()).add(record.getAmountAsBigDecimal()));
	}

	private Map&lt;String, BigDecimal&gt; calculateOverallTotalsForAllCurrencies() {
		HashMap&lt;String, BigDecimal&gt; recordAmount = new HashMap&lt;String, BigDecimal&gt;();
		for (String currency : currencies) {
			recordAmount.put(currency, calculateOverallTotal(currency));
		}
		return recordAmount;
	}

	private BigDecimal calculateOverallTotal(String currency) {
		return calculateOverAllTotalForSign(currency, Constants.CREDIT)
			.subtract(calculateOverAllTotalForSign(currency, Constants.DEBIT)).abs();
	}

	private BigDecimal calculateOverAllTotalForSign(String currency, String sign) {
		return balancedAmounts.getValue(currency, sign)
			.add(recordAmounts.getValue(currency, sign)
			.add(sansAmounts.getValue(currency, sign))
			.subtract(faultyAccountRecordAmounts.getValue(currency, sign)));
	}

	private void setRecordCurrencyCodeToClientIfUnset(GenericRecord sansDupRec) {
		Integer currencyCode = sansDupRec.getCurrencyNumericCode();
		if (currencyCode == null) {
			Currency currency = utils.getDefaultCurrencyForClient(client);
			sansDupRec.setCurrencyNumericCode(currency.getCode());
		}
	}

	private void setRecordSignToClientSignIfUnset(GenericRecord tempRecord) {
		if (tempRecord.getSign() == null) {
			String sign = client.getCreditDebit();
			tempRecord.setSign(sign);
		}
	}
}
package com.kalpesh.bankstatement;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.kalpesh.bankstatement.dao.TransactionDAO;
import com.kalpesh.bankstatement.model.Mode;
import com.kalpesh.bankstatement.model.Transaction;

@SpringBootApplication
public class BankStatementApplication implements CommandLineRunner {

	@Autowired
	private TransactionDAO transactionDAO;

	public static void main(String[] args) {
		SpringApplication.run(BankStatementApplication.class, args);
	}

	@Override
	public void run(String... args) throws IOException {
		int startingIndex = 0;
		PDDocument document = PDDocument.load(new File("/Users/kalpesh.panchbhai/Downloads/540569072_2022_M04.pdf"));
		if (!document.isEncrypted()) {
			PDFTextStripper stripper = new PDFTextStripper();
			String pdf = stripper.getText(document);
			String[] texts = pdf.split(" |\n");
			for (int i = 0; i < texts.length; i++) {
				if (texts[i].equals("DATE") && texts[i + 1].equals("MODE") && texts[i + 2].equals("PARTICULARS") && texts[i + 3].equals("DEPOSITS") && texts[i + 4].equals("WITHDRAWALS") && texts[i + 5].equals("BALANCE")) {
					startingIndex = i + 6;
					break;
				}
			}

			List<Transaction> transactionList = new ArrayList<>();
			Transaction transaction = null;
			boolean dateFound = false;
			boolean bringingForward = false;
			Double transactionAmount = null;
			Double lastBalance = null;
			Double balance = null;
			boolean balanceFound = false;
			String particular = "";

			for (; startingIndex < texts.length; startingIndex++) {
				if (!texts[startingIndex].equals("")) {
					if (!(dateFound)) {
						transaction = new Transaction();
						try {
							LocalDate transactionDate = LocalDate.parse(texts[startingIndex], DateTimeFormatter.ofPattern("dd-MM-yyyy"));
							transaction.setDate(transactionDate);
							dateFound = true;
						} catch (DateTimeParseException e) {
						}
					} else {
						if (Mode.contains(particular + " " + texts[startingIndex])) {
							transaction.setMode(Mode.value(particular + " " + texts[startingIndex]));
							particular = "";
						} else {
							boolean amountFound = false;
							String value = texts[startingIndex].replace(",", "");
							if (value.lastIndexOf(".") == value.length() - 3) {
								try {
									Double.valueOf(value);
									if (transactionAmount == null) {
										transactionAmount = Double.valueOf(value);
									} else {
										balance = Double.valueOf(value);
										balanceFound = true;
									}

									if (bringingForward) {
										transaction.setBalance(lastBalance = transactionAmount);
										transactionAmount = null;
										dateFound = false;
										bringingForward = false;
										transactionList.add(transaction);
										particular = "";
									} else if (balanceFound) {
										if (Double.compare(balance, lastBalance) < 0) {
											transaction.setWithdrawal(transactionAmount);
										} else {
											transaction.setDeposit(transactionAmount);
										}
										transaction.setBalance(balance);
										transactionAmount = null;
										lastBalance = balance;
										balanceFound = false;
										transactionList.add(transaction);
										particular = "";
										dateFound = false;
									}
									amountFound = true;
								} catch (NumberFormatException e) {
								}
							}
							if (!amountFound) {
								if (texts[startingIndex].equals("B/F")) {
									bringingForward = true;
									particular = texts[startingIndex];
								} else {
									particular = particular == "" ? texts[startingIndex] : particular + " " + texts[startingIndex];
								}
								transaction.setParticular(particular);
							}
						}
					}
				} else if (texts[startingIndex].equals("Total:")) {
					break;
				}
			}
			transactionDAO.saveAll(transactionList);
		}
		document.close();
	}

}

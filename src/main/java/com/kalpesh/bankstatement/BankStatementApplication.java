package com.kalpesh.bankstatement;

import com.kalpesh.bankstatement.dao.TransactionDAO;
import com.kalpesh.bankstatement.model.Mode;
import com.kalpesh.bankstatement.model.Transaction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.kalpesh.bankstatement.model.Mode.*;

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
        String currentDir = System.getProperty("user.dir");
        //Creating a File object for directory
        File directoryPath = new File(currentDir);
        //List of all files and directories
        List<String> contents = Arrays.stream(Objects.requireNonNull(directoryPath.list())).filter(s -> s.endsWith(".pdf")).toList();
        for (int j = 0; j < contents.size(); j++) {

            if (contents.get(j).endsWith("_yearly.pdf")) {
                PDDocument document = PDDocument.load(new File(currentDir + "/" + contents.get(j)));
                if (document.isEncrypted()) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String pdf = stripper.getText(document);
                    String[] texts = pdf.split(" |\n");
                    for (int i = 0; i < texts.length; i++) {
                        if (texts[i].equals("S") && texts[i + 1].equals("No.") && texts[i + 2].equals("Value") && texts[i + 3].equals("Date") && texts[i + 4].equals("Transaction") && texts[i + 5].equals("Date") && texts[i + 6].equals("Cheque") && texts[i + 7].equals("Number") && texts[i + 8].equals("Transaction") && texts[i + 9].equals("Remarks") && texts[i + 10].equals("Withdrawal") && texts[i + 11].equals("Amount") && texts[i + 12].equals("(INR") && texts[i + 13].equals(")") && texts[i + 14].equals("Deposit") && texts[i + 15].equals("Amount") && texts[i + 16].equals("(INR") && texts[i + 17].equals(")") && texts[i + 18].equals("Balance") && texts[i + 19].equals("(INR") && texts[i + 20].equals(")")) {
                            startingIndex = i + 21;
                            break;
                        }
                    }

                    Transaction transaction = null;
                    boolean dateFound = false;
                    boolean bringingForward = false;
                    Double transactionAmount = null;
                    Double lastBalance = Double.valueOf("0.0");
                    Double balance = null;
                    boolean balanceFound = false;
                    boolean zeroFound = false;
                    boolean DepositAmount = false;
                    String particular = "";
                    boolean modeFound = false;

                    for (; startingIndex < texts.length - 2; startingIndex++) {
                        if (!texts[startingIndex].equals("")) {
                            if (!(dateFound)) {
                                transaction = new Transaction();
                                try {
                                    startingIndex += 2;
                                    LocalDate transactionDate = LocalDate.parse(texts[startingIndex], DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                                    transaction.setDate(transactionDate);
                                    dateFound = true;
                                } catch (DateTimeParseException e) {
                                    startingIndex -= 1;
                                }
                            } else {
                                if (Mode.contains(particular + " " + texts[startingIndex])) {
                                    transaction.setMode(Mode.value(particular + " " + texts[startingIndex]));
                                    particular = "";
                                    modeFound = true;
                                } else if (!texts[startingIndex].equals("-")) {
                                    boolean amountFound = false;
                                    String value = texts[startingIndex].replace(",", "");
                                    if (value.lastIndexOf(".") != -1 && value.lastIndexOf(".") >= value.length() - 3 && (!value.equals("0.0") || zeroFound) && !value.equals("36.25")) {
                                        try {
                                            Double.valueOf(value);
                                            if (transactionAmount == null) {
                                                if (zeroFound) DepositAmount = true;
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
                                                if (transactionDAO.findByDateAndModeAndParticularAndDepositAndWithdrawalAndBalance(transaction.getDate(), transaction.getMode(), transaction.getParticular(), transaction.getDeposit(), transaction.getWithdrawal(), transaction.getBalance()).isEmpty())
                                                    transactionDAO.save(transaction);
                                                particular = "";
                                            } else if (balanceFound) {
                                                if (!DepositAmount) {
                                                    transaction.setWithdrawal(transactionAmount);
                                                } else {
                                                    transaction.setDeposit(transactionAmount);
                                                }
                                                transaction.setBalance(balance);
                                                transactionAmount = null;
                                                lastBalance = balance;
                                                balanceFound = false;
                                                DepositAmount = false;
                                                zeroFound = false;
                                                if (transactionDAO.findByDateAndModeAndParticularAndDepositAndWithdrawalAndBalance(transaction.getDate(), transaction.getMode(), transaction.getParticular(), transaction.getDeposit(), transaction.getWithdrawal(), transaction.getBalance()).isEmpty())
                                                    transactionDAO.save(transaction);
                                                particular = "";
                                                dateFound = false;
                                            }
                                            amountFound = true;
                                        } catch (NumberFormatException e) {
                                        }
                                    } else if (value.equals("0.0")) {
                                        zeroFound = true;
                                    }
                                    if (!amountFound && !value.equals("0.0")) {
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
                            if (particular.contains("/") && Mode.contains(particular.substring(0, particular.indexOf("/"))) && Mode.value(particular.substring(0, particular.indexOf("/"))).equals(UPI)) {
                                transaction.setMode(UPI);
                            } else if (particular.contains("/") && Mode.contains(particular.substring(0, particular.indexOf("/"))) && Mode.value(particular.substring(0, particular.indexOf("/"))).equals(ATM)) {
                                transaction.setMode(ATM);
                            } else if (!modeFound) {
                                transaction.setMode(OTHER);
                                modeFound = false;
                            }
                        } else if (texts[startingIndex].equals("Total:")) {
                            break;
                        }
                    }
                }
                document.close();

            } else {
                PDDocument document = PDDocument.load(new File(currentDir + "/" + contents.get(j)));
                if (!document.isEncrypted()) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String pdf = stripper.getText(document);
                    String[] texts = pdf.split(" |\n");
                    boolean savingAccount = false;
                    for (int i = 0; i < texts.length; i++) {
                        if (texts[i].equals("Savings") && texts[i + 1].equals("Account")) {
                            savingAccount = true;
                        }
                        if (savingAccount && texts[i].equals("DATE") && texts[i + 1].equals("MODE") && texts[i + 2].equals("PARTICULARS") && texts[i + 3].equals("DEPOSITS") && texts[i + 4].equals("WITHDRAWALS") && texts[i + 5].equals("BALANCE")) {
                            startingIndex = i + 6;
                            break;
                        }
                    }

                    Transaction transaction = null;
                    boolean dateFound = false;
                    boolean bringingForward = false;
                    Double transactionAmount = null;
                    Double lastBalance = null;
                    Double balance = null;
                    boolean balanceFound = false;
                    String particular = "";
                    boolean modeFound = false;

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
                                    modeFound = true;
                                } else {
                                    boolean amountFound = false;
                                    String value = texts[startingIndex].replace(",", "");
                                    if (value.lastIndexOf(".") != -1 && value.lastIndexOf(".") >= value.length() - 3) {
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
                                                if (transactionDAO.findByDateAndModeAndParticularAndDepositAndWithdrawalAndBalance(transaction.getDate(), transaction.getMode(), transaction.getParticular(), transaction.getDeposit(), transaction.getWithdrawal(), transaction.getBalance()).isEmpty())
                                                    transactionDAO.save(transaction);
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
                                                if (transactionDAO.findByDateAndModeAndParticularAndDepositAndWithdrawalAndBalance(transaction.getDate(), transaction.getMode(), transaction.getParticular(), transaction.getDeposit(), transaction.getWithdrawal(), transaction.getBalance()).isEmpty())
                                                    transactionDAO.save(transaction);
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
                                if (particular.contains("/") && Mode.contains(particular.substring(0, particular.indexOf("/"))) && Mode.value(particular.substring(0, particular.indexOf("/"))).equals(UPI)) {
                                    transaction.setMode(UPI);
                                    modeFound = false;
                                } else if (particular.contains("/") && Mode.contains(particular.substring(0, particular.indexOf("/"))) && Mode.value(particular.substring(0, particular.indexOf("/"))).equals(ATM)) {
                                    transaction.setMode(ATM);
                                    modeFound = false;
                                } else if (!modeFound) {
                                    transaction.setMode(OTHER);
                                    modeFound = false;
                                }
                            }
                        } else if (texts[startingIndex].equals("Total:")) {
                            break;
                        }
                    }
                }
                document.close();
            }
        }
    }

}

package com.kalpesh.bankstatement.dao;

import com.kalpesh.bankstatement.model.Mode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalpesh.bankstatement.model.Transaction;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface TransactionDAO extends JpaRepository<Transaction, Long> {

    public Optional<Transaction> findByDateAndModeAndParticularAndDepositAndWithdrawalAndBalance(LocalDate date, Mode mode, String particular, Double deposit, Double withdrawal, Double balance);
}

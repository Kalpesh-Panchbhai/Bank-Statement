package com.kalpesh.bankstatement.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kalpesh.bankstatement.model.Transaction;

@Repository
public interface TransactionDAO extends JpaRepository<Transaction, Long> {
}

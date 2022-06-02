package com.kalpesh.bankstatement.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Transaction {
	@Id
	@Column(name = "id", nullable = false)
	private Long id;

	private LocalDate date;

	private Mode mode;

	private String particular;

	private Double deposit;

	private Double withdrawal;

	private Double balance;
}

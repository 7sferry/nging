package com.example.nging.accounting.domain.getall;

import com.example.nging.accounting.domain.AccountRecord;

import java.util.List;

public record GetAllBalancesResult(List<AccountRecord> accounts) {
}

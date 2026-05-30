package com.example.nging.user.domain.getall;

import com.example.nging.user.domain.UserWithBalance;

import java.util.List;

public record GetAllUsersResult(List<UserWithBalance> users) {
}

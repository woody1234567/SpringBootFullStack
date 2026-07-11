package com.example.expensetracker.repository;

import com.example.expensetracker.repository.model.CreateUserResult;
import com.example.expensetracker.repository.model.FindUserResult;

public interface UserRepository {

    CreateUserResult createUser(String email, String passwordHash, String displayName);

    FindUserResult findByEmail(String email);
}

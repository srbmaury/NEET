package com.neet.backend.auth

class EmailAlreadyExistsException(message: String) : Exception(message)

class InvalidCredentialsException(message: String) : Exception(message)

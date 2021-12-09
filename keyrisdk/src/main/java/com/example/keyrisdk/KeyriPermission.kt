package com.example.keyrisdk

enum class KeyriPermission(val id: String) {
    SESSION("getSession"),
    ACCOUNTS("accounts"),
    SIGNUP("signUp"),
    LOGIN("login"),
    MOBILE_SIGNUP("mobileSignUp"),
    MOBILE_LOGIN("mobileLogin")
}

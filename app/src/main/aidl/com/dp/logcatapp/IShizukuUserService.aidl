package com.dp.logcatapp;

interface IShizukuUserService {
    void exit() = 16777114;
    int grantPermission(String packageName, String permission) = 1;
}

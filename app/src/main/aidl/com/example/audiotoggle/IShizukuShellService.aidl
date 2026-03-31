package com.example.audiotoggle;

interface IShizukuShellService {
    int exec(String command) = 1;
    @nullable String execReadLine(String command) = 2;
    void destroy() = 16777114;
}

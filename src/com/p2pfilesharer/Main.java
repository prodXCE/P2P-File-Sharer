package com.p2pfilesharer;

import com.p2pfilesharer.cli.CliHandler;

public class Main {
    public static void main(String[] args) {
        CliHandler cli = new CliHandler();
        cli.start();
    }
}
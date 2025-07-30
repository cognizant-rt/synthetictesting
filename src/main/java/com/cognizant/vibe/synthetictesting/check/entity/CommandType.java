package com.cognizant.vibe.synthetictesting.check.entity;

public enum CommandType {
    /**
     * Performs an HTTP GET request.
     */
    GET,

    /**
     * Performs an ICMP echo request (ping).
     */
    PING,

    /**
     * Checks if a specific TCP port is open.
     */
    TCP_PORT
}

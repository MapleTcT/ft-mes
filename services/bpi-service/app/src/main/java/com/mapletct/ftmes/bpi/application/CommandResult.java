package com.mapletct.ftmes.bpi.application;

public record CommandResult<T>(T data, boolean replayed) {
}

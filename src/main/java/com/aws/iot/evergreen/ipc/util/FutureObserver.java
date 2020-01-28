/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

package com.aws.iot.evergreen.ipc.util;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FutureObserver<V> implements StreamObserver<V>, Future<List<V>> {
    private final CompletableFuture<List<V>> fut = new CompletableFuture<>();
    private final List<V> soFar = new ArrayList<>();

    @Override
    public void onNext(V v) {
        soFar.add(v);
    }

    @Override
    public void onError(Throwable throwable) {
        fut.completeExceptionally(throwable);
    }

    @Override
    public void onCompleted() {
        fut.complete(soFar);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return fut.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return fut.isCancelled();
    }

    @Override
    public boolean isDone() {
        return fut.isDone();
    }

    @Override
    public List<V> get() throws InterruptedException, ExecutionException {
        return fut.get();
    }

    @Override
    public List<V> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return fut.get(timeout, unit);
    }
}

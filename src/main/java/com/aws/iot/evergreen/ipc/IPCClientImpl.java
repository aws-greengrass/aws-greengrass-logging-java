package com.aws.iot.evergreen.ipc;

import com.aws.iot.evergreen.ipc.config.KernelIPCClientConfig;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;


//TODO: implement logging
//TODO: throw ipc client specific runtime exceptions
public class IPCClientImpl {
    private final KernelIPCClientConfig config;
    private final ManagedChannel channel;

    public IPCClientImpl(KernelIPCClientConfig config) {
        this.config = config;

        channel = ManagedChannelBuilder.forAddress(config.getHostAddress(), config.getPort())
                .intercept(new ClientInterceptor() {
                    @Override
                    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                            @Override
                            public void start(Listener<RespT> responseListener, Metadata headers) {
                                headers.put(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + config.getToken());
                                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {}, headers);
                            }
                        };
                    }
                })
                .usePlaintext().build();
    }

    public ManagedChannel getChannel() {
        return channel;
    }
}

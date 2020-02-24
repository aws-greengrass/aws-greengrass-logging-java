package com.aws.iot.evergreen.ipc.services.common;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class IPCUtil {
    private static final ObjectMapper mapper = new CBORMapper();

    /**
     * Constructs an application packet and embeds that in a protocol packet before sending it to IPC client.
     * @param ipc Core IPC client used to send the message
     * @param destination Application level destination on the kernel side
     * @param version Application level API Protocol
     * @param opCode Application level request type
     * @param data actual data that need to be exchanges
     * @param returnType return type
     * @return response returned by the kernel cast into type returnType
     */
    public static <T> CompletableFuture<T> sendAndReceive(IPCClient ipc, int destination, int version, int opCode,
                                                          Object data, final Class<T> returnType) {
        byte[] payload;
        try {
            payload = encode(data);
        } catch (IOException e) {
            CompletableFuture<T> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }
        ApplicationMessage request = new ApplicationMessage(version, opCode, payload);
        CompletableFuture<FrameReader.Message> fut = ipc.sendRequest(destination,
                new FrameReader.Message(request.toByteArray()));
        return fut.thenApply((message) -> {
            try {
                ApplicationMessage response = ApplicationMessage.fromBytes(message.getPayload());
                if (response.getVersion() != version) {
                    throw new IllegalArgumentException(String.format(
                            "Protocol version not supported requested %d, returned %d",
                            version, response.getVersion()));
                }
                return decode(response.getPayload(), returnType);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static byte[] encode(Object o) throws IOException {
        return mapper.writeValueAsBytes(o);
    }

    public static <T> T decode(byte[] data, final Class<T> returnTypeClass) throws IOException {
        return mapper.readValue(data, returnTypeClass);
    }
}

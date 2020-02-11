package com.aws.iot.evergreen.ipc.services.common;

import com.aws.iot.evergreen.ipc.IPCClient;
import com.aws.iot.evergreen.ipc.common.FrameReader;
import com.aws.iot.evergreen.ipc.common.GenericErrors;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class IPCUtil {
    private static final JSON encoder = JSON.std.with(new JacksonJrsTreeCodec()).with(new CBORFactory());
    private static final ObjectCodec mapper = new CBORMapper();

    /**
     * Send a request to the server and then get the response as a future.
     *
     * @param ipc         IPC client used for sending the request
     * @param destination destination service to receive the request
     * @param data        the message to be sent
     * @param clazz       the type of the response which is expected
     * @return future containing the deserialized response class
     */
    public static <T, E extends Enum<?> & GenericErrors> CompletableFuture<GeneralResponse<T, E>> sendAndReceive(
            IPCClient ipc, int destination, Object data, TypeReference<GeneralResponse<T, E>> clazz) {
        byte[] payload;
        try {
            payload = encode(data);
        } catch (IOException e) {
            CompletableFuture<GeneralResponse<T, E>> f = new CompletableFuture<>();
            f.completeExceptionally(e);
            return f;
        }

        CompletableFuture<FrameReader.Message> fut = ipc.sendRequest(destination, new FrameReader.Message(payload));
        return fut.thenApply((m) -> {
            try {
                return decode(m, clazz);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    public static <T> T decode(FrameReader.Message data, TypeReference<T> clazz) throws IOException {
        TreeNode tree = encoder.treeFrom(data.getPayload());
        return tree.traverse(mapper).readValueAs(clazz);
    }

    public static byte[] encode(Object o) throws IOException {
        return encoder.asBytes(o);
    }
}

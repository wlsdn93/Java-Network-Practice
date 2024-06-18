import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.Base64;

public class WSServer {
    private static final int PORT = 12345;
    private static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버가 포트 " + PORT + "에서 대기 중입니다.");

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("클라이언트가 연결되었습니다: " + clientSocket.getInetAddress());
                    handleClient(clientSocket);
                } catch (IOException e) {
                    System.out.println("클라이언트와의 통신 중 오류 발생: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("서버 소켓을 열 수 없습니다: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"))) {

            String request = readRequest(in);
            System.out.println("수신된 메시지: " + request);

            if (isWebSocketUpgrade(request)) {
                handleWebSocketHandshake(out, request);
                handleWebSocketCommunication(clientSocket.getInputStream(), out);
            }
        }
    }

    private static String readRequest(BufferedReader in) throws IOException {
        StringBuilder requestBuilder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            requestBuilder.append(line).append("\n");
        }
        return requestBuilder.toString();
    }

    private static boolean isWebSocketUpgrade(String request) {
        return request.contains("Upgrade: websocket");
    }

    private static void handleWebSocketHandshake(BufferedWriter out, String request) throws IOException {
        String webSocketKey = extractWebSocketKey(request);
        if (webSocketKey != null) {
            String acceptKey = generateWebSocketAcceptKey(webSocketKey);
            out.write("HTTP/1.1 101 Switching Protocols\r\n");
            out.write("Upgrade: websocket\r\n");
            out.write("Connection: Upgrade\r\n");
            out.write("Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n");
            out.flush();
            System.out.println("WebSocket 핸드셰이크 완료 !\n");
        }
    }

    private static String extractWebSocketKey(String request) {
        String[] lines = request.split("\n");
        for (String line : lines) {
            if (line.startsWith("Sec-WebSocket-Key: ")) {
                return line.substring(19).trim();
            }
        }
        return null;
    }

    private static String generateWebSocketAcceptKey(String webSocketKey) {
        try {
            String key = webSocketKey + WEBSOCKET_MAGIC_STRING;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(key.getBytes());
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not found", e);
        }
    }

    private static void handleWebSocketCommunication(InputStream inputStream, BufferedWriter out) {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                String message = decodeWebSocketFrame(buffer, read);
                System.out.println("Client: " + message);
            }
        } catch (IOException e) {
            System.out.println("WebSocket 통신 중 오류 발생: " + e.getMessage());
        }
    }

    private static String decodeWebSocketFrame(byte[] buffer, int length) throws IOException {
        int offset = 0;

        // FIN, RSV1-3, OpCode
        byte b = buffer[offset++];
        boolean fin = (b & 0x80) != 0;
        int opcode = b & 0x0F;

        // Mask and Payload length
        b = buffer[offset++];
        boolean mask = (b & 0x80) != 0;
        int payloadLength = b & 0x7F;

        if (payloadLength == 126) {
            payloadLength = ((buffer[offset++] & 0xFF) << 8) | (buffer[offset++] & 0xFF);
        } else if (payloadLength == 127) {
            payloadLength = (int) ((buffer[offset++] & 0xFFL) << 56
                    | (buffer[offset++] & 0xFFL) << 48
                    | (buffer[offset++] & 0xFFL) << 40
                    | (buffer[offset++] & 0xFFL) << 32
                    | (buffer[offset++] & 0xFFL) << 24
                    | (buffer[offset++] & 0xFFL) << 16
                    | (buffer[offset++] & 0xFFL) << 8
                    | (buffer[offset++] & 0xFFL));
        }

        byte[] maskingKey = new byte[4];
        if (mask) {
            for (int i = 0; i < 4; i++) {
                maskingKey[i] = buffer[offset++];
            }
        }

        byte[] payload = new byte[payloadLength];
        for (int i = 0; i < payloadLength; i++) {
            payload[i] = (byte) (buffer[offset++] ^ (mask ? maskingKey[i % 4] : 0));
        }

        return new String(payload, "UTF-8");
    }
}

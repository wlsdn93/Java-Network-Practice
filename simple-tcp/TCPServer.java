import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPServer {
    public static void main(String[] args) {
        int port = 12345; // 서버가 대기할 포트 번호

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("서버가 포트 " + port + "에서 대기 중입니다.");

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                     BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                    System.out.println("클라이언트가 연결되었습니다: " + clientSocket.getInetAddress());

                    String receivedMessage;
                    while ((receivedMessage = in.readLine()) != null) {
                        System.out.println("수신된 메시지: " + receivedMessage);
                    }
                } catch (IOException e) {
                    System.out.println("클라이언트와의 통신 중 오류 발생: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("서버 소켓을 열 수 없습니다: " + e.getMessage());
        }
    }
}


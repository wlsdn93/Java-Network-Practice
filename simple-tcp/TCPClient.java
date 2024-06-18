import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

public class TCPClient {
    public static void main(String[] args) {
        String serverAddress = "localhost"; // 서버 주소
        int serverPort = 12345; // 서버 포트 번호

        try (Socket socket = new Socket(serverAddress, serverPort);
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("서버에 연결되었습니다. 메시지를 입력하세요 (종료하려면 'exit' 입력):");

            String message;
            while (true) {
                System.out.print("메시지: ");
                message = scanner.nextLine();

                if ("exit".equalsIgnoreCase(message)) {
                    System.out.println("클라이언트 종료");
                    break;
                }

                out.write(message);
                out.newLine(); // 서버에서 BufferedReader의 readLine 메서드가 줄바꿈을 기준으로 데이터를 읽기 때문에 줄바꿈을 추가해야 합니다.
                out.flush(); // 버퍼에 남아있는 데이터를 전송합니다.
                System.out.println("서버로 메시지를 전송했습니다: " + message);
            }
        } catch (IOException e) {
            System.out.println("서버에 연결할 수 없습니다: " + e.getMessage());
        }
    }
}


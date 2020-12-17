package chat01;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiServer {
	
	public static void main(String[] args) {

		ServerSocket serverSocket = null;
		Socket socket = null;
		PrintWriter out = null;
		BufferedReader in = null;
		String s = "";

		try {
			/*
			9999번으로 포트번호를 설정하여 서버객체를 생성하고 
			클라이언트의 접속을 기다린다. 
			 */
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			////...접속대기중...

			/*
			클라이언트가 접속요청을 하면 accept()를 통해 허가한다. 
			 */
			socket = serverSocket.accept();
			
			
			
			
			
			
			/*
			getInetAddress() : 소켓이 연결되어있는 원격 IP주소를 얻어옴
			getPort() : 원격 포트번호를 얻어옴.
				즉 클라이언트의 IP와 포트번호를 얻어와서 출력함.
			 */

			/*
			getInetAddress() / getPort()
			 	: 소켓이 연결된 원격 호스트와 포트를 알려준다. 
			getLocalAddress() / getLocalPost()
			 	: 연결이 시작된 네트워크 인터페이스와 포트를 알려준다.

	/192.168.219.142의 50174포트에(접속자)
 		/192.168.219.127의 9999포트로(서버) 연결되었습니다.
 * 
 * 
 * 
InetAddress getInetAddress()	
	소켓에 연결된 원격 컴퓨터의 InetAddress 객체 반환
int getPort()	
	소켓에 연결된 컴퓨터의 포트 번호 반환

InetAddress getLocalAddress()	
	소켓에 연결된 지역 컴퓨터의 InetAddress 객체 반환	
int getLocalPort()	
	소켓에 연결된 지역 컴퓨터의 포트 번호 반환


			'잘 알려진 포트(Well-known port)'[0~1023번]를 사용하는 원격 포트와 달리,
			로컬 포트는 일반적으로 실행 시점에 이용 가능한 포트 중에서 시스템에 의해 결정된다.
			이러한 구조로 인해 단일 시스템에서 실행중인 많은 클라이언트가 동시에 같은 서비스에 
			접근하는 것이 가능해진다. 로컬포트는 로컬 호스트의 IP주소와 함께 밖으로 나가는 IP패킷에 
			박혀 있다. 그래서 서버가 클라이언트의 올바른 포트로 응답을 보낼 수 있다.
			 */
			System.out.println(socket.getInetAddress()+"(클라이언트)의 "
					+socket.getPort()+ "포트를 통해 " 
					+socket.getLocalAddress()+"(서버)의 "
					+socket.getLocalPort()+ "포트로 연결되었습니다.");
			
			
			
			

			//서버->클라이언트 측으로 메세지를 전송하기위한 스트림 생성
			out = new PrintWriter(socket.getOutputStream(), true);
			//클라이언트로부터 메세지를 받기위한 스트림 생성
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			//클라이언트가 보낸 메세지를 라인단위로 읽어옴
			s = in.readLine();
			//읽어온 내용을 콘솔에 출력
			System.out.println("Client에서읽어옴:"+ s);
			//클라이언트로 응답메세지(Echo)를 보냄
			out.println("Server에서응답:"+ s);
			//콘솔에 종료메세지 출력
			System.out.println("Bye...!!!");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				//입출력 스트림 종료(자원해제)
				in.close();
				out.close();
				//소켓 종료(자원해제)
				socket.close();
				serverSocket.close();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

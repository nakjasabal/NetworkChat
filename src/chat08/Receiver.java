package chat08;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;

//서버에서 보내는 메시지를 클라이언트의 콘솔에 출력하는 쓰레드
public class Receiver extends Thread {

	Socket socket;
	BufferedReader in = null;
	
	//Socket을 매개변수로 받는 생성자
	public Receiver(Socket socket) {
		this.socket = socket;
		
		try {
			in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
		} 
		catch (Exception e) {
			System.out.println("예외>Receiver>생성자:"+ e);			
		}
	}
	
	@Override
	public void run() {
		
		while(in != null) {
			try {
				String r_msg = URLDecoder.decode(in.readLine(), "UTF-8");
				System.out.println("Thread Receive : "+ r_msg);
			}
			catch(SocketException ne) {
				System.out.println("SocketException");
				//소켓이 종료될경우 루프를 탈출한다. 
				break;
			}
			catch (Exception e) {
				System.out.println("예외>Receiver>run1:"+ e);	
			}
		}
		
		//채팅종료
		try {
			in.close();
		} 
		catch (Exception e) {
			System.out.println("예외>Receiver>run2:"+ e);	
		}
	}	
}

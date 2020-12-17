package chat08;

import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.Scanner;

public class Sender extends Thread {

	Socket socket;
	PrintWriter out = null;
	String name;
	
	//소켓객체와 클라이언트의 이름을 인자로 센더객체를 생성한다. 
	public Sender(Socket socket, String name) {
		this.socket = socket;
		try {
			out = new PrintWriter(this.socket.getOutputStream(), true);
			this.name = name;
		} 
		catch (Exception e) {
			System.out.println("예외>Sender>생성자:"+ e);
		}
	}
	
	@Override
	public void run() {
		Scanner s = new Scanner(System.in);
		
		try {
			//클라이언트가 서버로 입력한 사용자이름을 보내준다. 
			name = URLEncoder.encode(name, "UTF-8");
			out.println(name);
			
			while(out != null) {
				try {
					String s2 = s.nextLine();
					if(s2.equalsIgnoreCase("Q")){
						break;
					}
					else {
						//out.println(s2);
						out.println(URLEncoder.encode(s2, "UTF-8"));
					}
				} 
				catch (Exception e) {
					System.out.println("예외>Sender>run1:"+ e);
				}
			}
			
			out.close();
			socket.close();
		} 
		catch (Exception e) {
			System.out.println("예외>Sender>run2:"+ e);
		}		
	}
}

package chat08;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MultiServer extends DBConnect {
		
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	Map<String, PrintWriter> clientMap;	
	
	//생성자
	public MultiServer() {		
		//클라이언트의 출력스트림을 저장할 해쉬맵 생성
		clientMap = new HashMap<String, PrintWriter>();
		//해쉬맵 동기화 설정. 동시접근을 막아주는 역할을 한다. 
		Collections.synchronizedMap(clientMap);
	}
	
	public void init() {
		
		try {
			//9999번 port로 설정해 서버를 생성후 클라이언트의 접속을 대기...
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			while(true) {
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress()+":"+socket.getPort());
				
				//클라이언트의 메세지를 다른 클라이언트에게 전달하기위한 쓰레드 생성 및 시동
				Thread mst = new MultiServerT(socket);
				mst.start();
			}			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
			}
			catch (Exception e) {			
				e.printStackTrace();
			}
		}	
	}
	
	// 접속된 모든 클라이언트들에게 메시지를 전달.
	public void sendAllMsg(String name, String msg) {
		
		//Map에 저장된 객체들의 키값을 가져온다.  
		Iterator<String> it = clientMap.keySet().iterator();		

		while(it.hasNext()) {
			try {
				PrintWriter it_out = (PrintWriter) clientMap.get(it.next());
				//msg = URLEncoder.encode(msg, "UTF-8");
				//name = URLEncoder.encode(name, "UTF-8");
				
				/*
				name이 빈값이면 메세지만...
				그외는 이름과 메세지를 합쳐서 전달한다.
				 */
				if(name.equals("")) {					
					it_out.println(URLEncoder.encode(msg, "UTF-8"));
				}
				else {					
					it_out.println("["+ URLEncoder.encode(name, "UTF-8") +"] "+ URLEncoder.encode(msg, "UTF-8"));
				}
			} 
			catch(Exception e) {
				System.out.println("예외:"+e);
			}			
		}
	}
	 
	public static void main(String[] args) {
		//서버객체생성
		MultiServer ms = new MultiServer();
		//채팅을 위한 초기화 부분
		ms.init();
	}
		
	
	//내부클래스
	/*
	클라이언트로부터 읽어온 메시지를 다른 클라이언트에게
	보내는 역할을 하는 메소드
	 */
	class MultiServerT extends Thread {
		
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(), true);
				/*in = new BufferedReader(new InputStreamReader(
						this.socket.getInputStream()));*/
				in = new BufferedReader(new InputStreamReader(
						this.socket.getInputStream(), "UTF-8"));
			} 
			catch (Exception e) {
				System.out.println("예외:"+ e);
			}
		}
		
		@Override
		public void run() {
			
			String name = ""; //클라이언트로부터 받은 이름을 저장할 변수
			String s = "";
			
			try {
				//클라이언트에서 처음으로 보내는 메시지는 클라이언트가 사용할 이름이다.
				name = URLDecoder.decode(in.readLine(), "UTF-8");

				//현재 객체가 가지고있는 소켓을 제외하고 다른 소켓(클라이언트)들에게 접속을 알림.
				sendAllMsg("", name + "님이 입장하셨습니다.");//첫번째 인자없이 메소드호출				
				
				//해쉬맵에 클라이언트의 이름을 키값으로 출력스트림 객체를 저장한다.
				clientMap.put(name, out);  
				
				System.out.println(name + "> 접속");
				System.out.println("현재 접속자 수는 "+clientMap.size()+"명 입니다.");
				
				// 입력스트림이 null이 아니면 반복.
				while (in!=null) { 
					s = URLDecoder.decode(in.readLine(), "UTF-8");					
					if ( s == null ) break;					
					
					//DB입력
					try {
						String query = "INSERT INTO chatting_tb VALUES (chatting_seq.nextval, ?, ?, sysdate)";
						psmt = con.prepareStatement(query);
						psmt.setString(1, name);
						psmt.setString(2, s);					 
						int affected = psmt.executeUpdate();
						System.out.println(affected +"행이 입력되었습니다.");
					}
					catch (Exception e) {
						e.printStackTrace();
					}					
					
					System.out.println(name + " >> " + s);
					sendAllMsg(name, s);//첫번째 인자 포함해서 메소드호출
				}
			} 
			catch (Exception e) {
				System.out.println("예외:"+ e);
			}
			finally {
				//예외가 발생할때 퇴장. 해쉬맵에서 해당 데이터 제거.
				//보통 종료하거나 나가면 java.net.SocketException: 예외발생
				clientMap.remove(name); 
				sendAllMsg("", name + "님이 퇴장하셨습니다.");				 
				System.out.println(name + " [" + Thread.currentThread().getName() +  "] 퇴장");
				System.out.println("현재 접속자 수는 "+clientMap.size()+"명 입니다.");

				try {
					in.close();
					out.close();
					socket.close();
				} 
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}		
	}
}

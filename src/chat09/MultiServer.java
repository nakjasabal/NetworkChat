package chat09;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class MultiServer extends DBConnect {//
		
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	Map<String, PrintWriter> clientMap;
	HashSet<String> blackListSet = new HashSet<String>(); //블랙리스트
	HashSet<String> prohibitedWords = new HashSet<String>(); //금지단어(금칙어)
	HashMap<String, String> tofixMap = new HashMap<String, String>();//귓속말고정
	HashMap<String, String> blockMap = new HashMap<String, String>();//블록설정
	
	
	//생성자
	public MultiServer() {		
		//클라이언트의 출력스트림을 저장할 해쉬맵 생성
		clientMap = new HashMap<String, PrintWriter>();
		//해쉬맵 동기화 설정. 동시접근을 막아주는 역할을 한다. 
		Collections.synchronizedMap(clientMap);
		
		//블랙리스트 추가
		blackListSet.add("개아들");blackListSet.add("십여덟");blackListSet.add("우라질레이션");
		
		//금지단어 추가
		prohibitedWords.add("대출");prohibitedWords.add("광고");prohibitedWords.add("김미영팀장");
	}
	
	public void init() {
		
		try {
			//9999번 port로 설정해 서버를 생성후 클라이언트의 접속을 대기...
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			while(true) {
				socket = serverSocket.accept();
				System.out.println("로컬서버:"+socket.getLocalAddress()+":"+socket.getLocalPort());
				System.out.println("원격클라이언트:"+socket.getInetAddress()+":"+socket.getPort());
				
			 				
				//클라이언트의 메세지를 다른 클라이언트에게 전달하기위한 쓰레드 생성 및 시동
				Thread mst = new MultiServerT(socket);
								
				if(clientMap.size()>=Const.CLIENT_CNT) {
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
					out.println(URLEncoder.encode("정원("+Const.CLIENT_CNT+"명)을 초과하여 접속하실수 없습니다", "UTF-8"));					
					break;
				}
				else {
					mst.start();			 
				}
			}			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			try {
				serverSocket.close();
				socket.close();
			}
			catch (Exception e) {			
				e.printStackTrace();
			}
		}	
	}
	
	// 접속된 모든 클라이언트들에게 메시지를 전달.
	public void sendAllMsg(String name, String msg, String flag) {
		
		//Map에 저장된 객체들의 키값을 가져온다.  
		Iterator<String> it = clientMap.keySet().iterator();
		String clientName;	
		
		while(it.hasNext()) {
			try {
				//수신자의 이름
				clientName = it.next();
				
				/*
				만약 a가 b와 c를 차단했다면 blockMap에는 Key:a => value:b|c| 와 같이 추가되어있다.
				즉, b가 a로 혹은 c가 a로 메세지를 발송한다면 차단되어야 한다.
				거꾸로 수신자 a가 메세지를 받을때 발송자가 b or c라면 차단한다.  
				 */
				String blockValue = null;
				boolean cFlag = true;
				if(blockMap.containsKey(clientName)) {
					blockValue = blockMap.get(clientName);
					String[] blockNameArr = blockValue.split("|");
					for(int i=0 ; i<blockNameArr.length-1 ; i++) {
						if(name.equals(blockNameArr[i]))
							cFlag = false;
					}
					if(cFlag==false) continue;
				}
				
				
				

				
				//수신자의 PrintWriter 객체
				PrintWriter it_out = (PrintWriter) clientMap.get(clientName);
								
				//금지단어(금칙어)가 포함되면 즉시 경고메세지만 띄우고 종료한다.
				String proComment = "금지단어가 포함 되었으므로 출력되지 않습니다";
				for(String s : prohibitedWords) {
					if(msg.indexOf(s)!=-1) {
						msg = proComment;
						
						System.out.println(proComment);
						it_out.println("["+ URLEncoder.encode(name, "UTF-8") +"] "+
								URLEncoder.encode(proComment, "UTF-8"));
						return;
					}
				}
				
				/*
				name이 빈값이면 메세지만...
				그외는 이름과 메세지를 합쳐서 전달한다.
				 */
				if(flag.equals("One")) {
					//귓속말일때는 한사람한테만..
					if(name.equals(clientName)) {
						it_out.println("["+URLEncoder.encode("귓속말", "UTF-8")+"]"+ 
								URLEncoder.encode(msg, "UTF-8"));
					}
				}
				else {
					//나머지는 전체메세지
					if(name.equals("")) {					
						it_out.println(URLEncoder.encode(msg, "UTF-8"));
					}
					else {					
						it_out.println("["+ URLEncoder.encode(name, "UTF-8") +"] "+ 
								URLEncoder.encode(msg, "UTF-8"));
					}						
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
		
	
	//블럭처리 : 블럭요청자, 차단할사용자, 추가or삭제
	public void addMinBlock(String user, String blockUser, char flag) {
		if(flag=='+') {
			if(blockMap==null) {
				//비어있다면 그냥 삽입
				blockMap.put(user, blockUser+"|");
			}
			else {
				if(blockMap.containsKey(user)) {
					//차단 내역이 있는경우 : 추가함
					blockMap.put(user, blockMap.get(user)+blockUser+"|");
				}
				else {
					//차단 내역이 없는경우 : 그냥삽입
					blockMap.put(user, blockUser+"|");
				}
			}
		}
		else if(flag=='-') {
			if(blockMap!=null && blockMap.containsKey(user)) {
				String newblockUser = blockMap.get(user).replace(blockUser+"|", "");
				blockMap.put(user, newblockUser);
			}
		}
		System.out.println(blockMap);
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
				
								
				//동일한 대화명으로 접속하면 접속불가 처리한다.
				Iterator<String> itr = clientMap.keySet().iterator();
				while(itr.hasNext()) {
					String connName = itr.next();
					if(connName.equalsIgnoreCase(name)) {
						System.out.println("동일한 대화명이 존재합니다. 접속을 종료합니다.");
						out.println(URLEncoder.encode("동일한 대화명이 존재합니다. 접속을 종료합니다.", "UTF-8"));
						//동일한 대화명이므로 기존의 클라이언트가 삭제되므로 임의로 변경한다.
						name = name +"temp";
						this.interrupt();
						return;
					}
				}
				
				//블랙리스트처리
				for(String b : blackListSet) {
					if(name.equals(b)) {
						System.out.println("블랙리스트로 처리된 대화명입니다. 접속을 종료합니다.");
						out.println(URLEncoder.encode("블랙리스트로 처리된 대화명입니다. 접속을 종료합니다.", "UTF-8"));
						name = name +"temp";
						this.interrupt();
						return;
					}
				}
				

				//현재 객체가 가지고있는 소켓을 제외하고 다른 소켓(클라이언트)들에게 접속을 알림.
				sendAllMsg("", name + "님이 입장하셨습니다.", "All");//첫번째 인자없이 메소드호출				
				
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
						System.out.println(query +"행이 입력되었습니다.");
					}
					catch (Exception e) {
						e.printStackTrace();
					}				
					
					
					//귓속말고정 on & off
					if(s.equals("/untofix")) {
						//만약 귓속말 고정을 해제하는 명령이라면
						tofixMap.remove(name);
						out.println(URLEncoder.encode("귓속말고정이 해제되었습니다.", "UTF-8"));
					}
					else {
						//귓속말고정인지 판단해서 명령어로 만들어줌
						if(tofixMap!=null && tofixMap.containsKey(name)) {
							s = "/to "+ tofixMap.get(name) +" "+ s;
							System.out.println("s="+ s);
						}
					}
					
					
					//클라이언트가 전송한 메세지가 명령어인지 판단
					if(s.charAt(0)=='/') {
						//첫문자가 /면 명령어
						if(s.equals("/list")) {							
							//System.out.println("리스트출력");
							//리스트 명령은 나한테만 Echo하면 된당.
							StringBuffer sb = new StringBuffer();
							sb.append("[접속자리스트]\n");
							//Map의 키값이 접속자 이름
							Iterator<String> it = clientMap.keySet().iterator();
							while(it.hasNext()) {
								sb.append(it.next()+"\n");
							}
							sb.append("-----------------");
							
							System.out.println("["+name+"]님이 리스트를 출력하셨습니다.");
							out.println(URLEncoder.encode(sb.toString(), "UTF-8"));
						}
						else {
							String[] strArr = s.split(" ");
							//System.out.println("다른명령");
							if(strArr[0].equals("/to")) {
								//귓속말 : 한사람한테만 보냄
								/*
								명령어형식이 "/to 대화명 대화내용"이므로 대화내용에 스페이스가 있는경우 
								문장의 끝까지를 출력해야한다. 
								*/
								String contents = "";
								for(int i=2 ; i<strArr.length ; i++) 
									contents += strArr[i]+" ";
								//파라미터 : 대화명, 대화내용
								sendAllMsg(strArr[1], "["+name+"]"+ contents, "One");
								System.out.println("["+name+"]님이 ["+strArr[1]+"]님께 귓속말을 보냈습니다.");
								out.println(URLEncoder.encode("["+strArr[1]+"]님께 귓속말을 보냈습니다.", "UTF-8"));
							}
							else if(strArr[0].equals("/tofix")) {
								//귓속말고정 : 한사람한테 지속적으로 귓속말 보냄.
								/*
								명령어형식이 "/tofix 대화명" 이므로 명령어를 입력한 사용자를 key로
								상대방을 value로 입력한다. 차후 대화가 올라올때마다 Map에 내 이름이 있는지 
								검사한 후 상대방에게 지속적으로 귓속말을 보내준다. 
								 */
								System.out.println(name +" <> "+ strArr[1]);
								tofixMap.put(name, strArr[1]);
								out.println(URLEncoder.encode(strArr[1]+"님께 귓속말이 고정되었습니다.", "UTF-8"));
							}
							else if(strArr[0].equals("/block")) {
								//대화상태 차단 기능 : 나를 제외한 나머지에게만 메세지가 전달된다.
								/*
								-기존에 차단을 요청한 사용자가 있는지 확인
								-없으면 즉시 입력
								-만약 있다면 문자열 끝에 |와 함께 추가입력
								-차단을 해제할때는 문자열에서 삭제
								*/
								addMinBlock(name, strArr[1], '+');
								out.println(URLEncoder.encode(strArr[1]+"님이 차단되었습니다.", "UTF-8"));
							}		
							else if(strArr[0].equals("/unblock")) {
								//대화상태 차단 해제
								addMinBlock(name, strArr[1], '-');
								out.println(URLEncoder.encode(strArr[1]+"님이 차단 해제되었습니다.", "UTF-8"));
							}		
						}
					}
					else {
						//아니면 일반 메세지
						//System.out.println("일반메세지");
						
						System.out.println(name + " >> " + s);
						sendAllMsg(name, s, "All");//첫번째 인자 포함해서 메소드호출
					}
				}
			} 
			catch (Exception e) {
				System.out.println("예외:"+ e);
				e.printStackTrace();
			}
			finally {
				//예외가 발생할때 퇴장. 해쉬맵에서 해당 데이터 제거.
				//보통 종료하거나 나가면 java.net.SocketException: 예외발생
				clientMap.remove(name); 
				sendAllMsg("", name + "님이 퇴장하셨습니다.", "All");				 
				System.out.println(name + " [" + Thread.currentThread().getName() +  "] 퇴장");
				System.out.println("현재 접속자 수는 "+clientMap.size()+"명 입니다.");

				try {
					in.close();
					out.close();
					socket.close();
					System.out.println("종료는되나?");
				} 
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}		
	}
}

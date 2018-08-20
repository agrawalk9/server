package chatserver;


import java.util.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class Maps{
	static Map<String,Socket> mapUserToSocket=new HashMap<String,Socket>();
	  static Map<Socket,String> mapSocketToUser=new HashMap<Socket,String>();
	  static DataFetch d1=new DataFetch();
}

class users{
	int id;
  String name,username,dob;
  public users(String username,String name,String dob){		
          this.name=name;
          this.username=username;
          this.dob=dob;
  }	
}

class DataFetch {
	ResultSet rs2;
  Connection con;
  Statement stmt;
  void fetchData(){
      try{
          Class.forName("com.mysql.jdbc.Driver");
         con=DriverManager.getConnection(  
          "jdbc:mysql://127.0.0.1:3306/users","root","kartikay");    
          stmt=con.createStatement();  
          rs2=stmt.executeQuery("select * from user");
      }
      catch(Exception ex){
          System.out.println(ex);
      }                           
  }
}


class Server1 implements Runnable{
  Socket senderSocket = null;
  String name,user;
	String[] tokens;
	Map<String,users> mapName;              
  
  public void run() {
      if(senderSocket == null)
          return;

      try {
          InputStream is = senderSocket.getInputStream();

          InputStreamReader isr = new InputStreamReader(is);
          BufferedReader br = new BufferedReader(isr);
          char[] msg = new char[1024];
          while(true){  
              try{Thread.sleep(200);}catch(Exception e){}
              int status=0;
              try  {
                  System.out.println("status before:"+status);
                  
                  status = br.read(msg,0,1024);
                  System.out.println("status1");
                  System.out.println("status after:"+status);
              }
              catch(Exception e){
                  System.out.println(e);
                  // remove user from map;
                  return;
              }
              System.out.println("Read "+status);

              //System.out.println("read status = "+status);
              if( status != -1 ){
                  System.out.println("Parsing Data");
                  parse(msg);
              } 
          }
      }
      catch (IOException ex) {
          Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
      }
	    
  }
      
	void parse(char m[]){
		String s1 = new String(m); 
      String delims = "[:]";
      tokens = s1.split(delims);
      if(tokens[0].equals("CHAT")){
          System.out.println("read ");
          if(tokens[1].equals("CON")){
          	SendMSGonConn(senderSocket,"Connection Established");
          }
          else if(tokens[1].equals("LOGIN")) {
               Maps.d1.fetchData();
                try{
                		boolean b=false;
                		String msg="";
                       while(Maps.d1.rs2.next()){
                      	 String u2=Maps.d1.rs2.getString("username");
                      	 String p2=Maps.d1.rs2.getString("password");
                      	 System.out.println("AB");
                           if(u2.equals(tokens[2])){
                               if(p2.equals(tokens[3])){
                            	   b=true;
                            	   msg="Logged in";
                              	   System.out.println("user : "+tokens[2]);
                              	   System.out.println("password : "+tokens[3]);
                              	   break;
                               }
                               else {
                              	 msg="A Invalid username or password";
                               }
                           }
                           else {
                          	 msg="B Invalid username or password";
                           }
                       }
                       if(b) {
                    	   Maps.mapSocketToUser.put(senderSocket,tokens[2]);
                           Maps.mapUserToSocket.put(tokens[2],senderSocket);
                           SendMSGonConn(senderSocket, msg);
                           System.out.println("msg : "+msg);
                       }
                       else {
                           SendMSGonConn(senderSocket, msg);
                       }
                       //Maps.d1.con.close();
                 }      
                 catch(Exception e) {
              	   System.out.println(e);
                 }
          }
          else if(tokens[1].equals("MSG")){
              if(Maps.mapUserToSocket.containsKey(tokens[1])) {
              	SendMsg(tokens[2],tokens[3]);
              }
              else {
              	SendMsg(Maps.mapSocketToUser.get(senderSocket),"User Offline");
              }
          }
          else if(tokens[1].equals("LGT")){
              Logout(tokens[2]);
          }
      }
		
	}

	void Logout(String userName){
		Socket sender=Maps.mapUserToSocket.get(userName);		
      Maps.mapUserToSocket.remove(userName);
      Maps.mapSocketToUser.remove(sender);
      System.out.println("Logged out successfully");
	}

	void SendMSGonConn(Socket recieverSocket,String msg) {
		String data="";
		String senderName="Server";
		data="CHAT:MSGFROM:"+senderName+":"+msg;
		OutputStream os=null;
		OutputStreamWriter osw=null;
	      BufferedWriter bw=null;
	      try {
	          os = recieverSocket.getOutputStream();
	          osw = new OutputStreamWriter(os);
	          bw = new BufferedWriter(osw);
	          System.out.println(recieverSocket.getInetAddress());
	          if(!recieverSocket.getInetAddress().equals(null))
	          {
	             bw.flush();
	             bw.write(data);
	             bw.flush();
	          }
	          else{return;}
	          System.out.println("sent "+data);
	      } 
	      catch (IOException ex) {
	          Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
	      }
	}

	void SendMsg(String recieverName,String msg){
		String data="";
		Socket recieverSocket=null;
		String senderName=(String)Maps.mapSocketToUser.get(senderSocket);
      data="CHAT:MSGFROM:"+senderName+":"+msg;
      recieverSocket=Maps.mapUserToSocket.get(recieverName);
      OutputStream os=null;
      OutputStreamWriter osw=null;
      BufferedWriter bw=null;
      try {
          os = recieverSocket.getOutputStream();
          osw = new OutputStreamWriter(os);
          bw = new BufferedWriter(osw);
           //System.out.println(recieverSocket.getInetAddress());
          if(!recieverSocket.getInetAddress().equals(null))
          {
             bw.flush();
             bw.write(data);
             bw.flush();
          }
          else{return;}
          System.out.println("sent "+data);
      } 
      catch (IOException ex) {
          Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
      }
  }
  void acceptConnection(Socket soc){
      senderSocket = soc;
  }
}
class Server{        
  public static void main(String args[]) throws IOException{
      Server1 s;
      ServerSocket serverSocket = null;
      int port = 25000;
      serverSocket = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"));
      System.out.println("Server Started");
      while(true){                  
          System.out.println("listening to port 25000");
          s=new Server1();
          try {
              s.acceptConnection(serverSocket.accept());
          } 
          catch (IOException ex) {
              System.out.println("Cannot accept connection");
              ex.printStackTrace();
          }
          Thread t=new Thread(s);
          t.start();
      }
  }

}
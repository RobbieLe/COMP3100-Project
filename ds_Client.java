import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class ds_Client {
    public static void main(String[] args) throws Exception {
		Socket s = new Socket("localhost", 50000);
		DataInputStream din = new DataInputStream(s.getInputStream());
		BufferedReader socketIn = new BufferedReader(new InputStreamReader(din));
		DataOutputStream dout = new DataOutputStream(s.getOutputStream());
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		ArrayList<String> Servers = new ArrayList<String>();
		
		String str="", str2="";
		
		str = "HELO";
		dout.write((str + "\n").getBytes());
		
		str2 = socketIn.readLine();
		System.out.println("Server: " + str2);
		
		//AUTH client name
		if(str2.equals("OK")) {
			System.out.println("Type your name.");
			str=br.readLine();
			dout.write(("AUTH " + str + "\n").getBytes());
		}
		
		str2 = socketIn.readLine();
		System.out.println("Server: " + str2);
		
		//REDY
		if(str2.equals("OK")) {
			str = "REDY";
			dout.write((str + "\n").getBytes());
		}
		
		str2 = socketIn.readLine();
		System.out.println("Server: " + str2);
		
		//GETS servers
		//keep reading until all packages received
		if(str2.contains("JOBN")) {
			str = "GETS All";
			dout.write((str + "\n").getBytes());
		}
		
		str2 = socketIn.readLine();
		System.out.println("Server: " + str2);
		
		if(str2.contains("DATA")) {
			str = "OK";
			dout.write((str + "\n").getBytes());
			
			int count = Integer.parseInt(str2.substring(5, 6));
			
			for(int i=0; i<count; i++){
				str2 = socketIn.readLine();
				System.out.println(str2);
				Servers.add(str2);
				
				count--;
				System.out.println(count);
			}
			
			str = "OK";
			dout.write((str + "\n").getBytes());
		}
		
		//SCHD job on a particular server
		
		for(String serv : Servers) {
			System.out.println(serv);
		}
		
		while(!str.contains("QUIT")) {
			str=br.readLine();
			dout.write((str + "\n").getBytes());
			str2 = socketIn.readLine();
			System.out.println(str2);
		}
		
		dout.close();
		s.close();
	}
}

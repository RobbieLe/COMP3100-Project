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

        String str = "", str2 = "";
        String state = "";

        while (!str.contains("QUIT")) {
            // Start of protocol
            if (state == "") {
            str = "HELO";
            dout.write((str + "\n").getBytes());
            state = "Initial";
            }

            str2 = socketIn.readLine();
            System.out.println("Server: " + str2);

            // AUTH client name
            if (str2.equals("OK") || state == "Initial") {
                System.out.println("Type your name.");
                str = br.readLine();
                dout.write(("AUTH " + str + "\n").getBytes());
                state = "Authorised";
            }

            str2 = socketIn.readLine();
            System.out.println("Server: " + str2);

            // REDY
            if (str2.equals("OK") || state == "Authorised") {
                str = "REDY";
                dout.write((str + "\n").getBytes());
                state = "Ready";
            }

            str2 = socketIn.readLine();
            System.out.println("Server: " + str2);

            // GETS servers
            // keep reading until all packages received
            if (str2.contains("JOBN") || state == "Ready") {
                str = "GETS All";
                dout.write((str + "\n").getBytes());
                state = "SysInfo";
            }
            
            str2 = socketIn.readLine();
            System.out.println("Server: " + str2);

            // Read server list
            if (str2.contains("DATA") || state == "SysInfo") {
                str = "OK";
                dout.write((str + "\n").getBytes());

                String[] unparsedString = str2.split(" ");
                System.out.println(str2);
                int count = Integer.parseInt(unparsedString[1]);

                for (int i = 0; i < count; i++) {
                    str2 = socketIn.readLine();
                    Servers.add(str2);
                }

                state = "DoneSysInfo";
                str = "OK";
                dout.write((str + "\n").getBytes());
            }

            str2 = socketIn.readLine();
            System.out.println("Server: " + str2);

            // SCHD job on a particular server

            for (String serv : Servers) {
                System.out.println(serv);
            }

            if (state == "DoneSysInfo") {
                str = "QUIT";
                dout.write((str + "\n").getBytes());
                state = "Quitting";
            }
            str2 = socketIn.readLine();
            System.out.println("Server: " + str2);
        }

        if (str.contains("QUIT") || state == "Quitting") {
            dout.close();
            din.close();
            s.close();
        }
    }
}

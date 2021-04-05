import java.net.*;
import java.io.*;
import java.util.ArrayList;

/*
CODE IS HACKED TOGETHER
NO loops so far
Done from top to bottom so there are repeated instructions
*/
public class ds_Client {
    public static void main(String args[])throws Exception{
        Socket s=new Socket("localhost",50000);
        DataInputStream din = new DataInputStream(s.getInputStream());
        DataOutputStream dout = new DataOutputStream(s.getOutputStream());
        BufferedReader inBuffer = new BufferedReader(new InputStreamReader(din));

        String inString="", outString="";   //inString = Received Data, outString = Sent Data
        String state=""; 
        ArrayList <String> serverList = new ArrayList<String>();


        dout.write(("HELO\n").getBytes());      //Send HELO message to server to initate communication
        inString = inBuffer.readLine();     //Read the response from server into inString
        if (inString.contains("OK")) {      //Makes sure the response was "OK"
            outString = "AUTH RobbieLe";        //Sets the message to "AUTH [RANDOM STRING]" following communication protocol
            dout.write((outString + "\n").getBytes());      //Sends the message to server (Should have been AUTH RobbieLe)
            state = "Authorised";       //Sets the state of the connection to Authorised to alert remaining code
        }

        while (!outString.contains("QUIT")) {
            inString = inBuffer.readLine();

            if (state.contains("Authorised") && inString.contains("OK")) {
                outString = "REDY";
                state = "Ready";
            }

            if (state.contains("Ready") && inString.contains("JOBN")) {
                outString = "GETS All";
                dout.write((outString + "\n").getBytes());

                state = "SysInfo";
            }

            if (state.contains("SysInfo") && inString.contains("DATA")) {
                outString = "OK";
                dout.write((outString + "\n").getBytes());

                String [] unparsedString = inString.split(" ");
                int parsedInt = Integer.parseInt(unparsedString[1]);
                
                for (int i = 0; i < parsedInt; i++) {
                    inString = inBuffer.readLine();
                    serverList.add(inString);
                }
                outString = "OK";
                state = "DoneSysInfo";
                dout.write((outString + "\n").getBytes());
            }

            System.out.println(largestServer(serverList));

            if (state.contains("DoneSysInfo")) {
                outString = "QUIT";
                dout.write((outString + "\n").getBytes());

            }

            if (!state.contains("SysInfo")) {
                dout.write((outString + "\n").getBytes());
            }
        }

        inString = inBuffer.readLine();
        if (inString.contains("QUIT")) {
            din.close();
            dout.close();
            s.close();
        }
    }

    public static ArrayList<String> largestServer(ArrayList<String> list) {
        String[] temp1;     //Split up server listing
        String[] temp2;     //
        int cpu1, cpu2, ram1, ram2, storage1, storage2;

        ArrayList<String> temp = new ArrayList<String>();

        for (int i = 0; i < list.size() -1; i++) {
            temp1 = list.get(i).split(" ");
            temp2 = list.get(i+1).split(" ");
            
            cpu1 = Integer.parseInt(temp1[4]);
            cpu2 = Integer.parseInt(temp2[4]);

            ram1 = Integer.parseInt(temp1[5]);
            ram2 = Integer.parseInt(temp2[5]);

            storage1 = Integer.parseInt(temp1[6]);
            storage2 = Integer.parseInt(temp2[6]);

            if (cpu1 > cpu2) {
                temp.set(0, list.get(i));
            } else
            if (cpu1 == cpu2) {
                if (ram1 > ram2) {
                    temp.set(0, list.get(i));
                } else
                if (ram1 == ram2) {
                    if (storage1 > storage2) {
                        temp.set(0, list.get(i));
                    } else
                    temp.add(list.get(i+1));
                }
            }
        }
        return temp;
    }
}
/* 
Server type
ID
Active
-1
CPU
RAM
Storage
*/
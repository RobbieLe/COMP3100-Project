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

        ArrayList<String[]> serverList = new ArrayList<String[]>();     //Holds the list of the servers existing

        String inString = "", outString = "";   //outString (output to server) inString (input to client)
        String state = "";  //state (used to show where in the communication stack, the process is)
        String jobString = "";  //Keeping job assignment in this variable for later use in scheduling
        int serverCount = 0;

        while (state != "Quitting") {       //Looping for the whole communication process until the client is going to quit
            if (state != "") {      //Ensures that it doesn't read anything in for the initial HELO communication
                inString = socketIn.readLine();
            }

            switch (state) {
                case "":        //First communication to the server
                    outString = "HELO";     //Send HELO to initiate communication procotol
                    state = "Initial";      //Change it to Initial to represent that it finished changing the required items for this stage
                    break;
                case "Initial":     //Authorisation with the server
                    System.out.println("Type your name.");      //Send to user, to alert for input of name for AUTH message
                    state = "Authorised";
                    break;
                case "Authorised":      //Telling the server that client is ready to schedule
                    outString = "REDY";
                    state = "Ready";
                    break;
                case "Ready":       //Retrives a list of the servers
                    outString = "GETS All";
                    jobString = inString;       //Keep the job assigned to client for later use
                    state = "SysInfoPrep";
                    break;
                case "SysInfoPrep":     //Get list of servers via DATA header
                    serverCount = getServerCount(inString);     //Get the total amount of servers existing
                    outString = "OK";
                    state = "SysInfoReading";
                    break;
                case "SysInfoReading":       //Gets the list of the servers one-by-one     
                    readSystemList(inString, serverList, socketIn, serverCount);    //Calling this method to add servers to list
                    outString = "OK";
                    state = "DoneSysInfo";
                    break;
                case "DoneSysInfo":     //Finished reading list, Need to implement job scheduling
                    
                    //CHANGE THESE FIELDS BELOW, TO IMPLEMENT SCHEDULING
                
                    outString = "QUIT";     //Change to schedule when implementing it
                    state = "Quitting";     //Change to something more relevant to scheduling
                    break;
                default:
                    System.out.println("Error has occurred");       //Hopefully we don't get here, but here as a safeguard
                    quitCommunication(din, dout, s);        //Close connection, if something bad happens
            }

            if (state == "Authorised") {        //If we are in AUTH stage, ensure we are sending the right message with the User's name
            dout.write(("AUTH " + br.readLine() + "\n").getBytes());
            } else {
            dout.write((outString + "\n").getBytes());      //Send the message set by the switch(outString) with its various states
            }
        }
        
        if (state == "Quitting") {      //Finished with while loop, proceeding with closing connection with server
            quitCommunication(din, dout, s);    //Close connection to finish the communication
        }
    }

    private static int getServerCount(String inString) {        //To allow the ability for readSystemList() to correctly get the list of the servers
        int serverCount;
        String[] unparsedString = inString.split(" ");      //Getting the words in the inString whilst ignore spaces to add into the array
        serverCount = Integer.parseInt(unparsedString[1]);      //Get the number from the inString we just processed aiming for the first integer. Which should have been (DATA "5" 124)

        return serverCount;     //The number of servers existing
    }

    //  (joon           0       active      97          1           15300   60200   0               1) 
    //  (Server-type    ID      State       Start-time  Core-count  Memory  Disk    Waiting-jobs    Running-jobs)
    private static void readSystemList(String inString, ArrayList<String[]> serverList, BufferedReader socketIn, int serverCount) throws IOException {
        String[] temp;
        for (int i = 0; i < serverCount; i++) {     //Looping with result from getServerCount() as limit
            if (i != 0) {       //Ensure we are not reading with the initial loop to prevent skipping records of the server list, since we have already read it with the outer while loop
                inString = socketIn.readLine();     //Read the server record and store into this String
            }
            temp = inString.split(" ");
            serverList.add(temp);       //Adding the server record into the arrayList for future processing (getting the largest server)         
        }
    }

    //  Closing the connections with the server
    private static void quitCommunication(DataInputStream din, DataOutputStream dout, Socket s) throws IOException {
        din.close();
        dout.close();
        s.close();
    }

    //  Return the server with the largest core count 
    //  (joon           0       active      97          1           15300   60200   0               1) 
    //  (Server-type    ID      State       Start-time  Core-count  Memory  Disk    Waiting-jobs    Running-jobs)
    private static String getLargestServer(ArrayList<String[]> serverList) {
        String largestServer[] = serverList.get(0);
        String test[];
        String[] server;

        for (int i = 0; i < serverList.size() -1; i++) {
            test = serverList.get(i+1);
            if (Integer.parseInt(largestServer[4]) < Integer.parseInt(test[4])) {
                largestServer = serverList.get(i+1);
            }
        }
        server = largestServer;
        return server[0] + " " + server[1];        //We want to return the Server-type and ID
    }

    //  Return the Job ID from JOBN message
    //  (JOBN    172         4       320         2       50      120)
    //  (JOBN    submitTime  jobID   estRuntime  core    memory  disk)
    private static String getJobID(String jobString) {
        String jobID = "";
        String[] unparsedString = jobString.split(" ");      //Getting the words in the inString whilst ignore spaces to add into the array

        jobID = unparsedString[2];
        return jobID;   //return the 3rd word of the JOBN message, should be the JOBID
    }
}
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

        String inString = "", outString = "";       //  outString (output to server) inString (input to client)
        String state = "Initial";   //   state (used to show where in the communication stack, the process is)
        String jobString = "";      //  Keeping job assignment in this variable for later use in scheduling
        int serverCount = 0;
        int retryCount = 0;     //  May not be neccessary feel free to remove


        /*  I have commented out the println for Server, State and Client Messages
        //  Just uncomment if you need some debugging
        //  TO PASS THE TEST FILES, IT NEEDS THE AUTOMATED AUTH CODE (LINE 130) JUST UNCOMMENT THAT AND COMMENT OUT LINE 40 & 125
        */

        while (!state.equals("QUIT")) {       //    Looping for the whole communication process until the client is going to quit
            if (state != "Initial") {      //   Ensures that it doesn't read anything in for the initial HELO communication
                inString = socketIn.readLine();
            }
            //System.out.println("Server Said: " + inString);

            switch (state) {
                //  INITAL CONNECTIONS
                case "Initial":        //   First communication to the server
                    outString = "HELO";     //  Send HELO to initiate communication procotol
                    state = "Authorisation";      //    Change it to Initial to represent that it finished changing the required items for this stage
                    break;
                case "Authorisation":     //    Authorisation with the server
                    //System.out.println("Type your name.");      //  Send to user, to alert for input of name for AUTH message
                    state = "Authorised";
                    break;
                case "Authorised":      //  Telling the server that client is ready to schedule
                    outString = "REDY";
                    state = "Ready";
                    break;
                case "Ready":       //  Retrives a list of the servers or ask for new job
                    if (serverCount == 0 && retryCount < 3) {       //  Purpose: To check if we proceed with GETS ALL, only when there is no servers in the list and the loop count for this block is not 3 
                                                                    //  Little back-up that probs ain't required, if you think its not neccessary feel free to remove this entire code block and replace with 
                                                                    //  (outString = "GETS ALL"; state = "SysInfoPrep"; jobString = inString;)
                        outString = "GETS All";
                        state = "SysInfoPrep";
                        retryCount++;
                    } else {
                        if (retryCount == 2)
                            state = "Quitting";     //  Quit the program to prevent spam when loop count is reached
                        else
                            state = "JobScheduling";        //  Just send to job scheduling, was intending to reuse this entire case, but broke away for my own sanity
                    }

                    if (!state.equals("SysInfoPrep")) {     // Only go to this path if we do not have to check the server list
                        if (inString.contains("NONE")) {        //If it had NONE, start the quit process, which does not work properly, sends duplicate REDY before actually
                            outString = "QUIT";
                            state = "Quitting";
                        } else if(inString.contains("JOBN")){       //If it had an actual job to be schedule flip switch to JobSchedule
                            state = "JobScheduling";
                        } else {        //If none of the above, just assume it needs a new job and flip switch to JobSchedule
                        outString = "REDY";
                        state = "JobScheduling";
                        }
                    }

                    jobString = inString;       //Keep the job assigned to client for later use
                    break;
                
                //  SCHEDULING JOBS
                case "JobScheduling":
                    if (inString.contains("JOBN")) { //Check if the message the server sent was indeed the JOBN
                        jobString = inString;       //Keep the job assigned to client for later use
                    }
                    
                    if (inString.contains("NONE")) {    //If the message was NONE, start stopping the program
                        outString = "QUIT";
                        state = "Quitting";
                        break;
                    } else if (inString.contains("JCPL")) {     //  If the server message had JCPL (meaning that a job was completed)
                        if (outString.equals("REDY")) {     /*  
                                                            /   If the message the client was going to send was indeed REDY get the fk out of this case. This was neccessary because of a bug
                                                            /   The bug being if the last message JCPL it tended to send 2 REDY messages, due to the nature of the switch allowing only one case to operate
                                                            /   And the nature of this case, was that it wouldn't change the outString but just the state so it would send out 2 REDY, one for JCPL and one repeated
                                                            /   As it would in the process of just swapping the state during multiple loops
                                                            */ 
                            break;
                        } else
                        state = "NewJob";       //  Send to NewJob to ask for the new JOBN
                    } else {
                    outString = "SCHD " + getJobID(jobString) + " " + getLargestServer(serverList);     //SCHD Message construction
                    state = "Ready";       //  Change state, to then ask for a new job
                    }
                    break;

                //  GETTING SERVER INFORMATION
                case "SysInfoPrep":     //Get list of servers via DATA header
                    serverCount = getServerCount(inString);     //Get the total amount of servers existing
                    outString = "OK";
                    state = "SysInfoReading";
                    break;
                case "SysInfoReading":       //Gets the list of the servers one-by-one     
                    readSystemList(inString, serverList, socketIn, serverCount);    //Calling this method to add servers to list
                    outString = "OK";
                    state = "JobScheduling";
                    break;
                case "DoneSysInfo":     //Finished reading list, Need to implement job scheduling
                    state = "Ready";     //Change to something more relevant to scheduling
                    break;
                
                //In process to close connection, Sending QUIT, then close
                case "Quitting":
                    outString = "QUIT";
                    state = "QUIT";
                    break;
                default:
                    System.out.println("Error has occurred");       //Hopefully we don't get here, but here as a safeguard
                    quitCommunication(din, dout, s);        //Close connection, if something bad happens
            }

            if (state == "Authorised") {        //If we are in AUTH stage, ensure we are sending the right message with the User's name
                //dout.write(("AUTH " + br.readLine() + "\n").getBytes());

                //System.out.println("Client State: " + state);
                //System.out.println("Client Said: " + outString + "\n");
                
                dout.write(("AUTH " + System.getProperty("user.name") + "\n").getBytes());        //THIS WAS THE AUTOMATED AUTH MESSAGE (WE CAN DELETE THIS)

            } else {

                //System.out.println("Client State: " + state);
                //System.out.println("Client Said: " + outString + "\n");

                dout.write((outString + "\n").getBytes());      //Send the message set by the switch(outString) with its various states
            }
        }
        
        if (state == "QUIT") {      //Finished with while loop, proceeding with closing connection with server
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

        for (int i = 0; i < serverList.size() -1; i++) {
            test = serverList.get(i+1);
            if (Integer.parseInt(largestServer[4]) < Integer.parseInt(test[4])) {
                largestServer = test;
            }
        }
		
        return largestServer[0] + " " + largestServer[1];        //We want to return the Server-type and ID
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

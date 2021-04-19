/*
COMP3100
	Joshua Brooks	43603467
	Robinson Le	45948852
*/

import java.net.Socket;
import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;

public class ds_Client {
    public static void main(String[] args) throws Exception {
        Socket s = new Socket("localhost", 50000);
        DataInputStream din = new DataInputStream(s.getInputStream());
        BufferedReader socketIn = new BufferedReader(new InputStreamReader(din));
        DataOutputStream dout = new DataOutputStream(s.getOutputStream());

        ArrayList<String[]> serverList = new ArrayList<String[]>();     //Holds the list of the servers existing

        String inString = "", outString = "";       //  outString (output to server) inString (input to client)
        String state = "Initial";   //  Used to show where in the communication protocol, the process is, set to initial to begin communication
        String jobString = "";      //  Keeping job assignment in this variable for later use in scheduling
        int serverCount = 0;        //Used to 

        while (!state.equals("QUIT")) {       //    Looping for the whole communication process until the client is going to quit
            if (!state.equals("Initial")) {      //   Ensures that it doesn't read anything in for the initial HELO communication
                inString = socketIn.readLine();
            }
            
            switch (state) {
                //  INITAL CONNECTIONS
                case "Initial":        //   First communication to the server
                    outString = "HELO";     //  Send HELO to initiate communication procotol
                    state = "Authorisation";      //    Change it to Authorisation to signal that Initial contact has been
                    break;
                case "Authorisation":     //    Authorisation with the server
                    outString = "AUTH " + System.getProperty("user.name");      //AUTH message with the system username to be sended to the server
                    state = "Authorised";       //Proceed to next stage
                    break;
                case "Authorised":      //  Telling the server that client is ready to schedule
                    outString = "REDY";
                    state = "Ready";
                    break;
                case "Ready":       //  Retrives a list of the servers or ask for new job
                    if (serverCount == 0) {       //  Purpose: To check if we proceed with GETS ALL, only when there is no servers in the list
                        outString = "GETS All";
                        state = "SysInfoPrep";
                    } 

                    if (!state.equals("SysInfoPrep")) {     // Only go to this path if we do not have to check the server list
                        if (inString.contains("NONE")) {        //If it had NONE, start the quit process, which does not work properly, sends duplicate REDY before actually
                            outString = "QUIT";
                            state = "Quitting";
                        } else if(inString.contains("JOBN")){       //If it had an actual job to be schedule flip switch to JobSchedule
                            state = "JobScheduling";
                        } else {        //If none of the above, just assume it needs a new job and flip switch to JobScheduling
                        outString = "REDY";     //  Send this message to request a new job
                        state = "JobScheduling";        //Begin the JobScheduling process
                        }
                    }

                    jobString = inString;       //Keep the job assigned to client for later use
                    break;
                
                //  GETTING SERVER CLUSTER INFORMATION
                case "SysInfoPrep":     //Get number of servers via DATA header (e.g. DATA "5")
                    serverCount = getServerCount(inString);     //Get the total amount of servers existing
                    outString = "OK";       //Reply OK to confirm we received the message
                    state = "SysInfoReading";       //Swap to the next state ("SysInfoReading") to begin next stage (Reading all the specs of the servers)
                    break;
                case "SysInfoReading":      //Gets the list of the servers one-by-one     
                    readSystemList(inString, serverList, socketIn, serverCount);    //Calling this method to add servers to the list serverList
                    outString = "OK";       //Reply OK to confirm we received the message
                    state = "JobScheduling";        //Swap the next state ("JobScheduling") to begin to schedule the job given before
                    break;
                
                //  SCHEDULING JOBS
                case "JobScheduling":
                    if (inString.contains("JOBN")) {        // Check if the message the server sent was indeed the JOBN
                        jobString = inString;       //  Keep the job assigned to client for later use
                    }
                    
                    if (inString.contains("NONE")) {    //If the message was NONE, start stopping the program
                        outString = "QUIT";     //  Change the message we send to QUIT to tell the server we are done
                        state = "Quitting";     //  Begin the quitting process
                        break;  
                    } else if (inString.contains("JCPL")) {     //  If the server message had JCPL (meaning that a job was completed)
                        if (outString.equals("REDY")) {     
                            /*  
                            /   If the message the client was going to send was indeed REDY ensure we do not send another message. 
                            /   This was neccessary because of a bug
                            /
                            /   The bug being if the last message JCPL it tended to send 2 REDY messages, due to the nature of the switch allowing only one case to operate
                            /   And the nature of this case, was that it wouldn't change the outString but just the state so it would send out 2 REDY, one for JCPL and one repeated
                            /   As it would in the process of just swapping the state during multiple loops
                            */ 
                            break;
                        } else
                        state = "NewJob";       //  Send to NewJob to ask for the new JOBN
                    } else {
                        outString = "SCHD " + getJobID(jobString) + " " + getLargestServer(serverList);     //SCHD Message construction
                        state = "Ready";        //  Change state, to then ask for a new job
                    }
                    break;
                
                //  QUITTING PROCESS
                case "Quitting":        
                    outString = "QUIT";     //  Send the QUIT to server to signal we are done
                    state = "QUIT";     //  Allow the escape of this loop
                    break;
                default:
                    System.out.println("Error has occurred");       //  Hopefully we don't get here, but here as a safeguard
                    quitCommunication(din, dout, s);        //  Close connection, if something bad happens
            }       //  End of Switch(state)

            dout.write((outString + "\n").getBytes());      //  Send the message set by the switch(outString) with its various states to represent the stage of the protocol it reached
                                                            //  Message also contains the newline character need to talk with ds-server
        }
        
        if (state.equals("QUIT")) {      //Finished with while loop, proceeding with closing connection with server
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
        din.close();        //  Close InputBufferStream
        dout.close();       //  Close OutputBufferStream
        s.close();      //      Close Socket with port 50000 as specified above
    }

    //  Return the server with the largest core count 
    //  (joon           0       active      97          1           15300   60200   0               1) 
    //  (Server-type    ID      State       Start-time  Core-count  Memory  Disk    Waiting-jobs    Running-jobs)
    private static String getLargestServer(ArrayList<String[]> serverList) {
        String largestServer[] = serverList.get(0);     //  Creating a string array to store the information of the current largest server
        String test[];      //  Create a second string array to hold what we are going to compared largestServer[] to

        for (int i = 0; i < serverList.size() -1; i++) {        //  Loop with serverList
            test = serverList.get(i+1);     //  Have test have a copy of the server details one interation ahead
            if (Integer.parseInt(largestServer[4]) < Integer.parseInt(test[4])) {       //  Compare the Core-Count in the String which will be parsed to an int
                largestServer = test;       //  If the test had the server with more counts, swap largestServer to that
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

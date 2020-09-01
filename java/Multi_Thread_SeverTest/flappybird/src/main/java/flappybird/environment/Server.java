package flappybird.environment;

/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import java.util.*;
import java.net.*;
import java.io.*;
import org.json.*;

public class Server {
    public static void main(String[] args) throws IOException {
        
        final int portNumber = 50007;
        
        ServerSocket serverSocket = new ServerSocket(portNumber);
        System.out.println("SeverSocket Created Sucessfully.");
        while(true) {
        	Socket clientSocket = null;
        	try {
        		clientSocket = serverSocket.accept();
        		if (clientSocket != null)
                	System.out.println("ClientSocket " + clientSocket.toString() + "Connected Sucessfully.");
                else
                	System.exit(-1);
        		PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);                   
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                
                System.out.println("Assigning new thread for this client"); 
                Thread t = new ClientHandler(clientSocket, in, out); 
                t.start();
        	}
        	catch(Exception e){
        		clientSocket.close();
        		serverSocket.close();
        	    System.out.println("End of servive");
        	    e.printStackTrace();
        	}
        }
    }
}

class ClientHandler extends Thread{
	final Socket s;
	final BufferedReader in;
	final PrintWriter out;

	Environment env = new Environment();
	List<Double> states = new ArrayList<>();
	Double cumulativeReward = 0.0;
	
	int step = 1;
	
	public ClientHandler(Socket s, BufferedReader in, PrintWriter out)
    { 
        this.s = s; 
        this.in = in; 
        this.out = out;
    } 
	
	@Override
	public void run() {
		String inputLine = "";
		boolean is_done = false;
        System.out.println("Start Process logic.");
        
        //Return variables
        String outStr = "";
        final int state_size = 8;
        final int action_size = 2;
        
        while(!is_done)
        {
        	try {
				inputLine = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
				is_done = true;
			}
        	System.out.println(inputLine);
        	JSONObject inputJSON = new JSONObject(inputLine);
        	String request = inputJSON.getString("request");
        	
            states.add(env.getGameState().birdY);
            states.add(env.getGameState().birdVelocity);

    		states.add(env.getGameState().nextPillarDistToBird);
    		states.add(env.getGameState().nextPillarTopY);
    		states.add(env.getGameState().nextPillarBottomY);

    		states.add(env.getGameState().nextNextPillarDistToBird);
    		states.add(env.getGameState().nextNextPillarTopY);
    		states.add(env.getGameState().nextNextPillarBottomY);
        	
        	switch(request) {
        		case "init":
        			System.out.println("Request: init");
        			outStr = String.format("{\"state_size\": %d, \"action_size\": %d}", state_size, action_size);
        			out.println(outStr);
        			break;
        		case "reset":
        			System.out.println("Request: reset");
        			outStr = String.format("{\"state\": [%.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f]}",
        					states.get(0),states.get(1),states.get(2),states.get(3),
        					states.get(4),states.get(5),states.get(6),states.get(7)
        					);
        			out.println(outStr);
        			states.clear();
       				break;
       			case "act":
        			System.out.println("Request: act");
        			if(!env.gameOver()) {
       					System.out.println(String.format("----------step %d----------", step));
        					
       					// print current state (For RL)
       					System.out.println("game state(RL):");
       					System.out.println(env.getGameState());

        				// print current state (For human readable)
        				// don't train with this information
        				System.out.println("--------------------");
        				System.out.println("bird and pillars information(human):");
        				env.printState();
       					
        				String act = inputJSON.getString("action");
        				if(act == "flap")
        					cumulativeReward += env.act(Action.FLAP);
        				else
        					cumulativeReward += env.act(Action.NONE);
        				System.out.println("--------------------");
       					System.out.println(String.format("your action performed in this step: %s", act));

        				System.out.println("--------------------");
        				System.out.println("Cumulative Reward: " + cumulativeReward);
        				System.out.println("--------------------\n");
        				step += 1;
        			}
        			
        			//Process State info
        			outStr = String.format("{\"state\": [%.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f], ",
        					states.get(0),states.get(1),states.get(2),states.get(3),
        					states.get(4),states.get(5),states.get(6),states.get(7)
        					);
        			String temp_reward = String.format("\"reward\": %.3f, ",cumulativeReward);
        			String temp_done = is_done ? "\"is_done\": \"True\"," : "\"is_done\": \"False\", ";
        			String temp_info = "\"info\": \"none\"}";
        			outStr += temp_reward;
        			outStr += temp_done;
        			outStr += temp_info;
        			out.println(outStr);
        			states.clear();
        			break;
        		default:
        			break;
        		}
        }
        try {
        	this.in.close();
            this.out.close();
            this.s.close();
        }
        catch(IOException e){
        	e.printStackTrace(); 
        }
        
	}
}

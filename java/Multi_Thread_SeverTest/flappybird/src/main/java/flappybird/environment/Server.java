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
import java.nio.file.*;
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
        		OutputStream  out = clientSocket.getOutputStream();                   
        		InputStream in = clientSocket.getInputStream();
                
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
	final InputStream in;
	final OutputStream out;

	Environment env = new Environment();
	List<Double> states = new ArrayList<>();
	byte[] messageByte = new byte[1024];
	Double cumulativeReward = 0.0;
	
	int step = 1;
	boolean isFinished = false;
	
	public ClientHandler(Socket s, InputStream in, OutputStream out)
    { 
        this.s = s; 
        this.in = in; 
        this.out = out;
    } 
	
	@Override
	public void run() {
		String inputLine = "";
		boolean is_done = false;
		double Max_Reward = -5.0;
        System.out.println("Start Process logic.");
        
        //Return variables
        String outStr = "";
        final int state_size = 8;
        final int action_size = 2;
        double action_reward = 0.0;
        //Elapsed Timer
        long start = 0, stop = 0;
        long current = 0;
        /*
        long avgMean = 0, innerAvgMean = 0, current = 0;
        double avgMeanInSec = 0.0, innerAvgMeanInSec = 0.0;
        */
        //File Manage
        int action_count = 0;
        String folderName = "./avgMean";
        Path foldrPath = Paths.get(folderName);
        try {
        	if(!Files.exists(foldrPath)) {
        		Files.createDirectory(foldrPath);
        		System.out.println("CSV Directory created");
        	}
        	else
        		System.out.println("CSV Directory already exists");
            
        }
        catch(IOException e){
        	e.printStackTrace();
        }
        String threadID = "Thread-" + Thread.currentThread().getId();
        String csvName = "./avgMean/" + threadID +".csv";
        String csvName1000 = "./avgMean/" + threadID +"_1000.csv";
        try {
        	Files.write(Paths.get(csvName), "Transmission Mean Time(ms), Sever InnerTime(ms)\n".getBytes());
        	Files.write(Paths.get(csvName1000), "Transmission Mean Time/1000 acts (ms), Sever InnerTime(ms)\n".getBytes());
        }
        catch (IOException e) {
			e.printStackTrace();
		}

        
        while(!isFinished)
        {
        	try {
        		int length;
        		length = in.read(messageByte);
        		stop = System.nanoTime();
        		if(start != 0)
        			current = stop - start;
        		/*
            	avgMean = current/20 + avgMean*19/20;
            	avgMeanInSec = (double) avgMean/1000000;
            	//System.out.println(avgMeanInSec + "s");
            	String fout = avgMeanInSec + ",";
            	*/
        		double cur2double = (double) current/1000000;
        		String fout = cur2double + ",";
            	Files.write(Paths.get(csvName), fout.getBytes(), StandardOpenOption.APPEND);
            	if(action_count++ >= 1000){
            		Files.write(Paths.get(csvName1000), fout.getBytes(), StandardOpenOption.APPEND);
            		action_count = 0;
            	}
        		inputLine = new String(messageByte, 0, length);
			} catch (IOException e) {
				e.printStackTrace();
				isFinished = true;
			}
        	
        	
        	
        	
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
        			System.out.println(outStr);
        			try {
        				out.write(outStr.getBytes());
            			out.flush();
            			start = System.nanoTime();
        			}
        			catch(IOException e){
        				e.printStackTrace();
        			}
        			break;
        		case "reset":
        			//System.out.println("Request: reset");
        			env.init();
        			outStr = String.format("{\"state\": [%.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f]}",
        					states.get(0),states.get(1),states.get(2),states.get(3),
        					states.get(4),states.get(5),states.get(6),states.get(7)
        					);
        			//System.out.println(outStr);
        			try {
        				out.write(outStr.getBytes());
            			out.flush();
            			start = System.nanoTime();
        			}
        			catch(IOException e){
        				e.printStackTrace();
        			}
        			states.clear();
       				break;
       			case "act":
        			if(!env.gameOver()) {
        				int act = inputJSON.getInt("action");
        				if( Integer.compare(act, 0) == 0 )
        				{
        					action_reward = env.act(Action.FLAP);
        					cumulativeReward += action_reward;
        				}
        					
        				else {
        					action_reward = env.act(Action.NONE);
        					cumulativeReward += action_reward;
        				}
        				
        				if (cumulativeReward > Max_Reward) {
        					Max_Reward = cumulativeReward;
        					System.out.println(String.format("----------New Record Reward: %.3f----------", cumulativeReward));
        					System.out.println(String.format("----------step %d----------", step));
        					
           					// print current state (For RL)
           					System.out.println("game state(RL):");
           					System.out.println(env.getGameState());

            				// print current state (For human readable)
            				// don't train with this information
            				System.out.println("--------------------");
            				System.out.println("bird and pillars information(human):");
            				env.printState();
            						
            				System.out.println("--------------------");
           					System.out.println(String.format("your action performed in this step: %s", act));

            				System.out.println("--------------------");
            				System.out.println("Cumulative Reward: " + cumulativeReward);
            				System.out.println("--------------------\n");
        				}
        				step += 1;
        			}
        			else{
        				is_done = true;
        				step = 1;
        				cumulativeReward = 0.0;
        				action_reward = 0.0;
        			}
        			//Process State info
        			outStr = String.format("{\"state\": [%.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f, %.3f], ",
        					states.get(0),states.get(1),states.get(2),states.get(3),
        					states.get(4),states.get(5),states.get(6),states.get(7)
        					);
        			String temp_reward = String.format("\"reward\": %.3f, ",action_reward);
        			String temp_done = is_done ? "\"is_done\": \"True\"," : "\"is_done\": \"False\", ";
        			String temp_info = "\"info\": \"none\"}";
        			outStr += temp_reward;
        			outStr += temp_done;
        			outStr += temp_info;
        			//System.out.println(outStr);
        			try {
        				out.write(outStr.getBytes());
            			out.flush();
            			start = System.nanoTime();
        			}
        			catch(IOException e){
        				e.printStackTrace();
        			}
        			states.clear();
        			is_done = false;
        			break;
        		default:
        			break;
        		}
        	/*
        	innerAvgMean = (start - stop)/20 + innerAvgMean*19/20;
        	innerAvgMeanInSec = (double) innerAvgMean/1000000;
        	*/
        	double fout = (double) (start - stop)/1000000;
        	try {
        		Files.write(Paths.get(csvName), (fout +"\n").getBytes(), StandardOpenOption.APPEND);
            	if(action_count++ >= 1000){
            		Files.write(Paths.get(csvName1000), (fout +"\n").getBytes(), StandardOpenOption.APPEND);
            		action_count = 0;
            	}
        	}
        	catch(IOException e){
				e.printStackTrace();
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

package searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import searchclient.Memory;
import searchclient.Strategy.*;
import searchclient.Heuristic.*;
import searchclient.ElementWithColor.*;



public class SearchClient {
	public Node initialState;
	public static int agentCount;
	public HashMap<Character, Color> colorAssignments;
	
	public static enum StrategyType {
		bfs, dfs, astar, wastar, greedy
	}

	public SearchClient(BufferedReader serverMessages) throws Exception {
		List<String> lines = new ArrayList<String>();
	
		
		colorAssignments = new HashMap<Character, Color>();
		
		// Read lines specifying colors
		String fileline = serverMessages.readLine();
		while(fileline.matches("^[a-z]+:\\s*[0-9A-Z](\\s*,\\s*[0-9A-Z])*\\s*$")){
			lines.add(fileline);
			fileline = serverMessages.readLine();
		}		
		
		for (String line : lines) {
			Pattern pattern = Pattern.compile("([a-z]+)");
			Matcher matcher = pattern.matcher(line);
			
			if(matcher.find()){
				String color = matcher.group(1);
				Color cColor;
				
				try {
					cColor = Color.valueOf(color);
					
					pattern = Pattern.compile("([0-9A-Z])");
					matcher = pattern.matcher(line);
					while (matcher.find()){
						//System.err.println(matcher.group(1).charAt(0));
						colorAssignments.put(matcher.group(1).charAt(0), cColor);
					}
				} catch (IllegalArgumentException e){
					System.err.println("Invalid color");
					System.exit(1);
				}
			}
		}
		lines.clear();
		
		// Read lines specifying the layout of the level
		int rows = 0;
		int cols = 0;
		
		int lineLength;
		
		while (!fileline.equals("")) {
			lineLength = fileline.length();
			if (lineLength > cols)
				cols = lineLength;
			
			rows++;
			
			lines.add(fileline);
			
			fileline = serverMessages.readLine();
		}

		int row = 0;
		boolean[] agentFound = new boolean[10];
		
		for (String line : lines) {
			for (int col = 0; col < line.length(); col++) {
				char chr = line.charAt(col);
				if ('0' <= chr && chr <= '9') { // Agent.
					
					if(!colorAssignments.containsKey(chr)) {
						// Adds the character with color blue to the color map if it has not been assigned any color
						colorAssignments.put(chr, Color.blue);
					}
					
					int agentNo = Character.getNumericValue(chr);
					
					if (agentFound[agentNo]) {
						System.err.println("Error, multiple agents with the same number");
						System.exit(1);
					}
					
					agentFound[agentNo] = true;
					agentCount++;
				}
			}
			row++;
		}
		
		this.initialState = new Node(null, rows, cols, agentCount);
		
		row = 0;

		for (String line : lines) {
			for (int col = 0; col < line.length(); col++) {
				char chr = line.charAt(col);

				if (chr == '+') { // Wall.
					this.initialState.walls[row][col] = true;
				} else if ('0' <= chr && chr <= '9') { // Agent.
					
					int agentNo = Character.getNumericValue(chr);
					
					this.initialState.agents[agentNo][0] = row;
					this.initialState.agents[agentNo][1] = col;
					
				} else if ('A' <= chr && chr <= 'Z') { // Box.
					if(!colorAssignments.containsKey(chr)) {
						// Adds the character with color blue to the color map if it has not been assigned any color
						colorAssignments.put(chr, Color.blue);
					}
					this.initialState.boxes[row][col] = chr;
					
				} else if ('a' <= chr && chr <= 'z') { // Goal.
					this.initialState.goals[row][col] = chr;
				} else if (chr == ' ') {
					// Free space.
				} else {
					System.err.println("Error, read invalid level character: " + (int) chr);
					System.exit(1);
				}
			}
			row++;
		}
		this.initialState.setcolormap(colorAssignments);
	}

	HashMap<Integer, LinkedList> hmap = new HashMap<Integer, LinkedList>();
	int[] plancounter ;

	public LinkedList<String> Search(StrategyType strategyType, SearchClient client) throws IOException {
		//system.err.format("Search starting with strategy %s.\n", strategyType);
		
		int longestPlanSize = 0;
		
		HashMap<Integer, LinkedList<Node>> agentPlans = new HashMap<Integer, LinkedList<Node>>();
		
		for(int agentNo = 0; agentNo < this.agentCount; agentNo++){
			Strategy strategy = createStrategy(strategyType, client);
			this.initialState.agentNo = agentNo;
			strategy.addToFrontier(this.initialState);
			
			//system.err.println("Agent: " + agentNo);
			LinkedList<Node> planForAgent = searchForAgent(strategy, agentNo);
			//system.err.println(agentNo + " has plan of size " + planForAgent.size());
			agentPlans.put(agentNo, planForAgent);
			
			if(planForAgent.size() >= longestPlanSize) {
				longestPlanSize = planForAgent.size();
			}
		}
		
	
	
		
			for(int agent = 0; agent < this.agentCount; agent++) {	    
			 hmap.put(agent, new  LinkedList<String>()); /// add empty cs linked for each agent
			}
	    
			/// add route coordinates to corresponding linked list
		    for(int step = 0; step < longestPlanSize; step++) {
		    	for(int agent = 0; agent < this.agentCount; agent++) {
		    		String newagentpos=""  ;
		    		if(step < agentPlans.get(agent).size()){
		    			int newagentrow = agentPlans.get(agent).get(step).agents[agent][0]; // agent row
			    		int newagentcol = agentPlans.get(agent).get(step).agents[agent][1]; // agent col
			    		newagentpos = newagentrow+"-"+newagentcol ;
		    		}
		    		else {
		    			newagentpos = "-" ;
		    		}
		    		
		    		hmap.get(agent).add(newagentpos);
		    	}
		    }
		    
		    for(int agent = 0; agent < this.agentCount; agent++) {	    
				for(int j=0 ; j< hmap.get(0).size() ; j++){
					System.err.print(hmap.get(agent).get(j)+", ");
				}
				System.err.println(" ");
			}
			
		    // select pair of routes  
		    
			 // for every pair find critical section
			      // assume we have only one pair
                  // find critical cells
		             /// implement solution for many critical sections
		    
		    
		    LinkedList<String> criticalcells = new  LinkedList<String>();
		    int usingcs = -1 ;
		     
		       /// add critical cells to critical section
			   for(int walker1=0; walker1<hmap.get(0).size();walker1++){
				   for(int walker2=0; walker2<hmap.get(0).size(); walker2++){
					   if((hmap.get(0).get(walker1).equals(hmap.get(1).get(walker2)) && hmap.get(1).get(walker1).equals(hmap.get(0).get(walker2)))
							   ||  (hmap.get(0).get(walker2).equals(hmap.get(1).get(walker2)) )
							   ){
						   //system.err.println(hmap.get(0).get(walker1)); // to be removed
						   criticalcells.add(hmap.get(0).get(walker1).toString());
					   }
				   }
			   }
			   
			   
		    
		    
		LinkedList<String> jointActions = new LinkedList<String>();
		boolean readyToleave = false ;
		int readyincounter = 0;
		
		plancounter = new int[this.agentCount];
		int[] planssteps = new int[this.agentCount];
		
		/// set all plancounter entries to largestPlanSize
		for(int i=0;i<plancounter.length;i++){
			plancounter[i] =  hmap.get(0).size() ;
		}
		
		// set all planssteps to 0
		for(int i=0;i<planssteps.length;i++){
			planssteps[i] =  0 ;
		}
		
	  
		
		
		while(arepAllPlanCounterEntriesnull()==false){
			
			String jointAction = "[";
			
			for(int agent=0; agent<this.agentCount; agent++){
				
				// get agent pos
			
                if(planssteps[agent]>= agentPlans.get(agent).size()){
                	jointAction += "NoOp";
	    		}
                
	    		else {
	    			
					int newagentrow = agentPlans.get(agent).get(planssteps[agent]).agents[agent][0]; // agent row
		    		int newagentcol = agentPlans.get(agent).get(planssteps[agent]).agents[agent][1]; // agent col
		    		String newagentpos = newagentrow+"-"+newagentcol ;
		    		
	    			
	    		if(criticalcells.getLast().equals(newagentpos) &&  usingcs==agent ){
	    			
	    			jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();			
	    		    planssteps[agent]++;
	    		    plancounter[agent]--;
	    		    readyToleave = true ;
	    			System.err.print(" agent "+agent+" leaves critical section");
	    		}
	    		else if(criticalcells.getFirst().equals(newagentpos) && usingcs==-1){
	    			usingcs = agent ;
	    			System.err.print(" agent "+agent+" is in the critical section");
	    			jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();
	    			 planssteps[agent]++;
	    			 plancounter[agent]--;
	    			
	    		}
	    		
	    		else if(usingcs==agent){
	    			jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();
	    			 planssteps[agent]++;
		    		 plancounter[agent]--;
	    		}
	    		
	    		else if(usingcs!=-1 && usingcs!=agent){
	    			jointAction += "NoOp";
	    			
	    		}
	    		
	    		else {
	    			jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();
	    			 planssteps[agent]++;
		    		 plancounter[agent]--;
	    		}
	    		
			}
	    		
	    		if(agent < this.agentCount - 1) {	// Add commas for all cases except the last one.
	    			jointAction += ",";
	    		}
	    		
			}
			
			if(readyToleave==true && readyincounter<2){
				readyincounter++;
			}
			else if(readyincounter==2){
				usingcs = -1 ;
				readyincounter = 0;
				readyToleave=false;
			}
			
			jointAction += "]";
			System.err.println(jointAction);
	    	jointActions.add(jointAction);
			
		}
		 
		
		/*
		 * int recoverystep = 10000000 ;
	    for(int step = 0; step < longestPlanSize; step++) {
	    	
	    	String jointAction = "[";
	    	for(int agent = 0; agent < this.agentCount; agent++) {
	    		//system.err.println("YEAH");
	    	
	    		if(step < agentPlans.get(agent).size()){

	    			int newagentrow = agentPlans.get(agent).get(step).agents[agent][0]; // agent row
		    		int newagentcol = agentPlans.get(agent).get(step).agents[agent][1]; // agent col
		    		String newagentpos = newagentrow+"-"+newagentcol ;
		    		
		    	
		    		if(criticalcells.getLast().equals(newagentpos) &&  usingcs==agent ){
		    			usingcs = -1 ;
		    			jointAction += agentPlans.get(agent).get(step).action.toString();
		    			step = recoverystep ;
		    			
		    		}
		    		
		    		else if(criticalcells.getFirst().equals(newagentpos) && (usingcs==-1 || usingcs==agent )){
		    			usingcs = agent ;
		    			jointAction += agentPlans.get(agent).get(step).action.toString();
		    		}
		    		else if(usingcs!=-1 && usingcs!=agent){
		    			if(step<recoverystep) recoverystep = step ;
		    			//system.err.print("step="+recoverystep);
		    			jointAction += "NoOp";
		    		}
		    		else{
		    			jointAction += agentPlans.get(agent).get(step).action.toString();
		    		}
	    			
	    		}
	    		else {
	    			jointAction += "NoOp";
		    	
	    		}
	    	
	    		
	    	
	    
	    		
	    		
	    		if(agent < this.agentCount - 1) {	// Add commas for all cases except the last one.
	    			jointAction += ",";
	    		}
	    	}
	    	jointAction += "]";
	    	jointActions.add(jointAction);
	    }
	    */
		
	//print agents route based on their location to be removed
	    for(int k=0; k<hmap.size(); k++){

	    	for(int j=0; j<hmap.get(k).size() ; j++){
	    		
	    		//system.err.print(hmap.get(k).get(j)+", ");
	    		
	    	}
	    	//system.err.println(" ");
	    }
	    
		
	
	   
	    
		return jointActions;
	}
	
	
	public boolean arepAllPlanCounterEntriesnull(){
		
		for(int i=0; i<plancounter.length; i++){
			if(plancounter[i]!=0) return false ;
		}
		
		return true ;
	}
	
	public Strategy createStrategy(StrategyType searchType, SearchClient client) {
		switch(searchType) {	//bfs, dfs, astar, wastar, greedy
			case bfs:
				return new StrategyBFS();
			case dfs:
				return new StrategyDFS();
			case astar:
				return new StrategyBestFirst(new AStar(client.initialState));
			case wastar:
				return new StrategyBestFirst(new WeightedAStar(client.initialState, 5));
			case greedy:
				return new StrategyBestFirst(new Greedy(client.initialState));
			default:
				return new StrategyBFS();
		}
	}
	
	public LinkedList<Node> searchForAgent(Strategy strategy, int agentNo) {
		int iterations = 0;
		while (true) {
            if (iterations == 1000) {
				System.err.println(strategy.searchStatus());
				iterations = 0;
			}

			if (strategy.frontierIsEmpty()) {
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();

			if (leafNode.isGoalState(agentNo)) {
				System.err.println("FOUND!!!");
				return leafNode.extractPlan();
			}

			strategy.addToExplored(leafNode);
			
			//System.err.println(leafNode.actions[0].toString() + "," + leafNode.actions[1].toString());
			
			
			
			
			for (Node n : leafNode.getExpandedNodes(agentNo)) { // The list of expanded nodes is shuffled randomly; see Node.java.
				
				//System.err.println(!strategy.isExplored(n) + "," + !strategy.inFrontier(n));
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
					//System.err.println(n.agents[0][0] + "," + n.agents[0][1]);
					strategy.addToFrontier(n);
					try{
						System.err.println("Added n with: " + n.action.toString());
						System.err.println(n.isGoalState(agentNo));
					}
					catch(NullPointerException e){
						 System.err.print("NoOp"); 
					}
					
//					if(n.isGoalState(agentNo)){
//						System.err.println("BREAK!");
//						break;
//					}
					/*try{
						System.err.println(n.action.toString());
					}
					 catch(NullPointerException e){
						 System.err.println("NoOp"); 
					}*/
				}
			}
			iterations++;
		}
		
		
		
	}

	public static void main(String[] args) throws Exception {
		BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

		// Use stderr to print to console
		System.err.println("SearchClient initializing. I am sending this using the error output stream.");

		// Read level and create the initial state of the problem
		SearchClient client = new SearchClient(serverMessages);

		StrategyType strategyType = null;
		
        Strategy strategy;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "-bfs":
                	strategyType = strategyType.bfs;
                    break;
                case "-dfs":
                	strategyType = strategyType.dfs;
                    break;
                case "-astar":
                	strategyType = strategyType.astar;
                    break;
                case "-wastar":
                    // You're welcome to test WA* out with different values, but for the report you must at least indicate benchmarks for W = 5.
                	strategyType = strategyType.wastar;
                    break;
                case "-greedy":
                	strategyType = strategyType.greedy;
                    break;
                default:
                	strategyType = strategyType.bfs;
                    System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
        	strategyType = strategyType.bfs;
            System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
        }
        
		LinkedList<String> solution;
		try {
			solution = client.Search(strategyType, client);
		} catch (OutOfMemoryError ex) {
			System.err.println("Maximum memory usage exceeded.");
			solution = null;
		}

		if (solution == null) {
			//System.err.println(strategy.searchStatus());
			System.err.println("Unable to solve level.");
			System.exit(0);
		} else {
			//System.err.println("\nSummary for " + strategy.toString());
			System.err.println("Found solution of length " + solution.size());
			//System.err.println(strategy.searchStatus());

			for (String s : solution) {	// Create separate object?
//				String act = "[";
//				try{
//					act += n.action.toString();
//				}
//				 catch(NullPointerException e){
//					act += "NoOp"; 
//				 }
//				for(int i = 1; i < n.agentCount; i++){
//					try{
//						act += "," + n.action.toString();
//					}
//					 catch(NullPointerException e){
//						act += ",NoOp"; 
//					 }
//				}
//				act += "]";
				System.err.println(s);
				System.out.println(s);
				String response = serverMessages.readLine();
				if (response.contains("false")) {
					System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, s);
					//System.err.format("%s was attempted in \n%s\n", s, n.toString());
					break;
				}
			}
			RoomDetector rd = new RoomDetector();
			int[][] roomRegions = rd.detectRooms(client.initialState.walls, client.initialState.rows, client.initialState.cols,
						   						 client.initialState.agents[0][0], client.initialState.agents[0][1]);
			
			
			for(int i = 0; i < client.initialState.rows; i++){
				for(int j = 0; j < client.initialState.cols; j++){
					System.err.print(roomRegions[i][j]);
				}
				System.err.println("");
			}
		}
	}
}

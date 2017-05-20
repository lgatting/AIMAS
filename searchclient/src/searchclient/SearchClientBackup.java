package searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.*;



import searchclient.Memory;
import searchclient.Strategy.*;
import searchclient.Heuristic.*;
import searchclient.ElementWithColor.*;
import solvers.DeadEndCorridorSolverV2;
import solvers.Lewis;
import solvers.PositionPenalizer;



public class SearchClientBackup {
	public Node initialState;
	public static int agentCount;
	public HashMap<Character, Color> colorAssignments;
	HashMap<Integer, LinkedList> hmap = new HashMap<Integer, LinkedList>();
	int[] plancounter;
	
	private Set<Goal> discoveredGoals;
	private Set<Box> discoveredBoxes;
	private Set<Agent> discoveredAgents;
	
	public Perception perception;
	
	public HashMap<Integer, Node> agentBeliefs = new HashMap<Integer, Node>();
	
	public String[] agentsAction;	// An array of the agents' next actions
	
	public static enum StrategyType {
		bfs, dfs, astar, wastar, greedy
	}

	/**
	 * Reads a level from a file and accordingly sets up internal data structure representing that level. 
	 * @param serverMessages
	 * @throws Exception
	 */
	public SearchClientBackup(BufferedReader serverMessages) throws Exception {
		List<String> lines = new ArrayList<String>();
	
		colorAssignments = new HashMap<Character, Color>();

		discoveredGoals = new HashSet<Goal>();
		discoveredBoxes = new HashSet<Box>();
		discoveredAgents = new HashSet<Agent>();
		
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
						//System.err.println("Error, multiple agents with the same number");
						System.exit(1);
					}
					
					discoveredAgents.add(new Agent(agentNo, colorAssignments.get(chr)));
					
					agentFound[agentNo] = true;
					agentCount++;
				}
			}
			row++;
		}
		
		this.initialState = new Node(null, rows, cols, agentCount);
		
		row = 0;
		
		int nextBoxId = 1;
		int nextGoalId = 1;

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
					this.initialState.boxIds[row][col] = nextBoxId++;
					this.discoveredBoxes.add(new Box(this.initialState.boxIds[row][col], chr, colorAssignments.get(chr)));
					
				} else if ('a' <= chr && chr <= 'z') { // Goal.
					this.initialState.goals[row][col] = chr;
					this.initialState.goalIds[row][col] = nextGoalId++;
					this.discoveredGoals.add(new Goal(this.initialState.goalIds[row][col], chr));
				} else if (chr == ' ') {
					// Free space.
				} else {
					//System.err.println("Error, read invalid level character: " + (int) chr);
					System.exit(1);
				}
			}
			row++;
		}
		this.initialState.setcolormap(colorAssignments);
		
		agentsAction = new String[agentCount];
		
		// The below creates an initial perception of the level that will be used to update each agent's perception of the level.
		this.perception = new Perception(this.initialState.rows, this.initialState.cols, this.initialState.agentCount,
										 this.initialState.boxes, this.initialState.boxIds, this.initialState.agents);
	}
	
	/**
	 * Generates a plan of high level actions for given agent.
	 * @param agent
	 * @return
	 */
	private List<HighLevelAction> generateHLAPlan(Agent agent) {
		List<HighLevelAction> plan = new ArrayList<HighLevelAction>();
		// Uncomment this section to play around with the corridor solver and comment out the for-loop that follows this comment
		List<Box> orderedBoxes = new ArrayList<Box>();
		
		for (Box box : agent.boxes) {
			orderedBoxes.add(box);
		}
		DeadEndCorridorSolverV2 decsv2 = new DeadEndCorridorSolverV2(discoveredGoals, initialState);
		List<Goal> orderedGoals = decsv2.orderGoals();
		
		decsv2.printDependancyMatrix();

		Lewis l = new Lewis(discoveredGoals, initialState);
		l.solve();
		
		//PositionPenalizer pp = new PositionPenalizer(discoveredGoals, initialState);
		//pp.solve();
		
		System.err.println("Discovered order of goals using corridor solver: " + orderedGoals);
		
		System.err.println();

		Collections.sort(orderedBoxes);
		Collections.reverse(orderedBoxes);
		
		for (Box box : orderedBoxes) {
			//System.err.println("ADDED");
			plan.add(new GoToHLA(box));
			plan.add(new SatisfyGoalHLA(box, box.goal));
		}
		
		/*for (Box box : agent.boxes) {
			plan.add(new GoToHLA(box));
			plan.add(new SatisfyGoalHLA(box, box.goal));
		}*/
		
		return plan;
	}
	
	/**
	 * Returns the agent object with given agent number/ID.
	 * @param agentNo
	 * @return
	 */
	private Agent getAgentObject(int agentNo) {
		for (Agent agent : discoveredAgents)
			if (agent.id == agentNo)
				return agent;
		
		return null;
	}

	/**
	 * Finds plans for all agents and returns them as a dictionary where the key is the agents' ID and value is the plan itself.
	 * @param strategyType
	 * @param client
	 * @return
	 */
	private void discoverAgentPlans(StrategyType strategyType, SearchClientBackup client) {
		for (int agentNo = 0; agentNo < agentCount; agentNo++) {
			Strategy strategy = createStrategy(strategyType, client);
			
			Node copy = this.initialState.copyOfNode();
			
			copy.plannedActions = generateHLAPlan(getAgentObject(agentNo));
			
			System.err.println("HLA plan for agent " + agentNo + ": " + copy.plannedActions);
			
			copy.agentNo = agentNo;
			copy.strategy = strategy;
			
			System.err.println("Creating a relaxed plan for agent " + agentNo);
			
			copy.relaxNode();
			
			agentBeliefs.put(agentNo, copy);			
		}
	}
	
	/**
	 * Returns the length of the longest plan amongst all agents. 
	 * @param plans Dictionary produced by the discoverAgentPlans method.
	 * @return Length of the longest plan.
	 */
	private int longestPlanSize(HashMap<Integer, LinkedList<Node>> plans) {
		int result = 0;
		
		for (LinkedList<Node> plan : plans.values())
			if (plan.size() > result)
				result = plan.size();
		
		return result;
	}
	
	/**
	 * Takes the generated agent plans and resolves any conflicts that may have occurred.
	 * @param agentPlans
	 * @return An array of strings where each string is a representation of joint action ready to be sent to a server.
	 */
	private LinkedList<String> resolveConflicts(HashMap<Integer, LinkedList<Node>> agentPlans) {
		for(int agent = 0; agent < agentCount; agent++) {	    
			hmap.put(agent, new  LinkedList<String>()); /// add empty cs linked for each agent
		}
		
		int longestPlanSize = longestPlanSize(agentPlans);
    
		/// add route coordinates to corresponding linked list
	    for(int step = 0; step < longestPlanSize; step++) {
	    	for(int agent = 0; agent < agentCount; agent++) {
	    		String newagentpos = "";
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
	    
	    for(int agent = 0; agent < agentCount; agent++) {	    
			for(int j=0 ; j< hmap.get(0).size() ; j++){
				//System.err.print(hmap.get(agent).get(j)+", ");
			}
			//System.err.println(" ");
		}
		
	    // select pair of routes  
	    
		 // for every pair find critical section
		      // assume we have only one pair
              // find critical cells
	             /// implement solution for many critical sections
	    
	    
	    LinkedList<String> criticalcells = new LinkedList<String>();
	    int usingcs = -1 ;
	    boolean sharecs = false ;
	    int sharecsdelay = 0 ;
	    
	    // add critical cells to critical section
	    for(int walker1=0; walker1<hmap.get(0).size();walker1++) {
	    	for(int walker2=0; walker2<hmap.get(0).size(); walker2++){
	    		if ((hmap.get(0).get(walker1).equals(hmap.get(1).get(walker2)) && hmap.get(1).get(walker1).equals(hmap.get(0).get(walker2)))
	    				||  (hmap.get(0).get(walker2).equals(hmap.get(1).get(walker2)))) {
	    			criticalcells.add(hmap.get(0).get(walker1).toString());
	    		}
			   
    			if(hmap.get(0).get(walker2).equals(hmap.get(1).get(walker2))){
				   	sharecs = true;
			  	}
	    	}
	    }

		LinkedList<String> jointActions = new LinkedList<String>();
		boolean readyToleave = false ;
		int readyincounter = 0;
		
		plancounter = new int[agentCount];
		int[] planssteps = new int[agentCount];
		
		/// set all plancounter entries to largestPlanSize
		for (int i = 0; i < plancounter.length; i++) {
			plancounter[i] = hmap.get(0).size();
		}
		
		// set all planssteps to 0
		for (int i = 0; i < planssteps.length; i++) {
			planssteps[i] = 0;
		}
		
		/*
		for (int i = 0; i < longestPlanSize; i++) {
			String jointAction = "[";

			for(int agent = 0; agent < agentCount; agent++) {
				Node agentPlan;
				
				if (agentPlans.get(agent).size() > i)
					agentPlan = agentPlans.get(agent).get(i);
				else
					agentPlan = null;
				
				if (agentPlan != null) {
					jointAction += agentPlan.action;
				} else {
					jointAction += "NoOp";
				}
				jointAction += ",";
			}
			
			jointAction = jointAction.substring(0, jointAction.length() - 1) + "]";
			
	    	jointActions.add(jointAction);
		}
		*/
		
		int globalc = 0;
		while (!areAllPlanCounterEntriesNull()) {
			int actionc = 0;
			String jointAction = "[";
			
			for (int agent = 0; agent < agentCount; agent++) {
				
				// get agent pos
			
                if(planssteps[agent] >= agentPlans.get(agent).size()){
                	jointAction += "NoOp";
                	actionc++;
	    		} else {
					int newagentrow = agentPlans.get(agent).get(planssteps[agent]).agents[agent][0]; // agent row
		    		int newagentcol = agentPlans.get(agent).get(planssteps[agent]).agents[agent][1]; // agent col
		    		String newagentpos = newagentrow+"-"+newagentcol ;
		    
			    	if(sharecsdelay > 5) {
			    		jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();
		    		    planssteps[agent]++;
		    		    plancounter[agent]--;
			    	} else if (criticalcells.getLast().equals(newagentpos) && usingcs==agent) {
		    			jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();
		    		    planssteps[agent]++;
		    		    plancounter[agent]--;
		    		    readyToleave = true;
		    			//System.err.print(" agent "+agent+" leaves critical section");
		    		} else if (criticalcells.getFirst().equals(newagentpos) && usingcs==-1) {
		    			usingcs = agent ;
		    			//System.err.print(" agent "+agent+" is in the critical section");
		    			jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();
		    			planssteps[agent]++;
		    			plancounter[agent]--;
		    		} else if (usingcs==agent) {
		    			jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();
		    			planssteps[agent]++;
			    		plancounter[agent]--;
		    		} else if (usingcs!=-1 && usingcs!=agent) {
		    			jointAction += "NoOp";
		    			actionc++;
		    		} else {
		    			jointAction += agentPlans.get(agent).get(planssteps[agent]).action.toString();
		    			planssteps[agent]++;
			    		plancounter[agent]--;
		    		}
	    		}
	    		
	    		if(agent < this.agentCount - 1) {	// Add commas for all cases except the last one.
	    			jointAction += ",";
	    		}
	    		
	    		if(sharecs){
	    			sharecsdelay++ ;
	    		}
	    		
	    		
			}
			
			if(readyToleave==true && readyincounter<2){
				readyincounter++;
			} else if(readyincounter==2){
				usingcs = -1 ;
				readyincounter = 0;
				readyToleave=false;
			}
			
			if (actionc == agentCount)
				globalc++;
			
			if (globalc == 20)
				break;
			
			jointAction += "]";
	    	jointActions.add(jointAction);
		}
		
		//print agents route based on their location to be removed
	    for(int k=0; k<hmap.size(); k++){

	    	for(int j=0; j<hmap.get(k).size() ; j++){
	    		
	    		////System.err.print(hmap.get(k).get(j)+", ");
	    		
	    	}
	    	////System.err.println(" ");
	    	//
	    }
	    
	    return jointActions;
	}
	
	/**
	 * All goals will be assigned some distinct box and no two goals will be assigned the same box.
	 */
	private void createGoalBoxRelationship() {
		int bestDist = Integer.MAX_VALUE;
		Goal bestGoal = null;
		
		Set<Goal> assignedGoals = new HashSet<Goal>();
		
		for (Box box : discoveredBoxes) {
			int[] boxPos = Utils.findBoxPosition(box, this.initialState.boxIds);

			for (Goal goal : discoveredGoals) {
				int[] goalPos = Utils.findGoalPosition(goal, this.initialState.goalIds);
				
				// This goal has already been assigned; skip this goal
				if (assignedGoals.contains(goal))
					continue;
				
				if (box.goal == null && box.letter == goal.letter) {
					int pathLength = (new BFS(this.initialState)).distance(goalPos[0], goalPos[1], boxPos[0], boxPos[1]);

					if (pathLength != -1 && pathLength < bestDist) {
						bestDist = pathLength;
						bestGoal = goal;
					}
				}
			}
			
			box.goal = bestGoal;
			
			assignedGoals.add(bestGoal);
			
			bestGoal = null;
			bestDist = Integer.MAX_VALUE;
		}
	}
	
	/**
	 * Assigns all boxes that have been assigned to goals to some agent.
	 */
	private void createBoxAgentRelationship() {
		// For now, assignes a box to the first available agent with matching color
		for (Box box : discoveredBoxes) {
			for (Agent agent : discoveredAgents) {
				if (box.goal != null && box.color == agent.color) {
					agent.boxes.add(box);
					break;
				}
			}
		}
	}

	public void init(StrategyType strategyType, SearchClientBackup client) throws IOException {
		System.err.format("Search starting with strategy %s.\n", strategyType);
		
		createGoalBoxRelationship();
		createBoxAgentRelationship();
		
		discoverAgentPlans(strategyType, client);

//		System.err.println("Planning finished for all agents.");
		
//		// Sending joint actions to server for both SA and MA levels
//		LinkedList<String> jointActions = resolveConflicts(agentPlans);
//		System.err.println("Joint actions successfuly generated.");
//		System.err.println(jointActions);
//		
//		return jointActions;
		
		//return agentPlans;
		
		//return formJointActions(agentPlans);
		
		
	}

	public void planNextHLA(SearchClientBackup client, HashMap<Integer, LinkedList<Node>> agentPlans, int agentNo) {
		Node n = agentBeliefs.get(agentNo);
		
		n.addPlannedAction();
		
		n.updatePerception(perception); // This updates the perception of the level; boxes, boxIds and agents arrays are updated
		
		n.strategy.addToFrontier(n);	// NOTE! THE LATEST PERCEPT MUST BE PART OF THE ADDED NODE, OTHERWISE THE PLANNING WILL CRASH DUE TO LATEST AGENT AND BOX POSITION UNKNOWN!
		
		//System.err.println("Level:\n" + n);
		
		System.err.println(n.curAction);
		
//		for(int row = 0; row < n.rows; row++) {
//			for(int col = 0; col < n.cols; col++) {
//				System.err.print(n.goalIds[row][col]);
//			}
//			System.err.println();
//		}
		
		LinkedList<Node> planForAgent = searchForAgent(n.strategy, agentNo);
		
		agentPlans.put(agentNo, planForAgent);
		
//		if(planForAgent != null) {
//			System.err.println("Solution found for agent " + agentNo + ":");
//			for(Node node : planForAgent) {
//				System.err.println(node.toString());
//			}
//		}
		
		n.strategy.clearFrontier();
	}
	
	public LinkedList<String> formJointActions(HashMap<Integer, LinkedList<Node>> listOfActions) {
		int longestPlan = 0;
		for(int agentNo = 0; agentNo < agentCount; agentNo++) {
			int planSize = listOfActions.get(agentNo).size();
			if(planSize > longestPlan) {
				longestPlan = planSize; 
			}
		}
		
		LinkedList<String> jointActions = new LinkedList<String>();
		
		for(int step = 0; step < longestPlan; step++) {
			String jointAction = "[";
			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
				LinkedList<Node> curPlan = listOfActions.get(agentNo);
				if(step < curPlan.size()) {
					jointAction += curPlan.get(step).action.toString();
				}
				else {
					jointAction += "NoOp";
				}
				if(agentNo != agentCount - 1) {
					jointAction += ",";
				}
			}
			jointAction += "]";
			
			jointActions.add(jointAction);
		}
		
		return jointActions;
	}
	
	public boolean areAllPlanCounterEntriesNull(){
		for (int i = 0; i < plancounter.length; i++)
			if(plancounter[i] != 0)
				return false;
		
		return true;
	}
	
	public Strategy createStrategy(StrategyType searchType, SearchClientBackup client) {
		switch(searchType) {
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
	
	/**
	 * Finds a solution for agent if one such exists and the search strategy algorithm is able to discover it. 
	 * @param strategy Type of strategy that is used (e.g. astar, bfs, etc.).
	 * @param agentNo ID of the agent.
	 * @return Plan for the agent or null if no solution found.
	 */
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
			
			if (leafNode.isGoalState()) {
				return leafNode.extractPlan();
			}

			strategy.addToExplored(leafNode);
			
			for (Node n : leafNode.getExpandedNodes(agentNo)) { // The list of expanded nodes is shuffled randomly; see Node.java.
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
					strategy.addToFrontier(n);
				}
			}
			iterations++;
		}
	}
	
	public String formNextJointAction(HashMap<Integer, LinkedList<Node>> agentPlans) {
		String jointAction = "[";
		for(int agentNo = 0; agentNo < agentCount; agentNo++) {
			LinkedList<Node> curPlan = agentPlans.get(agentNo);
			if(!curPlan.isEmpty()) {
				agentsAction[agentNo] = curPlan.remove(0).action.toString();
				jointAction += agentsAction[agentNo];
			}
			else {
				agentsAction[agentNo] = "NoOp";
				jointAction += "NoOp";
			}
			if(agentNo != agentCount - 1) {
				jointAction += ",";
			}
		}
		jointAction += "]";
		
		return jointAction;
	}

	public static void main(String[] args) throws Exception {
		BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

		// Use stderr to print to console
		System.err.println("SearchClient initializing. I am sending this using the error output stream.");

		// Read level and create the initial state of the problem
		SearchClientBackup client = new SearchClientBackup(serverMessages);

		StrategyType strategyType = null;
		
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "-bfs":
                	strategyType = StrategyType.bfs;
                    break;
                case "-dfs":
                	strategyType = StrategyType.dfs;
                    break;
                case "-astar":
                	strategyType = StrategyType.astar;
                    break;
                case "-wastar":
                	strategyType = StrategyType.wastar;
                    break;
                case "-greedy":
                	strategyType = StrategyType.greedy;
                    break;
                default:
                	strategyType = StrategyType.bfs;
                    System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
        	strategyType = StrategyType.bfs;
            System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
        }
        
        /*
        HashMap<Integer, Belief> beliefs;	// HasMaps may not be necessary if the below agent control is made into an object.
        HashMap<Integer, Intention> intentions;
        HashMap<Integer, Desire> desires;
        
        Perception p = readLevelState();
        for(int agentNo = 0; agentNo < agentCount; agentNo++) {
        	Belief b = new Belief(null, p);
        	beliefs.add(agentNo, b);	// Adds the initial belief for each agent
        	intentions.add(agentNo, create some initial intentions);
    	}
    	*/
        
        /*
        Belief b = initialBelief();
        Intention i = initialIntention();
        
        // Threading required to make each agent send their action then attempt to perform the action.
        while(true) {
        	Perception p = readLevelState();
    		b = brf(b, p);
    		Desire d = options(b, i);
    		i = filter(b, d, i);
    		Plan pi = plan(b, i);
    		while(!pi.isEmpty() && !succeeded(i, b) && !impossible(i, b)) {
        			Action a = pi.pop();
        			execute(a); // this action must be executed simultaneously with other agent's actions. Threading is required for this to work.
        			// Only after the joint action has been performed shall the control agent be allowed to continue...
        			Perception p = readLevelState();
        			b = brf(b, p);
        			if(reconsider(i, b)) {
        				d = options(b, i);
        				i = filter(b, d, i);
        			}
        			if !sound(pi, i, b) {
        				pi = plan(b, i);
    				}
    		}
        }
        
        HashMap<Integer, LinkedList<Node>> agentPlans;
        
        agentPlans = client.search(strategyType, client);
        
        while(true){
        	agentPlans = client.search(strategyType, client);
        	while() {
        		// Generate
        	}
        }
        */
        
        
        HashMap<Integer, LinkedList<Node>> agentPlans = new HashMap<Integer, LinkedList<Node>>();
        
		try {
			client.init(strategyType, client);
		} catch (OutOfMemoryError ex) {
			System.err.println("Maximum memory usage exceeded.");
		}
		
		for(int agentNo = 0; agentNo < agentCount; agentNo++) {
			client.planNextHLA(client, agentPlans, agentNo);
		}
		
		List<HighLevelAction> hlaPlan = client.agentBeliefs.get(0).plannedActions;
		for(int step = 0; step < hlaPlan.size(); step++) {
			System.err.print(hlaPlan.get(step).toString());
		}
		System.err.println();
		
		LinkedList<Node> agentPlan = agentPlans.get(0);
		for(int step = 0; step < agentPlan.size(); step++) {
			System.err.print(agentPlan.get(step).action.toString() + ",");
		}
		System.err.println();
		
		ResponseParser responsePar = new ResponseParser(agentCount);
		serverMessages.readLine(); // This is called to ignore the initial server message
		
		while(true) {
			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
				if(agentPlans.get(agentNo).isEmpty()) {
					System.err.println(client.agentBeliefs.get(agentNo).action);
					client.planNextHLA(client, agentPlans, agentNo);
				}
			}
			
			String jointAction = client.formNextJointAction(agentPlans);
			
			System.out.println(jointAction);
			String response = serverMessages.readLine();
			
			System.err.println("Action:" + jointAction);
			System.err.println("Response:" + response);
			boolean[] parsedResponse = responsePar.parseResponse(response);
			
			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
				if(parsedResponse[agentNo]) {
					client.perception.update(client.agentsAction[agentNo], agentNo);
				}
				else {
					System.err.format("Server responsed with %s to the inapplicable action of agent %s: %s\n", false, agentNo, client.agentsAction[agentNo]);
				}
			}
			
			int noOfEmptyPlans = 0;
			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
				if(client.agentBeliefs.get(agentNo).plannedActions.isEmpty() && agentPlans.get(agentNo).isEmpty()) {
					noOfEmptyPlans += 1;
				}
			}
			
			if(noOfEmptyPlans == agentCount) {
				break;
			}
			
//			if (solution == null) {
//				System.err.println("Unable to solve level.");
//				System.exit(0);
//			} else {
//				System.err.println("A solution has been found.");
//				
//				for (String s : solution) {
//					System.out.println(s);
//					String response = serverMessages.readLine();
//					/*if (response.contains("false")) {
//						System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, s);
//						break;
//					}*/
//				}
//			}
		}
	}
}
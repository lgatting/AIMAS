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



public class SearchClient {
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
	public SearchClient(BufferedReader serverMessages) throws Exception {
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
					//System.err.println("Invalid color");
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
						////System.err.println("Error, multiple agents with the same number");
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
					////System.err.println("Error, read invalid level character: " + (int) chr);
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
//		System.err.println("Generating HLA Plan for agent: " + agent.id);
		
		List<HighLevelAction> plan = new ArrayList<HighLevelAction>();
		// Uncomment this section to play around with the corridor solver and comment out the for-loop that follows this comment
		List<Box> orderedBoxes = new ArrayList<Box>();
		
		for (Box box : agent.boxes) {
			orderedBoxes.add(box);
		}
		DeadEndCorridorSolverV2 decsv2 = new DeadEndCorridorSolverV2(discoveredGoals, initialState);
		List<Goal> orderedGoals = decsv2.orderGoals();
		
//		decsv2.printDependancyMatrix();

		Lewis l = new Lewis(discoveredGoals, initialState);
		l.solve();
		
		//PositionPenalizer pp = new PositionPenalizer(discoveredGoals, initialState);
		//pp.solve();
		
		//System.err.println("Discovered order of goals using corridor solver: " + orderedGoals);
		
		//System.err.println();

		Collections.sort(orderedBoxes);
		Collections.reverse(orderedBoxes);
		
		for (Box box : orderedBoxes) {
//			System.err.println("ADDED");
			plan.add(new GoToHLA(box));
			if(box.goal != null) {
				plan.add(new SatisfyGoalHLA(box, box.goal));	//NOTE. This must be edited for MATALK to work!
			}
		}
		
//		for(int i=0; i < plan.size() ; i++){
//			System.err.print(plan.get(i) + ",");
//		}
//		System.err.println();
		
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
	private void discoverAgentPlans(StrategyType strategyType, SearchClient client) {
		for (int agentNo = 0; agentNo < agentCount; agentNo++) {
			Strategy strategy = createStrategy(strategyType, client);
			
			Node copy = this.initialState.copyOfNode();
			
			copy.plannedActions = generateHLAPlan(getAgentObject(agentNo));
			
			//System.err.println("HLA plan for agent " + agentNo + ": " + copy.plannedActions);
			
			copy.agentNo = agentNo;
			copy.strategy = strategy;
			
			//System.err.println("Creating a relaxed plan for agent " + agentNo);
			
			//copy.relaxNode();
			
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
		return null ;
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
		/*
		for (Box box : discoveredBoxes) {
			for (Agent agent : discoveredAgents) {
				if (box.goal != null && box.color == agent.color) {
					agent.boxes.add(box);
					break;
				}
			}
		}
		*/
		
		int[] numberofboxesAttributed = new int[this.agentCount];
		
		for (Box box : discoveredBoxes) {
			LinkedList<Agent> potentialagent = new LinkedList<Agent>();
			for (Agent agent : discoveredAgents) {
				if (box.goal != null && box.color == agent.color) {
					//agent.boxes.add(box);
					potentialagent.add(agent);
				}
			}
				
			int lowestagentcount = 100;
			int agentnumber = -1 ;
			System.err.println("potential agents : ");
			for(int i=0; i<potentialagent.size();i++){
				System.err.print(potentialagent.get(i).id+", ");
				
				int nboxes = numberofboxesAttributed[potentialagent.get(i).id];
				
				if(nboxes<lowestagentcount) {lowestagentcount = nboxes; agentnumber = potentialagent.get(i).id;  }

			}
			System.err.println("  ");
			for (Agent agent : discoveredAgents) {
				if (agent.id == agentnumber) {
				agent.boxes.add(box);
				numberofboxesAttributed[agent.id]++;
				}
			}
		}
		
	}

	public void init(StrategyType strategyType, SearchClient client) throws IOException {
		//System.err.format("Search starting with strategy %s.\n", strategyType);
		
		createGoalBoxRelationship();
		createBoxAgentRelationship();
		
		for(Agent a : discoveredAgents) {
			System.err.println("Agent " + a.id + " is assigned: " + a.boxes);
		}
		
		discoverAgentPlans(strategyType, client);

//		//System.err.println("Planning finished for all agents.");
		
//		// Sending joint actions to server for both SA and MA levels
//		LinkedList<String> jointActions = resolveConflicts(agentPlans);
//		//System.err.println("Joint actions successfuly generated.");
//		//System.err.println(jointActions);
//		
//		return jointActions;
		
		//return agentPlans;
		
		//return formJointActions(agentPlans);
		
		
	}
	
	/**
	 * This method checks whether the goal state og the HLA can be reached before letting the heuristic search.
	 */
	public boolean bfsFindsPath(int agentNo) {
		Node n = agentBeliefs.get(agentNo);
		
		BFS bfs = new BFS(n);
		
		HighLevelAction hla = n.curAction;
		
		if(hla instanceof GoToHLA) {
			GoToHLA gthla = (GoToHLA) hla;
			int[] boxPos = Utils.findBoxPosition(gthla.box, n.boxIds);
			
			int dist = bfs.hasClearPath(n.agents[agentNo][0], n.agents[agentNo][1], boxPos[0], boxPos[1]);
			
			Utils.printArray(n.boxes, n.rows, n.cols);
			
			System.err.println("Found Distance for GoToHLA: " + dist);
			return dist != -1;
		}
		if(hla instanceof GiveWayHLA) {
			GiveWayHLA gwhla = (GiveWayHLA) hla;
			
			int dist = bfs.hasClearPath(n.agents[agentNo][0], n.agents[agentNo][1], gwhla.cell[0], gwhla.cell[1]);
			
			return dist != -1;
		}
		if(hla instanceof StoreTempHLA) {
			StoreTempHLA sthla = (StoreTempHLA) hla;
			
			int dist = bfs.hasClearPath(n.agents[agentNo][0], n.agents[agentNo][1], sthla.cell[0], sthla.cell[1]);
			
			return dist != -1;
		}
		if(hla instanceof SatisfyGoalHLA) {
			SatisfyGoalHLA gthla = (SatisfyGoalHLA) hla;
			int[] goalPos = Utils.findGoalPosition(gthla.goal, n.goalIds);
			
			int dist = bfs.hasClearPath(n.agents[agentNo][0], n.agents[agentNo][1], goalPos[0], goalPos[1]);
			
			return dist != -1;
		}
		
		return false;
	}

	public void planNextHLA(SearchClient client, HashMap<Integer, LinkedList<Node>> agentPlans, int agentNo, boolean relaxPlan) {
		Node n = agentBeliefs.get(agentNo);
		
//		System.err.println("Agent " + agentNo + " has the plan " + n.plannedActions);
		
//		//System.err.println("Agent 0 pos: (" + client.perception.agents[agentNo][0] + "," + client.perception.agents[agentNo][1] + ")");
		
		n.addPlannedAction();
		
		n.updatePerception(perception); // This updates the perception of the level; boxes, boxIds and agents arrays are updated
		
		if(relaxPlan) {
//			System.err.println("RELAX!");
			n.relaxNode();
			for(int a = 0; a < this.initialState.agentCount; a++) {
				if(a != agentNo) {
//					System.err.println("agentNo set to zero: " + a);
					n.agents[a][0] = 0;
					n.agents[a][1] = 0;
				}
			}
		}
		
		n.strategy.addToFrontier(n);	// NOTE! THE LATEST PERCEPT MUST BE PART OF THE ADDED NODE, OTHERWISE THE PLANNING WILL CRASH DUE TO LATEST AGENT AND BOX POSITION UNKNOWN!
		
		System.err.println("Planning action " + n.curAction + " for agent " + agentNo);
//		
//		System.err.println("Planned actions size: " + n.plannedActions.size());
		
//		for(int row = 0; row < n.rows; row++) {
//			for(int col = 0; col < n.cols; col++) {
//				//System.err.print(n.goalIds[row][col]);
//			}
//			//System.err.println();
//		}
		
		LinkedList<Node> planForAgent = null;
		
//		System.err.println("bfsFindsPath(" + agentNo + ")" + bfsFindsPath(agentNo));
		
		if(n.curAction != null && bfsFindsPath(agentNo)) {
			planForAgent = searchForAgent(n.strategy, agentNo);
		}
		
//		System.err.println(planForAgent);
		
		if(planForAgent == null) {
			n.plannedActions.add(0, n.curAction);
			planForAgent = new LinkedList<Node>();
		}
		
		agentPlans.put(agentNo, planForAgent);
		
//		if(planForAgent != null) {
//			//System.err.println("Solution found for agent " + agentNo + ":");
//			for(Node node : planForAgent) {
//				//System.err.println(node.toString());
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
	
	public Strategy createStrategy(StrategyType searchType, SearchClient client) {
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
		int uniqueStates = 0;
		int limit = 40;
		
		while (true) {
            if (iterations == 1000) {
				System.err.println(strategy.searchStatus());
				iterations = 0;
			}
            

			if (strategy.frontierIsEmpty()) {
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();
			if(limit > 0) {
				System.err.println(leafNode);
				limit--;
			}
			
			if (leafNode.isGoalState()) {
				return leafNode.extractPlan();
			}
			
//			//System.err.println("Agent 0 pos: (" + leafNode.agents[agentNo][0] + "," + leafNode.agents[agentNo][1] + ")");
			
			
			strategy.addToExplored(leafNode);
			
			for (Node n : leafNode.getExpandedNodes(agentNo)) { // The list of expanded nodes is shuffled randomly; see Node.java.
//				//System.err.println("3. Agent 0 pos: (" + n.agents[agentNo][0] + "," + n.agents[agentNo][1] + ")");
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
//					System.err.println("Unique States: " + uniqueStates++);
//					System.err.println(n);
					strategy.addToFrontier(n);
				}
			}
			iterations++;
		}
	}
	
	public static LinkedList<String> actualAction ;

	
	public String formNextJointAction(HashMap<Integer, LinkedList<Node>> agentPlans) {
		actualAction = new LinkedList<String>();
		String jointAction = "[";
		for(int agentNo = 0; agentNo < agentCount; agentNo++) {
			LinkedList<Node> curPlan = agentPlans.get(agentNo);
			if(!curPlan.isEmpty()) {
				agentsAction[agentNo] = curPlan.get(0).action.toString();
				
				jointAction += agentsAction[agentNo];
			}
			else {
				agentsAction[agentNo] = "NoOp";
				
				jointAction += "NoOp";
			}
			if(agentNo != agentCount - 1) {
				jointAction += ",";
			}
			actualAction.add(agentsAction[agentNo]);
		}
		jointAction += "]";
		
		return jointAction;
	}
	
	public static void RemoveJointAction(HashMap<Integer, LinkedList<Node>> agentPlans,int agentNo){
		LinkedList<Node> curPlan = agentPlans.get(agentNo);
		if(!curPlan.isEmpty()) {
			 curPlan.remove(0);
		}
	}
 
	public static void main(String[] args) throws Exception {
		BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));

		// Use stderr to print to console
		//System.err.println("SearchClient initializing. I am sending this using the error output stream.");

		// Read level and create the initial state of the problem
		SearchClient client = new SearchClient(serverMessages);

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
                    //System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
        	strategyType = StrategyType.bfs;
            //System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
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
			//System.err.println("Maximum memory usage exceeded.");
		}
		
		for(int agentNo = 0; agentNo < agentCount; agentNo++) {
			System.err.println("Initial planning");
			client.planNextHLA(client, agentPlans, agentNo, false);
			if(agentPlans.get(agentNo).isEmpty()) {
				System.err.println("Creating a relaxed plan for: " + agentNo);
				client.planNextHLA(client, agentPlans, agentNo, true);
//				if(agentPlans.get(agentNo).isEmpty()) {
//					client.agentBeliefs.get(agentNo).removeHeadOfPlannedActions();
//				}
			}
		}
		
//		System.err.println("Plan for agent 5:");
//		for(int i = 0; i < agentPlans.get(5).size(); i++) {
//			System.err.println(agentPlans.get(5).get(i));
//		}
		
		List<HighLevelAction> hlaPlan = client.agentBeliefs.get(0).plannedActions;
		for(int step = 0; step < hlaPlan.size(); step++) {
			//System.err.print(hlaPlan.get(step).toString());
		}
		//System.err.println();
		
		LinkedList<Node> agentPlan = agentPlans.get(0);
		for(int step = 0; step < agentPlan.size(); step++) {
			//System.err.print(agentPlan.get(step).action.toString() + ",");
		}
		//System.err.println();
		
		ResponseParser responsePar = new ResponseParser(agentCount);
		serverMessages.readLine(); // This is called to ignore the initial server message
		
		int[] trials = new int[client.initialState.agentCount];
		
//		for(int row = 0; row < client.initialState.rows; row++) {
//			for(int col = 0; col < client.initialState.cols; col++) {
//				System.err.print(client.initialState.boxIds[row][col]);
//			}
//			System.err.println();
//		}
		System.err.println(client.agentBeliefs.get(0).goalIds);
		boolean flag2 = false ;
		
		while(true) {
			
			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
				if(agentPlans.get(agentNo).isEmpty()) {
					//System.err.println(client.agentBeliefs.get(agentNo).action);
					client.planNextHLA(client, agentPlans, agentNo, false);
					if(agentPlans.get(agentNo).isEmpty()) {
						System.err.println("Creating a relaxed plan for: " + agentNo);
						client.planNextHLA(client, agentPlans, agentNo, true);
					}
				}
				
			}
			
			String jointAction = client.formNextJointAction(agentPlans);
			
			System.err.println("Agent 4 plannedActions: " + client.agentBeliefs.get(4).plannedActions);
			
			System.out.println(jointAction);
			String response = serverMessages.readLine();
			
			
			
			System.err.print("Action:" + jointAction);
			System.err.println(" Response:" + response);
			boolean[] parsedResponse = responsePar.parseResponse(response);
			boolean flag = true ; // assume only two agents
			/// assume only two agents otherwise trials should be an array
		
			//System.err.println("enter for:");
			
			for(int i=0;i<parsedResponse.length;i++){
				if(parsedResponse[i]) RemoveJointAction(agentPlans,i);
				else if(trials[i]<=3) trials[i]++ ;
				else if (trials[i] > 3 && flag) {
					
					int nopriorityagent = i ;
				//	int prioritizedagent = (i==0 ? 1:0) ;
					int prioritizedagent = -1 ;
					
					Node n = client.agentBeliefs.get(nopriorityagent);
					
					n.updatePerception(client.perception); // This updates the perception of the level; boxes, boxIds and agents arrays are updated
					
					int agentrow = n.agents[i][0]; // agent row
		    		int agentcol =  n.agents[i][1]; // agent col
		    		
		    		System.err.println("Agent Action: " + n.curAction);
					
		    		searchclient.ObjectFinder objectFinder = new searchclient.ObjectFinder(agentrow, agentcol);
		    		int[] potentialObject = objectFinder.getBoxPos(actualAction.get(i));
		    		
		    		if(n.boxIds[potentialObject[0]][potentialObject[1]]!=0){
		    			
		    			System.err.println(potentialObject[0]+" c:"+potentialObject[1]+"------------------------------------box");
		    			
		    			char boxChar = n.boxes[potentialObject[0]][potentialObject[1]];
		    			
		    			Color boxColor = n.colorAssignments.get(boxChar);
		    			
		    			for(int agentNo = 0; agentNo < client.agentCount; agentNo++) {
		    				HighLevelAction curHla = client.agentBeliefs.get(agentNo).curAction;
		    				// The below section adds a GoToHLA to the agent who is giving way, to ensure returning to the box after helping
		    				if(curHla instanceof SatisfyGoalHLA) {
		    					SatisfyGoalHLA satHla = (SatisfyGoalHLA) curHla;
		    					if(satHla.box.id == n.boxIds[potentialObject[0]][potentialObject[1]]) {
		    						prioritizedagent = agentNo;
		    						System.err.println("----**---"+agentNo);
		    						break;
		    					}
		    				} 
		    				
		    				
		    				
		    			}
		    			
		    			if(prioritizedagent == -1) {
			    			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
			    				if(n.colorAssignments.get((char) (agentNo + '0')) == boxColor) {
			    					prioritizedagent = agentNo;
			    					break;
			    				}
			    			}
		    			}
		    			
		    			/*
		    			if(actualAction.get(i).contains("Push")) {
	    					
	    					int temploc = prioritizedagent;
			    			prioritizedagent = nopriorityagent;
			    		    nopriorityagent = temploc ;
	    					
	    					
	    				}
	    				*/
		    			
		    			System.err.println("***action for agent "+i+": "+actualAction.get(i));
		    			
		    			BFS cbfs = new BFS(n);
			    		
			    		int[] tmpCell = cbfs.searchForTempCell(new int[]{potentialObject[0], potentialObject[1]}, nopriorityagent, prioritizedagent, n, agentPlans);
			    		
			    		System.err.println("TmpCell: " + tmpCell[0] + "," + tmpCell[1]);
			    		
			    		Box boxToMove = null;
			    		boolean foundBox = false;
			    		
			    		for (Iterator<Box> it = client.discoveredBoxes.iterator(); it.hasNext(); ) {
			    		    boxToMove = it.next();
			    		    if(boxToMove.id == n.boxIds[potentialObject[0]][potentialObject[1]]) {
			    		    	foundBox = true;
			    		    	break;
			    		    }
			    		}
			    		if (foundBox) {
//			    			if(n.agents[prioritizedagent][0] == 1 && n.agents[prioritizedagent][1] == 18) {
//			    				agentPlans.get(prioritizedagent).clear();
//			    			}
			    			
				    		StoreTempHLA sthla = new StoreTempHLA(boxToMove, tmpCell[0], tmpCell[1]);
				    		
				    		if(!client.agentBeliefs.get(prioritizedagent).plannedActions.isEmpty()) {
					    		HighLevelAction plannedNextAction = client.agentBeliefs.get(prioritizedagent).plannedActions.get(0);
					    		
					    		if(plannedNextAction instanceof SatisfyGoalHLA) {
					    			SatisfyGoalHLA shla = (SatisfyGoalHLA) plannedNextAction;
					    			GoToHLA gthla = new GoToHLA(shla.box);
					    			System.err.println("Adding GoToHLA in boxes conflict solver");
					    			client.agentBeliefs.get(prioritizedagent).plannedActions.add(0, gthla);
					    		}
				    		}
				    		System.err.println("Adding StoreTempHLA in boxes conflict solver");
				    		client.agentBeliefs.get(prioritizedagent).plannedActions.add(0, sthla);
				    		
				    		
			    		}
		    		}
		    		else {
		    			
		    			System.err.println(potentialObject[0]+" c:"+potentialObject[1]+"------"+actualAction.get(i)+"----------------agent");
		    			
		    			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
		    				if(n.agents[agentNo][0] == potentialObject[0] && n.agents[agentNo][1] == potentialObject[1]) {
		    					prioritizedagent = agentNo;
		    				}
		    			}
		    			
//		    			flag2 = true? false : true ;
//		    			System.err.println("flag"+flag2);
//		    			
//		    			if(flag2){
//		    			int temploc = prioritizedagent;
//		    			prioritizedagent = nopriorityagent;
//		    		    nopriorityagent = temploc ;
//		    			}
		    			
		    			BFS cbfs = new BFS(n);
						int[] freeCellPos = cbfs.searchForFreeCell(nopriorityagent, prioritizedagent, n, agentPlans);
						
						if(freeCellPos[0] == n.agents[nopriorityagent][0] && freeCellPos[1] == n.agents[nopriorityagent][1]) {
							int temploc = prioritizedagent;
			    			prioritizedagent = nopriorityagent;
			    		    nopriorityagent = temploc ;
							freeCellPos = cbfs.searchForFreeCell(nopriorityagent, prioritizedagent, n, agentPlans);
							n = client.agentBeliefs.get(nopriorityagent);
						}
						
						System.err.println("freeCellPos: " + freeCellPos[0] + "," + freeCellPos[1]);
//						//System.err.println("found cell is null:" + freeCellPos[0]);
//						//System.err.println("found cell is null:" + freeCellPos[1]);
						
						GiveWayHLA gwhla = new GiveWayHLA(freeCellPos[0], freeCellPos[1]);
						
						
//						//System.err.println("Planned Actions size:" + n.plannedActions.size());
						System.err.println("Adding GiveWayHLA in agents conflict solver");
						n.plannedActions.add(0, gwhla);
						if(n.curAction != null) {
//							if(n.curAction instanceof SatisfyGoalHLA) {
//								n.plannedActions.add(1, n.curAction);
//								
//								SatisfyGoalHLA sghla = (SatisfyGoalHLA) n.curAction;
//								n.plannedActions.add(1, new GoToHLA(sghla.box));
//							}
//							else {
							System.err.println("Adding CurActionHLA in agents conflict solver");
							n.plannedActions.add(1, n.curAction);
//							}
						}
//						//System.err.println("Planned Actions size:" + n.plannedActions.size());
//						//System.err.println("Agent 0 pos: (" + client.perception.agents[nopriorityagent][0] + "," + client.perception.agents[nopriorityagent][1] + ")");
						agentPlans.get(nopriorityagent).clear();
//						//System.err.println("Planned Actions size:" + n.plannedActions.size());
						client.planNextHLA(client, agentPlans, nopriorityagent, false);
		    		}
		    		
//					System.exit(1);
					
//					//System.err.println("Agent 0 pos: (" + client.perception.agents[nopriorityagent][0] + "," + client.perception.agents[nopriorityagent][1] + ")");
					
					//System.err.println(agentPlans);
					
					for(int agentNo = 0; agentNo < client.initialState.agentCount; agentNo++) {
						trials[agentNo] = 0;
					}
					flag = false ;
				}
				
			}
			
			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
				if(parsedResponse[agentNo]) {
					client.perception.update(client.agentsAction[agentNo], agentNo);
				}
				else {
					//System.err.format("Server responsed with %s to the inapplicable action of agent %s: %s\n", false, agentNo, client.agentsAction[agentNo]);
				}
			}
			
			int noOfEmptyPlans = 0;
			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
				Node n = client.agentBeliefs.get(agentNo);
				if(n.plannedActions.isEmpty() && agentPlans.get(agentNo).isEmpty()) {
					n.checkHLAs();	// Checks whether the past SatisfyGoalAction is still satisfied. If not, it's added back to the plannedActions of the agent.
					if(n.plannedActions.isEmpty()) {
						noOfEmptyPlans += 1;
					}
				}
			}
			
			if(noOfEmptyPlans == agentCount) {
				break;
			}
			
//			if (solution == null) {
//				//System.err.println("Unable to solve level.");
//				System.exit(0);
//			} else {
//				//System.err.println("A solution has been found.");
//				
//				for (String s : solution) {
//					System.out.println(s);
//					String response = serverMessages.readLine();
//					/*if (response.contains("false")) {
//						//System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, s);
//						break;
//					}*/
//				}
//			}
		}
	}
}

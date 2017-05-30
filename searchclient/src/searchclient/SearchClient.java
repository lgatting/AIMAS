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
import java.util.StringTokenizer;
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
	private int[] potentialObject;
	
	public Perception perception;
	
	public HashMap<Integer, Node> agentOriginNode = new HashMap<Integer, Node>();
	
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
					////System.err.println("Invalid color");
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
						//////System.err.println("Error, multiple agents with the same number");
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
					//////System.err.println("Error, read invalid level character: " + (int) chr);
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
//		//System.err.println("Generating HLA Plan for agent: " + agent.id);
		
		List<HighLevelAction> plan = new ArrayList<HighLevelAction>();
		// Uncomment this section to play around with the corridor solver and comment out the for-loop that follows this comment
		List<Box> orderedBoxes = new ArrayList<Box>();
		
		//System.err.println("boxes");
		for (Box box : agent.boxes) {
			orderedBoxes.add(box);
			//System.err.println(box.id+""+box.letter);
		}
		
		
		
		DeadEndCorridorSolverV2 decsv2 = new DeadEndCorridorSolverV2(discoveredGoals, initialState);
		List<Goal> orderedGoals = decsv2.orderGoals();
		

		Lewis l = new Lewis(discoveredGoals, initialState);
		l.solve();
		


		Collections.sort(orderedBoxes);
		Collections.reverse(orderedBoxes);
		
		for (Box box : orderedBoxes) {
//			//System.err.println("ADDED");
			plan.add(new GoToHLA(box));
			if(box.goal != null) {
				plan.add(new SatisfyGoalHLA(box, box.goal));	//NOTE. This must be edited for MATALK to work!
			}
		}
		

		
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
			//System.err.println("discover for agent: "+agentNo);
			Strategy strategy = createStrategy(strategyType, client);
			
			Node copy = this.initialState.copyOfNode();
			
			copy.plannedActions = generateHLAPlan(getAgentObject(agentNo));
			
			////System.err.println("HLA plan for agent " + agentNo + ": " + copy.plannedActions);
			
			copy.agentNo = agentNo;
			copy.strategy = strategy;
			
			////System.err.println("Creating a relaxed plan for agent " + agentNo);
			
			//copy.relaxNode();
			
			agentOriginNode.put(agentNo, copy);			
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

		
		int[] numberofboxesAttributed = new int[this.agentCount];
	
		for (Box box : discoveredBoxes) { // for everybox

			LinkedList<Agent> potentialagent = new LinkedList<Agent>();
			for (Agent agent : discoveredAgents) { // find potential agents
				
				if (box.goal != null && box.color == agent.color) {

				
					int[] boxPos = Utils.findBoxPosition(box, initialState.boxIds);
					    	
					
		int pathLength = (new BFS(this.initialState)).distance(initialState.agents[agent.id][0], initialState.agents[agent.id][1], boxPos[0], boxPos[1]);

					
					if(pathLength!=-1){
						potentialagent.add(agent);
					}
					
					
					
				
				}
			}
			
				
			int lowestagentcount = 100;
			int agentnumber = -1 ;
			
			for(int i=0; i<potentialagent.size();i++){
			
				
				int nboxes = numberofboxesAttributed[potentialagent.get(i).id];
				
				if(nboxes<lowestagentcount) {lowestagentcount = nboxes; agentnumber = potentialagent.get(i).id;  }

			}
			
			for (Agent agent : discoveredAgents) {
				if (agent.id == agentnumber) {
				agent.boxes.add(box);
				numberofboxesAttributed[agent.id]++;
		 
				}
			}
			
		
			
		}
		
	}

	public void init(StrategyType strategyType, SearchClient client) throws IOException {

		
		createGoalBoxRelationship();
		createBoxAgentRelationship();
		
		for(Agent a : discoveredAgents) {
		
		}
		
		discoverAgentPlans(strategyType, client);

		
	}
	
	/**
	 * This method checks whether the goal state og the HLA can be reached before letting the heuristic search.
	 */
	public boolean bfsFindsPath(int agentNo) {
		Node n = agentOriginNode.get(agentNo);
		
		BFS bfs = new BFS(n);
		
		HighLevelAction hla = n.curAction;
		
		if(hla instanceof GoToHLA) {
			////System.err.println("bfsFindsPath() for agent: "+agentNo+" instance GoToHLA");
			GoToHLA gthla = (GoToHLA) hla;
			int[] boxPos = Utils.findBoxPosition(gthla.box, n.boxIds);
			
			int dist = bfs.hasClearPath(n.agents[agentNo][0], n.agents[agentNo][1], boxPos[0], boxPos[1]);
			
			Utils.printArray(n.boxes, n.rows, n.cols);
			
			// //System.err.println("Found Distance for GoToHLA: " + dist);
			return dist != -1;
		}
		if(hla instanceof GiveWayHLA) {
				GiveWayHLA gwhla = (GiveWayHLA) hla;
			// 	//System.err.println("bfsFindsPath() for agent: "+agentNo+" instance giveway agent position:"+n.agents[agentNo][0]+","+n.agents[agentNo][1]+". gwla cell pos: "+gwhla.cell[0]+","+gwhla.cell[1]);
				
			int dist = bfs.hasClearPath(n.agents[agentNo][0], n.agents[agentNo][1], gwhla.cell[0], gwhla.cell[1]);
			
			return dist != -1;
		}
		if(hla instanceof StoreTempHLA) {
			// //System.err.println("bfsFindsPath() for agent: "+agentNo+" instance storetamphla");
			StoreTempHLA sthla = (StoreTempHLA) hla;
		//	//System.err.println("bfsFindsPath() for agent: "+agentNo+" instance  agent position:"+n.agents[agentNo][0]+","+n.agents[agentNo][1]+". gwla cell pos: "+sthla.cell[0]+","+sthla.cell[1]);
			
			
			
			int dist = bfs.hasClearPath(n.agents[agentNo][0], n.agents[agentNo][1], sthla.cell[0], sthla.cell[1]);
			
			////System.err.println("bfsFindsPath() for agent: "+agentNo+"  distance="+dist);
			
			return dist != -1;
		}
		if(hla instanceof SatisfyGoalHLA) {
		//	//System.err.println("bfsFindsPath() for agent: "+agentNo+" instance satisfygoalhla");
			SatisfyGoalHLA gthla = (SatisfyGoalHLA) hla;
			int[] goalPos = Utils.findGoalPosition(gthla.goal, n.goalIds);
		//	//System.err.println("bfsFindsPath() for agent: "+agentNo+" instance agent position:"+n.agents[agentNo][0]+","+n.agents[agentNo][1]+". cell pos: "+goalPos[0]+","+goalPos[1]);
			
			
			int dist = bfs.hasClearPath(n.agents[agentNo][0], n.agents[agentNo][1], goalPos[0], goalPos[1]);
		//	//System.err.println("bfsFindsPath() for agent: "+agentNo+"  distance="+dist);
			
			return dist != -1;
		}
		
		return false;
	}

	public void planNextHLA(HashMap<Integer, LinkedList<Node>> agentLowLevelPlans, int agentNo, boolean relaxPlan, boolean intial) {
		// relax plan boolean 
		
		Node n = agentOriginNode.get(agentNo); // node correspin
		
	
		n.addPlannedAction(); /// extract the head HLA action
		
		
		
		n.updatePerception(perception); 		
		boolean agentoverbox = false;

		
		
		if(deadlock || (relaxPlan && intial) ){ /// used only for replaning
			for(int i=0 ; i<this.agentCount ; i++){
				if(i!=agentNo){
					n.agents[i][0] = 0 ;
					n.agents[i][1] = 0 ;
				}
				
			}
		}
		
		Node temp = n.copyOfNode() ;
		
	if(relaxPlan) { 
			
			
			
			n.relaxNode();
			
		
			
			
		}
		

		
		n.strategy.addToFrontier(n);	
		
		LinkedList<Node> planForAgent = null; /// initiate low level plan
		
	
		
		if(n.curAction != null ) {
	
			planForAgent = searchForAgent(n.strategy, agentNo); /// generate the low level actions
		
		}
		
	
		
		if(planForAgent == null) {
			n.plannedActions.add(0, n.curAction); /// we put it again in plannedAction for second trial that will be relaxed
			planForAgent = new LinkedList<Node>(); // prevent null pointer exception
		}
		

		
		agentLowLevelPlans.put(agentNo, planForAgent); // hashmap of linked list of each each agent lower plans

		
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
		int iterations2 = 0;
		int iterations3 = 0;
	
		
		while (true) {
			
			iterations2++;
			iterations3++;
			
            if (iterations == 1000) {
		
				iterations = 0;
				
			}
            

			if (strategy.frontierIsEmpty()) {
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();
			if(limit > 0) {
	
				limit--;
			}
			
			if(iterations2==200){
				iterations2 = 0 ;
				for(int i=0 ; i<this.agentCount ; i++){
				if(i!=agentNo){
					leafNode.agents[i][0] = 0 ;
					leafNode.agents[i][1] = 0 ;
				}
				
			}
				for(int row = 0; row < leafNode.rows; row++) {
					for(int col = 0; col < leafNode.cols; col++) {
						char boxChar = leafNode.boxes[row][col];
						if(boxChar > 0 && !leafNode.sameColorAsAgent(leafNode.agentNo, boxChar)) {
							leafNode.boxes[row][col] = 0;
						}
					}
				}
			}
			
			
		
			
			
			
			if (leafNode.isGoalState(iterations3)) {
				iterations2 = 0 ;
				iterations3 = 0;
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
	
	public static LinkedList<String> actualAction ;

	
	public String formNextJointAction(HashMap<Integer, LinkedList<Node>> agentPlans) {
		actualAction = new LinkedList<String>();
		String jointAction = "[";
		for(int agentNo = 0; agentNo < agentCount; agentNo++) {
			LinkedList<Node> agentlowlevelplan = agentPlans.get(agentNo);
			if(!agentlowlevelplan.isEmpty()) {
				agentsAction[agentNo] = agentlowlevelplan.get(0).action.toString(); 
				
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
	
	public static void RemoveJointAction(HashMap<Integer, LinkedList<Node>> agentlowlevelsplans,int agentNo){
		LinkedList<Node> curPlan = agentlowlevelsplans.get(agentNo);
		if(!curPlan.isEmpty()) {
			 curPlan.remove(0); // remove the head
		}
	}
 
	public String getBlockingObjectType(Node n,int agentNo){
		
		int agentrow = n.agents[agentNo][0]; // agent row
		int agentcol =  n.agents[agentNo][1]; // agent col

		
		searchclient.ObjectFinder objectFinder = new searchclient.ObjectFinder(agentrow, agentcol);
		 potentialObject = objectFinder.getBoxPos(actualAction.get(agentNo));
    	if(potentialObject[0]!=-1 && potentialObject[1]!=-1)
		if(n.boxIds[potentialObject[0]][potentialObject[1]]!=0){
			return "box" ;
		}
		else {
			
			
			for(int i=0; i<n.agentCount ; i++){
				if(n.agents[i][0]== potentialObject[0] && n.agents[i][1]== potentialObject[1]){
					return "agent";
				}
			}
			
		}
		
		return "unknown" ;
	}
	
	public HashMap<Integer,Integer> highlowp = new HashMap<Integer,Integer>();
	
	public int[] GetHighLowPriorityAgent(Node n, int AgentNo, int DetectedAgent,String type){
		
		int[] agents = new int[2];
		
		/// default case
		agents[0] = AgentNo ;        
		agents[1] = DetectedAgent ;
		
		if(type.equals("box")){
			

			
		}
		else if(type.equals("agents")){
			
			/// give priority to agent with push or pull action
			if(!actualAction.get(AgentNo).contains("Push") && !actualAction.get(AgentNo).contains("Pull") && (actualAction.get(DetectedAgent).contains("Push") || actualAction.get(DetectedAgent).contains("Pull"))) {
				agents[0] = DetectedAgent ;        
				agents[1] =  AgentNo;
				//System.err.println("---priorities switched");
			}
			
			// //System.err.println("---prioritizedagent:"+prioritizedagent+"--noneprioritizedagent"+ nopriorityagent );
			
		}
		
		
	
		highlowp.put(agents[0],0);
		highlowp.put(agents[1],1);
		
	
		
		return agents ;
	}
	
	public int getOpposingAgent(Node n){
		for(int agentNo = 0; agentNo < agentCount; agentNo++) {
			if(n.agents[agentNo][0] == potentialObject[0] && n.agents[agentNo][1] == potentialObject[1]) {
				return  agentNo;
			}
		}
		return -1 ;
	}
	
	public int getAgentBox(Node n){
		
		char boxChar = n.boxes[potentialObject[0]][potentialObject[1]];
		
		Color boxColor = n.colorAssignments.get(boxChar);
		
		int availableAgents[] = new int[agentCount];
		for(int i = 0; i < agentCount; i++) {
			availableAgents[i]=-1 ;
		}
		
		
		int counter = 0 ;
		for(int i = 0; i < agentCount; i++) {
			if(n.colorAssignments.get((char) (i + '0')) == boxColor) { 

				availableAgents[counter]=i ;
				counter++;
			}
		}
		counter = 0 ;
		
		int closestAgent = availableAgents[0] ;
		int distance = 90000 ;
		int shortestdistance = 1000000 ;
		int choosedAgent = -1 ;
		
	
		
	for(counter=0; counter<availableAgents.length; counter++){
			
		
		if(availableAgents[counter]!=-1){
			BFS cbfs = new BFS(n);
			int agentn = availableAgents[counter];
			
			distance = cbfs.distance(n.agents[agentn][0], n.agents[agentn][1], potentialObject[0], potentialObject[1]);
			
			if(distance<shortestdistance){
				shortestdistance= distance ;
				choosedAgent = agentn;
			}
		}
		
			
			
		
		}
		
		
		return choosedAgent ;
	}
	
	public void PlanForStoreTemp(Node n, int highPagent, int lowPagent,HashMap<Integer, LinkedList<Node>> agentLowLevelPlans){
		Box boxToMove = null;
		boolean foundBox = false;
		
		
		
		BFS cbfs = new BFS(n);
		
		
		int[] tmpCell = cbfs.searchForTempCell(new int[]{potentialObject[0], potentialObject[1]}, highPagent, lowPagent, n, agentLowLevelPlans);
	 
		if(tmpCell!= null){
		//	System.err.println("TmpCell: " + tmpCell[0] + "," + tmpCell[1]);
			

			/// get the box object from the box location 
			for (Iterator<Box> it = discoveredBoxes.iterator(); it.hasNext(); ) {
			    boxToMove = it.next();
			    if(boxToMove.id == n.boxIds[potentialObject[0]][potentialObject[1]]) {
			    	foundBox = true;
			    	break;
			    }
			}
			if (foundBox && lowPagent!=-1) {
				
				agentLowLevelPlans.get(lowPagent).clear();
			
				
	    		StoreTempHLA sthla = new StoreTempHLA(boxToMove, tmpCell[0], tmpCell[1]);
	    		
	    		if(!agentOriginNode.get(lowPagent).plannedActions.isEmpty()) { /// check if there are high level actions
	    			
	    		
					HighLevelAction plannedNextAction = agentOriginNode.get(lowPagent).plannedActions.get(0);

				
					if (plannedNextAction instanceof SatisfyGoalHLA) {
						SatisfyGoalHLA shla = (SatisfyGoalHLA) plannedNextAction;
						GoToHLA gthla = new GoToHLA(shla.box); /// go to the box
																/// that should be
																/// satisfied
						agentOriginNode.get(lowPagent).plannedActions.add(0, gthla);
					}
				}

				
				agentOriginNode.get(lowPagent).plannedActions.add(0, sthla);
				
			
				for(int k=0;k<agentOriginNode.get(lowPagent).plannedActions.size();k++){
					
				}
				
				
			}
		}
		
		
	}
	
	public void PlanForGiveWay(Node n, int highPagent, int lowPagent,HashMap<Integer, LinkedList<Node>> agentLowLevelPlans){
		BFS cbfs = new BFS(n);
		int[] freeCellPos = cbfs.searchForFreeCell(highPagent,lowPagent, n, agentLowLevelPlans);
		
		if(freeCellPos != null){
			
			if(freeCellPos[0] == n.agents[lowPagent][0] && freeCellPos[1] == n.agents[lowPagent][1]) {
			// 	System.err.println("special case");
				int temploc = lowPagent;
				lowPagent = highPagent;
				highPagent = temploc ;
				freeCellPos = cbfs.searchForFreeCell(highPagent, lowPagent, n, agentLowLevelPlans);
	
			}
			else {
				
			}
			
			
			
			GiveWayHLA gwhla = new GiveWayHLA(freeCellPos[0], freeCellPos[1]);
			
			
			if(!agentOriginNode.get(lowPagent).plannedActions.isEmpty()) { /// check if there are high level actions
	    		
				
				HighLevelAction plannedNextAction = agentOriginNode.get(lowPagent).plannedActions.get(0);

	    		if(plannedNextAction instanceof SatisfyGoalHLA) {
	    			SatisfyGoalHLA shla = (SatisfyGoalHLA) plannedNextAction;
	    			GoToHLA gthla = new GoToHLA(shla.box); /// go to the box that should be satisfied
	    			agentOriginNode.get(lowPagent).plannedActions.add(0, gthla);
	    		}
			}
			
			
			 agentOriginNode.get(lowPagent).plannedActions.add(0, gwhla);

			
			
		agentLowLevelPlans.get(lowPagent).clear();

			
			
		}
		
		
	}
	
	int deadlockcounter = 0 ;
	boolean isdeadlock = false ; 
	
	public void deadlockdetector(boolean[] response){
		
		boolean deadlock = true ;
		for(int i=0; i<response.length;i++){
			if(response[i]==true) {
				deadlock = false ;
				deadlockcounter = 0 ;
				break;
			}
		}
		
		if(deadlock==true){
			deadlockcounter++;
		}
		
		if(deadlockcounter>3){
		
		    isdeadlock= true ;

		}

	}
	
	public HashMap<Integer, Integer> blocker = new HashMap<Integer, Integer>();
	
	
	public boolean deadlockhandler(int lowPagent, Node n, HashMap<Integer, LinkedList<Node>> agentLowLevelPlans,int highPagent, String type){
		boolean returnvalue = false ;
		boolean cellfound = false ;
		
		if(isdeadlock){
		
			
			BFS cbfs = new BFS(n);
			int[] freeCellPos = new int[2];
			freeCellPos[0] = -1 ;
			freeCellPos[1] = -1 ;
			
		
			freeCellPos = cbfs.searchForFreeCell2(highPagent, n, agentLowLevelPlans);
			
			if(freeCellPos != null){
			
				isdeadlock = false ;
				cellfound = true ;
				
			}
			
			
				
				if(cellfound && lowPagent!=-1){
					
				
					GiveWayHLA gwhla = new GiveWayHLA(freeCellPos[0], freeCellPos[1]);		
					if(!agentOriginNode.get(lowPagent).plannedActions.isEmpty()) { /// check if there are high level actions
                     HighLevelAction plannedNextAction = agentOriginNode.get(lowPagent).plannedActions.get(0);
			    		if(plannedNextAction instanceof SatisfyGoalHLA) {
			    			SatisfyGoalHLA shla = (SatisfyGoalHLA) plannedNextAction;
			    			GoToHLA gthla = new GoToHLA(shla.box); /// go to the box that should be satisfied
			    			agentOriginNode.get(lowPagent).plannedActions.add(0, gthla);
			    		}
					}
					 agentOriginNode.get(lowPagent).plannedActions.add(0, gwhla);
					agentLowLevelPlans.get(lowPagent).clear();
					returnvalue = true ;
					
					
						agentOriginNode.get(lowPagent).blocked = true ;
						blocker.put(highPagent, lowPagent);
					
				
					
					
					
			          freeCellPos = cbfs.searchNoneDeadLockedCell(freeCellPos, highPagent,lowPagent, n, agentLowLevelPlans);

			          
			          if(freeCellPos!=null)
			          
							
							 gwhla = new GiveWayHLA(freeCellPos[0], freeCellPos[1]);		
							if(!agentOriginNode.get(highPagent).plannedActions.isEmpty()) {
		                     HighLevelAction plannedNextAction = agentOriginNode.get(highPagent).plannedActions.get(0);
					    		if(plannedNextAction instanceof SatisfyGoalHLA) {
					    			SatisfyGoalHLA shla = (SatisfyGoalHLA) plannedNextAction;
					    			GoToHLA gthla = new GoToHLA(shla.box); 
					    			agentOriginNode.get(highPagent).plannedActions.add(0, gthla);
					    		}
							}
							 agentOriginNode.get(highPagent).plannedActions.add(0, gwhla);
							agentLowLevelPlans.get(highPagent).clear();
							returnvalue = true ;
							
				
                    }
			
				}
					
			return returnvalue ;
		
		
		
	}
	
	public HashMap<Integer, Integer> cnflicting_agents = new HashMap<Integer, Integer>();
	  
public int  IsLowLevelPlanNotConflicting(HashMap<Integer, LinkedList<Node>> agentLowLevelPlans){
		
		HashMap <String,Integer> positions = new HashMap <String,Integer>();
		
		for(int agentNo=0; agentNo<this.agentCount; agentNo++){
			if(!agentLowLevelPlans.get(agentNo).isEmpty()){
				if(agentLowLevelPlans.get(agentNo).get(0).action.actionType.equals(Command.Type.Pull) || agentLowLevelPlans.get(agentNo).get(0).action.actionType.equals(Command.Type.Push)){
						
					int newagentrow = agentLowLevelPlans.get(agentNo).get(0).agents[agentNo][0]; 
		    		int newagentcol = agentLowLevelPlans.get(agentNo).get(0).agents[agentNo][1];
		    		String newagentpos = newagentrow+"-"+newagentcol ;
		    		if(positions.containsKey(newagentpos)){
		    			return positions.get(newagentpos) ; 
		    		}
		    		else {
		    			positions.put(newagentpos, agentNo);
		    		}
					
				}
				
	    		
			}
			
    		
		}
		
		return -1;
		
	}
	
boolean deadlock = false ;
	
	public static void main(String[] args) throws Exception {
		BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));
	
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
                             }
        } else {
        	strategyType = StrategyType.bfs;
               }
        
     
        
        
        HashMap<Integer, LinkedList<Node>> agentLowLevelPlans = new HashMap<Integer, LinkedList<Node>>();
        
		try {
		
			for(int i=0;i<client.initialState.rows;i++){
				for(int j=0; j<client.initialState.cols;j++){
					
				}
		
			}
			
			client.init(strategyType, client);
		} catch (OutOfMemoryError ex) {
			
		}
		
		

		for(int agentNo = 0; agentNo < agentCount; agentNo++) {
		//	System.err.println("Initial planning");
			client.planNextHLA(agentLowLevelPlans, agentNo, false, false);
			if(agentLowLevelPlans.get(agentNo).isEmpty()) {
			//	System.err.println("Creating a relaxed plan for: " + agentNo);
				client.planNextHLA(agentLowLevelPlans, agentNo, true, false);
			}
		}
		

		
		List<HighLevelAction> hlaPlan = client.agentOriginNode.get(0).plannedActions; 

		ResponseParser responsePar = new ResponseParser(agentCount);
		serverMessages.readLine(); // This is called to ignore the initial server message
		
		
		
		int[] trials = new int[client.initialState.agentCount];
	
		 boolean[] replaned = new boolean[client.agentCount] ;
		 
		int k=-1;
		
		
		
		while(true) {
			
			k++;
		
		
			for (int agentNo = 0; agentNo < agentCount; agentNo++) {
				// System.err.println("Creating a normal plan for: " + agentNo);
			
				
				client.agentOriginNode.get(agentNo).updatePerception(client.perception); // ***// check that it does not affect SA levels

				
				if(client.IsLowLevelPlanNotConflicting(agentLowLevelPlans)!=-1 && client.IsLowLevelPlanNotConflicting(agentLowLevelPlans)!=agentNo){
					
				//	 System.err.println("Cplan cleared: " + agentNo);
					
					agentLowLevelPlans.get(agentNo).clear();
				}
				
				
			
       
				if (agentLowLevelPlans.get(agentNo).isEmpty() && !client.agentOriginNode.get(agentNo).blocked) {
				

			//		System.err.println("plan for: " + agentNo + " is empty and agent no blocked");

						client.planNextHLA(agentLowLevelPlans, agentNo, false, false);
						
				//		System.err.println(agentLowLevelPlans.get(agentNo).toString());
						

					if(client.blocker.containsKey(agentNo)){
						int other = client.blocker.get(agentNo);
						client.agentOriginNode.get(client.blocker.get(agentNo)).blocked = false ;
						client.blocker.remove(agentNo);
						client.blocker.remove(other);
					}
					
					
					if (agentLowLevelPlans.get(agentNo).isEmpty()){
				//	System.err.println("no plan need to relax");
						client.planNextHLA(agentLowLevelPlans, agentNo, true, false);
					}
					
					
				}
				
				
				
				

			}
			
			
			String jointAction = client.formNextJointAction(agentLowLevelPlans);  /// forming join action

			System.out.println(jointAction); // send it to the server
			String response = serverMessages.readLine();
			

			
		    System.err.print("Action:" + jointAction);
			System.err.println(" Response:" + response);
			System.err.println("");
			boolean[] parsedResponse = responsePar.parseResponse(response);
			
			boolean[] editedcopyOfResponse = new boolean[parsedResponse.length];
			
			System.arraycopy(parsedResponse, 0, editedcopyOfResponse, 0, parsedResponse.length);
			
			int allnoop = 0 ;
			for(int l=0;l<parsedResponse.length;l++){
				if(actualAction.get(l).equals("NoOp") || parsedResponse[l] == false ){
					editedcopyOfResponse[l] = false ;
					allnoop++;
					
				}
			}
			
			if(allnoop==client.agentCount){
				client.deadlock = true ;
			}
			
			client.deadlockdetector(editedcopyOfResponse);
		
			boolean flag = true ; // assume only two agents
		
			for (int i = 0; i < parsedResponse.length; i++) {
				if (parsedResponse[i] && !client.deadlock) {
					RemoveJointAction(agentLowLevelPlans, i);
				} 
				else if (trials[i] == 1) {
					// replan
				//	  System.err.print("Rplaning for agent:" + i);
					  
					Node n = client.agentOriginNode.get(i); 
					client.agentOriginNode.get(i).updatePerception(client.perception);
					
					
					agentLowLevelPlans.get(i).clear();
					
					replaned[i] = true ;
					trials[i]++;
					
				}
				else if(trials[i]==0){
					trials[i]++;
				}

				else if (trials[i] > 1 && flag) {
                    
					Node n = client.agentOriginNode.get(i); 
		
					client.deadlock = false ; 
					
					n.updatePerception(client.perception);
					

					if (client.getBlockingObjectType(n, i).equals("box")) {

				//		System.err.println("Enter box condition");

						int agentbox = client.getAgentBox(n);

						int highPagent = client.GetHighLowPriorityAgent(n, i, agentbox, "box")[0];
						int lowPagent = client.GetHighLowPriorityAgent(n, i, agentbox, "box")[1];

					//	 System.err.println("*High agent:"+highPagent+" , Low agent"+lowPagent);
						
						if(!client.deadlockhandler(lowPagent, n, agentLowLevelPlans,highPagent,"box")){
							client.PlanForStoreTemp(n, highPagent, lowPagent, agentLowLevelPlans);
						}
						
						
						

		    		}
		    		else if(client.getBlockingObjectType(n,i).equals("agent")){
		    				    			
						//	System.err.println("Enter agent condition");
		    			
							int highPagent = client.GetHighLowPriorityAgent(n, i, client.getOpposingAgent(n), "agent")[0];
							int lowPagent = client.GetHighLowPriorityAgent(n, i, client.getOpposingAgent(n), "agent")[1];
		    			
							//   System.err.println("*High agent:"+highPagent+" , Low agent"+lowPagent);
							   
							   
						if(!client.deadlockhandler(lowPagent, n, agentLowLevelPlans,highPagent,"agent")){
						//	  System.err.println("no deadlock");
							client.PlanForGiveWay(n, highPagent, lowPagent,agentLowLevelPlans);
						}
							
		    		
		    			   
		    			   
		    			   
				
		    		}
		    		
		    		
		    	
					
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
			
			}
			
			int noOfEmptyPlans = 0;
			for(int agentNo = 0; agentNo < agentCount; agentNo++) {
				Node n = client.agentOriginNode.get(agentNo);
				if(n.plannedActions.isEmpty() && agentLowLevelPlans.get(agentNo).isEmpty()) {
					n.checkHLAs();	
					if(n.plannedActions.isEmpty()) {
						noOfEmptyPlans += 1;
					}
				}
			}
			
			if(noOfEmptyPlans == agentCount) {
				break;
			}
			
			client.highlowp.clear();
	
	} 
	
	}
}

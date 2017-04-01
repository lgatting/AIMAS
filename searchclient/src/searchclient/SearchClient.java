package searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import searchclient.Memory;
import searchclient.Strategy.*;
import searchclient.Heuristic.*;
import searchclient.ElementWithColor.*;



public class SearchClient {
	public Node initialState;

	public SearchClient(BufferedReader serverMessages) throws Exception {
		List<String> lines = new ArrayList<String>();
	
		
		HashMap<Character, Color> colorAssignments = new HashMap<Character, Color>();
		
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
		this.initialState = new Node(null, rows, cols);
		this.initialState.setcolormap(colorAssignments);

		for (String line : lines) {
			for (int col = 0; col < line.length(); col++) {
				char chr = line.charAt(col);

				if (chr == '+') { // Wall.
					this.initialState.walls[row][col] = true;
				} else if ('0' <= chr && chr <= '9') { // Agent.
					
					int agentNo = Character.getNumericValue(chr);
					
					if (agentFound[agentNo]) {
						System.err.println("Error, multiple agents with the same number");
						System.exit(1);
					}
					
					agentFound[agentNo] = true;
										
					this.initialState.agents[row][col]=agentNo;
					
					
				} else if ('A' <= chr && chr <= 'Z') { // Box.
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
		
		
		
	}

	public LinkedList<Node> Search(Strategy strategy) throws IOException {
		System.err.format("Search starting with strategy %s.\n", strategy.toString());
		strategy.addToFrontier(this.initialState);

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
			for (Node n : leafNode.getExpandedNodes()) { // The list of expanded nodes is shuffled randomly; see Node.java.
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
					try{
						System.err.print(n.actions[0].toString());
					}
					 catch(NullPointerException e){
						 System.err.print("NoOp"); 
					 }
					try{
						System.err.println(n.actions[1].toString());
					}
					 catch(NullPointerException e){
						 System.err.println("NoOp"); 
					 }
					strategy.addToFrontier(n);
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

		
        Strategy strategy;
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "-bfs":
                    strategy = new StrategyBFS();
                    break;
                case "-dfs":
                    strategy = new StrategyDFS();
                    break;
                case "-astar":
                    strategy = new StrategyBestFirst(new AStar(client.initialState));
                    break;
                case "-wastar":
                    // You're welcome to test WA* out with different values, but for the report you must at least indicate benchmarks for W = 5.
                    strategy = new StrategyBestFirst(new WeightedAStar(client.initialState, 5));
                    break;
                case "-greedy":
                    strategy = new StrategyBestFirst(new Greedy(client.initialState));
                    break;
                default:
                    strategy = new StrategyBFS();
                    System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
            }
        } else {
            strategy = new StrategyBFS();
            System.err.println("Defaulting to BFS search. Use arguments -bfs, -dfs, -astar, -wastar, or -greedy to set the search strategy.");
        }

		LinkedList<Node> solution;
		try {
			solution = client.Search(strategy);
		} catch (OutOfMemoryError ex) {
			System.err.println("Maximum memory usage exceeded.");
			solution = null;
		}

		if (solution == null) {
			System.err.println(strategy.searchStatus());
			System.err.println("Unable to solve level.");
			System.exit(0);
		} else {
			System.err.println("\nSummary for " + strategy.toString());
			System.err.println("Found solution of length " + solution.size());
			System.err.println(strategy.searchStatus());

			for (Node n : solution) {
				String act = n.action.toString();
				
				System.out.println(act);
				String response = serverMessages.readLine();
				if (response.contains("false")) {
					System.err.format("Server responsed with %s to the inapplicable action: %s\n", response, act);
					System.err.format("%s was attempted in \n%s\n", act, n.toString());
					break;
				}
			}
		}
	}
}

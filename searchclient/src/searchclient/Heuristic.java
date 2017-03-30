package searchclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import searchclient.NotImplementedException;

public abstract class Heuristic implements Comparator<Node> {
	
	int oldBoxesNotSatisfied ;
	
	public ArrayList<int[]> goals;
	
	
	public Heuristic(Node initialState) {
		// Here's a chance to pre-process the static parts of the level.

		goals = new ArrayList<int[]>();
		
		for (int row = 1; row < initialState.rows - 1; row++) {
			for (int col = 1; col < initialState.cols - 1; col++) {
				char g = initialState.goals[row][col];
				if (g > 0) {
					int[] pos = {row, col, 0};
					
					goals.add(pos);
				}
			}
		}
	}
	
	public int getAgentAtPos(Node n, int row, int col){
		for (int agentNo = 0; agentNo < n.agentCount; agentNo++){
			if(n.agents[agentNo][0] == row && n.agents[agentNo][1] == col){
				return agentNo;
			}
		}
		return -1;
	}

	public int h(Node n) {
		double closestBox = -1;
		double goalToBoxDistances = 0;
		int boxesNotSatisfied = goals.size();
		
		String table = "" ;
		
		
                                  
        
		for (int agentNo = 0; agentNo < n.agentCount; agentNo++){
			char c = Character.toLowerCase(n.goals[n.agents[agentNo][0]][n.agents[agentNo][1]]);
			if (c > 0) { // prevent agent to step over a goal
                return 10000000 ;
            }   
			for (int row = 1; row < n.rows - 1; row++) {
				for (int col = 1; col < n.cols - 1; col++) {
					
					char b = Character.toLowerCase(n.boxes[row][col]);
					char g = n.goals[row][col];
					
					int agentAtPos = getAgentAtPos(n, row, col);
					if(agentAtPos >= 0) table += agentAtPos;
					else if(b>0) table += n.boxes[row][col] ;
	             	else if(g>0) table += g ; 
	             	else table += " ";
					
					if (b > 0 && n.goals[row][col] != b) { 
					
						int rowdiff = row - n.agents[agentNo][0];
						int coldiff = col - n.agents[agentNo][1];

						double distance = Math.sqrt(Math.pow(Math.abs(rowdiff),2) + Math.pow(Math.abs(coldiff),2));
						
						if (closestBox == -1 || closestBox > distance)
							closestBox = distance;
						
						// We now have the distance to closest box from the agent
						
						Map<Character, Double> goalDists = new HashMap<Character, Double>();
						for (int[] goalPos : goals) {
							int gr = goalPos[0];
							int gc = goalPos[1];
							char goalChar = n.goals[gr][gc];
							
							if (goalChar == b) {
								rowdiff = gr - row;
								coldiff = gc - col;
								
								distance = Math.abs(rowdiff) + Math.abs(coldiff);
								
								if (!goalDists.containsKey(goalChar))
									goalDists.put(goalChar, distance);
								
								else if (goalDists.get(goalChar) > distance) {
									goalDists.put(goalChar, distance);
								}
							}
						}
						
						Iterator it = goalDists.entrySet().iterator();
						while(it.hasNext()) {
							Map.Entry<Character, Double> pair = (Map.Entry<Character, Double>)it.next();
							goalToBoxDistances += pair.getValue();
						}
						
						// Now we have found closest distance between goals and their box
					}
				}	
				table += '\n';
			}
		}
		
		
		
		
		
		for (int i = 0; i < goals.size(); i++) {
			int[] goalPos = goals.get(i);
			int gr = goalPos[0];
			int gc = goalPos[1];

            if (Character.toLowerCase(n.boxes[gr][gc]) == n.goals[gr][gc]) {
            	boxesNotSatisfied -= 1;            
			}
             
            if(oldBoxesNotSatisfied!=boxesNotSatisfied) {
            	closestBox = 0 ;
        	}
            oldBoxesNotSatisfied = boxesNotSatisfied;                      
        }
		
		
		// Now we also know how many boxes have not been satisfied so far
		/*
        if(n.agentCol==21 && n.agentRow==1) {
		try {
                    String txt = "----choice "+'\n'+table+ +'\n'+"closestBox"+(int)Math.round(closestBox)+" "
                            + "goalToBoxDistances"+(int)Math.round(goalToBoxDistances)+" "
                              + "boxesNotSatisfied"+(int)Math.round(boxesNotSatisfied)+" "                  
                              + "total"+(int)Math.round(closestBox + goalToBoxDistances + boxesNotSatisfied)+" "
                            +'\n'+'\n';
    Files.write(Paths.get("log.txt"), txt.getBytes(), StandardOpenOption.APPEND);
}catch (IOException e) {
    //exception handling left as an exercise for the reader
}}
*/
		
		return (int) (goalToBoxDistances + boxesNotSatisfied);
	}
	
	
	/*public int findShortestDistanceToBox(Node n){
		for (int row = 1; row < n.rows - 1; row++) {
			for (int col = 1; col < n.cols - 1; col++) {
				char b = n.boxes[row][col];
				if(b > 0){
					char goals[][] = n.goals;
					
				}
			}
		}
		
		return 0;
	}*/

	public abstract int f(Node n);

	@Override
	public int compare(Node n1, Node n2) {
		return this.f(n1) - this.f(n2);
	}

	public static class AStar extends Heuristic {
		public AStar(Node initialState) {
			super(initialState);
		}

		@Override
		public int f(Node n) {
			return n.g() + this.h(n);
		}

		@Override
		public String toString() {
			return "A* evaluation";
		}
	}

	public static class WeightedAStar extends Heuristic {
		private int W;

		public WeightedAStar(Node initialState, int W) {
			super(initialState);
			this.W = W;
		}

		@Override
		public int f(Node n) {
			return n.g() + this.W * this.h(n);
		}

		@Override
		public String toString() {
			return String.format("WA*(%d) evaluation", this.W);
		}
	}

	public static class Greedy extends Heuristic {
		public Greedy(Node initialState) {
			super(initialState);
		}

		@Override
		public int f(Node n) {
			return this.h(n);
		}

		@Override
		public String toString() {
			return "Greedy evaluation";
		}
	}
}
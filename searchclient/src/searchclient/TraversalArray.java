package searchclient;

import java.util.HashMap;
import java.util.LinkedList;

public class TraversalArray {

	
	public static boolean[][] generateTraversalArray(Node n, int agentid, HashMap<Integer, LinkedList<Node>> agentPlans) {
		boolean[][] plan = new boolean[n.rows][n.cols];
		
		// assume 2 agents 1 and 0
		LinkedList<Node> agentPlan = agentPlans.get(agentid);
		for(int step=0; step<agentPlan.size(); step++){
			int newagentrow = agentPlan.get(step).agents[agentid][0]; // agent row
    		int newagentcol = agentPlan.get(step).agents[agentid][1]; // agent col
    		plan[newagentrow][newagentcol] = true ;
		}
		
		
		for(int r=0; r<n.rows; r++){
			for(int c=0; c<n.cols; c++){
				System.err.print(plan[r][c]+" ");
			}
			System.err.println("");
		}
		System.err.println("----------------------------------");
		
		return plan;
	}
}
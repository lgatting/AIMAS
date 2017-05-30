package searchclient;

import java.util.HashMap;
import java.util.LinkedList;

import searchclient.ElementWithColor.Color;

public class TraversalArray {

	
	public static boolean[][] generateTraversalArray(Node n, int agentid, HashMap<Integer, LinkedList<Node>> agentPlans, int conflictingAgent) {
		boolean[][] plan = new boolean[n.rows][n.cols];
		
	
		LinkedList<Node> agentPlan = agentPlans.get(agentid);
		
	
		
		
			plan[n.agents[agentid][0]][n.agents[agentid][1]] = true ;
		
	
		
		for(int step=0; step<agentPlan.size(); step++){ 
			int newagentrow = agentPlan.get(step).agents[agentid][0]; 
    		int newagentcol = agentPlan.get(step).agents[agentid][1]; 
    		plan[newagentrow][newagentcol] = true ;
		}
		
		
		

		
		
		for(int r=0; r<n.rows; r++){
			for(int c=0; c<n.cols; c++){
			//	System.err.print(plan[r][c]+" ");
			}
		//	System.err.println("");
		}
		
		return plan;
	}
	
	
	public static boolean[][] generateTraversalArray2(Node n, HashMap<Integer, LinkedList<Node>> agentPlans) {
		boolean[][] plan = new boolean[n.rows][n.cols];
		
		
        for(int agentid=0 ; agentid<n.agentCount ; agentid++){
        	
		
        	LinkedList<Node> agentPlan = agentPlans.get(agentid);

		for(int step=0; step<agentPlan.size(); step++){ /// goes trgough agent plan and mark all cells that will be going to enter
			int newagentrow = agentPlan.get(step).agents[agentid][0]; // agent row
    		int newagentcol = agentPlan.get(step).agents[agentid][1]; // agent col
    		plan[newagentrow][newagentcol] = true ;
		}
		
		
		
		for(int r=0; r<n.rows; r++){
			for(int c=0; c<n.cols; c++){

				if(n.goalIds[r][c]!=0){

					Color color = n.colorAssignments.get(Character.toUpperCase(n.goalIds[r][c]));


					if(n.colorAssignments.get((char) (agentid + '0')).equals(color)) { 
					
						plan[r][c] = true ;
					}


				
				}


			}

		}
		
		
		
		
	} 
        return plan;
        }
}
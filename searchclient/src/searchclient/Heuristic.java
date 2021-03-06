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
import java.util.List;
import java.util.Map;

import models.*;

import searchclient.Command.Type;
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
		// Since we cannot use decimal numbers for the comparator, we'll multiply the result to account for the small differences
		int precision = 100;
		
		if (n.curAction == null) {
			// No other HLAs; heuristic function cannot help 
			return 1;
		}
		
		HighLevelAction hla = n.curAction;
		
		int pastGoalSatisficationHLAsCount = 0;
		if (n.curAction instanceof SatisfyGoalHLA)
			pastGoalSatisficationHLAsCount++;
		
		if (hla instanceof GoToHLA) {
			GoToHLA action = (GoToHLA) hla;

			int agentRow = n.agents[n.agentNo][0];
			int agentCol = n.agents[n.agentNo][1];
			int boxRow = -1;
			int boxCol = -1;
			
			for (int row = 1; row < n.rows - 1; row++) {
				for (int col = 1; col < n.cols - 1; col++) {
					if (n.boxIds[row][col] == action.box.id) {
						// We have found the box towards which we want to move
						
						boxRow = row;
						boxCol = col;
					}
					
					// Sufficient to check just row/col since once either of them has been set to something else, then
					// the other value must've been set as well
					if (boxRow != -1) {
						if (Utils.isNeighboringPosition(agentRow, agentCol, boxRow, boxCol)) {
							return 0;
						} 
							
						// We now have found both the agent and the box; 
						// Calculate the distance between them

						
						BFS dbfs = new BFS(n);
						
						int dist = dbfs.distance(agentRow, agentCol, boxRow, boxCol);
						
						int cost = (int)Math.round(dist * precision);
						
						cost += Math.abs(n.unsatisfiedGoalCount()) * precision * 5000; // 999999
						
						if (n.action.actionType != Type.Move)
							// Previously MAX_VALUE
							cost += 10 * precision;
						
						if (n.action.actionType == Type.Pull)
							cost += 2 * precision;
						
						return cost;
					}
				}
			}
		} else if (hla instanceof SatisfyGoalHLA) {
			SatisfyGoalHLA action = (SatisfyGoalHLA) hla;

			int agentRow = n.agents[n.agentNo][0];
			int agentCol = n.agents[n.agentNo][1];
			int boxRow = -1;
			int boxCol = -1;
			int goalRow = -1;
			int goalCol = -1;
			
			for (int row = 1; row < n.rows - 1; row++) {
				for (int col = 1; col < n.cols - 1; col++) {
					if (n.boxIds[row][col] == action.box.id) {
						// We have found the box towards which we want to move
						
						boxRow = row;
						boxCol = col;
					}
					
					if (n.goalIds[row][col] == action.box.goal.id) {
						// We have found the goal, which we want to satisfy

						goalRow = row;
						goalCol = col;
					}
					
					// Sufficient to check just row/col since once either of them has been set to something else, then
					// the other value must've been set as well
					if (boxRow != -1 && goalRow != -1) {
//						.println(boxRow + "," + boxCol + "," + goalRow + "," + goalCol);
						if (goalRow == boxRow && goalCol == boxCol) {
//							// This action has been satisfied, move to next HLA
//							n.pastActions.add(n.curAction);
//							//n.curAction = null;
//							.println("SatisfyGoalHLA satisfied, now removed");
//							.println("Remaining unsatisfied HLAs: " + n.unsatisfiedGoalCount());
//							.println(n.curAction);
//							//n.strategy.refresh(n);
//							
//							List<Node> nodes = n.getExpandedNodes(n.agentNo);
//
////							if (n.curAction.size() == 0)
////								n.checkHLAs();
//
//							for (Node newNode : nodes)
//								n.strategy.addToFrontier(newNode);
							
							return 0;
						}
							
						// We now have found both the agent and the box; 
						// Calculate the distance between them

//						int w = Math.abs(goalRow - boxRow);
//						int h = Math.abs(goalCol - boxCol);
//
//						double distBG = Math.sqrt(w*w + h*h);
						
						BFS dbfs = new BFS(n);
						
						int distBG = dbfs.distance(boxRow, boxCol, goalRow, goalCol);
						//.println("distBG: " + distBG);
						
//						if(distBG == -1) {
//							.println("No path from box at (" + boxRow + "," + boxCol + ") to goal at (" + goalRow + "," + goalCol + ")");
//						}
						
//						int w = Math.abs(agentRow - boxRow);
//						int h = Math.abs(agentCol - boxCol);
//						
//						double distAB = Math.sqrt(w*w + h*h);
						
						int distAB = dbfs.distance(agentRow, agentCol, boxRow, boxCol);
						
						// The agent should stay as close to his box as possible at all times
						distAB = distAB * 50;
						
						int dist = distBG + distAB;
						
						int cost = (int)Math.round(dist * precision);
						
						cost += Math.abs(n.unsatisfiedGoalCount()) * precision * 1000;
						
						// Prefer pushing to pulling mainly because of the corridors since we don't want to end up
						// locked up in there
						if (n.action.actionType == Type.Pull)
							cost += 0 * precision;
						
						if(action.box.letter == 'c') {
						//	.println("aRow, aCol:" + agentRow + "," + agentCol);
						}
						
						//cost += n.boxesOnWrongGoalsCount() * 1000;
						if(action.box.letter == 'c') {
						//	.println("BG, AB:" + distBG + "," + distAB);
						}
						
						return cost;
					}
				}
			}
		}
		else if (hla instanceof GiveWayHLA) {
			GiveWayHLA action = (GiveWayHLA) hla;

			int agentRow = n.agents[n.agentNo][0];
			int agentCol = n.agents[n.agentNo][1];
			
			BFS dbfs = new BFS(n);
			
			int dist = dbfs.distance(agentRow, agentCol, action.cell[0], action.cell[1]);

//			.println("dist: " + dist);
//			.println("agentRow: " + agentRow);
//			.println("agentCol: " + agentCol);
//			.println("cell[0]: " + action.cell[0]);
//			.println("cell[1]: " + action.cell[1]);
			//System.exit(1);
			
			int cost = (int)Math.round(dist * precision);
			
			if (n.action.actionType != Type.Move)
				// Previously MAX_VALUE
				cost += 10 * precision;
			
			if (n.action.actionType == Type.Pull)
				cost += 2 * precision;
			
			return cost;
		}
		else if (hla instanceof StoreTempHLA) {
			StoreTempHLA action = (StoreTempHLA) hla;

			int agentRow = n.agents[n.agentNo][0];
			int agentCol = n.agents[n.agentNo][1];
			int boxRow = -1;
			int boxCol = -1;
			
			for (int row = 1; row < n.rows - 1; row++) {
				for (int col = 1; col < n.cols - 1; col++) {
					if (n.boxIds[row][col] == action.box.id) {
						// We have found the box towards which we want to move
						
						boxRow = row;
						boxCol = col;
					}
					
					// Sufficient to check just row/col since once either of them has been set to something else, then
					// the other value must've been set as well
					if (boxRow != -1) {
						// We now have found both the agent and the box; 
						// Calculate the distance between them
						
						BFS dbfs = new BFS(n);
						
						int distBC = dbfs.distance(boxRow, boxCol, action.cell[0], action.cell[1]);
						
						int w = Math.abs(agentRow - boxRow);
						int h = Math.abs(agentCol - boxCol);
						
						double distAB = Math.sqrt(w*w + h*h);
						
						// The agent should stay as close to his box as possible at all times
						distAB = distAB * 50;
						
						double dist = distBC + distAB;
						
						int cost = (int)Math.round(dist * precision);
						
						cost += Math.abs(n.unsatisfiedGoalCount()) * precision * 1000;
						
						// Prefer pushing to pulling mainly because of the corridors since we don't want to end up
						// locked up in there
						if (n.action.actionType == Type.Pull)
							cost += 0 * precision;
						
						if(action.box.letter == 'c') {
						//	.println("aRow, aCol:" + agentRow + "," + agentCol);
						}
						
						//cost += n.boxesOnWrongGoalsCount() * 1000;
						if(action.box.letter == 'c') {
					//		.println("BC, AB:" + distBC + "," + distAB);
						}
						
						return cost;
					}
				}
			}
		}
		else {
			// Unknown action; heuristic is unable to help
			return 1000000;
		}
		
		// This scenario will not happen since both agent and the box must be eventually found
		return 1000000;
	}

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
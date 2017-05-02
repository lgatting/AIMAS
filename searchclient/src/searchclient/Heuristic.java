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

import models.*;

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

	/**
	 * Whether the first position (x1, y1) is a direct neighbor to x2, y2.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return True if it is, false otherwise.
	 */
	private boolean isNeighboringPosition(int x1, int y1, int x2, int y2) {
		return Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1;
	}

	public int h(Node n) {
		// Since we cannot use decimal numbers for the comparator, we'll multiply the result to account for the small differences
		int precision = 1000;
		
		if (n.agentsActions.size() == 0) {
			// No other HLAs; heuristic function cannot help 
			return 1;
		}
		
		HighLevelAction hla = n.agentsActions.get(0);
		
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
						if (isNeighboringPosition(agentRow, agentCol, boxRow, boxCol)) {
							// This action has been satisfied, move to next HLA
							n.pastActions.add(n.agentsActions.remove(0));
							System.err.println("removed1");
							System.err.println(n.agentsActions);
							n.strategy.refreshFrontier();
							return h(n);
						} 
							
						// We now have found both the agent and the box; 
						// Calculate the distance between them

						int w = Math.abs(agentRow - boxRow);
						int h = Math.abs(agentCol - boxCol);

						double dist = Math.sqrt(w*w + h*h);
						
						int cost = (int)Math.round(dist * precision);
						
						
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
						if (goalRow == boxRow && goalCol == boxCol) {
							// This action has been satisfied, move to next HLA
							n.pastActions.add(n.agentsActions.remove(0));
							System.err.println("removed2");
							System.err.println(n.agentsActions);
							n.strategy.refreshFrontier();
							return h(n);
						}
							
						// We now have found both the agent and the box; 
						// Calculate the distance between them

						int w = Math.abs(goalRow - boxRow);
						int h = Math.abs(goalCol - boxCol);

						double distBG = Math.sqrt(w*w + h*h);
						
						w = Math.abs(agentRow - boxRow);
						h = Math.abs(agentCol - boxCol);
						
						double distAB = Math.sqrt(w*w + h*h);
						
						double dist = distBG;
						
						int cost = (int)Math.round(dist * precision);
						
						return cost;
					}
				}
			}
		} else {
			// Unknown action; heuristic is unable to help
			return 1;
		}
		
		// This scenario will not happen since both agent and the box must be eventually found
		return 1;
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
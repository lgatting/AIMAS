package searchclient;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.PriorityQueue;

import searchclient.ElementWithColor.Color;

public class Matt {
	boolean[][] walls;
	char[][] goals;
	char[][] levelToSearch;
	char[][] copyOfLevelToSearch;
	
	int rows;
	int cols;
	
	DistanceBFS dbfs;
	
	PriorityQueue<int[]> goalQueue;
	
	HashMap<Character, Color> colorAssignments = new HashMap<Character, Color>();
	
	HashMap<Integer, int[]> goalIndexes = new HashMap<Integer, int[]>();
	
	public Matt(Node n){
		this.goalQueue = new PriorityQueue<int[]>(new GoalPriorityComparator());
		
		this.walls = n.walls;
		this.goals = n.goals;
		
		levelToSearch = new char[rows][cols];
		
		for(int row = 0; row < rows; row++) {
			for(int col = 0; col < cols; col++) {
				if(goals[row][col] > 0){
					levelToSearch[row][col] = goals[row][col];
				}
				else if(n.boxes[row][col] > 0){
					levelToSearch[row][col] = n.boxes[row][col];
				}
			}
		}
		
		this.dbfs = new DistanceBFS(n);
	}
	
	public boolean[][] fillDependenceMatrix(int startRow, int startCol){
		int goalCount = 0;
		
		// To reuse this object for searches, make a copy of the levelToSearchArray here!
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < cols; col++){
				if(goals[row][col] > 0){
					goalIndexes.put(goalCount, new int[]{row, col, goals[row][col]});
					int priority = dbfs.closestBoxFromGoal(row, col, goals[row][col]);
					if(priority > 0){
						int[] goalCell = {row, col, priority};
						goalQueue.offer(goalCell);
						goalCount++;
					}
				}
			}
		}
		
		boolean[][] dependencyMatrix = new boolean[goalCount][goalCount];
		
		// Check for each prioritised goal whether they can be solved without depending on other goals.
		
		copyOfLevelToSearch = new char[rows][cols];
		
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < cols; col++){
				copyOfLevelToSearch[row][col] = levelToSearch[row][col];
			}
		}
		
		for(int goal = 0; goal < goalCount; goal++){
			for(int dependsOnGoal = 0; dependsOnGoal < goalCount; dependsOnGoal++){
				if(goal != dependsOnGoal){
					int[] goalPos = goalIndexes.get(goal);
					int[] dependsOnGoalPos = goalIndexes.get(dependsOnGoal);
					
					dbfs.setWall(dependsOnGoalPos[0], dependsOnGoalPos[1], true);
					int closestDistance = dbfs.closestBoxFromGoal(goalPos[0], goalPos[1], (char) goalPos[2]);
					dbfs.setWall(dependsOnGoalPos[0], dependsOnGoalPos[1], false);
					
					// If there is a path to the goal when the one goal is considered as a wall, then set to true
					dependencyMatrix[goal][dependsOnGoal] = closestDistance >= 0;
				}
				else {
					dependencyMatrix[goal][dependsOnGoal] = false;
				}
			}
		}
		
		return dependencyMatrix;
		
	}
}

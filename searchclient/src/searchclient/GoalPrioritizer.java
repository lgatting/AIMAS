package searchclient;

import java.util.ArrayDeque;
import java.util.PriorityQueue;

public class GoalPrioritizer {
	boolean[][] walls;
	char[][] goals;
	int[][] levelToSearch;
	char[][] copyOfLevelToSearch;
	
	int rows;
	int cols;
	
	DistanceBFS dbfs;
	
	PriorityQueue<int[]> goalQueue;
	
	ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
	
	public GoalPrioritizer(boolean[][] walls, char[][] goals, char[][] boxes, int rows, int cols){
		this.goalQueue = new PriorityQueue<int[]>(new GoalPriorityComparator());
		
		this.walls = walls;
		this.goals = goals;
		
		for(int row = 0; row < rows; row++) {
			for(int col = 0; col < cols; col++) {
				if(goals[row][col] > 0){
					levelToSearch[row][col] = goals[row][col];
				}
				else if(boxes[row][col] > 0){
					levelToSearch[row][col] = boxes[row][col];
				}
			}
		}
		
		this.dbfs = new DistanceBFS(walls, boxes, rows, cols);
	}
	
	public int[][] prioritizeGoals(int startRow, int startCol){
		
		// To reuse this object for searches, make a copy of the levelToSearchArray here!
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < cols; col++){
				if(goals[row][col] > 0){
					int priority = dbfs.closestBoxFromGoal(row, col, goals[row][col]);
					if(priority > 0){
						int[] goalCell = {row, col, priority};
						goalQueue.offer(goalCell);
					}
				}
			}
		}
		
		// Check for each prioritised goal whether they can be solved without depending on other goals.
		
		levelToSearch = new int[rows][cols];
		
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				levelToSearch[i][j] = 0;
			}
		}
		
		this.walls = walls;
		queue.add(createPosArray(startRow, startCol));
		
		boolean complete = false;
		
		while(!queue.isEmpty()) {
			addJobsToQueue();
		}
		
		return levelToSearch;
		
	}
	
	private int[] createPosArray(int row, int col){
		int[] posArray = new int[2];
		posArray[0] = row;
		posArray[1] = col;
		return posArray;
	}
	
	public void addJobsToQueue(){	//Adds 
		int[] curPos = queue.remove();
		int row = curPos[0];
		int col = curPos[1];
		levelToSearch[row][col] = curRoom;
		
		if(!walls[row-1][col] && levelToSearch[row-1][col] == 0) {	// If the neighbouring cell is not a wall
			queue.add(createPosArray(row-1,col));
		}
		if(!walls[row+1][col] && levelToSearch[row+1][col] == 0) {
			queue.add(createPosArray(row+1,col));
		}
		if(!walls[row][col-1] && levelToSearch[row][col-1] == 0) {
			queue.add(createPosArray(row,col-1));
		}
		if(!walls[row][col+1] && levelToSearch[row][col+1] == 0) {
			queue.add(createPosArray(row,col+1));
		}
	}
}

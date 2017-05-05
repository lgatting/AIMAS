package searchclient;

import java.util.ArrayDeque;
import java.util.HashMap;

import searchclient.ElementWithColor.Color;

// Used to calculate the closest distance to a box from a goal.
public class DistanceBFS {
	boolean[][] walls;
	int[][] agents;
	char[][] levelToSearch;
	char[][] copyOfLevelToSearch;
	HashMap<Character, Color> colorAssignments;
	
	int rows;
	int cols;
	
	char boxChar;
	
	ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
	
	public static enum Direction {
		left, right, up, down
	}
	
	public DistanceBFS(boolean[][] walls, char[][] boxes, int[][] agents, HashMap<Character, Color> colorAssignments, int rows, int cols){
		this.walls = walls;
		this.rows = rows;
		this.cols = cols;
		this.agents = agents;
		this.colorAssignments = colorAssignments;
		
		levelToSearch = new char[rows][cols];
		copyOfLevelToSearch = new char[rows][cols];
		
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < cols; col++){
				if(boxes[row][col] > 0){
					levelToSearch[row][col] = boxes[row][col];
				}
				else {
					levelToSearch[row][col] = '?';
				}
			}
		}
	}
	
	public int closestBoxFromGoal(int startRow, int startCol, char goalChar){
		init(startRow, startCol);
		
		this.boxChar =  java.lang.Character.toUpperCase(goalChar);
		
		return performSearch(null, null, null, 0);
		
	}
	
	public int closestMovableBoxFromAgent(int startRow, int startCol, int agent){
		init(startRow, startCol);
		
		Color agentColor = this.colorAssignments.get((char) (agent  + '0'));
		
		System.err.println("" + startRow + "," + startCol + "," +agentColor);
		
		return performSearch(agentColor, null, null, 1);
	}
	
	// otherAgentsPlan is a boolean array with the fields that other agents are planning to traverse set to true
	public int closestSafeCellForBox(boolean[][] otherAgentsPlan, char[][] agents, int boxStartRow, int boxStartCol){		
		init(boxStartRow, boxStartCol);
		
		char boxChar = levelToSearch[boxStartRow][boxStartCol];
		
		Color boxColor = this.colorAssignments.get(boxChar);
		
		System.err.println("" + boxStartRow + "," + boxStartCol);
		
		return performSearch(null, otherAgentsPlan, boxColor, 2);
	}
	
	public void init(int row, int col){
		resetCopyOfLevel();
		copyOfLevelToSearch[row][col] = '!';	// Mark current cell as searched
		queue.add(createPosDistArray(row, col, 0));
	}
	
	public int performSearch(Color agentColor, boolean[][] otherAgentsPlan, Color boxColor, int mode){
		while(!queue.isEmpty()) {
			int result = exploreNew(agentColor, otherAgentsPlan, boxColor, mode);
			if(result >= 0){
				queue.clear();
				return result;
			}
		}
		
		return -1;
	}
	
	private int[] createPosDistArray(int row, int col, int dist){
		int[] posDistArray = new int[3];
		
		posDistArray[0] = row;
		posDistArray[1] = col;
		posDistArray[2] = dist;
		
		return posDistArray;
	}
	
	public int explore(){	// Explores cells and add them to the queue if the wanted box is not in the current cell
		int[] curPosDist = queue.remove();
		
		int row = curPosDist[0];
		int col = curPosDist[1];
		int dist = curPosDist[2];
		
		// If the wanted box is encountered, the distance from the goal to the box is returned
		if(copyOfLevelToSearch[row][col] == this.boxChar){
			return dist;
		}
		
		/*if(!walls[row-1][col] && copyOfLevelToSearch[row-1][col] != '!') {	// If the neighbouring cell is not a wall
			queue.add(createPosDistArray(row-1,col, dist+1));
			copyOfLevelToSearch[row][col] = '!';
		}
		if(!walls[row+1][col] && copyOfLevelToSearch[row+1][col] != '!') {
			queue.add(createPosDistArray(row+1,col, dist+1));
			copyOfLevelToSearch[row][col] = '!';
		}
		if(!walls[row][col-1] && copyOfLevelToSearch[row][col-1] != '!') {
			queue.add(createPosDistArray(row,col-1, dist+1));
			copyOfLevelToSearch[row][col] = '!';
		}
		if(!walls[row][col+1] && copyOfLevelToSearch[row][col+1] != '!') {
			queue.add(createPosDistArray(row,col+1, dist+1));
			copyOfLevelToSearch[row][col] = '!';
		}*/
		
		return -1;
	}
	
	// Explores cells and add them to the queue if a box of the same color as the agent
	// is not in the current cell
	public int explore2(Color agentColor){
		int[] curPosDist = queue.remove();
		
		int row = curPosDist[0];
		int col = curPosDist[1];
		int dist = curPosDist[2];
		
		// If the wanted box is encountered, the distance from the goal to the box is returned
		if(copyOfLevelToSearch[row][col] != '?' &&
		   this.colorAssignments.get(copyOfLevelToSearch[row][col]) == agentColor){
			return dist;
		}
		
		if(!walls[row-1][col] && copyOfLevelToSearch[row-1][col] != '!') {	// If the neighbouring cell is not a wall
			if(copyOfLevelToSearch[row-1][col] >= 'A' && copyOfLevelToSearch[row-1][col] <= 'Z' &&
				this.colorAssignments.get(copyOfLevelToSearch[row-1][col]) != agentColor){
				
			}
			else {
				queue.add(createPosDistArray(row-1,col, dist+1));
			}
			copyOfLevelToSearch[row][col] = '!';
		}
		if(!walls[row+1][col] && copyOfLevelToSearch[row+1][col] != '!') {
			if(copyOfLevelToSearch[row+1][col] >= 'A' && copyOfLevelToSearch[row+1][col] <= 'Z' &&
				this.colorAssignments.get(copyOfLevelToSearch[row+1][col]) != agentColor){
				
			}
			else {
				queue.add(createPosDistArray(row+1,col, dist+1));
			}
			copyOfLevelToSearch[row][col] = '!';
		}
		if(!walls[row][col-1] && copyOfLevelToSearch[row][col-1] != '!') {
			if(copyOfLevelToSearch[row][col-1] >= 'A' && copyOfLevelToSearch[row][col-1] <= 'Z' &&
				this.colorAssignments.get(copyOfLevelToSearch[row][col-1]) != agentColor){
				
			}
			else {
				queue.add(createPosDistArray(row,col-1, dist+1));
			}
			copyOfLevelToSearch[row][col] = '!';
		}
		if(!walls[row][col+1] && copyOfLevelToSearch[row][col+1] != '!') {
			if(copyOfLevelToSearch[row][col+1] >= 'A' && copyOfLevelToSearch[row][col+1] <= 'Z' &&
				this.colorAssignments.get(copyOfLevelToSearch[row][col+1]) != agentColor){
				
			}
			else {
				queue.add(createPosDistArray(row,col+1, dist+1));
			}
			copyOfLevelToSearch[row][col] = '!';
		}
		
		return -1;
	}
	
	/*
	 * This method returns the distance the closest free cell from the current position of a box.
	 * It serves as a heuristic to clear boxes off critical sections.
	 */
	public int explore3(boolean[][] otherAgentsPlan, Color boxColor){
		int[] curPosDist = queue.remove();
		
		int row = curPosDist[0];
		int col = curPosDist[1];
		int dist = curPosDist[2];
		
		// If a free cell (non-critical) is encountered, the distance from the box to the cell is returned
		if(!otherAgentsPlan[row][col]){
			return dist;
		}
		
		// Missing the logic to consider agents and their colors
		if(!walls[row-1][col] && copyOfLevelToSearch[row-1][col] != '!' && otherAgentsPlan[row-1][col] && !differentColoredAgentInCell(row-1,col, boxColor)) {	// If the neighbouring cell is not a wall
			queue.add(createPosDistArray(row-1,col, dist+1));
			copyOfLevelToSearch[row-1][col] = '!';
		}
		if(!walls[row+1][col] && copyOfLevelToSearch[row+1][col] != '!' && otherAgentsPlan[row+1][col] && !differentColoredAgentInCell(row+1,col, boxColor)) {
			queue.add(createPosDistArray(row+1,col, dist+1));
			copyOfLevelToSearch[row+1][col] = '!';
		}
		if(!walls[row][col-1] && copyOfLevelToSearch[row][col-1] != '!' && otherAgentsPlan[row][col-1] && !differentColoredAgentInCell(row,col-1, boxColor)) {
			queue.add(createPosDistArray(row,col-1, dist+1));
			copyOfLevelToSearch[row][col-1] = '!';
		}
		if(!walls[row][col+1] && copyOfLevelToSearch[row][col+1] != '!' && otherAgentsPlan[row][col+1] && !differentColoredAgentInCell(row,col+1, boxColor)) {
			queue.add(createPosDistArray(row,col+1, dist+1));
			copyOfLevelToSearch[row][col+1] = '!';
		}
		
		return -1;
	}
	
	public int exploreNew(Color agentColor, boolean[][] otherAgentsPlan, Color boxColor, int mode) {
		int[] curPosDist = queue.remove();
		
		int row = curPosDist[0];
		int col = curPosDist[1];
		int dist = curPosDist[2];
		
		switch(mode) {
			case 0:
				if(copyOfLevelToSearch[row][col] == this.boxChar){
					return dist;
				}
				break;
				
			case 1:
				if(copyOfLevelToSearch[row][col] != '?' &&
				   this.colorAssignments.get(copyOfLevelToSearch[row][col]) == agentColor){
					return dist;
				}
				break;
				
			case 2:
				if(!otherAgentsPlan[row][col]){
					return dist;
				}
				break;
		}
		
		dist += 1;
		
		for(Direction dir : Direction.values()){
			exploreNewAux(row, col, dist, agentColor, otherAgentsPlan, boxColor, dir, mode);
		}
		
		return -1;
	}
	
	/*
	 * This method contains conditions to continue the BFS and has several modes for the different cases.
	 * The current row, and column
	 * @param row
	 * @param col
	 * @param dist
	 * @param agentColor
	 * @param otherAgentsPlan
	 * @param boxColor
	 * @param dir
	 * @param mode
	 */
	public void exploreNewAux(int row, int col, int dist, Color agentColor, boolean[][] otherAgentsPlan, Color boxColor, Direction dir, int mode) {
		switch(dir) {
			case left:
				row -= 1;
				break;
			case right:
				row += 1;
				break;
			case up:
				col -= 1;
				break;
			case down:
				col += 1;
		}
		
		if(!walls[row][col] && copyOfLevelToSearch[row][col] != '!'){	// Check whether the new/neighbour cell has not already been explored
			switch(mode) {
			case 0:
				queue.add(createPosDistArray(row, col, dist));	// Explores cells and add them to the queue if the wanted box is not in the current cell
				break;
				
			case 1:
				if(copyOfLevelToSearch[row][col] >= 'A' && copyOfLevelToSearch[row][col] <= 'Z' &&
					this.colorAssignments.get(copyOfLevelToSearch[row][col]) != agentColor){	// Check that a box of the same color as the agent is not in the current cell
					break;
				}
				queue.add(createPosDistArray(row,col, dist));
				break;
				
			case 2:
				if(otherAgentsPlan[row][col] && !differentColoredAgentInCell(row, col, boxColor)) {	// Checks whether the cell is in a critical section. PROBLEM: Never adds the free cell!!!
					queue.add(createPosDistArray(row,col, dist));
				}
				
			}
			copyOfLevelToSearch[row][col] = '!';	// This marks that the cell has already been considered
		}
		
	}
	
	public boolean differentColoredAgentInCell(int row, int col, Color boxColor) {
		for(int i = 0; i < agents.length; i++) {
			if(agents[i][0] == row && agents[i][1] == col && colorAssignments.get((char) (i + '0')) != boxColor) {
				return true;
			}
		}
		return false;
	}
	
	public void setWall(int row, int col, boolean setTo){
		this.walls[row][col] = setTo;
	}
	
	public void resetCopyOfLevel(){
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < cols; col++){
				copyOfLevelToSearch[row][col] = levelToSearch[row][col];
			}
		}
	}
}

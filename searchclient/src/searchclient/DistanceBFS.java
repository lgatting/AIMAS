package searchclient;

import java.util.ArrayDeque;
import java.util.HashMap;

import searchclient.ElementWithColor.Color;

// Used to calculate the closest distance to a box from a goal.
public class DistanceBFS {
	boolean[][] walls;
	char[][] levelToSearch;
	char[][] copyOfLevelToSearch;
	
	int rows;
	int cols;
	
	char boxChar;
	
	ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
	
	HashMap<Character, Color> colorAssignments;
	
	public DistanceBFS(boolean[][] walls, char[][] boxes, HashMap<Character, Color> colorAssignments, int rows, int cols){
		this.walls = walls;
		this.rows = rows;
		this.cols = cols;
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
		
		resetCopyOfLevel();
		
		this.boxChar =  java.lang.Character.toUpperCase(goalChar);
		
		copyOfLevelToSearch[startRow][startCol] = '!';	// Mark current cell as searched
		
		queue.add(createPosDistArray(startRow, startCol, 0));
		
		while(!queue.isEmpty()) {
			int result = explore();
			if(result >= 0){
				queue.clear();
				return result;
			}
		}
		
		return -1;
		
	}
	
	public int closestMovableBoxFromAgent(int startRow, int startCol, int agent){
		resetCopyOfLevel();
		
		Color agentColor = this.colorAssignments.get((char) (agent  + '0'));
		
		System.err.println("" + startRow + "," + startCol + "," +agentColor);
		
		copyOfLevelToSearch[startRow][startCol] = '!';	// Mark current cell as searched
		
		queue.add(createPosDistArray(startRow, startCol, 0));
		
		while(!queue.isEmpty()) {
			int result = explore2(agentColor);
			if(result >= 0){
				queue.clear();
				return result;
			}
		}
		
		return -1;
	}
	
	// otherAgentsPlan is a boolean array with the fields that other agents are planning to traverse set to true
	public int closestSafeCellForBox(boolean[][] otherAgentsPlan, int boxStartRow, int boxStartCol){
		// If the box is in a non-critical cell
		if(!otherAgentsPlan[boxStartRow][boxStartCol]){
			return 0;
		}
		
		resetCopyOfLevel();
		
		char boxChar = levelToSearch[boxStartRow][boxStartCol];
		
		Color boxColor = this.colorAssignments.get(boxChar);
		
		System.err.println("" + boxStartRow + "," + boxStartCol);
		
		copyOfLevelToSearch[boxStartRow][boxStartCol] = '!';	// Mark current cell as searched
		
		queue.add(createPosDistArray(boxStartRow, boxStartCol, 0));
		
		while(!queue.isEmpty()) {
			int result = explore3(otherAgentsPlan, boxColor);
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
		
		if(!walls[row-1][col] && copyOfLevelToSearch[row-1][col] != '!') {	// If the neighbouring cell is not a wall
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
		}
		
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
		if(!walls[row-1][col] && copyOfLevelToSearch[row-1][col] != '!' && otherAgentsPlan[row-1][col]) {	// If the neighbouring cell is not a wall
			queue.add(createPosDistArray(row-1,col, dist+1));
			copyOfLevelToSearch[row-1][col] = '!';
		}
		if(!walls[row+1][col] && copyOfLevelToSearch[row+1][col] != '!' && otherAgentsPlan[row+1][col]) {
			queue.add(createPosDistArray(row+1,col, dist+1));
			copyOfLevelToSearch[row+1][col] = '!';
		}
		if(!walls[row][col-1] && copyOfLevelToSearch[row][col-1] != '!' && otherAgentsPlan[row][col-1]) {
			queue.add(createPosDistArray(row,col-1, dist+1));
			copyOfLevelToSearch[row][col-1] = '!';
		}
		if(!walls[row][col+1] && copyOfLevelToSearch[row][col+1] != '!' && otherAgentsPlan[row][col+1]) {
			queue.add(createPosDistArray(row,col+1, dist+1));
			copyOfLevelToSearch[row][col+1] = '!';
		}
		
		return -1;
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

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
	
	/**
 	 * Representation of a final position; The array is expected to have 2 values, [0] to represent rows, [1] to represent cols.
 	 * Used during computation in mode 3.
 	 */
 	int[] destination;

	
	ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
	
	public static enum Direction {
		left, right, up, down
	}
	
	public DistanceBFS(Node n){
		this.walls = n.walls;
		this.rows = n.rows;
		this.cols = n.cols;
		this.agents = n.agents;
		this.colorAssignments = n.colorAssignments;
		
		levelToSearch = new char[rows][cols];
		copyOfLevelToSearch = new char[rows][cols];
		
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < cols; col++){
				if(n.boxes[row][col] > 0){
					levelToSearch[row][col] = n.boxes[row][col];
//					System.err.print(levelToSearch[row][col]);
				}
				else {
					levelToSearch[row][col] = '?';
//					System.err.print(levelToSearch[row][col]);
				}
			}
//			System.err.println();
		}
	}
	
	/**
 	 * Finds a distance between [x1, y1] and [x2, y2] or returns -1 if there is no path.
 	 * @param x1
 	 * @param y1
 	 * @param x2
 	 * @param y2
 	 * @return
 	 */
	public int distance(int x1, int y1, int x2, int y2) {
 		init(x1, y1);
 		
 		destination = new int[] { x2, y2 };
 		
 		return performSearch(null, null, null, 3);
 	}
	
	/**
	 * Finds a distance between [x1, y1] and [x2, y2] or returns -1 if there is no path.
	 * This method will consider boxes as obstacles.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return
	 */
 	public int distance2(int x1, int y1, int x2, int y2) {
 		init(x1, y1);
 		
 		destination = new int[] { x2, y2 };
 		
 		return performSearch(null, null, null, 4);
 	}
	
	public int closestBoxFromGoal(int startRow, int startCol, char goalChar){
		init(startRow, startCol);
		
		this.boxChar = java.lang.Character.toUpperCase(goalChar);
		
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
			case 3:
			case 4:
				// Mode for generalized problem of searching path between two locations
				if (destination[0] == row && destination[1] == col) {
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
	
	/**
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
					break;
				case 3:
					queue.add(createPosDistArray(row, col, dist));
					break;
				case 4:
					if(copyOfLevelToSearch[row][col] == '?'){
						queue.add(createPosDistArray(row, col, dist));
					}
			}
			copyOfLevelToSearch[row][col] = '!';	// This marks that the cell has already been considered
			
//			for(int r = 0; r < rows; r++) {
//				for(int c = 0; c < cols; c++) {
//					if(walls[r][c]){
//						System.err.print("+");
//					}
//					else {
//						System.err.print(copyOfLevelToSearch[r][c]);
//					}
//				}
//				System.err.println();
//			}
//			System.err.println();
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

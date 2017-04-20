package searchclient;

import java.util.ArrayDeque;

// Used to calculate the closest distance to a box from a goal.
public class DistanceBFS {
	boolean[][] walls;
	char[][] levelToSearch;
	char[][] copyOfLevelToSearch;
	
	int rows;
	int cols;
	
	char boxChar;
	
	ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
	
	public DistanceBFS(boolean[][] walls, char[][] boxes, int rows, int cols){
		this.walls = walls;
		this.rows = rows;
		this.cols = cols;
		
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
		
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < cols; col++){
				copyOfLevelToSearch[row][col] = levelToSearch[row][col];
			}
		}
		
		this.boxChar =  java.lang.Character.toUpperCase(goalChar);
		
		copyOfLevelToSearch[startRow][startCol] = '!';	// Mark current cell as searched
		
		queue.add(createPosDistArray(startRow, startCol, 0));
		
		while(!queue.isEmpty()) {
			int result = explore();
			if(result >= 0){
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
		
		copyOfLevelToSearch[row][col] = '!';
		
		if(!walls[row-1][col] && copyOfLevelToSearch[row-1][col] == '?') {	// If the neighbouring cell is not a wall
			queue.add(createPosDistArray(row-1,col, dist+1));
		}
		if(!walls[row+1][col] && copyOfLevelToSearch[row+1][col] == '?') {
			queue.add(createPosDistArray(row+1,col, dist+1));
		}
		if(!walls[row][col-1] && copyOfLevelToSearch[row][col-1] == '?') {
			queue.add(createPosDistArray(row,col-1, dist+1));
		}
		if(!walls[row][col+1] && copyOfLevelToSearch[row][col+1] == '?') {
			queue.add(createPosDistArray(row,col+1, dist+1));
		}
		
		return -1;
	}
}

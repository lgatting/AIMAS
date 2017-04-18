package searchclient;

import java.util.ArrayDeque;

public class RoomDetector {
	boolean[][] walls;
	int[][] roomNoAssignments;
	int curRoom = 1;
	ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
	
	
	
	
	public int[][] detectRooms(boolean[][] walls, int rows, int cols, int startRow, int startCol){
		roomNoAssignments = new int[rows][cols];
		
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				roomNoAssignments[i][j] = 0;
			}
		}
		
		this.walls = walls;
		queue.add(createPosArray(startRow, startCol));
		
		boolean complete = false;
		
		while(!queue.isEmpty()) {
			addJobsToQueue();
		}
		
		return roomNoAssignments;
		
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
		roomNoAssignments[row][col] = curRoom;
		
		if(!walls[row-1][col] && roomNoAssignments[row-1][col] == 0) {	// If the neighbouring cell is not a wall
			queue.add(createPosArray(row-1,col));
		}
		if(!walls[row+1][col] && roomNoAssignments[row+1][col] == 0) {
			queue.add(createPosArray(row+1,col));
		}
		if(!walls[row][col-1] && roomNoAssignments[row][col-1] == 0) {
			queue.add(createPosArray(row,col-1));
		}
		if(!walls[row][col+1] && roomNoAssignments[row][col+1] == 0) {
			queue.add(createPosArray(row,col+1));
		}
	}
}

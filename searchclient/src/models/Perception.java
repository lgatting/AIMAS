package models;

/**
 * @author KaZ
 * This class is used to keep track of positions of the boxes and agents.
 */
public class Perception {
	int rows;
	int cols;
	int agentCount;
	public char[][] boxes;
	public int[][] boxIds;
	public int[][] agents;
	
	public Perception(int rows, int cols, int agentCount, char[][] boxes, int[][] boxIds, int[][] agents) {
		this.rows = rows;
		this.cols = cols;
		this.agentCount = agentCount;
		this.boxes = new char[rows][cols];
		this.boxIds = new int[rows][cols];
		this.agents = new int[agentCount][2];
		
		for (int row = 0; row < rows; row++) {
			System.arraycopy(boxes[row], 0, this.boxes[row], 0, cols);
			System.arraycopy(boxIds[row], 0, this.boxIds[row], 0, cols);
		}
		for (int agent = 0; agent < agentCount; agent++) {
			System.arraycopy(agents[agent], 0, this.agents[agent], 0, 2);
		}
	}
	
	public void update(String action, int agentNo) {
		if(action == "NoOp") {
			return;
		}
		
		String actionType = action.substring(0,4);
		String agentDir = action.substring(5,6);
		
		switch(actionType) {
			case "Move":
				updateAgentPos(agentDir, agentNo);
				break;
			case "Push":
				String boxDir = action.substring(7,8);
				updateBoxPosPush(agentDir, boxDir, agentNo);
				updateAgentPos(agentDir, agentNo);
				break;
			case "Pull":
				String curDirBox = action.substring(7,8);
				updateBoxPosPush(curDirBox, reverse(curDirBox), agentNo);
				updateAgentPos(agentDir, agentNo);
				break;
		}
	}
	
	private void updateAgentPos(String dir, int agentNo) {
		switch(dir) {
			case "N":
				this.agents[agentNo][0] -= 1;
				break;
			case "S":
				this.agents[agentNo][0] += 1;
				break;
			case "E":
				this.agents[agentNo][1] += 1;
				break;
			case "W":
				this.agents[agentNo][1] -= 1;
				break;
		}
	}
	
	private void updateBoxPosPush(String agentDir, String boxDir, int agentNo) {
		int agentRow = this.agents[agentNo][0];
		int agentCol = this.agents[agentNo][1];
		int[] boxPos = calcBoxPos(agentRow, agentCol, agentDir);
		char boxChar;
		int boxId;
		switch(boxDir) {
			case "N":
				boxChar = this.boxes[boxPos[0]][boxPos[1]];
				this.boxes[boxPos[0]][boxPos[1]] = 0;
				this.boxes[boxPos[0]-1][boxPos[1]] = boxChar;
				
				boxId = this.boxIds[boxPos[0]][boxPos[1]];
				this.boxIds[boxPos[0]][boxPos[1]] = 0;
				this.boxIds[boxPos[0]-1][boxPos[1]] = boxId;
				return;
			case "S":
				boxChar = this.boxes[boxPos[0]][boxPos[1]];
				this.boxes[boxPos[0]][boxPos[1]] = 0;
				this.boxes[boxPos[0]+1][boxPos[1]] = boxChar;
				
				boxId = this.boxIds[boxPos[0]][boxPos[1]];
				this.boxIds[boxPos[0]][boxPos[1]] = 0;
				this.boxIds[boxPos[0]+1][boxPos[1]] = boxId;
				return;
			case "E":
				boxChar = this.boxes[boxPos[0]][boxPos[1]];
				this.boxes[boxPos[0]][boxPos[1]] = 0;
				this.boxes[boxPos[0]][boxPos[1]+1] = boxChar;
				
				boxId = this.boxIds[boxPos[0]][boxPos[1]];
				this.boxIds[boxPos[0]][boxPos[1]] = 0;
				this.boxIds[boxPos[0]][boxPos[1]+1] = boxId;
				return;
			case "W":
				boxChar = this.boxes[boxPos[0]][boxPos[1]];
				this.boxes[boxPos[0]][boxPos[1]] = 0;
				this.boxes[boxPos[0]][boxPos[1]-1] = boxChar;
				
				boxId = this.boxIds[boxPos[0]][boxPos[1]];
				this.boxIds[boxPos[0]][boxPos[1]] = 0;
				this.boxIds[boxPos[0]][boxPos[1]-1] = boxId;
				return;
		}
		System.err.println("Invalid boxDir in updateBoxPosPush");
	}
	
	private int[] calcBoxPos(int row, int col, String dir) {
		int[] newPos = {row, col};
		switch(dir) {
			case "N":
				newPos[0] -= 1;
				return newPos;
			case "S":
				newPos[0] += 1;
				return newPos;
			case "E":
				newPos[1] += 1;
				return newPos;
			case "W":
				newPos[1] -= 1;
				return newPos;
		}
		System.err.println("Invalid direction provided for calcNewPos");
		return null;
	}
	
	private String reverse(String dir) {
		switch(dir) {
			case "N":
				return "S";
			case "S":
				return "N";
			case "E":
				return "W";
			case "W":
				return "E";
		}
		return "Invalid direction";
	}
	
}

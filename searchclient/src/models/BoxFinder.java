package models;

import java.util.LinkedList;

public class BoxFinder {
	int agentrow;
	int agentcol;
	
	public BoxFinder(int agentrow, int agentcol) {
	
		this.agentrow = agentrow;
		this.agentcol = agentcol;
	}
	
	public int[] GetBoxPos(String action) {
		int[] coordinates = new int[2];
		
		if(action == "NoOp") {
			coordinates[0] = -1 ;
			coordinates[1] = -1 ;
		}
		else {
			String actionType = action.substring(0,4);
			String agentDir = action.substring(5,6);
			
			switch(actionType) {
				case "Move": {
					coordinates[0] = updateAgentPos(agentDir)[0];
					coordinates[1] = updateAgentPos(agentDir)[1];
					break; }
				case "Push":
					String boxDir = action.substring(7,8);
					GetBoxPosPush(agentDir, boxDir);
					
					break;
				case "Pull":
					String curDirBox = action.substring(7,8);
					GetBoxPosPush(curDirBox, reverse(curDirBox));
					
					break;
			}
		}
		return coordinates ;
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
	
	
	private int[] GetBoxPosPush(String agentDir, String boxDir) {
		int[] coordinates = new int[2];
		int[] boxPos = calcBoxPos(agentrow, agentcol, agentDir);
		char boxChar;
		int boxId;
		switch(boxDir) {
			case "N":
				
				coordinates[0] = boxPos[0]-1;
				coordinates[1] = boxPos[1];
				
				return coordinates;
				
			case "S":
				
				coordinates[0] = boxPos[0]+1;
				coordinates[1] = boxPos[1];
				
				return coordinates;
			case "E":
				
				
				coordinates[0] = boxPos[0];
				coordinates[1] = boxPos[1]+1;
				
				return coordinates;
			case "W":
				
				coordinates[0] = boxPos[0];
				coordinates[1] = boxPos[1]-1;
				
				return coordinates;
		}
		System.err.println("Invalid boxDir in updateBoxPosPush");
		return coordinates;
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
	
	
	private int[] updateAgentPos(String dir) {
		int[] coordinates = new int[2];
		switch(dir) {
			case "N": {
				coordinates[0] = agentrow-1;
				coordinates[1] = agentcol;
				break;
			}
			case "S":
			{
				coordinates[0] = agentrow+1;
				coordinates[1] = agentcol;
				break;
			}
			case "E":
			{
				coordinates[0] = agentrow;
				coordinates[1] = agentcol+1;
				break;
			}
			case "W":
			{
				coordinates[0] = agentrow;
				coordinates[1] = agentcol-1;
				break;
			}
		}
		return coordinates ;
	}
	
}

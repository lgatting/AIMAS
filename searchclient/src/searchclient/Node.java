package searchclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import searchclient.Command.Type;
import searchclient.ElementWithColor.Color;

public class Node {
	private static final Random RND = new Random(1);

	public int rows;
	public int cols;

	
	// Arrays are indexed from the top-left of the level, with first index being row and second being column.
	// Row 0: (0,0) (0,1) (0,2) (0,3) ...
	// Row 1: (1,0) (1,1) (1,2) (1,3) ...
	// Row 2: (2,0) (2,1) (2,2) (2,3) ...
	// ...
	// (Start in the top left corner, first go down, then go right)
	// E.g. this.walls[2] is an array of booleans.
	// this.walls[row][col] is true if there's a wall at (row, col)
	//

	public boolean[][] walls;
	public char[][] boxes;
	public char[][] goals;
	public int[][] agents ;
	
	public HashMap<Character, Color> colorAssignments;

	public Node parent;
	public int agentNo;
	public Command action;
	
	public int agentCount;

	private int g;
	
	private int _hash = 0;
	
	public void setcolormap(HashMap<Character, Color> map){
		
	this.colorAssignments = map ;
	}
	

	public Node(Node parent, int rows, int cols, int agentCount) {
		this.parent = parent;
		this.colorAssignments = new HashMap<Character, Color>();
		
		this.walls = new boolean[rows][cols];
		this.boxes = new char[rows][cols];
		this.goals = new char[rows][cols];
		this.agents = new int[agentCount][2];
		this.agentCount = agentCount;
		
		this.rows = rows;
		this.cols = cols;
		
		if (parent == null) {
			this.g = 0;
		} else {
			this.g = parent.g() + 1;
		}
	}

	public int g() {
		return this.g;
	}

	public boolean isInitialState() {
		return this.parent == null;
	}

	public boolean isGoalState(int agentNo) {
		int goalCount = 0;
		int satisfiedGoals = 0;
		for (int row = 1; row < this.rows - 1; row++) {
			for (int col = 1; col < this.cols - 1; col++) {
				char g = goals[row][col];
				char b = boxes[row][col];
				if(g > 0 && sameColorAsAgent(agentNo, Character.toUpperCase(g))) {
					goalCount++;
				}
				if (g > 0 && Character.toLowerCase(b) == g && sameColorAsAgent(agentNo, b)) {
					satisfiedGoals++;
				}
			}
		}
		return goalCount == satisfiedGoals;
	}
	

	public boolean sameColorAsAgent(int agent, char box) {
		Color agentColor = colorAssignments.get((char) (agent  + '0'));
		Color boxColor = colorAssignments.get(box);
		return agentColor == boxColor;
	}

	public ArrayList<Node> getExpandedNodes(int agentNo) {
		ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);
		
		assignCommands(expandedNodes, agentNo);
		
		////System.err.println(expandedNodes.get(0).agents[0][0] + "," + expandedNodes.get(0).agents[0][1]);
		//Collections.shuffle(expandedNodes, RND);
		return expandedNodes;
	}
	
	public void assignCommands(ArrayList<Node> expandedNodes, int agentNo){
		
		// PROBLEM in else: Changes are immediately applied for each agent. The actions must only be applied
		// after they have all been observed and each of their moves have been deemed applicable
		
		/*if(agentNo == this.agentCount){
<<<<<<< HEAD
			//System.err.println(this.actions[0].toString() + "," + this.actions[1].toString());
=======
			////System.err.println(this.actions[0].toString() + "," + this.actions[1].toString());
>>>>>>> 7bad6d165b4efa01ecca3b59a3b5f8a3d642c879
			
			expandedNodes.add(this.ChildNode());	// THE CURRENT PROBLEM IS HERE!!!!! Boxes arrays are not updated before the node is pushed onto the arraylist!
		}
		else{*/
			for (Command c : Command.EVERY) {
				//System.err.println("Prev: " + this.action);
				
				// Determine applicability of action
				int newAgentRow = this.agents[agentNo][0] + Command.dirToRowChange(c.dir1);
				int newAgentCol = this.agents[agentNo][1] + Command.dirToColChange(c.dir1);
				
				Node n = this.ChildNode();
				
				
				
				if (c.actionType == Type.Move) {
					// Check if there's a wall or box on the cell to which the agent is moving
					if (this.cellIsFree(agentNo, newAgentRow, newAgentCol)) {
						//Node n = parent.ChildNode();
						n.action = c;
						//n.assignCommands(expandedNodes, agentNo+1);
						
						// The action is only applied if the other agents' moves are deemed applicable. THIS NEEDS TO BE FIXED as other agents may also not move
						
						n.agents[agentNo][0] = newAgentRow;
						n.agents[agentNo][1] = newAgentCol;
						
						expandedNodes.add(n);
					}
				} else if (c.actionType == Type.Push) {
					
					// Make sure that there's actually a box to move
					if (this.boxAt(newAgentRow, newAgentCol) && sameColorAsAgent(agentNo, this.boxes[newAgentRow][newAgentCol])) {

						////System.err.println("Trying PUSH");
						//System.err.println("Box: " + this.boxes[newAgentRow][newAgentCol] + " " + colorAssignments.get((char) (agentNo + '0')) + " " + colorAssignments.get(this.boxes[newAgentRow][newAgentCol]));
						int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
						int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
						// .. and that new cell of box is free
						if (this.cellIsFree(agentNo, newBoxRow, newBoxCol)) {
							//Node n = parent.ChildNode();
					
							n.action = c;
							//n.assignCommands(expandedNodes, agentNo+1);
							n.agents[agentNo][0] = newAgentRow;
							n.agents[agentNo][1] = newAgentCol;
							n.boxes[newBoxRow][newBoxCol] = this.boxes[newAgentRow][newAgentCol];
							n.boxes[newAgentRow][newAgentCol] = 0;
							
							expandedNodes.add(n);
						}
					}
				} else if (c.actionType == Type.Pull) {
					// Cell is free where agent is going
					if (this.cellIsFree(agentNo, newAgentRow, newAgentCol)) {
						int boxRow = this.agents[agentNo][0] + Command.dirToRowChange(c.dir2);
						int boxCol = this.agents[agentNo][1] + Command.dirToColChange(c.dir2);
						// .. and there's a box in "dir2" of the agent
						if (this.boxAt(boxRow, boxCol) && sameColorAsAgent(agentNo, this.boxes[newAgentRow][newAgentCol])) {
							//Node n = parent.ChildNode();
							n.action = c;
							//n.assignCommands(expandedNodes, agentNo+1);
							n.agents[agentNo][0] = newAgentRow;
							n.agents[agentNo][1] = newAgentCol;
							n.boxes[this.agents[agentNo][0]][this.agents[agentNo][1]] = this.boxes[boxRow][boxCol];
							n.boxes[boxRow][boxCol] = 0;
							
							expandedNodes.add(n);
						}
					}
				}
				//n.assignCommands(n, expandedNodes, agentNo+1);
			}
		//}
	}

	private boolean cellIsFree(int agentNo, int row, int col) {
		return !this.walls[row][col] && this.boxes[row][col] == 0 && !agentAt(agentNo,row,col) ;
	}
	
	// agentNo is the agent that would like to check whether there is another agent blocking
	private boolean agentAt(int agentNo, int row, int col) {
		for(int agent = 0; agent < this.agentCount; agent++){
			if(this.agents[agent][0] == row && this.agents[agent][1] == col && agent != agentNo) {
				return true;
			}
		}
		return false;
	}

	private boolean boxAt(int row, int col) {
		return this.boxes[row][col] > 0;
	}

	private Node ChildNode() {
		Node copy = new Node(this, this.rows, this.cols, this.agentCount);
		
		copy.colorAssignments = this.colorAssignments;
		copy.walls = this.walls;
		copy.goals = this.goals;
		copy.action = this.action;
		for (int row = 0; row < this.rows; row++) {
			System.arraycopy(this.boxes[row], 0, copy.boxes[row], 0, this.cols);
		}
		for (int agent = 0; agent < this.agentCount; agent++) {
			System.arraycopy(this.agents[agent], 0, copy.agents[agent], 0, 2);
		}
		
		return copy;
	}

	public LinkedList<Node> extractPlan() {
		LinkedList<Node> plan = new LinkedList<Node>();
		Node n = this;
		while (!n.isInitialState()) {
			plan.addFirst(n);
			n = n.parent;
		}
		return plan;
	}

	@Override
	public int hashCode() {
		if (this._hash == 0) {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.deepHashCode(this.agents);
			result = prime * result + Arrays.deepHashCode(this.boxes);
			result = prime * result + Arrays.deepHashCode(this.goals);
			result = prime * result + Arrays.deepHashCode(this.walls);
			this._hash = result;
		}
		return this._hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (this.action.equals(other.action))
			return false;
		if (!Arrays.deepEquals(this.agents, other.agents))
			return false;
		if (!Arrays.deepEquals(this.boxes, other.boxes))
			return false;
		if (!Arrays.deepEquals(this.goals, other.goals))
			return false;
		if (!Arrays.deepEquals(this.walls, other.walls))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int row = 0; row < this.rows; row++) {
			if (!this.walls[row][0]) {
				break;
			}
			colLoop:
			for (int col = 0; col < this.cols; col++) {
				if (this.boxes[row][col] > 0) {
					s.append(this.boxes[row][col]);
				} else if (this.goals[row][col] > 0) {
					s.append(this.goals[row][col]);
				} else if (this.walls[row][col]) {
					s.append("+");
				} else {
					for(int agentNo = 0; agentNo < this.agentCount; agentNo++){
						if(this.agents[agentNo][0] == row && this.agents[agentNo][1] == col){
							s.append(agentNo);
							continue colLoop;
						}
					}
					s.append(" ");
				}
			}
			s.append("\n");
		}
		
	
		
		return s.toString();
	}

}
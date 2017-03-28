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
	private HashMap<Character, Color> colorAssignments;

	public Node parent;
	public Command action;

	private int g;
	
	private int _hash = 0;
	
	public void setcolormap(HashMap<Character, Color> map){
		
	this.colorAssignments = map ;
	}
	

	public Node(Node parent, int rows, int cols) {
		this.parent = parent;
		this.colorAssignments = new HashMap<Character, Color>();
		
		this.walls = new boolean[rows][cols];
		this.boxes = new char[rows][cols];
		this.goals = new char[rows][cols];
		this.agents = new int[rows][cols];
		
		for(int row = 0; row < rows; row++){
			for(int col = 0; col < cols; col++){
				this.agents[row][col] = -1;
			}
		}
		
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

	public boolean isGoalState() {
		for (int row = 1; row < this.rows - 1; row++) {
			for (int col = 1; col < this.cols - 1; col++) {
				char g = goals[row][col];
				char b = Character.toLowerCase(boxes[row][col]);
				if (g > 0 && b != g) {
					return false;
				}
			}
		}
		return true;
	}

	public ArrayList<Node> getExpandedNodes() {
		ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);
		
		for(int agentRow=0; agentRow < this.rows ; agentRow++){
			for(int agentCol=0; agentCol< this.cols ; agentCol++){
				if(agents[agentRow][agentCol] >= 0){
					int agentNo = agents[agentRow][agentCol];
					for (Command c : Command.EVERY) {
						// Determine applicability of action
						int newAgentRow = agentRow + Command.dirToRowChange(c.dir1);
						int newAgentCol = agentCol + Command.dirToColChange(c.dir1);
			
						if (c.actionType == Type.Move) {
							// Check if there's a wall or box on the cell to which the agent is moving
							if (this.cellIsFree(newAgentRow, newAgentCol)) {
								Node n = this.ChildNode();
								n.action = c;
								n.agents[newAgentRow][newAgentCol] = agentNo;
								n.agents[agentRow][agentCol] = -1;
								expandedNodes.add(n);
							}
						} else if (c.actionType == Type.Push) {
							// Make sure that there's actually a box to move
							if (this.boxAt(newAgentRow, newAgentCol)) {
								int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
								int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
								// .. and that new cell of box is free
								if (this.cellIsFree(newBoxRow, newBoxCol)) {
									Node n = this.ChildNode();
									n.action = c;
									n.action = c;
									n.agents[newAgentRow][newAgentCol] = agentNo;
									n.agents[agentRow][agentCol] = -1;
									n.boxes[newBoxRow][newBoxCol] = this.boxes[newAgentRow][newAgentCol];
									n.boxes[newAgentRow][newAgentCol] = 0;
									expandedNodes.add(n);
									try {
										String txt = "";
										for(int agentRow1=0; agentRow1 < this.rows ; agentRow1++){
											for(int agentCol1=0; agentCol1< this.cols ; agentCol1++){
												txt += agents[agentRow1][agentCol1] ;
											}
											txt += '\n';
											}
							          
							Files.write(Paths.get("log2.txt"), txt.getBytes(), StandardOpenOption.APPEND);
							}catch (IOException e) {
							//exception handling left as an exercise for the reader
							}
								}
							}
						} else if (c.actionType == Type.Pull) {
							// Cell is free where agent is going
							if (this.cellIsFree(newAgentRow, newAgentCol)) {
								int boxRow = agentRow + Command.dirToRowChange(c.dir2);
								int boxCol = agentCol + Command.dirToColChange(c.dir2);
								// .. and there's a box in "dir2" of the agent
								if (this.boxAt(boxRow, boxCol)) {
									Node n = this.ChildNode();
									n.action = c;
									n.action = c;
									n.agents[newAgentRow][newAgentCol] = agentNo;
									n.agents[agentRow][agentCol] = -1;
									n.boxes[agentRow][agentCol] = this.boxes[boxRow][boxCol];
									n.boxes[boxRow][boxCol] = 0;
									expandedNodes.add(n);
								}
							}
						}
					}
				}
			}
		}
		Collections.shuffle(expandedNodes, RND);
		return expandedNodes;
	}

	private boolean cellIsFree(int row, int col) {
		return !this.walls[row][col] && this.boxes[row][col] == 0;
	}

	private boolean boxAt(int row, int col) {
		return this.boxes[row][col] > 0;
	}

	private Node ChildNode() {
		Node copy = new Node(this, this.rows, this.cols);
		copy.agents = this.agents;
		copy.colorAssignments = this.colorAssignments;
		copy.walls = this.walls;
		copy.goals = this.goals;
		for (int row = 0; row < this.rows; row++) {
			System.arraycopy(this.boxes[row], 0, copy.boxes[row], 0, this.cols);
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
			for (int col = 0; col < this.cols; col++) {
				if (this.boxes[row][col] > 0) {
					s.append(this.boxes[row][col]);
				} else if (this.goals[row][col] > 0) {
					s.append(this.goals[row][col]);
				} else if (this.walls[row][col]) {
					s.append("+");
				} else if (this.agents[row][col] >= 0) {
					s.append(agents[row][col]);
				} else {
					s.append(" ");
				}
			}
			s.append("\n");
		}
		
	
		
		return s.toString();
	}

}
package searchclient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

import entities.*;
import searchclient.Command.Type;

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
	
	public ArrayList<Agent> agents;
	public ArrayList<Wall> walls;
	public ArrayList<Box> boxes;
	public ArrayList<Goal> goals;

	public Node parent;
	public Command action;

	private int g;
	
	private int _hash = 0;

	public Node(Node parent, int rows, int cols) {
		this.parent = parent;
		
		this.agents = new ArrayList<Agent>();
		this.walls = new ArrayList<Wall>();
		this.boxes = new ArrayList<Box>();
		this.goals = new ArrayList<Goal>();
		
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
		for(Goal g : goals){
			if(!boxes.contains(g)) {
				return false;
			}
		}
		return true;
	}

	public ArrayList<Node> getExpandedNodes() {
		ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.EVERY.length);
		for(Agent agent : agents) {
			for (Command c : Command.EVERY) {
				// Determine applicability of action
				int newAgentRow = agent.getRow() + Command.dirToRowChange(c.dir1);
				int newAgentCol = agent.getCol() + Command.dirToColChange(c.dir1);
	
				if (c.actionType == Type.Move) {
					// Check if there's a wall or box on the cell to which the agent is moving
					if (this.cellIsFree(newAgentRow, newAgentCol)) {
						Node n = this.ChildNode();
						n.action = c;
					    int agentIndex = n.agents.indexOf(agent);
					    Agent foundAgent = n.agents.get(agentIndex);	// By reference
					    foundAgent.setRow(newAgentRow);
						foundAgent.setCol(newAgentCol);
						expandedNodes.add(n);
					}
				} else if (c.actionType == Type.Push) {
					
					//IMPLEMENT COLOR CHECK
					
					// Make sure that there's actually a box to move
					if (this.boxAt(newAgentRow, newAgentCol)) {
						int newBoxRow = newAgentRow + Command.dirToRowChange(c.dir2);
						int newBoxCol = newAgentCol + Command.dirToColChange(c.dir2);
						// .. and that new cell of box is free
						if (this.cellIsFree(newBoxRow, newBoxCol)) {
							Node n = this.ChildNode();
							n.action = c;
							int agentIndex = n.agents.indexOf(agent);
							Agent foundAgent = n.agents.get(agentIndex);	// By reference
							foundAgent.setRow(newAgentRow);
							foundAgent.setCol(newAgentCol);
							
							int boxIndex = this.boxes.indexOf(new Box(newAgentRow, newAgentCol));
							Box foundBox = this.boxes.get(boxIndex);
							foundBox.setRow(newBoxRow);
							foundBox.setCol(newBoxCol);							
							//n.boxes[newBoxRow][newBoxCol] = this.boxes[newAgentRow][newAgentCol];
							//n.boxes[newAgentRow][newAgentCol] = 0;
							expandedNodes.add(n);
						}
					}
				} else if (c.actionType == Type.Pull) {
					
					//IMPLEMENT COLOR CHECK
					
					// Cell is free where agent is going
					if (this.cellIsFree(newAgentRow, newAgentCol)) {
						int boxRow = agent.getRow() + Command.dirToRowChange(c.dir2);
						int boxCol = agent.getCol() + Command.dirToColChange(c.dir2);
						// .. and there's a box in "dir2" of the agent
						if (this.boxAt(boxRow, boxCol)) {
							Node n = this.ChildNode();
							n.action = c;
							int agentIndex = n.agents.indexOf(agent);
							Agent foundAgent = n.agents.get(agentIndex);
							foundAgent.setRow(newAgentRow);
							foundAgent.setCol(newAgentCol);
							
							int boxIndex = this.boxes.indexOf(new Box(boxRow, boxCol));
							Box foundBox = this.boxes.get(boxIndex);
							foundBox.setRow(agent.getRow());
							foundBox.setCol(agent.getCol());	
							
							//n.boxes[this.agentRow][this.agentCol] = this.boxes[boxRow][boxCol];
							//n.boxes[boxRow][boxCol] = 0;
							expandedNodes.add(n);
						}
					}
				}
			}
		}
		Collections.shuffle(expandedNodes, RND);
		return expandedNodes;
	}

	private boolean cellIsFree(int row, int col) {
		return this.walls.contains(new Wall(row, col));
	}

	private boolean boxAt(int row, int col) {
		return this.boxes.contains(new Box(row, col));
	}

	private Node ChildNode() {
		Node copy = new Node(this, this.rows, this.cols);
		copy.agents = new ArrayList<>(this.agents);
		copy.walls = new ArrayList<>(this.walls);
		copy.boxes = new ArrayList<>(this.boxes);
		copy.goals = new ArrayList<>(this.goals);
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
			result = prime * result + this.agentCol;
			result = prime * result + this.agentRow;
			result = prime * result + Arrays.deepHashCode(this.boxes);
			result = prime * result + Arrays.deepHashCode(this.goals);
			result = prime * result + Arrays.deepHashCode(this.walls);
			this._hash = result;
		}
		return this._hash;
	}
	
	public boolean listsAreEqual(ArrayList<?> list1, ArrayList<?> list2){		
		for(int i = 0; i < list1.size(); i++){
			if(!list2.contains(list1.get(i))){
				return false;
			}
		}
		return true;
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
		if (this.agentRow != other.agentRow || this.agentCol != other.agentCol)
			return false;
		if (!listsAreEqual(this.boxes, other.boxes))
			return false;
		if (!listsAreEqual(this.goals, other.goals))
			return false;
		if (!listsAreEqual(this.walls, other.walls))
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
				} else if (row == this.agentRow && col == this.agentCol) {
					s.append("0");
				} else {
					s.append(" ");
				}
			}
			s.append("\n");
		}
		return s.toString();
	}

}
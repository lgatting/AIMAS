package models;

/**
 * Represents an action of agent moving to a box.
 */
public class GoToHLA extends HighLevelAction {
	/**
	 * The intention behind this command is that the agent should only move and therefore he'll get a penalty in
	 * heuristic if he tries to move a box.
	 */
	public static final int PUSH_PULL_PENALTY = 10;
	
	/**
	 * Box to reach.
	 */
	public Box box;
	
	public GoToHLA(Box box) {
		this.box = box;
	}
	
	@Override
	public String toString() {
		return "GoTo " + Character.toUpperCase(box.letter) + " (" + box.id + ")";
	}
}

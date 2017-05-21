package models;

/**
 * Represents an action of temporarily giving way to another agent.
 */
public class GiveWayHLA extends HighLevelAction {
	/**
	 * The intention behind this command is that the agent should only move and therefore he'll get a penalty in
	 * heuristic if he tries to move a box.
	 */
	public static final int PUSH_PULL_PENALTY = 10;
	
	/**
	 * Free cell to reach.
	 */
	public int[] cell;
	
	public GiveWayHLA(int row, int col) {
		this.cell = new int[]{row, col};
		
	}
	
	@Override
	public String toString() {
		return cell.length+"GiveWayHLA ("+ cell[0] + "," + cell[1] + ")";
	}
}

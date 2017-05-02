package models;

/**
 * Represents an action of moving a box to a goal and this goal should not be unsatisfied again.
 */
public class SatisfyGoalHLA extends HighLevelAction {
	/**
	 * Box to move.
	 */
	public Box box;
	
	/**
	 * Goal to satisfy.
	 */
	public Goal goal;
	
	public SatisfyGoalHLA(Box box, Goal goal) {
		this.box = box;
		this.goal = goal;
	}
	
	@Override
	public String toString() {
		return "SatisfyGoal " + box.letter + " (" + box.id + ") with " + Character.toUpperCase(box.letter) + " (" + box.id + ")";
	}
}

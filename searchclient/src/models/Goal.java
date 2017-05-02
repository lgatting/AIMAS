package models;

public class Goal {
	/**
	 * ID of this goal.
	 */
	public int id;
	
	public char letter;
	
	public Goal(int id, char letter) {
		this.letter = letter;
		this.id = id;
	}
}

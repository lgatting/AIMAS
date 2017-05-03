package models;

public class Goal implements Comparable<Goal> {
	/**
	 * ID of this goal.
	 */
	public int id;
	
	public char letter;
	
	public int numberOfDependencies;
	
	public Goal(int id, char letter) {
		this.id = id;
		this.letter = letter;
		this.numberOfDependencies = 0;
	}
	
	@Override
	public int compareTo(Goal g) {
		return g.numberOfDependencies - numberOfDependencies;
	}
	
	@Override
	public String toString() {
		return "Goal " + letter + " (" + id + "), " + numberOfDependencies; 
	}
}

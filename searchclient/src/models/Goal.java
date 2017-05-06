package models;

public class Goal implements Comparable<Goal> {
	/**
	 * ID of this goal.
	 */
	public int id;
	
	public char letter;
	
	public int numberOfDependencies;
	
	/**
	 * Lewis score of the goal; based on the types of neighbors, a wall neighbors increases
	 * value by 2, goal neighbor by 1.
	 */
	public int lewisScore;
	
	/**
	 * Based on wall count surrounding the goal. Wall count, from best to worst: 3, 0, 1, 2. 
	 */
	public int positionPenalty;
	
	public Goal(int id, char letter) {
		this.id = id;
		this.letter = letter;
		this.numberOfDependencies = 0;
		this.lewisScore = 0;
		this.positionPenalty = 0;
	}
	
	@Override
	public int compareTo(Goal g) {
		int corridorScore = g.numberOfDependencies - numberOfDependencies;
		
		if (corridorScore != 0)
			return corridorScore;
		else
			return (g.lewisScore - lewisScore) + (positionPenalty - g.positionPenalty);
	}
	
	@Override
	public String toString() {
		return "Goal " + letter + " (" + id + "); Dep's: " + numberOfDependencies + "; Lewis: " + lewisScore + ", PS: " + positionPenalty; 
	}
}

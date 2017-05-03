package models;

import searchclient.ElementWithColor.Color;

public class Box implements Comparable<Box>  {
	/**
	 * ID of this goal.
	 */
	public int id;
	
	/**
	 * Goal for which this box is designated.
	 */
	public Goal goal;
	
	/**
	 * Although in general boxes are denoted with a capital letter, here is saved a lowercase version of
	 * it for the sake of easier comparison with goals.
	 */
	public char letter;
	
	public Color color;
	
	public Box(int id, char letter, Color color) {
		this.id = id;
		this.letter = Character.toLowerCase(letter);
		this.color = color;
		this.goal = null;
	}
	
	@Override
	public int compareTo(Box b) {
		if (b.goal == null && goal == null)
			return 0;
		else if (b.goal == null && goal != null)
			return 1;
		else if (b.goal != null && goal == null)
			return -1;
		else
			return b.goal.compareTo(goal);
	}
	
	@Override
	public String toString() {
		return "Box " + Character.toUpperCase(letter) + " (" + id + ")";
	}
}

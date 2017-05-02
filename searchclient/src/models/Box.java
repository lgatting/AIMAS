package models;

import searchclient.ElementWithColor.Color;

public class Box {
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
}

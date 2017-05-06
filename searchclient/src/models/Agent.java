package models;

import java.util.HashSet;
import java.util.Set;

import searchclient.ElementWithColor.Color;

public class Agent {
	/**
	 * ID of this agent; typically a number between 0-9.
	 */
	public int id;
	
	/**
	 * Set of boxes for which this agent is responsible.
	 */
	public Set<Box> boxes;
	
	public Color color;
	
	public Agent(int id, Color color) {
		this.id = id;
		this.color = color;
		
		this.boxes = new HashSet<Box>();
	}
}

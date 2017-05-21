package models;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import searchclient.ElementWithColor.Color;

public class Agent {
	/**
	 * ID of this agent; typically a number between 0-9.
	 */
	public int id;
	public enum state {iddle,busy,busywaiting,assisting};
	
	public LinkedList<Agent> AssitQueue ;
	
	/**
	 * Set of boxes for which this agent is responsible.
	 */
	public Set<Box> boxes;
	
	public Color color;
	
	public Agent(int id, Color color) {
		this.id = id;
		this.color = color;
		this.AssitQueue = new LinkedList();
		
		this.boxes = new HashSet<Box>();
		
	}
	
	
	
	
}

package entities;

import entities.Box.Color;

public class Agent extends Element {
	public static enum Color {
		Blue, Red, Green, Cyan, Magenta, Orange, Pink, Yellow
	};
	
	private int number;
	private Color color;
	
	public Agent(int row, int col) {
		super(row, col);
	}
	
	public Agent(Agent toCopy){
		super(toCopy.getRow(),toCopy.getCol());
		this.number = toCopy.getNumber();
		this.color = toCopy.getColor();
	}
	
	public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public Color getColor() {
		return color;
	}

	public void setColor(Color color) {
		this.color = color;
	}
}

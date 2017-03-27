package entities;

public class Box extends ElementWithColor {	
	private char letter;
	private Color color;
	private boolean locked;

	public Box(int row, int col, char letter, Color color) {
		super(row,col);
		this.letter = letter;
		this.color = color;
	}

	public Box(Box toCopy) {
		super(toCopy.getRow(),toCopy.getCol());
		this.letter = toCopy.getLetter();
		this.color = toCopy.getColor();
		this.locked = toCopy.isLocked();
	}

	public char getLetter() {
		return letter;
	}
	
	public Color getColor() {
		return color;
	}
	
	public boolean isLocked() {
		return locked;
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}

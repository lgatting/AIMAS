package entities;

public class Box extends Element {	
	public static enum Color {
		Blue, Red, Green, Cyan, Magenta, Orange, Pink, Yellow
	};
	
	private char letter;
	private Color color;
	private boolean locked;

	public Box(int row, int col) {
		super(row,col);
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
	
	public void setLetter(char letter) {
		this.letter = letter;
	}
	
	public Color getColor() {
		return color;
	}
	
	public void setColor(Color color) {
		this.color = color;
	}
	
	public boolean isLocked() {
		return locked;
	}
	
	public void setLocked(boolean locked) {
		this.locked = locked;
	}
}

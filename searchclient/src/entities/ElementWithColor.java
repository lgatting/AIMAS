package entities;

public abstract class ElementWithColor extends Element {
	public static enum Color {
		blue, red, green, cyan, magenta, orange, pink, yellow
	};
	
	public ElementWithColor(int row, int col) {
		super(row, col);
	}
}

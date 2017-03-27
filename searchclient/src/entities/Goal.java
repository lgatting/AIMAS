package entities;

public class Goal extends Element {
	private char letter;
	private boolean satisfied;
	
	public Goal(int row, int col) {
		super(row,col);
	}

	public char getLetter() {
		return letter;
	}

	public void setLetter(char letter) {
		this.letter = letter;
	}
	
	public boolean isSatisfied() {
		return satisfied;
	}
	
	public void setSatisfied(boolean satisfied) {
		this.satisfied = satisfied;
	}
}

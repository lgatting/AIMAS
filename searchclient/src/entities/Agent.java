package entities;

public class Agent extends ElementWithColor {
	private int agentNo;
	private Color color;
	
	public Agent(int row, int col, int agentNo, Color color) {
		super(row, col);
		this.agentNo = agentNo;
		this.color = color;
	}
	
	public Agent(Agent toCopy){
		super(toCopy.getRow(),toCopy.getCol());
		this.agentNo = toCopy.getNumber();
		this.color = toCopy.getColor();
	}
	
	public int getNumber() {
		return agentNo;
	}

	public Color getColor() {
		return color;
	}

}

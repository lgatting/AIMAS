package entities;

public class Element {
	private int row ;
	private int col ;
	
	public Element(int row, int col) {
		this.row = row;
		this.col = col;
	}
	
	public int getRow() {
		return row;
	}
	public void setRow(int row) {
		this.row = row;
	}
	public int getCol() {
		return col;
	}
	public void setCol(int col) {
		this.col = col;
	}
	
	@Override
	public boolean equals(Object other){
		Element e = (Element) other;
		if(this.row == e.getRow() && this.col == e.getCol()){
			return true;
		}
		return false;
	}
}

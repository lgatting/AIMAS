package models;

/**
 * Represents an action of moving a box to a temporary location.
 */
public class StoreTempHLA extends HighLevelAction {
	/**
	 * Box to move.
	 */
	public Box box;
	
	/**
	 * Cell to temporarily store the box.
	 */
	public int[] cell;
	
	public StoreTempHLA(Box box, int row, int col) {
		this.box = box;
		this.cell = new int[]{row, col};
	}
	
	@Override
	public String toString() {
		return "StoreTemp box " + Character.toUpperCase(box.letter) + " (" + box.id + ") at (" + cell[0] + "," + cell[1] + ")";
	}
}
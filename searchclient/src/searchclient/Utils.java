package searchclient;

import java.util.Set;

import models.Goal;

public class Utils {
	/**
	 * Whether the first position (x1, y1) is a direct neighbor to x2, y2.
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @return True if it is, false otherwise.
	 */
	public static boolean isNeighboringPosition(int x1, int y1, int x2, int y2) {
		return Math.abs(x1 - x2) + Math.abs(y1 - y2) == 1;
	}
	
	/**
	 * Returns number of walls that are "touching" the cell. Can be a number between 0-4, both included. It is assummed that
	 * x and y never point to a wall cell and therefore that cell is never on the border of a level.
	 * @param x
	 * @param y
	 * @param walls
	 * @return Number of walls surrounding this cell.
	 */
	public static int neighbouringWallsCount(int x, int y, boolean[][] walls) {
		int count = 0;
		
		if (walls[x + 1][y])
			count++;
		if (walls[x][y + 1])
			count++;
		if (walls[x - 1][y])
			count++;
		if (walls[x][y - 1])
			count++;
			
		return count;
	}
	
	/**
	 * Finds the position of the goal in the given map.
	 * @param goal
	 * @param goalIds
	 * @return Integer array with 2 elements, first being row, second column of the goal.
	 */
	public static int[] findGoalPosition(Goal goal, int[][] goalIds) {
		int rows = goalIds.length;
		for (int i = 0; i < rows; i++) {
			int cols = goalIds[i].length;
			for (int j = 0; j < cols; j++) {
				if (goal.id == goalIds[i][j])
					return new int[] { i, j };
			}
		}
		
		return null;
	}
	
	/**
	 * Finds a goal object at given position.
	 * @param goals
	 * @param goalIds
	 * @param p Position, which will be checked.
	 * @return
	 */
	public static Goal findGoal(Set<Goal> goals, int[][] goalIds, int[] p) {
		for (Goal g : goals)
			if (g.id == goalIds[p[0]][p[1]])
				return g;
		
		return null;
	}
	
	/**
	 * Finds a goal with given ID in given set of goals.
	 * @param goals
	 * @param id
	 * @return
	 */
	public static Goal findGoal(Set<Goal> goals, int id) {
		for (Goal g : goals)
			if (g.id == id)
				return g;
		
		return null;
	}

	/**
	 * Whether a cell at given position is free (i.e. no agent, box or wall is in that place).
	 * @param n
	 * @param row
	 * @param col
	 * @return
	 */
	public static boolean cellIsFree(Node n, int row, int col) {
		return !n.walls[row][col] && n.boxes[row][col] == 0 && !n.agentAt(row,col) ;
	}
}

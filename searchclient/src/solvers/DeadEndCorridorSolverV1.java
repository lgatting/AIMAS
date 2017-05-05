package solvers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import searchclient.Command.Dir;

import models.Goal;
/*
public class DeadEndCorridorSolverV1 {
	public static List<Goal> frontier;
	public static Map<Integer, Goal> priorities;
	
	public DeadEndCorridorSolverV1(Set<Goal> goals, Node n) {
		frontier = new ArrayList<Goal>();
		priorities = new HashMap<Integer, Goal>();
		
		for (Goal goal : goals)
			frontier.add(goal);
		
		for (Goal g : frontier) {
			// Find actual position of the goal
			int goalRow = ;
			int goalCol = ;
			
			if (Utils.neighbouringWallsCount(goalRow, goalCol, n.walls) == 2) {
				Dir dir1 = ;
				Dir dir2 = ;
				int[] deadend1 = explore(g, dir1, n.walls);
				int[] deadend2 = explore(g, dir2, n.walls);
				
				if ((deadend1[0] == -1 && deadend2[0] == -1) || (deadend1[0] != -1 && deadend2[0] != -1))
					// This is not a deadend type of corridor or it is a tube (i.e. whole level is a corridor)
					continue;
				if (deadend1 != -1)
					rankGoals(deadend1, goodDir, 0);
			}
		}
	}
	
	public int[] explore(int[] i, Dir d, boolean[][] walls) {
		int[] neighbor = neighborOf(i, d);
		
		int neighborWallCount = Utils.neighbouringWallsCount(neighbor[0], neighbor[1], walls);
		
		if (neighborWallCount == 2)
			return explore(neighbor, , walls);
		if (neighborWallCount == 3)
			return neighbor;
		if (neighborWallCount <= 1)
			return new int[] { -1, -1 };
	}
	
	public int[] neighborOf(int[] i, Dir d) {
		if (d == Dir.E)
			return new int[] { i[0] + 1, i[1] };
		if (d == Dir.S)
			return new int[] { i[0], i[1] + 1 };
		if (d == Dir.W)
			return new int[] { i[0] - 1, i[1] };
		if (d == Dir.N)
			return new int[] { i[0], i[1] - 1 };
		
		return null;
	}
	
	public void rankGoals(int[] i, Dir d, int counter, boolean[][] walls) {
		int wallCount = Utils.neighbouringWallsCount(i[0], i[1], walls); 
		
		if (wallCount == 2 || wallCount == 3) {
			counter++;
			
			if (i is goal) {
				frontier.remove(goalAtI)
				priorities.put(counter, goalAtI);
			}

			int[] neighbor = neighborOf(i, d);
			rankGoals(neighbor, d, counter, walls);
		}
	}
}
*/

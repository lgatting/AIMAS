package solvers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.omg.CORBA.CODESET_INCOMPATIBLE;

import searchclient.Command;
import searchclient.Node;
import searchclient.Utils;
import searchclient.Command.Dir;

import models.Goal;

public class DeadEndCorridorSolverV2 {
	private List<Goal> frontier;
	private List<Goal> numberedGoals;
	private Map<Integer, Goal> priorities;
	private int[][] dependancyMatrix;
	private Node node;
	private Set<Goal> goals;

	public DeadEndCorridorSolverV2(Set<Goal> goals, Node n) {
		frontier = new ArrayList<Goal>();
		numberedGoals = new ArrayList<Goal>();
		priorities = new HashMap<Integer, Goal>();
		dependancyMatrix = new int[goals.size()][goals.size()];
		node = n;
		this.goals = goals;
	}
	
	public List<Goal> orderGoals() {
		solve();
		
		Collections.sort(numberedGoals);
		
		return numberedGoals;
	}
	
	/**
	 * Used for debugging.
	 */
	public void printDependancyMatrix() {
		for (int i = 0; i < dependancyMatrix.length; i++) {
			for (int j = 0; j < dependancyMatrix[i].length; j++) {
				System.err.print(dependancyMatrix[i][j]);
			}
			System.err.println();
		}
	}
	
	private void solve() {	
		int i = 0;
		int j = 0;
		
		Map<Integer, int[]> goalPositions = new HashMap<Integer, int[]>();
		
		i = 0;
		for (Goal g : goals) {
			frontier.add(g);
			numberedGoals.add(g);
			g.numberOfDependencies = 0;
			
			goalPositions.put(g.id, Utils.findGoalPosition(g, node.goalIds));
			
			for (j = 0; j < goals.size(); j++)
				dependancyMatrix[i][j] = 0;
				
			i++;
		}
		
		for (int k = 0; k < frontier.size(); ) {
			Goal g = frontier.get(k);
			
			int[] goalPos = goalPositions.get(g.id);
			
			int wallCount = Utils.neighbouringWallsCount(goalPos[0], goalPos[1], node.walls);
			
			if (wallCount == 2 || wallCount == 3) {
				int[] deadend1 = explore(goalPos, freeNeighborsDir(goalPos).get(0));
				int[] deadend2 = null;
				
				if (wallCount == 3)
					deadend2 = goalPos;
				if (wallCount == 2)
					deadend2 = explore(goalPos, freeNeighborsDir(goalPos).get(1));
				
				if ((deadend1 == null && deadend2 == null) || (deadend1 != null && deadend2 != null))
					// This goal is in a corridor that is open from both ends
					frontier.remove(g);
				if (deadend1 != null)
					findDependencies(deadend1, freeNeighborsDir(deadend1).get(0), null);
				if (deadend2 != null)
					findDependencies(deadend2, freeNeighborsDir(deadend2).get(0), null);
			} else {
				// This goal is not in a corridor
				frontier.remove(g);
			}
		}
		
		Collections.reverse(numberedGoals);
		
		for (i = 0; i < dependancyMatrix.length; i++) {
			calculateNumberOfDependencies(Utils.findGoal(goals, i + 1));
		}
	}
	
	private int calculateNumberOfDependencies(Goal g) {
		if (g == null)
			return 0;
		
		for (int i = 0; i < dependancyMatrix[g.id - 1].length; i++) {
			if (dependancyMatrix[g.id - 1][i] == 1) {
				g.numberOfDependencies++;
				
				Goal dependant = Utils.findGoal(goals, i + 1);

				g.numberOfDependencies += dependant.numberOfDependencies;
			}
		}
		
		return g.numberOfDependencies;
	}
	
	private void findDependencies(int[] i, Dir d, Goal previousGoal) {
		int wallCount = Utils.neighbouringWallsCount(i[0], i[1], node.walls);
		
		if (wallCount == 2 || wallCount == 3) {
			int[] neighbor = Utils.neighborOf(i, d);
			
			Goal goalAtI = Utils.findGoal(goals, node.goalIds, i);
			
			if (goalAtI != null) {
				frontier.remove(goalAtI);
				if (previousGoal != null) {
					dependancyMatrix[previousGoal.id - 1][goalAtI.id - 1] = 1;
				}
				
				findDependencies(neighbor, goodDir(neighbor, d), goalAtI);
			} else {
				findDependencies(neighbor, goodDir(neighbor, d), previousGoal);
			}
		}
	}
	
	private List<Dir> freeNeighborsDir(int[] i) {
		List<Dir> freeNeighbors = new ArrayList<Dir>();

		int[] nNeighbor = Utils.neighborOf(i, Dir.N);
		int[] sNeighbor = Utils.neighborOf(i, Dir.S);
		int[] eNeighbor = Utils.neighborOf(i, Dir.E);
		int[] wNeighbor = Utils.neighborOf(i, Dir.W);

		if (!node.walls[nNeighbor[0]][nNeighbor[1]])
			freeNeighbors.add(Dir.N);
		if (!node.walls[sNeighbor[0]][sNeighbor[1]])
			freeNeighbors.add(Dir.S);
		if (!node.walls[eNeighbor[0]][eNeighbor[1]])
			freeNeighbors.add(Dir.E);
		if (!node.walls[wNeighbor[0]][wNeighbor[1]])
			freeNeighbors.add(Dir.W);
		
		return freeNeighbors;
	}
	
	private int[] explore(int[] i, Dir d) {
		int[] neighbor = Utils.neighborOf(i, d);
		
		int wallCount = Utils.neighbouringWallsCount(neighbor[0], neighbor[1], node.walls);
		
		if (wallCount == 2)
			return explore(neighbor, goodDir(neighbor, d));
		else if (wallCount == 3)
			return neighbor;
		else if (wallCount < 2)
			return null;
		else
			return null;
	}
	
	private Dir goodDir(int[] i, Dir previousDir) {
		Set<Dir> possibleDirs = new HashSet<Dir>();

		possibleDirs.add(Dir.N);
		possibleDirs.add(Dir.S);
		possibleDirs.add(Dir.W);
		possibleDirs.add(Dir.E);
		
		possibleDirs.remove(oppositeDir(previousDir));
		
		for (Dir d : possibleDirs) {
			int[] neighbor = Utils.neighborOf(i, d);
			
			// Checking if the cell is free; we don't consider boxes and agents since those can be moved away and are not
			// static obstacles like walls
			if (!node.walls[neighbor[0]][neighbor[1]]) {
				return d;
			}
		}
		return null;
	}
	
	private Dir oppositeDir(Dir d) {
		if (d == Dir.N)
			return Dir.S;
		if (d == Dir.S)
			return Dir.N;
		if (d == Dir.W)
			return Dir.E;
		if (d == Dir.E)
			return Dir.W;
		
		return null;
	}
}

package solvers;

import java.util.HashSet;
import java.util.Set;

import searchclient.Command.Dir;
import searchclient.Node;
import searchclient.Utils;

import models.Goal;

/**
 * Implements the initial idea of Lewis algorithm.
 */
public class Lewis {
	private Node node;
	private Set<Goal> goals;
	
	public Lewis(Set<Goal> goals, Node n) {
		this.node = n;
		this.goals = goals;
	}
	
	public void solve() {
		for (int i = 0; i < node.goals.length; i++) {
			for (int j = 0; j < node.goals[i].length; j++) {
				if (Utils.findGoal(goals, node.goalIds[i][j]) == null)
					continue;
				
				// Score for current node
				int lewisScore = 0;
				
				Set<int[]> neighbors = new HashSet<int[]>();
				
				neighbors.add(Utils.neighborOf(new int[] { i, j }, Dir.N));
				neighbors.add(Utils.neighborOf(new int[] { i, j }, Dir.S));
				neighbors.add(Utils.neighborOf(new int[] { i, j }, Dir.W));
				neighbors.add(Utils.neighborOf(new int[] { i, j }, Dir.E));

				for (int[] neighbor : neighbors) {
					if (node.walls[neighbor[0]][neighbor[1]]) {
						lewisScore += 2;
					}
					if (node.goalIds[neighbor[0]][neighbor[1]] != 0) {
						lewisScore += 1;
					}
				}

				Goal goal = Utils.findGoal(goals, node.goalIds[i][j]);
				goal.lewisScore = lewisScore;
			}
		}
	}
}

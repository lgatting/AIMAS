package solvers;

import java.util.HashSet;
import java.util.Set;

import searchclient.Node;
import searchclient.Utils;
import searchclient.Command.Dir;

import models.Goal;

/**
 * Depending on the goals position and it's intermediate neighbors, the goal will
 * be assigned appropriate penalty
 */
public class PositionPenalizer {
	private Set<Goal> goals;
	private Node node;
	
	public PositionPenalizer(Set<Goal> goals, Node node) {
		this.node = node;
		this.goals = goals;
	}
	
	public void solve() {
		for (Goal goal : goals) {
			int[] goalPos = Utils.findGoalPosition(goal, node.goalIds);
			
			Set<int[]> neighbors = new HashSet<int[]>();
			
			neighbors.add(Utils.neighborOf(new int[] { goalPos[0], goalPos[1] }, Dir.N));
			neighbors.add(Utils.neighborOf(new int[] { goalPos[0], goalPos[1] }, Dir.S));
			neighbors.add(Utils.neighborOf(new int[] { goalPos[0], goalPos[1] }, Dir.W));
			neighbors.add(Utils.neighborOf(new int[] { goalPos[0], goalPos[1] }, Dir.E));
			
			int wallCount = 0;
			for (int[] neighbor : neighbors) {
				if (node.walls[neighbor[0]][neighbor[1]]) {
					wallCount++;
				}
			}
			
			if (wallCount == 3)
				goal.positionPenalty = 0;
			if (wallCount == 2)
				goal.positionPenalty = 4;
			if (wallCount == 1)
				goal.positionPenalty = 2;
			if (wallCount == 0)
				goal.positionPenalty = 1;
		}
	}
}

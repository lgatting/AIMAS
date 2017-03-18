package searchclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.PriorityQueue;

import searchclient.Memory;
import searchclient.NotImplementedException;

public abstract class Strategy extends Thread {
	private HashSet<Node> explored;
	private final long startTime;

	public Strategy() {
		this.explored = new HashSet<Node>();
		this.startTime = System.currentTimeMillis();
	}

	public void addToExplored(Node n) {
		this.explored.add(n);
	}

	public boolean isExplored(Node n) {
		return this.explored.contains(n);
	}

	public int countExplored() {
		return this.explored.size();
	}

	public String searchStatus() {
		return String.format("#Explored: %,6d, #Frontier: %,6d, #Generated: %,6d, Time: %3.2f s \t%s", this.countExplored(), this.countFrontier(), this.countExplored()+this.countFrontier(), this.timeSpent(), Memory.stringRep());
	}

	public float timeSpent() {
		return (System.currentTimeMillis() - this.startTime) / 1000f;
	}

	public abstract Node getAndRemoveLeaf();

	public abstract void addToFrontier(Node n);

	public abstract boolean inFrontier(Node n);

	public abstract int countFrontier();

	public abstract boolean frontierIsEmpty();

	@Override
	public abstract String toString();

	public static class StrategyBFS extends Strategy {
		private ArrayDeque<Node> frontier;
		private HashSet<Node> frontierSet;

		public StrategyBFS() {
			super();
			frontier = new ArrayDeque<Node>();
			frontierSet = new HashSet<Node>();
		}

		@Override
		public Node getAndRemoveLeaf() {
			Node n = frontier.pollFirst();
			frontierSet.remove(n);
			return n;
		}

		@Override
		public void addToFrontier(Node n) {
			frontier.addLast(n);
			frontierSet.add(n);
		}

		@Override
		public int countFrontier() {
			return frontier.size();
		}

		@Override
		public boolean frontierIsEmpty() {
			return frontier.isEmpty();
		}

		@Override
		public boolean inFrontier(Node n) {
			return frontierSet.contains(n);
		}

		@Override
		public String toString() {
			return "Breadth-first Search";
		}
	}

	public static class StrategyDFS extends Strategy {
        private ArrayDeque<Node> frontier;
        private HashSet<Node> frontierSet;
        
		public StrategyDFS() {
			super();
            frontier = new ArrayDeque<Node>();
            frontierSet = new HashSet<Node>();
		}

		@Override
		public Node getAndRemoveLeaf() {
            Node n = frontier.pollLast();
            frontierSet.remove(n);
            return n;
		}

		@Override
		public void addToFrontier(Node n) {
            frontier.addLast(n);
            frontierSet.add(n);
		}

        @Override
        public int countFrontier() {
            return frontier.size();
        }
        
        @Override
        public boolean frontierIsEmpty() {
            return frontier.isEmpty();
        }
        
        @Override
        public boolean inFrontier(Node n) {
            return frontierSet.contains(n);
        }
		
		@Override
        public String toString() {
			return "Depth-first Search";
		}
	}

	// Ex 3: Best-first Search uses a priority queue (Java contains no implementation of a Heap data structure)
	public static class StrategyBestFirst extends Strategy {
		private Heuristic heuristic;
		
		private PriorityQueue<Node> frontier;
        private HashSet<Node> frontierSet;

		public StrategyBestFirst(Heuristic h) {
			super();
			
			heuristic = h;
			
			frontier = new PriorityQueue<Node>(heuristic);
                        frontierSet = new HashSet<Node>();
                        
		}

		@Override
		public Node getAndRemoveLeaf() {
			Node n = frontier.poll();
			frontierSet.remove(n);
                     
			return n;
		}

                /*
                public boolean agentONgoal(Node n){
                    
                   
				char b = Character.toLowerCase(n.goals[n.agentRow][n.agentCol]);
				if (b > 0) { // if not a goal 
                                    return true ;
                                }
                                else return false ;
                }
                */
		@Override
		public void addToFrontier(Node n) {
                  
                 //  if(!agentONgoal(n)){
			frontier.offer(n);
                     frontierSet.add(n);
                ///   }
		}

		@Override
		public int countFrontier() {
            return frontier.size();
		}

		@Override
		public boolean frontierIsEmpty() {
            return frontier.isEmpty();
		}

		@Override
		public boolean inFrontier(Node n) {
            return frontierSet.contains(n);
		}

		@Override
		public String toString() {
			return "Best-first Search using " + this.heuristic.toString();
		}
	}
}

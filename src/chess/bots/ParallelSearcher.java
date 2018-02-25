package chess.bots;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;
import cse332.exceptions.NotYetImplementedException;
import chess.bots.SimpleSearcher;

public class ParallelSearcher<M extends Move<M>, B extends Board<M, B>> extends
        AbstractSearcher<M, B> {
	private static final ForkJoinPool POOL = new ForkJoinPool();
	private static final int DIVIDE_CUTOFF = 5;
	
    public M getBestMove(B board, int myTime, int opTime) {
        BestMove<M> best = parallel(this.evaluator, board, cutoff);
        return best.move;
    }

    private BestMove<M> parallel(Evaluator<B> evaluator, B board, int cutoff) {
    	return POOL.invoke(new ParallelTask(evaluator, board, cutoff, null, 0, 0));
	}
    
    private class ParallelTask extends RecursiveTask<BestMove<M>> {
    	Evaluator<B> evaluator;
    	B board;
    	int depth;
    	ParallelTask[] arr;
    	int lo;
    	int hi;
    	
    	public ParallelTask(Evaluator<B> evaluator, B board, int depth, ParallelTask[] arr, int lo, int hi) {
    		this.evaluator = evaluator;
    		this.board = board;
    		this.depth = depth;
    		this.arr = arr;
    		this.lo = lo;
    		this.hi = hi;
    	}
    	
    	// when CUTOFF is reached, call minimax
    	// when DIVIDE_CUTOFF is reached, switch from divide-and-conquer to forking sequentially
		@Override
		protected BestMove<M> compute() {
			// If cutoff is reached, execute sequentially
    		if (depth == cutoff) {
    			board = board.copy();
    			return SimpleSearcher.minimax(evaluator, board, depth);
    		}
    		
    		// Forking sequentially
    		if ((hi - lo) <= DIVIDE_CUTOFF) {    		    		
    			ArrayList<ParallelSearcher<M, B>.ParallelTask> tasks = new ArrayList<ParallelTask>(); // list of tasks
	    		BestMove<M> bestMove = new BestMove<M>(-evaluator.infty());		// initializes best move with negative infinity
	    		List<M> moves = board.generateMoves();							// list of available moves
	        	
	    		// for each move, copy the b apply the move, add parallel task, fork parallel task
	    		for (int i = lo; i < hi - 1; i++) {
	        		this.board = board.copy();
	        		M move = moves.get(i);
	        		this.board.applyMove(move);
	        
	        		ParallelTask task = new ParallelTask(evaluator, board, depth - 1, arr, 0, board.generateMoves().size());
	        		tasks.add(task);
	        		task.fork();
	        	}
	        	// Exits the for loop and evaluates the last move
	    		this.board = board.copy();
	    		M move = moves.get(hi - 1);		// retrieves the last move
	        	board.applyMove(move);
	        	
        		ParallelTask task = new ParallelTask(evaluator, board, depth - 1, arr, 0, board.generateMoves().size());
        		BestMove<M> current = task.compute();		// computing current task will return the current best move
        			
        		if (current.value > bestMove.value) {
        			bestMove = new BestMove<M>(move, current.value);
        		}
        		
        		// finally, compare last move with each move associated with each parallel task
        		for (int i = lo; lo < hi; i++) {
        			this.board = board.copy();
        			move = moves.get(i + lo);
        			this.board.applyMove(move);
        			
        			task = new ParallelTask(evaluator, board, depth - 1, arr, 0, board.generateMoves().size());
        			current = task.compute();
        			
        			if (current.value > bestMove.value) {
        				bestMove = new BestMove<M>(move, current.value);
        			}
        		}        		
        		
        		return bestMove;
	        } else { 
    			// Forking via divide-and-conquer
        		ParallelTask left = new ParallelTask(evaluator, board, depth - 1, arr, lo, (lo + hi) / 2);
	    		ParallelTask right = new ParallelTask(evaluator, board, depth - 1, arr, lo + (hi - lo) / 2, hi);
	    		
	    		left.fork();
	    		
	    		BestMove<M> leftResult = right.compute();
	    		BestMove<M> rightResult = left.join();
	    				
	    		if (leftResult != null) {
	    			return leftResult;
	    		} else if (rightResult != null) {
	    			return rightResult;
	    		} else {
	    			return null;
	    		}
    		}
		}
    }
}
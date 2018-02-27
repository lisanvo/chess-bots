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
	private static final int DIVIDE_CUTOFF = 3;
	
    public M getBestMove(B board, int myTime, int opTime) {
        BestMove<M> best = parallel(this.evaluator, board, cutoff);
        return best.move;
    }

    private BestMove<M> parallel(Evaluator<B> evaluator, B board, int cutoff) {
    	return POOL.invoke(new ParallelTask(evaluator, board, cutoff, 0, board.generateMoves().size()));
	}
    
    private class ParallelTask extends RecursiveTask<BestMove<M>> {
    	Evaluator<B> evaluator;
    	B board;
    	int depth;
    	int lo;
    	int hi;
    	
    	public ParallelTask(Evaluator<B> evaluator, B board, int depth, int lo, int hi) {
    		this.evaluator = evaluator;
    		this.board = board;
    		this.depth = depth;
    		this.lo = lo;
    		this.hi = hi;
    	}
    	
    	// when CUTOFF is reached, call minimax
    	// when DIVIDE_CUTOFF is reached, switch from divide-and-conquer to forking sequentially
		@Override
		protected BestMove<M> compute() {
			// If cutoff is reached, execute sequentially
    		if (depth == cutoff) {
    			B boardCopy = board.copy();
    			return SimpleSearcher.minimax(evaluator, boardCopy, depth);
    		}
    		
    		// Forking sequentially
    		if ((hi - lo) <= DIVIDE_CUTOFF) {
    			ArrayList<ParallelSearcher<M, B>.ParallelTask> tasks = new ArrayList<ParallelTask>();
	    		BestMove<M> bestMove = new BestMove<M>(-evaluator.infty());		// initializes best move with negative infinity
	    		List<M> moves = board.generateMoves();
	        	
	    		// for each move, copy the b apply the move, add parallel task, fork parallel task
	    		for (int i = lo; i < hi - 1; i++) {
	    			B boardCopy = board.copy();
	    			M move = moves.get(i + lo);
	        		boardCopy.applyMove(move);
	        		moves = boardCopy.generateMoves();
	        
	        		ParallelTask task = new ParallelTask(evaluator, boardCopy, depth - 1, i + lo, moves.size());
	        		tasks.add(task);
	        		task.fork();
	        	}
	    		
	        	// Exits the for loop and evaluates the last move
	    		B boardCopy = board.copy();
	    		M move = moves.get(hi - 1);
	    		boardCopy.applyMove(move);
	    		moves = boardCopy.generateMoves();
	        	
        		ParallelTask task = new ParallelTask(evaluator, boardCopy, depth - 1, lo, moves.size());
        		BestMove<M> current = task.compute().negate();		// computing current task will return the current best move
        			
        		if (current.value > bestMove.value) {
        			bestMove = new BestMove<M>(move, current.value);
        		}
        		
        		// finally, compare last move with each move associated with each parallel task
        		for (int i = 0; i < tasks.size(); i++) {
        			ParallelTask forkedTask = tasks.get(i);
        			M joinMove = board.generateMoves().get(i + lo);
        			BestMove<M> currentMove = forkedTask.join().negate();
        			
        			if (currentMove.value > bestMove.value) {
        				bestMove = new BestMove<M>(joinMove, currentMove.value);
        			}
        		}        		
        		
        		return bestMove;
	        } else { 
    			// Forking via divide-and-conquer
        		ParallelTask left = new ParallelTask(evaluator, board, depth, lo, lo + (hi - lo) / 2);
	    		ParallelTask right = new ParallelTask(evaluator, board, depth, lo + (hi - lo) / 2, hi);
	    		
	    		left.fork();
	    		
	    		BestMove<M> rightResult = right.compute();			
	    		BestMove<M> leftResult = left.join();
	    
	    				
	    		if (leftResult.value > rightResult.value) {
	    			return leftResult;
	    		} else {
	    			return rightResult;
	    		}
    		}
		}
    }
}
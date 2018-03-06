package chess.bots;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

public class JamboreeSearcher<M extends Move<M>, B extends Board<M, B>> extends
        AbstractSearcher<M, B> {
	private static final ForkJoinPool POOL = new ForkJoinPool();
	private static final int DIVIDE_CUTOFF = 5;
	private static final double PERCENTAGE_SEQUENTIAL = 0.5;
	
	public M getBestMove(B board, int myTime, int opTime) {
    		List<M> moves = board.generateMoves();
    		BestMove<M> move = POOL.invoke(new JamboreeTask(this.evaluator, board, moves, this.cutoff, null, ply, 0, moves.size(), 
    														-evaluator.infty(), evaluator.infty(), null, true)); 
    		return move.move;
    }
    
    private class JamboreeTask extends RecursiveTask<BestMove<M>> {
		private static final long serialVersionUID = 1L;
		Evaluator<B> evaluator;
		List<M> moves;
		M move;
		B board;
		BestMove<M> best;
		int cutoff;
		int depth;
		int lo;
		int hi;
		int alpha;
		int beta;
		boolean perSequential;
		
		public JamboreeTask(Evaluator<B> evaluator, B board, List<M> moves, int cutoff, M move, 
				int depth, int lo, int hi, int alpha, int beta, BestMove<M> best, boolean perSequential) {
			this.evaluator = evaluator;
			this.board = board;
			this.moves = moves;
			this.cutoff = cutoff;
			this.move = move;
			this.depth = depth;
			this.lo = lo;
			this.hi = hi;
			this.alpha = alpha;
			this.beta = beta;
			this.best = best;
			this.perSequential = perSequential;
		}

		// when CUTOFF is reached, call minimax
		// when DIVIDE_CUTOFF is reached, switch from divide-and-conquer to forking sequentially
		@Override
		protected BestMove<M> compute() {
			// If cutoff is reached, execute sequentially
			if (move != null) {
				board = board.copy();
				board.applyMove(move);
				moves = board.generateMoves();
				hi = moves.size();
			}
			if (depth <= cutoff) {
				return AlphaBetaSearcher.alphabeta(evaluator, board, depth, alpha, beta);
			}
    		if (perSequential) {
    			lo = (int) Math.ceil(PERCENTAGE_SEQUENTIAL * hi);
    			BestMove<M> bestMove = new BestMove<M>(-evaluator.infty());
    			for (int i = 0; i < lo; i++) {
    				M tryMove = moves.get(i);
    				board.applyMove(tryMove);
    				List<M> moveList = board.generateMoves();
    				JamboreeTask task = new JamboreeTask(evaluator, board, moveList, cutoff, moves.get(i), depth - 1, 0, 0, 
							-beta, -alpha, null, true);
    				int value = task.compute().negate().value;
    				board.undoMove();
    				if (value > alpha) {
    					alpha = value;
    					bestMove.value = value;
    					bestMove.move = tryMove;
    				}
    				if (alpha >= beta) {
    					return bestMove;
    				}	
    			}
    			this.best = bestMove;
    		}
			
//			if (perSequential) {
//				BestMove<M> bestMove = new BestMove<M>(alpha);
//				lo = (int) Math.ceil(PERCENTAGE_SEQUENTIAL * hi);
//				for (int i = 0; i < lo; i++) {
//					M move = moves.get(i);
//					board.applyMove(move);
//					List<M> newMoves = board.generateMoves();
//					JamboreeTask task = new JamboreeTask(evaluator, board, newMoves, cutoff, moves.get(i), depth - 1, 0, 0, 
//														-beta, -alpha, bestMove, true);
//					BestMove<M> current = task.compute().negate();
//					board.undoMove();
//					
//					if (current.value > alpha) {
//						this.alpha = current.value;
//						bestMove.value = current.value;
//						bestMove.move = move;
//					}
//					
//		    		if (current.value >= beta) {
//		    			bestMove = new BestMove<M>(beta);
//		    			return bestMove;
//		    		}
//	
//		    		if (this.alpha > bestMove.value) {
//		    			bestMove = new BestMove<M>(move, this.alpha);
//		    		}
//				}
//				return bestMove;
//			}
			// Forking sequentially
    		if (hi - lo <= DIVIDE_CUTOFF) {
				List<JamboreeTask> tasks = new ArrayList<JamboreeTask>();
				BestMove<M> bestMove = new BestMove<M>(-evaluator.infty());
				for (int i = lo; i < hi; i++) {
					JamboreeTask tak = new JamboreeTask(evaluator, board, moves, cutoff, moves.get(i), depth - 1, 0, 0, 
														-beta, -bestMove.value, bestMove, true);
					tasks.add(tak);
					if (i != lo) {
						tak.fork();
					}
				}
				for (int i = 0; i < tasks.size(); i++) {
					int alpha;
					JamboreeTask tak = tasks.get(i);
					if (i == 0) {
						alpha = tak.compute().negate().value;
					} else {
						alpha = tak.join().negate().value;
					}
					if (alpha > bestMove.value) {
						bestMove.value = alpha;
						bestMove.move = moves.get(lo + i);
						if (alpha > beta) {
							return bestMove;
						}
					}
				}
				return bestMove;
			} else {
				// Forking via divide-and-conquer
				int mid = lo + (hi - lo) / 2;
				JamboreeTask left = new JamboreeTask(evaluator, board, moves, cutoff, null, depth, lo, mid, alpha, beta, best, false);
				JamboreeTask right = new JamboreeTask(evaluator, board, moves, cutoff, null, depth, mid, hi, alpha, beta, best, false);
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
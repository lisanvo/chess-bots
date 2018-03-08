package experiments;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

public class ParallelSearcher<M extends Move<M>, B extends Board<M, B>> extends
		AbstractSearcher<M, B> {
	private static final ForkJoinPool POOL = new ForkJoinPool();
	private static final int DIVIDE_CUTOFF = 3;

	public M getBestMove(B board, int myTime, int opTime) {
		BestMove<M> best = parallel(this.evaluator, board, cutoff);
		return best.move;
	}

	private BestMove<M> parallel(Evaluator<B> evaluator, B board, int cutoff) {
		List<M> moves = board.generateMoves();
		return POOL.invoke(new ParallelTask(evaluator, board, ply, moves, null, 0, moves.size()));
	}

	private class ParallelTask extends RecursiveTask<BestMove<M>> {
		private static final long serialVersionUID = 1L;
		Evaluator<B> evaluator;
		List<M> moves;
		M move;
		B board;
		int depth;
		int lo;
		int hi;

		public ParallelTask(Evaluator<B> evaluator, B board, int depth, List<M> moves, M move, int lo, int hi) {
			this.evaluator = evaluator;
			this.board = board;
			this.depth = depth;
			this.lo = lo;
			this.hi = hi;
			this.moves = moves;
			this.move = move;
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
				return SimpleSearcher.minimax(evaluator, board, depth);
			}
			// Forking sequentially
			if (hi - lo <= DIVIDE_CUTOFF) {
				List<ParallelTask> tasks = new ArrayList<ParallelTask>();
				BestMove<M> bestMove = new BestMove<M>(-evaluator.infty());		// initializes best move with negative infinity
				for (int i = lo; i < hi; i++) {
					ParallelTask tak = new ParallelTask(evaluator, board, depth - 1, moves, moves.get(i), 0, 0);
					tasks.add(tak);
					if (i != lo) {
						tak.fork();
					}
				}
				for (int i = 0; i < tasks.size(); i++) {
					int v;
					ParallelTask current = tasks.get(i);
					if (i == 0) {
						v = current.compute().negate().value;
					} else {
						v = current.join().negate().value;
					}
					if (v > bestMove.value) {
						bestMove.move = moves.get(i + lo);
						bestMove.value = v;
					}
				}
				return bestMove;
			} else {
				// Forking via divide-and-conquer
				int mid = lo + (hi - lo) / 2;
				ParallelTask left = new ParallelTask(evaluator, board, depth, moves, null, lo, mid);
				ParallelTask right = new ParallelTask(evaluator, board, depth, moves, null, mid, hi);
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
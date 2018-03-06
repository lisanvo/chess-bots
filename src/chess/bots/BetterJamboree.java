package chess.bots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;

public class BetterJamboree<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {
	private static final ForkJoinPool POOL = new ForkJoinPool();
	private static final int DIVIDE_CUTOFF = 5;
	private static final double PERC_SEQ = 0.3;

	public M getBestMove(B board, int myTime, int opTime) {
		List<M> moves = board.generateMoves();
		BestMove<M> move = POOL.invoke(new JamboreeTask(true, this.evaluator, board, moves, this.cutoff, null, ply, 0,
				moves.size(), -evaluator.infty(), evaluator.infty(), null));
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
		boolean sequential;

		public JamboreeTask(boolean sequential, Evaluator<B> evaluator, B board, List<M> moves, int cutoff, M move,
				int depth, int lo, int hi, int alpha, int beta, BestMove<M> best) {
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
			this.sequential = sequential;
		}

		// when CUTOFF is reached, call minimax
		// when DIVIDE_CUTOFF is reached, switch from divide-and-conquer to forking
		// sequentially
		@Override
		protected BestMove<M> compute() {
			if (move != null) {
				board = board.copy();
				board.applyMove(move);
				if (depth > cutoff && !moves.isEmpty()){
					moves = board.generateMoves();
					hi = moves.size();
				}
			}
			if (moves.isEmpty()) {
				return new BestMove<M>(board.inCheck() ? -evaluator.mate() - depth : -evaluator.stalemate());
			}
			// If cutoff is reached, execute sequentially
			if (depth <= cutoff) {
				return AlphaBetaSearcher.alphabeta(evaluator, board, depth, alpha, beta);
			}
			if (best == null) {
				Collections.sort(moves, new Comparator<M>() {
					@Override
					public int compare(M one, M two) {
						board.applyMove(one);
						int x = evaluator.eval(board);
						board.undoMove();
						board.applyMove(two);
						int y = evaluator.eval(board);
						board.undoMove();
						return Integer.signum(x - y);
					}
				});
			}
			// fork sequentially
			if (sequential) {
				lo = (int) (PERC_SEQ * hi);
				BestMove<M> move = new BestMove<>(-evaluator.infty());
				for (int i = 0; i < lo; i++) {
					M move2 = moves.get(i);
					board.applyMove(move2);
					;
					List<M> moves2 = board.generateMoves();
					int value = new JamboreeTask(true, evaluator, board, moves2, cutoff, null, depth - 1, 0,
							moves2.size(), -beta, -alpha, null).compute().negate().value;
					board.undoMove();
					if (value > alpha) {
						alpha = value;
						move.value = value;
						move.move = move2;
					}
					if (alpha >= beta) {
						return move;
					}
				}
				best = move;
			} else if (hi - lo <= DIVIDE_CUTOFF) {
				List<JamboreeTask> tasks = new ArrayList<JamboreeTask>();
				for (int i = lo; i < hi; i++) {
					JamboreeTask tak = new JamboreeTask(true, evaluator, board, moves, cutoff, moves.get(i), depth - 1,
							0, 0, -beta, -best.value, null);
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
					if (alpha > best.value) {
						best.value = alpha;
						best.move = moves.get(lo + i);
					}
					if (alpha >= beta) {
						return best;
					}
				}
				return best;
			}
			// Forking via divide-and-conquer
			int mid = lo + (hi - lo) / 2;
			JamboreeTask left = new JamboreeTask(false, evaluator, board, moves, cutoff, null, depth, lo, mid, alpha,
					beta, best);
			JamboreeTask right = new JamboreeTask(false, evaluator, board, moves, cutoff, null, depth, mid, hi, alpha,
					beta, best);
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
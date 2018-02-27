package chess.bots;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;
import cse332.exceptions.NotYetImplementedException;

public class AlphaBetaSearcher<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {
	
    public M getBestMove(B board, int myTime, int opTime) {
    	BestMove<M> best = alphabeta(this.evaluator, board, ply, -evaluator.infty(), evaluator.infty());
        return best.move;
    }
    
    static <M extends Move<M>, B extends Board<M, B>> BestMove<M> alphabeta(Evaluator<B> evaluator, B board, int depth,
    																		int alpha, int beta) {	
		if (depth == 0) {
			int value = evaluator.eval(board);
			return new BestMove<M>(value);
		}
		
		if (board.generateMoves().isEmpty()) {
			if (board.inCheck()) {
				return new BestMove<M>(-evaluator.mate() - depth);
			} else {
				return new BestMove<M>(-evaluator.stalemate());
			}
		} 
		
		BestMove<M> bestMove = new BestMove<M>(alpha);
		int newAlpha = alpha;
    	for (M move : board.generateMoves()) {
    		board.applyMove(move);
    		BestMove<M> current = alphabeta(evaluator, board, depth - 1, -beta, -newAlpha).negate();
    		board.undoMove();
    		
    		// if current value > the lower bound (alpha), update newAlpha because
    		// newAlpha has to be maximized
			if (current.value > alpha) {
				newAlpha = current.value;
			}
    		
    		// if current value >= the upper bound (beta), use beta because 
			// the corresponding move is unreachable
    		if (current.value >= beta) {
    			bestMove = new BestMove<M>(beta);
    			return bestMove;
    		}
    		
    		// compare newAlpha and best value
    		if (newAlpha > bestMove.value) {
    			bestMove = new BestMove<M>(move, newAlpha);
    		}
    	}
    	
    	return bestMove;	
    }
}
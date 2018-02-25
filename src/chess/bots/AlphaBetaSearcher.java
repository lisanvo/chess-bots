package chess.bots;

import cse332.chess.interfaces.AbstractSearcher;
import cse332.chess.interfaces.Board;
import cse332.chess.interfaces.Evaluator;
import cse332.chess.interfaces.Move;
import cse332.exceptions.NotYetImplementedException;

public class AlphaBetaSearcher<M extends Move<M>, B extends Board<M, B>> extends AbstractSearcher<M, B> {
	
    public M getBestMove(B board, int myTime, int opTime) {
    	BestMove<M> best = alphabeta(this.evaluator, board, ply);
        return best.move;
    }
    
    static <M extends Move<M>, B extends Board<M, B>> BestMove<M> alphabeta(Evaluator<B> evaluator, B board, int depth) {	
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
		
		BestMove<M> bestMove = new BestMove<M>(-evaluator.infty());
    	for (M move : board.generateMoves()) {
    		board.applyMove(move);
    		BestMove<M> current = alphabeta(evaluator, board, depth - 1).negate();
    		board.undoMove();
    		if (current.value > bestMove.value) {
    			bestMove = new BestMove<M>(move, current.value);
    		}
    	}
    	return bestMove;	
    }
}
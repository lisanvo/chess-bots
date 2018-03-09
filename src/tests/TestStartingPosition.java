package tests;

import chess.board.ArrayBoard;
import chess.board.ArrayMove;
import chess.bots.ParallelSearcher;
import chess.game.SimpleEvaluator;
import cse332.chess.interfaces.Searcher;

public class TestStartingPosition {
    public static String STARTING_POSITION = "2kr2r1/p6p/2n5/3pp3/1pp5/2qPPQ2/P1P4P/R1R2K2 b Hh -" + 
    		" 0 1";

    public static ArrayMove getBestMove(String fen, Searcher<ArrayMove, ArrayBoard> searcher, int depth, int cutoff) { 
        searcher.setDepth(depth);
        searcher.setCutoff(cutoff);
        searcher.setEvaluator(new SimpleEvaluator());

        return searcher.getBestMove(ArrayBoard.FACTORY.create().init(fen), 0, 0);
    }
    
    public static void printMove(String fen, Searcher<ArrayMove, ArrayBoard> searcher, int depth, int cutoff) {
        String botName = searcher.getClass().toString().split(" ")[1].replace("chess.bots.", "");
        System.out.println(botName + " returned " + getBestMove(fen, searcher, depth, cutoff));
    }
    public static void main(String[] args) {
        Searcher<ArrayMove, ArrayBoard> dumb = new ParallelSearcher<>();
        printMove(STARTING_POSITION, dumb, 3, 0);
    }
}

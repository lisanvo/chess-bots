package experiments;

import chess.board.ArrayBoard;
import chess.board.ArrayMove;
import chess.game.SimpleEvaluator;
import chess.game.SimpleTimer;
import cse332.chess.interfaces.Searcher;
import chess.bots.JamboreeSearcher;
import chess.bots.ParallelSearcher;

public class CutoffTests {
	private static final int[] cutoffs = {1, 2, 3, 4};
	private static final String[] fens = {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -",
										"r3k2r/pp5p/2n1p1p1/2pp1p2/5B2/2qP1Q2/P1P2PPP/R4RK1 w Hkq -",
										"2k3r1/p6p/2n5/3pp3/1pp1P3/2qP4/P1P1K2P/R1R5 b Hh -"};
	private static final int TRIALS = 20;
	
	public static void test(int fenIndex, int cutoffIndex) {
		ParallelSearcher<ArrayMove, ArrayBoard> parallel = new ParallelSearcher<ArrayMove, ArrayBoard>();
		JamboreeSearcher<ArrayMove, ArrayBoard> jamboree = new JamboreeSearcher<ArrayMove, ArrayBoard>();
		long[] paraRuntimes = new long[TRIALS];
		long[] jamRuntimes = new long[TRIALS];
		
		// trials
		for (int i = 0; i < TRIALS; i++) {
			long paraStart = 0;
			long paraStop = 0;
			long jamStart = 0;
			long jamStop = 0;
			
			paraStart = System.currentTimeMillis();
			run(parallel, fens[fenIndex], cutoffs[cutoffIndex]);
			paraStop = System.currentTimeMillis();
			
			jamStart = System.currentTimeMillis();
			run(jamboree, fens[fenIndex], cutoffs[cutoffIndex]);
			jamStop = System.currentTimeMillis();
			
			paraRuntimes[i] = paraStop - paraStart;
			jamRuntimes[i] = jamStop - jamStart;
		}
		
		long paraAverage = 0;
		long jamAverage = 0;
		
		for (int j = 0; j < TRIALS; j++) {
			paraAverage += paraRuntimes[j];
			jamAverage += jamRuntimes[j];
		}
		
		System.out.println("FEN_INDEX: " + fenIndex + "| CUTOFF_INDEX: " + cutoffIndex);
		System.out.println("Parallel: " + (paraAverage /= TRIALS));
		System.out.println("Parallel: " + (jamAverage /= TRIALS));
	}
	
	public static void run(Searcher<ArrayMove, ArrayBoard> bot, String fen, int cutoff) {
		bot.setCutoff(cutoff);
		bot.setDepth(5);
		bot.setEvaluator(new SimpleEvaluator());
        bot.getBestMove(ArrayBoard.FACTORY.create().init(fen), 0, 0);
	}

	public static void main(String[] args) {	
		// Beginning fen tested with each cutoff
		test(0, 0);
		test(0, 1);
		test(0, 2);
		test(0, 3);
		// Middle fen tested with each cutoff
		test(1, 0);
		test(1, 1);
		test(1, 2);
		test(1, 3);
		// End state tested with each cutoff
		test(2, 0);
		test(2, 1);
		test(2, 2);
		test(2, 3);
	}
}

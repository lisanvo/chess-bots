package experiments;

import chess.board.ArrayBoard;
import chess.board.ArrayMove;
import chess.game.SimpleEvaluator;
import chess.game.SimpleTimer;
import cse332.chess.interfaces.Searcher;
import experiments.SimpleSearcher;
import experiments.AlphaBetaSearcher;
import experiments.ParallelSearcher;
import experiments.JamboreeSearcher;

public class ComparisonTests {
	private static final int[][] processors = {{0, 0},  // start board
											   {0, 0},  // middle board
											   {0, 0}}; // end board
	private static final int[][] cutoffs = {{3, 2},  	// start board
											{2, 1},		// middle board
											{2, 1}};	// end board
	private static final String[] fens = {"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -",
										"r3k2r/pp5p/2n1p1p1/2pp1p2/5B2/2qP1Q2/P1P2PPP/R4RK1 w Hkq -",
										"2k3r1/p6p/2n5/3pp3/1pp1P3/2qP4/P1P1K2P/R1R5 b Hh -"};
	private static final int TRIALS = 20;
	
	public static void test(int fenIndex, int cutoffIndex, int processorIndex) {
		SimpleSearcher<ArrayMove, ArrayBoard> simple = new SimpleSearcher<ArrayMove, ArrayBoard>();
		AlphaBetaSearcher<ArrayMove, ArrayBoard> ab = new AlphaBetaSearcher<ArrayMove, ArrayBoard>();
		ParallelSearcher<ArrayMove, ArrayBoard> parallel = new ParallelSearcher<ArrayMove, ArrayBoard>();
		JamboreeSearcher<ArrayMove, ArrayBoard> jamboree = new JamboreeSearcher<ArrayMove, ArrayBoard>();
		long[] simRuntimes = new long[TRIALS];
		long[] abRuntimes = new long[TRIALS];
		long[] paraRuntimes = new long[TRIALS];
		long[] jamRuntimes = new long[TRIALS];
		
		// loop by number of trials
		for (int i = 0; i < TRIALS; i++) {
			parallel.setProcessors(processors[processorIndex][0]);
			jamboree.setProcessors(processors[processorIndex][1]);
			
			long simStart = 0;
			long simStop = 0;
			long abStart = 0;
			long abStop = 0;
			long paraStart = 0;
			long paraStop = 0;
			long jamStart = 0;
			long jamStop = 0;
			
			simStart = System.nanoTime();
			run(simple, fens[fenIndex], cutoffs[cutoffIndex][cutoffIndex]);
			simStop = System.nanoTime();
			
			abStart = System.nanoTime();
			run(ab, fens[fenIndex], cutoffs[cutoffIndex][cutoffIndex]);
			abStop = System.nanoTime();
			
			paraStart = System.nanoTime();
			run(parallel, fens[fenIndex], cutoffs[cutoffIndex][0]);
			paraStop = System.nanoTime();
			
			jamStart = System.nanoTime();
			run(jamboree, fens[fenIndex], cutoffs[cutoffIndex][1]);
			jamStop = System.nanoTime();
			
			paraRuntimes[i] = paraStop - paraStart;
			jamRuntimes[i] = jamStop - jamStart;
		}
		
		long simAverage = 0;
		long abAverage = 0;
		long paraAverage = 0;
		long jamAverage = 0;
		
		for (int j = 0; j < TRIALS; j++) {
			simAverage += simRuntimes[j];
			abAverage += abRuntimes[j];
			paraAverage += paraRuntimes[j];
			jamAverage += jamRuntimes[j];
		}
		
		System.out.println("FEN_INDEX: " + fenIndex + " | CUTOFF_INDEX: " + cutoffIndex);
		System.out.println("Simple: " + (simAverage /= TRIALS));
		System.out.println("AlphaBeta: " + (abAverage /= TRIALS));
		System.out.println("Parallel: " + (paraAverage /= TRIALS));
		System.out.println("Jamboree: " + (jamAverage /= TRIALS));
	}
	
	public static void run(Searcher<ArrayMove, ArrayBoard> bot, String fen, int cutoff) {
		bot.setCutoff(cutoff);
		bot.setDepth(5);
		bot.setEvaluator(new SimpleEvaluator());
        bot.getBestMove(ArrayBoard.FACTORY.create().init(fen), 0, 0);
	}

	public static void main(String[] args) {	
		// Beginning fen tested with each cutoff
		test(0, 0, 0);
		test(0, 1, 1);
		test(0, 2, 2);
		test(0, 3, 3);
		// Middle fen tested with each cutoff
		test(1, 0, 0);
		test(1, 1, 1);
		test(1, 2, 2);
		test(1, 3, 3);
		// End state tested with each cutoff
		test(2, 0, 0);
		test(2, 1, 1);
		test(2, 2, 2);
		test(2, 3, 3);
	}
}

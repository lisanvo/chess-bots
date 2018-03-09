package tests;

import chess.board.ArrayBoard;
import chess.board.ArrayMove;
import chess.game.SimpleEvaluator;
import cse332.chess.interfaces.Searcher;

public class TestGame {
	public Searcher<ArrayMove, ArrayBoard> whitePlayer;
	public Searcher<ArrayMove, ArrayBoard> blackPlayer;
	public static final String STARTING_POSITION = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

	private ArrayBoard board;

	int depth = 2;
	int cutoff = depth / 2;
	
	public static void main(String[] args) {
		TestGame game = new TestGame();
		game.play();
	}
	
	
	public TestGame() {
		setupWhitePlayer(new SimpleSearcher<ArrayMove, ArrayBoard>(), depth + 1, cutoff);
		setupBlackPlayer(new tests.AlphaBetaSearcher<ArrayMove, ArrayBoard>(), depth, cutoff);
	}

	public void play() {
		this.board = ArrayBoard.FACTORY .create().init(STARTING_POSITION);
		Searcher<ArrayMove, ArrayBoard> currentPlayer = this.blackPlayer;

		int turn = 0;

		/* Note that this code does NOT check for stalemate... */
		while (!board.inCheck() || board.generateMoves().size() > 0) {
			currentPlayer = currentPlayer.equals(this.whitePlayer) ? this.blackPlayer : this.whitePlayer;
			System.out.printf("%3d: " + board.fen() + "\n", turn);
			this.board.applyMove(currentPlayer.getBestMove(board, 1000, 1000));
			turn++;
		}
		System.out.println(JamboreeSearcher.COUNTER + AlphaBetaSearcher.COUNTER);
	}

	public Searcher<ArrayMove, ArrayBoard> setupPlayer(Searcher<ArrayMove, ArrayBoard> searcher, int depth, int cutoff) {
		searcher.setDepth(depth);
		searcher.setCutoff(cutoff);
		searcher.setEvaluator(new SimpleEvaluator());
		return searcher; 
	}
	public void setupWhitePlayer(Searcher<ArrayMove, ArrayBoard> searcher, int depth, int cutoff) {
		this.whitePlayer = setupPlayer(searcher, depth, cutoff);
	}
	public void setupBlackPlayer(Searcher<ArrayMove, ArrayBoard> searcher, int depth, int cutoff) {
		this.blackPlayer = setupPlayer(searcher, depth, cutoff);
	}
}

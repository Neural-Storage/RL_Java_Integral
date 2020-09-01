package flappybird.environment;

public class Run {
	public static void main(String[] args) {
		Environment env = new Environment();
		Double cumulativeReward = 0.0;

		int step = 1;
		String action;

		while (!env.gameOver()) {
			System.out.println(String.format("----------step %d----------", step));
			
			// print current state (For RL)
			System.out.println("game state(RL):");
			System.out.println(env.getGameState());

			// print current state (For human readable)
			// don't train with this information
			System.out.println("--------------------");
			System.out.println("bird and pillars information(human):");
			env.printState();
			
			if (Environment.r.nextDouble() < 0.15) {
				action = "flap";
				cumulativeReward += env.act(Action.FLAP);
			} else {
				action = "no action";
				cumulativeReward += env.act(Action.NONE);
			}
			System.out.println("--------------------");
			System.out.println(String.format("your action performed in this step: %s", action));

			System.out.println("--------------------");
			System.out.println("Cumulative Reward: " + cumulativeReward);
			System.out.println("--------------------\n");
			step += 1;
		}
	}

}

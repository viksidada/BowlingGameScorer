package com.bowling.refimpl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

import com.bowling.tests.ScorerImpl1ErrorTest;
import com.interview.bowling.Scorer;

public class ScorerRefImpl implements Scorer {                                   
	Integer playersCount;                                        // to save memory
	HashMap<String, Integer> playersIndex;
	List<String> players;
	List<Integer> playersScore;
	List<Integer> prevScoreCount;
	BufferedReader reader;

	public void init(InputStream inputStream) throws InvalidInput {              
		if(inputStream == null)
			throw new InvalidInput("input stream supplied is null");
		players = new ArrayList<String>();
		playersScore = new ArrayList<Integer>();
		prevScoreCount = new ArrayList<Integer>();
		playersIndex = new HashMap<String, Integer>();
		reader = new BufferedReader(new InputStreamReader(inputStream));
		try {
			// READ number of players
			String line = reader.readLine();
			checkLineValidation(line, LineType.NUMBER_OF_PLAYERS);
			playersCount = Integer.parseInt(line);

			if(playersCount < 1)
				throw new InvalidInput("players count cannot be less than 1");
			
			// READ names of players
			for (int i = 0; i < playersCount; i++) {
				line = reader.readLine();
				checkLineValidation(line, LineType.PLAYER_NAME);
				players.add(new String(line));
				playersScore.add(new Integer(0));              // what is being added to playersScore?
				prevScoreCount.add(new Integer(0));
				playersIndex.put(line, i);
			}

			// READ players score for every frame
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < playersCount; j++) {
					line = reader.readLine();
					if (i < 9)
						checkLineValidation(line, LineType.FRAME);
					else
						checkLineValidation(line, LineType.TENTH_FRAME);
					updatePlayerScore(line, j);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new InvalidInput("input stream supplied is invalid");
		}
		finally {
			if(inputStream!=null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					//ignore
				}
			}
		}

	}

	private void updatePlayerScore(String line, int playerIndex) {
		
		String []numChars = spacePattern.split(line);           
		int [] nums = new int[numChars.length];
		int frameScore = 0;
		for(int i=0;i<numChars.length;i++)
			nums[i] = Integer.parseInt(numChars[i]);
		if (nums.length == 1) {
			frameScore = 10;
			frameScore += getPrevCountScore(frameScore, playerIndex);
			changeCount(playerIndex, 2);
		} else if (nums.length == 2) {
			frameScore = nums[0]+nums[1];
			frameScore += getPrevCountScore(nums[0], playerIndex);
			frameScore += getPrevCountScore(nums[1], playerIndex);
			if(nums[0]+nums[1] == 10)
				changeCount(playerIndex, 1);
		} else {
			frameScore = nums[0]+nums[1]+nums[2];
			frameScore += getPrevCountScore(nums[0], playerIndex);
			frameScore += getPrevCountScore(nums[1], playerIndex);
			frameScore += getPrevCountScore(nums[2], playerIndex);
		}
		playersScore.set(playerIndex, playersScore.get(playerIndex)+frameScore);    //sets playersScore at index playerIndex in playersScore
	}

	private int getPrevCountScore(int score, int playerIndex) {
		int result = 0;
		int count = prevScoreCount.get(playerIndex);
		if(count > 1) {
			result += (score*(count-1));
			changeCount(playerIndex,-1*(count-1));
		}
		else if(count == 1){
			result += score;
			changeCount(playerIndex,-1);
		}
		return result;
	}

	private void changeCount(int playerIndex, int count) {              //just sets the count value in prevScoreCount
		prevScoreCount.set(playerIndex, prevScoreCount.get(playerIndex)+count);
	}

	public List<String> getPlayers() {
		final List<String> list = new ArrayList(players);
		return list;
	}

	public Integer getPlayerScore(String player) throws InvalidInput {
		if(player == null)
			throw new InvalidInput("Player name cannot be null");
		Integer index = playersIndex.get(player);
		if(index == null)
			throw new InvalidInput("Player name '"+player+"' was not supplied in input");
		return playersScore.get(playersIndex.get(player));
	}

	public static void main(String[] args) {
		checkNegetiveCases(args);
		InputStream stream = fromClasspath(args[0]);
		ScorerRefImpl scorer = new ScorerRefImpl();
		scorer.init(stream);
		List<String> players = scorer.getPlayers();
		for (String player : players) {
			System.out.println(String.format("Player %s scored %d", player, scorer.getPlayerScore(player)));
		}
	}

	private static void checkNegetiveCases(String[] args) {
		if (args.length == 0) {
			System.out.println("Need file name as argument to this program");
			System.exit(1);                                          //System.exit(system call) terminates the currently running Java virtual machine by initiating its shutdown sequence
		}
		if (args[0] == null || args[0].length() == 0) {
			System.out.println("pass a valid file name as argument");
			System.exit(1);
		}
	}

	private static InputStream fromClasspath(String onClasspath) {
		ClassLoader loader = (ScorerImpl1ErrorTest.class.getClassLoader() == null ? ClassLoader.getSystemClassLoader()
				: ScorerImpl1ErrorTest.class.getClassLoader());
		InputStream resource = loader.getResourceAsStream(onClasspath);
		if (resource == null) {
			try {
				resource = new FileInputStream("src/test/resources/" + onClasspath);
			} catch (FileNotFoundException fnfe) {
				throw new AssertionError(String.format(
						"Could not find %s [ try running on command-line via 'mvn test' or 'ant test' ]", onClasspath));
			}
		}
		return resource;
	}

	Pattern numberPattern = Pattern.compile("^\\d+$");  // .compile caches pattern in-memory and is better when matching is done repeatedly
	Pattern alphaNumSpacePattern = Pattern.compile("^[a-zA-Z 0-9]+$");
	Pattern numberAndSpacePattern = Pattern.compile("^[0-9 ]+$");
	Pattern spacePattern = Pattern.compile(" ");

	private void checkLineValidation(String line, LineType type) {                    
		switch (type) {
		case NUMBER_OF_PLAYERS:
			if (!numberPattern.matcher(line).matches())
				throw new InvalidInput("Number of players provided contains some characters which are not number");
			break;
		case PLAYER_NAME:
			if (!alphaNumSpacePattern.matcher(line).matches())
				throw new InvalidInput("Player name is either empty or contains other than alpha numeric characters");
			if(playersIndex.get(line)!=null)                         //if playerIndex is present for this players name, it is a duplicate
				throw new InvalidInput("Player name duplication");
			break;
		case FRAME:
			if (!numberAndSpacePattern.matcher(line).matches())
				throw new InvalidInput("Score frame contains non-numeric characters");
			String[] frameScores = spacePattern.split(line);         // Breaking the frame score in individual strings and saving them in an array
			if (frameScores.length != 1 && frameScores.length != 2)
				throw new InvalidInput("A (1-9th) score frame can contain only 1-2 scores, but an entry with "
						+ frameScores.length + " found");
			if(frameScores.length == 1 && Integer.parseInt(frameScores[0]) != 10)
				throw new InvalidInput("There cannot be a single score in frame with value other than 10 (X-strike)");
			if(frameScores.length == 2 && (Integer.parseInt(frameScores[0])+Integer.parseInt(frameScores[1]) > 10))
				throw new InvalidInput("two scores in the frame exceeds 10 pins: "+line);
			if(frameScores.length == 2 && Integer.parseInt(frameScores[0]) == 10)
				throw new InvalidInput("two scores in the frame with first one as strike: "+line);
			break;
		case TENTH_FRAME:
			if (!numberAndSpacePattern.matcher(line).matches())
				throw new InvalidInput("Score frame contains non-numeric characters");
			String[] _10thFrameScores = spacePattern.split(line);
			if (_10thFrameScores.length == 1)
				throw new InvalidInput("there should be minimum 2 scores in 10th frame, even if first is strike");
			if (_10thFrameScores.length == 2 && Integer.parseInt(_10thFrameScores[0]) ==10)
				throw new InvalidInput("There should be 3 scores if first one is strike");
			if ((Integer.parseInt(_10thFrameScores[0])+Integer.parseInt(_10thFrameScores[1])) == 10 && _10thFrameScores.length==2)
				throw new InvalidInput("no third score on spare");
			if ((Integer.parseInt(_10thFrameScores[0])+Integer.parseInt(_10thFrameScores[1])) < 10 && _10thFrameScores.length==3)
				throw new InvalidInput("there cannot be third score when there is no spare");
			break;
		default:
			System.err.println("Invalid line validation type");
		}
	}

}

enum LineType {
	NUMBER_OF_PLAYERS, PLAYER_NAME, FRAME, TENTH_FRAME;
}

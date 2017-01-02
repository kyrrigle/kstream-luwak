package com.lexalytics.kstreamluwak;

import java.util.Arrays;

import com.lexalytics.kstreamluwak.plus.KStreamLuwakPlusExample;

public class Main {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			usage();
			System.exit(1);
		}
		String cmd = args[0];
		args = Arrays.copyOfRange(args, 1, args.length);
		if (cmd.equalsIgnoreCase("basic")) {
			KStreamLuwakExample.main(args);
		}
		else if (cmd.equalsIgnoreCase("plus")) {
			KStreamLuwakPlusExample.main(args);
		}
		else {
			System.err.println("ERROR: I don't know what '" + cmd + "' is?");
			usage();
		}
	}

	private static void usage() {
		System.err.println("Usage: CMD [CMD_ARGS]");
		System.err.println("\nCommands:");
		System.err.println("   regular    - Very simple example with queries and docs in the same input topic");
		System.err.println("   plus       - Separate query and document topics where each have their id as the topic key");
	}

}

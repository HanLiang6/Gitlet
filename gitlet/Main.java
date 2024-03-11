package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Han Liang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "init" -> {
                numberOfOperands(args.length, 1);
                Repository.setupPersistence();
            }
            case "add" -> {
                Repository.repoExist();
                numberOfOperands(args.length, 2);
                Repository.add(args[1]);
            }
            case "commit" -> {
                Repository.repoExist();
                commitMessage(args.length);
                numberOfOperands(args.length, 2);
                Repository.newCommit(args[1]);
            }
            case "rm" -> {
                numberOfOperands(args.length, 2);
                Repository.remove(args[1]);
            }
            case "log" -> {
                numberOfOperands(args.length, 1);
                Repository.log();
            }
            case "global-log" -> {
                numberOfOperands(args.length, 1);
                Repository.globalLog();
            }
            case "checkout" -> {
                if (args.length == 3 && args[1].equals("--")) {
                    String fileName = args[2];
                    Repository.checkoutFile(fileName, Repository.refHEADCommit());
                } else if (args.length == 4 && args[2].equals("--")) {
                    String fileName = args[3];
                    Repository.checkoutFile(fileName, args[1]);
                } else if (args.length == 2) {
                    String branchName = args[1];
                    Repository.checkoutBranch(branchName);
                } else {
                    operandsError();
                }
            }
            case "status" -> {
                numberOfOperands(args.length, 1);
                Repository.status();
            }
            case "branch" -> {
                numberOfOperands(args.length, 2);
                Repository.branch(args[1]);
            }
            case "rm-branch" -> {
                numberOfOperands(args.length, 2);
                Repository.rmBranch(args[1]);
            }
            case "reset" -> {
                numberOfOperands(args.length, 2);
                Repository.reset(args[1]);
            }
            case "merge" -> {
                numberOfOperands(args.length, 2);
                Repository.merge(args[1]);
            }
            default -> operandsError();
        }
    }

    private static void numberOfOperands(int n, int m) {
        if (n != m) {
            operandsError();
        }
    }

    private static void commitMessage(int n) {
        if (n == 1) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
    }

    private static void operandsError() {
        System.out.println("Incorrect operands.");
        System.exit(0);
    }
}

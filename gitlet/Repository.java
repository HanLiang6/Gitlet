package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Han Liang
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /** Commit directory. */
    static final File COMMIT_DIR = join(GITLET_DIR, "commits");
    /** Staged for add directory. */
    static final File STAGED_DIR = join(GITLET_DIR, "index");
    /** Staged for removal directory. */
    static final File STAGED_RM_DIR = join(GITLET_DIR, "removal");
    /** Blob directory. */
    static final File BLOB_DIR = join(GITLET_DIR, "objects");
    /** Reference to heads and branches. */
    static final File HEADS_DIR = join(GITLET_DIR, "heads");
    /** Reference to the HEAD. */
    static final File HEAD = join(GITLET_DIR, "HEAD");

    public static void setupPersistence() throws IOException {
        if(GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }

        GITLET_DIR.mkdirs();
        COMMIT_DIR.mkdirs();
        STAGED_DIR.mkdirs();
        STAGED_RM_DIR.mkdirs();
        BLOB_DIR.mkdirs();
        HEADS_DIR.mkdirs();

        File master = join(HEADS_DIR,"master");
        HEAD.createNewFile();
        master.createNewFile();
        writeContents(HEAD, master.getPath());

        Commit init = new Commit();
        saveCommit(init);
    }

    public static void add(String fileName) {
        File fileAdded = join(CWD, fileName);

        if (!fileAdded.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        Commit commit = HEADCommit();
        byte[] fileContent = readContents(fileAdded);
        fileAdded = join(STAGED_DIR, fileName);

        if (commit.trees.containsKey(fileName)) {
            if (commit.trees.get(fileName).equals(sha1(fileContent))) {
                fileAdded.delete();
                return;
            }
        }

        writeContents(fileAdded, fileContent);
    }

    public static void newCommit(String message) {
        repoExist();
        if (emptyIndex()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit commit = new Commit(message);
        saveCommit(commit);
        emptyStage();
    }


    public static void remove(String fileName) throws IOException {
        repoExist();
        Commit commit = HEADCommit();
        File file = join(STAGED_DIR, fileName);
        boolean exist = file.delete();
        if (commit.trees.containsKey(fileName)) {
            file = join(STAGED_RM_DIR, fileName);
            file.createNewFile();
            file = join(CWD, fileName);
            restrictedDelete(file);
            exist = true;
        }
        if (!exist) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    public static void log() {
        repoExist();

        String refCommit = readContentsAsString(HEADPointer());
        Commit commit;
        while (refCommit != null) {
            commit = readObject(join(COMMIT_DIR, refCommit), Commit.class);
            System.out.println("===");
            System.out.println("commit " + refCommit);
            if (commit.getSecondParent() != null) {
                System.out.println("Merge: " + commit.getParent().substring(0, 7) + " " + commit.getSecondParent().substring(0, 7));
            }
            System.out.println(String.format("Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz", commit.getDate()));
            System.out.println(commit.getMessage());
            System.out.println("");
            refCommit = commit.getParent();
        }
    }

    public static void globalLog() {
        repoExist();

        for (String refCommit : plainFilenamesIn(COMMIT_DIR)) {
            Commit commit = readObject(join(COMMIT_DIR, refCommit), Commit.class);
            System.out.println("===");
            System.out.println("commit " + refCommit);
            if (commit.getSecondParent() != null) {
                System.out.println("Merge: " + commit.getParent().substring(0, 7) + " " + commit.getSecondParent().substring(0, 7));
            }
            System.out.println(String.format("Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz", commit.getDate()));
            System.out.println(commit.getMessage());
            System.out.println("");
        }
    }

    public static void checkoutFile(String fileName, String refCommit) {
        repoExist();
        File commitFile = join(COMMIT_DIR, refCommit);
        if (!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        Commit commit = readObject(commitFile, Commit.class);
        if (!commit.trees.containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        File file = join(BLOB_DIR, commit.trees.get(fileName));
        byte[] fileContent = readContents(file);
        file = join(CWD, fileName);
        writeContents(file, fileContent);
    }

    public static void checkoutBranch(String branchName) {
        repoExist();
        String currentBranch = HEADPointer().getName();
        if (currentBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        File branchFile = join(HEADS_DIR, branchName);
        if (!branchFile.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        String refCommit = readContentsAsString(branchFile);
        Commit commit = readObject(join(COMMIT_DIR, refCommit), Commit.class);
        checkOutCommit(commit);

        writeContents(HEAD, branchFile.getPath());
        emptyStage();
    }

    // TODO: Finish status command.
    public static void status() {
        repoExist();

        System.out.println("=== Branches ===");
        for(String branchName : plainFilenamesIn(HEADS_DIR)) {
            if (branchName.equals(HEADPointer().getName())) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        for(String fileName : plainFilenamesIn(STAGED_DIR)) {
            System.out.println(fileName);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for(String fileName : plainFilenamesIn(STAGED_RM_DIR)) {
            System.out.println(fileName);
        }
        System.out.println();
    }

    public static void branch(String branchName) {
        repoExist();
        File branch = join(HEADS_DIR, branchName);
        if (branch.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String refCurrentHead = refHEADCommit();
        writeContents(branch, refCurrentHead);
    }

    public static void rmBranch(String branchName) {
        repoExist();

        File branch = join(HEADS_DIR, branchName);
        if (!branch.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(HEADPointer().getName())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branch.delete();
    }

    public static void reset(String refCommit) {
        repoExist();
        File commitFile = join(COMMIT_DIR, refCommit);
        if(!commitFile.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit commit = readObject(commitFile, Commit.class);
        checkOutCommit(commit);

        writeContents(HEADPointer(), refCommit);
    }

    // TODO: Finish merge.
    public static void merge(String branchName) {
        repoExist();

        if (!emptyIndex()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        if (!join(HEADS_DIR, branchName).exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        if (branchName.equals(HEADPointer().getName())) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        String refGivenBranchHead = readContentsAsString(join(HEADS_DIR, branchName));
        Commit givenBranchHead = readObject(join(COMMIT_DIR, refGivenBranchHead), Commit.class);
        Commit splitCommit = splitPoint(HEADCommit(), givenBranchHead);

        if (splitCommit.equals(HEADCommit())) {
            checkoutBranch(branchName);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        if (splitCommit.equals(givenBranchHead)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        for (String fileName : plainFilenamesIn(CWD)) {
            File currentFile = join(CWD, fileName);
            String refFileContent = sha1(readContents(currentFile));

            if (splitCommit.trees.containsKey(fileName)) {
                boolean givenChanged = !splitCommit.trees.get(fileName).equals(givenBranchHead.trees.get(fileName));
                boolean notBothDeleted = HEADCommit().trees.containsKey(fileName) || givenBranchHead.trees.containsKey(fileName);
                boolean changedFromHead = !refFileContent.equals(HEADCommit().trees.get(fileName));
                if (givenChanged && notBothDeleted && changedFromHead) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            } else if (givenBranchHead.trees.containsKey(fileName)) {
                boolean changedFromHead = !refFileContent.equals(HEADCommit().trees.get(fileName));
                if (!givenBranchHead.trees.get(fileName).equals(HEADCommit().trees.get(fileName)) && changedFromHead) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
            // TODO: Recheck this part.
        }

        Set<String> fixedFiles = new HashSet<>();
        HashMap<String, String> trees = HEADCommit().trees;

        for (String fileName : HEADCommit().trees.keySet()) {
            String refContentHEAD = HEADCommit().trees.get(fileName);
            String refContentSplit = splitCommit.trees.get(fileName);
            String refContentGiven = givenBranchHead.trees.get(fileName);

            if (refContentSplit == null) {
                if (refContentGiven != null) {
                    mergeFile(refContentHEAD, refContentGiven, fileName);
                    trees.put(fileName, sha1(readContents(join(STAGED_DIR, fileName))));
                }
            } else {
                if (refContentSplit.equals(refContentHEAD) && !refContentSplit.equals(refContentGiven)) {
                    if (refContentGiven == null) {
                        saveFileAs(join(BLOB_DIR, refContentHEAD), STAGED_RM_DIR, fileName);
                        restrictedDelete(join(CWD, fileName));
                        trees.remove(fileName);
                    } else {
                        saveFileAs(join(BLOB_DIR, refContentHEAD), STAGED_DIR, fileName);
                        checkOutToCWD(join(STAGED_DIR, fileName), fileName);
                        trees.put(fileName, sha1(readContents(join(STAGED_DIR, fileName))));
                    }
                } else if (!refContentSplit.equals(refContentHEAD) && !refContentSplit.equals(refContentGiven)) {
                    if (!refContentHEAD.equals(refContentGiven)) {
                        mergeFile(refContentHEAD, refContentGiven, fileName);
                        trees.put(fileName, sha1(readContents(join(STAGED_DIR, fileName))));
                    }
                }
            }
            fixedFiles.add(fileName);
        }

        for (String fileName : givenBranchHead.trees.keySet()) {
            if (!fixedFiles.contains(fileName)) {
                String refFileGiven = HEADCommit().trees.get(fileName);
                if (splitCommit.trees.containsKey(fileName)) {
                    if (!refFileGiven.equals(splitCommit.trees.get(fileName))) {
                        mergeFile(null, refFileGiven, fileName);
                        trees.put(fileName, sha1(readContents(join(STAGED_DIR, fileName))));
                    }
                } else {
                    saveFileAs(join(BLOB_DIR, refFileGiven), STAGED_DIR, fileName);
                    checkOutToCWD(join(STAGED_DIR, fileName), fileName);
                    // TODO: Checkout the file to CWD.
                    trees.put(fileName, sha1(readContents(join(STAGED_DIR, fileName))));
                }
            }
        }

        if (emptyIndex()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        // TODO: Merge commit.
        Commit commit = new Commit(branchName, trees);
        saveCommit(commit);
        emptyStage();
    }

    /** Helper methods */

    /** Check out file to CWD. */
    private static void checkOutToCWD(File file, String fileName) {
        byte[] content = readContents(file);
        File fileCWD = join(CWD, fileName);
        writeContents(fileCWD, content);
    }

    /** Generate a merged file for solving the conflict to the STAGED_DIR.
     * The merged file is also checked out to the CWD. */
    private static void mergeFile(String refFile1, String refFile2, String fileName) {
        File fileStaged = join(STAGED_DIR, fileName);
        String HEADLine = "<<<<<<< HEAD" + "\n";
        String middleLine = "=======" + "\n";
        String endLine = ">>>>>>>" + "\n";
        String content = HEADLine + fileContent(refFile1) + middleLine + fileContent(refFile2) + endLine;
        writeContents(fileStaged, content);
        checkOutToCWD(fileStaged, fileName);
    }

    /** Read content as string from a file named fileName in the BLOB directory. */
    private static String fileContent(String fileName) {
        if (fileName == null) {
            return "";
        }
        File file = join(BLOB_DIR, fileName);
        return readContentsAsString(file);
    }

    /** Stage a file by adding it to directory as given name. Directories are usually STAGED_DIR or STAGED_RM_DIR.
     * Should check out the file to the CWD after staged for add, and delete the file from CWD after staged for
     * remove. */
    // TODO: Use this function in remove method and others.
    private static void saveFileAs(File fileName, File dir, String savedFileName) {
        byte[] fileContent = readContents(fileName);
        writeContents(join(dir, savedFileName), fileContent);
    }

    /** Empty the staged area in the repository. */
    public static void emptyStage() {
        for (String fileName : plainFilenamesIn(STAGED_DIR)) {
            join(STAGED_DIR, fileName).delete();
        }
        for (String fileName : plainFilenamesIn(STAGED_RM_DIR)) {
            join(STAGED_RM_DIR, fileName).delete();
        }
    }

    /** Check if the repository exists, i.e., the .gitlet directory exists. */
    public static void repoExist() {
        if(!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    /** Save the commit object to a file in the directory of commits. */
    public static void saveCommit(Commit commit) {
        String refCommit = sha1(serialize(commit));
        File initFile = join(COMMIT_DIR, refCommit);
        writeObject(initFile, commit);
        writeContents(HEADPointer(), refCommit);
    }

    /** Check if the staged area is not empty, so there are files to commit. */
    private static boolean emptyIndex() {
        return plainFilenamesIn(STAGED_DIR).size() + plainFilenamesIn(STAGED_RM_DIR).size() == 0;
    }

    /** Generate the file which the HEAD is pointing to. */
    public static File HEADPointer() {
        return new File(readContentsAsString(HEAD));
    }

    /** Return the ref string to the HEAD commit. */
    public static String refHEADCommit() {
        return readContentsAsString(HEADPointer());
    }

    /** Retrieve the HEAD commit from the file. */
    public static Commit HEADCommit() {
        return readObject(join(COMMIT_DIR, readContentsAsString(HEADPointer())), Commit.class);
    }

    /** Check out commit. */
    private static void checkOutCommit(Commit commit) {
        for (String fileName : plainFilenamesIn(CWD)) {
            if (commit.trees.containsKey(fileName) && (!HEADCommit().trees.containsKey(fileName))) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }
        for (String fileName : HEADCommit().trees.keySet()) {
            File currentFile = join(CWD, fileName);
            restrictedDelete(currentFile);
        }
        for (String fileName : commit.trees.keySet()) {
            byte[] fileContent = readContents(join(BLOB_DIR, commit.trees.get(fileName)));
            writeContents(join(CWD, fileName), fileContent);
        }
    }

    /** Return the commit at the split point of two commits. */
    private static Commit splitPoint(Commit commit1, Commit commit2) {
        Set<Commit> commitSet = new HashSet<>();
        findAncestor(commitSet, commit1);
        return latestCommonAncestor(commitSet, commit2);
    }

    /**
     * Put all ancestors of the given commit to the set of commit recursively.
     * @param commitSet The set of ancestors of commit.
     * @param commit
     */
    private static void findAncestor(Set<Commit> commitSet, Commit commit) {
        commitSet.add(commit);
        if (commit.getParent() != null) {
            findAncestor(commitSet, readObject(join(COMMIT_DIR, commit.getParent()), Commit.class));
        }
        if (commit.getSecondParent() != null) {
            findAncestor(commitSet, readObject(join(COMMIT_DIR, commit.getSecondParent()), Commit.class));
        }
    }

    /**
     * Find the latest common ancestor of the given commit and the commit whose ancestors
     * are in the given set of commit.
     * @param commitSet
     * @param commit
     * @return
     */
    private static Commit latestCommonAncestor(Set<Commit> commitSet, Commit commit) {
        if (commitSet.contains(commit)) {
            return commit;
        }

        Queue<Commit> fringe = new LinkedList<>();
        fringe.add(commit);

        while(!fringe.isEmpty()) {
            Commit currCommit = fringe.poll();
            if (currCommit.getParent() != null) {
                Commit firstParent = readObject(join(COMMIT_DIR, commit.getParent()), Commit.class);
                if (commitSet.contains(firstParent)) {
                    return firstParent;
                }
                fringe.add(firstParent);
            }
            if (currCommit.getSecondParent() != null) {
                Commit secondParent = readObject(join(COMMIT_DIR, commit.getSecondParent()), Commit.class);
                if (commitSet.contains(secondParent)) {
                    return secondParent;
                }
                fringe.add(secondParent);
            }
        }

        return null;
    }
}

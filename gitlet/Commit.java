package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Date; // You'll likely use this in this class
import java.util.HashMap;

import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Han Liang
 */
public class Commit implements Serializable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */



    /** The message of this Commit. */
    private String message;

    /** Timestamp of this commit. */
    private Date date;

    /** Parent and second parent (if this is a merge) of this commit. */
    private String parent;
    private String secondParent;

    /** Map from file names to reference of contents. */
    public HashMap<String, String> trees;

    /** Construct the initial commit of the repository. */
    public Commit() {
        message = "initial commit";
        date = new Date(0);
        trees = new HashMap<>();
    }

    /** Construct a new commit. */
    public Commit(String message) {
        this.message = message;
        date = new Date();
        this.parent = readContentsAsString(Repository.HEADPointer());
        Commit parentCommit = Repository.HEADCommit();
        this.trees = parentCommit.trees;
        for (String fileName : plainFilenamesIn(Repository.STAGED_DIR)) {
            File filePath = join(Repository.STAGED_DIR, fileName);
            byte[] fileContent = readContents(filePath);
            String fileRef = sha1(fileContent);
            this.trees.put(fileName, fileRef);
            filePath = join(Repository.BLOB_DIR, fileRef);
            if (!filePath.exists()) {
                writeContents(filePath, fileContent);
            }
        }
        for (String fileName : plainFilenamesIn(Repository.STAGED_RM_DIR)) {
            this.trees.remove(fileName);
        }
    }

    public Commit(String givenBranchName, HashMap<String, String> trees) {
        String currentBranchName = Repository.HEADPointer().getName();
        this.message = "Merged " + givenBranchName + " into " + currentBranchName + ".";
        this.date = new Date();
        this.parent = readContentsAsString(Repository.HEADPointer());
        this.secondParent = readContentsAsString(join(Repository.HEADS_DIR, givenBranchName));
        this.trees = trees;
        // Add staged files to the blob.
        for (String fileName : plainFilenamesIn(Repository.STAGED_DIR)) {
            File filePath = join(Repository.STAGED_DIR, fileName);
            byte[] fileContent = readContents(filePath);
            String fileRef = sha1(fileContent);
            filePath = join(Repository.BLOB_DIR, fileRef);
            if (!filePath.exists()) {
                writeContents(filePath, fileContent);
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        Commit commit = (Commit) other;

        boolean equal = this.message.equals(commit.message) && this.date.equals(commit.date);

        if (this.parent != null) {
            equal = equal && this.parent.equals(commit.parent);
        }
        if (this.secondParent != null) {
            equal = equal && this.secondParent.equals(commit.secondParent);
        }
        return equal;
    }

    @Override
    public int hashCode() {
        return this.date.hashCode();
    }

    private Commit fromFile (String commitName) {
        File commitFile = join(Repository.COMMIT_DIR, commitName);
        return readObject(commitFile, Commit.class);
    }

    public String getParent() {
        return this.parent;
    }

    public String getSecondParent() {
        return this.secondParent;
    }

    public Date getDate() {
        return this.date;
    }

    public String getMessage() {
        return this.message;
    }
}

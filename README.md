# Gitlet
This a version-control system, similar to the popular system Git but much simpler, implemented in Java.

## Design

### Classes and data structures

#### Repository
This class contains a set of static methods that set up and operate the files in the repository.

#### Commit
Every time changes of files are committed to the repository, a Commit object is created. A Commit
consist of a timestamp, a log message, a mapping of file names to blob references, a parent reference, and (for merges) a second parent reference.

### Algorithms

When user commit changes to the repository, the contents of files, which we refer to as 'blobs', are stored in the `.gitlet` directory. Each Commit object has a map which maps the file names to the corresponding blobs in the repository. The Commit objects are also serialized and stored in the `.gitlet` directory.

Every object–every blob and every commit–has a unique integer id that serves as a reference to the object. Gitlet accomplishes this in the same way as Git: by using a cryptographic hash function called SHA-1 (Secure Hash 1).

## Commands

### init
* Usage: `java gitlet.Main init`
* Starts a repository in the current directory.

### add
* Usage: `java gitlet.Main add [file name]`
* Adds a copy of the file as it currently exists to the staging area.

### commit
* Usage: `java gitlet.Main commit [message]`
* Creates a new commit. Save the tracked files in the staging area.

### rm
* Usage: `java gitlet.Main rm [file name]`
* Unstages the file if it is currently staged for addition. If the file is tracked in the current commit, stage it for removal and remove the file from the working directory if the user has not already done so.

### log
* Usage: `java gitlet.Main log`
* Displays information about each commit starting from the current head backwards along the commit tree until the initial commit.

### global-log
* Usage: `java gitlet.Main global-log`
* Displays information about all commits ever made.

### status
* Usage: `java gitlet.Main status`
* Displays branches currently exist and files in the staging area.

### checkout
* Usages:
1. `java gitlet.Main checkout -- [file name]`
2. `java gitlet.Main checkout [commit id] -- [file name]`
3. `java gitlet.Main checkout [branch name]`
* Descriptions:
1. Takes the version of the file as it exists in the head commit and puts it in the working directory.
2. Takes the version of the file as it exists in the commit with the given id, and puts it in the working directory.
3. Changes branch.

### branch
* Usage: `java gitlet.Main branch [branch name]`
* Creates a new branch with the given name, and points it at the current head commit.

### rm-branch
* Usage: `java gitlet.Main rm-branch [branch name]`
* Removes branch with the given name.

### reset
* Usage: `java gitlet.Main reset [commit id]`
* Checks out all the files tracked by the given commit.

### merge
* Usage: `java gitlet.Main merge [branch name]`
* Merges two branches.

## Acknowledgement
The design of this project follows the instruction of the course project [Gitlet](https://sp21.datastructur.es/materials/proj/proj2/proj2), from the course _Data Structures and Algorithms_, University of California, Berkeley.
